import type React from "react";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { EvaluationOrganizationMappingPage } from "./SCR-EVALUATION-ORG-MAPPING";
import { BusinessStatusCodePage } from "./SCR-BUSINESS-STATUS-CODE";
import { BusinessStatusTransitionPage } from "./SCR-BUSINESS-STATUS-TRANSITION";
import { RejectionReasonPage } from "./SCR-REJECTION-REASON";
import { DataChangeHistoryPage } from "./SCR-DATA-CHANGE-HISTORY";
import { DeletedBusinessDataPage } from "./SCR-DELETED-BUSINESS-DATA";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  const emptyResponse = (collectionKey: string) => ({
    success: true,
    data: {
      [collectionKey]: [],
      page: 0,
      size: 20,
      totalElements: 0,
    },
    meta: {},
  });
  return {
    ...actual,
    evaluationOrganizationMappingApi: {
      listEvaluationOrganizationMappings: vi.fn(async () =>
        emptyResponse("mappings"),
      ),
      saveEvaluationOrganizationMapping: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
    businessStatusCodeApi: {
      listBusinessStatusCodes: vi.fn(async () => emptyResponse("statusCodes")),
      saveBusinessStatusCode: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
    businessStatusTransitionApi: {
      listBusinessStatusTransitions: vi.fn(async () =>
        emptyResponse("transitions"),
      ),
      saveBusinessStatusTransition: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
    rejectionReasonApi: {
      listRejectionReasons: vi.fn(async () => emptyResponse("reasons")),
      saveRejectionReason: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
    dataChangeHistoryApi: {
      listDataChangeHistories: vi.fn(async () => emptyResponse("histories")),
    },
    deletedBusinessDataApi: {
      listDeletedBusinessData: vi.fn(async () => emptyResponse("deletedData")),
    },
  };
});

type PageSize = 20 | 50 | 100;

type ListCase = {
  routeName: string;
  renderPage: () => React.ReactElement;
  selectTestId: string;
  importMock: () => Promise<ReturnType<typeof vi.fn>>;
};

const listCases: ListCase[] = [
  {
    routeName: "SCR-EVALUATION-ORG-MAPPING",
    renderPage: () => <EvaluationOrganizationMappingPage />,
    selectTestId: "evaluation-organization-page-size-select",
    importMock: async () =>
      (await import("../../api/apiClient")).evaluationOrganizationMappingApi
        .listEvaluationOrganizationMappings as ReturnType<typeof vi.fn>,
  },
  {
    routeName: "SCR-BUSINESS-STATUS-CODE",
    renderPage: () => <BusinessStatusCodePage />,
    selectTestId: "business-status-code-page-size-select",
    importMock: async () =>
      (await import("../../api/apiClient")).businessStatusCodeApi
        .listBusinessStatusCodes as ReturnType<typeof vi.fn>,
  },
  {
    routeName: "SCR-BUSINESS-STATUS-TRANSITION",
    renderPage: () => <BusinessStatusTransitionPage />,
    selectTestId: "business-status-transition-page-size-select",
    importMock: async () =>
      (await import("../../api/apiClient")).businessStatusTransitionApi
        .listBusinessStatusTransitions as ReturnType<typeof vi.fn>,
  },
  {
    routeName: "SCR-REJECTION-REASON",
    renderPage: () => <RejectionReasonPage />,
    selectTestId: "rejection-reason-page-size-select",
    importMock: async () =>
      (await import("../../api/apiClient")).rejectionReasonApi
        .listRejectionReasons as ReturnType<typeof vi.fn>,
  },
  {
    routeName: "SCR-DATA-CHANGE-HISTORY",
    renderPage: () => <DataChangeHistoryPage />,
    selectTestId: "data-change-history-page-size-select",
    importMock: async () =>
      (await import("../../api/apiClient")).dataChangeHistoryApi
        .listDataChangeHistories as ReturnType<typeof vi.fn>,
  },
  {
    routeName: "SCR-DELETED-BUSINESS-DATA",
    renderPage: () => <DeletedBusinessDataPage />,
    selectTestId: "deleted-business-data-page-size-select",
    importMock: async () =>
      (await import("../../api/apiClient")).deletedBusinessDataApi
        .listDeletedBusinessData as ReturnType<typeof vi.fn>,
  },
];

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("BASIC-32 common verification", () => {
  it("applies default 20 rows and 20/50/100 selectable sizes to every new list screen for T999", async () => {
    for (const item of listCases) {
      const listMock = await item.importMock();
      render(item.renderPage());

      const select = screen.getByTestId(item.selectTestId) as HTMLSelectElement;
      expect(select, item.routeName).toHaveValue("20");
      expect(select.querySelectorAll("option")).toHaveLength(3);
      expect(optionValues(select)).toEqual(["20", "50", "100"]);
      await waitFor(() =>
        expect(listMock).toHaveBeenCalledWith(
          expect.objectContaining({ page: 0, size: 20 }),
        ),
      );

      for (const size of [50, 100] satisfies PageSize[]) {
        fireEvent.change(select, { target: { value: String(size) } });
        await waitFor(() =>
          expect(listMock).toHaveBeenCalledWith(
            expect.objectContaining({ page: 0, size }),
          ),
        );
        expect(select).toHaveValue(String(size));
      }

      cleanup();
      vi.clearAllMocks();
    }
  });
});

function optionValues(select: HTMLSelectElement) {
  return Array.from(select.options).map((option) => option.value);
}

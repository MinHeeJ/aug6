import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { CourseOfferingOperationAchievementPage } from "./SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT";
import { EmploymentRateAchievementPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENT";
import { EmploymentRateImprovementAchievementPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT";
import { TeachingImprovementAchievementPage } from "./SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const screenContracts = [
  {
    apiPath: "/api/business/employment-rate-improvements?page=0&pageSize=50",
    page: <EmploymentRateImprovementAchievementPage />,
    pageSizeTestId: "employment-rate-improvement-page-size-select",
    rootTestId: "employment-rate-improvement-page",
  },
  {
    apiPath: "/api/business/course-operations?page=0&pageSize=50",
    page: <CourseOfferingOperationAchievementPage />,
    pageSizeTestId: "course-operation-page-size-select",
    rootTestId: "course-operation-page",
  },
  {
    apiPath: "/api/business/lecture-improvements?page=0&pageSize=50",
    page: <TeachingImprovementAchievementPage />,
    pageSizeTestId: "teaching-improvement-page-size-select",
    rootTestId: "teaching-improvement-page",
  },
  {
    apiPath: "/api/business/employment-rate-achievements?page=0&pageSize=50",
    page: <EmploymentRateAchievementPage />,
    pageSizeTestId: "employment-rate-achievement-page-size-select",
    rootTestId: "employment-rate-achievement-page",
  },
];

describe("BASIC-83 browser compatibility", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [] },
      meta: {},
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("keeps every achievement screen usable with the required 20/50/100 list size control", async () => {
    for (const contract of screenContracts) {
      const view = render(contract.page);
      const pageSize = await screen.findByTestId(contract.pageSizeTestId);

      expect(screen.getByTestId(contract.rootTestId)).toBeVisible();
      expect(pageSize).toHaveValue("20");
      expect(pageSize).toHaveTextContent("20건");
      expect(pageSize).toHaveTextContent("50건");
      expect(pageSize).toHaveTextContent("100건");

      fireEvent.change(pageSize, { target: { value: "50" } });
      await waitFor(() => {
        expect(
          vi
            .mocked(apiRequest)
            .mock.calls.some(([path]) => path === contract.apiPath),
        ).toBe(true);
      });
      view.unmount();
    }
  });

  it("requires a save confirmation before the employment-rate mutation request", async () => {
    const confirm = vi.fn(() => false);
    vi.stubGlobal("confirm", confirm);
    const view = render(<EmploymentRateAchievementPage />);

    await screen.findByTestId("employment-rate-achievement-save-button");
    await waitFor(() => {
      expect(apiRequest).toHaveBeenCalledTimes(1);
    });
    fireEvent.click(
      screen.getByTestId("employment-rate-achievement-save-button"),
    );

    expect(confirm).toHaveBeenCalledWith("취업률 실적을 저장하시겠습니까?");
    expect(apiRequest).toHaveBeenCalledTimes(1);
    view.unmount();
  });
});

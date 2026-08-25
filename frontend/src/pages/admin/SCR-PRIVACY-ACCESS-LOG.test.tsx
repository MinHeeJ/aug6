import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PrivacyAccessLogPage } from "./SCR-PRIVACY-ACCESS-LOG";
import { privacyAccessLogApi } from "../../api/apiClient";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    privacyAccessLogApi: {
      searchPrivacyAccessLogs: vi.fn(),
      getPrivacyAccessLog: vi.fn(),
    },
  };
});

const logRow = {
  historyId: 9001,
  processType: "VIEW" as const,
  actorUserId: 1,
  actorLoginId: "admin",
  targetRef: "TARGET-2026-001",
  processPurpose: "감사 검증 목적",
  processedAt: "2026-08-25T09:10:00",
  requestIp: "203.0.113.10",
  processResult: "SUCCESS" as const,
};

describe("PrivacyAccessLogPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(privacyAccessLogApi.searchPrivacyAccessLogs).mockResolvedValue({
      success: true,
      data: { logs: [logRow], page: 0, size: 20, totalElements: 1 },
      meta: {},
    });
    vi.mocked(privacyAccessLogApi.getPrivacyAccessLog).mockResolvedValue({
      success: true,
      data: logRow,
      meta: {},
    });
  });

  it("기본 20건으로 처리이력을 조회하고 원문 개인정보 열을 표시하지 않는다", async () => {
    render(<PrivacyAccessLogPage />);

    expect(await screen.findByText("TARGET-2026-001")).toBeInTheDocument();
    expect(
      screen.getByTestId("privacy-access-log-page-size-select"),
    ).toHaveValue("20");
    expect(screen.queryByText("actualValue")).not.toBeInTheDocument();
    expect(screen.queryByText("originalValue")).not.toBeInTheDocument();
    expect(privacyAccessLogApi.searchPrivacyAccessLogs).toHaveBeenCalledWith({
      actorUserId: undefined,
      targetRef: "",
      processType: "",
      processedFrom: "",
      processedTo: "",
      page: 0,
      size: 20,
    });
  });

  it("검색 조건과 상세 선택을 API로 연결하고 상세 영역은 조회 전용이다", async () => {
    render(<PrivacyAccessLogPage />);
    await screen.findByText("TARGET-2026-001");

    fireEvent.change(screen.getByTestId("privacy-access-log-actor-input"), {
      target: { value: "1" },
    });
    fireEvent.change(
      screen.getByTestId("privacy-access-log-process-type-select"),
      {
        target: { value: "VIEW" },
      },
    );
    fireEvent.click(screen.getByTestId("privacy-access-log-search-button"));

    await waitFor(() =>
      expect(
        privacyAccessLogApi.searchPrivacyAccessLogs,
      ).toHaveBeenLastCalledWith(
        expect.objectContaining({ actorUserId: 1, processType: "VIEW" }),
      ),
    );

    fireEvent.click(screen.getByTestId("privacy-access-log-row-9001"));
    await waitFor(() =>
      expect(
        screen.getByTestId("privacy-access-log-detail-region"),
      ).toHaveTextContent("감사 검증 목적"),
    );
    expect(
      screen.queryByTestId("privacy-access-log-edit-button"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("privacy-access-log-delete-button"),
    ).not.toBeInTheDocument();
  });
});

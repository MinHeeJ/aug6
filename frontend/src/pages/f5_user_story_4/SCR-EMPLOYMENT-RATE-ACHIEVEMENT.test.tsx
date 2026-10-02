import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { EmploymentRateAchievementPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

function uploadResponse(data: unknown, status = 200) {
  return new Response(
    JSON.stringify({ success: status < 400, data, meta: {} }),
    { status, headers: { "content-type": "application/json" } },
  );
}

describe("SCR-EMPLOYMENT-RATE-ACHIEVEMENT", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("saves a valid individual achievement through the create contract", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievement: {
            achievementId: 71,
            managementItemCode: "EMPLOYMENT_RATE",
            achievementDate: "2026-04-10",
            achievementName: "취업률 제고 활동",
          },
        },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
        meta: {},
      });

    render(<EmploymentRateAchievementPage />);

    await screen.findByText("조회된 취업률 실적이 없습니다");
    fireEvent.change(
      screen.getByTestId("employment-rate-management-item-input"),
      {
        target: { value: "EMPLOYMENT_RATE" },
      },
    );
    fireEvent.change(
      screen.getByTestId("employment-rate-achievement-date-input"),
      {
        target: { value: "2026-04-10" },
      },
    );
    fireEvent.change(
      screen.getByTestId("employment-rate-achievement-name-input"),
      {
        target: { value: "취업률 제고 활동" },
      },
    );
    fireEvent.click(screen.getByTestId("employment-rate-save-button"));
    fireEvent.click(screen.getByTestId("employment-rate-save-confirm-button"));

    await screen.findByText("저장되었습니다.");
    expect(vi.mocked(apiRequest)).toHaveBeenNthCalledWith(
      2,
      "/api/business/employment-rate-achievements",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("renders an Excel validation error and keeps the result out of commit flow", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: {},
    });
    const fetch = vi.fn().mockResolvedValue(
      uploadResponse({
        uploadId: "UPLOAD-ERR",
        totalCount: 1,
        successCount: 0,
        errorCount: 1,
        errors: [
          {
            rowNumber: 2,
            columnName: "managementItemCode",
            errorReason: "관리항목은 필수입니다.",
          },
        ],
      }),
    );
    vi.stubGlobal("fetch", fetch);

    render(<EmploymentRateAchievementPage />);

    await screen.findByText("조회된 취업률 실적이 없습니다");
    fireEvent.click(screen.getByRole("tab", { name: "Excel 일괄등록" }));
    const file = new File(["invalid excel"], "employment-rate.xlsx", {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });
    fireEvent.change(screen.getByTestId("employment-rate-excel-file-input"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByTestId("employment-rate-excel-upload-button"));

    expect(
      await screen.findByText("오류 행이 있어 전체 반영하지 않았습니다."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("2행 managementItemCode: 관리항목은 필수입니다."),
    ).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/excel-uploads",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("confirms a bulk request and renders the returned R07 job result", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: {
          jobId: "JOB-83",
          jobStatus: "COMPLETED",
          totalCount: 2,
          processedCount: 1,
          unprocessedCount: 1,
          items: [
            {
              targetUserId: 101,
              processedYn: "N",
              unprocessedReason: "승인 조건 확인 필요",
            },
          ],
        },
        meta: {},
      });

    render(<EmploymentRateAchievementPage />);

    await screen.findByText("조회된 취업률 실적이 없습니다");
    fireEvent.click(screen.getByRole("tab", { name: "일괄 생성·삭제" }));
    fireEvent.change(
      screen.getByTestId("employment-rate-bulk-evaluation-year-input"),
      { target: { value: "2026" } },
    );
    fireEvent.click(screen.getByTestId("employment-rate-bulk-request-button"));
    fireEvent.click(screen.getByTestId("employment-rate-bulk-confirm-button"));

    await screen.findByTestId("employment-rate-bulk-result");
    expect(vi.mocked(apiRequest)).toHaveBeenNthCalledWith(
      2,
      "/api/business/employment-rate-achievements/bulk-jobs",
      expect.objectContaining({ method: "POST" }),
    );
    expect(screen.getByText("승인 조건 확인 필요")).toBeInTheDocument();
    await waitFor(() =>
      expect(
        screen.getByText("작업 ID: JOB-83 / 상태: COMPLETED"),
      ).toBeInTheDocument(),
    );
  });
});

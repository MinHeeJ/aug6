import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { EmploymentRateAchievementsPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENTS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const draftRow = {
  achievementId: 91,
  managementItemCode: "EMPLOYMENT_RATE",
  achievementDate: "2026-04-15",
  achievementName: "취업률 실적",
  attachmentIds: ["ATT-1"],
  achievementStatus: "DRAFT",
};

describe("SCR-EMPLOYMENT-RATE-ACHIEVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.stubGlobal(
      "confirm",
      vi.fn(() => true),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("loads an authorized row, retrieves detail, and saves selected data", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [draftRow],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })
      .mockResolvedValueOnce({ success: true, data: draftRow, meta: {} })
      .mockResolvedValueOnce({
        success: true,
        data: { achievement: draftRow, occurredDateWarning: false },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [draftRow],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      });

    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("취업률 실적");
    fireEvent.click(screen.getByTestId("employment-rate-detail-button"));
    await waitFor(() => expect(apiRequest).toHaveBeenCalledTimes(2));
    fireEvent.click(screen.getByTestId("employment-rate-save-button"));

    await screen.findByText("저장되었습니다.");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/91",
      expect.objectContaining({ method: "PUT" }),
    );
  });

  it("shows validation result from Excel upload and keeps an error result visible", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: {},
    });
    const fetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          success: true,
          data: {
            uploadId: "ER-UP-ERR",
            originalFileName: "employment-rate.csv",
            totalCount: 1,
            successCount: 0,
            errorCount: 1,
            errors: [
              {
                rowNumber: 2,
                columnName: "achievementDate",
                errorReason: "날짜 오류",
              },
            ],
          },
          meta: {},
        }),
        { status: 200, headers: { "content-type": "application/json" } },
      ),
    );
    vi.stubGlobal("fetch", fetch);

    render(<EmploymentRateAchievementsPage />);
    await screen.findByTestId("employment-rate-excel-tab");
    fireEvent.click(screen.getByTestId("employment-rate-excel-tab"));
    const file = new File(["header\n"], "employment-rate.csv", {
      type: "text/csv",
    });
    fireEvent.change(screen.getByTestId("employment-rate-file-input"), {
      target: { files: [file] },
    });
    fireEvent.click(screen.getByTestId("employment-rate-upload-button"));

    await screen.findByText("오류가 있어 전체 반영하지 않습니다.", {
      exact: false,
    });
    expect(fetch).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/excel-uploads",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("renders batch result only after an R07-visible job identity is entered", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: {
          batchJobId: "B83-BATCH-001",
          evaluationYear: "2026",
          actionType: "GENERATE",
          jobStatus: "COMPLETED",
          totalCount: 3,
          successCount: 3,
          failureCount: 0,
          unprocessedCount: 0,
        },
        meta: {},
      });

    render(<EmploymentRateAchievementsPage />);
    fireEvent.click(screen.getByTestId("employment-rate-batch-tab"));
    fireEvent.change(screen.getByTestId("employment-rate-batch-job-id-input"), {
      target: { value: "B83-BATCH-001" },
    });
    fireEvent.click(screen.getByTestId("employment-rate-batch-result-button"));

    await screen.findByTestId("employment-rate-batch-result");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/bulk-jobs/B83-BATCH-001",
    );
  });
});

import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { EmploymentRateImprovementsPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENTS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const confirmedRow = {
  achievementId: 81,
  managementNo: "B83-ERI-003",
  teacherName: "business-owner",
  managementItemCode: "EMPLOYMENT_RATE_IMPROVEMENT",
  achievementDate: "2026-04-12",
  specialLectureStartDate: "2026-04-01",
  specialLectureEndDate: "2026-04-10",
  mockExamQuestionPeriod: "2026-1",
  attachmentIds: ["opaque-file-ref"],
  achievementStatus: "EVALUATION_CONFIRMED",
};

describe("SCR-EMPLOYMENT-RATE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("loads the approved list and locks a selected evaluation-confirmed achievement", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [confirmedRow],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })
      .mockResolvedValueOnce({ success: true, data: confirmedRow, meta: {} });

    render(<EmploymentRateImprovementsPage />);

    await screen.findByText("B83-ERI-003");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-improvements?page=0&pageSize=20",
    );

    fireEvent.click(
      screen.getByTestId("employment-rate-improvements-detail-button"),
    );

    await screen.findByTestId("employment-rate-improvements-lock-message");
    expect(
      screen.getByTestId("employment-rate-improvements-save-button"),
    ).toBeDisabled();
    expect(
      screen.getByTestId("employment-rate-improvements-achievement-date-input"),
    ).toBeDisabled();
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-improvements/81",
    );
  });
});

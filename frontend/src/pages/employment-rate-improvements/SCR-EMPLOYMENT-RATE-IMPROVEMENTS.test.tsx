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
  teacherName: "교원",
  managementItemCode: "EMPLOYMENT_IMPROVEMENT",
  achievementDate: "2026-04-15",
  specialLectureStartDate: "2026-04-11",
  specialLectureEndDate: "2026-04-15",
  mockExamQuestionPeriod: "확정 잠금 검증",
  certificationStatus: "EVALUATION_CONFIRMED",
  hasAttachments: false,
};

describe("SCR-EMPLOYMENT-RATE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("shows an evaluation-confirmed selected record as read-only", async () => {
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
    fireEvent.click(
      screen.getByTestId("employment-rate-improvements-detail-button"),
    );

    expect(
      await screen.findByTestId("employment-rate-improvements-lock-message"),
    ).toHaveTextContent("평가확정 실적은 수정할 수 없습니다.");
    expect(
      screen.getByTestId("employment-rate-improvements-save-button"),
    ).toBeDisabled();
    expect(
      screen.getByTestId("employment-rate-improvements-management-item-input"),
    ).toBeDisabled();
  });

  it("submits a new record to the approved relative API path", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievement: { ...confirmedRow, certificationStatus: "DRAFT" },
          achievementDateWarning: false,
        },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
        meta: {},
      });

    render(<EmploymentRateImprovementsPage />);

    await screen.findByText("조회된 취업률 제고 실적이 없습니다");
    fireEvent.change(
      screen.getByTestId("employment-rate-improvements-management-item-input"),
      { target: { value: "EMPLOYMENT_IMPROVEMENT" } },
    );
    fireEvent.change(
      screen.getByTestId("employment-rate-improvements-achievement-date-input"),
      { target: { value: "2026-04-10" } },
    );
    fireEvent.click(
      screen.getByTestId("employment-rate-improvements-save-button"),
    );

    await screen.findByText("저장되었습니다.");
    expect(vi.mocked(apiRequest)).toHaveBeenCalledWith(
      "/api/business/employment-rate-improvements",
      expect.objectContaining({ method: "POST" }),
    );
  });
});

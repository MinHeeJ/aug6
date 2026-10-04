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
  achievementId: 83,
  managementNo: "B83-ERI-003",
  teacherName: "교원",
  managementItemCode: "EMPLOYMENT_RATE_IMPROVEMENT",
  achievementDate: "2026-04-12",
  specialLectureStartDate: "2026-04-05",
  specialLectureEndDate: "2026-04-12",
  mockExamQuestionPeriod: "2026-2차",
  achievementStatus: "EVALUATION_CONFIRMED",
};

describe("SCR-EMPLOYMENT-RATE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("renders API rows and prevents saving an evaluation-confirmed record", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: {
        achievements: [confirmedRow],
        page: 0,
        pageSize: 20,
        totalElements: 1,
      },
      meta: {},
    });

    render(<EmploymentRateImprovementsPage />);

    await screen.findByText("B83-ERI-003");
    fireEvent.click(
      screen.getByTestId("employment-rate-improvements-detail-button"),
    );

    expect(
      screen.getByTestId("employment-rate-improvements-confirmed-lock-message"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("employment-rate-improvements-save-button"),
    ).toBeDisabled();
  });
});

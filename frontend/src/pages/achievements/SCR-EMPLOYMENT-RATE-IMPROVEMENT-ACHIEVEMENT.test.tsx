import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { EmploymentRateImprovementAchievementPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT";

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
  managementItemCode: "EMPLOYMENT_RATE",
  achievementDate: "2026-04-12",
  certificationStatus: "EVALUATION_CONFIRMED",
};

describe("SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("loads the selected confirmed row and prevents a mutation", async () => {
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

    render(<EmploymentRateImprovementAchievementPage />);

    await screen.findByText("B83-ERI-003");
    fireEvent.click(
      screen.getByTestId("employment-rate-improvement-detail-button"),
    );

    expect(
      await screen.findByText("평가확정 실적은 수정할 수 없습니다."),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("employment-rate-improvement-save-button"),
    ).toBeDisabled();
  });
});

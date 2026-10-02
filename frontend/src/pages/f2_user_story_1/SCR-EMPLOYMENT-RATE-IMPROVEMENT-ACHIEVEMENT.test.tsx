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
  achievementId: 83,
  managementItemCode: "EMPLOYMENT_IMPROVEMENT",
  achievementDate: "2026-04-10",
  specialLectureStartDate: "2026-04-01",
  specialLectureEndDate: "2026-04-10",
  certificationStatus: "EVALUATION_CONFIRMED",
};

describe("SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("locks a confirmed achievement after its API detail is loaded", async () => {
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
      .mockResolvedValueOnce({
        success: true,
        data: { achievement: confirmedRow },
        meta: {},
      });

    render(<EmploymentRateImprovementAchievementPage />);

    await screen.findByText("EMPLOYMENT_IMPROVEMENT");
    fireEvent.click(
      screen.getByTestId("employment-rate-improvement-detail-button"),
    );

    expect(
      await screen.findByTestId(
        "employment-rate-improvement-confirmed-lock-message",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("employment-rate-improvement-save-button"),
    ).toBeDisabled();
    expect(vi.mocked(apiRequest)).toHaveBeenLastCalledWith(
      "/api/business/employment-rate-improvements/83",
    );
  });

  it("blocks a missing required management item before an API save", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: {},
    });

    render(<EmploymentRateImprovementAchievementPage />);

    await screen.findByText("조회된 취업률 제고 실적이 없습니다");
    fireEvent.click(
      screen.getByTestId("employment-rate-improvement-save-button"),
    );

    expect(
      await screen.findByText("관리항목은 필수입니다."),
    ).toBeInTheDocument();
    expect(vi.mocked(apiRequest)).toHaveBeenCalledTimes(1);
  });
});

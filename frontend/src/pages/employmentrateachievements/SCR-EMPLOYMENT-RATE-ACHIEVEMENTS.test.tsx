import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { EmploymentRateAchievementsPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENTS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

describe("SCR-EMPLOYMENT-RATE-ACHIEVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("renders API-provided rows and locks a confirmed selected achievement", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: {
        achievements: [
          {
            achievementId: 501,
            managementNo: "ERA-001",
            managementItemCode: "EMPLOYMENT_RATE",
            achievementDate: "2026-04-10",
            achievementName: "취업률 실적",
            achievementStatus: "EVALUATION_CONFIRMED",
          },
        ],
        page: 0,
        pageSize: 20,
        totalElements: 1,
      },
      meta: {},
    });

    render(<EmploymentRateAchievementsPage />);

    await screen.findByText("ERA-001");
    fireEvent.click(
      screen.getByTestId("employment-rate-achievements-detail-button"),
    );

    expect(
      screen.getByTestId("employment-rate-achievements-confirmed-message"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("employment-rate-achievements-save-button"),
    ).toBeDisabled();
  });

  it("submits only a confirmed bulk preview with selected target IDs", async () => {
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
          jobId: "ERB-001",
          jobStatus: "REQUESTED",
          totalCount: 2,
          processedCount: 0,
          unprocessedCount: 2,
        },
        meta: {},
      });

    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("조회된 취업률 실적이 없습니다");
    fireEvent.change(
      screen.getByTestId("employment-rate-achievements-bulk-year-input"),
      { target: { value: "2026" } },
    );
    fireEvent.change(
      screen.getByTestId("employment-rate-achievements-target-user-ids-input"),
      { target: { value: "101, 202" } },
    );
    fireEvent.click(
      screen.getByTestId("employment-rate-achievements-bulk-button"),
    );

    await screen.findByTestId("employment-rate-achievements-bulk-result");
    expect(apiRequest).toHaveBeenLastCalledWith(
      "/api/business/employment-rate-achievements/bulk-jobs",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          evaluationYear: "2026",
          actionType: "GENERATE",
          targetCondition: { confirmed: true, targetUserIds: [101, 202] },
        }),
      }),
    );
  });

  it("renders an empty state when the list response has no rows", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: {},
    });

    render(<EmploymentRateAchievementsPage />);

    expect(
      await screen.findByText("조회된 취업률 실적이 없습니다"),
    ).toBeInTheDocument();
  });
});

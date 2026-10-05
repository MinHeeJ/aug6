import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
    vi.stubGlobal(
      "confirm",
      vi.fn(() => true),
    );
  });

  it("loads an API-backed row, reads its detail, and saves the selected achievement", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [
            {
              achievementId: 91,
              managementNo: "B83-ERA-001",
              managementItemCode: "EMPLOYMENT_RATE",
              achievementDate: "2026-04-10",
              achievementName: "취업률 실적",
              achievementStatus: "DRAFT",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievementId: 91,
          managementNo: "B83-ERA-001",
          managementItemCode: "EMPLOYMENT_RATE",
          achievementDate: "2026-04-10",
          achievementName: "취업률 실적",
          achievementStatus: "DRAFT",
        },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievement: {
            achievementId: 91,
            managementNo: "B83-ERA-001",
            managementItemCode: "EMPLOYMENT_RATE",
            achievementDate: "2026-04-10",
            achievementName: "수정 취업률 실적",
            achievementStatus: "DRAFT",
          },
          occurredDateWarning: false,
        },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
        meta: {},
      });

    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("B83-ERA-001");
    fireEvent.click(screen.getByTestId("employment-rate-detail-button"));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/employment-rate-achievements/91",
      ),
    );
    fireEvent.change(screen.getByTestId("employment-rate-name-input"), {
      target: { value: "수정 취업률 실적" },
    });
    fireEvent.click(screen.getByTestId("employment-rate-save-button"));

    await screen.findByText("저장되었습니다.");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/91",
      expect.objectContaining({ method: "PUT" }),
    );
  });

  it("renders the explicit policy conflict from the R07 batch endpoint", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
        meta: {},
      })
      .mockRejectedValueOnce(new Error("BULK_POLICY_PENDING"));

    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("조회된 취업률 실적이 없습니다");
    fireEvent.click(screen.getByTestId("employment-rate-bulk-request-button"));

    await screen.findByText("BULK_POLICY_PENDING");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/bulk-jobs",
      expect.objectContaining({ method: "POST" }),
    );
  });
});

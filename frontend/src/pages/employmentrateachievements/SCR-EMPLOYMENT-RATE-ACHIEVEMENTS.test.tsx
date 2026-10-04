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

const row = {
  achievementId: 83,
  managementNo: "B83-ER-001",
  managementItemCode: "EMPLOYMENT_RATE",
  achievementDate: "2026-04-01",
  achievementName: "취업률 실적",
  attachmentIds: [],
  achievementStatus: "DRAFT",
};

describe("SCR-EMPLOYMENT-RATE-ACHIEVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("loads a selected achievement detail and sends its selected identifier on update", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      })
      .mockResolvedValueOnce({ success: true, data: row, meta: {} })
      .mockResolvedValueOnce({ success: true, data: row, meta: {} })
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      });

    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("B83-ER-001");
    fireEvent.click(screen.getByTestId("employment-rate-detail-button"));
    await waitFor(() => {
      expect(apiRequest).toHaveBeenNthCalledWith(
        2,
        "/api/business/employment-rate-achievements/83",
      );
    });
    fireEvent.change(screen.getByTestId("employment-rate-name-input"), {
      target: { value: "수정 취업률 실적" },
    });
    fireEvent.click(screen.getByTestId("employment-rate-save-button"));

    await waitFor(() => {
      expect(apiRequest).toHaveBeenNthCalledWith(
        3,
        "/api/business/employment-rate-achievements/83",
        expect.objectContaining({ method: "PUT" }),
      );
    });
  });

  it("uses the server download operation and exposes an empty list state", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: {},
    });

    render(<EmploymentRateAchievementsPage />);
    await screen.findByText("조회된 취업률 실적이 없습니다");
    expect(screen.getByTestId("employment-rate-download-link")).toHaveAttribute(
      "href",
      "/api/business/employment-rate-achievements/download?page=0&pageSize=20",
    );
  });
});

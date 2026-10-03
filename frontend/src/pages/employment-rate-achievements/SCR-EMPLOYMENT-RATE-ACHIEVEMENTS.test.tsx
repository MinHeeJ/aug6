import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { EmploymentRateAchievementsPage } from "./SCR-EMPLOYMENT-RATE-ACHIEVEMENTS";

vi.mock("../../api/apiClient", async () => ({
  ...(await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  )),
  apiRequest: vi.fn(),
}));

describe("SCR-EMPLOYMENT-RATE-ACHIEVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("renders API rows and displays upload validation outcomes", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [
            {
              achievementId: 1,
              managementNo: "ER-1",
              managementItemCode: "EMPLOYMENT_RATE",
              achievementDate: "2026-03-01",
              achievementName: "취업률 제고",
              certificationStatus: "DRAFT",
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
          uploadId: "UP-1",
          totalCount: 1,
          successCount: 0,
          errorCount: 1,
          persistedCount: 0,
          errors: [{ rowNumber: 2, errorReason: "중복 행" }],
        },
        meta: {},
      });

    render(<EmploymentRateAchievementsPage />);

    expect(await screen.findByText("취업률 제고")).toBeInTheDocument();
    fireEvent.change(screen.getByTestId("employment-rate-upload"), {
      target: {
        files: [new File(["x"], "input.csv", { type: "text/csv" })],
      },
    });
    expect(
      await screen.findByText("정상 0건 / 오류 1건 / 반영 0건"),
    ).toBeInTheDocument();
  });
});

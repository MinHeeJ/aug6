import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { EmploymentRateImprovementsPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENTS";

function response(data: unknown, status = 200) {
  return new Response(
    JSON.stringify({ success: status < 400, data, meta: {} }),
    {
      status,
      headers: { "content-type": "application/json" },
    },
  );
}

describe("SCR-EMPLOYMENT-RATE-IMPROVEMENTS", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("loads the API-backed list and creates an employment-rate improvement", async () => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(
        response({ achievements: [], page: 0, pageSize: 20, totalElements: 0 }),
      )
      .mockResolvedValueOnce(
        response({
          achievementId: 83,
          managementNo: "ERI-83",
          managementItemCode: "EMPLOYMENT_RATE_IMPROVEMENT",
          achievementDate: "2026-03-02",
          achievementStatus: "DRAFT",
        }),
      )
      .mockResolvedValueOnce(
        response({
          achievements: [
            {
              achievementId: 83,
              managementNo: "ERI-83",
              managementItemCode: "EMPLOYMENT_RATE_IMPROVEMENT",
              achievementDate: "2026-03-02",
              achievementStatus: "DRAFT",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        }),
      );
    vi.stubGlobal("fetch", fetch);

    render(<EmploymentRateImprovementsPage />);
    await screen.findByText("등록된 실적이 없습니다");
    fireEvent.change(
      screen.getByTestId("employment-rate-management-item-input"),
      {
        target: { value: "EMPLOYMENT_RATE_IMPROVEMENT" },
      },
    );
    fireEvent.change(
      screen.getByTestId("employment-rate-achievement-date-input"),
      {
        target: { value: "2026-03-02" },
      },
    );
    fireEvent.click(screen.getByTestId("employment-rate-save-button"));

    await screen.findByText("취업률 제고 실적을 저장했습니다.");
    expect(fetch).toHaveBeenNthCalledWith(
      2,
      "/api/business/employment-rate-improvements",
      expect.objectContaining({ method: "POST" }),
    );
    await waitFor(() => expect(screen.getByText("ERI-83")).toBeInTheDocument());
  });

  it("surfaces an API forbidden response as a permission state", async () => {
    const fetch = vi.fn().mockResolvedValueOnce(response(null, 403));
    vi.stubGlobal("fetch", fetch);

    render(<EmploymentRateImprovementsPage />);

    await screen.findByText("취업률 제고 실적 권한이 없습니다");
  });
});

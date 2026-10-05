import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { EmploymentRateImprovementsPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENTS";

vi.mock("../../api/apiClient", () => ({ apiRequest: vi.fn() }));

describe("SCR-EMPLOYMENT-RATE-IMPROVEMENTS", () => {
  beforeEach(() => vi.mocked(apiRequest).mockReset());
  it("renders API-provided employment-rate improvement rows", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: {
        achievements: [
          {
            achievementId: 82,
            managementNo: "B83-ERI-001",
            managementItemCode: "EMPLOYMENT_RATE_IMPROVEMENT",
            achievementDate: "2026-04-10",
            achievementStatus: "DRAFT",
          },
        ],
        page: 0,
        pageSize: 20,
        totalElements: 1,
      },
      meta: {},
    });
    render(<EmploymentRateImprovementsPage />);
    expect(await screen.findByText("B83-ERI-001")).toBeInTheDocument();
  });
});

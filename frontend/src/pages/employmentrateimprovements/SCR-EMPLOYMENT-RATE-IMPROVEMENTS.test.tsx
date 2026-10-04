import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { EmploymentRateImprovementsPage } from "./SCR-EMPLOYMENT-RATE-IMPROVEMENTS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const row = {
  achievementId: 501,
  teacherLoginId: "professor1",
  organizationCode: "KNUE-DEPT-COMP",
  evaluationYear: "2026",
  managementItemCode: "EMPLOYMENT_RATE_IMPROVEMENT",
  achievementDate: "2026-04-10",
  achievementStatus: "DRAFT",
  specialLectureStartDate: "2026-04-01",
  specialLectureEndDate: "2026-04-10",
  mockExamQuestionPeriod: "2026-03-15~2026-03-20",
  attachmentIds: ["B83-ATTACHMENT-001"],
};

describe("SCR-EMPLOYMENT-RATE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("renders the approved screen and requests the 20/50/100 paging contract", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
      meta: {},
    });

    render(<EmploymentRateImprovementsPage />);

    await screen.findByText("professor1");
    expect(
      screen.getByTestId("employment-rate-improvements-page"),
    ).toHaveAttribute("data-screen-id", "SCR-EMPLOYMENT-RATE-IMPROVEMENTS");
    fireEvent.change(
      screen.getByTestId("employment-rate-improvements-page-size-select"),
      { target: { value: "100" } },
    );
    await waitFor(() =>
      expect(vi.mocked(apiRequest)).toHaveBeenLastCalledWith(
        "/api/business/employment-rate-improvements?page=0&pageSize=100",
      ),
    );
  });

  it("loads a selected detail, saves its real id path, and refreshes the list", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      })
      .mockResolvedValueOnce({ success: true, data: row, meta: {} })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievement: {
            ...row,
            mockExamQuestionPeriod: "2026-03-21~2026-03-25",
          },
          occurredDateWarning: false,
        },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      });

    render(<EmploymentRateImprovementsPage />);

    await screen.findByText("professor1");
    fireEvent.click(
      screen.getByTestId("employment-rate-improvements-detail-button"),
    );
    await screen.findByDisplayValue("2026-03-15~2026-03-20");
    fireEvent.click(
      screen.getByTestId("employment-rate-improvements-save-button"),
    );

    await screen.findByText("저장되었습니다.");
    expect(vi.mocked(apiRequest)).toHaveBeenCalledWith(
      "/api/business/employment-rate-improvements/501",
      expect.objectContaining({ method: "PUT" }),
    );
    await waitFor(() => expect(vi.mocked(apiRequest)).toHaveBeenCalledTimes(4));
  });

  it("disables mutation controls after selecting an evaluation-confirmed row", async () => {
    const confirmedRow = { ...row, achievementStatus: "EVALUATION_CONFIRMED" };
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

    await screen.findByText("professor1");
    fireEvent.click(
      screen.getByTestId("employment-rate-improvements-detail-button"),
    );
    await screen.findByTestId(
      "employment-rate-improvements-confirmed-lock-message",
    );
    expect(
      screen.getByTestId("employment-rate-improvements-save-button"),
    ).toBeDisabled();
  });
});

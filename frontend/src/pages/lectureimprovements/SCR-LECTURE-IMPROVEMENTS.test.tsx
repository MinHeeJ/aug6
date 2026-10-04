import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { LectureImprovementsPage } from "./SCR-LECTURE-IMPROVEMENTS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const row = {
  achievementId: 31,
  managementNo: "B83-LI-001",
  teacherName: "교원",
  managementItemCode: "LECTURE_IMPROVEMENT",
  achievementDate: "2026-03-03",
  achievementStatus: "DRAFT",
  achievementContent: "강의 개선 실적내용",
  academicYear: 2026,
  semester: 1,
  attachmentRef: null,
};

describe("SCR-LECTURE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("loads a selected detail and sends the approved create payload from the new form", async () => {
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

    render(<LectureImprovementsPage />);

    await screen.findByText("B83-LI-001");
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-button"));
    await screen.findByDisplayValue("강의 개선 실적내용");
    fireEvent.click(screen.getByTestId("lecture-improvements-new-button"));
    fireEvent.change(
      screen.getByTestId("lecture-improvements-management-item-input"),
      {
        target: { value: "LECTURE_IMPROVEMENT" },
      },
    );
    fireEvent.change(screen.getByTestId("lecture-improvements-date-input"), {
      target: { value: "2026-04-01" },
    });
    fireEvent.change(
      screen.getByTestId("lecture-improvements-academic-year-input"),
      {
        target: { value: "2026" },
      },
    );
    fireEvent.change(screen.getByTestId("lecture-improvements-content-input"), {
      target: { value: "신규 강의개선 내용" },
    });
    fireEvent.click(screen.getByTestId("lecture-improvements-save-button"));

    await screen.findByText("저장되었습니다.");
    expect(apiRequest).toHaveBeenNthCalledWith(
      3,
      "/api/business/lecture-improvements",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          managementItemCode: "LECTURE_IMPROVEMENT",
          achievementDate: "2026-04-01",
          achievementContent: "신규 강의개선 내용",
          academicYear: 2026,
          semester: 1,
          attachmentIds: [],
        }),
      }),
    );
  });

  it("disables editing for a selected evaluation-confirmed record", async () => {
    const finalized = { ...row, achievementStatus: "EVALUATION_CONFIRMED" };
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [finalized],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })
      .mockResolvedValueOnce({ success: true, data: finalized, meta: {} });

    render(<LectureImprovementsPage />);

    await screen.findByText("B83-LI-001");
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-button"));

    expect(
      await screen.findByTestId("lecture-improvements-finalized-message"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("lecture-improvements-save-button"),
    ).toBeDisabled();
  });
});

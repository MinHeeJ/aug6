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

const confirmedRow = {
  achievementId: 83,
  managementItemCode: "LECTURE_IMPROVEMENT",
  achievementDate: "2025-11-01",
  achievementContent: "평가확정 상태의 강의개선 내역",
  academicYear: 2025,
  semester: 1 as const,
  achievementStatus: "EVALUATION_CONFIRMED",
  attachmentIds: "[]",
};

describe("SCR-LECTURE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("loads a selected confirmed row and prevents its mutation", async () => {
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

    render(<LectureImprovementsPage />);

    await screen.findByText("EVALUATION_CONFIRMED");
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-83"));

    expect(
      await screen.findByTestId("lecture-improvements-confirmed-lock"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("lecture-improvements-save-button"),
    ).toBeDisabled();
    expect(
      screen.getByTestId("lecture-improvements-content-input"),
    ).toBeDisabled();
  });

  it("blocks missing required fields before calling the save operation", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: {},
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<LectureImprovementsPage />);

    await screen.findByText("조회된 강의개선 실적이 없습니다");
    fireEvent.click(screen.getByTestId("lecture-improvements-save-button"));

    expect(
      await screen.findByText("필수 입력 항목을 확인하세요."),
    ).toBeInTheDocument();
    expect(vi.mocked(apiRequest)).toHaveBeenCalledTimes(1);
  });
});

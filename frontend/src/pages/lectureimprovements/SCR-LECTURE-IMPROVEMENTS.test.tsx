import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
  achievementId: 65,
  managementNo: "B83-LI-003",
  teacherName: "교원",
  managementItemCode: "LECTURE_IMPROVEMENT",
  achievementDate: "2025-12-31",
  achievementStatus: "EVALUATION_CONFIRMED",
  achievementContent: "강의 품질 개선 활동",
  academicYear: 2025,
  semester: 2,
  attachmentIds: "[]",
};

describe("SCR-LECTURE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.stubGlobal(
      "confirm",
      vi.fn(() => true),
    );
  });

  it("loads a selected detail and prevents confirmed records from being edited", async () => {
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

    await screen.findByText("B83-LI-003");
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-65"));

    expect(
      await screen.findByTestId("lecture-improvements-confirmed-message"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("lecture-improvements-save-button"),
    ).toBeDisabled();
    expect(
      screen.getByTestId("lecture-improvements-academic-year-input"),
    ).toHaveValue(2025);
    expect(
      screen.getByTestId("lecture-improvements-semester-select"),
    ).toHaveValue("2");
  });

  it("blocks an incomplete form before a create API request", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: {},
    });

    render(<LectureImprovementsPage />);

    await screen.findByText("조회된 강의개선 실적이 없습니다");
    fireEvent.click(screen.getByTestId("lecture-improvements-save-button"));

    await waitFor(() => {
      expect(
        screen.getByText("필수 입력 항목을 확인하세요."),
      ).toBeInTheDocument();
    });
    expect(apiRequest).toHaveBeenCalledTimes(1);
  });
});

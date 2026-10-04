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
  managementNo: "B83-LI-001",
  teacherName: "교원",
  managementItemCode: "LECTURE_IMPROVEMENT",
  achievementDate: "2025-09-01",
  achievementContent: "개선 내용",
  academicYear: 2025,
  semester: 2,
  attachmentIds: [],
  achievementStatus: "EVALUATION_CONFIRMED",
};

describe("SCR-LECTURE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("loads the list and prevents saving an evaluation-confirmed detail row", async () => {
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

    await screen.findByText("B83-LI-001");
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-83"));

    expect(
      await screen.findByText("평가확정 실적은 수정할 수 없습니다."),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("lecture-improvements-save-button"),
    ).toBeDisabled();
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/lecture-improvements/83",
    );
  });
});

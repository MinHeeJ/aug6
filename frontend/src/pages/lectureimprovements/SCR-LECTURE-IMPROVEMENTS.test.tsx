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
  achievementId: 301,
  managementNo: "B83-LI-002",
  teacherName: "교원",
  managementItemCode: "LECTURE_IMPROVEMENT",
  achievementDate: "2025-12-31",
  achievementStatus: "EVALUATION_CONFIRMED",
  achievementContent: "강의 개선 실적",
  academicYear: 2025,
  semester: 2,
  attachmentIds: ["opaque-file-ref"],
};

describe("SCR-LECTURE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("loads a 2025 semester row and prevents an evaluation-confirmed row from being saved", async () => {
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

    await screen.findByText("B83-LI-002");
    expect(screen.getByText("2025 / 2")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-button"));

    expect(
      await screen.findByTestId("lecture-improvements-lock-message"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("lecture-improvements-save-button"),
    ).toBeDisabled();
  });
});

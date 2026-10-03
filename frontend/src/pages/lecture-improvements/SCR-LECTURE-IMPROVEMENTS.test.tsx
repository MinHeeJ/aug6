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

describe("SCR-LECTURE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("loads a list row and displays the selected academic year and semester", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [
            {
              achievementId: 91,
              teacherName: "교원",
              managementItemCode: "LECTURE_IMPROVEMENT",
              achievementDate: "2025-05-01",
              achievementContent: "수업 개선 보고서 작성",
              academicYear: 2025,
              semester: 2,
              achievementStatus: "DRAFT",
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
          achievementId: 91,
          teacherName: "교원",
          managementItemCode: "LECTURE_IMPROVEMENT",
          achievementDate: "2025-05-01",
          achievementContent: "수업 개선 보고서 작성",
          academicYear: 2025,
          semester: 2,
          achievementStatus: "DRAFT",
          attachmentIds: [],
        },
        meta: {},
      });

    render(<LectureImprovementsPage />);

    await screen.findByText("수업 개선 보고서 작성");
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-button"));

    expect(await screen.findByDisplayValue("2025")).toBeInTheDocument();
    expect(screen.getByDisplayValue("2")).toBeInTheDocument();
  });

  it("shows an empty state when no lecture improvements are returned", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: {},
    });

    render(<LectureImprovementsPage />);

    expect(
      await screen.findByText("조회된 실적이 없습니다"),
    ).toBeInTheDocument();
  });

  it("blocks an incomplete save with field-level validation without an API request", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: {},
    });

    render(<LectureImprovementsPage />);
    await screen.findByText("조회된 실적이 없습니다");
    fireEvent.click(screen.getByTestId("lecture-improvements-save-button"));

    expect(screen.getByText("학기를 선택하세요.")).toBeInTheDocument();
    expect(vi.mocked(apiRequest)).toHaveBeenCalledTimes(1);
  });
});

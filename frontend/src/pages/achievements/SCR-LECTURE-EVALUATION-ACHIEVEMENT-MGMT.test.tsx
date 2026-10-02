import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LectureEvaluationAchievementManagementPage } from "./SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT";
import { apiRequest } from "../../api/apiClient";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const row = {
  achievementId: 81,
  managementNo: "B77-LE-001",
  teacherName: "교원",
  managementItemCode: "LECTURE_EVALUATION",
  occurredDate: "2026-04-10",
  achievementDetail: '{"score":95}',
  certificationStatus: "DRAFT",
  attachmentRef: null,
};

describe("SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("renders the approved route screen with filtered API data and 20/50/100 page sizes", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
      meta: {},
    });

    render(<LectureEvaluationAchievementManagementPage />);

    await screen.findByText("B77-LE-001");
    expect(
      screen.getByTestId("lecture-evaluation-achievement-page"),
    ).toHaveAttribute(
      "data-screen-id",
      "SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT",
    );
    expect(screen.getByText("강의평가 실적 관리")).toBeInTheDocument();
    expect(
      screen.getByTestId("lecture-evaluation-page-size-select"),
    ).toHaveValue("20");
    fireEvent.change(
      screen.getByTestId("lecture-evaluation-page-size-select"),
      {
        target: { value: "100" },
      },
    );
    await waitFor(() =>
      expect(vi.mocked(apiRequest)).toHaveBeenLastCalledWith(
        "/api/business/lecture-evaluation-achievements?page=0&pageSize=100",
      ),
    );
  });

  it("saves the selected dynamic detail and refreshes the list after success", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievement: row,
          occurredDateWarning: true,
          warningMessage: "발생일 경고와 함께 저장되었습니다.",
        },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      });

    render(<LectureEvaluationAchievementManagementPage />);
    await screen.findByText("B77-LE-001");
    fireEvent.click(screen.getByTestId("lecture-evaluation-detail-button"));
    fireEvent.click(screen.getByTestId("lecture-evaluation-save-button"));

    await screen.findByText("발생일 경고와 함께 저장되었습니다.");
    expect(vi.mocked(apiRequest)).toHaveBeenCalledWith(
      "/api/business/lecture-evaluation-achievements",
      expect.objectContaining({ method: "POST" }),
    );
    await waitFor(() => expect(vi.mocked(apiRequest)).toHaveBeenCalledTimes(3));
  });

  it("locks the save action when a confirmed achievement is selected", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: {
        achievements: [{ ...row, certificationStatus: "EVALUATION_CONFIRMED" }],
        page: 0,
        pageSize: 20,
        totalElements: 1,
      },
      meta: {},
    });

    render(<LectureEvaluationAchievementManagementPage />);

    await screen.findByText("B77-LE-001");
    fireEvent.click(screen.getByTestId("lecture-evaluation-detail-button"));

    expect(
      screen.getByTestId("lecture-evaluation-confirmed-lock-message"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("lecture-evaluation-save-button")).toBeDisabled();
  });
});

import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { CourseOperationsPage } from "./SCR-COURSE-OPERATIONS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const draftAchievement = {
  achievementId: 901,
  teacherName: "교원",
  organizationCode: "KNUE-DEPT-COMP",
  evaluationYear: "2026",
  managementItemCode: "COURSE_OPERATION",
  achievementDate: "2026-04-11",
  achievementName: "현장실습 강좌 운영",
  performanceDetails: "현장실습 강좌 운영",
  attachmentIds: [],
  achievementStatus: "DRAFT",
};

const confirmedAchievement = {
  achievementId: 83,
  teacherName: "교원",
  organizationCode: "KNUE-DEPT-COMP",
  evaluationYear: "2026",
  managementItemCode: "COURSE_OPERATION",
  achievementDate: "2026-05-02",
  achievementName: "확정 강좌 운영",
  performanceDetails: "확정 내역",
  attachmentIds: [],
  achievementStatus: "EVALUATION_CONFIRMED",
};

describe("SCR-COURSE-OPERATIONS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.restoreAllMocks();
  });

  it("sends the approved request shape and lets the server derive the persisted display name", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievement: draftAchievement,
          achievementDateWarning: false,
        },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
        meta: {},
      });

    render(<CourseOperationsPage />);
    await screen.findByText("조회된 강좌 개설·운영 실적이 없습니다");
    fireEvent.change(
      screen.getByTestId("course-operations-management-item-input"),
      { target: { value: "COURSE_OPERATION" } },
    );
    fireEvent.change(screen.getByTestId("course-operations-date-input"), {
      target: { value: "2026-04-11" },
    });
    fireEvent.change(
      screen.getByTestId("course-operations-performance-details-input"),
      { target: { value: "현장실습 강좌 운영" } },
    );
    fireEvent.click(screen.getByTestId("course-operations-save-button"));

    await screen.findByText("저장되었습니다.");
    expect(vi.mocked(apiRequest)).toHaveBeenNthCalledWith(
      2,
      "/api/business/course-operations",
      {
        method: "POST",
        body: JSON.stringify({
          managementItemCode: "COURSE_OPERATION",
          achievementDate: "2026-04-11",
          performanceDetails: "현장실습 강좌 운영",
          attachmentIds: [],
        }),
      },
    );
  });

  it("loads the course-operation list and disables mutation for a confirmed record", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [confirmedAchievement],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: confirmedAchievement,
        meta: {},
      });

    render(<CourseOperationsPage />);
    await screen.findByText("COURSE_OPERATION");
    fireEvent.click(screen.getByTestId("course-operations-detail-button"));
    expect(
      await screen.findByTestId("course-operations-confirmed-lock-message"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("course-operations-save-button")).toBeDisabled();
  });
});

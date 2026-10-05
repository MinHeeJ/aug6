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

const confirmedRow = {
  achievementId: 71,
  managementNo: "B83-CO-003",
  teacherName: "교원",
  managementItemCode: "COURSE_OPERATION",
  achievementDate: "2026-04-13",
  achievementName: "인증 상태 강좌 운영 실적",
  performanceDetails: "인증 상태 강좌 운영 실적내역",
  achievementStatus: "EVALUATION_CONFIRMED",
  attachmentIds: "[]",
};

describe("SCR-COURSE-OPERATIONS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("loads the list and disables saving for a selected non-draft achievement", async () => {
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

    render(<CourseOperationsPage />);

    await screen.findByText("B83-CO-003");
    fireEvent.click(screen.getByTestId("course-operations-detail-button"));

    expect(
      await screen.findByTestId("course-operations-status-lock-message"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("course-operations-save-button")).toBeDisabled();
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/course-operations/71",
    );
  });
});

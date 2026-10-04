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
  achievementId: 83,
  managementNo: "B83-CO-001",
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2026",
  managementItemCode: "COURSE_OPERATION",
  achievementDate: "2026-04-10",
  performanceDetails: "강좌 운영 실적",
  attachmentIds: "[]",
  achievementStatus: "EVALUATION_CONFIRMED",
};

describe("SCR-COURSE-OPERATIONS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("loads API-backed list data and locks a confirmed detail row", async () => {
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

    await screen.findByText("B83-CO-001");
    fireEvent.click(screen.getByTestId("course-operations-detail-button"));

    expect(
      await screen.findByTestId("course-operations-confirmed-lock-message"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("course-operations-save-button")).toBeDisabled();
  });

  it("renders an API permission failure as the required permission state", async () => {
    const { ApiClientError } = await import("../../api/apiClient");
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "권한이 없습니다."),
    );

    render(<CourseOperationsPage />);

    expect(
      await screen.findByText("강좌 개설·운영 실적 관리 권한이 없습니다"),
    ).toBeInTheDocument();
  });
});

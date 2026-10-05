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

const draftRow = {
  achievementId: 82,
  managementNo: "CO-001",
  teacherName: "교원",
  managementItemCode: "COURSE",
  achievementDate: "2026-04-10",
  performanceDetails: "신규 강좌 운영",
  achievementStatus: "DRAFT",
  attachmentRefs: '["file-1"]',
};

describe("SCR-COURSE-OPERATIONS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.stubGlobal(
      "confirm",
      vi.fn(() => true),
    );
  });

  it("loads a row, retrieves its detail, and saves the selected row using PUT", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [draftRow],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })
      .mockResolvedValueOnce({ success: true, data: draftRow, meta: {} })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievement: { ...draftRow, performanceDetails: "수정 강좌 운영" },
          occurredDateWarning: false,
        },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [draftRow],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      });

    render(<CourseOperationsPage />);

    await screen.findByText("CO-001");
    fireEvent.click(screen.getByTestId("course-operations-detail-button"));
    await screen.findByDisplayValue("신규 강좌 운영");
    fireEvent.change(
      screen.getByTestId("course-operations-performance-details-input"),
      { target: { value: "수정 강좌 운영" } },
    );
    fireEvent.click(screen.getByTestId("course-operations-save-button"));

    await screen.findByText("처리 완료");
    expect(apiRequest).toHaveBeenNthCalledWith(
      2,
      "/api/business/course-operations/82",
    );
    expect(apiRequest).toHaveBeenNthCalledWith(
      3,
      "/api/business/course-operations/82",
      expect.objectContaining({ method: "PUT" }),
    );
  });

  it("prevents a selected evaluation-confirmed row from being edited", async () => {
    const confirmed = {
      ...draftRow,
      achievementStatus: "EVALUATION_CONFIRMED",
    };
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [confirmed],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })
      .mockResolvedValueOnce({ success: true, data: confirmed, meta: {} });

    render(<CourseOperationsPage />);

    await screen.findByText("CO-001");
    fireEvent.click(screen.getByTestId("course-operations-detail-button"));
    expect(
      await screen.findByTestId("course-operations-lock-message"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("course-operations-save-button")).toBeDisabled();
    expect(
      screen.getByTestId("course-operations-performance-details-input"),
    ).toBeDisabled();
  });
});

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { CourseOperationsPage } from "./SCR-COURSE-OPERATIONS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const row = {
  achievementId: 83,
  managementNo: "B83-CO-001",
  managementItemCode: "COURSE_OPERATION",
  achievementDate: "2026-04-10",
  performanceDetails: "강좌 운영 실적",
  achievementStatus: "DRAFT",
};

describe("SCR-COURSE-OPERATIONS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("renders API-backed records and requests the selected allowed page size", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
      meta: {},
    });

    render(<CourseOperationsPage />);

    await screen.findByText("B83-CO-001");
    expect(screen.getByTestId("course-operations-page")).toHaveAttribute(
      "data-screen-id",
      "SCR-COURSE-OPERATIONS",
    );
    fireEvent.change(screen.getByTestId("course-operations-page-size-select"), {
      target: { value: "100" },
    });
    await waitFor(() =>
      expect(vi.mocked(apiRequest)).toHaveBeenLastCalledWith(
        "/api/business/course-operations?page=0&pageSize=100",
      ),
    );
  });

  it("loads the selected detail, saves with PUT, and refreshes the list", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      })
      .mockResolvedValueOnce({ success: true, data: row, meta: {} })
      .mockResolvedValueOnce({
        success: true,
        data: { ...row, performanceDetails: "수정된 강좌 운영 실적" },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
        meta: {},
      });

    render(<CourseOperationsPage />);

    await screen.findByText("B83-CO-001");
    fireEvent.click(screen.getByTestId("course-operations-detail-button"));
    await screen.findByDisplayValue("강좌 운영 실적");
    fireEvent.change(
      screen.getByTestId("course-operations-performance-details-input"),
      { target: { value: "수정된 강좌 운영 실적" } },
    );
    fireEvent.click(screen.getByTestId("course-operations-save-button"));

    await screen.findByText("강좌 개설·운영 실적이 저장되었습니다.");
    expect(vi.mocked(apiRequest)).toHaveBeenCalledWith(
      "/api/business/course-operations/83",
      expect.objectContaining({ method: "PUT" }),
    );
    await waitFor(() => expect(vi.mocked(apiRequest)).toHaveBeenCalledTimes(4));
  });

  it("disables saving when the selected record is evaluation confirmed", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [{ ...row, achievementStatus: "EVALUATION_CONFIRMED" }],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: { ...row, achievementStatus: "EVALUATION_CONFIRMED" },
        meta: {},
      });

    render(<CourseOperationsPage />);

    await screen.findByText("B83-CO-001");
    fireEvent.click(screen.getByTestId("course-operations-detail-button"));
    await screen.findByTestId("course-operations-confirmed-lock-message");
    expect(screen.getByTestId("course-operations-save-button")).toBeDisabled();
  });
});

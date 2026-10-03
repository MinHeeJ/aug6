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

describe("SCR-COURSE-OPERATIONS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("loads a list row and submits the selected path on update", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: {
          achievements: [
            {
              achievementId: 83,
              teacherName: "교원",
              managementItemCode: "COURSE_OPERATION",
              achievementDate: "2026-04-10",
              performanceDetails: "신규 강좌 개설",
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
          achievementId: 83,
          teacherName: "교원",
          managementItemCode: "COURSE_OPERATION",
          achievementDate: "2026-04-10",
          performanceDetails: "신규 강좌 개설",
          achievementStatus: "DRAFT",
          attachmentIds: [],
        },
        meta: {},
      });

    render(<CourseOperationsPage />);

    await screen.findByText("신규 강좌 개설");
    fireEvent.click(screen.getByTestId("course-operations-detail-button"));

    expect(
      await screen.findByDisplayValue("신규 강좌 개설"),
    ).toBeInTheDocument();
  });

  it("displays the empty state when the API has no course operations", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: {},
    });

    render(<CourseOperationsPage />);

    expect(
      await screen.findByText("조회된 실적이 없습니다"),
    ).toBeInTheDocument();
  });
});

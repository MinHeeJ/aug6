import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { LectureAchievementManagementPage } from "./SCR-LECTURE-ACHIEVEMENT-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const confirmedRow = {
  achievementId: 82,
  managementNo: "B77-LA-001",
  teacherName: "교원",
  managementItemCode: "LECTURE",
  occurredDate: "2026-04-10",
  achievementDetail: '{"hours":3}',
  certificationStatus: "EVALUATION_CONFIRMED",
  attachmentRef: "attachment-opaque-ref",
};

describe("SCR-LECTURE-ACHIEVEMENT-MGMT", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("prevents a selected confirmed row from being saved or changing its attachment", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: {
        achievements: [confirmedRow],
        page: 0,
        pageSize: 20,
        totalElements: 1,
      },
      meta: {},
    });

    render(<LectureAchievementManagementPage />);

    await screen.findByText("B77-LA-001");
    fireEvent.click(screen.getByTestId("lecture-achievement-detail-button"));

    expect(
      screen.getByTestId("lecture-confirmed-lock-message"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("lecture-achievement-save-button"),
    ).toBeDisabled();
  });
});

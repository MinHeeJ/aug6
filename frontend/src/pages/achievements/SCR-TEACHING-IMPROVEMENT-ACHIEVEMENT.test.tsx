import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { TeachingImprovementAchievementPage } from "./SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const confirmedRow = {
  achievementId: 83,
  managementNo: "B83-LIA-003",
  teacherName: "교원",
  managementItemCode: "LECTURE",
  achievementDate: "2026-04-12",
  achievementContent: "학습성과 기반 개선",
  academicYear: 2025,
  semester: 1,
  certificationStatus: "EVALUATION_CONFIRMED",
};

describe("SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
  });

  it("loads a confirmed row and prevents its mutation", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce({
        success: true,
        data: { achievements: [confirmedRow] },
        meta: {},
      })
      .mockResolvedValueOnce({ success: true, data: confirmedRow, meta: {} });

    render(<TeachingImprovementAchievementPage />);

    await screen.findByText("B83-LIA-003");
    fireEvent.click(screen.getByTestId("teaching-improvement-detail-button"));

    expect(
      await screen.findByText("평가확정 실적은 수정할 수 없습니다."),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("teaching-improvement-save-button"),
    ).toBeDisabled();
  });
});

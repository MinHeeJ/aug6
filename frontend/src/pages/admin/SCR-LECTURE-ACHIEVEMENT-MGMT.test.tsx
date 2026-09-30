import { fireEvent, render, screen } from "@testing-library/react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";

const achievementApiMocks = vi.hoisted(() => ({
  list: vi.fn(),
  save: vi.fn(),
}));

vi.mock("../../api/apiClient", () => ({
  ApiClientError: class extends Error {
    readonly status = 0;
  },
  lectureAchievementApi: achievementApiMocks,
}));

import { LectureAchievementManagementPage } from "./SCR-LECTURE-ACHIEVEMENT-MGMT";

describe("SCR-LECTURE-ACHIEVEMENT-MGMT", () => {
  it("renders the lecture achievement entry and list contract", () => {
    const html = renderToStaticMarkup(<LectureAchievementManagementPage />);

    expect(html).toContain('data-testid="lecture-achievement-page"');
    expect(html).toContain("강의 실적 관리");
    expect(html).toContain('data-testid="lecture-management-item"');
    expect(html).toContain('data-testid="lecture-occurred-date"');
    expect(html).toContain('data-testid="lecture-detail"');
    expect(html).toContain('data-testid="lecture-save-button"');
    expect(html).toContain("인증상태");
    expect(html).toContain("첨부");
  });

  it("locks a selected evaluation-confirmed row before an edit request can be sent", async () => {
    achievementApiMocks.list.mockResolvedValueOnce({
      data: {
        items: [
          {
            achievementId: 91,
            managementNo: "B77-LA-CONFIRMED",
            managementItemCode: "LECTURE",
            occurredDate: "2026-04-10",
            certificationStatus: "EVALUATION_CONFIRMED",
            achievementDetail: { courseName: "교육평가론" },
            attachmentCount: 1,
          },
        ],
      },
    });

    render(<LectureAchievementManagementPage />);
    fireEvent.click(await screen.findByTestId("lecture-select-91"));

    expect(
      screen.getByTestId("lecture-confirmed-lock-message"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("lecture-save-button")).toBeDisabled();
    expect(achievementApiMocks.save).not.toHaveBeenCalled();
  });
});

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { renderToStaticMarkup } from "react-dom/server";
import { afterEach, describe, expect, it, vi } from "vitest";
import { lectureEvaluationAchievementApi } from "../../api/apiClient";
import { LectureEvaluationAchievementManagementPage } from "./SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    lectureEvaluationAchievementApi: { list: vi.fn(), save: vi.fn() },
  };
});

afterEach(() => vi.clearAllMocks());

describe("SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT", () => {
  it("renders the direct-route search, list, detail, state, page-size, and Excel contracts", () => {
    const html = renderToStaticMarkup(
      <LectureEvaluationAchievementManagementPage />,
    );
    expect(html).toContain(
      'data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"',
    );
    expect(html).toContain("업적 입력 관리 / 교육영역 / 강의평가 실적 관리");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain("Excel 다운로드");
    expect(html).toContain('data-testid="lecture-evaluation-save-button"');
  });

  it("locks a selected evaluation-confirmed row in the UI before a save or attachment mutation can be sent", async () => {
    vi.mocked(lectureEvaluationAchievementApi.list).mockResolvedValue({
      success: true,
      data: {
        achievements: [
          {
            achievementId: 77,
            managementNo: "LE-77",
            teacherName: "교원",
            managementItemCode: "EDU_LECTURE_EVALUATION",
            organizationCode: "KNUE-COL-EDU",
            occurredDate: "2026-03-15",
            achievementDetail: "평가확정 실적",
            certificationStatus: "EVALUATION_CONFIRMED",
            attachmentRef: "retained-attachment-ref",
          },
        ],
        page: 0,
        pageSize: 20,
        totalElements: 1,
      },
      error: undefined,
      meta: {},
    });

    render(<LectureEvaluationAchievementManagementPage />);
    await waitFor(() =>
      expect(lectureEvaluationAchievementApi.list).toHaveBeenCalled(),
    );
    fireEvent.click(await screen.findByTestId("lecture-evaluation-row"));

    expect(
      screen.getByTestId("lecture-evaluation-confirmed-lock-message"),
    ).toHaveTextContent("첨부파일을 삭제할 수 없습니다");
    expect(screen.getByTestId("lecture-evaluation-save-button")).toBeDisabled();
  });
});

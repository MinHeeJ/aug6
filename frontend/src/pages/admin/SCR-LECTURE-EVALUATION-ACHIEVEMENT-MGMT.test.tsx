import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { LectureEvaluationAchievementManagementPage } from "./SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    lectureEvaluationAchievementApi: {
      list: vi.fn(async () => ({
        success: true,
        data: { achievements: [], page: 0, size: 20, totalElements: 0 },
        meta: {},
      })),
      save: vi.fn(),
    },
  };
});

describe("SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT", () => {
  it("renders the authorized direct route screen with its required states and controls", () => {
    const html = renderToStaticMarkup(
      <LectureEvaluationAchievementManagementPage />,
    );

    expect(html).toContain(
      'data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"',
    );
    expect(html).toContain("강의평가 실적 관리");
    expect(html).toContain("관리번호");
    expect(html).toContain("성명");
    expect(html).toContain("업적발생일");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain('data-testid="lecture-evaluation-save-button"');
    expect(html).toContain('data-testid="lecture-evaluation-excel-button"');

    const source = LectureEvaluationAchievementManagementPage.toString();
    expect(source).toContain("권한이 없습니다");
    expect(source).toContain("조회된 강의평가 실적이 없습니다");
    expect(source).toContain("저장하시겠습니까");
  });

  it("uses the relative lecture-evaluation API client", async () => {
    const { lectureEvaluationAchievementApi } = await import(
      "../../api/apiClient"
    );

    expect(lectureEvaluationAchievementApi.list.toString()).not.toContain(
      "localhost",
    );
  });
});

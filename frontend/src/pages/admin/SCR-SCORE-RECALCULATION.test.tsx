import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { ScoreRecalculationPage } from "./SCR-SCORE-RECALCULATION";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    scoreRecalculationApi: {
      listScoreRecalculations: vi.fn(async () => ({
        success: true,
        data: {
          recalculations: [
            {
              evaluationMaterialId: 460001,
              evaluationYear: "2026",
              areaCode: "RESEARCH_CREATION",
              organizationCode: "KNUE-DEPT-COMP",
              targetUserId: 52,
              sourceAchievementId: 846001,
              materialStatus: "CERTIFIED",
              previousScore: 10,
              recalculatedScore: 12,
              formulaVersionId: 320001,
              generationNo: 2,
              recalculationBatchId: "B46-RECALC-0001",
              selectionReason: "기본 규정버전 재계산",
              excludedReason: null,
              calculatedAt: "2026-09-03T09:00:00",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      createScoreRecalculation: vi.fn(async () => ({
        success: true,
        data: {
          recalculationBatchId: "B46-RECALC-0001",
          evaluationYear: "2026",
          areaCode: "RESEARCH_CREATION",
          targetUserId: 52,
          formulaVersionId: "320001",
          totalCount: 1,
          successCount: 1,
          failureCount: 0,
          excludedCount: 0,
          requestId: "REQ-B46-RECALC-0001",
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-SCORE-RECALCULATION", () => {
  it("renders score recalculation condition, generation preservation, and comparison controls", () => {
    const html = renderToStaticMarkup(<ScoreRecalculationPage />);
    expect(html).toContain('data-screen-id="SCR-SCORE-RECALCULATION"');
    expect(html).toContain('data-testid="score-recalculation-page"');
    expect(html).toContain("점수 재계산");
    expect(html).toContain("재계산 조건 및 산식버전");
    expect(html).toContain("평가연도");
    expect(html).toContain("평가영역");
    expect(html).toContain("대상자 ID");
    expect(html).toContain("산식버전");
    expect(html).toContain("선택 사유");
    expect(html).toContain("재계산 대상 및 전후 비교");
    expect(html).toContain("이전점수");
    expect(html).toContain("재계산점수");
    expect(html).toContain("계산세대");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    const source = ScoreRecalculationPage.toString();
    expect(source).toContain("조건으로 점수를 재계산하시겠습니까");
    expect(source).toContain(
      "산식 정의와 원천 실적은 이 화면에서 수정하지 않습니다",
    );
    expect(source).toContain("점수 재계산 권한이 없습니다");
  });

  it("uses relative API client calls for search and recalculation", async () => {
    const { scoreRecalculationApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<ScoreRecalculationPage />);
    expect(
      scoreRecalculationApi.listScoreRecalculations,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const source = scoreRecalculationApi.createScoreRecalculation.toString();
    expect(source).not.toContain("http://localhost");
  });
});

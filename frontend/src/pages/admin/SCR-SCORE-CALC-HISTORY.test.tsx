import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { ScoreCalculationHistoryPage } from "./SCR-SCORE-CALC-HISTORY";

describe("SCR-SCORE-CALC-HISTORY", () => {
  it("renders target search, score rows, calculation basis steps, source achievement link, states, and Excel action", () => {
    const html = renderToStaticMarkup(<ScoreCalculationHistoryPage />);
    expect(html).toContain('data-screen-id="SCR-SCORE-CALC-HISTORY"');
    expect(html).toContain('data-testid="score-calculation-history-page"');
    expect(html).toContain("점수 산출 이력");
    expect(html).toContain("대상자");
    expect(html).toContain("평가연도");
    expect(html).toContain("평가영역");
    expect(html).toContain("대상자별 평가점수 목록");
    expect(html).toContain("선택 점수 산출근거");
    expect(html).toContain("원천 실적");
    expect(html).toContain("관리항목");
    expect(html).toContain("기준점수");
    expect(html).toContain("참여구분");
    expect(html).toContain("배분율");
    expect(html).toContain("산식");
    expect(html).toContain("산출점수");
    expect(html).toContain("원천 업적 상세로 이동");
    expect(html).toContain("Excel");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });

  it("uses only relative read APIs for list, detail, and Excel download", async () => {
    const { scoreCalculationHistoryApi } = await import("../../api/apiClient");
    expect(
      scoreCalculationHistoryApi.listScoreCalculationHistories.toString(),
    ).toContain("/api/business/score-calculation-histories");
    expect(
      scoreCalculationHistoryApi.getScoreCalculationHistoryDetail.toString(),
    ).toContain("/api/business/score-calculation-histories/");
    expect(
      scoreCalculationHistoryApi.downloadScoreCalculationHistoriesExcel.toString(),
    ).toContain("/api/business/score-calculation-histories/download");
    expect(
      scoreCalculationHistoryApi.listScoreCalculationHistories.toString(),
    ).not.toContain("http://");
  });
});

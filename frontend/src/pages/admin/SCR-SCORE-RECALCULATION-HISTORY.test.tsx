import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { ScoreRecalculationHistoryPage } from "./SCR-SCORE-RECALCULATION-HISTORY";

describe("SCR-SCORE-RECALCULATION-HISTORY", () => {
  it("renders work period filters, job list, detail comparison, no execute CTA, permission state, and Excel action", () => {
    const html = renderToStaticMarkup(<ScoreRecalculationHistoryPage />);
    expect(html).toContain('data-screen-id="SCR-SCORE-RECALCULATION-HISTORY"');
    expect(html).toContain('data-testid="score-recalculation-history-page"');
    expect(html).toContain("재계산 이력");
    expect(html).toContain("평가연도");
    expect(html).toContain("작업 시작일");
    expect(html).toContain("작업 종료일");
    expect(html).toContain("재계산 작업 목록");
    expect(html).toContain("작업ID");
    expect(html).toContain("산식버전");
    expect(html).toContain("대상범위");
    expect(html).toContain("변경건수");
    expect(html).toContain("전총점");
    expect(html).toContain("후총점");
    expect(html).toContain("대상자별 주요 변경내역");
    expect(html).toContain("사용기준 상세");
    expect(html).toContain("Excel");
    expect(html).toContain("20건");
    expect(html).toContain("재계산 실행 기능은 제공하지 않습니다");
    expect(html).not.toContain(
      'data-testid="score-recalculation-execute-button"',
    );
  });

  it("uses only relative read APIs for list, detail, and Excel download", async () => {
    const { scoreRecalculationHistoryApi } = await import(
      "../../api/apiClient"
    );
    expect(
      scoreRecalculationHistoryApi.listScoreRecalculationHistories.toString(),
    ).toContain("/api/business/score-recalculation-histories");
    expect(
      scoreRecalculationHistoryApi.getScoreRecalculationHistoryDetail.toString(),
    ).toContain("/api/business/score-recalculation-histories/");
    expect(
      scoreRecalculationHistoryApi.downloadScoreRecalculationHistoriesExcel.toString(),
    ).toContain("/api/business/score-recalculation-histories/download");
    expect(
      scoreRecalculationHistoryApi.listScoreRecalculationHistories.toString(),
    ).not.toContain("http://");
  });
});

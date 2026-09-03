import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { ScoreAdjustmentHistoryPage } from "./SCR-SCORE-ADJUSTMENT-HISTORY";

describe("SCR-SCORE-ADJUSTMENT-HISTORY", () => {
  it("renders search filters, before-after list, detail approval trace, permission state, and Excel action", () => {
    const html = renderToStaticMarkup(<ScoreAdjustmentHistoryPage />);
    expect(html).toContain('data-screen-id="SCR-SCORE-ADJUSTMENT-HISTORY"');
    expect(html).toContain('data-testid="score-adjustment-history-page"');
    expect(html).toContain("점수 조정 이력");
    expect(html).toContain("대상자");
    expect(html).toContain("평가연도");
    expect(html).toContain("평가영역");
    expect(html).toContain("조정대상");
    expect(html).toContain("점수·평가백분율 조정 목록");
    expect(html).toContain("전값");
    expect(html).toContain("후값");
    expect(html).toContain("사유");
    expect(html).toContain("조정자");
    expect(html).toContain("승인자");
    expect(html).toContain("비고 전문");
    expect(html).toContain("승인 경위");
    expect(html).toContain("Excel");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });

  it("uses only relative read APIs for list, detail, and Excel download", async () => {
    const { scoreAdjustmentHistoryApi } = await import("../../api/apiClient");
    expect(
      scoreAdjustmentHistoryApi.listScoreAdjustmentHistories.toString(),
    ).toContain("/api/business/score-adjustment-histories");
    expect(
      scoreAdjustmentHistoryApi.getScoreAdjustmentHistoryDetail.toString(),
    ).toContain("/api/business/score-adjustment-histories/");
    expect(
      scoreAdjustmentHistoryApi.downloadScoreAdjustmentHistoriesExcel.toString(),
    ).toContain("/api/business/score-adjustment-histories/download");
    expect(
      scoreAdjustmentHistoryApi.listScoreAdjustmentHistories.toString(),
    ).not.toContain("http://");
  });
});

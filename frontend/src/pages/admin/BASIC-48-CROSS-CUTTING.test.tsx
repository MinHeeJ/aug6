import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { EvaluationSnapshotHistoryPage } from "./SCR-EVAL-SNAPSHOT-HISTORY";
import { ScoreAdjustmentHistoryPage } from "./SCR-SCORE-ADJUSTMENT-HISTORY";
import { ScoreCalculationHistoryPage } from "./SCR-SCORE-CALC-HISTORY";
import { ScoreRecalculationHistoryPage } from "./SCR-SCORE-RECALCULATION-HISTORY";

const screens = [
  {
    name: "시점 데이터 관리",
    html: () => renderToStaticMarkup(<EvaluationSnapshotHistoryPage />),
    screenId: "SCR-EVAL-SNAPSHOT-HISTORY",
    testId: "evaluation-snapshot-page",
  },
  {
    name: "점수 산출 이력",
    html: () => renderToStaticMarkup(<ScoreCalculationHistoryPage />),
    screenId: "SCR-SCORE-CALC-HISTORY",
    testId: "score-calculation-history-page",
  },
  {
    name: "점수 조정 이력",
    html: () => renderToStaticMarkup(<ScoreAdjustmentHistoryPage />),
    screenId: "SCR-SCORE-ADJUSTMENT-HISTORY",
    testId: "score-adjustment-history-page",
  },
  {
    name: "재계산 이력",
    html: () => renderToStaticMarkup(<ScoreRecalculationHistoryPage />),
    screenId: "SCR-SCORE-RECALCULATION-HISTORY",
    testId: "score-recalculation-history-page",
  },
] as const;

describe("BASIC-48 cross-cutting UI verification", () => {
  it("renders all four screens with stable screen ids, labelled filters, page-size controls, states, and Excel actions", () => {
    for (const screen of screens) {
      const html = screen.html();
      expect(html, screen.name).toContain(
        `data-screen-id="${screen.screenId}"`,
      );
      expect(html, screen.name).toContain(`data-testid="${screen.testId}"`);
      expect(html, screen.name).toContain("검색조건");
      expect(html, screen.name).toContain("평가연도");
      expect(html, screen.name).toContain("조회");
      expect(html, screen.name).toContain("Excel");
      expect(html, screen.name).toContain("20건");
      expect(html, screen.name).toContain("50건");
      expect(html, screen.name).toContain("100건");
      expect(html, screen.name).toContain("불러오는 중입니다");
    }
  });

  it("does not expose score creation, score mutation, recalculation execution, or finalization controls on read-only screens", () => {
    for (const screen of screens) {
      const html = screen.html();
      expect(html, screen.name).not.toContain("저장");
      expect(html, screen.name).not.toContain("삭제");
      expect(html, screen.name).not.toContain("확정 처리");
      expect(html, screen.name).not.toContain("확정취소");
      expect(html, screen.name).not.toContain(
        'data-testid="score-recalculation-execute-button"',
      );
    }
  });

  it("keeps personal-data and secure-coding smoke boundaries in user-visible error states", () => {
    for (const screen of screens) {
      const html = screen.html();
      expect(html, screen.name).not.toContain("Exception");
      expect(html, screen.name).not.toContain("password");
      expect(html, screen.name).not.toContain("secret");
    }
  });
});

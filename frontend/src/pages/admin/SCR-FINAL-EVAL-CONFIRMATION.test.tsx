import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { FinalEvaluationConfirmationPage } from "./SCR-FINAL-EVAL-CONFIRMATION";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    finalEvaluationConfirmationApi: {
      listFinalEvaluationConfirmations: vi.fn(async () => ({
        success: true,
        data: {
          confirmations: [
            {
              targetUserId: 52,
              evaluationYear: "2026",
              finalScore: 12,
              latestRecalculationBatchId: "B46-BATCH-RECALC-001",
              latestRecalculationStatus: "SUCCESS",
              finalStatus: "CERTIFIED",
              confirmedBy: null,
              confirmedAt: null,
              canceledBy: null,
              canceledAt: null,
              cancelReason: null,
              snapshotRef: "B46-SNAPSHOT-PENDING-001",
              materialCount: 2,
              confirmedMaterialCount: 0,
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveFinalEvaluationConfirmationTransition:
        actual.finalEvaluationConfirmationApi
          .saveFinalEvaluationConfirmationTransition,
    },
  };
});

describe("SCR-FINAL-EVAL-CONFIRMATION", () => {
  it("renders final evaluation confirmation search, status table, and separated confirm/cancel controls", () => {
    const html = renderToStaticMarkup(<FinalEvaluationConfirmationPage />);
    expect(html).toContain('data-screen-id="SCR-FINAL-EVAL-CONFIRMATION"');
    expect(html).toContain('data-testid="final-evaluation-confirmation-page"');
    expect(html).toContain("평가 확정·취소");
    expect(html).toContain("최종평가 조회 조건");
    expect(html).toContain("평가연도");
    expect(html).toContain("대상자 ID");
    expect(html).toContain("확정상태");
    expect(html).toContain("대상자별 최종평가 상태");
    expect(html).toContain("최종점수");
    expect(html).toContain("최신재계산");
    expect(html).toContain("확정자/일시");
    expect(html).toContain("취소사유");
    expect(html).toContain("CONFIRM");
    expect(html).toContain("CANCEL");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    const source = FinalEvaluationConfirmationPage.toString();
    expect(source).toContain("최종평가를 확정하시겠습니까");
    expect(source).toContain("최종평가 확정을 취소하시겠습니까");
    expect(source).toContain(
      "점수와 산출근거는 이 화면에서 직접 수정하지 않고",
    );
  });

  it("uses relative API client calls and selected target id for transition", async () => {
    const { finalEvaluationConfirmationApi } = await import(
      "../../api/apiClient"
    );
    renderToStaticMarkup(<FinalEvaluationConfirmationPage />);
    expect(
      finalEvaluationConfirmationApi.listFinalEvaluationConfirmations,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("http") }),
    );
    const source =
      finalEvaluationConfirmationApi.saveFinalEvaluationConfirmationTransition.toString();
    expect(source).toContain("/api/business/final-evaluation-confirmations/");
    expect(source).not.toContain("http://");
  });
});

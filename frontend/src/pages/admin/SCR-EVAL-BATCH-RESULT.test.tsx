import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { BatchProcessingResultPage } from "./SCR-EVAL-BATCH-RESULT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    batchProcessingResultApi: {
      listBatchProcessingResults:
        actual.batchProcessingResultApi.listBatchProcessingResults,
      listBatchProcessingResultErrors:
        actual.batchProcessingResultApi.listBatchProcessingResultErrors,
    },
  };
});

describe("SCR-EVAL-BATCH-RESULT", () => {
  it("renders batch result search, total/success/failure/excluded counts, and error detail area", () => {
    const html = renderToStaticMarkup(<BatchProcessingResultPage />);
    expect(html).toContain('data-screen-id="SCR-EVAL-BATCH-RESULT"');
    expect(html).toContain('data-testid="batch-processing-result-page"');
    expect(html).toContain("처리 결과 조회");
    expect(html).toContain("작업유형");
    expect(html).toContain("배치ID");
    expect(html).toContain("대상조건");
    expect(html).toContain("총건수");
    expect(html).toContain("성공");
    expect(html).toContain("실패");
    expect(html).toContain("제외");
    expect(html).toContain("오류 상세");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });

  it("contains no execution or rerun CTA and uses only relative read APIs", async () => {
    renderToStaticMarkup(<BatchProcessingResultPage />);
    const pageSource = BatchProcessingResultPage.toString();
    expect(pageSource).not.toContain("실행");
    expect(pageSource).not.toContain("재실행");
    const { batchProcessingResultApi } = await import("../../api/apiClient");
    expect(
      batchProcessingResultApi.listBatchProcessingResults.toString(),
    ).toContain("/api/business/batch-processing-results");
    expect(
      batchProcessingResultApi.listBatchProcessingResultErrors.toString(),
    ).toContain("/api/business/batch-processing-results/");
    expect(
      batchProcessingResultApi.listBatchProcessingResults.toString(),
    ).not.toContain("http://");
  });
});

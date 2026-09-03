import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationSnapshotHistoryPage } from "./SCR-EVAL-SNAPSHOT-HISTORY";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationSnapshotApi: {
      listEvaluationSnapshots:
        actual.evaluationSnapshotApi.listEvaluationSnapshots,
      getEvaluationSnapshotDetail:
        actual.evaluationSnapshotApi.getEvaluationSnapshotDetail,
      downloadEvaluationSnapshotsExcel:
        actual.evaluationSnapshotApi.downloadEvaluationSnapshotsExcel,
    },
  };
});

describe("SCR-EVAL-SNAPSHOT-HISTORY", () => {
  it("renders search conditions, snapshot list, detail tabs, states, and Excel action", () => {
    const html = renderToStaticMarkup(<EvaluationSnapshotHistoryPage />);
    expect(html).toContain('data-screen-id="SCR-EVAL-SNAPSHOT-HISTORY"');
    expect(html).toContain('data-testid="evaluation-snapshot-page"');
    expect(html).toContain("시점 데이터 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("확정시점");
    expect(html).toContain("기준정보 snapshot");
    expect(html).toContain("평가자료 snapshot");
    expect(html).toContain("보존 결과 대조");
    expect(html).toContain("Excel");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });

  it("uses only relative read APIs for list, detail, and Excel download", async () => {
    const { evaluationSnapshotApi } = await import("../../api/apiClient");
    expect(evaluationSnapshotApi.listEvaluationSnapshots.toString()).toContain(
      "/api/business/evaluation-snapshots",
    );
    expect(
      evaluationSnapshotApi.getEvaluationSnapshotDetail.toString(),
    ).toContain("/api/business/evaluation-snapshots/");
    expect(
      evaluationSnapshotApi.downloadEvaluationSnapshotsExcel.toString(),
    ).toContain("/api/business/evaluation-snapshots/download");
    expect(
      evaluationSnapshotApi.listEvaluationSnapshots.toString(),
    ).not.toContain("http://");
  });
});

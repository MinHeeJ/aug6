import { describe, expect, it } from "vitest";
import {
  batchExecutionApi,
  createEmptyBatchExecutionState,
  getBatchExecutionRouteContract,
  reduceBatchExecutionState,
} from "./SCR-BATCH-EXECUTION-MGMT";

describe("SCR-BATCH-EXECUTION-MGMT route contract and state handling", () => {
  it("declares batch execution route and relative API operations", () => {
    expect(getBatchExecutionRouteContract()).toEqual({
      route: "/admin/batch-executions",
      screenId: "SCR-BATCH-EXECUTION-MGMT",
      operations: [
        "listBatchExecutions",
        "createBatchExecution",
        "updateBatchExecutionStatus",
        "createBatchRerun",
      ],
    });
    expect(
      batchExecutionApi.paths.list({
        batchId: "BATCH-EVAL",
        executionStatus: "RUNNING",
        page: 0,
        size: 20,
      }),
    ).toBe(
      "/api/admin/batch-executions?page=0&size=20&batchId=BATCH-EVAL&executionStatus=RUNNING",
    );
    expect(batchExecutionApi.paths.create()).toBe(
      "/api/admin/batch-executions",
    );
    expect(batchExecutionApi.paths.status("BEX-1")).toBe(
      "/api/admin/batch-executions/BEX-1/status",
    );
    expect(batchExecutionApi.paths.rerun("BEX-1")).toBe(
      "/api/admin/batch-executions/BEX-1/rerun",
    );
  });

  it("represents loading empty error permission progress and success states", () => {
    const loading = reduceBatchExecutionState(
      createEmptyBatchExecutionState(),
      {
        type: "loading",
      },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceBatchExecutionState(loading, {
      type: "loaded",
      executions: [],
    });
    expect(empty.status).toBe("empty");
    const progress = reduceBatchExecutionState(empty, {
      type: "progress",
      message: "진행 중",
    });
    expect(progress.status).toBe("progress");
    const error = reduceBatchExecutionState(progress, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceBatchExecutionState(error, { type: "permission" });
    expect(permission.status).toBe("permission");
    const success = reduceBatchExecutionState(permission, {
      type: "success",
      message: "배치 수동실행이 요청되었습니다.",
    });
    expect(success.status).toBe("success");
  });

  it("uses default 20 rows and exposes only 20/50/100 page size options plus Excel OQ", () => {
    expect(batchExecutionApi.paths.list()).toBe(
      "/api/admin/batch-executions?page=0&size=20",
    );
    expect([...batchExecutionApi.pageSizeOptions]).toEqual([20, 50, 100]);
    expect(batchExecutionApi.excelDownloadOq).toContain("REQ-386 OQ");
  });

  it("blocks run stop and rerun when required fields are missing with field-level messages", () => {
    const emptyForm = {
      batchId: "",
      executionId: "",
      parametersText: "{}",
      reason: "",
    };
    expect(batchExecutionApi.validateForm(emptyForm, "run")).toMatchObject({
      batchId: "배치ID는 필수입니다.",
      reason: "사유는 필수입니다.",
    });
    expect(batchExecutionApi.validateForm(emptyForm, "stop")).toMatchObject({
      executionId: "실행ID를 선택하세요.",
      reason: "사유는 필수입니다.",
    });
    expect(batchExecutionApi.validateForm(emptyForm, "rerun")).toMatchObject({
      executionId: "실행ID를 선택하세요.",
      reason: "사유는 필수입니다.",
    });
  });

  it("documents batch action confirmation, progress, and completion 안내 messages", () => {
    expect(batchExecutionApi.uiMessages.runConfirm("BATCH-A")).toBe(
      "BATCH-A 배치를 수동실행하시겠습니까?",
    );
    expect(batchExecutionApi.uiMessages.stopConfirm("EXEC-A")).toBe(
      "EXEC-A 실행을 중지하시겠습니까?",
    );
    expect(batchExecutionApi.uiMessages.rerunConfirm("EXEC-A")).toBe(
      "EXEC-A 실행을 재실행하시겠습니까?",
    );
    expect(batchExecutionApi.uiMessages.progress).toContain("10초 이상");
    expect(batchExecutionApi.uiMessages.runSuccess).toContain("요청되었습니다");
  });

  it("builds execution payload from selected domain data without hardcoded path ids", () => {
    const payload = batchExecutionApi.toPayload(
      {
        batchId: "BATCH-A",
        executionId: "BEX-ORIGINAL",
        parametersText: '{"year":2026}',
        reason: "운영자 수동실행",
      },
      true,
    );
    expect(payload).toEqual({
      batchId: "BATCH-A",
      parameters: { year: 2026 },
      reason: "운영자 수동실행",
    });
    const rerunPayload = batchExecutionApi.toPayload(
      {
        batchId: "BATCH-A",
        executionId: "BEX-ORIGINAL",
        parametersText: "{}",
        reason: "장애 조치 후 재실행",
      },
      false,
    );
    expect(rerunPayload).toEqual({
      parameters: {},
      reason: "장애 조치 후 재실행",
    });
  });
});

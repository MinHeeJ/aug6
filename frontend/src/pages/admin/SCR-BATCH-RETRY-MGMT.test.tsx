import { describe, expect, it } from "vitest";
import {
  batchRetryApi,
  createEmptyBatchRetryState,
  getBatchRetryRouteContract,
  reduceBatchRetryState,
} from "./SCR-BATCH-RETRY-MGMT";

describe("SCR-BATCH-RETRY-MGMT route contract and state handling", () => {
  it("declares retry route and relative API operations", () => {
    expect(getBatchRetryRouteContract()).toEqual({
      route: "/admin/batch-retries",
      screenId: "SCR-BATCH-RETRY-MGMT",
      operations: ["listBatchRetryTargets", "createBatchRetry"],
    });
    expect(
      batchRetryApi.paths.targets({
        originalExecutionId: "EXEC-FAILED-001",
        failedItemKey: "ITEM-FAIL-001",
        page: 0,
        size: 20,
      }),
    ).toBe(
      "/api/admin/batch-retries/targets?page=0&size=20&originalExecutionId=EXEC-FAILED-001&failedItemKey=ITEM-FAIL-001",
    );
    expect(batchRetryApi.paths.create()).toBe("/api/admin/batch-retries");
  });

  it("represents loading empty error permission and success states", () => {
    const loading = reduceBatchRetryState(createEmptyBatchRetryState(), {
      type: "loading",
    });
    expect(loading.status).toBe("loading");
    const empty = reduceBatchRetryState(loading, {
      type: "loaded",
      targets: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceBatchRetryState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceBatchRetryState(error, { type: "permission" });
    expect(permission.status).toBe("permission");
    const success = reduceBatchRetryState(permission, {
      type: "success",
      message: "재처리 요청이 등록되었습니다.",
    });
    expect(success.status).toBe("success");
  });

  it("uses default 20 rows and exposes only 20/50/100 page size options plus Excel OQ", () => {
    expect(batchRetryApi.paths.targets()).toBe(
      "/api/admin/batch-retries/targets?page=0&size=20",
    );
    expect([...batchRetryApi.pageSizeOptions]).toEqual([20, 50, 100]);
    expect(batchRetryApi.excelDownloadOq).toContain("REQ-386 OQ");
  });

  it("blocks retry when failed target or required reason is missing", () => {
    expect(batchRetryApi.validateRetry(null, "")).toMatchObject({
      target: "실패 대상을 선택하세요.",
      retryReason: "재처리 사유를 입력하세요.",
    });
  });

  it("documents retry confirmation progress and completion 안내 messages", () => {
    expect(batchRetryApi.uiMessages.retryConfirm("EXEC-FAILED-001")).toBe(
      "EXEC-FAILED-001 실패 대상을 재처리하시겠습니까?",
    );
    expect(batchRetryApi.uiMessages.progress).toContain("10초 이상");
    expect(batchRetryApi.uiMessages.retrySuccess).toBe(
      "재처리 요청이 등록되었습니다.",
    );
  });

  it("represents long-running retry progress state", () => {
    const progress = reduceBatchRetryState(createEmptyBatchRetryState(), {
      type: "progress",
      message: batchRetryApi.uiMessages.progress,
    });
    expect(progress.status).toBe("progress");
  });

  it("builds retry payload from selected failed target and reason without hardcoded ids", () => {
    const payload = batchRetryApi.toCreatePayload(
      {
        originalExecutionId: "EXEC-FAILED-001",
        batchId: "BATCH-A",
        executionStatus: "FAILED",
        failedItemKey: "ITEM-FAIL-001",
        failureReason: "필수값 누락",
      },
      " 원천자료 보완 후 재처리 ",
    );
    expect(payload).toEqual({
      originalExecutionId: "EXEC-FAILED-001",
      failedItemKey: "ITEM-FAIL-001",
      retryReason: "원천자료 보완 후 재처리",
    });
  });
});

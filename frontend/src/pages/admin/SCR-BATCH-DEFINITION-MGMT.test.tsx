import { describe, expect, it } from "vitest";
import {
  batchDefinitionApi,
  createEmptyBatchDefinitionState,
  getBatchDefinitionRouteContract,
  reduceBatchDefinitionState,
} from "./SCR-BATCH-DEFINITION-MGMT";

describe("SCR-BATCH-DEFINITION-MGMT route contract and state handling", () => {
  it("declares batch definition route and relative API operations", () => {
    expect(getBatchDefinitionRouteContract()).toEqual({
      route: "/admin/batch-definitions",
      screenId: "SCR-BATCH-DEFINITION-MGMT",
      operations: ["listBatchDefinitions", "saveBatchDefinition"],
    });
    expect(
      batchDefinitionApi.paths.list({
        batchId: "BATCH-EVAL",
        batchType: "EVALUATION_DATA",
        page: 0,
        size: 20,
      }),
    ).toBe(
      "/api/admin/batch-definitions?page=0&size=20&batchId=BATCH-EVAL&batchType=EVALUATION_DATA",
    );
    expect(batchDefinitionApi.paths.save()).toBe(
      "/api/admin/batch-definitions",
    );
  });

  it("represents loading empty error permission and success states", () => {
    const loading = reduceBatchDefinitionState(
      createEmptyBatchDefinitionState(),
      {
        type: "loading",
      },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceBatchDefinitionState(loading, {
      type: "loaded",
      definitions: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceBatchDefinitionState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceBatchDefinitionState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
    const success = reduceBatchDefinitionState(permission, {
      type: "success",
      message: "배치 정의가 저장되었습니다.",
    });
    expect(success.status).toBe("success");
  });

  it("uses default 20 rows and exposes only 20/50/100 page size options plus Excel OQ", () => {
    expect(batchDefinitionApi.paths.list()).toBe(
      "/api/admin/batch-definitions?page=0&size=20",
    );
    expect([...batchDefinitionApi.pageSizeOptions]).toEqual([20, 50, 100]);
    expect(batchDefinitionApi.excelDownloadOq).toContain("REQ-386 OQ");
  });

  it("blocks save when required fields are missing with field-level messages", () => {
    const errors = batchDefinitionApi.validateForm({
      batchId: "",
      batchType: "",
      scheduleCycle: "",
      ownerUserId: "",
      maxExecutionSeconds: "",
      predecessorBatchIds: "",
      successorBatchIds: "",
      parametersText: "{}",
    });
    expect(errors).toMatchObject({
      batchId: "배치ID는 필수입니다.",
      batchType: "업무유형은 필수입니다.",
      scheduleCycle: "실행주기는 필수입니다.",
      ownerUserId: "담당자는 필수입니다.",
    });
  });

  it("documents save confirmation and completion 안내 messages", () => {
    expect(batchDefinitionApi.uiMessages.saveConfirm("BATCH-A")).toBe(
      "BATCH-A 배치 정의를 저장하시겠습니까?",
    );
    expect(batchDefinitionApi.uiMessages.saveSuccess).toBe(
      "배치 정의가 저장되었습니다.",
    );
  });

  it("builds save payload with dependencies and parameter JSON without hardcoded path ids", () => {
    const payload = batchDefinitionApi.toSavePayload({
      batchId: "BATCH-A",
      batchType: "EVALUATION_DATA",
      scheduleCycle: "DAILY 02:00",
      ownerUserId: "1",
      maxExecutionSeconds: "3600",
      predecessorBatchIds: "BATCH-PREV, BATCH-UPSTREAM",
      successorBatchIds: "BATCH-NEXT",
      parametersText: '{"year":2026}',
    });
    expect(payload).toEqual({
      batchId: "BATCH-A",
      batchType: "EVALUATION_DATA",
      scheduleCycle: "DAILY 02:00",
      ownerUserId: 1,
      maxExecutionSeconds: 3600,
      predecessorBatchIds: ["BATCH-PREV", "BATCH-UPSTREAM"],
      successorBatchIds: ["BATCH-NEXT"],
      parameters: { year: 2026 },
    });
  });
});

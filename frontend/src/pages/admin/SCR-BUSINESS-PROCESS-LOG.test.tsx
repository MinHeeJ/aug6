import { describe, expect, it } from "vitest";
import {
  businessProcessLogApi,
  createEmptyBusinessProcessLogState,
  getBusinessProcessLogRouteContract,
  reduceBusinessProcessLogState,
} from "./SCR-BUSINESS-PROCESS-LOG";

describe("SCR-BUSINESS-PROCESS-LOG route contract and state handling", () => {
  it("declares business process log route and relative list operation", () => {
    expect(getBusinessProcessLogRouteContract()).toEqual({
      route: "/admin/audit/business-process-logs",
      screenId: "SCR-BUSINESS-PROCESS-LOG",
      operations: ["listBusinessProcessLogs"],
    });
    expect(
      businessProcessLogApi.paths.list({
        filter: "admin",
        actionType: "UPDATE",
        targetKey: "SEED-BUSINESS-AUDIT-001",
        actorUserId: "1",
        resultStatus: "SUCCESS",
        fromDate: "2026-08-01",
        toDate: "2026-08-31",
        page: 0,
        size: 20,
      }),
    ).toBe(
      "/api/admin/audit/business-process-logs?page=0&size=20&filter=admin&actionType=UPDATE&targetKey=SEED-BUSINESS-AUDIT-001&actorUserId=1&resultStatus=SUCCESS&fromDate=2026-08-01&toDate=2026-08-31",
    );
  });

  it("represents loading empty error and permission states", () => {
    const loading = reduceBusinessProcessLogState(
      createEmptyBusinessProcessLogState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceBusinessProcessLogState(loading, {
      type: "loaded",
      logs: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceBusinessProcessLogState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceBusinessProcessLogState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
  });

  it("uses default 20 rows and exposes only 20/50/100 page size options", () => {
    expect(businessProcessLogApi.paths.list()).toBe(
      "/api/admin/audit/business-process-logs?page=0&size=20",
    );
    expect([...businessProcessLogApi.pageSizeOptions]).toEqual([20, 50, 100]);
  });

  it("exposes immutable action/result filters without mutating actions", () => {
    expect([...businessProcessLogApi.actionTypeOptions]).toEqual([
      "",
      "CREATE",
      "UPDATE",
      "DELETE",
      "CONFIRM",
      "AUTH",
      "APPROVE",
      "CANCEL",
      "BATCH",
      "SESSION_TERMINATE",
    ]);
    expect([...businessProcessLogApi.resultStatusOptions]).toEqual([
      "",
      "SUCCESS",
      "FAILURE",
    ]);
    expect("create" in businessProcessLogApi).toBe(false);
    expect("update" in businessProcessLogApi).toBe(false);
    expect("delete" in businessProcessLogApi).toBe(false);
  });
});

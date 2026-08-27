import { describe, expect, it } from "vitest";
import {
  createEmptyPermissionChangeLogState,
  getPermissionChangeLogRouteContract,
  permissionChangeLogApi,
  reducePermissionChangeLogState,
} from "./SCR-PERMISSION-CHANGE-LOG";

describe("SCR-PERMISSION-CHANGE-LOG route contract and state handling", () => {
  it("declares permission change log route and relative list operation", () => {
    expect(getPermissionChangeLogRouteContract()).toEqual({
      route: "/admin/audit/permission-change-logs",
      screenId: "SCR-PERMISSION-CHANGE-LOG",
      operations: ["listPermissionChangeLogs"],
    });
    expect(
      permissionChangeLogApi.paths.list({
        targetType: "FUNCTION",
        targetId: "SCR-FUNCTION-PERMISSION-MGMT:R09:UPDATE",
        approverUserId: "1",
        changedBy: "1",
        fromDate: "2026-08-01",
        toDate: "2026-08-31",
        page: 0,
        size: 20,
      }),
    ).toBe(
      "/api/admin/audit/permission-change-logs?page=0&size=20&targetType=FUNCTION&targetId=SCR-FUNCTION-PERMISSION-MGMT%3AR09%3AUPDATE&approverUserId=1&changedBy=1&fromDate=2026-08-01&toDate=2026-08-31",
    );
  });

  it("represents loading empty error and permission states", () => {
    const loading = reducePermissionChangeLogState(
      createEmptyPermissionChangeLogState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reducePermissionChangeLogState(loading, {
      type: "loaded",
      logs: [],
    });
    expect(empty.status).toBe("empty");
    const error = reducePermissionChangeLogState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reducePermissionChangeLogState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
  });

  it("uses default 20 rows and exposes only 20/50/100 page size options", () => {
    expect(permissionChangeLogApi.paths.list()).toBe(
      "/api/admin/audit/permission-change-logs?page=0&size=20",
    );
    expect([...permissionChangeLogApi.pageSizeOptions]).toEqual([20, 50, 100]);
  });

  it("exposes readonly filters and never includes permission mutation actions", () => {
    expect([...permissionChangeLogApi.targetTypeOptions]).toEqual([
      "",
      "ROLE",
      "MENU",
      "FUNCTION",
      "DATA_SCOPE",
      "TEMPORARY",
    ]);
    expect("create" in permissionChangeLogApi).toBe(false);
    expect("update" in permissionChangeLogApi).toBe(false);
    expect("delete" in permissionChangeLogApi).toBe(false);
    expect(JSON.stringify(permissionChangeLogApi)).not.toMatch(
      /grantPermission|revokePermission|savePermission/,
    );
  });
});

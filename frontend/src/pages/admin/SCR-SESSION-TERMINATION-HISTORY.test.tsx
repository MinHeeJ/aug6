import { describe, expect, it } from "vitest";
import {
  createEmptySessionTerminationHistoryState,
  getSessionTerminationHistoryRouteContract,
  reduceSessionTerminationHistoryState,
  sessionTerminationHistoryApi,
} from "./SCR-SESSION-TERMINATION-HISTORY";

describe("SCR-SESSION-TERMINATION-HISTORY route contract and state handling", () => {
  it("declares session termination history route and relative list operation", () => {
    expect(getSessionTerminationHistoryRouteContract()).toEqual({
      route: "/admin/security/session-termination-histories",
      screenId: "SCR-SESSION-TERMINATION-HISTORY",
      operations: ["listSessionTerminationHistories"],
    });
    expect(
      sessionTerminationHistoryApi.paths.list({
        filter: "홍길동",
        terminationType: "IDLE_TIMEOUT",
        fromDate: "2026-08-01",
        toDate: "2026-08-31",
        page: 0,
        size: 20,
      }),
    ).toBe(
      "/api/admin/security/session-termination-histories?page=0&size=20&filter=%ED%99%8D%EA%B8%B8%EB%8F%99&terminationType=IDLE_TIMEOUT&fromDate=2026-08-01&toDate=2026-08-31",
    );
  });

  it("represents loading empty error and permission states", () => {
    const loading = reduceSessionTerminationHistoryState(
      createEmptySessionTerminationHistoryState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceSessionTerminationHistoryState(loading, {
      type: "loaded",
      histories: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceSessionTerminationHistoryState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceSessionTerminationHistoryState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
  });

  it("uses default 20 rows and exposes only 20/50/100 page size options", () => {
    expect(sessionTerminationHistoryApi.paths.list()).toBe(
      "/api/admin/security/session-termination-histories?page=0&size=20",
    );
    expect([...sessionTerminationHistoryApi.pageSizeOptions]).toEqual([
      20, 50, 100,
    ]);
  });

  it("exposes the four immutable termination history types without mutating actions", () => {
    expect([...sessionTerminationHistoryApi.terminationTypeOptions]).toEqual([
      "",
      "LOGOUT",
      "IDLE_TIMEOUT",
      "ABSOLUTE_TIMEOUT",
      "ADMIN_TERMINATED",
    ]);
    expect("create" in sessionTerminationHistoryApi).toBe(false);
    expect("update" in sessionTerminationHistoryApi).toBe(false);
    expect("delete" in sessionTerminationHistoryApi).toBe(false);
  });
});

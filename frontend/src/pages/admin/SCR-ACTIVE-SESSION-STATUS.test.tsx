import { describe, expect, it } from "vitest";
import {
  activeSessionApi,
  createEmptyActiveSessionState,
  getActiveSessionRouteContract,
  reduceActiveSessionState,
} from "./SCR-ACTIVE-SESSION-STATUS";

describe("SCR-ACTIVE-SESSION-STATUS route contract and state handling", () => {
  it("declares active session route and relative API operations", () => {
    expect(getActiveSessionRouteContract()).toEqual({
      route: "/admin/security/active-sessions",
      screenId: "SCR-ACTIVE-SESSION-STATUS",
      operations: ["listActiveSessions", "terminateActiveSession"],
    });
    expect(
      activeSessionApi.paths.list({ filter: "홍길동", page: 0, size: 20 }),
    ).toBe(
      "/api/admin/security/active-sessions?page=0&size=20&filter=%ED%99%8D%EA%B8%B8%EB%8F%99",
    );
    expect(activeSessionApi.paths.terminate("SESS-1")).toBe(
      "/api/admin/security/active-sessions/SESS-1/terminate",
    );
  });

  it("represents loading empty error permission and success states", () => {
    const loading = reduceActiveSessionState(createEmptyActiveSessionState(), {
      type: "loading",
    });
    expect(loading.status).toBe("loading");
    const empty = reduceActiveSessionState(loading, {
      type: "loaded",
      sessions: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceActiveSessionState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceActiveSessionState(error, { type: "permission" });
    expect(permission.status).toBe("permission");
    const success = reduceActiveSessionState(permission, {
      type: "success",
      message: "완료",
      sessions: [],
    });
    expect(success.status).toBe("success");
  });

  it("uses default 20 rows and exposes only 20/50/100 page size options", () => {
    expect(activeSessionApi.paths.list()).toBe(
      "/api/admin/security/active-sessions?page=0&size=20",
    );
    expect([...activeSessionApi.pageSizeOptions]).toEqual([20, 50, 100]);
  });

  it("blocks terminate when reason is missing and documents confirmation and success messages", () => {
    expect(activeSessionApi.validateReason("")).toEqual({
      reason: "강제종료 사유는 필수입니다.",
    });
    expect(activeSessionApi.validateReason("비정상 접속")).toEqual({});
    expect(activeSessionApi.uiMessages.terminateConfirm("SESS-1")).toBe(
      "SESS-1 세션을 강제종료하시겠습니까?",
    );
    expect(activeSessionApi.uiMessages.terminateSuccess).toContain("완료");
  });
});

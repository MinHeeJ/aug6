import { describe, expect, it } from "vitest";
import {
  createEmptyRoleManagementState,
  getRoleManagementRouteContract,
  reduceRoleManagementState,
  roleManagementApi,
} from "./SCR-ROLE-MGMT";

describe("SCR-ROLE-MGMT route contract and state handling", () => {
  it("declares the role management route, screen id and relative API operations", () => {
    expect(getRoleManagementRouteContract()).toEqual({
      route: "/admin/roles",
      screenId: "SCR-ROLE-MGMT",
      operations: ["listRoles", "updateRole"],
    });
    expect(roleManagementApi.paths.list({ filter: "R09" })).toBe(
      "/api/admin/roles?filter=R09",
    );
    expect(roleManagementApi.paths.update("R09")).toBe("/api/admin/roles/R09");
  });

  it("represents loading, empty, error, permission and success states for the role screen", () => {
    const loading = reduceRoleManagementState(
      createEmptyRoleManagementState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceRoleManagementState(loading, {
      type: "loaded",
      roles: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceRoleManagementState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceRoleManagementState(error, { type: "permission" });
    expect(permission.status).toBe("permission");
    const success = reduceRoleManagementState(permission, {
      type: "success",
      message: "역할 정보가 저장되었습니다.",
    });
    expect(success.status).toBe("success");
    expect(success.message).toBe("역할 정보가 저장되었습니다.");
  });

  it("keeps roleCode immutable by excluding roleCode from update payloads", () => {
    const payload = roleManagementApi.toUpdatePayload({
      roleCode: "R09",
      roleName: "시스템 관리자",
      purpose: "공통기능 관리",
      assignmentCriteria: "R09 관리자",
      defaultDataScope: "전체 데이터",
      changeReason: "역할 목적 정비",
    });

    expect(payload).toEqual({
      roleName: "시스템 관리자",
      purpose: "공통기능 관리",
      assignmentCriteria: "R09 관리자",
      defaultDataScope: "전체 데이터",
      changeReason: "역할 목적 정비",
    });
    expect(payload).not.toHaveProperty("roleCode");
  });
});

import { describe, expect, it } from "vitest";
import {
  createEmptyUserRoleManagementState,
  getUserRoleManagementRouteContract,
  reduceUserRoleManagementState,
  userRoleManagementApi,
} from "./SCR-USER-ROLE-MGMT";

describe("SCR-USER-ROLE-MGMT route contract and state handling", () => {
  it("declares the required route, screen id and relative API operations", () => {
    expect(getUserRoleManagementRouteContract()).toEqual({
      route: "/admin/user-roles",
      screenId: "SCR-USER-ROLE-MGMT",
      operations: [
        "assignUserRole",
        "updateUserRole",
        "revokeUserRole",
        "listCurrentUserRoles",
      ],
    });
    expect(userRoleManagementApi.paths.list({ roleCodeFilter: "R01" })).toBe(
      "/api/admin/user-roles?roleCodeFilter=R01",
    );
    expect(userRoleManagementApi.paths.listCurrentUserRoles(2)).toBe(
      "/api/admin/users/2/roles",
    );
    expect(userRoleManagementApi.paths.assign()).toBe("/api/admin/user-roles");
    expect(userRoleManagementApi.paths.update(20)).toBe(
      "/api/admin/user-roles/20",
    );
    expect(userRoleManagementApi.paths.revoke(20)).toBe(
      "/api/admin/user-roles/20",
    );
  });

  it("represents loading, empty, error, permission and success states for the user role screen", () => {
    const loading = reduceUserRoleManagementState(
      createEmptyUserRoleManagementState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceUserRoleManagementState(loading, {
      type: "loaded",
      assignments: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceUserRoleManagementState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceUserRoleManagementState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
    const success = reduceUserRoleManagementState(permission, {
      type: "success",
      message: "사용자 역할이 저장되었습니다.",
    });
    expect(success.status).toBe("success");
    expect(success.message).toBe("사용자 역할이 저장되었습니다.");
  });
});

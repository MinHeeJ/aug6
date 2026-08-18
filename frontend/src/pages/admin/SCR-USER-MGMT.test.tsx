import { describe, expect, it } from "vitest";
import {
  createEmptyUserManagementState,
  getUserManagementRouteContract,
  reduceUserManagementState,
  userManagementApi,
} from "./SCR-USER-MGMT";

describe("SCR-USER-MGMT route contract and state handling", () => {
  it("declares the required route, screen id, Korean labels and relative API operations", () => {
    expect(getUserManagementRouteContract()).toEqual({
      route: "/admin/users",
      screenId: "SCR-USER-MGMT",
      operations: ["searchUsers", "updateUserAccount", "updateUserRoles"],
    });
    expect(userManagementApi.paths.search({ name: "홍길동" })).toBe(
      "/api/admin/users?name=%ED%99%8D%EA%B8%B8%EB%8F%99",
    );
    expect(userManagementApi.paths.updateAccount(2)).toBe(
      "/api/admin/users/2/account",
    );
    expect(userManagementApi.paths.updateRoles(2)).toBe(
      "/api/admin/users/2/roles",
    );
  });

  it("represents loading, empty, error, permission and success states for the user screen", () => {
    const loading = reduceUserManagementState(
      createEmptyUserManagementState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceUserManagementState(loading, {
      type: "loaded",
      users: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceUserManagementState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceUserManagementState(error, { type: "permission" });
    expect(permission.status).toBe("permission");
    const success = reduceUserManagementState(permission, {
      type: "success",
      message: "사용자 정보가 저장되었습니다.",
    });
    expect(success.status).toBe("success");
    expect(success.message).toBe("사용자 정보가 저장되었습니다.");
  });
});

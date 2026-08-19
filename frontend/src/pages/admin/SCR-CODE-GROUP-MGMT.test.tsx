import { describe, expect, it } from "vitest";
import {
  codeGroupManagementApi,
  createEmptyCodeGroupManagementState,
  getCodeGroupManagementRouteContract,
  reduceCodeGroupManagementState,
} from "./SCR-CODE-GROUP-MGMT";

describe("SCR-CODE-GROUP-MGMT route contract and state handling", () => {
  it("declares the code group route, screen id and relative API operations", () => {
    expect(getCodeGroupManagementRouteContract()).toEqual({
      route: "/admin/code-groups",
      screenId: "SCR-CODE-GROUP-MGMT",
      operations: ["listCodeGroups", "createCodeGroup", "updateCodeGroup"],
    });
    expect(codeGroupManagementApi.paths.list({ groupIdFilter: "EVAL" })).toBe(
      "/api/admin/code-groups?groupIdFilter=EVAL",
    );
    expect(codeGroupManagementApi.paths.create()).toBe(
      "/api/admin/code-groups",
    );
    expect(codeGroupManagementApi.paths.update("PROC_STATUS")).toBe(
      "/api/admin/code-groups/PROC_STATUS",
    );
  });

  it("represents loading, empty, error, permission and success states for the code group screen", () => {
    const loading = reduceCodeGroupManagementState(
      createEmptyCodeGroupManagementState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceCodeGroupManagementState(loading, {
      type: "loaded",
      codeGroups: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceCodeGroupManagementState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceCodeGroupManagementState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
    const success = reduceCodeGroupManagementState(permission, {
      type: "success",
      message: "코드그룹이 등록되었습니다.",
    });
    expect(success.status).toBe("success");
    expect(success.message).toBe("코드그룹이 등록되었습니다.");
  });

  it("normalizes groupId and keeps detail code navigation tied to selected row groupId", () => {
    const payload = codeGroupManagementApi.toPayload({
      groupId: "proc_status",
      groupName: "처리상태",
      description: "처리상태 코드 묶음",
      managingDepartment: "교수지원과",
      systemUseYn: "Y",
      changeReason: "신규 등록",
    });

    expect(payload.groupId).toBe("PROC_STATUS");
    expect(codeGroupManagementApi.paths.detailCodes(payload.groupId)).toBe(
      "/admin/detail-codes?groupId=PROC_STATUS",
    );
  });
});

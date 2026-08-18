import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import {
  createEmptyDetailCodeManagementState,
  DetailCodeManagementPage,
  detailCodeManagementApi,
  getDetailCodeManagementRouteContract,
  reduceDetailCodeManagementState,
} from "./SCR-DETAIL-CODE-MGMT";

describe("SCR-DETAIL-CODE-MGMT route contract and state handling", () => {
  it("declares the detail code route, screen id and relative API operations", () => {
    expect(getDetailCodeManagementRouteContract()).toEqual({
      route: "/admin/detail-codes",
      screenId: "SCR-DETAIL-CODE-MGMT",
      operations: ["listDetailCodes", "createDetailCode", "updateDetailCode"],
    });
    expect(detailCodeManagementApi.paths.list("COMMON_STATUS")).toBe(
      "/api/admin/code-groups/COMMON_STATUS/codes",
    );
    expect(detailCodeManagementApi.paths.create("COMMON_STATUS")).toBe(
      "/api/admin/code-groups/COMMON_STATUS/codes",
    );
    expect(
      detailCodeManagementApi.paths.update("COMMON_STATUS", "ACTIVE"),
    ).toBe("/api/admin/code-groups/COMMON_STATUS/codes/ACTIVE");
  });

  it("represents loading, empty, error, permission and success states for the detail code screen", () => {
    const loading = reduceDetailCodeManagementState(
      createEmptyDetailCodeManagementState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceDetailCodeManagementState(loading, {
      type: "loaded",
      detailCodes: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceDetailCodeManagementState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceDetailCodeManagementState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
    const success = reduceDetailCodeManagementState(permission, {
      type: "success",
      message: "상세코드가 등록되었습니다.",
    });
    expect(success.status).toBe("success");
    expect(success.message).toBe("상세코드가 등록되었습니다.");
  });

  it("does not invent a COMMON_STATUS groupId when the route query is missing", () => {
    const html = renderToStaticMarkup(<DetailCodeManagementPage />);

    expect(html).toContain("코드그룹 ID");
    expect(html).not.toContain('value="COMMON_STATUS"');
    expect(html).toContain("추가속성");
  });

  it("normalizes code identity and blocks arbitrary additional attribute payload until REQ-062 is resolved", () => {
    const payload = detailCodeManagementApi.toPayload({
      codeValue: "pending",
      codeName: "대기",
      parentCodeValue: "active",
      sortOrder: "3",
      additionalAttributesText: '{"externalCode":"P"}',
      systemUseYn: "Y",
      validStartDate: "2026-01-01",
      validEndDate: "",
      changeReason: "상세코드 등록",
    });

    expect(payload.codeValue).toBe("PENDING");
    expect(payload.parentCodeValue).toBe("ACTIVE");
    expect(payload.additionalAttributes).toBeUndefined();
  });
});

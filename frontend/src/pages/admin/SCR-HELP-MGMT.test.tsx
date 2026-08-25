import { describe, expect, it } from "vitest";
import {
  createEmptyHelpContentManagementState,
  getHelpContentManagementRouteContract,
  helpContentManagementApi,
  reduceHelpContentManagementState,
} from "./SCR-HELP-MGMT";

describe("SCR-HELP-MGMT route contract and state handling", () => {
  it("declares help content management route and relative API operations", () => {
    expect(getHelpContentManagementRouteContract()).toEqual({
      route: "/admin/help-contents",
      screenId: "SCR-HELP-MGMT",
      operations: ["listHelpContents", "saveHelpContent", "getHelpContent"],
    });
    expect(
      helpContentManagementApi.paths.list({ screenId: "SCR-USER-MGMT" }),
    ).toBe("/api/admin/help-contents?screenId=SCR-USER-MGMT&page=0&size=20");
    expect(helpContentManagementApi.paths.save("SCR-USER-MGMT")).toBe(
      "/api/admin/help-contents/SCR-USER-MGMT",
    );
    expect(helpContentManagementApi.paths.get("SCR-USER-MGMT")).toBe(
      "/api/help-contents/SCR-USER-MGMT",
    );
  });

  it("represents loading empty error permission and success states", () => {
    const loading = reduceHelpContentManagementState(
      createEmptyHelpContentManagementState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceHelpContentManagementState(loading, {
      type: "loaded",
      helpContents: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceHelpContentManagementState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceHelpContentManagementState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
    const success = reduceHelpContentManagementState(permission, {
      type: "success",
      message: "도움말이 저장되었습니다.",
    });
    expect(success.status).toBe("success");
    expect(success.message).toBe("도움말이 저장되었습니다.");
  });

  it("builds save payload without mutating screenId in request body", () => {
    const payload = helpContentManagementApi.toSavePayload({
      screenId: "SCR-USER-MGMT",
      businessDescription: "사용자 계정 관리",
      inputCriteria: "필수 항목을 입력합니다.",
      faq: "Q. 저장은 언제 하나요?",
      contact: "admin@knue.ac.kr",
      changeReason: "도움말 정비",
    });
    expect(payload).toEqual({
      businessDescription: "사용자 계정 관리",
      inputCriteria: "필수 항목을 입력합니다.",
      faq: "Q. 저장은 언제 하나요?",
      contact: "admin@knue.ac.kr",
      changeReason: "도움말 정비",
    });
  });
});

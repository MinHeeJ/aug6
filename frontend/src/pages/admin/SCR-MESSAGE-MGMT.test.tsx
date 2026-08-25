import { describe, expect, it } from "vitest";
import {
  createEmptyMessageManagementState,
  getMessageManagementRouteContract,
  messageManagementApi,
  reduceMessageManagementState,
} from "./SCR-MESSAGE-MGMT";

describe("SCR-MESSAGE-MGMT route contract and state handling", () => {
  it("declares message management route and relative API operations", () => {
    expect(getMessageManagementRouteContract()).toEqual({
      route: "/admin/messages",
      screenId: "SCR-MESSAGE-MGMT",
      operations: ["listMessages", "saveMessage", "getMessageText"],
    });
    expect(
      messageManagementApi.paths.list({
        messageType: "SAVE",
        messageCode: "SAVE",
      }),
    ).toBe(
      "/api/admin/system-settings/messages?messageType=SAVE&messageCode=SAVE&page=0&size=20",
    );
    expect(messageManagementApi.paths.save("SAVE.SUCCESS")).toBe(
      "/api/admin/system-settings/messages/SAVE.SUCCESS",
    );
    expect(messageManagementApi.paths.text("SAVE.SUCCESS")).toBe(
      "/api/system/messages/SAVE.SUCCESS",
    );
  });

  it("represents loading empty error permission and success states", () => {
    const loading = reduceMessageManagementState(
      createEmptyMessageManagementState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceMessageManagementState(loading, {
      type: "loaded",
      messages: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceMessageManagementState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceMessageManagementState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
    const success = reduceMessageManagementState(permission, {
      type: "success",
      message: "메시지가 저장되었습니다.",
    });
    expect(success.status).toBe("success");
    expect(success.message).toBe("메시지가 저장되었습니다.");
  });

  it("builds save payload without mutating messageCode in request body", () => {
    const payload = messageManagementApi.toSavePayload({
      messageCode: "SAVE.SUCCESS",
      messageType: "SAVE",
      userMessage: "저장되었습니다.",
      changeReason: "문구 정비",
    });
    expect(payload).toEqual({
      messageType: "SAVE",
      userMessage: "저장되었습니다.",
      changeReason: "문구 정비",
    });
  });
});

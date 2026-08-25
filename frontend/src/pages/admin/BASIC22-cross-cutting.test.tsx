import { describe, expect, it } from "vitest";
import { helpContentManagementApi } from "./SCR-HELP-MGMT";
import { manualManagementApi } from "./SCR-MANUAL-MGMT";
import { messageManagementApi } from "./SCR-MESSAGE-MGMT";
import { noticeManagementApi } from "./SCR-NOTICE-MGMT";

const contractedPageSizes = [20, 50, 100] as const;
const environmentSmokeMatrix = [
  "Edge desktop",
  "Chrome desktop",
  "Safari desktop",
  "Opera desktop",
  "Whale desktop",
  "iPadOS tablet",
  "Android tablet",
] as const;

describe("BASIC-22 cross-cutting verification", () => {
  it("keeps all new list API helpers at default 20 rows and permits 20/50/100 selections", () => {
    expect(messageManagementApi.paths.list()).toBe(
      "/api/admin/system-settings/messages?page=0&size=20",
    );
    expect(noticeManagementApi.paths.list()).toBe(
      "/api/admin/notices?page=0&pageSize=20",
    );
    expect(helpContentManagementApi.paths.list()).toBe(
      "/api/admin/help-contents?page=0&size=20",
    );
    expect(manualManagementApi.paths.list()).toBe(
      "/api/admin/manuals?page=0&size=20",
    );

    expect(
      contractedPageSizes.map((size) =>
        messageManagementApi.paths.list({ size }),
      ),
    ).toEqual([
      "/api/admin/system-settings/messages?page=0&size=20",
      "/api/admin/system-settings/messages?page=0&size=50",
      "/api/admin/system-settings/messages?page=0&size=100",
    ]);
    expect(
      contractedPageSizes.map((pageSize) =>
        noticeManagementApi.paths.list({ pageSize }),
      ),
    ).toEqual([
      "/api/admin/notices?page=0&pageSize=20",
      "/api/admin/notices?page=0&pageSize=50",
      "/api/admin/notices?page=0&pageSize=100",
    ]);
    expect(
      contractedPageSizes.map((size) =>
        helpContentManagementApi.paths.list({ size }),
      ),
    ).toEqual([
      "/api/admin/help-contents?page=0&size=20",
      "/api/admin/help-contents?page=0&size=50",
      "/api/admin/help-contents?page=0&size=100",
    ]);
    expect(
      contractedPageSizes.map((size) =>
        manualManagementApi.paths.list({ size }),
      ),
    ).toEqual([
      "/api/admin/manuals?page=0&size=20",
      "/api/admin/manuals?page=0&size=50",
      "/api/admin/manuals?page=0&size=100",
    ]);
  });

  it("documents save confirmation and post-processing user guidance messages separately from system errors", () => {
    expect(messageManagementApi.uiMessages.saveConfirm("SAVE.SUCCESS")).toBe(
      "SAVE.SUCCESS 메시지를 저장하시겠습니까?",
    );
    expect(messageManagementApi.uiMessages.saveSuccess).toBe(
      "메시지가 저장되었습니다.",
    );
    expect(noticeManagementApi.uiMessages.saveConfirm).toBe(
      "공지사항을 저장하시겠습니까?",
    );
    expect(noticeManagementApi.uiMessages.saveSuccess).toBe(
      "공지사항이 저장되었습니다.",
    );
    expect(
      helpContentManagementApi.uiMessages.saveConfirm("SCR-MESSAGE-MGMT"),
    ).toBe("SCR-MESSAGE-MGMT 도움말을 저장하시겠습니까?");
    expect(helpContentManagementApi.uiMessages.saveSuccess).toBe(
      "도움말이 저장되었습니다.",
    );
    expect(manualManagementApi.uiMessages.createConfirm("USER", "v1.0")).toBe(
      "USER v1.0 매뉴얼을 등록하시겠습니까?",
    );
    expect(manualManagementApi.uiMessages.createSuccess).toBe(
      "매뉴얼이 등록되었습니다.",
    );

    const systemError = new Error(
      ["org.postgresql.util.PSQLException", "password", "redacted"].join("="),
    );
    expect(systemError.message).toContain("redacted");
    expect(messageManagementApi.uiMessages.error).toBe(
      "메시지 정보를 처리하지 못했습니다.",
    );
    expect(noticeManagementApi.uiMessages.error).toBe(
      "공지사항을 처리하지 못했습니다.",
    );
    expect(helpContentManagementApi.uiMessages.error).toBe(
      "도움말 정보를 처리하지 못했습니다.",
    );
    expect(manualManagementApi.uiMessages.error).toBe(
      "매뉴얼 정보를 처리하지 못했습니다.",
    );
    expect(
      [
        messageManagementApi.uiMessages.error,
        noticeManagementApi.uiMessages.error,
        helpContentManagementApi.uiMessages.error,
        manualManagementApi.uiMessages.error,
      ].join(" "),
    ).not.toContain("secret");
  });

  it("keeps the quickstart browser and tablet smoke scope explicit for the final phase", () => {
    expect(environmentSmokeMatrix).toEqual([
      "Edge desktop",
      "Chrome desktop",
      "Safari desktop",
      "Opera desktop",
      "Whale desktop",
      "iPadOS tablet",
      "Android tablet",
    ]);
  });
});

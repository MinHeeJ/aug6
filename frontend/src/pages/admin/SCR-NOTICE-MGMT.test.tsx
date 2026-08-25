import { describe, expect, it } from "vitest";
import {
  createEmptyNoticeManagementState,
  getNoticeManagementRouteContract,
  noticeManagementApi,
  reduceNoticeManagementState,
  type NoticeRow,
} from "./SCR-NOTICE-MGMT";

describe("SCR-NOTICE-MGMT route contract and state handling", () => {
  it("declares notice management route and relative API operations", () => {
    expect(getNoticeManagementRouteContract()).toEqual({
      route: "/admin/notices",
      screenId: "SCR-NOTICE-MGMT",
      operations: [
        "listNotices",
        "createNotice",
        "saveNotice",
        "downloadNoticeAttachment",
      ],
    });
    expect(
      noticeManagementApi.paths.list({
        targetRoleCode: "R09",
        targetOrganizationCode: "ORG001",
        pageSize: 20,
      }),
    ).toBe(
      "/api/admin/notices?page=0&pageSize=20&targetRoleCode=R09&targetOrganizationCode=ORG001",
    );
    expect(noticeManagementApi.paths.create()).toBe("/api/admin/notices");
    expect(noticeManagementApi.paths.save(11)).toBe("/api/admin/notices/11");
    expect(noticeManagementApi.paths.download(11, 101, "ORG001")).toBe(
      "/api/admin/notices/11/attachments/101/download?organizationCode=ORG001",
    );
  });

  it("represents loading empty error permission and success states", () => {
    const loading = reduceNoticeManagementState(
      createEmptyNoticeManagementState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceNoticeManagementState(loading, {
      type: "loaded",
      notices: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceNoticeManagementState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceNoticeManagementState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
    const success = reduceNoticeManagementState(permission, {
      type: "success",
      message: "공지사항이 저장되었습니다.",
    });
    expect(success.status).toBe("success");
    expect(success.message).toBe("공지사항이 저장되었습니다.");
  });

  it("builds save payload from selected role and organization without leaking internal attachment fields", () => {
    const payload = noticeManagementApi.toSavePayload({
      title: " 시스템 점검 안내 ",
      content: " 점검 본문 ",
      publishStartDate: "2026-08-25",
      publishEndDate: "2026-08-31",
      importantYn: "Y",
      targets: [],
      attachments: [
        { attachmentId: 101, originalFileName: "점검안내.txt", fileSize: 12 },
      ],
      roleTarget: "R09",
      organizationTarget: "ORG001",
      pendingAttachments: [
        { originalFileName: "점검안내.txt", contentText: "첨부 내용" },
      ],
      changeReason: " 공지 등록 ",
    } satisfies NoticeRow & {
      roleTarget: string;
      organizationTarget: string;
      pendingAttachments: Array<{
        originalFileName: string;
        contentText: string;
      }>;
      changeReason: string;
    });

    expect(payload).toEqual({
      title: "시스템 점검 안내",
      content: "점검 본문",
      publishStartDate: "2026-08-25",
      publishEndDate: "2026-08-31",
      importantYn: "Y",
      targets: [
        { targetType: "ROLE", targetId: "R09" },
        { targetType: "ORGANIZATION", targetId: "ORG001" },
      ],
      attachments: [
        { originalFileName: "점검안내.txt", contentText: "첨부 내용" },
      ],
      changeReason: "공지 등록",
    });
  });
});

import { describe, expect, it } from "vitest";
import {
  attachmentMetadataApi,
  createEmptyAttachmentMetadataState,
  getAttachmentMetadataRouteContract,
  reduceAttachmentMetadataState,
} from "./SCR-ATTACHMENT-METADATA";

describe("SCR-ATTACHMENT-METADATA route, API and state contract", () => {
  it("declares the attachment metadata route and relative API operations", () => {
    expect(getAttachmentMetadataRouteContract()).toEqual({
      route: "/admin/attachments",
      screenId: "SCR-ATTACHMENT-METADATA",
      operations: ["listAttachments", "getAttachmentDownload"],
    });
    expect(
      attachmentMetadataApi.paths.list({
        businessRecordId: "FE-2026-0001",
        page: 0,
        size: 20,
      }),
    ).toBe(
      "/api/admin/attachments?businessRecordId=FE-2026-0001&page=0&size=20",
    );
    expect(attachmentMetadataApi.paths.download(1001)).toBe(
      "/api/admin/attachments/1001/download",
    );
  });

  it("keeps storagePath and storedFilename out of public attachment metadata rows", () => {
    const publicKeys = attachmentMetadataApi.publicRowKeys();

    expect(publicKeys).toContain("originalFilename");
    expect(publicKeys).toContain("businessRecordId");
    expect(publicKeys).not.toContain("storagePath");
    expect(publicKeys).not.toContain("storedFilename");
  });

  it("represents loading, empty, error, permission and success states", () => {
    const loading = reduceAttachmentMetadataState(
      createEmptyAttachmentMetadataState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceAttachmentMetadataState(loading, {
      type: "loaded",
      attachments: [],
      page: 0,
      size: 20,
      totalElements: 0,
    });
    expect(empty.status).toBe("empty");
    const error = reduceAttachmentMetadataState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceAttachmentMetadataState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
    const success = reduceAttachmentMetadataState(permission, {
      type: "downloaded",
      message: "다운로드를 시작했습니다.",
    });
    expect(success.status).toBe("success");
  });
});

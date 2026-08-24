import { describe, expect, it, vi } from "vitest";
import {
  attachmentIntegrityApi,
  createEmptyAttachmentIntegrityState,
  getAttachmentIntegrityRouteContract,
  reduceAttachmentIntegrityState,
} from "./SCR-ATTACHMENT-INTEGRITY";

describe("SCR-ATTACHMENT-INTEGRITY route, API and progress contract", () => {
  it("declares route and relative API operations", () => {
    expect(getAttachmentIntegrityRouteContract()).toEqual({
      route: "/admin/attachment-integrity",
      screenId: "SCR-ATTACHMENT-INTEGRITY",
      operations: [
        "createAttachmentIntegrityCheck",
        "listAttachmentIntegrityResults",
        "downloadAttachmentIntegrityExcel",
      ],
    });
    expect(attachmentIntegrityApi.paths.createCheck()).toBe(
      "/api/admin/attachment-integrity-checks",
    );
    expect(
      attachmentIntegrityApi.paths.listResults({
        checkId: 3001,
        anomalyType: "MISSING_STORAGE_FILE",
        page: 0,
        size: 20,
      }),
    ).toBe(
      "/api/admin/attachment-integrity-results?checkId=3001&anomalyType=MISSING_STORAGE_FILE&page=0&size=20",
    );
    expect(
      attachmentIntegrityApi.paths.downloadExcel({
        checkId: 3001,
        anomalyType: "MISSING_STORAGE_FILE",
      }),
    ).toBe(
      "/api/admin/attachment-integrity-results/excel?checkId=3001&anomalyType=MISSING_STORAGE_FILE",
    );
  });

  it("shows progress only after a check runs for at least 10 seconds", () => {
    vi.useFakeTimers();
    const now = new Date("2026-08-24T09:00:00Z").getTime();
    vi.setSystemTime(now);
    const running = reduceAttachmentIntegrityState(
      createEmptyAttachmentIntegrityState(),
      {
        type: "running",
        startedAtMs: now,
      },
    );
    expect(running.status).toBe("running");
    expect(running.showProgress).toBe(false);

    vi.setSystemTime(now + 10_001);
    const progressed = reduceAttachmentIntegrityState(running, {
      type: "tick",
    });
    expect(progressed.showProgress).toBe(true);
    expect(progressed.message).toContain("진행 중");
    vi.useRealTimers();
  });

  it("represents completed result list and excel download success states", () => {
    const completed = reduceAttachmentIntegrityState(
      createEmptyAttachmentIntegrityState(),
      {
        type: "completed",
        check: {
          checkId: 3001,
          status: "COMPLETED",
          startedBy: 1,
          startedAt: "2026-08-24T09:00:00",
          completedAt: "2026-08-24T09:00:01",
          findingCount: 1,
          anomalyTypes: ["MISSING_STORAGE_FILE"],
        },
      },
    );
    expect(completed.status).toBe("success");
    const loaded = reduceAttachmentIntegrityState(completed, {
      type: "loaded",
      results: [
        {
          findingId: 4002,
          checkId: 3001,
          fileId: 1005,
          storageObjectRef: "/secure/path/file.bin",
          anomalyType: "MISSING_STORAGE_FILE",
          resultMessage: "DB 메타정보의 실제 저장소 파일이 없습니다.",
          createdAt: "2026-08-24T09:00:00",
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
    });
    expect(loaded.results).toHaveLength(1);
    const downloaded = reduceAttachmentIntegrityState(loaded, {
      type: "downloaded",
      message: "엑셀 다운로드를 시작했습니다.",
    });
    expect(downloaded.message).toBe("엑셀 다운로드를 시작했습니다.");
  });
});

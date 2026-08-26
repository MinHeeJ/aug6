import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import {
  batchResultApi,
  BatchResultManagementPage,
  getBatchResultRouteContract,
} from "./SCR-BATCH-RESULT-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    apiRequest: vi.fn(async (path: string) => {
      if (path.includes("/log")) {
        return {
          success: true,
          data: {
            executionId: "SEED-BATCH-RESULT-FAILED-001",
            logFileRef: "logs/batch/SEED-BATCH-RESULT-FAILED-001.log",
          },
          meta: {},
        };
      }
      return {
        success: true,
        data: {
          results: [
            {
              executionId: "SEED-BATCH-RESULT-SUCCESS-001",
              batchId: "SEED-BATCH-DEF-001",
              batchType: "EVALUATION_DATA",
              executionStatus: "COMPLETED",
              startedAt: "2026-08-26T02:00:00",
              endedAt: "2026-08-26T02:05:30",
              totalCount: 120,
              successCount: 118,
              failureCount: 1,
              excludedCount: 1,
              elapsedMillis: 330000,
              hasLog: true,
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      };
    }),
  };
});

describe("SCR-BATCH-RESULT-MGMT route contract and read-only guard", () => {
  it("declares result inquiry route and relative API operations", () => {
    expect(getBatchResultRouteContract()).toEqual({
      route: "/admin/batch-results",
      screenId: "SCR-BATCH-RESULT-MGMT",
      operations: ["listBatchResults", "getBatchResultLog"],
    });
    expect(
      batchResultApi.paths.list({
        executionId: "EXEC-001",
        batchId: "BATCH-A",
        executionStatus: "FAILED",
        page: 0,
        size: 20,
      }),
    ).toBe(
      "/api/admin/batch-results?page=0&size=20&executionId=EXEC-001&batchId=BATCH-A&executionStatus=FAILED",
    );
    expect(batchResultApi.paths.log("EXEC-001")).toBe(
      "/api/admin/batch-results/EXEC-001/log",
    );
  });

  it("uses default 20 rows and exposes only 20/50/100 page size options plus Excel OQ", () => {
    expect(batchResultApi.paths.list()).toBe(
      "/api/admin/batch-results?page=0&size=20",
    );
    expect([...batchResultApi.pageSizeOptions]).toEqual([20, 50, 100]);
    expect(batchResultApi.excelDownloadOq).toContain("REQ-386 OQ");
  });

  it("renders result fields, log lookup panel, and all readonly states without mutation CTAs", () => {
    const html = renderToStaticMarkup(<BatchResultManagementPage />);

    expect(html).toContain('data-screen-id="SCR-BATCH-RESULT-MGMT"');
    expect(html).toContain('data-testid="batch-result-management-screen"');
    expect(html).toContain("배치 결과 조회");
    expect(html).toContain("시작시간");
    expect(html).toContain("종료시간");
    expect(html).toContain("처리건수");
    expect(html).toContain("성공건수");
    expect(html).toContain("실패건수");
    expect(html).toContain("제외건수");
    expect(html).toContain("소요시간");
    expect(html).toContain("로그 조회");
    expect(html).toContain('data-testid="batch-result-export-button"');
    expect(html).toContain("배치 결과 조회 권한이 없습니다");
    expect(html).toContain("조회된 배치 결과가 없습니다");
    expect(html).toContain(
      "결과와 로그는 조회 전용이며 재실행, 실패자료 변경, 로그 수정·삭제 버튼이 없습니다",
    );
    expect(html).not.toContain('data-testid="batch-result-rerun-button"');
    expect(html).not.toContain('data-testid="batch-result-save-button"');
    expect(html).not.toContain('data-testid="batch-result-delete-log-button"');
  });
});

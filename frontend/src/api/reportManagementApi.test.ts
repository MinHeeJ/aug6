import { describe, expect, it, vi } from "vitest";
import { reportManagementApi } from "./apiClient";

describe("reportManagementApi Phase 2 common rules", () => {
  it("uses relative report list path with default 20-row pagination and optional filters only when present", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({
        success: true,
        data: { reports: [], page: 0, pageSize: 20, totalElements: 0 },
        meta: {},
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await reportManagementApi.listReports({
      businessCategory: "FACULTY_ACHIEVEMENT",
      page: 0,
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/business/reports?page=0&size=20&businessCategory=FACULTY_ACHIEVEMENT",
    );
    expect(fetchMock.mock.calls[0][0]).not.toContain("localhost");
    expect(fetchMock.mock.calls[0][0]).not.toContain("activeYn=");
  });

  it("sends single output requests from selected report and target data without sample IDs", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({
        success: true,
        data: {
          reportId: "FINAL_EVALUATION",
          formVersionName: "v2.0",
          outputBaseDate: "2026-01-01",
        },
        meta: {},
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await reportManagementApi.createReportOutput("FINAL_EVALUATION", {
      outputFormat: "PDF",
      outputBaseDate: "2026-01-01",
      targetPersonIds: [101, 102],
      targetSummary: "2026학년도 확정 대상",
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/business/reports/FINAL_EVALUATION/outputs",
    );
    expect(fetchMock.mock.calls[0][1].method).toBe("POST");
    expect(fetchMock.mock.calls[0][1].body).toContain("101");
    expect(fetchMock.mock.calls[0][1].body).not.toContain("USER-1");
  });

  it("creates bulk report jobs with base date and Excel/PDF output format from form state", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({
        success: true,
        data: { jobId: 901, status: "QUEUED" },
        meta: {},
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await reportManagementApi.createBulkReportJob({
      reportId: "FINAL_EVALUATION",
      targetPersonIds: [101, 102],
      targetHash: "HASH-FINAL-SELECTED",
      outputFormat: "EXCEL",
      outputBaseDate: "2026-01-01",
    });

    expect(fetchMock.mock.calls[0][0]).toBe("/api/business/bulk-report-jobs");
    expect(fetchMock.mock.calls[0][1].body).toContain("EXCEL");
    expect(fetchMock.mock.calls[0][1].body).toContain("2026-01-01");
  });

  it("uses common page-size options for report history and bulk target list calls", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({ success: true, data: {}, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await reportManagementApi.listReportPrintHistories({ page: 0, size: 50 });
    await reportManagementApi.listBulkReportTargets({
      reportId: "FINAL_EVALUATION",
      evaluationYear: "2026",
      page: 0,
      size: 100,
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/business/report-print-histories?page=0&size=50",
    );
    expect(fetchMock.mock.calls[1][0]).toBe(
      "/api/business/bulk-report-jobs/targets?page=0&size=100&reportId=FINAL_EVALUATION&evaluationYear=2026",
    );
  });

  it("downloads bulk job results with the result-file response contract", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({
        success: true,
        data: {
          jobId: 901,
          reportId: "FINAL_EVALUATION",
          resultFileRef: "reports/bulk/final-10.zip",
          successCount: 10,
          failCount: 1,
          failures: [{ targetPersonId: 103, errorDetail: "권한 범위 밖" }],
        },
        meta: {},
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const response = await reportManagementApi.downloadBulkReportJobResult(901);

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/business/bulk-report-jobs/901/result",
    );
    expect(response.data?.failures[0].errorDetail).toBe("권한 범위 밖");
  });
});

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import {
  BulkReportJobManagementPage,
  ReportFormVersionManagementPage,
  ReportListManagementPage,
  ReportPermissionManagementPage,
  ReportPrintHistoryPage,
} from "./ReportManagementPages";

const pages = [
  ["SCR-REPORT-LIST-MGMT", <ReportListManagementPage />],
  ["SCR-REPORT-FORM-VERSION-MGMT", <ReportFormVersionManagementPage />],
  ["SCR-REPORT-PERMISSION-MGMT", <ReportPermissionManagementPage />],
  ["SCR-REPORT-PRINT-HISTORY", <ReportPrintHistoryPage />],
  ["SCR-BULK-REPORT-JOB-MGMT", <BulkReportJobManagementPage />],
] as const;

describe("BASIC-54 report management UI vertical slices", () => {
  it("renders all Phase 3 report management screens with stable screen ids", () => {
    pages.forEach(([screenId, page]) => {
      const html = renderToStaticMarkup(page);
      expect(html).toContain(`data-screen-id=\"${screenId}\"`);
    });
  });

  it("renders report list search, required form, duplicate error area, and inactive exclusion guidance", () => {
    const html = renderToStaticMarkup(<ReportListManagementPage />);
    expect(html).toContain("보고서 목록 관리");
    expect(html).toContain("업무구분");
    expect(html).toContain("사용여부");
    expect(html).toContain("보고서ID*");
    expect(html).toContain("템플릿 파일*");
    expect(html).toContain("DUPLICATE_REPORT_ID");
    expect(html).toContain("미사용 보고서는 일반 사용자 출력대상에서 제외");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });

  it("renders form version list, save/cancel form, current flag, and preservation guidance", () => {
    const html = renderToStaticMarkup(<ReportFormVersionManagementPage />);
    expect(html).toContain("보고서 양식 관리");
    expect(html).toContain("보고서 종류");
    expect(html).toContain("버전명*");
    expect(html).toContain("시행일*");
    expect(html).toContain("양식 파일*");
    expect(html).toContain("현재 적용여부");
    expect(html).toContain("이전 버전은 삭제하지 않고 보존");
    expect(html).toContain("form-version-cancel-button");
  });

  it("renders permission matrix with bulk save and forbidden format guidance", () => {
    const html = renderToStaticMarkup(<ReportPermissionManagementPage />);
    expect(html).toContain("보고서 권한 관리");
    expect(html).toContain("대상유형");
    expect(html).toContain("조회");
    expect(html).toContain("미리보기");
    expect(html).toContain("인쇄");
    expect(html).toContain("PDF 출력");
    expect(html).toContain("엑셀 출력");
    expect(html).toContain("데이터 범위");
    expect(html).toContain("403");
  });

  it("keeps print history read-only without output, edit, or delete CTA test ids", () => {
    const html = renderToStaticMarkup(<ReportPrintHistoryPage />);
    expect(html).toContain("보고서 출력 이력");
    expect(html).toContain("조회 전용");
    expect(html).toContain("형식");
    expect(html).toContain("건수");
    expect(html).toContain("결과");
    expect(html).not.toContain("report-history-output-button");
    expect(html).not.toContain("report-history-edit-button");
    expect(html).not.toContain("report-history-delete-button");
  });

  it("renders bulk target preview, execution confirmation, progress, download, and failure detail UI", () => {
    const html = renderToStaticMarkup(<BulkReportJobManagementPage />);
    expect(html).toContain("대량 출력 관리");
    expect(html).toContain("대상건수 미리보기");
    expect(html).toContain("대량생성 실행");
    expect(html).toContain("진행률");
    expect(html).toContain("성공");
    expect(html).toContain("실패");
    expect(html).toContain("결과파일 다운로드");
    expect(html).toContain("실패 대상 목록/오류 상세");
  });

  it("keeps excluded business capabilities out of report management UI actions", () => {
    const html = pages.map(([, page]) => renderToStaticMarkup(page)).join("\n");
    expect(html).not.toContain("평가 확정 실행");
    expect(html).not.toContain("확정 취소");
    expect(html).not.toContain("시점 데이터 생성");
    expect(html).not.toContain("점수 산출 실행");
    expect(html).not.toContain("업적 입력 저장");
    expect(html).not.toContain("평가대상자 선정");
    expect(html).not.toContain("외부 연계 실행");
    expect(html).not.toContain("원천자료 수정");
    expect(html).not.toContain(
      'data-testid="report-out-of-scope-action-button"',
    );
  });

  it("surfaces open-question policy isolation without adding source-data actions", () => {
    const html = renderToStaticMarkup(<BulkReportJobManagementPage />);
    expect(html).toContain("미확정 정책은 설정값으로만 격리");
    expect(html).toContain("REPORT_RESULT_FILE_RETENTION_DAYS");
    expect(html).toContain("REPORT_PERMISSION_MERGE_STRATEGY");
    expect(html).toContain("REPORT_UNAUTHORIZED_BULK_TARGET_POLICY");
    expect(html).toContain("REPORT_RECORD_FAILED_PRINT_HISTORY");
    expect(html).not.toContain("결과파일 자동삭제 실행");
    expect(html).not.toContain("권한 병합 정책 확정");
    expect(html).not.toContain("원천 평가자료 변경");
  });

  it("uses OpenAPI relative paths for all report management API methods", async () => {
    const { reportManagementApi } = await import("../../api/apiClient");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({ success: true, data: {}, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await reportManagementApi.listReportFormVersions({ reportId: "FINAL" });
    await reportManagementApi.saveReportFormVersion({
      reportId: "FINAL",
      versionName: "v2.0",
      effectiveDate: "2026-01-01",
      formFileRef: "FILE-1",
      changeReason: "변경",
    });
    await reportManagementApi.listReportPermissions({
      granteeType: "ROLE",
      granteeId: "R03",
    });
    await reportManagementApi.saveReportPermissions({ permissions: [] });
    await reportManagementApi.listBulkReportJobs({ status: "RUNNING" });
    await reportManagementApi.getBulkReportJob(901);
    await reportManagementApi.downloadBulkReportJobResult(901);

    const calledPaths = fetchMock.mock.calls.map((call) => call[0] as string);
    expect(calledPaths).toEqual([
      "/api/business/report-form-versions?page=0&size=20&reportId=FINAL",
      "/api/business/report-form-versions/save",
      "/api/business/report-permissions?page=0&size=20&granteeType=ROLE&granteeId=R03",
      "/api/business/report-permissions/save",
      "/api/business/bulk-report-jobs?page=0&size=20&status=RUNNING",
      "/api/business/bulk-report-jobs/901",
      "/api/business/bulk-report-jobs/901/result",
    ]);
    calledPaths.forEach((path) => expect(path.startsWith("/api/")).toBe(true));
  });
});

import type React from "react";
import { RefreshCw, Save, Search, ShieldCheck } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  reportManagementApi,
  type ApiErrorField,
  type BulkReportJob,
  type BulkReportTarget,
  type PageSize,
  type ReportFormVersion,
  type ReportOutputFormat,
  type ReportPermission,
  type ReportPrintHistory,
  type ReportRow,
  type YesNo,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

const pageSizes: PageSize[] = [20, 50, 100];
const businessCategories = [
  "FACULTY_ACHIEVEMENT",
  "FINAL_EVALUATION",
  "RESULT_NOTICE",
];
const datasetCodes = [
  "FINAL_EVALUATION_DATASET",
  "PERSONAL_RESULT_DATASET",
  "NOTICE_RESULT_DATASET",
];
const dataScopes = ["SELF", "DEPARTMENT", "COLLEGE", "ALL"];

export function ReportListManagementPage() {
  const api = useApiState("보고서 목록 처리를 완료하지 못했습니다.");
  const [filters, setFilters] = useState({
    businessCategory: "",
    activeYn: "",
  });
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [rows, setRows] = useState<ReportRow[]>([]);
  const [form, setForm] = useState({
    reportId: "",
    reportName: "",
    businessCategory: businessCategories[0],
    templateFileRef: "",
    datasetCode: datasetCodes[0],
    activeYn: "Y" as YesNo,
    changeReason: "",
  });

  const load = async () => {
    try {
      api.start();
      const response = await reportManagementApi.listReports({
        businessCategory: filters.businessCategory,
        activeYn: filters.activeYn as YesNo | "",
        size: pageSize,
      });
      setRows(response.data?.reports ?? []);
    } catch (caught) {
      api.handle(caught);
    } finally {
      api.finish();
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const save = async () => {
    const errors = requiredErrors(form, [
      "reportId",
      "reportName",
      "businessCategory",
      "templateFileRef",
      "datasetCode",
      "changeReason",
    ]);
    if (Object.keys(errors).length > 0) {
      api.setFieldErrors(errors);
      api.setError("필수 입력 항목을 확인하세요.");
      return;
    }
    if (!window.confirm("보고서 정보를 저장하시겠습니까?")) return;
    try {
      await reportManagementApi.saveReport(form);
      api.setSuccess("보고서 저장 후 목록을 재조회했습니다.");
      await load();
    } catch (caught) {
      api.handle(caught);
    }
  };

  return (
    <ReportScreen
      screenId="SCR-REPORT-LIST-MGMT"
      title="보고서 목록 관리"
      menuPath="보고서 관리 > 보고서 관리 > 보고서 목록 관리"
      description="보고서ID, 템플릿 파일, 데이터셋, 사용여부를 중앙에서 관리합니다. 미사용 보고서는 일반 사용자 출력대상에서 제외됩니다."
      state={api}
      testId="report-list-page"
    >
      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">검색</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <SelectField
            label="업무구분"
            value={filters.businessCategory}
            onChange={(value) =>
              setFilters({ ...filters, businessCategory: value })
            }
            testId="report-business-category-select"
            options={["", ...businessCategories]}
          />
          <SelectField
            label="사용여부"
            value={filters.activeYn}
            onChange={(value) => setFilters({ ...filters, activeYn: value })}
            testId="report-active-select"
            options={["", "Y", "N"]}
          />
          <PageSizeSelect
            value={pageSize}
            onChange={setPageSize}
            testId="report-page-size-select"
          />
        </div>
        <ActionButton
          kind="secondary"
          onClick={() => void load()}
          testId="report-search-button"
        >
          <Search size={16} />
          조회
        </ActionButton>
      </section>
      <ReportTable
        rows={rows}
        onSelect={(row) => setForm({ ...row, changeReason: "" })}
      />
      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">보고서 등록·수정</h2>
        <p className="mt-2 text-sm text-muted">
          오류 표시 영역: DUPLICATE_REPORT_ID / INVALID_TEMPLATE
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <TextField
            label="보고서ID*"
            value={form.reportId}
            error={api.fieldErrors.reportId}
            onChange={(value) => setForm({ ...form, reportId: value })}
            testId="report-id-input"
          />
          <TextField
            label="보고서명*"
            value={form.reportName}
            error={api.fieldErrors.reportName}
            onChange={(value) => setForm({ ...form, reportName: value })}
            testId="report-name-input"
          />
          <SelectField
            label="업무구분*"
            value={form.businessCategory}
            onChange={(value) => setForm({ ...form, businessCategory: value })}
            testId="report-form-business-category-select"
            options={businessCategories}
          />
          <TextField
            label="템플릿 파일*"
            value={form.templateFileRef}
            error={api.fieldErrors.templateFileRef}
            onChange={(value) => setForm({ ...form, templateFileRef: value })}
            testId="report-template-file-input"
            placeholder="FileStorage ref"
          />
          <SelectField
            label="데이터셋*"
            value={form.datasetCode}
            onChange={(value) => setForm({ ...form, datasetCode: value })}
            testId="report-dataset-select"
            options={datasetCodes}
          />
          <SelectField
            label="사용여부"
            value={form.activeYn}
            onChange={(value) => setForm({ ...form, activeYn: value as YesNo })}
            testId="report-form-active-select"
            options={["Y", "N"]}
          />
          <TextField
            label="변경사유*"
            value={form.changeReason}
            error={api.fieldErrors.changeReason}
            onChange={(value) => setForm({ ...form, changeReason: value })}
            testId="report-change-reason-input"
          />
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <ActionButton
            kind="primary"
            onClick={() => void save()}
            testId="report-save-button"
          >
            <Save size={16} />
            저장
          </ActionButton>
          <ActionButton
            kind="secondary"
            onClick={() =>
              setForm({
                ...form,
                reportId: "",
                reportName: "",
                templateFileRef: "",
                changeReason: "",
              })
            }
            testId="report-cancel-button"
          >
            취소
          </ActionButton>
        </div>
      </section>
    </ReportScreen>
  );
}

export function ReportFormVersionManagementPage() {
  const api = useApiState("보고서 양식 처리를 완료하지 못했습니다.");
  const [reportId, setReportId] = useState("");
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [rows, setRows] = useState<ReportFormVersion[]>([]);
  const [form, setForm] = useState({
    reportId: "",
    versionName: "",
    effectiveDate: "",
    formFileRef: "",
    changeReason: "",
  });

  const load = async () => {
    try {
      api.start();
      const response = await reportManagementApi.listReportFormVersions({
        reportId,
        size: pageSize,
      });
      setRows(response.data?.formVersions ?? []);
    } catch (caught) {
      api.handle(caught);
    } finally {
      api.finish();
    }
  };
  useEffect(() => {
    void load();
  }, [pageSize]);

  const save = async () => {
    const errors = requiredErrors(form, [
      "reportId",
      "versionName",
      "effectiveDate",
      "formFileRef",
      "changeReason",
    ]);
    if (Object.keys(errors).length > 0) {
      api.setFieldErrors(errors);
      api.setError("필수 입력 항목을 확인하세요.");
      return;
    }
    if (!window.confirm("보고서 양식 버전을 저장하시겠습니까?")) return;
    try {
      await reportManagementApi.saveReportFormVersion(form);
      api.setSuccess("저장 후 이전 버전 포함 목록을 재조회했습니다.");
      await load();
    } catch (caught) {
      api.handle(caught);
    }
  };

  return (
    <ReportScreen
      screenId="SCR-REPORT-FORM-VERSION-MGMT"
      title="보고서 양식 관리"
      menuPath="보고서 관리 > 보고서 관리 > 보고서 양식 관리"
      description="보고서 종류별 양식 버전과 시행일을 보존하고 현재 적용여부를 확인합니다."
      state={api}
      testId="report-form-version-page"
    >
      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="grid gap-4 md:grid-cols-3">
          <TextField
            label="보고서 종류"
            value={reportId}
            onChange={setReportId}
            testId="form-version-report-select"
            placeholder="보고서ID"
          />
          <PageSizeSelect
            value={pageSize}
            onChange={setPageSize}
            testId="form-version-page-size-select"
          />
        </div>
        <ActionButton
          kind="secondary"
          onClick={() => void load()}
          testId="form-version-search-button"
        >
          <Search size={16} />
          조회
        </ActionButton>
      </section>
      <SimpleTable
        testId="form-version-table"
        emptyTitle="조회된 양식 버전이 없습니다"
        loading={api.loading}
        rows={rows}
        columns={[
          "reportName",
          "reportId",
          "versionName",
          "effectiveDate",
          "currentYn",
          "formFileRef",
        ]}
        onSelect={(row) =>
          setForm({
            reportId: row.reportId,
            versionName: row.versionName,
            effectiveDate: row.effectiveDate,
            formFileRef: row.formFileRef,
            changeReason: "",
          })
        }
      />
      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">양식 버전 등록·변경</h2>
        <p className="mt-2 text-sm text-muted">
          이전 버전은 삭제하지 않고 보존합니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-4">
          <TextField
            label="보고서ID*"
            value={form.reportId}
            error={api.fieldErrors.reportId}
            onChange={(value) => setForm({ ...form, reportId: value })}
            testId="form-version-report-id-input"
          />
          <TextField
            label="버전명*"
            value={form.versionName}
            error={api.fieldErrors.versionName}
            onChange={(value) => setForm({ ...form, versionName: value })}
            testId="form-version-name-input"
          />
          <TextField
            label="시행일*"
            type="date"
            value={form.effectiveDate}
            error={api.fieldErrors.effectiveDate}
            onChange={(value) => setForm({ ...form, effectiveDate: value })}
            testId="form-version-effective-date-input"
          />
          <TextField
            label="양식 파일*"
            value={form.formFileRef}
            error={api.fieldErrors.formFileRef}
            onChange={(value) => setForm({ ...form, formFileRef: value })}
            testId="form-version-file-input"
          />
          <TextField
            label="변경사유*"
            value={form.changeReason}
            error={api.fieldErrors.changeReason}
            onChange={(value) => setForm({ ...form, changeReason: value })}
            testId="form-version-change-reason-input"
          />
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <ActionButton
            kind="primary"
            onClick={() => void save()}
            testId="form-version-save-button"
          >
            <Save size={16} />
            저장
          </ActionButton>
          <ActionButton
            kind="secondary"
            onClick={() =>
              setForm({
                reportId: "",
                versionName: "",
                effectiveDate: "",
                formFileRef: "",
                changeReason: "",
              })
            }
            testId="form-version-cancel-button"
          >
            취소
          </ActionButton>
        </div>
      </section>
    </ReportScreen>
  );
}

export function ReportPermissionManagementPage() {
  const api = useApiState("보고서 권한 처리를 완료하지 못했습니다.");
  const [filters, setFilters] = useState({
    granteeType: "ROLE",
    granteeId: "",
    reportId: "",
  });
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [rows, setRows] = useState<ReportPermission[]>([]);

  const load = async () => {
    try {
      api.start();
      const response = await reportManagementApi.listReportPermissions({
        ...filters,
        size: pageSize,
      });
      setRows(response.data?.permissions ?? []);
    } catch (caught) {
      api.handle(caught);
    } finally {
      api.finish();
    }
  };
  useEffect(() => {
    void load();
  }, [pageSize]);

  const save = async () => {
    if (rows.length === 0) {
      api.setError("저장할 권한 행이 없습니다.");
      return;
    }
    if (!window.confirm("보고서 권한을 일괄 저장하시겠습니까?")) return;
    try {
      await reportManagementApi.saveReportPermissions({
        permissions: rows,
        changeReason: "UI 일괄 저장",
      });
      api.setSuccess("권한 matrix 저장 후 재조회했습니다.");
      await load();
    } catch (caught) {
      api.handle(caught);
    }
  };

  return (
    <ReportScreen
      screenId="SCR-REPORT-PERMISSION-MGMT"
      title="보고서 권한 관리"
      menuPath="보고서 관리 > 보고서 관리 > 보고서 권한 관리"
      description="역할·조직·사용자별 허용행위와 데이터 범위를 matrix로 설정합니다. 허용되지 않은 출력형식은 화면에서 비활성/숨김 처리하며 서버 403 ApiError도 표시합니다."
      state={api}
      testId="report-permission-page"
    >
      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="grid gap-4 md:grid-cols-4">
          <SelectField
            label="대상유형"
            value={filters.granteeType}
            onChange={(value) => setFilters({ ...filters, granteeType: value })}
            testId="permission-grantee-type-select"
            options={["ROLE", "ORG", "USER"]}
          />
          <TextField
            label="대상 검색"
            value={filters.granteeId}
            onChange={(value) => setFilters({ ...filters, granteeId: value })}
            testId="permission-grantee-id-input"
          />
          <TextField
            label="보고서"
            value={filters.reportId}
            onChange={(value) => setFilters({ ...filters, reportId: value })}
            testId="permission-report-id-input"
          />
          <PageSizeSelect
            value={pageSize}
            onChange={setPageSize}
            testId="permission-page-size-select"
          />
        </div>
        <ActionButton
          kind="secondary"
          onClick={() => void load()}
          testId="permission-search-button"
        >
          <Search size={16} />
          조회
        </ActionButton>
      </section>
      <PermissionMatrix rows={rows} setRows={setRows} loading={api.loading} />
      <ActionButton
        kind="primary"
        onClick={() => void save()}
        testId="permission-bulk-save-button"
      >
        <ShieldCheck size={16} />
        일괄 저장
      </ActionButton>
    </ReportScreen>
  );
}

export function ReportPrintHistoryPage() {
  const api = useApiState("보고서 출력 이력 조회를 완료하지 못했습니다.");
  const [filters, setFilters] = useState({
    reportId: "",
    requesterId: "",
    fromDate: "",
    toDate: "",
    outputFormat: "",
    resultCode: "",
  });
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [rows, setRows] = useState<ReportPrintHistory[]>([]);

  const load = async () => {
    try {
      api.start();
      const response = await reportManagementApi.listReportPrintHistories({
        reportId: filters.reportId,
        requesterId: filters.requesterId,
        fromDate: filters.fromDate,
        toDate: filters.toDate,
        size: pageSize,
      });
      setRows(response.data?.histories ?? []);
    } catch (caught) {
      api.handle(caught);
    } finally {
      api.finish();
    }
  };
  useEffect(() => {
    void load();
  }, [pageSize]);

  return (
    <ReportScreen
      screenId="SCR-REPORT-PRINT-HISTORY"
      title="보고서 출력 이력"
      menuPath="보고서 관리 > 보고서 관리 > 보고서 출력 이력"
      description="보고서 출력·다운로드 이력을 조회만 합니다. 조회 전용 화면이므로 출력 생성·수정·삭제 CTA가 없습니다."
      state={api}
      testId="report-print-history-page"
    >
      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="grid gap-4 md:grid-cols-5">
          <TextField
            label="보고서ID"
            value={filters.reportId}
            onChange={(value) => setFilters({ ...filters, reportId: value })}
            testId="history-report-id-input"
          />
          <TextField
            label="출력자"
            value={filters.requesterId}
            onChange={(value) => setFilters({ ...filters, requesterId: value })}
            testId="history-requester-input"
          />
          <TextField
            label="출력기간 From"
            type="date"
            value={filters.fromDate}
            onChange={(value) => setFilters({ ...filters, fromDate: value })}
            testId="history-from-date-input"
          />
          <TextField
            label="출력기간 To"
            type="date"
            value={filters.toDate}
            onChange={(value) => setFilters({ ...filters, toDate: value })}
            testId="history-to-date-input"
          />
          <PageSizeSelect
            value={pageSize}
            onChange={setPageSize}
            testId="history-page-size-select"
          />
        </div>
        <ActionButton
          kind="secondary"
          onClick={() => void load()}
          testId="history-search-button"
        >
          <Search size={16} />
          조회
        </ActionButton>
      </section>
      <SimpleTable
        testId="history-table"
        emptyTitle="조회된 출력 이력이 없습니다"
        loading={api.loading}
        rows={rows}
        columns={[
          "reportName",
          "requesterName",
          "targetSummary",
          "outputFormat",
          "outputCount",
          "resultCode",
          "outputAt",
        ]}
      />
    </ReportScreen>
  );
}

export function BulkReportJobManagementPage() {
  const api = useApiState("대량 출력 처리를 완료하지 못했습니다.");
  const [filters, setFilters] = useState({
    reportId: "",
    evaluationYear: "2026",
    organizationCode: "",
    targetPersonId: "",
    outputFormat: "PDF" as ReportOutputFormat,
    outputBaseDate: "2026-01-01",
  });
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [targets, setTargets] = useState<BulkReportTarget[]>([]);
  const [jobs, setJobs] = useState<BulkReportJob[]>([]);
  const [selectedJob, setSelectedJob] = useState<BulkReportJob | null>(null);
  const [resultMessage, setResultMessage] = useState<string | null>(null);

  const targetIds = useMemo(
    () =>
      targets
        .map((target) => target.targetPersonId)
        .filter((id) => Number.isFinite(id)),
    [targets],
  );
  const targetHash = useMemo(
    () =>
      `${filters.reportId}:${filters.evaluationYear}:${filters.organizationCode}:${targetIds.join(",")}`,
    [
      filters.reportId,
      filters.evaluationYear,
      filters.organizationCode,
      targetIds,
    ],
  );

  const loadTargets = async () => {
    try {
      api.start();
      const response = await reportManagementApi.listBulkReportTargets({
        reportId: filters.reportId,
        evaluationYear: filters.evaluationYear,
        organizationCode: filters.organizationCode,
        size: pageSize,
      });
      const previewTargets = response.data?.targets ?? [];
      const targetPersonFilter = filters.targetPersonId.trim();
      const selectedTargetPersonId = Number(targetPersonFilter);
      setTargets(
        targetPersonFilter && Number.isFinite(selectedTargetPersonId)
          ? previewTargets.filter(
              (target) => target.targetPersonId === selectedTargetPersonId,
            )
          : previewTargets,
      );
    } catch (caught) {
      api.handle(caught);
    } finally {
      api.finish();
    }
  };

  const loadJobs = async () => {
    try {
      const response = await reportManagementApi.listBulkReportJobs({
        reportId: filters.reportId,
        size: pageSize,
      });
      setJobs(response.data?.jobs ?? []);
    } catch (caught) {
      api.handle(caught);
    }
  };

  useEffect(() => {
    void loadTargets();
    void loadJobs();
  }, [pageSize]);

  const createJob = async () => {
    if (!filters.reportId || targetIds.length === 0) {
      api.setError("보고서와 대상자 목록을 먼저 선택하세요.");
      return;
    }
    if (
      !window.confirm(
        `권한 범위 대상 ${targetIds.length}건 대량생성을 실행하시겠습니까?`,
      )
    )
      return;
    try {
      api.start();
      const response = await reportManagementApi.createBulkReportJob({
        reportId: filters.reportId,
        targetPersonIds: targetIds,
        targetHash,
        outputFormat: filters.outputFormat,
        outputBaseDate: filters.outputBaseDate,
      });
      if (response.data) setSelectedJob(response.data);
      api.setSuccess(
        `대량 출력 작업이 등록되었습니다. jobId ${response.data?.jobId ?? "-"}`,
      );
      await loadJobs();
    } catch (caught) {
      api.handle(caught);
    } finally {
      api.finish();
    }
  };

  const loadDetail = async (jobId: number) => {
    try {
      const response = await reportManagementApi.getBulkReportJob(jobId);
      setSelectedJob(response.data ?? null);
    } catch (caught) {
      api.handle(caught);
    }
  };

  const downloadResult = async (jobId: number) => {
    try {
      const response =
        await reportManagementApi.downloadBulkReportJobResult(jobId);
      const result = response.data;
      setResultMessage(
        result
          ? `결과파일 다운로드: ${result.resultFileRef ?? "파일"}, 실패 ${result.failCount}건`
          : "결과파일 다운로드 요청이 완료되었습니다.",
      );
    } catch (caught) {
      api.handle(caught);
    }
  };

  return (
    <ReportScreen
      screenId="SCR-BULK-REPORT-JOB-MGMT"
      title="대량 출력 관리"
      menuPath="보고서 관리 > 보고서 관리 > 대량 출력 관리"
      description="권한 범위의 대상자를 선택해 대량 보고서 생성작업을 비동기로 실행하고 진행률·결과파일·실패 상세를 확인합니다."
      state={api}
      testId="bulk-report-job-page"
    >
      <section
        className="rounded-md border border-ld bg-lightsecondary p-4 text-sm text-muted"
        data-testid="bulk-oq-policy-guard"
      >
        <h2 className="font-semibold">미확정 정책은 설정값으로만 격리</h2>
        <p className="mt-2">
          결과파일 보관기간, 권한 병합, 권한 밖 대상 처리, 실패 이력 기록은
          reviewer 확정 전까지 아래 환경변수로만 제어하며 별도 자동삭제나
          원천자료 변경 action은 제공하지 않습니다.
        </p>
        <ul className="mt-2 list-disc pl-5">
          <li>REPORT_RESULT_FILE_RETENTION_DAYS</li>
          <li>REPORT_PERMISSION_MERGE_STRATEGY</li>
          <li>REPORT_UNAUTHORIZED_BULK_TARGET_POLICY</li>
          <li>REPORT_RECORD_FAILED_PRINT_HISTORY</li>
        </ul>
      </section>
      {resultMessage ? (
        <SuccessState title="결과파일 다운로드" message={resultMessage} />
      ) : null}
      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">대상 선택</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-6">
          <TextField
            label="보고서*"
            value={filters.reportId}
            onChange={(value) => setFilters({ ...filters, reportId: value })}
            testId="bulk-report-id-select"
          />
          <TextField
            label="평가연도*"
            value={filters.evaluationYear}
            onChange={(value) =>
              setFilters({ ...filters, evaluationYear: value })
            }
            testId="bulk-year-input"
          />
          <TextField
            label="소속"
            value={filters.organizationCode}
            onChange={(value) =>
              setFilters({ ...filters, organizationCode: value })
            }
            testId="bulk-organization-input"
          />
          <TextField
            label="대상자"
            value={filters.targetPersonId}
            onChange={(value) =>
              setFilters({ ...filters, targetPersonId: value })
            }
            testId="bulk-target-person-input"
          />
          <SelectField
            label="출력형식"
            value={filters.outputFormat}
            onChange={(value) =>
              setFilters({
                ...filters,
                outputFormat: value as ReportOutputFormat,
              })
            }
            testId="bulk-output-format-select"
            options={["PDF", "EXCEL"]}
          />
          <PageSizeSelect
            value={pageSize}
            onChange={setPageSize}
            testId="bulk-page-size-select"
          />
        </div>
        <p className="mt-3 text-sm text-muted">
          대상건수 미리보기: 권한 범위 대상 {targets.length}건
        </p>
        <div className="mt-4 flex flex-wrap gap-2">
          <ActionButton
            kind="secondary"
            onClick={() => void loadTargets()}
            testId="bulk-target-preview-button"
          >
            <Search size={16} />
            대상건수 미리보기
          </ActionButton>
          <ActionButton
            kind="primary"
            onClick={() => void createJob()}
            testId="bulk-create-job-button"
          >
            <RefreshCw size={16} />
            대량생성 실행
          </ActionButton>
        </div>
      </section>
      <SimpleTable
        testId="bulk-target-table"
        emptyTitle="선택된 대상자 목록이 없습니다"
        loading={api.loading}
        rows={targets}
        columns={[
          "targetPersonName",
          "targetPersonId",
          "targetOrganizationCode",
          "resultCode",
          "errorDetail",
        ]}
      />
      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">작업 진행률</h2>
        <div className="mt-4 overflow-x-auto">
          <table
            data-testid="bulk-job-table"
            className="min-w-full divide-y divide-ld text-sm"
          >
            <thead>
              <tr>
                <th>작업ID</th>
                <th>보고서명</th>
                <th>시작일시</th>
                <th>진행률</th>
                <th>성공</th>
                <th>실패</th>
                <th>상태</th>
                <th>상세</th>
                <th>결과</th>
              </tr>
            </thead>
            <tbody>
              {jobs.map((job) => (
                <tr key={job.jobId} data-testid="bulk-job-row">
                  <td>{job.jobId}</td>
                  <td>{job.reportName}</td>
                  <td>{job.requestedAt}</td>
                  <td>{job.progressRate}%</td>
                  <td>{job.successCount}</td>
                  <td>{job.failCount}</td>
                  <td>{job.status}</td>
                  <td>
                    <button
                      type="button"
                      className="text-primary underline"
                      data-testid="bulk-job-detail-button"
                      onClick={() => void loadDetail(job.jobId)}
                    >
                      상세조회
                    </button>
                  </td>
                  <td>
                    <button
                      type="button"
                      className="text-primary underline"
                      data-testid="bulk-result-download-button"
                      onClick={() => void downloadResult(job.jobId)}
                      disabled={job.status !== "COMPLETED"}
                    >
                      결과파일 다운로드
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {jobs.length === 0 ? (
          <EmptyState
            title="조회된 대량 출력 작업이 없습니다"
            message="완료된 작업은 결과파일 다운로드 버튼으로 내려받고, 실패 건은 실패 대상 목록/오류 상세에서 확인합니다."
          />
        ) : null}
      </section>
      <section
        className="rounded-md bg-white p-6 shadow-md"
        data-testid="bulk-failure-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          실패 대상 목록/오류 상세
        </h2>
        <p className="mt-2 text-sm text-muted">
          선택 작업: {selectedJob?.jobId ?? "미선택"} / 실패{" "}
          {selectedJob?.failCount ?? 0}건
        </p>
        <ul className="mt-3 text-sm text-muted">
          {targets
            .filter(
              (target) => target.resultCode === "FAILED" || target.errorDetail,
            )
            .map((target) => (
              <li key={target.jobTargetId} data-testid="bulk-failure-row">
                {target.targetPersonName} -{" "}
                {target.errorDetail ?? "오류 상세 없음"}
              </li>
            ))}
        </ul>
      </section>
    </ReportScreen>
  );
}

function ReportScreen({
  screenId,
  title,
  menuPath,
  description,
  state,
  testId,
  children,
}: {
  screenId: string;
  title: string;
  menuPath: string;
  description: string;
  state: ReturnType<typeof useApiState>;
  testId: string;
  children: React.ReactNode;
}) {
  if (state.permissionDenied) {
    return (
      <section data-screen-id={screenId} data-testid={testId}>
        <PermissionState
          title={`${title} 권한이 없습니다`}
          message="업무 역할 또는 메뉴 권한을 확인하세요."
        />
      </section>
    );
  }
  return (
    <section
      className="space-y-6"
      data-screen-id={screenId}
      data-testid={testId}
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">{menuPath}</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">{title}</h1>
        <p className="mt-2 text-sm text-muted">{description}</p>
      </div>
      {state.success ? (
        <SuccessState title="처리 완료" message={state.success} />
      ) : null}
      {state.error ? (
        <ErrorState title={`${title} 오류`} message={state.error} />
      ) : null}
      {children}
    </section>
  );
}

function ReportTable({
  rows,
  onSelect,
}: {
  rows: ReportRow[];
  onSelect: (row: ReportRow) => void;
}) {
  return (
    <SimpleTable
      testId="report-table"
      emptyTitle="조회된 보고서가 없습니다"
      loading={false}
      rows={rows}
      columns={[
        "reportId",
        "reportName",
        "businessCategory",
        "templateFileRef",
        "datasetCode",
        "activeYn",
      ]}
      onSelect={onSelect}
    />
  );
}

function PermissionMatrix({
  rows,
  setRows,
  loading,
}: {
  rows: ReportPermission[];
  setRows: React.Dispatch<React.SetStateAction<ReportPermission[]>>;
  loading: boolean;
}) {
  if (loading) return <LoadingState title="보고서 권한을 불러오는 중입니다" />;
  const toggle = (
    index: number,
    field: keyof Pick<
      ReportPermission,
      | "allowViewYn"
      | "allowPreviewYn"
      | "allowPrintYn"
      | "allowPdfYn"
      | "allowExcelYn"
    >,
  ) => {
    setRows((current) =>
      current.map((row, rowIndex) =>
        rowIndex === index
          ? { ...row, [field]: row[field] === "Y" ? "N" : "Y" }
          : row,
      ),
    );
  };
  return (
    <section className="rounded-md bg-white p-6 shadow-md">
      <h2 className="text-lg font-semibold text-dark">권한 matrix</h2>
      <div className="mt-4 overflow-x-auto">
        <table
          className="min-w-full divide-y divide-ld text-sm"
          data-testid="permission-matrix-table"
        >
          <thead>
            <tr>
              <th>대상</th>
              <th>조회</th>
              <th>미리보기</th>
              <th>인쇄</th>
              <th>PDF 출력</th>
              <th>엑셀 출력</th>
              <th>데이터 범위</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, index) => (
              <tr key={row.permissionId} data-testid="permission-matrix-row">
                <td>{row.granteeName ?? row.granteeId}</td>
                {(
                  [
                    "allowViewYn",
                    "allowPreviewYn",
                    "allowPrintYn",
                    "allowPdfYn",
                    "allowExcelYn",
                  ] as const
                ).map((field) => (
                  <td key={field}>
                    <input
                      type="checkbox"
                      checked={row[field] === "Y"}
                      onChange={() => toggle(index, field)}
                      data-testid={`permission-${field}-checkbox`}
                    />
                  </td>
                ))}
                <td>
                  <select
                    value={row.dataScope}
                    onChange={(event) =>
                      setRows((current) =>
                        current.map((item, rowIndex) =>
                          rowIndex === index
                            ? { ...item, dataScope: event.target.value }
                            : item,
                        ),
                      )
                    }
                    data-testid="permission-data-scope-select"
                  >
                    {dataScopes.map((scope) => (
                      <option key={scope} value={scope}>
                        {scope}
                      </option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {rows.length === 0 ? (
          <EmptyState
            title="조회된 보고서 권한이 없습니다"
            message="403 또는 미허용 출력형식은 오류 상태로 표시됩니다."
          />
        ) : null}
      </div>
    </section>
  );
}

function SimpleTable<T extends object>({
  testId,
  emptyTitle,
  loading,
  rows,
  columns,
  onSelect,
}: {
  testId: string;
  emptyTitle: string;
  loading: boolean;
  rows: T[];
  columns: Array<keyof T & string>;
  onSelect?: (row: T) => void;
}) {
  if (loading) return <LoadingState title="목록을 불러오는 중입니다" />;
  return (
    <section className="rounded-md bg-white p-6 shadow-md">
      <div className="overflow-x-auto">
        <table
          className="min-w-full divide-y divide-ld text-sm"
          data-testid={testId}
        >
          <thead>
            <tr>
              {onSelect ? <th>선택</th> : null}
              {columns.map((column) => (
                <th key={column}>{columnLabel(column)}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, index) => (
              <tr key={rowKey(row, index)} data-testid={`${testId}-row`}>
                {onSelect ? (
                  <td>
                    <button
                      type="button"
                      className="text-primary underline"
                      onClick={() => onSelect(row)}
                      data-testid={`${testId}-select-button`}
                    >
                      선택
                    </button>
                  </td>
                ) : null}
                {columns.map((column) => (
                  <td key={column}>{formatCell(row[column])}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {rows.length === 0 ? <EmptyState title={emptyTitle} /> : null}
    </section>
  );
}

function TextField({
  label,
  value,
  onChange,
  testId,
  error,
  type = "text",
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  error?: string;
  type?: string;
  placeholder?: string;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      {label}
      <input
        className="form-input mt-1 w-full"
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
        aria-label={label.replace("*", "")}
        aria-invalid={Boolean(error)}
        placeholder={placeholder}
      />
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function SelectField({
  label,
  value,
  onChange,
  testId,
  options,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  options: string[];
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      {label}
      <select
        className="form-input mt-1 w-full"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
        aria-label={label.replace("*", "")}
      >
        {options.map((option) => (
          <option key={option || "ALL"} value={option}>
            {option || "전체"}
          </option>
        ))}
      </select>
    </label>
  );
}

function PageSizeSelect({
  value,
  onChange,
  testId,
}: {
  value: PageSize;
  onChange: (value: PageSize) => void;
  testId: string;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      표시 건수
      <select
        className="form-input mt-1 w-full"
        value={value}
        onChange={(event) => onChange(Number(event.target.value) as PageSize)}
        data-testid={testId}
      >
        {pageSizes.map((size) => (
          <option key={size} value={size}>
            {size}건
          </option>
        ))}
      </select>
    </label>
  );
}

function ActionButton({
  kind,
  onClick,
  testId,
  children,
  disabled = false,
}: {
  kind: "primary" | "secondary";
  onClick: () => void;
  testId: string;
  children: React.ReactNode;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      className={`mt-4 inline-flex h-10 items-center gap-2 rounded-md px-4 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50 ${kind === "primary" ? "bg-primary text-white" : "border border-primary text-primary"}`}
      onClick={onClick}
      data-testid={testId}
      disabled={disabled}
    >
      {children}
    </button>
  );
}

function useApiState(defaultMessage: string) {
  const [loading, setLoading] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const start = () => {
    setLoading(true);
    setError(null);
    setPermissionDenied(false);
    setFieldErrors({});
  };
  const finish = () => setLoading(false);
  const handle = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(caught instanceof Error ? caught.message : defaultMessage);
  };
  return {
    loading,
    permissionDenied,
    error,
    success,
    fieldErrors,
    setError,
    setSuccess,
    setFieldErrors,
    start,
    finish,
    handle,
  };
}

function requiredErrors<T extends Record<string, string>>(
  form: T,
  keys: Array<keyof T>,
) {
  return keys.reduce<Record<string, string>>((errors, key) => {
    if (!form[key]?.trim()) errors[String(key)] = "필수 입력 항목입니다.";
    return errors;
  }, {});
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function formatCell(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "Y" : "N";
  return String(value);
}

function rowKey(row: object, index: number) {
  const record = row as Record<string, unknown>;
  return String(
    record.id ??
      record.reportId ??
      record.formVersionId ??
      record.permissionId ??
      record.jobId ??
      record.jobTargetId ??
      record.printHistoryId ??
      index,
  );
}

function columnLabel(column: string) {
  const labels: Record<string, string> = {
    reportId: "보고서ID",
    reportName: "보고서명",
    businessCategory: "업무구분",
    templateFileRef: "템플릿",
    datasetCode: "데이터셋",
    activeYn: "사용여부",
    versionName: "버전",
    effectiveDate: "시행일",
    currentYn: "현재 적용여부",
    formFileRef: "양식 파일",
    requesterName: "출력자",
    targetSummary: "대상",
    outputFormat: "형식",
    outputCount: "건수",
    resultCode: "결과",
    outputAt: "일시",
    targetPersonName: "대상자",
    targetPersonId: "대상자ID",
    targetOrganizationCode: "소속",
    errorDetail: "오류 상세",
  };
  return labels[column] ?? column;
}

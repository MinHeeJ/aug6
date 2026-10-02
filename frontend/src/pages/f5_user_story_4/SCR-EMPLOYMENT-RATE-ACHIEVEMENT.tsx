import { Download, FileUp, RefreshCw, Save, Upload } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type AchievementStatus = "EVALUATION_CONFIRMED" | string;

type EmploymentRateAchievement = {
  achievementId: number;
  managementNo?: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName?: string | null;
  attachmentIds?: string[] | null;
  status?: AchievementStatus;
  certificationStatus?: AchievementStatus;
};

type ListResponse = {
  achievements?: EmploymentRateAchievement[];
  items?: EmploymentRateAchievement[];
  page?: number;
  pageSize?: number;
  totalElements?: number;
  total?: number;
};

type DetailResponse = {
  achievement?: EmploymentRateAchievement;
  data?: EmploymentRateAchievement;
};

type SaveResponse = {
  achievement?: EmploymentRateAchievement;
  occurredDateWarning?: boolean;
  warningMessage?: string | null;
};

type UploadError = {
  rowNumber: number;
  columnName: string;
  errorReason: string;
};

type UploadResult = {
  uploadId?: string;
  originalFileName?: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  errors?: UploadError[];
  errorFileUrl?: string;
  savedCount?: number;
};

type BulkJob = {
  jobId?: string;
  batchJobId?: string;
  evaluationYear?: string;
  actionType?: "GENERATE" | "DELETE";
  jobStatus?: string;
  totalCount?: number;
  processedCount?: number;
  unprocessedCount?: number;
  items?: BulkJobItem[];
};

type BulkJobItem = {
  targetUserId?: number;
  targetName?: string;
  processedYn?: "Y" | "N";
  unprocessedReason?: string | null;
};

type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementName: string;
  attachmentIds: string;
};

type BulkForm = {
  evaluationYear: string;
  actionType: "GENERATE" | "DELETE";
  targetCondition: string;
};

type Tab = "individual" | "excel" | "bulk";

const initialForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementName: "",
  attachmentIds: "",
};

const initialBulkForm: BulkForm = {
  evaluationYear: "",
  actionType: "GENERATE",
  targetCondition: "{}",
};

/** 취업률 실적의 개별 입력, Excel 원자 반영, 승인된 일괄 작업 결과를 제공한다. */
export function EmploymentRateAchievementPage({
  initialTab = "individual",
}: {
  initialTab?: Tab;
}) {
  const [tab, setTab] = useState<Tab>(initialTab);
  const [rows, setRows] = useState<EmploymentRateAchievement[]>([]);
  const [selected, setSelected] = useState<EmploymentRateAchievement | null>(
    null,
  );
  const [form, setForm] = useState<Form>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [total, setTotal] = useState(0);
  const [file, setFile] = useState<File | null>(null);
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null);
  const [bulkForm, setBulkForm] = useState<BulkForm>(initialBulkForm);
  const [bulkJob, setBulkJob] = useState<BulkJob | null>(null);
  const [confirmingSave, setConfirmingSave] = useState(false);
  const [confirmingBulk, setConfirmingBulk] = useState(false);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const confirmed = statusOf(selected) === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<ListResponse>(listPath(page, pageSize));
      const data = response.data;
      setRows(data?.achievements ?? data?.items ?? []);
      setTotal(data?.totalElements ?? data?.total ?? 0);
    } catch (caught) {
      handleApiError(caught, setPermissionDenied, setError, setFieldErrors);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (tab === "individual") void load();
  }, [page, pageSize, tab]);

  const selectRow = async (row: EmploymentRateAchievement) => {
    try {
      setError(null);
      setFieldErrors({});
      const response = await apiRequest<DetailResponse>(
        `/api/business/employment-rate-achievements/${row.achievementId}`,
      );
      const detail = response.data?.achievement ?? response.data?.data ?? row;
      setSelected(detail);
      setForm(toForm(detail));
    } catch (caught) {
      handleApiError(caught, setPermissionDenied, setError, setFieldErrors);
    }
  };

  const requestSave = () => {
    const errors = validateForm(form);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      setError("필수 입력 항목을 확인하세요.");
      return;
    }
    setConfirmingSave(true);
  };

  const save = async () => {
    setConfirmingSave(false);
    try {
      setProcessing(true);
      setError(null);
      setFieldErrors({});
      const path = selected
        ? `/api/business/employment-rate-achievements/${selected.achievementId}`
        : "/api/business/employment-rate-achievements";
      const response = await apiRequest<SaveResponse>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(toPayload(form)),
      });
      const saved = response.data?.achievement;
      if (saved) {
        setSelected(saved);
        setForm(toForm(saved));
      }
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "평가대상 기간 밖 발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      handleApiError(caught, setPermissionDenied, setError, setFieldErrors);
    } finally {
      setProcessing(false);
    }
  };

  const upload = async () => {
    if (!file) {
      setError("취업률 실적 Excel 파일을 선택하세요.");
      return;
    }
    try {
      setProcessing(true);
      setError(null);
      setUploadResult(null);
      const body = new FormData();
      body.append("file", file);
      const response = await fetch(
        "/api/business/employment-rate-achievements/excel-uploads",
        { method: "POST", body, credentials: "include" },
      );
      const payload = (await response.json()) as {
        success?: boolean;
        data?: UploadResult;
        error?: {
          message?: string;
          fields?: { field: string; message: string }[];
        };
      };
      if (!response.ok || payload.success === false) {
        throw new ApiClientError(
          response.status,
          payload.error?.message ?? "Excel 검증 및 일괄등록에 실패했습니다.",
        );
      }
      setUploadResult(payload.data ?? null);
      const result = payload.data;
      setSuccess(
        result?.errorCount === 0
          ? `${result?.savedCount ?? result?.successCount ?? 0}건을 원자적으로 반영했습니다.`
          : "오류 행이 있어 업무 데이터는 반영하지 않았습니다.",
      );
    } catch (caught) {
      handleApiError(caught, setPermissionDenied, setError, setFieldErrors);
    } finally {
      setProcessing(false);
    }
  };

  const requestBulkJob = () => {
    const errors = validateBulkForm(bulkForm);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      setError("일괄 작업 조건을 확인하세요.");
      return;
    }
    setConfirmingBulk(true);
  };

  const createBulkJob = async () => {
    setConfirmingBulk(false);
    try {
      setProcessing(true);
      setError(null);
      setFieldErrors({});
      const response = await apiRequest<BulkJob>(
        "/api/business/employment-rate-achievements/bulk-jobs",
        {
          method: "POST",
          body: JSON.stringify({
            evaluationYear: bulkForm.evaluationYear.trim(),
            actionType: bulkForm.actionType,
            targetCondition: JSON.parse(bulkForm.targetCondition),
          }),
        },
      );
      setBulkJob(response.data ?? null);
      setSuccess("일괄 작업 요청이 접수되었습니다. 처리 결과를 확인하세요.");
    } catch (caught) {
      if (caught instanceof SyntaxError) {
        setFieldErrors({
          targetCondition: "대상 조건은 올바른 JSON 형식이어야 합니다.",
        });
        setError("일괄 작업 조건을 확인하세요.");
      } else {
        handleApiError(caught, setPermissionDenied, setError, setFieldErrors);
      }
    } finally {
      setProcessing(false);
    }
  };

  const loadBulkJob = async () => {
    const jobId = bulkJob?.jobId ?? bulkJob?.batchJobId;
    if (!jobId) return;
    try {
      setProcessing(true);
      const response = await apiRequest<BulkJob>(
        `/api/business/employment-rate-achievements/bulk-jobs/${encodeURIComponent(jobId)}`,
      );
      setBulkJob(response.data ?? null);
    } catch (caught) {
      handleApiError(caught, setPermissionDenied, setError, setFieldErrors);
    } finally {
      setProcessing(false);
    }
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENTS"
        data-testid="employment-rate-achievement-page"
      >
        <PermissionState
          title="취업률 실적 관리 권한이 없습니다"
          message="개별 실적은 R01, R02 또는 R04, Excel·일괄 처리는 R07 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENTS"
      data-testid="employment-rate-achievement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              취업률 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              개별 실적과 Excel 원자 반영, 승인된 일괄 작업 결과를 관리합니다.
            </p>
          </div>
          <button
            className={
              "inline-flex items-center justify-center gap-2 rounded-md " +
              "bg-primary px-4 py-2 text-sm font-semibold text-white"
            }
            data-testid="employment-rate-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </header>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="취업률 실적 오류" message={error} /> : null}

      <nav aria-label="취업률 실적 관리 탭" className="flex flex-wrap gap-2">
        <TabButton
          active={tab === "individual"}
          onClick={() => setTab("individual")}
        >
          개별 실적
        </TabButton>
        <TabButton active={tab === "excel"} onClick={() => setTab("excel")}>
          Excel 일괄등록
        </TabButton>
        <TabButton active={tab === "bulk"} onClick={() => setTab("bulk")}>
          일괄 생성·삭제
        </TabButton>
      </nav>

      {tab === "individual" ? (
        <IndividualPanel
          confirmed={confirmed}
          fieldErrors={fieldErrors}
          form={form}
          loading={loading}
          page={page}
          pageSize={pageSize}
          processing={processing}
          rows={rows}
          selected={selected}
          total={total}
          onFormChange={setForm}
          onPageSizeChange={(value) => {
            setPageSize(value);
            setPage(0);
          }}
          onRequestSave={requestSave}
          onSelectRow={(row) => void selectRow(row)}
        />
      ) : null}

      {tab === "excel" ? (
        <ExcelPanel
          file={file}
          processing={processing}
          result={uploadResult}
          onFileChange={setFile}
          onUpload={() => void upload()}
        />
      ) : null}

      {tab === "bulk" ? (
        <BulkPanel
          bulkForm={bulkForm}
          bulkJob={bulkJob}
          fieldErrors={fieldErrors}
          processing={processing}
          onBulkFormChange={setBulkForm}
          onLoadJob={() => void loadBulkJob()}
          onRequestJob={requestBulkJob}
        />
      ) : null}

      {confirmingSave ? (
        <ConfirmationDialog
          cancelTestId="employment-rate-save-cancel-button"
          confirmTestId="employment-rate-save-confirm-button"
          message="입력한 취업률 실적을 저장하시겠습니까?"
          testId="employment-rate-save-confirmation"
          title="저장 확인"
          onCancel={() => setConfirmingSave(false)}
          onConfirm={() => void save()}
        />
      ) : null}

      {confirmingBulk ? (
        <ConfirmationDialog
          cancelTestId="employment-rate-bulk-cancel-button"
          confirmTestId="employment-rate-bulk-confirm-button"
          message="대상 조건 미리보기를 확인했습니다. 일괄 작업을 요청하시겠습니까?"
          testId="employment-rate-bulk-confirmation"
          title="일괄 작업 확인"
          onCancel={() => setConfirmingBulk(false)}
          onConfirm={() => void createBulkJob()}
        />
      ) : null}
    </section>
  );
}

function IndividualPanel({
  confirmed,
  fieldErrors,
  form,
  loading,
  page,
  pageSize,
  processing,
  rows,
  selected,
  total,
  onFormChange,
  onPageSizeChange,
  onRequestSave,
  onSelectRow,
}: {
  confirmed: boolean;
  fieldErrors: Record<string, string>;
  form: Form;
  loading: boolean;
  page: number;
  pageSize: 20 | 50 | 100;
  processing: boolean;
  rows: EmploymentRateAchievement[];
  selected: EmploymentRateAchievement | null;
  total: number;
  onFormChange: (form: Form) => void;
  onPageSizeChange: (value: 20 | 50 | 100) => void;
  onRequestSave: () => void;
  onSelectRow: (row: EmploymentRateAchievement) => void;
}) {
  return (
    <>
      <section className="rounded-md border border-ld bg-white p-5 shadow-sm">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">취업률 실적 목록</h2>
          <div className="flex items-center gap-2">
            <label className="text-sm text-muted">
              표시 건수
              <select
                className="ml-2 rounded-md border border-ld px-2 py-1"
                data-testid="employment-rate-page-size-select"
                value={pageSize}
                onChange={(event) =>
                  onPageSizeChange(Number(event.target.value) as 20 | 50 | 100)
                }
              >
                {[20, 50, 100].map((value) => (
                  <option key={value} value={value}>
                    {value}건
                  </option>
                ))}
              </select>
            </label>
            <a
              className={
                "inline-flex items-center gap-2 rounded-md border border-ld " +
                "px-3 py-2 text-sm font-semibold text-link"
              }
              data-testid="employment-rate-download-link"
              href={downloadPath(page, pageSize)}
            >
              <Download size={16} /> Excel 다운로드
            </a>
          </div>
        </div>
        {loading ? <LoadingState title="취업률 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 취업률 실적이 없습니다"
            message="상세 영역에서 새 실적을 등록하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리번호</th>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">업적발생일</th>
                  <th className="px-3 py-2">실적명</th>
                  <th className="px-3 py-2">상태</th>
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr data-testid="employment-rate-row" key={row.achievementId}>
                    <td className="px-3 py-2">{row.managementNo ?? "-"}</td>
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{row.achievementName ?? "-"}</td>
                    <td className="px-3 py-2">{statusOf(row) || "작성중"}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid="employment-rate-detail-button"
                        onClick={() => onSelectRow(row)}
                        type="button"
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="mt-3 text-xs text-muted">
          총 {total}건 / {page + 1}페이지
        </p>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="employment-rate-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">취업률 실적 상세</h2>
        <p className="mt-2 text-sm text-muted">
          관리항목과 업적발생일은 필수입니다. 평가대상 기간 밖 날짜는 서버
          경고와 함께 저장될 수 있습니다.
        </p>
        {confirmed ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="employment-rate-confirmed-lock-message"
          >
            평가확정 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            label="관리항목"
            required
            error={fieldErrors.managementItemCode}
          >
            <input
              data-testid="employment-rate-management-item-input"
              disabled={confirmed}
              value={form.managementItemCode}
              onChange={(event) =>
                onFormChange({
                  ...form,
                  managementItemCode: event.target.value,
                })
              }
            />
          </Field>
          <Field
            label="업적발생일"
            required
            error={fieldErrors.achievementDate}
          >
            <input
              data-testid="employment-rate-achievement-date-input"
              disabled={confirmed}
              type="date"
              value={form.achievementDate}
              onChange={(event) =>
                onFormChange({ ...form, achievementDate: event.target.value })
              }
            />
          </Field>
          <Field label="실적명" error={fieldErrors.achievementName}>
            <input
              data-testid="employment-rate-achievement-name-input"
              disabled={confirmed}
              value={form.achievementName}
              onChange={(event) =>
                onFormChange({ ...form, achievementName: event.target.value })
              }
            />
          </Field>
          <Field
            label="첨부파일 ID (쉼표로 구분)"
            error={fieldErrors.attachmentIds}
          >
            <input
              data-testid="employment-rate-attachment-ids-input"
              disabled={confirmed}
              value={form.attachmentIds}
              onChange={(event) =>
                onFormChange({ ...form, attachmentIds: event.target.value })
              }
            />
          </Field>
          <Field label="상태">
            <input disabled value={statusOf(selected) || "작성중"} />
          </Field>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className={
              "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 " +
              "text-sm font-semibold text-white disabled:opacity-60"
            }
            data-testid="employment-rate-save-button"
            disabled={processing || confirmed}
            onClick={onRequestSave}
            type="button"
          >
            <Save size={16} /> {processing ? "저장 중" : "저장"}
          </button>
        </div>
      </section>
    </>
  );
}

function ExcelPanel({
  file,
  processing,
  result,
  onFileChange,
  onUpload,
}: {
  file: File | null;
  processing: boolean;
  result: UploadResult | null;
  onFileChange: (file: File | null) => void;
  onUpload: () => void;
}) {
  return (
    <section
      className="rounded-md border border-ld bg-white p-5 shadow-sm"
      data-testid="employment-rate-excel-panel"
    >
      <h2 className="text-lg font-semibold text-dark">
        취업률 실적 Excel 일괄등록
      </h2>
      <p className="mt-2 text-sm text-muted">
        업무 템플릿으로 작성한 파일을 업로드하면 모든 행을 먼저 검증합니다. 오류
        또는 중복 행이 하나라도 있으면 업무 데이터는 0건 반영됩니다.
      </p>
      <div className="mt-4 flex flex-wrap items-center gap-3">
        <input
          accept=".xlsx,.xls,.csv"
          data-testid="employment-rate-excel-file-input"
          onChange={(event) => onFileChange(event.target.files?.[0] ?? null)}
          type="file"
        />
        <button
          className={
            "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 " +
            "text-sm font-semibold text-white disabled:opacity-60"
          }
          data-testid="employment-rate-excel-upload-button"
          disabled={!file || processing}
          onClick={onUpload}
          type="button"
        >
          <Upload size={16} /> {processing ? "검증 중" : "업로드·검증·반영"}
        </button>
      </div>
      {result ? (
        <section
          className="mt-5 rounded-md border border-ld p-4"
          data-testid="employment-rate-excel-result"
        >
          <h3 className="font-semibold text-dark">검증 및 반영 결과</h3>
          <p className="mt-2 text-sm text-muted">
            정상행 {result.successCount}건 / 오류행 {result.errorCount}건 / 전체{" "}
            {result.totalCount}건
          </p>
          {result.errorCount > 0 ? (
            <div className="mt-3 text-sm text-error">
              <p>오류 행이 있어 전체 반영하지 않았습니다.</p>
              {result.errorFileUrl ? (
                <a
                  className="mt-3 inline-flex items-center gap-2 rounded-md border border-error px-3 py-2"
                  data-testid="employment-rate-error-download-link"
                  href={result.errorFileUrl}
                >
                  <Download size={16} /> 오류파일 다운로드
                </a>
              ) : null}
              <ul className="mt-3 list-disc pl-5">
                {(result.errors ?? []).map((item) => (
                  <li key={`${item.rowNumber}-${item.columnName}`}>
                    {item.rowNumber}행 {item.columnName}: {item.errorReason}
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <p className="mt-3 text-sm text-success">
              오류 0건을 확인하여 {result.savedCount ?? result.successCount}건을
              원자적으로 반영했습니다.
            </p>
          )}
        </section>
      ) : null}
    </section>
  );
}

function BulkPanel({
  bulkForm,
  bulkJob,
  fieldErrors,
  processing,
  onBulkFormChange,
  onLoadJob,
  onRequestJob,
}: {
  bulkForm: BulkForm;
  bulkJob: BulkJob | null;
  fieldErrors: Record<string, string>;
  processing: boolean;
  onBulkFormChange: (form: BulkForm) => void;
  onLoadJob: () => void;
  onRequestJob: () => void;
}) {
  return (
    <section
      className="rounded-md border border-ld bg-white p-5 shadow-sm"
      data-testid="employment-rate-bulk-panel"
    >
      <h2 className="text-lg font-semibold text-dark">
        취업률 실적 일괄 생성·삭제
      </h2>
      <p className="mt-2 text-sm text-muted">
        실행 정책과 삭제 허용 상태는 승인 전입니다. 아래 미리보기는 요청 조건만
        확인하며, 서버 정책이 확정되지 않으면 작업은 접수되지 않습니다.
      </p>
      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <Field label="평가연도" required error={fieldErrors.evaluationYear}>
          <input
            data-testid="employment-rate-bulk-evaluation-year-input"
            value={bulkForm.evaluationYear}
            onChange={(event) =>
              onBulkFormChange({
                ...bulkForm,
                evaluationYear: event.target.value,
              })
            }
          />
        </Field>
        <Field label="작업 유형" required error={fieldErrors.actionType}>
          <select
            data-testid="employment-rate-bulk-action-type-select"
            value={bulkForm.actionType}
            onChange={(event) =>
              onBulkFormChange({
                ...bulkForm,
                actionType: event.target.value as BulkForm["actionType"],
              })
            }
          >
            <option value="GENERATE">일괄 생성</option>
            <option value="DELETE">일괄 삭제</option>
          </select>
        </Field>
        <div className="md:col-span-2">
          <Field label="대상 조건(JSON)" error={fieldErrors.targetCondition}>
            <textarea
              className="min-h-28"
              data-testid="employment-rate-bulk-target-condition-input"
              value={bulkForm.targetCondition}
              onChange={(event) =>
                onBulkFormChange({
                  ...bulkForm,
                  targetCondition: event.target.value,
                })
              }
            />
          </Field>
        </div>
      </div>
      <section
        className="mt-4 rounded-md bg-lightsecondary p-4 text-sm"
        data-testid="employment-rate-bulk-preview"
      >
        <h3 className="font-semibold text-dark">요청 조건 미리보기</h3>
        <p className="mt-2">평가연도: {bulkForm.evaluationYear || "미입력"}</p>
        <p>
          작업 유형:{" "}
          {bulkForm.actionType === "GENERATE" ? "일괄 생성" : "일괄 삭제"}
        </p>
        <p className="break-all">
          대상 조건: {bulkForm.targetCondition || "{}"}
        </p>
      </section>
      <div className="mt-5 flex justify-end">
        <button
          className={
            "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 " +
            "text-sm font-semibold text-white disabled:opacity-60"
          }
          data-testid="employment-rate-bulk-request-button"
          disabled={processing}
          onClick={onRequestJob}
          type="button"
        >
          <FileUp size={16} /> 작업 요청
        </button>
      </div>
      {bulkJob ? (
        <section
          className="mt-5 rounded-md border border-ld p-4"
          data-testid="employment-rate-bulk-result"
        >
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 className="font-semibold text-dark">일괄 처리 결과</h3>
              <p className="mt-2 text-sm text-muted">
                작업 ID: {bulkJob.jobId ?? bulkJob.batchJobId ?? "-"} / 상태:{" "}
                {bulkJob.jobStatus ?? "-"}
              </p>
            </div>
            <button
              className="rounded border border-primary px-3 py-2 text-sm font-semibold text-primary"
              data-testid="employment-rate-bulk-result-refresh-button"
              disabled={processing}
              onClick={onLoadJob}
              type="button"
            >
              결과 새로고침
            </button>
          </div>
          <p className="mt-3 text-sm">
            처리 {bulkJob.processedCount ?? 0}건 / 미처리{" "}
            {bulkJob.unprocessedCount ?? 0}건 / 전체 {bulkJob.totalCount ?? 0}건
          </p>
          {(bulkJob.items ?? []).length > 0 ? (
            <div className="mt-3 overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">대상</th>
                    <th className="px-3 py-2">처리 여부</th>
                    <th className="px-3 py-2">미처리 사유</th>
                  </tr>
                </thead>
                <tbody>
                  {(bulkJob.items ?? []).map((item, index) => (
                    <tr
                      data-testid="employment-rate-bulk-result-row"
                      key={`${item.targetUserId ?? index}`}
                    >
                      <td className="px-3 py-2">
                        {item.targetName ?? item.targetUserId ?? "-"}
                      </td>
                      <td className="px-3 py-2">
                        {item.processedYn === "Y" ? "처리됨" : "미처리"}
                      </td>
                      <td className="px-3 py-2">
                        {item.unprocessedReason ?? "-"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      ) : null}
    </section>
  );
}

function ConfirmationDialog({
  cancelTestId,
  confirmTestId,
  message,
  testId,
  title,
  onCancel,
  onConfirm,
}: {
  cancelTestId: string;
  confirmTestId: string;
  message: string;
  testId: string;
  title: string;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  return (
    <section
      aria-modal="true"
      className="rounded-md border border-primary bg-white p-5 shadow-md"
      data-testid={testId}
      role="dialog"
    >
      <h2 className="text-lg font-semibold text-dark">{title}</h2>
      <p className="mt-2 text-sm text-muted">{message}</p>
      <div className="mt-4 flex justify-end gap-2">
        <button data-testid={cancelTestId} onClick={onCancel} type="button">
          취소
        </button>
        <button
          className="rounded-md bg-primary px-4 py-2 text-white"
          data-testid={confirmTestId}
          onClick={onConfirm}
          type="button"
        >
          확인
        </button>
      </div>
    </section>
  );
}

function TabButton({
  active,
  children,
  onClick,
}: {
  active: boolean;
  children: ReactNode;
  onClick: () => void;
}) {
  return (
    <button
      aria-selected={active}
      className={
        active
          ? "rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
          : "rounded-md border border-ld px-4 py-2 text-sm font-semibold text-muted"
      }
      onClick={onClick}
      role="tab"
      type="button"
    >
      {children}
    </button>
  );
}

function Field({
  label,
  required = false,
  error,
  children,
}: {
  label: string;
  required?: boolean;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ml-1 text-error">*</span> : null}
      <span
        className={
          "mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border " +
          "[&_input]:border-ld [&_input]:px-3 [&_input]:py-2 [&_input]:text-sm " +
          "[&_select]:w-full [&_select]:rounded-md [&_select]:border " +
          "[&_select]:border-ld [&_select]:px-3 [&_select]:py-2 [&_textarea]:w-full " +
          "[&_textarea]:rounded-md [&_textarea]:border [&_textarea]:border-ld " +
          "[&_textarea]:px-3 [&_textarea]:py-2"
        }
      >
        {children}
      </span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function listPath(page: number, pageSize: number) {
  return `/api/business/employment-rate-achievements?page=${page}&pageSize=${pageSize}` as `/api/${string}`;
}

function downloadPath(page: number, pageSize: number) {
  return `/api/business/employment-rate-achievements/download?page=${page}&pageSize=${pageSize}`;
}

function statusOf(row: EmploymentRateAchievement | null) {
  return row?.status ?? row?.certificationStatus ?? "";
}

function toForm(row: EmploymentRateAchievement): Form {
  return {
    managementItemCode: row.managementItemCode,
    achievementDate: row.achievementDate,
    achievementName: row.achievementName ?? "",
    attachmentIds: row.attachmentIds?.join(", ") ?? "",
  };
}

function toPayload(form: Form) {
  const attachmentIds = form.attachmentIds
    .split(",")
    .map((attachmentId) => attachmentId.trim())
    .filter(Boolean);
  return {
    managementItemCode: form.managementItemCode.trim(),
    achievementDate: form.achievementDate,
    achievementName: form.achievementName.trim() || undefined,
    attachmentIds: attachmentIds.length > 0 ? attachmentIds : undefined,
  };
}

function validateForm(form: Form) {
  const errors: Record<string, string> = {};
  if (!form.managementItemCode.trim())
    errors.managementItemCode = "관리항목은 필수입니다.";
  if (!form.achievementDate)
    errors.achievementDate = "업적발생일은 필수입니다.";
  return errors;
}

function validateBulkForm(form: BulkForm) {
  const errors: Record<string, string> = {};
  if (!/^\d{4}$/.test(form.evaluationYear.trim())) {
    errors.evaluationYear = "평가연도는 4자리 숫자로 입력하세요.";
  }
  try {
    JSON.parse(form.targetCondition);
  } catch {
    errors.targetCondition = "대상 조건은 올바른 JSON 형식이어야 합니다.";
  }
  return errors;
}

function handleApiError(
  caught: unknown,
  setPermissionDenied: (value: boolean) => void,
  setError: (value: string | null) => void,
  setFieldErrors: (value: Record<string, string>) => void,
) {
  if (caught instanceof ApiClientError) {
    setPermissionDenied(caught.status === 403);
    setError(caught.message);
    setFieldErrors(
      Object.fromEntries(
        (caught.apiError?.fields ?? []).map((field) => [
          field.field,
          field.message,
        ]),
      ),
    );
    return;
  }
  setError(
    caught instanceof Error
      ? caught.message
      : "취업률 실적을 처리하지 못했습니다.",
  );
}

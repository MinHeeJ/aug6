import { Download, RefreshCw, Save, Search, Upload } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type Achievement = {
  achievementId: number;
  managementNo: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName?: string | null;
  achievementStatus: string;
};

type ListResponse = {
  achievements: Achievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type SaveResponse = {
  achievement: Achievement;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
type BulkJob = {
  batchJobId: string;
  jobStatus: string;
  targetCount: number;
  processedCount: number;
};

const initialForm = {
  managementItemCode: "",
  achievementDate: "",
  achievementName: "",
};

/** Employment-rate achievement screen with individual, Excel, and policy-gated batch actions. */
export function EmploymentRateAchievementsPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [form, setForm] = useState(initialForm);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [file, setFile] = useState<File | null>(null);
  const [bulkJob, setBulkJob] = useState<BulkJob | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiRequest<ListResponse>(
        `/api/business/employment-rate-achievements?page=0&pageSize=${pageSize}`,
      );
      setRows(response.data?.achievements ?? []);
      setPermissionDenied(false);
    } catch (caught) {
      applyError(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const select = async (row: Achievement) => {
    try {
      const response = await apiRequest<Achievement>(
        `/api/business/employment-rate-achievements/${row.achievementId}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm({
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        achievementName: detail.achievementName ?? "",
      });
    } catch (caught) {
      applyError(caught);
    }
  };

  const save = async () => {
    if (!window.confirm("취업률 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setFieldErrors({});
      const path = selected
        ? `/api/business/employment-rate-achievements/${selected.achievementId}`
        : "/api/business/employment-rate-achievements";
      const response = await apiRequest<SaveResponse>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({ ...form, attachmentIds: [] }),
      });
      setSelected(response.data?.achievement ?? null);
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "평가대상 기간 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      applyError(caught);
    } finally {
      setSaving(false);
    }
  };

  const upload = async () => {
    if (!file) {
      setFieldErrors({ file: "업로드 파일을 선택하세요." });
      return;
    }
    if (!window.confirm("검증을 시작하시겠습니까?")) return;
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await apiRequest<{
        errorCount: number;
        successCount: number;
      }>("/api/business/employment-rate-achievements/excel-uploads", {
        method: "POST",
        body,
      });
      setSuccess(
        response.data?.errorCount === 0
          ? `${response.data?.successCount ?? 0}건의 검증이 완료되었습니다.`
          : "오류 행이 있어 전체 반영하지 않습니다.",
      );
    } catch (caught) {
      applyError(caught);
    }
  };

  const requestBulk = async () => {
    if (!window.confirm("대상 미리보기와 실행조건을 확인했습니까?")) return;
    try {
      const response = await apiRequest<BulkJob>(
        "/api/business/employment-rate-achievements/bulk-jobs",
        {
          method: "POST",
          body: JSON.stringify({
            evaluationYear: new Date().getFullYear().toString(),
            actionType: "GENERATE",
          }),
        },
      );
      setBulkJob(response.data ?? null);
      setSuccess("일괄 작업이 접수되었습니다.");
    } catch (caught) {
      applyError(caught);
    }
  };

  const applyError = (caught: unknown) => {
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
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENTS"
        data-testid="employment-rate-achievements-page"
      >
        <PermissionState
          title="취업률 실적 관리 권한이 없습니다"
          message="업적 조회는 R01/R02/R04, Excel·일괄 처리는 R07 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENTS"
      data-testid="employment-rate-achievements-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              취업률 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              개별 실적, Excel 검증, 일괄 작업 결과를 관리합니다.
            </p>
          </div>
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
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

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-list-panel"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">취업률 실적 목록</h2>
          <div className="flex items-center gap-2">
            <select
              data-testid="employment-rate-page-size-select"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
            <a
              className="inline-flex items-center gap-2 rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="employment-rate-download-link"
              href={`/api/business/employment-rate-achievements/download?page=0&pageSize=${pageSize}`}
            >
              <Download size={16} /> Excel 다운로드
            </a>
          </div>
        </div>
        {loading ? <LoadingState title="취업률 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 취업률 실적이 없습니다"
            message="상세 영역에서 새 실적을 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>관리항목</th>
                  <th>업적발생일</th>
                  <th>실적명</th>
                  <th>상태</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    data-testid="employment-rate-achievement-row"
                    key={row.achievementId}
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.achievementDate}</td>
                    <td>{row.achievementName}</td>
                    <td>{row.achievementStatus}</td>
                    <td>
                      <button
                        data-testid="employment-rate-detail-button"
                        onClick={() => void select(row)}
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
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">개별 취업률 실적</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <Field label="관리항목" error={fieldErrors.managementItemCode}>
            <input
              data-testid="employment-rate-management-item-input"
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            />
          </Field>
          <Field label="업적발생일" error={fieldErrors.achievementDate}>
            <input
              data-testid="employment-rate-date-input"
              type="date"
              value={form.achievementDate}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
            />
          </Field>
          <Field label="실적명">
            <input
              data-testid="employment-rate-name-input"
              value={form.achievementName}
              onChange={(event) =>
                setForm({ ...form, achievementName: event.target.value })
              }
            />
          </Field>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="employment-rate-save-button"
            disabled={saving}
            onClick={() => void save()}
            type="button"
          >
            <Save size={16} /> {saving ? "저장 중" : "저장"}
          </button>
        </div>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-upload-panel"
      >
        <h2 className="text-lg font-semibold text-dark">Excel 일괄등록</h2>
        <p className="mt-2 text-sm text-muted">
          파일을 검증하며 오류 또는 중복 행이 있으면 전체를 반영하지 않습니다.
        </p>
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <input
            data-testid="employment-rate-file-input"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
            type="file"
          />
          <button
            className="inline-flex items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm text-primary"
            data-testid="employment-rate-upload-button"
            onClick={() => void upload()}
            type="button"
          >
            <Upload size={16} /> 검증 및 업로드
          </button>
        </div>
        {fieldErrors.file ? (
          <p className="mt-2 text-sm text-error">{fieldErrors.file}</p>
        ) : null}
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-bulk-panel"
      >
        <h2 className="text-lg font-semibold text-dark">일괄 생성·삭제</h2>
        <p className="mt-2 text-sm text-muted">
          승인된 실행조건만 접수할 수 있습니다.
        </p>
        <button
          className={
            "mt-4 inline-flex items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm text-primary"
          }
          data-testid="employment-rate-bulk-request-button"
          onClick={() => void requestBulk()}
          type="button"
        >
          <Search size={16} /> 대상 미리보기 확인 후 접수
        </button>
        {bulkJob ? (
          <p className="mt-3 text-sm" data-testid="employment-rate-bulk-result">
            {bulkJob.batchJobId}: {bulkJob.processedCount}/{bulkJob.targetCount}
            건 처리 ({bulkJob.jobStatus})
          </p>
        ) : null}
      </section>
    </section>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      <span
        className={
          "mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border " +
          "[&_input]:border-ld [&_input]:px-3 [&_input]:py-2"
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

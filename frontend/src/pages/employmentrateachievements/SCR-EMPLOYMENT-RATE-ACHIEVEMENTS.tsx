import { Download, FileUp, Save, Search, Send } from "lucide-react";
import { useEffect, useState } from "react";
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
  attachmentIds: string[];
  achievementStatus: string;
};

type SearchResponse = {
  achievements: Achievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type UploadResult = {
  uploadId: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  errors: Array<{ rowNumber: number; columnName: string; errorReason: string }>;
};

type BulkJob = {
  jobId: string;
  evaluationYear: string;
  actionType: "GENERATE" | "DELETE";
  jobStatus: string;
  totalCount: number;
  processedCount: number;
  successCount: number;
  failureCount: number;
};

type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementName: string;
  attachmentIds: string;
};

const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementName: "",
  attachmentIds: "",
};

/**
 * Provides the approved individual, upload, and pending-policy bulk-result UI
 * for SCR-EMPLOYMENT-RATE-ACHIEVEMENTS using relative API requests only.
 */
export function EmploymentRateAchievementsPage() {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [file, setFile] = useState<File | null>(null);
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null);
  const [jobId, setJobId] = useState("");
  const [bulkJob, setBulkJob] = useState<BulkJob | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<SearchResponse>(
        `/api/business/employment-rate-achievements?page=0&pageSize=${pageSize}`,
      );
      setRows(response.data?.achievements ?? []);
    } catch (caught) {
      handleError(caught);
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
        attachmentIds: detail.attachmentIds.join(", "),
      });
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    if (!window.confirm("취업률 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const payload = {
        managementItemCode: form.managementItemCode.trim(),
        achievementDate: form.achievementDate,
        achievementName: form.achievementName.trim() || undefined,
        attachmentIds: form.attachmentIds
          .split(",")
          .map((value) => value.trim())
          .filter(Boolean),
      };
      const path = selected
        ? `/api/business/employment-rate-achievements/${selected.achievementId}`
        : "/api/business/employment-rate-achievements";
      const response = await apiRequest<Achievement>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(payload),
      });
      setSelected(response.data ?? null);
      setSuccess(
        selected
          ? "취업률 실적을 수정했습니다."
          : "취업률 실적을 저장했습니다.",
      );
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const upload = async () => {
    if (!file) {
      setError("업로드할 파일을 선택하세요.");
      return;
    }
    try {
      setSaving(true);
      const data = new FormData();
      data.append("file", file);
      const response = await fetch(
        "/api/business/employment-rate-achievements/excel-uploads",
        { method: "POST", body: data, credentials: "include" },
      );
      const body = await response.json();
      if (!response.ok || body.success === false) {
        throw new ApiClientError(
          response.status,
          body.error?.message ?? "업로드에 실패했습니다.",
          body.error,
        );
      }
      setUploadResult(body.data as UploadResult);
      setSuccess(
        body.data.errorCount === 0
          ? "전체 행을 반영했습니다."
          : "오류 행이 있어 업무 데이터는 반영하지 않았습니다.",
      );
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const requestBulkJob = async (actionType: "GENERATE" | "DELETE") => {
    if (
      !window.confirm(
        `${actionType === "GENERATE" ? "생성" : "삭제"} 작업을 요청하시겠습니까?`,
      )
    )
      return;
    try {
      setError(null);
      await apiRequest<BulkJob>(
        "/api/business/employment-rate-achievements/bulk-jobs",
        {
          method: "POST",
          body: JSON.stringify({
            evaluationYear: String(new Date().getFullYear()),
            actionType,
            targetCondition: {},
          }),
        },
      );
    } catch (caught) {
      handleError(caught);
    }
  };

  const loadBulkJob = async () => {
    if (!jobId.trim()) {
      setError("조회할 작업 ID를 입력하세요.");
      return;
    }
    try {
      const response = await apiRequest<BulkJob>(
        `/api/business/employment-rate-achievements/bulk-jobs/${encodeURIComponent(jobId.trim())}`,
      );
      setBulkJob(response.data ?? null);
    } catch (caught) {
      handleError(caught);
    }
  };

  const handleError = (caught: unknown) => {
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
      caught instanceof Error ? caught.message : "처리 중 오류가 발생했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-testid="employment-rate-achievements-page">
        <PermissionState
          title="취업률 실적 관리 권한이 없습니다"
          message="개별 조회는 R01, R02, R04이며 Excel·일괄 기능은 R07 역할이 필요합니다."
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
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          개별 실적, Excel 일괄등록, 일괄 처리결과를 관리합니다.
        </p>
      </header>
      {loading ? <LoadingState title="취업률 실적 조회 중" /> : null}
      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="취업률 실적 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-list-panel"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">취업률 실적 목록</h2>
          <div className="flex items-center gap-2">
            <select
              data-testid="employment-rate-page-size-select"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
            <a
              className="inline-flex items-center gap-2 rounded-md border border-primary px-3 py-2 text-sm text-primary"
              data-testid="employment-rate-download-link"
              href={`/api/business/employment-rate-achievements/download?page=0&pageSize=${pageSize}`}
            >
              <Download size={16} /> Excel 다운로드
            </a>
          </div>
        </div>
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 취업률 실적이 없습니다" />
        ) : null}
        {rows.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>관리항목</th>
                  <th>발생일</th>
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
                    <td>{row.achievementName ?? "-"}</td>
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
        <h2 className="text-lg font-semibold text-dark">개별 실적 입력</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <label>
            관리항목 *
            <input
              data-testid="employment-rate-management-item-input"
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            />
          </label>
          <label>
            업적발생일 *
            <input
              data-testid="employment-rate-date-input"
              type="date"
              value={form.achievementDate}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
            />
          </label>
          <label>
            실적명
            <input
              data-testid="employment-rate-name-input"
              value={form.achievementName}
              onChange={(event) =>
                setForm({ ...form, achievementName: event.target.value })
              }
            />
          </label>
          <label>
            첨부 참조
            <input
              data-testid="employment-rate-attachments-input"
              value={form.attachmentIds}
              onChange={(event) =>
                setForm({ ...form, attachmentIds: event.target.value })
              }
            />
          </label>
        </div>
        {Object.entries(fieldErrors).map(([field, message]) => (
          <p className="text-sm text-error" key={field}>
            {message}
          </p>
        ))}
        <button
          className="mt-4 inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-white"
          data-testid="employment-rate-save-button"
          disabled={saving}
          onClick={() => void save()}
          type="button"
        >
          <Save size={16} /> 저장
        </button>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-upload-panel"
      >
        <h2 className="text-lg font-semibold text-dark">Excel 일괄등록</h2>
        <p className="mt-2 text-sm text-muted">
          오류 행이 하나라도 있으면 업무 데이터는 전체 반영하지 않습니다.
        </p>
        <div className="mt-4 flex flex-wrap gap-3">
          <input
            accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            data-testid="employment-rate-upload-file-input"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
            type="file"
          />
          <button
            className="inline-flex items-center gap-2 rounded-md border border-primary px-4 py-2 text-primary"
            data-testid="employment-rate-upload-button"
            disabled={saving}
            onClick={() => void upload()}
            type="button"
          >
            <FileUp size={16} /> 업로드·검증
          </button>
        </div>
        {uploadResult ? (
          <p
            className="mt-3 text-sm"
            data-testid="employment-rate-upload-result"
          >
            정상 {uploadResult.successCount}건 / 오류 {uploadResult.errorCount}
            건
          </p>
        ) : null}
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="employment-rate-bulk-panel"
      >
        <h2 className="text-lg font-semibold text-dark">일괄 처리결과</h2>
        <p className="mt-2 text-sm text-muted">
          일괄 생성·삭제는 승인된 실행 정책이 확정되기 전까지 접수할 수
          없습니다.
        </p>
        <div className="mt-4 flex flex-wrap gap-3">
          <button
            data-testid="employment-rate-bulk-generate-button"
            onClick={() => void requestBulkJob("GENERATE")}
            type="button"
          >
            <Send size={16} /> 생성 요청
          </button>
          <button
            data-testid="employment-rate-bulk-delete-button"
            onClick={() => void requestBulkJob("DELETE")}
            type="button"
          >
            삭제 요청
          </button>
          <input
            data-testid="employment-rate-job-id-input"
            placeholder="작업 ID"
            value={jobId}
            onChange={(event) => setJobId(event.target.value)}
          />
          <button
            data-testid="employment-rate-job-search-button"
            onClick={() => void loadBulkJob()}
            type="button"
          >
            <Search size={16} /> 결과 조회
          </button>
        </div>
        {bulkJob ? (
          <p className="mt-3 text-sm" data-testid="employment-rate-job-result">
            {bulkJob.jobId}: {bulkJob.jobStatus} / {bulkJob.processedCount}건
            처리
          </p>
        ) : null}
      </section>
    </section>
  );
}

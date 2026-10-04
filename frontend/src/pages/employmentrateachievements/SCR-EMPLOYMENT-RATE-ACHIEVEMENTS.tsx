import { Download, FileUp, RefreshCw, Save, Search } from "lucide-react";
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

type SaveResponse = {
  achievement: Achievement;
  occurredDateWarning: boolean;
  warningMessage?: string | null;
};

type UploadResult = {
  uploadId: string;
  originalFileName: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  errors: { rowNumber: number; columnName: string; errorReason: string }[];
};

type BulkJob = {
  batchJobId: string;
  evaluationYear: string;
  actionType: string;
  jobStatus: string;
  totalCount: number;
  successCount: number;
  failureCount: number;
  unprocessedCount: number;
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

/** Tabbed individual, Excel, and deferred-batch UI for employment-rate achievements. */
export function EmploymentRateAchievementsPage() {
  const [tab, setTab] = useState<"individual" | "excel" | "batch">(
    "individual",
  );
  const [rows, setRows] = useState<Achievement[]>([]);
  const [form, setForm] = useState<Form>(emptyForm);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [file, setFile] = useState<File | null>(null);
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null);
  const [jobId, setJobId] = useState("");
  const [batchJob, setBatchJob] = useState<BulkJob | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
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

  const select = async (item: Achievement) => {
    try {
      const response = await apiRequest<Achievement>(
        `/api/business/employment-rate-achievements/${item.achievementId}`,
      );
      const detail = response.data ?? item;
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
      const response = await apiRequest<SaveResponse>(
        path as `/api/${string}`,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify(payload),
        },
      );
      setSelected(response.data?.achievement ?? null);
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "평가기간 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
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
      setError("취업률 실적 CSV 파일을 선택하세요.");
      return;
    }
    try {
      setSaving(true);
      setError(null);
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
          body.error?.message ?? "Excel 업로드에 실패했습니다.",
          body.error,
        );
      }
      setUploadResult(body.data);
      setSuccess("전체 행 검증 결과를 확인했습니다.");
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const loadBatchResult = async () => {
    if (!jobId.trim()) {
      setError("일괄 작업 ID를 입력하세요.");
      return;
    }
    try {
      setSaving(true);
      const response = await apiRequest<BulkJob>(
        `/api/business/employment-rate-achievements/bulk-jobs/${encodeURIComponent(jobId.trim())}`,
      );
      setBatchJob(response.data ?? null);
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      setFieldErrors(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((item) => [
            item.field,
            item.message,
          ]),
        ),
      );
      return;
    }
    setError(
      caught instanceof Error ? caught.message : "처리 중 오류가 발생했습니다.",
    );
  };

  const confirmed = selected?.achievementStatus === "EVALUATION_CONFIRMED";
  if (permissionDenied) {
    return (
      <section data-testid="employment-rate-achievements-page">
        <PermissionState
          title="취업률 실적 관리 권한이 없습니다"
          message="개별 조회는 R01, R02, R04, Excel 및 일괄 처리는 R07 권한이 필요합니다."
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
          개별 등록, Excel 일괄등록, 일괄 처리결과를 역할에 따라 확인합니다.
        </p>
      </header>
      {loading ? <LoadingState title="취업률 실적 조회 중" /> : null}
      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="취업률 실적 오류" message={error} /> : null}

      <nav className="flex gap-2" aria-label="취업률 실적 탭">
        <button
          data-testid="employment-rate-individual-tab"
          onClick={() => setTab("individual")}
          type="button"
        >
          개별 실적
        </button>
        <button
          data-testid="employment-rate-excel-tab"
          onClick={() => setTab("excel")}
          type="button"
        >
          Excel 등록
        </button>
        <button
          data-testid="employment-rate-batch-tab"
          onClick={() => setTab("batch")}
          type="button"
        >
          일괄 처리결과
        </button>
      </nav>

      {tab === "individual" ? (
        <>
          <section
            className="rounded-md border border-ld bg-white p-5"
            data-testid="employment-rate-list-panel"
          >
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-lg font-semibold text-dark">
                취업률 실적 목록
              </h2>
              <div className="flex gap-2">
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
                  className={
                    "inline-flex items-center gap-2 rounded-md border border-primary " +
                    "px-3 py-2 text-sm text-primary"
                  }
                  data-testid="employment-rate-download-link"
                  href={`/api/business/employment-rate-achievements/download?page=0&pageSize=${pageSize}`}
                >
                  <Download size={16} /> Excel 다운로드
                </a>
                <button
                  data-testid="employment-rate-refresh-button"
                  onClick={() => void load()}
                  type="button"
                >
                  <RefreshCw size={16} /> 조회
                </button>
              </div>
            </div>
            {!loading && rows.length === 0 ? (
              <EmptyState title="조회된 취업률 실적이 없습니다" />
            ) : null}
            {rows.length > 0 ? (
              <table className="min-w-full text-sm">
                <thead>
                  <tr>
                    <th>관리항목</th>
                    <th>업적발생일</th>
                    <th>실적명</th>
                    <th>상태</th>
                    <th>상세</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((item) => (
                    <tr
                      data-testid="employment-rate-row"
                      key={item.achievementId}
                    >
                      <td>{item.managementItemCode}</td>
                      <td>{item.achievementDate}</td>
                      <td>{item.achievementName ?? "-"}</td>
                      <td>{item.achievementStatus}</td>
                      <td>
                        <button
                          data-testid="employment-rate-detail-button"
                          onClick={() => void select(item)}
                          type="button"
                        >
                          상세
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : null}
          </section>
          <section
            className="rounded-md border border-ld bg-white p-5"
            data-testid="employment-rate-form-panel"
          >
            <h2 className="text-lg font-semibold text-dark">
              취업률 실적 상세
            </h2>
            {confirmed ? (
              <p data-testid="employment-rate-confirmed-lock-message">
                평가확정 실적은 수정할 수 없습니다.
              </p>
            ) : null}
            <div className="mt-4 grid gap-3 md:grid-cols-2">
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
                첨부 식별자
                <input
                  data-testid="employment-rate-attachment-input"
                  value={form.attachmentIds}
                  onChange={(event) =>
                    setForm({ ...form, attachmentIds: event.target.value })
                  }
                />
              </label>
            </div>
            {fieldErrors.managementItemCode ? (
              <p>{fieldErrors.managementItemCode}</p>
            ) : null}
            <button
              data-testid="employment-rate-save-button"
              disabled={saving || confirmed}
              onClick={() => void save()}
              type="button"
            >
              <Save size={16} /> 저장
            </button>
          </section>
        </>
      ) : null}

      {tab === "excel" ? (
        <section
          className="rounded-md border border-ld bg-white p-5"
          data-testid="employment-rate-excel-panel"
        >
          <h2 className="text-lg font-semibold text-dark">
            취업률 실적 Excel 등록
          </h2>
          <p className="mt-2 text-sm text-muted">
            오류 또는 중복 행이 하나라도 있으면 업무 데이터는 반영하지 않습니다.
          </p>
          <input
            accept=".csv"
            data-testid="employment-rate-file-input"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
            type="file"
          />
          <button
            data-testid="employment-rate-upload-button"
            disabled={saving}
            onClick={() => void upload()}
            type="button"
          >
            <FileUp size={16} /> 업로드·검증
          </button>
          {uploadResult ? (
            <div data-testid="employment-rate-upload-result">
              정상행 {uploadResult.successCount}건 / 오류행{" "}
              {uploadResult.errorCount}건
              {uploadResult.errorCount > 0
                ? " — 오류가 있어 전체 반영하지 않습니다."
                : " — 전체 반영되었습니다."}
            </div>
          ) : null}
        </section>
      ) : null}

      {tab === "batch" ? (
        <section
          className="rounded-md border border-ld bg-white p-5"
          data-testid="employment-rate-batch-panel"
        >
          <h2 className="text-lg font-semibold text-dark">일괄 처리결과</h2>
          <p className="mt-2 text-sm text-muted">
            일괄 생성·삭제 정책은 승인 전이므로 결과 조회만 제공합니다.
          </p>
          <input
            data-testid="employment-rate-batch-job-id-input"
            placeholder="일괄 작업 ID"
            value={jobId}
            onChange={(event) => setJobId(event.target.value)}
          />
          <button
            data-testid="employment-rate-batch-result-button"
            disabled={saving}
            onClick={() => void loadBatchResult()}
            type="button"
          >
            <Search size={16} /> 결과 조회
          </button>
          {batchJob ? (
            <dl data-testid="employment-rate-batch-result">
              <dt>상태</dt>
              <dd>{batchJob.jobStatus}</dd>
              <dt>성공</dt>
              <dd>{batchJob.successCount}건</dd>
              <dt>미처리</dt>
              <dd>{batchJob.unprocessedCount}건</dd>
            </dl>
          ) : null}
        </section>
      ) : null}
    </section>
  );
}

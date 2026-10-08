import { useEffect, useState } from "react";
import { useAuth } from "../../app/AuthProvider";
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
  teacherUserId: number;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName?: string;
  achievementDetail: string;
  achievementStatus: string;
  attachmentRef?: string;
};
type Search = { achievements: Achievement[]; totalElements: number };
type Form = {
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName: string;
  achievementDetail: string;
  attachmentRef: string;
};
type Upload = {
  uploadId: string;
  validationStatus: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  savedCount?: number;
  fileName?: string;
  errors?: { rowNumber: number; errorReason: string }[];
};
type Job = {
  jobId: string;
  totalCount: number;
  processedCount: number;
  unprocessedCount: number;
  items: {
    targetUserId: number;
    processedYn: string;
    unprocessedReason: string;
  }[];
};
const base = "/api/business/employment-rate-achievements" as const;
const initial: Form = {
  evaluationYear: "",
  managementItemCode: "",
  achievementDate: "",
  achievementName: "",
  achievementDetail: "{}",
  attachmentRef: "",
};
const button =
  "rounded-md border border-primary px-3 py-2 text-sm font-semibold text-primary disabled:opacity-50";
const panel = "rounded-md border border-ld bg-white p-5 shadow-sm";
const filterLabels: Record<string, string> = {
  evaluationYear: "평가연도 검색",
  managementItemCode: "관리항목 검색",
  achievementStatus: "상태 검색",
};
const formLabels: Record<string, string> = {
  evaluationYear: "평가연도 *",
  managementItemCode: "관리항목 *",
  achievementDate: "발생일 *",
  achievementName: "실적명",
  achievementDetail: "관리항목 동적 상세(JSON)",
  attachmentRef: "첨부 참조",
};

export function EmploymentRateAchievementsPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const admin = roles.includes("R09");
  const canRead = admin || roles.some((r) => ["R01", "R02", "R04"].includes(r));
  const canWrite = admin || roles.includes("R01");
  const canExcel = admin || roles.includes("R07");
  const [tab, setTab] = useState(canRead ? "records" : "excel");
  const [form, setForm] = useState<Form>(initial);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [rows, setRows] = useState<Achievement[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [filter, setFilter] = useState({
    evaluationYear: "",
    managementItemCode: "",
    achievementStatus: "",
  });
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const [file, setFile] = useState<File | null>(null);
  const [upload, setUpload] = useState<Upload | null>(null);
  const [histories, setHistories] = useState<Upload[]>([]);
  const [conditions, setConditions] = useState({
    evaluationYear: "",
    actionType: "GENERATE",
    targetCondition: "{}",
  });
  const [preview, setPreview] = useState<{
    executable: boolean;
    reason: string;
    targets: Search;
  } | null>(null);
  const [jobId, setJobId] = useState("");
  const [job, setJob] = useState<Job | null>(null);
  const locked =
    !!selected &&
    !["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.achievementStatus,
    );
  const readonly =
    !canWrite ||
    locked ||
    (!!selected && !admin && selected.teacherUserId !== user?.userId);
  const query = () =>
    new URLSearchParams({
      ...filter,
      page: String(page),
      pageSize: String(size),
    }).toString();

  function failure(caught: unknown) {
    setDenied(caught instanceof ApiClientError && caught.status === 403);
    setError(
      caught instanceof Error ? caught.message : "요청을 처리하지 못했습니다.",
    );
    setFields(
      caught instanceof ApiClientError
        ? Object.fromEntries(
            (caught.apiError?.fields ?? []).map((f) => [f.field, f.message]),
          )
        : {},
    );
  }
  async function load() {
    if (!canRead) return;
    setLoading(true);
    setError("");
    try {
      const response = await apiRequest<Search>(`${base}?${query()}`);
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      failure(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    void load();
  }, [page, size, canRead]);

  async function act(action: () => Promise<void>) {
    setBusy(true);
    setError("");
    setDenied(false);
    setSuccess("");
    setFields({});
    try {
      await action();
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }
  async function detail(id: number) {
    await act(async () => {
      const response = await apiRequest<Achievement>(`${base}/${id}`);
      if (!response.data) return;
      const row = response.data;
      setSelected(row);
      setForm({
        evaluationYear: row.evaluationYear,
        managementItemCode: row.managementItemCode,
        achievementDate: row.achievementDate,
        achievementName: row.achievementName ?? "",
        achievementDetail: row.achievementDetail,
        attachmentRef: row.attachmentRef ?? "",
      });
    });
  }
  async function save() {
    if (readonly) return;
    if (
      !form.evaluationYear ||
      !form.managementItemCode ||
      !form.achievementDate
    ) {
      setError("평가연도·관리항목·발생일을 입력하세요.");
      return;
    }
    if (!window.confirm("취업률 실적을 저장하시겠습니까?")) return;
    await act(async () => {
      const response = await apiRequest<{
        achievement: Achievement;
        occurredDateWarning: boolean;
        warningMessage: string;
      }>(selected ? `${base}/${selected.achievementId}` : base, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          evaluationYear: form.evaluationYear,
          managementItemCode: form.managementItemCode,
          achievementDate: form.achievementDate,
          achievementName: form.achievementName,
          achievementDetail: JSON.parse(form.achievementDetail),
          attachmentRef: form.attachmentRef || null,
        }),
      });
      setSelected(response.data?.achievement ?? null);
      setSuccess(
        response.data?.occurredDateWarning
          ? response.data.warningMessage
          : "저장되었습니다.",
      );
      await load();
    });
  }
  async function download(path: `/api/${string}`, name: string) {
    await act(async () => {
      const response = await fetch(path, { credentials: "include" });
      if (!response.ok)
        throw new ApiClientError(
          response.status,
          "다운로드 권한 또는 조건을 확인하세요.",
        );
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement("a");
      link.href = url;
      link.download = name;
      link.click();
      URL.revokeObjectURL(url);
    });
  }
  async function uploadFile() {
    if (!file) return;
    await act(async () => {
      const body = new FormData();
      body.append("file", file);
      const response = await fetch(`${base}/excel-uploads`, {
        method: "POST",
        body,
        credentials: "include",
      });
      const result = await response.json();
      if (result.data?.uploadId) setUpload(result.data);
      if (!response.ok)
        throw new ApiClientError(
          response.status,
          result.error?.message ?? "검증 실패",
          result.error,
        );
      setSuccess("검증되었습니다. 결과를 확인한 후 반영하세요.");
    });
  }
  const bulkPayload = () => ({
    ...conditions,
    targetCondition: JSON.parse(conditions.targetCondition),
  });
  if (!(canRead || canExcel))
    return <PermissionState message="취업률 실적 접근 권한이 없습니다." />;

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENTS"
      data-testid="employment-rate-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 실적 관리
        </h1>
      </header>
      {loading && <LoadingState />}
      {denied && <PermissionState message={error} />}
      {!denied && error && <ErrorState message={error} />}
      {success && <SuccessState message={success} />}
      <div className="flex flex-wrap gap-2" role="tablist">
        {canRead && (
          <button
            className={button}
            data-testid="records-tab"
            onClick={() => setTab("records")}
          >
            개별 실적
          </button>
        )}
        {canExcel && (
          <button
            className={button}
            data-testid="excel-tab"
            onClick={() => setTab("excel")}
          >
            Excel 등록
          </button>
        )}
        {canExcel && (
          <button
            className={button}
            data-testid="bulk-tab"
            onClick={() => setTab("bulk")}
          >
            일괄 처리
          </button>
        )}
        <button
          className={button}
          data-testid="download-button"
          onClick={() =>
            void download(`${base}/download?${query()}`, "취업률.xlsx")
          }
        >
          Excel 다운로드
        </button>
      </div>
      {tab === "records" && canRead && (
        <div className="space-y-6" data-testid="records-panel">
          <section className={panel}>
            <div className="grid gap-4 md:grid-cols-4">
              {Object.entries(filter).map(([key, value]) => (
                <label key={key} className="text-sm text-muted">
                  {filterLabels[key]}
                  <input
                    className="w-full rounded-md border border-ld p-2"
                    data-testid={`filter-${key}`}
                    value={value}
                    onChange={(e) =>
                      setFilter({ ...filter, [key]: e.target.value })
                    }
                  />
                </label>
              ))}
              <button
                className={button}
                data-testid="search-button"
                onClick={() => {
                  setPage(0);
                  void load();
                }}
              >
                조회
              </button>
            </div>
            <label>
              표시 건수
              <select
                data-testid="page-size"
                value={size}
                onChange={(e) => {
                  setSize(Number(e.target.value));
                  setPage(0);
                }}
              >
                {[20, 50, 100].map((n) => (
                  <option value={n} key={n}>
                    {n}
                  </option>
                ))}
              </select>
            </label>
            {!loading && rows.length === 0 && <EmptyState />}
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr>
                    <th>실적명</th>
                    <th>관리항목</th>
                    <th>평가연도</th>
                    <th>발생일</th>
                    <th>상태</th>
                    <th>상세</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr
                      key={row.achievementId}
                      data-testid={`achievement-row-${row.achievementId}`}
                    >
                      <td>{row.achievementName}</td>
                      <td>{row.managementItemCode}</td>
                      <td>{row.evaluationYear}</td>
                      <td>{row.achievementDate}</td>
                      <td>{row.achievementStatus}</td>
                      <td>
                        <button
                          className={button}
                          data-testid={`detail-${row.achievementId}`}
                          onClick={() => void detail(row.achievementId)}
                        >
                          상세
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p>
              총 {total}건 / {page + 1}페이지
            </p>
            <button
              className={button}
              data-testid="previous-page"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              이전
            </button>
            <button
              className={button}
              data-testid="next-page"
              disabled={(page + 1) * size >= total}
              onClick={() => setPage(page + 1)}
            >
              다음
            </button>
          </section>
          <section className={panel} data-testid="detail-panel">
            <h2 className="text-lg font-semibold text-dark">
              취업률 실적 상세
            </h2>
            {locked && (
              <p className="text-error">
                현재 상태에서는 수정할 수 없습니다. 평가확정 취소는 지원하지
                않습니다.
              </p>
            )}
            <p className="text-sm text-muted">
              평가연도는 수정 시 유지됩니다. 기간 밖 발생일은 경고와 함께
              저장됩니다.
            </p>
            <div className="grid gap-4 md:grid-cols-2">
              {Object.entries(form).map(([key, value]) => (
                <label key={key} className="text-sm text-muted">
                  {formLabels[key]}
                  <input
                    className="w-full rounded-md border border-ld p-2"
                    data-testid={`input-${key}`}
                    type={key === "achievementDate" ? "date" : "text"}
                    value={String(value)}
                    disabled={
                      readonly || (key === "evaluationYear" && !!selected)
                    }
                    onChange={(e) =>
                      setForm({ ...form, [key]: e.target.value })
                    }
                  />
                  {fields[key] && (
                    <span className="text-error">{fields[key]}</span>
                  )}
                </label>
              ))}
            </div>
            {canWrite && (
              <button
                className={button}
                data-testid="save-button"
                disabled={readonly || busy}
                onClick={() => void save()}
              >
                저장
              </button>
            )}
            {canWrite && (
              <button
                className={button}
                data-testid="new-button"
                onClick={() => {
                  setSelected(null);
                  setForm(initial);
                }}
              >
                신규
              </button>
            )}
          </section>
        </div>
      )}
      {tab === "excel" && canExcel && (
        <section className={panel} data-testid="excel-panel">
          <h2 className="text-lg font-semibold text-dark">
            양식 다운로드 → 업로드 → 검증 확인 → 반영
          </h2>
          <button
            className={button}
            data-testid="template-button"
            onClick={() =>
              void download(
                `${base}/excel-uploads/template`,
                "취업률_양식.xlsx",
              )
            }
          >
            양식 다운로드
          </button>
          <input
            type="file"
            accept=".xlsx"
            data-testid="upload-file"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
          <button
            className={button}
            data-testid="upload-button"
            disabled={!file || busy}
            onClick={() => void uploadFile()}
          >
            검증
          </button>
          {upload && (
            <div data-testid="upload-result">
              <p>
                전체 {upload.totalCount} / 정상 {upload.successCount} / 오류{" "}
                {upload.errorCount}
              </p>
              {upload.errors?.map((row) => (
                <p
                  key={row.rowNumber}
                  data-testid={`error-row-${row.rowNumber}`}
                >
                  {row.rowNumber}행: {row.errorReason}
                </p>
              ))}
              {upload.validationStatus === "REJECTED" && (
                <button
                  className={button}
                  data-testid="errors-download"
                  onClick={() =>
                    void download(
                      `${base}/excel-uploads/${upload.uploadId}/errors/download`,
                      "오류.xlsx",
                    )
                  }
                >
                  오류파일
                </button>
              )}
              <button
                className={button}
                data-testid="commit-button"
                disabled={busy || upload.validationStatus !== "VALIDATED"}
                onClick={() => {
                  if (
                    !window.confirm(
                      "검증 결과를 확인하셨습니까? 전체 반영하시겠습니까?",
                    )
                  )
                    return;
                  void act(async () => {
                    const response = await apiRequest<{ savedCount: number }>(
                      `${base}/excel-uploads/${upload.uploadId}/commit`,
                      { method: "POST" },
                    );
                    setUpload({ ...upload, validationStatus: "COMMITTED" });
                    setSuccess(
                      `${response.data?.savedCount ?? 0}건 반영되었습니다.`,
                    );
                  });
                }}
              >
                확인 후 전체 반영
              </button>
            </div>
          )}
          <button
            className={button}
            data-testid="histories-button"
            onClick={() =>
              void act(async () => {
                const response = await apiRequest<Upload[]>(
                  `${base}/excel-uploads/histories`,
                );
                setHistories(response.data ?? []);
              })
            }
          >
            업로드 이력
          </button>
          {histories.map((row) => (
            <p key={row.uploadId} data-testid={`history-${row.uploadId}`}>
              {row.fileName}: 전체 {row.totalCount} / 오류 {row.errorCount} /
              반영 {row.savedCount}
            </p>
          ))}
        </section>
      )}
      {tab === "bulk" && canExcel && (
        <section className={panel} data-testid="bulk-panel">
          <h2 className="text-lg font-semibold text-dark">일괄 처리</h2>
          <p>
            현재 생성·삭제 정책은 미승인입니다. 미리보기와 기존 진단 결과만
            확인할 수 있습니다.
          </p>
          <label>
            평가연도 *
            <input
              data-testid="bulk-year"
              value={conditions.evaluationYear}
              onChange={(e) => {
                setConditions({
                  ...conditions,
                  evaluationYear: e.target.value,
                });
                setPreview(null);
              }}
            />
          </label>
          <label>
            처리 유형
            <select
              data-testid="bulk-action"
              value={conditions.actionType}
              onChange={(e) => {
                setConditions({ ...conditions, actionType: e.target.value });
                setPreview(null);
              }}
            >
              <option value="GENERATE">생성</option>
              <option value="DELETE">삭제</option>
            </select>
          </label>
          <label>
            대상 조건(JSON)
            <input
              data-testid="bulk-condition"
              value={conditions.targetCondition}
              onChange={(e) => {
                setConditions({
                  ...conditions,
                  targetCondition: e.target.value,
                });
                setPreview(null);
              }}
            />
          </label>
          <button
            className={button}
            data-testid="preview-button"
            onClick={() =>
              void act(async () => {
                const response = await apiRequest<typeof preview>(
                  `${base}/bulk-jobs/preview`,
                  { method: "POST", body: JSON.stringify(bulkPayload()) },
                );
                setPreview(response.data ?? null);
              })
            }
          >
            대상 미리보기
          </button>
          {preview && (
            <p>
              {preview.reason} / 대상 {preview.targets.totalElements}건
            </p>
          )}
          <button
            className={button}
            data-testid="bulk-execute"
            disabled={!preview?.executable || busy}
            onClick={() => {
              if (!window.confirm("일괄 작업을 실행하시겠습니까?")) return;
              void act(async () => {
                await apiRequest(`${base}/bulk-jobs`, {
                  method: "POST",
                  body: JSON.stringify({ ...bulkPayload(), confirmed: true }),
                });
              });
            }}
          >
            확인 후 실행
          </button>
          <label>
            작업 ID
            <input
              data-testid="job-id"
              value={jobId}
              onChange={(e) => setJobId(e.target.value)}
            />
          </label>
          <button
            className={button}
            data-testid="job-button"
            disabled={!jobId}
            onClick={() =>
              void act(async () => {
                const response = await apiRequest<Job>(
                  `${base}/bulk-jobs/${encodeURIComponent(jobId)}`,
                );
                setJob(response.data ?? null);
              })
            }
          >
            처리결과 조회
          </button>
          {job && (
            <div data-testid="job-result">
              <p>
                전체 {job.totalCount} / 처리 {job.processedCount} / 미처리{" "}
                {job.unprocessedCount}
              </p>
              {job.items.map((item) => (
                <p
                  key={item.targetUserId}
                  data-testid={`job-item-${item.targetUserId}`}
                >
                  {item.targetUserId}: {item.processedYn}{" "}
                  {item.unprocessedReason}
                </p>
              ))}
            </div>
          )}
        </section>
      )}
    </section>
  );
}

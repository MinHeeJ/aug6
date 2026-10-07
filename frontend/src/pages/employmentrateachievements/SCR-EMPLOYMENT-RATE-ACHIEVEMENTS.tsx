import { useEffect, useState } from "react";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type Row = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName?: string;
  attachmentIds: string[];
  achievementStatus: string;
};
type Item = {
  managementItemCode: string;
  managementItemName: string;
  teacherEditableYn: string;
  requiredYn: string;
  dataType: string;
};
type SearchResult = {
  achievements: Row[];
  managementItems: Item[];
  totalElements: number;
};
type SaveResult = {
  achievement: Row;
  occurredDateWarning: boolean;
  warningMessage: string;
};
type UploadResult = {
  uploadId: string;
  successCount: number;
  errorCount: number;
  savedCount: number;
};
type Job = {
  jobId: string;
  totalCount: number;
  processedCount: number;
  unprocessedCount: number;
  items: {
    itemId: number;
    targetUserId: number;
    processedYn: string;
    unprocessedReason?: string;
  }[];
};
const base = "/api/business/employment-rate-achievements";
const blank = {
  managementItemCode: "",
  achievementDate: "",
  achievementName: "",
  attachmentIds: [] as string[],
};
const button =
  "rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50";
const panel = "rounded-md border border-ld bg-white p-5 shadow-sm space-y-4";

export function EmploymentRateAchievementsPage({
  user,
}: {
  user: CurrentUser | null;
}) {
  const roles = user?.roles ?? [];
  const canRead = roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const canWrite = roles.some((role) => ["R01", "R09"].includes(role));
  const canExcel = roles.some((role) => ["R07", "R09"].includes(role));
  const [tab, setTab] = useState(canRead ? "individual" : "excel");
  const [rows, setRows] = useState<Row[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [selected, setSelected] = useState<Row | null>(null);
  const [form, setForm] = useState(blank);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [filter, setFilter] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [denied, setDenied] = useState(false);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null);
  const [evaluationYear, setEvaluationYear] = useState("");
  const [actionType, setActionType] = useState("GENERATE");
  const [target, setTarget] = useState("{}");
  const [preview, setPreview] = useState<Record<string, unknown> | null>(null);
  const [jobId, setJobId] = useState("");
  const [job, setJob] = useState<Job | null>(null);
  const locked =
    !!selected &&
    (selected.achievementStatus !== "DRAFT" ||
      (!roles.includes("R09") && selected.teacherUserId !== user?.userId));
  const query = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  if (filter.trim()) query.set("managementItemCode", filter.trim());

  function fail(caught: unknown) {
    setSuccess("");
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403);
      setError(caught.message);
      const values = caught.apiError?.fields as unknown;
      setFields(
        Array.isArray(values)
          ? Object.fromEntries(
              values.map((field) => [field.field, field.message]),
            )
          : ((values as Record<string, string>) ?? {}),
      );
    } else {
      setError(
        caught instanceof Error ? caught.message : "처리하지 못했습니다.",
      );
    }
  }

  async function load() {
    if (!canRead) return;
    setLoading(true);
    setError("");
    try {
      const result = await apiRequest<SearchResult>(
        `${base}?${query}` as `/api/${string}`,
      );
      setRows(result.data?.achievements ?? []);
      setItems(result.data?.managementItems ?? []);
      setTotal(result.data?.totalElements ?? 0);
    } catch (caught) {
      setRows([]);
      fail(caught);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    if (canRead) void load();
  }, [page, pageSize, canRead]);

  async function select(row: Row) {
    setLoading(true);
    try {
      const result = await apiRequest<Row>(`${base}/${row.achievementId}`);
      const detail = result.data;
      if (detail) {
        setSelected(detail);
        setForm({
          managementItemCode: detail.managementItemCode,
          achievementDate: detail.achievementDate,
          achievementName: detail.achievementName ?? "",
          attachmentIds: detail.attachmentIds ?? [],
        });
        setFields({});
      }
    } catch (caught) {
      fail(caught);
    } finally {
      setLoading(false);
    }
  }

  async function save() {
    if (locked || !canWrite) return;
    if (!form.managementItemCode || !form.achievementDate) {
      setFields({
        managementItemCode: !form.managementItemCode
          ? "관리항목은 필수입니다."
          : "",
        achievementDate: !form.achievementDate
          ? "업적발생일은 필수입니다."
          : "",
      });
      return;
    }
    if (!window.confirm("취업률 실적을 저장하시겠습니까?")) return;
    setLoading(true);
    setFields({});
    try {
      const result = await apiRequest<SaveResult>(
        selected ? `${base}/${selected.achievementId}` : base,
        { method: selected ? "PUT" : "POST", body: JSON.stringify(form) },
      );
      if (result.data?.achievement) setSelected(result.data.achievement);
      setSuccess(
        result.data?.occurredDateWarning
          ? result.data.warningMessage
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      fail(caught);
    } finally {
      setLoading(false);
    }
  }

  async function upload() {
    if (!file) {
      setError("XLSX 파일을 선택하세요.");
      return;
    }
    if (
      !window.confirm(
        "전체 행을 검증하고 오류가 없으면 즉시 전 행을 반영합니다. 실행하시겠습니까?",
      )
    )
      return;
    setLoading(true);
    setFields({});
    setUploadResult(null);
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await fetch(`${base}/excel-uploads`, {
        method: "POST",
        body,
        credentials: "include",
      });
      const result = await response.json();
      if (!response.ok || !result.success) {
        throw new ApiClientError(
          response.status,
          result.error?.message ?? "업로드 오류",
          result.error,
        );
      }
      setUploadResult(result.data);
      setSuccess("전 행 반영 및 업로드 이력 기록이 완료되었습니다.");
      if (canRead) await load();
    } catch (caught) {
      fail(caught);
    } finally {
      setLoading(false);
    }
  }

  async function download() {
    setLoading(true);
    try {
      const response = await fetch(`${base}/download?${query}`, {
        credentials: "include",
      });
      if (!response.ok) {
        const result = await response.json();
        throw new ApiClientError(
          response.status,
          result.error?.message ?? "다운로드 실패",
          result.error,
        );
      }
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement("a");
      link.href = url;
      link.download = "취업률_실적.xlsx";
      link.click();
      URL.revokeObjectURL(url);
    } catch (caught) {
      fail(caught);
    } finally {
      setLoading(false);
    }
  }

  async function execute() {
    if (
      !preview ||
      !window.confirm(
        "정책 미승인으로 실행이 차단됩니다. 요청조건을 확인하고 전송하시겠습니까?",
      )
    )
      return;
    setLoading(true);
    try {
      await apiRequest(`${base}/bulk-jobs`, {
        method: "POST",
        body: JSON.stringify(preview),
      });
    } catch (caught) {
      fail(caught);
    } finally {
      setLoading(false);
    }
  }

  if (!canRead && !canExcel) {
    return (
      <PermissionState
        title="취업률 실적 권한이 없습니다"
        message="업무 역할이 필요합니다."
      />
    );
  }
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENTS"
      data-testid="employment-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="text-xl font-semibold text-dark">취업률 실적 관리</h1>
      </header>
      <nav className="flex gap-3" aria-label="취업률 업무 탭">
        {canRead && (
          <button
            className={button}
            data-testid="individual-tab"
            onClick={() => setTab("individual")}
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
            일괄 처리결과
          </button>
        )}
        <button
          className={button}
          data-testid="download"
          disabled={loading}
          onClick={() => void download()}
        >
          Excel 다운로드
        </button>
      </nav>
      {loading && <LoadingState title="처리 중" />}
      {denied && (
        <PermissionState title="범위 또는 권한이 없습니다" message={error} />
      )}
      {error && !denied && <ErrorState title="처리 오류" message={error} />}
      {success && <SuccessState title="처리 완료" message={success} />}
      {Object.entries(fields)
        .filter(([, value]) => value)
        .map(([key, value]) => (
          <p role="alert" key={key}>
            {key}: {value}
          </p>
        ))}
      {tab === "individual" && canRead && (
        <section className={panel} data-testid="individual-panel">
          <label>
            관리항목 검색
            <input
              data-testid="item-filter"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
          </label>
          <button
            className={button}
            data-testid="search"
            onClick={() => {
              setPage(0);
              void load();
            }}
          >
            조회
          </button>
          <label>
            표시 건수
            <select
              data-testid="page-size"
              value={pageSize}
              onChange={(event) => {
                setPage(0);
                setPageSize(Number(event.target.value));
              }}
            >
              {[20, 50, 100].map((size) => (
                <option key={size}>{size}</option>
              ))}
            </select>
          </label>
          {!loading && rows.length === 0 && (
            <EmptyState
              title="조회된 실적이 없습니다"
              message="조건을 변경하세요."
            />
          )}
          <div className="overflow-x-auto">
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
                    key={row.achievementId}
                    data-testid={`employment-row-${row.achievementId}`}
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.achievementDate}</td>
                    <td>{row.achievementName}</td>
                    <td>{row.achievementStatus}</td>
                    <td>
                      <button
                        data-testid={`detail-${row.achievementId}`}
                        onClick={() => void select(row)}
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
            data-testid="previous"
            disabled={page === 0 || loading}
            onClick={() => setPage(page - 1)}
          >
            이전
          </button>
          <button
            data-testid="next"
            disabled={(page + 1) * pageSize >= total || loading}
            onClick={() => setPage(page + 1)}
          >
            다음
          </button>
          {canWrite && (
            <button
              data-testid="new"
              onClick={() => {
                setSelected(null);
                setForm(blank);
                setFields({});
              }}
            >
              신규
            </button>
          )}
          <h2 className="text-lg font-semibold">실적 상세</h2>
          {locked && (
            <p role="status">
              작성중 본인 실적만 수정할 수 있습니다. 확정 또는 제출 실적은 잠겨
              있습니다.
            </p>
          )}
          <fieldset
            disabled={!canWrite || locked || loading}
            className="grid gap-4 md:grid-cols-2"
          >
            <label>
              관리항목 *
              <select
                data-testid="management-item"
                value={form.managementItemCode}
                onChange={(event) =>
                  setForm({ ...form, managementItemCode: event.target.value })
                }
              >
                <option value="">선택하세요</option>
                {items.map((item, index) => (
                  <option
                    key={`${item.managementItemCode}-${index}`}
                    value={item.managementItemCode}
                    disabled={item.teacherEditableYn !== "Y"}
                  >
                    {item.managementItemName} ({item.dataType},{" "}
                    {item.requiredYn === "Y" ? "필수" : "선택"})
                  </option>
                ))}
              </select>
            </label>
            <label>
              업적발생일 *
              <input
                data-testid="achievement-date"
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
                data-testid="achievement-name"
                value={form.achievementName}
                onChange={(event) =>
                  setForm({ ...form, achievementName: event.target.value })
                }
              />
            </label>
          </fieldset>
          <p>
            평가년도:{" "}
            {selected?.evaluationYear ??
              "서버의 활성 교육영역 입력기간에서 결정"}
          </p>
          <p>
            첨부: {form.attachmentIds.join(", ") || "없음"}. 신규 첨부 업로드는
            공개 계약이 없어 사용할 수 없습니다.
          </p>
          {canWrite && (
            <button
              className={button}
              data-testid="save"
              disabled={locked || loading}
              onClick={() => void save()}
            >
              저장
            </button>
          )}
        </section>
      )}
      {tab === "excel" && canExcel && (
        <section className={panel} data-testid="excel-panel">
          <h2>Excel 일괄등록</h2>
          <p>
            현행 단일 시트 .xlsx: 교번, 관리항목코드, 업적발생일(YYYY-MM-DD),
            실적명, 첨부참조 순서입니다.
          </p>
          <p>
            오류·중복이 하나라도 있으면 전체 0건 반영됩니다. 템플릿·오류파일
            다운로드와 이력 목록은 공개 API 계약이 없습니다.
          </p>
          <input
            data-testid="upload-file"
            type="file"
            accept=".xlsx"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
          <button
            className={button}
            data-testid="upload"
            disabled={loading}
            onClick={() => void upload()}
          >
            검증 후 전체 반영
          </button>
          {uploadResult && (
            <p>
              업로드 {uploadResult.uploadId}: 정상 {uploadResult.successCount},
              오류 {uploadResult.errorCount}, 반영 {uploadResult.savedCount}
            </p>
          )}
        </section>
      )}
      {tab === "bulk" && canExcel && (
        <section className={panel} data-testid="bulk-panel">
          <h2>일괄 요청조건 확인 / 결과조회</h2>
          <p>
            생성조건·삭제상태 정책 미승인: GENERATE/DELETE 모두 409이며 작업이
            생성되지 않습니다.
          </p>
          <label>
            평가년도 *
            <input
              data-testid="evaluation-year"
              value={evaluationYear}
              onChange={(event) => {
                setPreview(null);
                setEvaluationYear(event.target.value);
              }}
            />
          </label>
          <label>
            작업
            <select
              data-testid="action-type"
              value={actionType}
              onChange={(event) => {
                setPreview(null);
                setActionType(event.target.value);
              }}
            >
              <option>GENERATE</option>
              <option>DELETE</option>
            </select>
          </label>
          <label>
            요청조건 JSON
            <textarea
              data-testid="target-condition"
              value={target}
              onChange={(event) => {
                setPreview(null);
                setTarget(event.target.value);
              }}
            />
          </label>
          <button
            data-testid="preview"
            onClick={() => {
              try {
                if (!evaluationYear.trim())
                  throw new Error("평가년도는 필수입니다.");
                const condition: unknown = JSON.parse(target);
                if (
                  !condition ||
                  typeof condition !== "object" ||
                  Array.isArray(condition)
                )
                  throw new Error("JSON 객체가 필요합니다.");
                setPreview({
                  evaluationYear,
                  actionType,
                  targetCondition: condition,
                });
              } catch (caught) {
                fail(caught);
              }
            }}
          >
            요청조건 미리보기
          </button>
          {preview && (
            <p>실행 가능한 대상 목록이 아닙니다: {JSON.stringify(preview)}</p>
          )}
          <button
            data-testid="execute"
            disabled={!preview || loading}
            onClick={() => void execute()}
          >
            확인 후 요청
          </button>
          <label>
            조회할 작업 ID
            <input
              data-testid="job-id"
              value={jobId}
              onChange={(event) => setJobId(event.target.value)}
            />
          </label>
          <button
            data-testid="job-query"
            disabled={!jobId.trim() || loading}
            onClick={async () => {
              setLoading(true);
              setJob(null);
              try {
                setJob(
                  (
                    await apiRequest<Job>(
                      `${base}/bulk-jobs/${encodeURIComponent(jobId.trim())}`,
                    )
                  ).data ?? null,
                );
              } catch (caught) {
                fail(caught);
              } finally {
                setLoading(false);
              }
            }}
          >
            처리결과 조회
          </button>
          {job && (
            <div data-testid="job-result">
              <p>
                총 {job.totalCount} / 처리 {job.processedCount} / 미처리{" "}
                {job.unprocessedCount}
              </p>
              {job.items.map((item) => (
                <p key={item.itemId} data-testid={`job-item-${item.itemId}`}>
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

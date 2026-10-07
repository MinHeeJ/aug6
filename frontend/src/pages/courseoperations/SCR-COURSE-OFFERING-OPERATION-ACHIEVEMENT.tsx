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

export type CourseOperation = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  achievementStatus: string;
  attachmentIds: string[];
};
type SearchResponse = {
  achievements: CourseOperation[];
  totalElements: number;
  managementItems: string[];
};
type SaveResponse = {
  achievement: CourseOperation;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
const emptyForm = {
  managementItemCode: "",
  achievementDate: "",
  performanceDetails: "",
  attachmentIds: [] as string[],
};
const editable = ["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"];
const inputClass = "mt-1 w-full rounded-md border border-ld p-2 text-sm";
const buttonClass =
  "rounded-md bg-primary px-4 py-2 text-sm text-white disabled:opacity-50";

export function CourseOperationManagementPage({
  user,
}: {
  user?: CurrentUser | null;
}) {
  const [rows, setRows] = useState<CourseOperation[]>([]);
  const [options, setOptions] = useState<string[]>([]);
  const [selected, setSelected] = useState<CourseOperation | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [filter, setFilter] = useState("");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [denied, setDenied] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const canRead =
    !user || user.roles.some((r) => ["R01", "R02", "R04", "R09"].includes(r));
  const canWrite = !!user?.roles.some((r) => ["R01", "R09"].includes(r));
  const locked =
    !!selected &&
    (!editable.includes(selected.achievementStatus) ||
      (!user?.roles.includes("R09") &&
        selected.teacherUserId !== user?.userId));

  function handleError(caught: unknown) {
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403);
      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((f) => [f.field, f.message]),
        ),
      );
    }
    setError(
      caught instanceof Error ? caught.message : "요청 처리에 실패했습니다.",
    );
  }

  async function load() {
    if (!canRead) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError("");
    setDenied(false);
    try {
      const query = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      });
      if (search.trim()) query.set("managementNo", search.trim());
      const year = selected?.evaluationYear || form.achievementDate.slice(0, 4);
      if (year) query.set("evaluationYear", year);
      const result = await apiRequest<SearchResponse>(
        `/api/business/course-operations?${query}`,
      );
      setRows(result.data?.achievements ?? []);
      setOptions(result.data?.managementItems ?? []);
      setTotal(result.data?.totalElements ?? 0);
    } catch (caught) {
      handleError(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [page, pageSize, search, form.achievementDate.slice(0, 4)]);

  function applyRow(row: CourseOperation) {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      performanceDetails: row.performanceDetails,
      attachmentIds: row.attachmentIds ?? [],
    });
  }

  async function detail(id: number) {
    setError("");
    setSuccess("");
    try {
      const result = await apiRequest<CourseOperation>(
        `/api/business/course-operations/${id}`,
      );
      if (result.data) applyRow(result.data);
    } catch (caught) {
      handleError(caught);
    }
  }

  async function save(event: React.FormEvent) {
    event.preventDefault();
    if (
      !canWrite ||
      locked ||
      !window.confirm("강좌 개설·운영 실적을 저장하시겠습니까?")
    )
      return;
    setSaving(true);
    setError("");
    setSuccess("");
    setFields({});
    try {
      const path = selected
        ? (`/api/business/course-operations/${selected.achievementId}` as const)
        : "/api/business/course-operations";
      const result = await apiRequest<SaveResponse>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(form),
      });
      if (result.data) {
        applyRow(result.data.achievement);
        setSuccess(
          result.data.occurredDateWarning
            ? `저장되었습니다. ${result.data.warningMessage ?? "업적발생일이 평가대상 기간 밖입니다."}`
            : "저장되었습니다.",
        );
      }
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div
      data-testid="course-operations-screen"
      data-screen-id="SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT"
    >
      <section className="mb-6 rounded-md bg-lightsecondary p-6">
        <h1 className="text-xl font-semibold text-dark">
          강좌 개설·운영 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          교육영역 실적 조회 및 본인 실적 입력
        </p>
      </section>
      {!canRead || denied ? (
        <PermissionState />
      ) : (
        <>
          {error && <ErrorState message={error} />}
          {success && <SuccessState message={success} />}
          <section className="mb-6 rounded-md bg-white p-6">
            <div className="flex flex-wrap items-end gap-3">
              <label className="text-sm">
                관리번호 검색
                <input
                  data-testid="course-search-input"
                  className={inputClass}
                  value={filter}
                  onChange={(e) => setFilter(e.target.value)}
                />
              </label>
              <button
                data-testid="course-search-button"
                className={buttonClass}
                onClick={() => {
                  setPage(0);
                  setSearch(filter);
                  void load();
                }}
              >
                조회
              </button>
              <label className="text-sm">
                표시 건수
                <select
                  data-testid="course-page-size"
                  className={inputClass}
                  value={pageSize}
                  onChange={(e) => {
                    setPage(0);
                    setPageSize(Number(e.target.value));
                  }}
                >
                  {[20, 50, 100].map((n) => (
                    <option key={n} value={n}>
                      {n}건
                    </option>
                  ))}
                </select>
              </label>
              {canWrite && (
                <button
                  data-testid="course-new-button"
                  className={buttonClass}
                  onClick={() => {
                    setSelected(null);
                    setForm(emptyForm);
                    setFields({});
                    setSuccess("");
                  }}
                >
                  신규 등록
                </button>
              )}
            </div>
            {loading ? (
              <LoadingState />
            ) : rows.length === 0 ? (
              <EmptyState />
            ) : (
              <div className="mt-4 overflow-x-auto">
                <table>
                  <thead>
                    <tr>
                      {[
                        "관리번호",
                        "성명",
                        "관리항목",
                        "발생일",
                        "상태",
                        "상세",
                      ].map((h) => (
                        <th key={h}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row) => (
                      <tr
                        key={row.achievementId}
                        data-testid={`course-row-${row.achievementId}`}
                      >
                        <td>{row.managementNo}</td>
                        <td>{row.teacherName}</td>
                        <td>{row.managementItemCode}</td>
                        <td>{row.achievementDate}</td>
                        <td>{row.achievementStatus}</td>
                        <td>
                          <button
                            data-testid={`course-detail-${row.achievementId}`}
                            className="text-primary"
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
            )}
            <div className="mt-4 flex items-center gap-3">
              <button
                data-testid="course-prev-button"
                disabled={page === 0 || loading}
                onClick={() => setPage(page - 1)}
              >
                이전
              </button>
              <span>
                {page + 1} 페이지 / 총 {total}건
              </span>
              <button
                data-testid="course-next-button"
                disabled={(page + 1) * pageSize >= total || loading}
                onClick={() => setPage(page + 1)}
              >
                다음
              </button>
            </div>
          </section>
          <form
            data-testid="course-detail-panel"
            className="rounded-md bg-white p-6"
            onSubmit={save}
          >
            <h2 className="mb-4 text-lg font-semibold">
              {selected ? "실적 상세·수정" : "실적 등록"}
            </h2>
            {selected && (
              <p className="mb-3 text-sm">
                관리번호: {selected.managementNo} / 성명: {selected.teacherName}{" "}
                / 평가연도: {selected.evaluationYear}/ 상태:{" "}
                {selected.achievementStatus}
              </p>
            )}
            {locked && (
              <p className="mb-3 text-warning">
                현재 상태 또는 소유권으로 수정할 수 없습니다.
              </p>
            )}
            <fieldset
              disabled={!canWrite || locked || saving}
              className="grid gap-4 md:grid-cols-2"
            >
              <label className="text-sm">
                관리항목 *
                <select
                  data-testid="course-management-item"
                  className={inputClass}
                  required
                  value={form.managementItemCode}
                  onChange={(e) =>
                    setForm({ ...form, managementItemCode: e.target.value })
                  }
                >
                  <option value="">관리항목 선택</option>
                  {Array.from(
                    new Set([
                      ...options,
                      ...(selected ? [selected.managementItemCode] : []),
                    ]),
                  ).map((code) => (
                    <option key={code} value={code}>
                      {code}
                    </option>
                  ))}
                </select>
                {fields.managementItemCode && (
                  <span role="alert">{fields.managementItemCode}</span>
                )}
              </label>
              <label className="text-sm">
                업적발생일 *
                <input
                  data-testid="course-achievement-date"
                  className={inputClass}
                  type="date"
                  required
                  value={form.achievementDate}
                  onChange={(e) =>
                    setForm({ ...form, achievementDate: e.target.value })
                  }
                />
                {fields.achievementDate && (
                  <span role="alert">{fields.achievementDate}</span>
                )}
              </label>
              <label className="text-sm md:col-span-2">
                실적내역 *
                <textarea
                  data-testid="course-performance-details"
                  className={inputClass}
                  rows={5}
                  required
                  value={form.performanceDetails}
                  onChange={(e) =>
                    setForm({ ...form, performanceDetails: e.target.value })
                  }
                />
                {fields.performanceDetails && (
                  <span role="alert">{fields.performanceDetails}</span>
                )}
              </label>
              <div className="text-sm md:col-span-2">
                첨부:{" "}
                {form.attachmentIds.length
                  ? form.attachmentIds.join(", ")
                  : "등록된 첨부 없음"}
                <p className="text-muted">
                  기존 첨부 참조를 보존합니다. 신규 파일 업로드는 승인된 API가
                  없습니다.
                </p>
                {fields.attachmentIds && (
                  <span role="alert">{fields.attachmentIds}</span>
                )}
              </div>
              {canWrite && (
                <button
                  data-testid="course-save-button"
                  className={buttonClass}
                  type="submit"
                >
                  {saving ? "저장 중" : "저장"}
                </button>
              )}
            </fieldset>
          </form>
        </>
      )}
    </div>
  );
}

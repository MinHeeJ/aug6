import { useEffect, useState } from "react";
import { ApiClientError, type CurrentUser } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import {
  courseOperationsApi,
  type CourseOperation,
  type CourseOperationPage,
  type CourseOperationRequest,
} from "./courseOperationsApi";

const emptyForm: CourseOperationRequest = {
  managementItemCode: "",
  achievementDate: "",
  performanceDetails: "",
  attachmentIds: [],
};
const editableStates = [
  "DRAFT",
  "DEPARTMENT_REJECTED",
  "CERTIFICATION_REJECTED",
];

export function CourseOperationManagementPage({
  user,
}: {
  user: CurrentUser | null;
}) {
  const [rows, setRows] = useState<CourseOperation[]>([]);
  const [options, setOptions] = useState<
    CourseOperationPage["managementItems"]
  >([]);
  const [selected, setSelected] = useState<CourseOperation | null>(null);
  const [form, setForm] = useState<CourseOperationRequest>(emptyForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [filter, setFilter] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [denied, setDenied] = useState(false);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const [tab, setTab] = useState("detail");
  const admin = user?.roles.includes("R09") ?? false;
  const allowed =
    user?.roles.some((role) => ["R01", "R02", "R04", "R09"].includes(role)) ??
    false;
  const writer = admin || (user?.roles.includes("R01") ?? false);
  const locked =
    selected !== null && !editableStates.includes(selected.achievementStatus);
  const foreign =
    selected !== null && selected.teacherUserId !== user?.userId && !admin;
  const readOnly = !writer || locked || foreign;
  const year = selected?.evaluationYear ?? form.achievementDate.slice(0, 4);
  const itemOptions = options.filter(
    (option) => option.evaluationYear === year,
  );

  function failure(caught: unknown) {
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403);
      setError(caught.message);
      // Normalize this scoped contract locally; legacy apiClient fields remain unchanged.
      const values = caught.apiError?.fields;
      setFields(
        Array.isArray(values)
          ? Object.fromEntries(
              values.map((value) => [value.field, value.message]),
            )
          : ((values as unknown as Record<string, string>) ?? {}),
      );
    } else {
      setError("강좌 운영 실적을 처리하지 못했습니다.");
    }
  }

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      });
      if (search.trim()) query.set("managementNo", search.trim());
      const response = await courseOperationsApi.list(query);
      setRows(response.data?.achievements ?? []);
      setOptions(response.data?.managementItems ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      setRows([]);
      failure(caught);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (allowed) void load();
  }, [page, pageSize, search, allowed]);

  async function select(row: CourseOperation) {
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      const detail = (await courseOperationsApi.detail(row.achievementId)).data;
      if (detail) {
        setSelected(detail);
        setForm({
          managementItemCode: detail.managementItemCode,
          achievementDate: detail.achievementDate,
          performanceDetails: detail.performanceDetails,
          attachmentIds: detail.attachmentIds ?? [],
        });
        setTab("detail");
        setFields({});
      }
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }

  async function save(event: React.FormEvent) {
    event.preventDefault();
    if (readOnly || busy) return;
    const invalid: Record<string, string> = {};
    if (!form.achievementDate)
      invalid.achievementDate = "업적발생일을 입력하세요.";
    if (!form.managementItemCode)
      invalid.managementItemCode = "관리항목을 선택하세요.";
    if (!form.performanceDetails.trim())
      invalid.performanceDetails = "실적내역을 입력하세요.";
    setFields(invalid);
    if (
      Object.keys(invalid).length ||
      !window.confirm("강좌 운영 실적을 저장하시겠습니까?")
    )
      return;
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await courseOperationsApi.save(
        selected?.achievementId,
        form,
      );
      if (response.data) {
        setSelected(response.data.achievement);
        setSuccess(
          response.data.occurredDateWarning
            ? (response.data.warningMessage ??
                "발생일 경고와 함께 저장되었습니다.")
            : "저장되었습니다.",
        );
      }
      await load();
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  }

  if (!allowed || denied)
    return (
      <section data-testid="course-operation-page">
        <PermissionState
          title="강좌 운영 실적 접근 권한이 없습니다"
          message="역할과 소속 범위를 확인하세요."
        />
      </section>
    );

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT"
      data-testid="course-operation-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강좌 개설·운영 실적 관리
        </h1>
      </div>
      {error && <ErrorState title="처리 오류" message={error} />}
      {success && <SuccessState title="처리 완료" message={success} />}
      <section className="rounded-md border border-ld bg-white p-5">
        <div className="flex flex-wrap items-end gap-3">
          <label>
            관리번호 검색
            <input
              className="block rounded-md border border-ld p-2"
              data-testid="course-search"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
          </label>
          <button
            data-testid="course-search-button"
            className="rounded-md bg-primary px-4 py-2 text-white"
            onClick={() => {
              setPage(0);
              setSearch(filter);
            }}
          >
            조회
          </button>
          <label>
            표시 건수
            <select
              data-testid="course-page-size"
              className="block rounded-md border border-ld p-2"
              value={pageSize}
              onChange={(event) => {
                setPage(0);
                setPageSize(Number(event.target.value));
              }}
            >
              {[20, 50, 100].map((size) => (
                <option key={size} value={size}>
                  {size}
                </option>
              ))}
            </select>
          </label>
          {writer && (
            <button
              data-testid="course-new-button"
              className="rounded-md border border-ld px-4 py-2"
              disabled={busy}
              onClick={() => {
                setSelected(null);
                setForm(emptyForm);
                setFields({});
                setSuccess(null);
              }}
            >
              신규 등록
            </button>
          )}
        </div>
        {loading ? (
          <LoadingState title="조회 중" message="실적을 불러옵니다." />
        ) : rows.length === 0 ? (
          <EmptyState
            title="조회된 실적이 없습니다"
            message="검색조건을 확인하거나 신규 등록하세요."
          />
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>교원</th>
                  <th>발생일</th>
                  <th>상태</th>
                  <th>상세</th>
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
                    <td>{row.achievementDate}</td>
                    <td>{row.achievementStatus}</td>
                    <td>
                      <button
                        data-testid={`course-detail-${row.achievementId}`}
                        className="text-primary"
                        disabled={busy}
                        onClick={() => void select(row)}
                      >
                        상세 보기
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
            data-testid="course-prev"
            disabled={page === 0 || loading}
            onClick={() => setPage(page - 1)}
          >
            이전
          </button>
          <span>
            {page + 1} 페이지 / 총 {total}건
          </span>
          <button
            data-testid="course-next"
            disabled={(page + 1) * pageSize >= total || loading}
            onClick={() => setPage(page + 1)}
          >
            다음
          </button>
        </div>
      </section>
      <form
        className="rounded-md border border-ld bg-white p-5"
        onSubmit={(event) => void save(event)}
      >
        <h2 className="text-lg font-semibold">
          {selected ? "실적 상세" : "신규 실적"}
        </h2>
        {locked && (
          <p role="status">
            현재 상태에서는 수정할 수 없습니다. 평가확정 실적은 잠금 상태입니다.
          </p>
        )}
        {!writer && <p>조회 전용 권한입니다.</p>}
        <div
          className="my-3 flex gap-3"
          role="tablist"
          aria-label="실적 상세 탭"
        >
          <button
            type="button"
            role="tab"
            aria-selected={tab === "detail"}
            data-testid="course-detail-tab"
            onClick={() => setTab("detail")}
          >
            실적내역
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === "attachments"}
            data-testid="course-attachment-tab"
            onClick={() => setTab("attachments")}
          >
            첨부 참조
          </button>
        </div>
        {tab === "detail" ? (
          <div
            role="tabpanel"
            data-testid="course-detail-panel"
            className="grid gap-4 md:grid-cols-2"
          >
            <label>
              업적발생일 *
              <input
                type="date"
                required
                data-testid="course-date"
                disabled={readOnly || busy}
                value={form.achievementDate}
                onChange={(event) =>
                  setForm({ ...form, achievementDate: event.target.value })
                }
                className="block w-full rounded-md border border-ld p-2"
              />
              {fields.achievementDate && (
                <span role="alert">{fields.achievementDate}</span>
              )}
            </label>
            <label>
              관리항목 *
              <select
                required
                data-testid="course-item"
                value={form.managementItemCode}
                disabled={readOnly || busy}
                onChange={(event) =>
                  setForm({ ...form, managementItemCode: event.target.value })
                }
                className="block w-full rounded-md border border-ld p-2"
              >
                <option value="">선택하세요</option>
                {selected &&
                  !itemOptions.some(
                    (option) => option.code === selected.managementItemCode,
                  ) && (
                    <option value={selected.managementItemCode}>
                      {selected.managementItemCode} (기존 값)
                    </option>
                  )}
                {itemOptions.map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.name}
                  </option>
                ))}
              </select>
              {fields.managementItemCode && (
                <span role="alert">{fields.managementItemCode}</span>
              )}
            </label>
            <label className="md:col-span-2">
              실적내역 *
              <textarea
                required
                data-testid="course-performance"
                disabled={readOnly || busy}
                value={form.performanceDetails}
                onChange={(event) =>
                  setForm({ ...form, performanceDetails: event.target.value })
                }
                className="block min-h-32 w-full rounded-md border border-ld p-2"
              />
              {fields.performanceDetails && (
                <span role="alert">{fields.performanceDetails}</span>
              )}
            </label>
            {selected && <p>평가연도: {selected.evaluationYear} (수정 불가)</p>}
            {!loading && itemOptions.length === 0 && (
              <p>해당 연도의 입력 가능한 관리항목이 없습니다.</p>
            )}
          </div>
        ) : (
          <div role="tabpanel" data-testid="course-attachment-panel">
            <p>첨부 참조는 기존 공통 파일 기능에서 관리합니다.</p>
            {form.attachmentIds.length === 0 ? (
              <p>첨부 참조 없음</p>
            ) : (
              <ul>
                {form.attachmentIds.map((id) => (
                  <li key={id}>{id}</li>
                ))}
              </ul>
            )}
          </div>
        )}
        {writer && (
          <button
            type="submit"
            data-testid="course-save"
            disabled={readOnly || busy || loading || itemOptions.length === 0}
            className="mt-4 rounded-md bg-primary px-4 py-2 font-semibold text-white"
          >
            {busy ? "처리 중" : "저장"}
          </button>
        )}
      </form>
    </section>
  );
}

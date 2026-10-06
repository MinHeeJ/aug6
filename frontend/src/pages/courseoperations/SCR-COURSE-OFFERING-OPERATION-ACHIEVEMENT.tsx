import { useEffect, useRef, useState, type FormEvent } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import { useAuth } from "../../app/AuthProvider";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

// D2 canonical route: /faculty/course-offering-operation-achievements; T016 owns router registration.
type Achievement = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  organizationCode: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  achievementStatus: string;
  attachmentIds: string[];
  createdAt: string;
  updatedAt: string;
};
type ListData = {
  achievements: Achievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};
type SaveData = {
  achievement: Achievement;
  occurredDateWarning: boolean;
  warningMessage?: string | null;
};
type FormValues = Pick<
  Achievement,
  "managementItemCode" | "achievementDate" | "performanceDetails"
>;
type Filters = {
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  achievementStatus: string;
};
const base = "/api/business/course-operations" as const;
const emptyForm: FormValues = {
  managementItemCode: "",
  achievementDate: "",
  performanceDetails: "",
};
const emptyFilters: Filters = {
  managementNo: "",
  teacherName: "",
  managementItemCode: "",
  achievementStatus: "",
};
const editableStatuses = new Set([
  "DRAFT",
  "DEPARTMENT_REJECTED",
  "CERTIFICATION_REJECTED",
]);
const statusLabels: Record<string, string> = {
  DRAFT: "작성중",
  SUBMITTED: "제출",
  DEPARTMENT_CONFIRMED: "학과장확인",
  DEPARTMENT_REJECTED: "학과장미승인",
  CERTIFIED: "인증",
  CERTIFICATION_REJECTED: "인증반려",
  EVALUATION_CONFIRMED: "평가확정",
  DELETED: "삭제",
};
const filterLabels: Record<keyof Filters, string> = {
  managementNo: "관리번호",
  teacherName: "교원명",
  managementItemCode: "관리항목 코드",
  achievementStatus: "상태",
};
const fieldLabels: Record<keyof FormValues, string> = {
  managementItemCode: "관리항목 코드",
  achievementDate: "업적발생일",
  performanceDetails: "실적내역",
};
const inputClass = "form-input w-full";

function formFrom(row: Achievement): FormValues {
  return {
    managementItemCode: row.managementItemCode,
    achievementDate: row.achievementDate,
    performanceDetails: row.performanceDetails,
  };
}

export function CourseOperationManagementPage() {
  const auth = useAuth();
  const canView =
    auth.status === "authenticated" &&
    !!auth.user?.roles.some((role) => ["R01", "R02", "R04"].includes(role));
  const isWriter =
    auth.status === "authenticated" && !!auth.user?.roles.includes("R01");
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [query, setQuery] = useState({
    filters: emptyFilters,
    page: 0,
    pageSize: 20,
    revision: 0,
  });
  const [list, setList] = useState<ListData>({
    achievements: [],
    page: 0,
    pageSize: 20,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState<string | null>(null);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [mode, setMode] = useState<"new" | "loading" | "selected" | "failed">(
    "new",
  );
  const [form, setForm] = useState<FormValues>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [locked, setLocked] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const [warning, setWarning] = useState<string | null>(null);
  const detailVersion = useRef(0);
  const saveInFlight = useRef(false);
  const canEdit =
    isWriter &&
    !permissionDenied &&
    !locked &&
    (mode === "new" ||
      (mode === "selected" &&
        selected !== null &&
        selected.teacherUserId === auth.user?.userId &&
        editableStatuses.has(selected.achievementStatus)));

  useEffect(
    () => () => {
      detailVersion.current += 1;
    },
    [],
  );
  useEffect(() => {
    // Invalidate the previous session's detail and any still-pending response.
    detailVersion.current += 1;
    setSelected(null);
    setMode("new");
    setForm(emptyForm);
    setSaving(false);
    setFormError(null);
    setFieldErrors({});
    setSuccess(null);
    setWarning(null);
    setLocked(false);
    setPermissionDenied(false);
  }, [auth.user?.userId, canView]);

  useEffect(() => {
    if (!canView) return;
    let active = true;
    const controller = new AbortController();
    const params = new URLSearchParams({
      page: String(query.page),
      pageSize: String(query.pageSize),
    });
    for (const [key, value] of Object.entries(query.filters)) {
      if (value.trim()) params.set(key, value.trim());
    }
    setLoading(true);
    setListError(null);
    void apiRequest<ListData>(`${base}?${params.toString()}`, {
      signal: controller.signal,
    })
      .then((response) => {
        if (!active) return;
        if (!response.data) throw new Error("목록 응답 데이터가 없습니다.");
        setList(response.data);
      })
      .catch((caught: unknown) => {
        if (!active) return;
        if (
          caught instanceof ApiClientError &&
          [401, 403].includes(caught.status)
        )
          setPermissionDenied(true);
        setListError(
          caught instanceof Error
            ? caught.message
            : "목록을 불러오지 못했습니다.",
        );
        setList({
          achievements: [],
          page: query.page,
          pageSize: query.pageSize,
          totalElements: 0,
        });
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [canView, auth.user?.userId, query]);

  function clearMessages() {
    setFormError(null);
    setFieldErrors({});
    setSuccess(null);
    setWarning(null);
    setLocked(false);
  }

  function newEntry() {
    if (saveInFlight.current) return;
    detailVersion.current += 1;
    setSelected(null);
    setMode("new");
    setForm(emptyForm);
    clearMessages();
  }

  function resetForm() {
    if (saveInFlight.current) return;
    if (mode === "selected" && selected) {
      setForm(formFrom(selected));
      setFormError(null);
      setFieldErrors({});
      setSuccess(null);
      setWarning(null);
    } else newEntry();
  }

  function handleFormFailure(caught: unknown) {
    if (caught instanceof ApiClientError) {
      if ([401, 403].includes(caught.status)) setPermissionDenied(true);
      if (caught.apiError?.code === "CONFIRMED_DATA_LOCKED") setLocked(true);
      const fields = caught.apiError?.fields ?? [];
      setFieldErrors(
        Object.fromEntries(
          fields.map(({ field, message }) => [field, message]),
        ),
      );
      const otherMessages = fields
        .filter(({ field }) => !(field in fieldLabels))
        .map(({ message }) => message);
      setFormError([caught.message, ...otherMessages].join(" / "));
    } else
      setFormError(
        caught instanceof Error
          ? caught.message
          : "실적을 처리하지 못했습니다.",
      );
  }

  async function selectDetail(id: number) {
    if (saveInFlight.current) return;
    const version = ++detailVersion.current;
    setSelected(null);
    setMode("loading");
    setForm(emptyForm);
    clearMessages();
    try {
      const response = await apiRequest<Achievement>(
        `${base}/${encodeURIComponent(String(id))}`,
      );
      if (version !== detailVersion.current) return;
      if (!response.data || response.data.achievementId !== id) {
        throw new Error("선택한 실적의 상세 응답을 확인할 수 없습니다.");
      }
      setSelected(response.data);
      setForm(formFrom(response.data));
      setMode("selected");
    } catch (caught) {
      if (version !== detailVersion.current) return;
      setMode("failed");
      handleFormFailure(caught);
    }
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!canEdit || saveInFlight.current) return;
    setSuccess(null);
    setWarning(null);
    setFormError(null);
    const errors: Record<string, string> = {};
    for (const field of Object.keys(fieldLabels) as (keyof FormValues)[]) {
      if (!form[field].trim())
        errors[field] = `${fieldLabels[field]}을(를) 입력하세요.`;
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length) return;
    if (!window.confirm("강좌 개설·운영 실적을 저장하시겠습니까?")) return;
    saveInFlight.current = true;
    setSaving(true);
    const version = detailVersion.current;
    const id = mode === "selected" ? selected?.achievementId : undefined;
    const payload = {
      managementItemCode: form.managementItemCode.trim(),
      achievementDate: form.achievementDate,
      performanceDetails: form.performanceDetails.trim(),
      attachmentIds: selected ? [...selected.attachmentIds] : [],
    };
    try {
      const path =
        id === undefined ? base : `${base}/${encodeURIComponent(String(id))}`;
      const response = await apiRequest<SaveData>(path, {
        method: id === undefined ? "POST" : "PUT",
        body: JSON.stringify(payload),
      });
      if (version !== detailVersion.current) return;
      if (!response.data?.achievement) {
        throw new Error(
          "저장 응답 데이터가 없습니다. 목록을 다시 조회하여 결과를 확인하세요.",
        );
      }
      const saved = response.data.achievement;
      setSelected(saved);
      setMode("selected");
      setForm(formFrom(saved));
      setSuccess("저장되었습니다.");
      setWarning(
        response.data.occurredDateWarning
          ? response.data.warningMessage ||
              "평가대상 기간 밖 업적발생일입니다. 경고와 함께 저장되었습니다."
          : null,
      );
      setQuery((current) => ({ ...current, revision: current.revision + 1 }));
    } catch (caught) {
      if (version === detailVersion.current) handleFormFailure(caught);
    } finally {
      saveInFlight.current = false;
      if (version === detailVersion.current) setSaving(false);
    }
  }

  function applyFilters(event: FormEvent) {
    event.preventDefault();
    setQuery((current) => ({
      ...current,
      filters: { ...filters },
      page: 0,
      revision: current.revision + 1,
    }));
  }

  if (auth.status === "loading")
    return <LoadingState title="사용자 권한 확인 중" />;
  if (auth.status === "error") {
    return (
      <ErrorState
        title="사용자 권한 확인 실패"
        message={auth.error ?? "인증 상태를 확인할 수 없습니다."}
      />
    );
  }
  if (!canView || permissionDenied) {
    return (
      <section
        data-screen-id="SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT"
        data-testid="course-page"
      >
        <PermissionState
          title="강좌 개설·운영 실적 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 해당 데이터 접근 범위가 필요합니다. 로그인 및 권한을 확인하세요."
        />
      </section>
    );
  }
  const lastPage = Math.max(
    0,
    Math.ceil(list.totalElements / query.pageSize) - 1,
  );
  const readonlyMessage =
    locked || selected?.achievementStatus === "EVALUATION_CONFIRMED"
      ? "평가확정 실적은 수정할 수 없습니다."
      : "R01 교원만 본인의 작성중·학과장미승인·인증반려 실적을 등록·수정할 수 있습니다. 상세 조회에 실패한 경우 다시 선택하세요.";

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT"
      data-testid="course-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강좌 개설·운영 실적 관리
            </h1>
          </div>
          <button
            type="button"
            data-testid="course-new"
            className="btn-primary"
            disabled={!isWriter || saving}
            onClick={newEntry}
          >
            신규 입력
          </button>
        </div>
      </div>
      <form
        className="rounded-md border border-ld p-6"
        aria-label="실적 검색"
        onSubmit={applyFilters}
      >
        <h2 className="mb-4 text-lg font-semibold">검색조건</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {(Object.keys(filterLabels) as (keyof Filters)[]).map((key) => (
            <label key={key} className="space-y-2 text-sm">
              <span>{filterLabels[key]}</span>
              {key === "achievementStatus" ? (
                <select
                  className={inputClass}
                  data-testid={`course-filter-${key}`}
                  value={filters[key]}
                  onChange={(event) =>
                    setFilters((current) => ({
                      ...current,
                      [key]: event.target.value,
                    }))
                  }
                >
                  <option value="">전체</option>
                  {Object.entries(statusLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  className={inputClass}
                  data-testid={`course-filter-${key}`}
                  value={filters[key]}
                  onChange={(event) =>
                    setFilters((current) => ({
                      ...current,
                      [key]: event.target.value,
                    }))
                  }
                />
              )}
            </label>
          ))}
        </div>
        <div className="mt-4 flex gap-2">
          <button
            type="submit"
            data-testid="course-search"
            className="btn-primary"
            disabled={saving}
          >
            조회
          </button>
          <button
            type="button"
            data-testid="course-filter-reset"
            className="btn-secondary"
            disabled={saving}
            onClick={() => {
              setFilters(emptyFilters);
              setQuery((current) => ({
                ...current,
                filters: emptyFilters,
                page: 0,
                revision: current.revision + 1,
              }));
            }}
          >
            검색 초기화
          </button>
        </div>
      </form>
      <section
        className="rounded-md border border-ld p-6"
        aria-label="실적 목록"
        aria-busy={loading}
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">
            실적 목록{" "}
            <span className="text-sm text-muted">
              총 {list.totalElements}건
            </span>
          </h2>
          <label className="flex items-center gap-2 text-sm">
            표시 건수
            <select
              data-testid="course-page-size"
              className="form-input"
              value={query.pageSize}
              disabled={saving}
              onChange={(event) =>
                setQuery((current) => ({
                  ...current,
                  pageSize: Number(event.target.value),
                  page: 0,
                }))
              }
            >
              {[20, 50, 100].map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState />
        ) : listError ? (
          <div className="space-y-3">
            <ErrorState message={listError} />
            <button
              type="button"
              data-testid="course-list-retry"
              className="btn-secondary"
              onClick={() =>
                setQuery((current) => ({
                  ...current,
                  revision: current.revision + 1,
                }))
              }
            >
              다시 조회
            </button>
          </div>
        ) : !list.achievements.length ? (
          <EmptyState />
        ) : (
          <div className="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  {[
                    "관리번호",
                    "교원명",
                    "소속",
                    "평가년도",
                    "관리항목 코드",
                    "업적발생일",
                    "실적내역",
                    "상태",
                    "상세",
                  ].map((label) => (
                    <th key={label}>{label}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {list.achievements.map((row) => (
                  <tr
                    key={row.achievementId}
                    data-testid={`course-row-${row.achievementId}`}
                    className={
                      selected?.achievementId === row.achievementId
                        ? "bg-lightprimary"
                        : ""
                    }
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.teacherName}</td>
                    <td>{row.organizationCode}</td>
                    <td>{row.evaluationYear}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.achievementDate}</td>
                    <td className="max-w-xs">
                      <span className="line-clamp-2 break-words">
                        {row.performanceDetails}
                      </span>
                    </td>
                    <td className="whitespace-nowrap">
                      {statusLabels[row.achievementStatus] ??
                        row.achievementStatus}
                    </td>
                    <td>
                      <button
                        type="button"
                        data-testid={`course-detail-${row.achievementId}`}
                        className="btn-secondary whitespace-nowrap"
                        aria-label={`${row.managementNo} 상세 조회`}
                        disabled={saving}
                        onClick={() => void selectDetail(row.achievementId)}
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
        <div className="mt-4 flex items-center justify-end gap-3">
          <button
            type="button"
            className="btn-secondary"
            data-testid="course-prev"
            disabled={loading || saving || query.page === 0}
            onClick={() =>
              setQuery((current) => ({ ...current, page: current.page - 1 }))
            }
          >
            이전
          </button>
          <span className="text-sm">
            {query.page + 1} / {lastPage + 1} 페이지
          </span>
          <button
            type="button"
            className="btn-secondary"
            data-testid="course-next"
            disabled={
              loading || saving || !!listError || query.page >= lastPage
            }
            onClick={() =>
              setQuery((current) => ({ ...current, page: current.page + 1 }))
            }
          >
            다음
          </button>
        </div>
      </section>
      <form
        className="rounded-md border border-ld p-6"
        aria-label="실적 상세 입력"
        noValidate
        onSubmit={(event) => void save(event)}
        aria-busy={saving || mode === "loading"}
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">
            {mode === "new" ? "신규 실적 입력" : "실적 상세"}
          </h2>
          <span className="text-sm text-muted">* 필수 입력</span>
        </div>
        {mode === "loading" && <LoadingState title="상세 불러오는 중" />}
        {saving && <LoadingState title="저장 중" />}
        {formError && <ErrorState message={formError} />}
        {success && <SuccessState message={success} />}
        {warning && (
          <div
            role="status"
            className="my-3 rounded-xl bg-lightwarning p-4 text-sm text-warning"
            data-testid="course-date-warning"
          >
            {warning}
          </div>
        )}
        {selected && (
          <dl className="my-4 grid gap-3 rounded-xl bg-lightgray p-4 text-sm sm:grid-cols-2 lg:grid-cols-3">
            <div>
              <dt className="text-muted">관리번호</dt>
              <dd>{selected.managementNo}</dd>
            </div>
            <div>
              <dt className="text-muted">교원 / 소속</dt>
              <dd>
                {selected.teacherName} / {selected.organizationCode}
              </dd>
            </div>
            <div>
              <dt className="text-muted">평가년도 / 상태</dt>
              <dd>
                {selected.evaluationYear} /{" "}
                {statusLabels[selected.achievementStatus] ??
                  selected.achievementStatus}
              </dd>
            </div>
            <div>
              <dt className="text-muted">최초 입력일시</dt>
              <dd>{selected.createdAt}</dd>
            </div>
            <div>
              <dt className="text-muted">최종 수정일시</dt>
              <dd>{selected.updatedAt}</dd>
            </div>
          </dl>
        )}
        {!canEdit && mode !== "loading" && (
          <div data-testid="course-readonly" className="my-4">
            <PermissionState title="읽기 전용" message={readonlyMessage} />
          </div>
        )}
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          {(Object.keys(fieldLabels) as (keyof FormValues)[]).map((key) => (
            <div
              key={key}
              className={key === "performanceDetails" ? "sm:col-span-2" : ""}
            >
              <label
                htmlFor={`course-${key}`}
                className="mb-2 block text-sm font-medium"
              >
                {fieldLabels[key]} <span className="text-error">*</span>
              </label>
              {key === "performanceDetails" ? (
                <textarea
                  id={`course-${key}`}
                  data-testid={`course-${key}`}
                  className={`${inputClass} min-h-32`}
                  rows={5}
                  required
                  disabled={!canEdit || saving}
                  value={form[key]}
                  aria-invalid={!!fieldErrors[key]}
                  aria-describedby={
                    fieldErrors[key] ? `course-error-${key}` : undefined
                  }
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      [key]: event.target.value,
                    }))
                  }
                />
              ) : (
                <input
                  id={`course-${key}`}
                  data-testid={`course-${key}`}
                  className={inputClass}
                  type={key === "achievementDate" ? "date" : "text"}
                  required
                  disabled={!canEdit || saving}
                  value={form[key]}
                  aria-invalid={!!fieldErrors[key]}
                  aria-describedby={
                    fieldErrors[key] ? `course-error-${key}` : undefined
                  }
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      [key]: event.target.value,
                    }))
                  }
                />
              )}
              {fieldErrors[key] && (
                <p
                  id={`course-error-${key}`}
                  data-testid={`course-error-${key}`}
                  role="alert"
                  className="mt-1 text-sm text-error"
                >
                  {fieldErrors[key]}
                </p>
              )}
            </div>
          ))}
        </div>
        <div className="mt-4 space-y-2 rounded-xl border border-ld p-4 text-sm">
          <h3 className="font-semibold">첨부 참조 (읽기 전용)</h3>
          <p data-testid="course-attachment-refs">
            {selected?.attachmentIds.length
              ? selected.attachmentIds.join(", ")
              : "없음"}
          </p>
          <p className="text-muted" data-testid="course-attachment-gap">
            첨부 파일 업로드·다운로드 API가 아직 제공되지 않아 파일
            추가·삭제·열기를 지원하지 않습니다. 수정 시 기존 첨부 참조를 그대로
            유지합니다.
          </p>
        </div>
        <div className="mt-5 flex gap-2">
          <button
            type="submit"
            data-testid="course-save"
            className="btn-primary"
            disabled={!canEdit || saving}
          >
            {saving ? "저장 중…" : "저장"}
          </button>
          <button
            type="button"
            data-testid="course-form-reset"
            className="btn-secondary"
            disabled={saving}
            onClick={resetForm}
          >
            입력 초기화
          </button>
        </div>
      </form>
    </section>
  );
}

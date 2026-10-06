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

type Option = { value: string; label: string };
export type LectureImprovement = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  achievementContent: string;
  academicYear: number;
  semester: number;
  attachmentIds: string[];
};
type SearchResponse = {
  achievements: LectureImprovement[];
  page: number;
  pageSize: number;
  totalElements: number;
  academicYears: Option[];
  semesters: Option[];
  managementItems: Option[];
  canCreate: boolean;
  canUpdate: boolean;
};
type SaveResponse = {
  achievement: LectureImprovement;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: string;
  semester: string;
};
const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: "",
  semester: "",
};
const base = "/api/business/lecture-improvements";

export function LectureImprovementManagementPage() {
  const { user } = useAuth();
  const [data, setData] = useState<SearchResponse | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
  const [search, setSearch] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [revision, setRevision] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailFailed, setDetailFailed] = useState(false);
  const [saving, setSaving] = useState(false);
  const [denied, setDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const selectionVersion = useRef(0);
  const editable = selected
    ? data?.canUpdate === true &&
      selected.teacherUserId === user?.userId &&
      selected.achievementStatus === "DRAFT"
    : data?.canCreate === true;
  const choicesAvailable =
    !!data?.managementItems.length &&
    !!data?.academicYears.length &&
    !!data?.semesters.length;

  function showError(caught: unknown) {
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403 || caught.status === 401);
      setError(caught.message);
      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
    } else {
      setError("강의개선 실적을 처리하지 못했습니다. 다시 시도하세요.");
    }
  }

  useEffect(() => {
    const controller = new AbortController();
    const query = new URLSearchParams({
      page: String(page),
      pageSize: String(pageSize),
    });
    if (appliedSearch.trim()) query.set("teacherName", appliedSearch.trim());
    setLoading(true);
    setError(null);
    setDenied(false);
    void apiRequest<SearchResponse>(`${base}?${query}`, {
      signal: controller.signal,
    })
      .then((response) => {
        if (!controller.signal.aborted) setData(response.data ?? null);
      })
      .catch((caught: unknown) => {
        if (!controller.signal.aborted) {
          setData(null);
          showError(caught);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [page, pageSize, appliedSearch, revision]);

  function populate(row: LectureImprovement) {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      achievementContent: row.achievementContent,
      academicYear: String(row.academicYear),
      semester: String(row.semester),
    });
  }

  async function selectRow(row: LectureImprovement) {
    const version = ++selectionVersion.current;
    setDetailLoading(true);
    setDetailFailed(false);
    setSelected(null);
    setForm(emptyForm);
    setError(null);
    setSuccess(null);
    setFields({});
    try {
      const response = await apiRequest<LectureImprovement>(
        `${base}/${row.achievementId}`,
      );
      if (version !== selectionVersion.current) return;
      if (!response.data || response.data.achievementId !== row.achievementId) {
        throw new Error("선택한 실적의 상세 응답을 확인할 수 없습니다.");
      }
      populate(response.data);
    } catch (caught) {
      if (version === selectionVersion.current) {
        setDetailFailed(true);
        if (caught instanceof ApiClientError) showError(caught);
        else
          setError(
            caught instanceof Error
              ? caught.message
              : "상세 조회에 실패했습니다.",
          );
      }
    } finally {
      if (version === selectionVersion.current) setDetailLoading(false);
    }
  }

  function createMode() {
    ++selectionVersion.current;
    setDetailLoading(false);
    setDetailFailed(false);
    setSelected(null);
    setForm(emptyForm);
    setFields({});
    setSuccess(null);
    setError(null);
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!editable || saving || detailLoading || detailFailed || denied) return;
    const errors: Record<string, string> = {};
    for (const [field, value] of Object.entries(form)) {
      if (!value.trim()) errors[field] = "필수 입력 항목입니다.";
    }
    setFields(errors);
    if (Object.keys(errors).length || !choicesAvailable) return;
    if (!window.confirm("강의개선 실적을 저장하시겠습니까?")) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await apiRequest<SaveResponse>(
        selected ? `${base}/${selected.achievementId}` : base,
        {
          method: selected ? "PUT" : "POST",
          // Server-owned identity, owner, status, evaluationYear and existing attachments are never mutated by the form.
          body: JSON.stringify({
            managementItemCode: form.managementItemCode,
            achievementDate: form.achievementDate,
            achievementContent: form.achievementContent,
            academicYear: Number(form.academicYear),
            semester: Number(form.semester),
          }),
        },
      );
      if (response.data) {
        populate(response.data.achievement);
        setSuccess(
          response.data.occurredDateWarning
            ? (response.data.warningMessage ??
                "발생일 경고와 함께 저장되었습니다.")
            : "저장되었습니다.",
        );
      }
      setRevision((value) => value + 1);
    } catch (caught) {
      showError(caught);
    } finally {
      setSaving(false);
    }
  }

  const disabled =
    !editable || saving || detailLoading || detailFailed || denied;
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT"
      data-testid="lecture-improvement-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강의개선 실적 관리
        </h1>
      </div>
      {denied ? (
        <PermissionState message="인증 및 해당 실적 범위의 조회 권한이 필요합니다." />
      ) : null}
      {error && !denied ? <ErrorState message={error} /> : null}
      {success ? <SuccessState message={success} /> : null}
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="lecture-improvement-search-panel"
      >
        <form
          className="flex flex-wrap items-end gap-3"
          onSubmit={(event) => {
            event.preventDefault();
            setPage(0);
            setAppliedSearch(search);
            setRevision((value) => value + 1);
          }}
        >
          <label className="text-sm">
            성명 검색
            <input
              className="form-input ml-2"
              data-testid="lecture-improvement-search-input"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>
          <button
            className="btn-primary"
            data-testid="lecture-improvement-search-button"
            type="submit"
          >
            조회
          </button>
          <button
            className="btn-secondary"
            data-testid="lecture-improvement-refresh-button"
            onClick={() => setRevision((value) => value + 1)}
            type="button"
          >
            새로고침
          </button>
          <label className="text-sm">
            표시 건수
            <select
              className="form-input ml-2"
              data-testid="lecture-improvement-page-size"
              value={pageSize}
              onChange={(event) => {
                setPage(0);
                setPageSize(Number(event.target.value));
              }}
            >
              {[20, 50, 100].map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
        </form>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="lecture-improvement-list-panel"
      >
        {loading ? <LoadingState /> : null}
        {!loading && !error && !data?.achievements.length ? (
          <EmptyState />
        ) : null}
        {!loading && data?.achievements.length ? (
          <div className="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>성명</th>
                  <th>관리항목</th>
                  <th>업적발생일</th>
                  <th>학년도 / 학기</th>
                  <th>인증상태</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {data.achievements.map((row) => (
                  <tr
                    data-testid={`lecture-improvement-row-${row.achievementId}`}
                    key={row.achievementId}
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.teacherName}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.achievementDate}</td>
                    <td>
                      {row.academicYear} / {row.semester}
                    </td>
                    <td>{row.achievementStatus}</td>
                    <td>
                      <button
                        className="btn-secondary"
                        data-testid={`lecture-improvement-detail-${row.achievementId}`}
                        disabled={saving}
                        onClick={() => void selectRow(row)}
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
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <span>
            총 {data?.totalElements ?? 0}건 · {page + 1}페이지
          </span>
          <button
            className="btn-secondary"
            data-testid="lecture-improvement-previous-page"
            disabled={page === 0 || loading}
            onClick={() => setPage((value) => value - 1)}
            type="button"
          >
            이전
          </button>
          <button
            className="btn-secondary"
            data-testid="lecture-improvement-next-page"
            disabled={
              loading || (page + 1) * pageSize >= (data?.totalElements ?? 0)
            }
            onClick={() => setPage((value) => value + 1)}
            type="button"
          >
            다음
          </button>
        </div>
      </section>
      {!denied ? (
        <section
          className="rounded-md border border-ld bg-white p-5"
          data-testid="lecture-improvement-detail-panel"
        >
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-lg font-semibold">
              {selected ? "상세 / 수정" : "신규 실적"}
            </h2>
            <button
              className="btn-secondary"
              data-testid="lecture-improvement-create-button"
              disabled={!data?.canCreate || saving}
              onClick={createMode}
              type="button"
            >
              신규
            </button>
          </div>
          {detailLoading ? <LoadingState title="상세 불러오는 중" /> : null}
          {selected ? (
            <p
              className="mt-3 text-sm"
              data-testid="lecture-improvement-readonly-metadata"
            >
              관리번호: {selected.managementNo} · 성명: {selected.teacherName} ·
              인증상태: {selected.achievementStatus}
            </p>
          ) : null}
          {selected && !editable ? (
            <p className="mt-3 text-warning">
              본인의 작성중 실적만 수정할 수 있습니다.
            </p>
          ) : null}
          {!choicesAvailable && !loading ? (
            <p className="mt-3 text-error">
              관리항목·학년도·학기 코드가 없습니다. 관리 설정 확인 후
              저장하세요.
            </p>
          ) : null}
          <form
            className="mt-4 grid gap-4 md:grid-cols-2"
            onSubmit={(event) => void save(event)}
          >
            {(
              [
                ["managementItemCode", "관리항목", data?.managementItems ?? []],
                ["academicYear", "학년도", data?.academicYears ?? []],
                ["semester", "학기", data?.semesters ?? []],
              ] as const
            ).map(([field, label, options]) => (
              <label className="text-sm" key={field}>
                {label} *
                <select
                  className="form-input mt-2 w-full"
                  data-testid={`lecture-improvement-${field.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}`}
                  disabled={disabled}
                  required
                  value={form[field]}
                  onChange={(event) =>
                    setForm({ ...form, [field]: event.target.value })
                  }
                >
                  <option value="">선택하세요</option>
                  {!options.some((option) => option.value === form[field]) &&
                  form[field] ? (
                    <option value={form[field]} disabled>
                      {form[field]} (현재 선택 불가)
                    </option>
                  ) : null}
                  {options.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                {fields[field] ? (
                  <span className="text-error">{fields[field]}</span>
                ) : null}
              </label>
            ))}
            <label className="text-sm">
              업적발생일 *
              <input
                className="form-input mt-2 w-full"
                data-testid="lecture-improvement-achievement-date"
                disabled={disabled}
                required
                type="date"
                value={form.achievementDate}
                onChange={(event) =>
                  setForm({ ...form, achievementDate: event.target.value })
                }
              />
              {fields.achievementDate ? (
                <span className="text-error">{fields.achievementDate}</span>
              ) : null}
            </label>
            <label className="text-sm md:col-span-2">
              실적내용 *
              <textarea
                className="form-input mt-2 w-full"
                data-testid="lecture-improvement-achievement-content"
                disabled={disabled}
                required
                rows={4}
                value={form.achievementContent}
                onChange={(event) =>
                  setForm({ ...form, achievementContent: event.target.value })
                }
              />
              {fields.achievementContent ? (
                <span className="text-error">{fields.achievementContent}</span>
              ) : null}
            </label>
            <div className="text-sm md:col-span-2">
              첨부 참조 (읽기 전용):{" "}
              {selected?.attachmentIds?.join(", ") || "없음"}
              <p className="mt-1 text-muted">
                첨부 업로드·다운로드는 공통 첨부 서비스 연동 후 사용할 수
                있습니다.
              </p>
              {fields.attachmentIds ? (
                <p className="text-error">{fields.attachmentIds}</p>
              ) : null}
            </div>
            {fields.body ? <p className="text-error">{fields.body}</p> : null}
            <button
              className="btn-primary md:col-span-2"
              data-testid="lecture-improvement-save-button"
              disabled={disabled || !choicesAvailable}
              type="submit"
            >
              {saving ? "저장 중" : "저장"}
            </button>
          </form>
        </section>
      ) : null}
    </section>
  );
}

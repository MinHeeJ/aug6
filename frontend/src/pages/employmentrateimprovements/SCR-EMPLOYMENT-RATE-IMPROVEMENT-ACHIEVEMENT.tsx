import { RefreshCw, Save, Search } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
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

type Achievement = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  organizationCode: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  specialLectureStartDate: string | null;
  specialLectureEndDate: string | null;
  mockExamQuestionPeriod: string | null;
  attachmentIds: string[];
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate: string;
  specialLectureEndDate: string;
  mockExamQuestionPeriod: string;
  attachmentIds: string[];
};
type Filters = {
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
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
  warningMessage?: string | null;
};
const base = "/api/business/employment-rate-improvements";
const emptyFilters: Filters = {
  managementNo: "",
  teacherName: "",
  managementItemCode: "",
  achievementStatus: "",
};
const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
  attachmentIds: [],
};
const editableStatuses = [
  "DRAFT",
  "DEPARTMENT_REJECTED",
  "CERTIFICATION_REJECTED",
];
const statuses: Record<string, string> = {
  DRAFT: "작성중",
  DEPARTMENT_REJECTED: "학과 반려",
  CERTIFICATION_REJECTED: "인증 반려",
  DEPARTMENT_CONFIRMED: "학과 확인",
  CERTIFIED: "인증",
  SUBMITTED: "제출",
  DELETED: "삭제",
  EVALUATION_CONFIRMED: "평가확정",
};
const formFields: {
  key: Exclude<keyof Form, "attachmentIds">;
  label: string;
  type: string;
  required?: boolean;
}[] = [
  {
    key: "managementItemCode",
    label: "관리항목",
    type: "text",
    required: true,
  },
  { key: "achievementDate", label: "업적발생일", type: "date", required: true },
  { key: "specialLectureStartDate", label: "특강 시작일", type: "date" },
  { key: "specialLectureEndDate", label: "특강 종료일", type: "date" },
  { key: "mockExamQuestionPeriod", label: "모의고사 출제기간", type: "text" },
];
const inputClass =
  "mt-2 block w-full rounded-md border border-ld px-3 py-2 text-sm";
const buttonClass =
  "inline-flex items-center justify-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-link";

export function EmploymentRateImprovementAchievementPage({
  currentUser,
}: {
  currentUser?: CurrentUser | null;
}) {
  const canRead = Boolean(
    currentUser?.roles.some((role) => ["R01", "R02", "R04"].includes(role)),
  );
  const canWrite = Boolean(currentUser?.roles.includes("R01"));
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [query, setQuery] = useState({
    filters: emptyFilters,
    page: 0,
    pageSize: 20,
  });
  const [rows, setRows] = useState<Achievement[]>([]);
  const [total, setTotal] = useState(0);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [loading, setLoading] = useState(canRead);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [detailValid, setDetailValid] = useState(true);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string[]>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const savingRef = useRef(false);
  const listSequence = useRef(0);
  const busy = loading || detailLoading || saving;
  const editable =
    canWrite &&
    detailValid &&
    (!selected ||
      (selected.teacherUserId === currentUser?.userId &&
        editableStatuses.includes(selected.achievementStatus)));

  const describeError = useCallback((caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      return caught.message;
    }
    return caught instanceof Error
      ? caught.message
      : "취업률 제고 실적을 처리하지 못했습니다.";
  }, []);

  const load = useCallback(async () => {
    if (!canRead) return false;
    const sequence = ++listSequence.current;
    setLoading(true);
    setListError(null);
    const params = new URLSearchParams({
      page: String(query.page),
      pageSize: String(query.pageSize),
    });
    Object.entries(query.filters).forEach(([key, value]) => {
      if (value.trim()) params.set(key, value.trim());
    });
    try {
      const response = await apiRequest<ListResponse>(
        `${base}?${params.toString()}`,
      );
      if (!response.data) throw new Error("목록 응답을 확인할 수 없습니다.");
      if (sequence === listSequence.current) {
        setRows(response.data.achievements);
        setTotal(response.data.totalElements);
      }
      return true;
    } catch (caught) {
      if (sequence === listSequence.current) {
        setRows([]);
        setListError(describeError(caught));
      }
      return false;
    } finally {
      if (sequence === listSequence.current) setLoading(false);
    }
  }, [canRead, currentUser?.userId, query, describeError]);

  useEffect(() => {
    void load();
    return () => {
      listSequence.current += 1;
    };
  }, [load]);

  const applyDetail = (achievement: Achievement) => {
    setSelected(achievement);
    setForm({
      managementItemCode: achievement.managementItemCode,
      achievementDate: achievement.achievementDate,
      specialLectureStartDate: achievement.specialLectureStartDate ?? "",
      specialLectureEndDate: achievement.specialLectureEndDate ?? "",
      mockExamQuestionPeriod: achievement.mockExamQuestionPeriod ?? "",
      attachmentIds: achievement.attachmentIds ?? [],
    });
    setDetailValid(true);
    setFieldErrors({});
  };

  const selectDetail = async (id: number) => {
    if (!canRead || busy) return;
    setDetailLoading(true);
    setDetailValid(false);
    setError(null);
    setSuccess(null);
    setFieldErrors({});
    try {
      const response = await apiRequest<Achievement>(`${base}/${id}`);
      if (!response.data) throw new Error("상세 응답을 확인할 수 없습니다.");
      applyDetail(response.data);
    } catch (caught) {
      setError(describeError(caught));
    } finally {
      setDetailLoading(false);
    }
  };

  const newRegistration = () => {
    if (!canWrite || busy) return;
    setSelected(null);
    setForm({ ...emptyForm, attachmentIds: [] });
    setDetailValid(true);
    setError(null);
    setFieldErrors({});
    setSuccess(null);
  };

  const save = async () => {
    if (!canRead || !editable || busy || savingRef.current) return;
    const errors: Record<string, string[]> = {};
    if (!form.managementItemCode.trim())
      errors.managementItemCode = ["관리항목을 입력하세요."];
    if (!form.achievementDate)
      errors.achievementDate = ["업적발생일을 입력하세요."];
    if (
      form.specialLectureStartDate &&
      form.specialLectureEndDate &&
      form.specialLectureEndDate < form.specialLectureStartDate
    ) {
      errors.specialLectureEndDate = ["특강 종료일은 시작일 이후여야 합니다."];
    }
    setFieldErrors(errors);
    setError(null);
    setSuccess(null);
    if (Object.keys(errors).length) return;
    if (!window.confirm("취업률 제고 실적을 저장하시겠습니까?")) return;
    savingRef.current = true;
    setSaving(true);
    // Only approved request fields; identity and status remain server-controlled.
    const request = {
      managementItemCode: form.managementItemCode.trim(),
      achievementDate: form.achievementDate,
      specialLectureStartDate: form.specialLectureStartDate || null,
      specialLectureEndDate: form.specialLectureEndDate || null,
      mockExamQuestionPeriod: form.mockExamQuestionPeriod.trim() || null,
      attachmentIds: [...form.attachmentIds],
    };
    let committed = false;
    try {
      const response = await apiRequest<SaveResponse>(
        selected ? `${base}/${selected.achievementId}` : base,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify(request),
        },
      );
      committed = true;
      setDetailValid(false);
      const saved = response.data?.achievement;
      if (!saved)
        throw new Error(
          "저장 응답에 실적 정보가 없습니다. 목록에서 상세를 다시 조회하세요.",
        );
      setSelected(saved);
      let detailSucceeded = false;
      try {
        const detail = await apiRequest<Achievement>(
          `${base}/${saved.achievementId}`,
        );
        if (!detail.data) throw new Error("상세 응답을 확인할 수 없습니다.");
        applyDetail(detail.data);
        detailSucceeded = true;
      } catch (caught) {
        setError(describeError(caught));
      }
      const listSucceeded = await load();
      const savedMessage = response.data?.occurredDateWarning
        ? response.data.warningMessage || "발생일 경고와 함께 저장되었습니다."
        : "저장되었습니다.";
      setSuccess(
        !detailSucceeded
          ? "저장되었으나 상세 재조회에 실패했습니다. 상세를 다시 조회하세요."
          : !listSucceeded
            ? `${savedMessage} 목록 새로고침에 실패했습니다. 다시 조회하세요.`
            : savedMessage,
      );
    } catch (caught) {
      setError(describeError(caught));
      if (committed) {
        setSuccess(
          "저장 요청이 처리되었습니다. 목록에서 상세를 다시 조회하세요.",
        );
      } else if (caught instanceof ApiClientError) {
        const fields: Record<string, string[]> = {};
        for (const field of caught.apiError?.fields ?? []) {
          (fields[field.field] ??= []).push(field.message);
        }
        setFieldErrors(fields);
      }
    } finally {
      setSaving(false);
      savingRef.current = false;
    }
  };

  if (!canRead || permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENTS"
        data-testid="employment-improvement-page"
      >
        <PermissionState
          title="취업률 제고 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 해당 데이터 범위가 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENTS"
      data-testid="employment-improvement-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              취업률 제고 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              특강 기간과 모의고사 출제기간을 조회하고 본인의 실적을 등록합니다.
            </p>
          </div>
          <button
            className={buttonClass}
            data-testid="employment-improvement-refresh"
            disabled={busy}
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>
      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {listError ? (
        <ErrorState title="목록 조회 오류" message={listError} />
      ) : null}
      {error ? (
        <ErrorState title="취업률 제고 실적 오류" message={error} />
      ) : null}
      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="employment-improvement-search-panel"
      >
        <div className="grid gap-4 md:grid-cols-4">
          {(
            [
              ["managementNo", "관리번호"],
              ["teacherName", "성명"],
              ["managementItemCode", "검색 관리항목"],
            ] as const
          ).map(([key, label]) => (
            <label className="text-sm font-semibold text-dark" key={key}>
              {label}
              <input
                className={inputClass}
                data-testid={`employment-improvement-search-${kebab(key)}`}
                disabled={busy}
                onChange={(event) =>
                  setFilters({ ...filters, [key]: event.target.value })
                }
                value={filters[key]}
              />
            </label>
          ))}
          <label className="text-sm font-semibold text-dark">
            검색 업적상태
            <select
              className={inputClass}
              data-testid="employment-improvement-search-status"
              disabled={busy}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  achievementStatus: event.target.value,
                })
              }
              value={filters.achievementStatus}
            >
              <option value="">전체</option>
              {Object.entries(statuses).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="mt-4 flex justify-end">
          <button
            className={buttonClass}
            data-testid="employment-improvement-search"
            disabled={busy}
            onClick={() =>
              setQuery({ ...query, filters: { ...filters }, page: 0 })
            }
            type="button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="employment-improvement-list-panel"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            취업률 제고 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="employment-improvement-page-size"
              disabled={busy}
              onChange={(event) =>
                setQuery({
                  ...query,
                  page: 0,
                  pageSize: Number(event.target.value),
                })
              }
              value={query.pageSize}
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="취업률 제고 실적 조회 중" /> : null}
        {!loading && !listError && rows.length === 0 ? (
          <EmptyState
            title="조회된 취업률 제고 실적이 없습니다"
            message="검색조건을 변경하거나 신규 등록을 선택하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  {[
                    "관리번호",
                    "성명",
                    "관리항목",
                    "업적발생일",
                    "업적상태",
                    "첨부여부",
                    "상세",
                  ].map((label) => (
                    <th className="px-3 py-2" key={label} scope="col">
                      {label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    className={
                      selected?.achievementId === row.achievementId
                        ? "bg-lightprimary"
                        : "hover:bg-lightgray"
                    }
                    data-testid={`employment-improvement-row-${row.achievementId}`}
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementNo}</td>
                    <td className="px-3 py-2">{row.teacherName}</td>
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">
                      {statuses[row.achievementStatus] ?? row.achievementStatus}
                    </td>
                    <td className="px-3 py-2">
                      {row.attachmentIds?.length ? "있음" : "없음"}
                    </td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid={`employment-improvement-detail-${row.achievementId}`}
                        disabled={busy}
                        onClick={() => void selectDetail(row.achievementId)}
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
        <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
          <p className="text-xs text-muted">
            총 {total}건 / {query.page + 1}페이지
          </p>
          <div className="flex gap-2">
            <button
              className={buttonClass}
              data-testid="employment-improvement-previous"
              disabled={busy || query.page === 0}
              onClick={() => setQuery({ ...query, page: query.page - 1 })}
              type="button"
            >
              이전
            </button>
            <button
              className={buttonClass}
              data-testid="employment-improvement-next"
              disabled={busy || (query.page + 1) * query.pageSize >= total}
              onClick={() => setQuery({ ...query, page: query.page + 1 })}
              type="button"
            >
              다음
            </button>
          </div>
        </div>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="employment-improvement-detail-panel"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            취업률 제고 실적 상세
          </h2>
          {canWrite ? (
            <button
              className={buttonClass}
              data-testid="employment-improvement-new"
              disabled={busy}
              onClick={newRegistration}
              type="button"
            >
              신규 등록
            </button>
          ) : null}
        </div>
        <p className="mt-2 text-sm text-muted">
          관리항목과 업적발생일은 필수입니다. 본인의 작성중·반려 실적만 수정할
          수 있습니다.
        </p>
        {detailLoading ? <LoadingState title="실적 상세 조회 중" /> : null}
        {!editable && !detailLoading ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="employment-improvement-lock-message"
          >
            {!detailValid
              ? "상세를 다시 조회하거나 신규 등록을 선택하세요."
              : "조회 전용입니다. 다른 교원의 실적 및 확인·확정 실적은 수정할 수 없습니다."}
          </p>
        ) : null}
        {selected ? (
          <dl
            className="mt-4 grid gap-3 text-sm text-muted sm:grid-cols-3"
            data-testid="employment-improvement-metadata"
          >
            {[
              ["관리번호", selected.managementNo],
              ["교원", selected.teacherName],
              ["소속 코드", selected.organizationCode],
              ["평가연도", selected.evaluationYear],
              [
                "업적상태",
                statuses[selected.achievementStatus] ??
                  selected.achievementStatus,
              ],
            ].map(([label, value]) => (
              <div key={label}>
                <dt>{label}</dt>
                <dd>{value}</dd>
              </div>
            ))}
          </dl>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          {formFields.map((field) => {
            const id = `employment-improvement-${kebab(field.key)}`;
            return (
              <div key={field.key}>
                <label className="text-sm font-semibold text-dark" htmlFor={id}>
                  {field.label}
                  {field.required ? " *" : ""}
                </label>
                <input
                  aria-describedby={
                    fieldErrors[field.key]?.length ? `${id}-errors` : undefined
                  }
                  aria-invalid={Boolean(fieldErrors[field.key]?.length)}
                  aria-required={field.required}
                  className={inputClass}
                  data-testid={id}
                  disabled={!editable || busy}
                  id={id}
                  onChange={(event) => {
                    setForm({ ...form, [field.key]: event.target.value });
                    setFieldErrors((previous) => ({
                      ...previous,
                      [field.key]: [],
                    }));
                    setSuccess(null);
                  }}
                  type={field.type}
                  value={form[field.key]}
                />
                <div id={`${id}-errors`}>
                  {(fieldErrors[field.key] ?? []).map((message, index) => (
                    <p
                      className="mt-1 text-xs text-error"
                      key={`${message}-${index}`}
                    >
                      {message}
                    </p>
                  ))}
                </div>
              </div>
            );
          })}
          <div
            className="text-sm text-muted"
            data-testid="employment-improvement-attachments"
          >
            <p className="font-semibold text-dark">첨부파일</p>
            <p className="mt-2">
              {form.attachmentIds.length
                ? `등록된 첨부 ${form.attachmentIds.length}개 (조회 전용)`
                : "등록된 첨부 없음"}
            </p>
            <p className="mt-1 text-xs">
              기존 첨부는 유지됩니다. 이 화면에서는 파일 등록·변경을 제공하지
              않습니다.
            </p>
            {(fieldErrors.attachmentIds ?? []).map((message, index) => (
              <p
                className="mt-1 text-xs text-error"
                key={`${message}-${index}`}
              >
                {message}
              </p>
            ))}
          </div>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
            data-testid="employment-improvement-save"
            disabled={!editable || busy}
            onClick={() => void save()}
            type="button"
          >
            <Save size={16} /> {saving ? "저장 중" : "저장"}
          </button>
        </div>
      </section>
    </section>
  );
}

function kebab(value: string) {
  return value.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`);
}

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

export type EmploymentRateImprovementAchievement = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string | number;
  certificationStatus: string;
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate?: string | null;
  specialLectureEndDate?: string | null;
  mockExamQuestionPeriod?: string | null;
  attachmentIds: string[];
};
type AchievementList = {
  achievements: EmploymentRateImprovementAchievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};
type SaveResult = {
  achievement: EmploymentRateImprovementAchievement;
  occurredDateWarning: boolean;
  warningMessage?: string | null;
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate: string;
  specialLectureEndDate: string;
  mockExamQuestionPeriod: string;
};
type FieldErrors = Record<string, string[]>;
const ROOT = "/api/business/employment-rate-improvements";
const EDITABLE_STATUSES = [
  "DRAFT",
  "DEPARTMENT_REJECTED",
  "CERTIFICATION_REJECTED",
];
const controlClass = "w-full rounded-md border border-ld px-3 py-2 text-sm";
const buttonClass =
  "rounded-md border border-ld px-4 py-2 text-sm font-semibold text-link";
const blankForm = (): Form => ({
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
});
const toForm = (row: EmploymentRateImprovementAchievement): Form => ({
  managementItemCode: row.managementItemCode,
  achievementDate: row.achievementDate,
  specialLectureStartDate: row.specialLectureStartDate ?? "",
  specialLectureEndDate: row.specialLectureEndDate ?? "",
  mockExamQuestionPeriod: row.mockExamQuestionPeriod ?? "",
});
const messageOf = (error: unknown) =>
  error instanceof Error ? error.message : "실적을 처리하지 못했습니다.";

export function EmploymentRateImprovementAchievementPage() {
  const { user, status } = useAuth();
  const isAdministrator = !!user?.roles.includes("R09");
  const canRead =
    status === "authenticated" &&
    !!user?.roles.some((role) => ["R01", "R02", "R04", "R09"].includes(role));
  const canCreate =
    canRead && (!!user?.roles.includes("R01") || isAdministrator);
  const [rows, setRows] = useState<EmploymentRateImprovementAchievement[]>([]);
  const [selected, setSelected] =
    useState<EmploymentRateImprovementAchievement | null>(null);
  const [form, setForm] = useState<Form>(blankForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [lookupFailed, setLookupFailed] = useState(false);
  const [committed, setCommitted] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [success, setSuccess] = useState<string | null>(null);
  const [warning, setWarning] = useState<string | null>(null);
  const listSequence = useRef(0);
  const detailSequence = useRef(0);
  const mutationInFlight = useRef(false);
  const busy = saving || detailLoading;
  const canEdit =
    canCreate &&
    !lookupFailed &&
    (!selected ||
      ((isAdministrator || selected.teacherUserId === user?.userId) &&
        EDITABLE_STATUSES.includes(selected.certificationStatus)));

  const loadList = async (requestedPage = page, requestedSize = pageSize) => {
    if (!canRead) return;
    const sequence = ++listSequence.current;
    setLoading(true);
    setListError(null);
    try {
      const response = await apiRequest<AchievementList>(
        `${ROOT}?page=${requestedPage}&pageSize=${requestedSize}`,
      );
      if (!response.data) throw new Error("목록 응답이 없습니다.");
      if (sequence !== listSequence.current) return;
      setRows(response.data.achievements);
      setTotal(response.data.totalElements);
    } catch (caught) {
      if (sequence !== listSequence.current) return;
      setRows([]);
      setTotal(0);
      setListError(messageOf(caught));
    } finally {
      if (sequence === listSequence.current) setLoading(false);
    }
  };

  useEffect(() => {
    void loadList(page, pageSize);
    return () => {
      listSequence.current += 1;
    };
  }, [page, pageSize, canRead, user?.userId]);

  const selectDetail = async (achievementId: number) => {
    if (!canRead || mutationInFlight.current) return;
    const sequence = ++detailSequence.current;
    setDetailLoading(true);
    setLookupFailed(true);
    setError(null);
    setSuccess(null);
    setWarning(null);
    setFieldErrors({});
    try {
      const response = await apiRequest<EmploymentRateImprovementAchievement>(
        `${ROOT}/${achievementId}`,
      );
      if (!response.data) throw new Error("상세 응답이 없습니다.");
      if (sequence !== detailSequence.current) return;
      setSelected(response.data);
      setForm(toForm(response.data));
      setCommitted(false);
      setLookupFailed(false);
    } catch (caught) {
      if (sequence === detailSequence.current) setError(messageOf(caught));
    } finally {
      if (sequence === detailSequence.current) setDetailLoading(false);
    }
  };

  const newDraft = () => {
    if (!canCreate || busy) return;
    detailSequence.current += 1;
    setSelected(null);
    setForm(blankForm());
    setLookupFailed(false);
    setCommitted(false);
    setError(null);
    setFieldErrors({});
    setSuccess(null);
    setWarning(null);
  };

  const change = (field: keyof Form, value: string) => {
    if (!canEdit || busy) return;
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
    setFieldErrors({});
    setCommitted(false);
    setSuccess(null);
    setWarning(null);
  };

  const save = async (event: FormEvent) => {
    event.preventDefault();
    if (!canEdit || busy || committed || mutationInFlight.current) return;
    const errors: FieldErrors = {};
    if (!form.managementItemCode.trim())
      errors.managementItemCode = ["관리항목 코드를 입력하세요."];
    if (!form.achievementDate)
      errors.achievementDate = ["업적발생일을 입력하세요."];
    if (!!form.specialLectureStartDate !== !!form.specialLectureEndDate) {
      errors.specialLectureEndDate = [
        "특강 시작일과 종료일을 함께 입력하세요.",
      ];
    } else if (form.specialLectureStartDate > form.specialLectureEndDate) {
      errors.specialLectureEndDate = ["특강 종료일은 시작일 이후여야 합니다."];
    }
    setFieldErrors(errors);
    setError(null);
    setSuccess(null);
    setWarning(null);
    if (
      Object.keys(errors).length ||
      !window.confirm("취업률 제고 실적을 저장하시겠습니까?")
    )
      return;
    mutationInFlight.current = true;
    setSaving(true);
    let accepted = false;
    try {
      const response = await apiRequest<SaveResult>(
        selected ? `${ROOT}/${selected.achievementId}` : ROOT,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify({
            managementItemCode: form.managementItemCode.trim(),
            achievementDate: form.achievementDate,
            specialLectureStartDate: form.specialLectureStartDate || undefined,
            specialLectureEndDate: form.specialLectureEndDate || undefined,
            mockExamQuestionPeriod:
              form.mockExamQuestionPeriod.trim() || undefined,
            attachmentIds: selected?.attachmentIds ?? [],
          }),
        },
      );
      accepted = true;
      setCommitted(true);
      setLookupFailed(true);
      if (!response.data?.achievement)
        throw new Error("저장 응답에 실적 정보가 없습니다.");
      const result = response.data;
      setSelected(result.achievement);
      if (result.occurredDateWarning) {
        setWarning(
          result.warningMessage ||
            "업적발생일이 평가기간 밖에 있습니다. 저장은 완료되었습니다.",
        );
      }
      const detail = await apiRequest<EmploymentRateImprovementAchievement>(
        `${ROOT}/${result.achievement.achievementId}`,
      );
      if (!detail.data) throw new Error("상세 응답이 없습니다.");
      setSelected(detail.data);
      setForm(toForm(detail.data));
      setLookupFailed(false);
      setSuccess("저장되었습니다. 상세 정보를 다시 조회했습니다.");
    } catch (caught) {
      setError(messageOf(caught));
      if (accepted) {
        setSuccess(
          "저장은 완료되었으나 상세 재조회에 실패했습니다. 상세 정보를 다시 선택하세요.",
        );
      } else if (caught instanceof ApiClientError) {
        const errors: FieldErrors = {};
        for (const field of caught.apiError?.fields ?? []) {
          (errors[field.field] ??= []).push(field.message);
        }
        setFieldErrors(errors);
      }
    } finally {
      if (accepted) await loadList();
      setSaving(false);
      mutationInFlight.current = false;
    }
  };

  if (!canRead) {
    return (
      <section
        data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT"
        data-testid="employment-rate-improvement-page"
      >
        <PermissionState
          title="취업률 제고 실적 관리 권한이 없습니다"
          message="R01, R02, R04 또는 R09 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT"
      data-testid="employment-rate-improvement-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 제고 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          특강기간과 모의평가 출제기간을 조회하고 관리합니다.
          인증상태·평가연도·소유자는 서버에서 관리합니다.
        </p>
      </div>
      {success ? <SuccessState message={success} /> : null}
      {warning ? (
        <div
          className="rounded-md border border-ld bg-lightwarning p-4 text-sm"
          role="status"
          data-testid="employment-rate-improvement-warning"
        >
          {warning}
        </div>
      ) : null}
      {error ? <ErrorState title="실적 처리 오류" message={error} /> : null}
      {listError ? (
        <ErrorState title="목록 조회 오류" message={listError} />
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="employment-rate-improvement-list-panel"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            실적 목록 <span className="text-sm text-muted">총 {total}건</span>
          </h2>
          <button
            className={buttonClass}
            type="button"
            disabled={loading || busy}
            onClick={() => void loadList()}
            data-testid="employment-rate-improvement-refresh"
          >
            새로고침
          </button>
        </div>
        {loading ? (
          <LoadingState message="실적 목록을 불러오는 중입니다." />
        ) : !listError && rows.length === 0 ? (
          <EmptyState message="등록된 취업률 제고 실적이 없습니다." />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table
              className="w-full text-left text-sm"
              data-testid="employment-rate-improvement-table"
            >
              <caption className="sr-only">취업률 제고 실적 목록</caption>
              <thead className="border-b border-ld bg-lightgray">
                <tr>
                  {[
                    "관리번호",
                    "성명",
                    "평가연도",
                    "관리항목",
                    "업적발생일",
                    "특강기간",
                    "모의평가 출제기간",
                    "인증상태",
                    "상세",
                  ].map((title) => (
                    <th
                      className="whitespace-nowrap p-3"
                      scope="col"
                      key={title}
                    >
                      {title}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    className="border-b border-ld"
                    key={row.achievementId}
                    data-testid={`employment-rate-improvement-row-${row.achievementId}`}
                  >
                    <td className="p-3">{row.managementNo}</td>
                    <td className="p-3">{row.teacherName}</td>
                    <td className="p-3">{row.evaluationYear}</td>
                    <td className="p-3">{row.managementItemCode}</td>
                    <td className="whitespace-nowrap p-3">
                      {row.achievementDate}
                    </td>
                    <td className="whitespace-nowrap p-3">
                      {row.specialLectureStartDate || "-"} ~{" "}
                      {row.specialLectureEndDate || "-"}
                    </td>
                    <td className="p-3">{row.mockExamQuestionPeriod || "-"}</td>
                    <td className="p-3">
                      {statusLabel(row.certificationStatus)}
                    </td>
                    <td className="p-3">
                      <button
                        className={buttonClass}
                        type="button"
                        disabled={busy}
                        onClick={() => void selectDetail(row.achievementId)}
                        aria-label={`${row.managementNo} 상세`}
                        data-testid={`employment-rate-improvement-detail-${row.achievementId}`}
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
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
          <label
            className="flex items-center gap-2 text-sm"
            htmlFor="employment-page-size"
          >
            페이지 크기
            <select
              id="employment-page-size"
              className={controlClass}
              value={pageSize}
              disabled={loading || busy}
              onChange={(event) => {
                setPage(0);
                setPageSize(Number(event.target.value));
              }}
              data-testid="employment-rate-improvement-page-size"
            >
              {[20, 50, 100].map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
          <nav
            className="flex items-center gap-3"
            aria-label="실적 목록 페이지"
          >
            <button
              className={buttonClass}
              type="button"
              disabled={loading || busy || page === 0}
              onClick={() => setPage((value) => value - 1)}
              data-testid="employment-rate-improvement-previous"
            >
              이전
            </button>
            <span className="text-sm">
              {page + 1} / {Math.max(1, Math.ceil(total / pageSize))}
            </span>
            <button
              className={buttonClass}
              type="button"
              disabled={loading || busy || (page + 1) * pageSize >= total}
              onClick={() => setPage((value) => value + 1)}
              data-testid="employment-rate-improvement-next"
            >
              다음
            </button>
          </nav>
        </div>
      </section>

      <form
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        onSubmit={(event) => void save(event)}
        noValidate
        data-testid="employment-rate-improvement-form"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            {selected ? "실적 상세 / 수정" : "신규 실적 등록"}
          </h2>
          <button
            className={buttonClass}
            type="button"
            disabled={!canCreate || busy}
            onClick={newDraft}
            data-testid="employment-rate-improvement-new"
          >
            신규 등록
          </button>
        </div>
        {detailLoading ? (
          <LoadingState message="실적 상세를 불러오는 중입니다." />
        ) : null}
        {!canEdit ? (
          <p
            className="mb-4 text-sm text-warning"
            data-testid="employment-rate-improvement-readonly"
          >
            조회 전용입니다. 저장은 R01 본인 실적 또는 R09 관리자의 작성·반려
            상태에서만 가능합니다. 상세 조회에 실패한 경우 다시 선택하세요.
          </p>
        ) : null}
        <div className="mb-5 grid gap-4 md:grid-cols-4">
          <ReadonlyField
            label="관리번호"
            id="employment-management-no"
            value={selected?.managementNo ?? "저장 후 부여"}
          />
          <ReadonlyField
            label="소유자"
            id="employment-owner"
            value={
              selected
                ? `${selected.teacherName} (${selected.teacherUserId})`
                : (user?.name ?? "-")
            }
          />
          <ReadonlyField
            label="평가연도"
            id="employment-year"
            value={String(selected?.evaluationYear ?? "서버에서 결정")}
          />
          <ReadonlyField
            label="인증상태"
            id="employment-status"
            value={statusLabel(selected?.certificationStatus ?? "DRAFT")}
          />
        </div>
        <fieldset
          className="grid gap-4 md:grid-cols-2"
          disabled={!canEdit || busy}
        >
          <legend className="sr-only">실적 입력 항목</legend>
          {(Object.keys(blankForm()) as (keyof Form)[]).map((field) => (
            <div key={field}>
              <label
                className="mb-2 block text-sm font-semibold text-dark"
                htmlFor={`employment-${field}`}
              >
                {fieldLabel(field)}
              </label>
              <input
                id={`employment-${field}`}
                className={controlClass}
                type={field.endsWith("Date") ? "date" : "text"}
                value={form[field]}
                required={
                  field === "managementItemCode" || field === "achievementDate"
                }
                aria-invalid={!!fieldErrors[field]?.length}
                aria-describedby={
                  fieldErrors[field]?.length
                    ? `employment-${field}-errors`
                    : undefined
                }
                onChange={(event) => change(field, event.target.value)}
                data-testid={`employment-rate-improvement-${field.replace(
                  /[A-Z]/g,
                  (letter) => `-${letter.toLowerCase()}`,
                )}`}
              />
              {fieldErrors[field]?.length ? (
                <div
                  id={`employment-${field}-errors`}
                  className="mt-1 text-sm text-error"
                  role="alert"
                >
                  {fieldErrors[field].map((message, index) => (
                    <p key={index}>{message}</p>
                  ))}
                </div>
              ) : null}
            </div>
          ))}
        </fieldset>
        <section
          className="mt-5 rounded-md border border-ld p-4"
          data-testid="employment-rate-improvement-attachments"
        >
          <h3 className="text-sm font-semibold text-dark">첨부파일 참조</h3>
          <p className="mt-2 text-sm text-muted">
            기존 첨부 참조만 보존합니다. 첨부 업로드·다운로드 API가 제공되지
            않아 이 화면에서는 파일을 추가하거나 변경할 수 없습니다.
          </p>
          {selected?.attachmentIds?.length ? (
            <ul className="mt-2 space-y-1 text-sm">
              {selected.attachmentIds.map((id, index) => (
                <li
                  className="break-all"
                  key={`${id}-${index}`}
                  data-testid={`employment-rate-improvement-attachment-${index}`}
                >
                  {id}
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-2 text-sm text-muted">첨부 참조 없음</p>
          )}
        </section>
        {Object.entries(fieldErrors)
          .filter(([field]) => !(field in form))
          .map(([field, messages]) => (
            <div className="mt-2 text-sm text-error" role="alert" key={field}>
              {messages.map((message, index) => (
                <p key={index}>{message}</p>
              ))}
            </div>
          ))}
        <div className="mt-5 flex items-center justify-end gap-3">
          <p className="text-sm text-muted">
            입력기간과 확정 여부는 저장 시 서버에서 확인합니다.
          </p>
          <button
            className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            type="submit"
            disabled={!canEdit || busy || committed}
            data-testid="employment-rate-improvement-save"
          >
            {saving ? "저장 중" : "저장"}
          </button>
        </div>
      </form>
    </section>
  );
}

function ReadonlyField({
  label,
  id,
  value,
}: {
  label: string;
  id: string;
  value: string;
}) {
  return (
    <div>
      <label
        className="mb-2 block text-sm font-semibold text-dark"
        htmlFor={id}
      >
        {label}
      </label>
      <input
        id={id}
        className={controlClass}
        value={value}
        readOnly
        data-testid={`${id}-readonly`}
      />
    </div>
  );
}

function fieldLabel(field: keyof Form) {
  return {
    managementItemCode: "관리항목 코드",
    achievementDate: "업적발생일",
    specialLectureStartDate: "특강 시작일",
    specialLectureEndDate: "특강 종료일",
    mockExamQuestionPeriod: "모의평가 출제기간",
  }[field];
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    DRAFT: "작성",
    SUBMITTED: "제출",
    DEPARTMENT_CONFIRMED: "학과 확인",
    DEPARTMENT_REJECTED: "학과 반려",
    CERTIFIED: "인증",
    CERTIFICATION_REJECTED: "인증 반려",
    EVALUATION_CONFIRMED: "평가확정",
    DELETED: "삭제",
  };
  return labels[status] ?? status;
}

import { useEffect, useState, type FormEvent } from "react";
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

export type EmploymentRateImprovement = {
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
type Option = { managementItemCode: string; managementItemName: string };
type Page = {
  achievements: EmploymentRateImprovement[];
  totalElements: number;
  managementItems: Option[];
};
type Saved = {
  achievement: EmploymentRateImprovement;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
const base = "/api/business/employment-rate-improvements";
const blank: Form = {
  managementItemCode: "",
  achievementDate: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
  attachmentIds: [],
};

export function EmploymentRateImprovementPage({
  user,
}: {
  user: CurrentUser | null;
}) {
  const [form, setForm] = useState<Form>(blank);
  const [selected, setSelected] = useState<EmploymentRateImprovement | null>(
    null,
  );
  const [rows, setRows] = useState<EmploymentRateImprovement[]>([]);
  const [options, setOptions] = useState<Option[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [filter, setFilter] = useState("");
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [permission, setPermission] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const readAllowed = user?.roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const writeAllowed =
    user?.roles.includes("R09") ||
    (user?.roles.includes("R01") &&
      (!selected || selected.teacherUserId === user.userId));
  const editable =
    !selected ||
    ["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.achievementStatus,
    );
  const locked = !writeAllowed || !editable || busy;

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermission(caught.status === 403);
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
      setError("취업률 제고 실적을 처리하지 못했습니다.");
    }
  };

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize),
      });
      if (query) params.set("managementNo", query);
      const response = await apiRequest<Page>(`${base}?${params}`);
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
      setOptions(response.data?.managementItems ?? []);
    } catch (caught) {
      handleError(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (readAllowed) void load();
  }, [page, pageSize, query, readAllowed]);

  const select = async (id: number) => {
    setBusy(true);
    setError(null);
    setSuccess(null);
    setFields({});
    try {
      const response = await apiRequest<EmploymentRateImprovement>(
        `${base}/${id}`,
      );
      const row = response.data;
      if (!row) throw new Error("실적을 찾을 수 없습니다.");
      setSelected(row);
      setForm({
        managementItemCode: row.managementItemCode,
        achievementDate: row.achievementDate,
        specialLectureStartDate: row.specialLectureStartDate ?? "",
        specialLectureEndDate: row.specialLectureEndDate ?? "",
        mockExamQuestionPeriod: row.mockExamQuestionPeriod ?? "",
        attachmentIds: row.attachmentIds,
      });
      const optionsResponse = await apiRequest<Page>(
        `${base}?evaluationYear=${row.evaluationYear}`,
      );
      setOptions(optionsResponse.data?.managementItems ?? []);
    } catch (caught) {
      handleError(caught);
    } finally {
      setBusy(false);
    }
  };

  const save = async (event: FormEvent) => {
    event.preventDefault();
    if (locked) return;
    const errors: Record<string, string> = {};
    if (!form.managementItemCode)
      errors.managementItemCode = "관리항목을 선택하세요.";
    if (!form.achievementDate)
      errors.achievementDate = "업적발생일을 입력하세요.";
    if (
      Boolean(form.specialLectureStartDate) !==
        Boolean(form.specialLectureEndDate) ||
      (form.specialLectureEndDate &&
        form.specialLectureStartDate > form.specialLectureEndDate)
    ) {
      errors.specialLectureEndDate =
        "특강기간을 함께 입력하고 날짜 순서를 확인하세요.";
    }
    setFields(errors);
    if (
      Object.keys(errors).length ||
      !window.confirm("취업률 제고 실적을 저장하시겠습니까?")
    )
      return;
    setBusy(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await apiRequest<Saved>(
        selected ? `${base}/${selected.achievementId}` : base,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify({
            ...form,
            specialLectureStartDate: form.specialLectureStartDate || null,
            specialLectureEndDate: form.specialLectureEndDate || null,
            mockExamQuestionPeriod: form.mockExamQuestionPeriod || null,
          }),
        },
      );
      if (response.data) {
        setSelected(response.data.achievement);
        setSuccess(
          response.data.occurredDateWarning
            ? (response.data.warningMessage ??
                "기간 경고와 함께 저장되었습니다.")
            : "저장되었습니다.",
        );
      }
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setBusy(false);
    }
  };

  const changeDate = async (date: string) => {
    setForm((previous) => ({ ...previous, achievementDate: date }));
    if (!selected && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
      try {
        const response = await apiRequest<Page>(
          `${base}?evaluationYear=${date.slice(0, 4)}`,
        );
        setOptions(response.data?.managementItems ?? []);
      } catch (caught) {
        handleError(caught);
      }
    }
  };

  if (!readAllowed || permission)
    return (
      <section data-testid="employment-improvement-page">
        <PermissionState
          title="취업률 제고 실적 권한이 없습니다"
          message="해당 역할과 데이터 범위가 필요합니다."
        />
      </section>
    );

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT"
      data-testid="employment-improvement-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 제고 실적 관리
        </h1>
      </div>
      {error && <ErrorState title="실적 처리 오류" message={error} />}
      {success && <SuccessState title="처리 완료" message={success} />}
      <section className="rounded-md bg-white p-5 shadow-sm">
        <div className="flex flex-wrap items-end gap-3">
          <label>
            관리번호 검색
            <input
              className="form-input block"
              data-testid="employment-improvement-search-input"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
          </label>
          <button
            className="btn-primary"
            data-testid="employment-improvement-search-button"
            onClick={() => {
              setPage(0);
              setQuery(filter.trim());
              if (query === filter.trim()) void load();
            }}
          >
            조회
          </button>
          <button
            data-testid="employment-improvement-refresh"
            onClick={() => void load()}
          >
            새로고침
          </button>
          <label>
            표시 건수
            <select
              className="form-input block"
              data-testid="employment-improvement-page-size"
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
                  <th>관리번호</th>
                  <th>성명</th>
                  <th>관리항목</th>
                  <th>업적발생일</th>
                  <th>상태</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    key={row.achievementId}
                    data-testid={`employment-improvement-row-${row.achievementId}`}
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.teacherName}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.achievementDate}</td>
                    <td>{row.achievementStatus}</td>
                    <td>
                      <button
                        data-testid={`employment-improvement-detail-${row.achievementId}`}
                        disabled={busy}
                        onClick={() => void select(row.achievementId)}
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
        <div className="mt-4 flex items-center gap-4">
          <button
            data-testid="employment-improvement-previous"
            disabled={page === 0 || loading}
            onClick={() => setPage(page - 1)}
          >
            이전
          </button>
          <span>
            {page + 1} 페이지 / 총 {total}건
          </span>
          <button
            data-testid="employment-improvement-next"
            disabled={(page + 1) * pageSize >= total || loading}
            onClick={() => setPage(page + 1)}
          >
            다음
          </button>
        </div>
      </section>
      <form
        className="rounded-md bg-white p-5 shadow-sm"
        data-testid="employment-improvement-detail-panel"
        onSubmit={save}
      >
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">
            {selected ? "실적 상세·수정" : "신규 실적 등록"}
          </h2>
          <button
            type="button"
            data-testid="employment-improvement-new"
            disabled={
              busy || !user?.roles.some((role) => ["R01", "R09"].includes(role))
            }
            onClick={() => {
              setSelected(null);
              setForm(blank);
              setFields({});
              setSuccess(null);
              setError(null);
            }}
          >
            신규 입력
          </button>
        </div>
        {selected && (
          <p className="mt-2">
            {selected.managementNo} / {selected.teacherName} / 평가연도{" "}
            {selected.evaluationYear}
          </p>
        )}
        {!editable && (
          <p role="status" className="mt-3 text-error">
            확정 또는 제출 상태 실적은 수정할 수 없습니다.
          </p>
        )}
        {!writeAllowed && (
          <p className="mt-3 text-muted">
            본인 실적 입력 권한이 없어 조회만 가능합니다.
          </p>
        )}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <label>
            관리항목 *
            <select
              className="form-input block w-full"
              data-testid="employment-improvement-item"
              disabled={locked}
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            >
              <option value="">관리항목 선택</option>
              {options.map((option) => (
                <option
                  key={option.managementItemCode}
                  value={option.managementItemCode}
                >
                  {option.managementItemName}
                </option>
              ))}
            </select>
            {fields.managementItemCode && (
              <span role="alert">{fields.managementItemCode}</span>
            )}
          </label>
          <label>
            업적발생일 *
            <input
              type="date"
              className="form-input block w-full"
              data-testid="employment-improvement-date"
              disabled={locked}
              value={form.achievementDate}
              onChange={(event) => void changeDate(event.target.value)}
            />
            {fields.achievementDate && (
              <span role="alert">{fields.achievementDate}</span>
            )}
          </label>
          <label>
            특강 시작일
            <input
              type="date"
              className="form-input block w-full"
              data-testid="employment-improvement-start"
              disabled={locked}
              value={form.specialLectureStartDate}
              onChange={(event) =>
                setForm({
                  ...form,
                  specialLectureStartDate: event.target.value,
                })
              }
            />
          </label>
          <label>
            특강 종료일
            <input
              type="date"
              className="form-input block w-full"
              data-testid="employment-improvement-end"
              disabled={locked}
              value={form.specialLectureEndDate}
              onChange={(event) =>
                setForm({ ...form, specialLectureEndDate: event.target.value })
              }
            />
            {fields.specialLectureEndDate && (
              <span role="alert">{fields.specialLectureEndDate}</span>
            )}
          </label>
          <label className="md:col-span-2">
            모의시험 출제기간
            <textarea
              className="form-input block w-full"
              data-testid="employment-improvement-mock-period"
              disabled={locked}
              value={form.mockExamQuestionPeriod}
              onChange={(event) =>
                setForm({ ...form, mockExamQuestionPeriod: event.target.value })
              }
            />
          </label>
        </div>
        <p className="mt-3 text-sm text-muted">
          기존 첨부: {form.attachmentIds.join(", ") || "없음"}
        </p>
        <p className="text-sm text-muted">
          신규 첨부 업로드 및 상태 변경은 승인된 API가 없어 이 화면에서 제공하지
          않습니다.
        </p>
        <button
          className="btn-primary mt-4"
          data-testid="employment-improvement-save"
          disabled={locked}
          type="submit"
        >
          {busy ? "처리 중" : "저장"}
        </button>
        {Object.entries(fields)
          .filter(
            ([field]) =>
              ![
                "managementItemCode",
                "achievementDate",
                "specialLectureEndDate",
              ].includes(field),
          )
          .map(([field, message]) => (
            <p key={field} role="alert">
              {message}
            </p>
          ))}
      </form>
    </section>
  );
}

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

export type EmploymentRateImprovement = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  specialLectureStartDate: string;
  specialLectureEndDate: string;
  mockExamQuestionPeriod: string;
  achievementStatus: string;
  attachmentRef?: string | null;
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  evaluationYear: string;
  specialLectureStartDate: string;
  specialLectureEndDate: string;
  mockExamQuestionPeriod: string;
  attachmentRef: string;
};
type Results = {
  achievements: EmploymentRateImprovement[];
  totalElements: number;
  managementItemCodes: string[];
  canCreate: boolean;
  canUpdate: boolean;
};
const path = "/api/business/employment-rate-improvements";
const empty: Form = {
  managementItemCode: "",
  achievementDate: "",
  evaluationYear: "",
  specialLectureStartDate: "",
  specialLectureEndDate: "",
  mockExamQuestionPeriod: "",
  attachmentRef: "",
};
const fields: {
  key: keyof Form;
  label: string;
  type: string;
  required: boolean;
}[] = [
  { key: "achievementDate", label: "업적발생일", type: "date", required: true },
  { key: "evaluationYear", label: "평가연도", type: "text", required: false },
  {
    key: "specialLectureStartDate",
    label: "특강 시작일",
    type: "date",
    required: true,
  },
  {
    key: "specialLectureEndDate",
    label: "특강 종료일",
    type: "date",
    required: true,
  },
  {
    key: "mockExamQuestionPeriod",
    label: "모의고사 출제기간",
    type: "text",
    required: true,
  },
  { key: "attachmentRef", label: "첨부 참조", type: "text", required: false },
];

/** Keeps detail readback separate from list and uses the existing education screen styling. */
export function EmploymentRateImprovementPage({
  user,
}: {
  user: CurrentUser | null;
}) {
  const [results, setResults] = useState<Results | null>(null);
  const [selected, setSelected] = useState<EmploymentRateImprovement | null>(
    null,
  );
  const [form, setForm] = useState<Form>(empty);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [filter, setFilter] = useState({
    managementNo: "",
    teacherName: "",
    achievementStatus: "",
  });
  const [query, setQuery] = useState(filter);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [denied, setDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const editable =
    !selected ||
    ["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.achievementStatus,
    );
  const owns =
    !selected ||
    selected.teacherUserId === user?.userId ||
    user?.roles.includes("R09");
  const canWrite =
    !!user?.roles.some((role) => ["R01", "R09"].includes(role)) &&
    !!(selected ? results?.canUpdate : results?.canCreate) &&
    editable &&
    owns;

  const failure = (caught: unknown) => {
    setError(
      caught instanceof Error
        ? caught.message
        : "취업률 제고 실적 처리에 실패했습니다.",
    );
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setDenied(true);
      setErrors(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
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
      Object.entries(query).forEach(([key, value]) => {
        if (value.trim()) params.set(key, value.trim());
      });
      const response = await apiRequest<Results>(`${path}?${params}`);
      setResults(response.data ?? null);
    } catch (caught) {
      failure(caught);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [page, pageSize, query]);

  const detail = async (id: number) => {
    setBusy(true);
    setSuccess(null);
    setError(null);
    setErrors({});
    try {
      const response = await apiRequest<EmploymentRateImprovement>(
        `${path}/${id}`,
      );
      const row = response.data;
      if (row) {
        setSelected(row);
        setForm({
          managementItemCode: row.managementItemCode,
          achievementDate: row.achievementDate,
          evaluationYear: row.evaluationYear,
          specialLectureStartDate: row.specialLectureStartDate,
          specialLectureEndDate: row.specialLectureEndDate,
          mockExamQuestionPeriod: row.mockExamQuestionPeriod,
          attachmentRef: row.attachmentRef ?? "",
        });
      }
    } catch (caught) {
      failure(caught);
    } finally {
      setBusy(false);
    }
  };
  const save = async (submit = false) => {
    const required = [
      "managementItemCode",
      "achievementDate",
      "specialLectureStartDate",
      "specialLectureEndDate",
      "mockExamQuestionPeriod",
    ] as const;
    const invalid = Object.fromEntries(
      required
        .filter((key) => !form[key].trim())
        .map((key) => [key, "필수 항목입니다."]),
    );
    if (form.specialLectureEndDate < form.specialLectureStartDate) {
      invalid.specialLectureEndDate = "종료일은 시작일보다 빠를 수 없습니다.";
    }
    setErrors(invalid);
    if (
      Object.keys(invalid).length ||
      !canWrite ||
      !window.confirm("취업률 제고 실적을 저장하시겠습니까?")
    )
      return;
    setBusy(true);
    setError(null);
    try {
      const { evaluationYear, ...values } = form;
      const response = await apiRequest<{
        achievement: EmploymentRateImprovement;
        occurredDateWarning: boolean;
        warningMessage?: string;
      }>(selected ? `${path}/${selected.achievementId}` : path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          ...values,
          attachmentRef: values.attachmentRef || null,
          ...(!selected && evaluationYear ? { evaluationYear } : {}),
          ...(submit && selected ? { achievementStatus: "SUBMITTED" } : {}),
        }),
      });
      if (response.data) {
        await detail(response.data.achievement.achievementId);
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
  };

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT"
      data-testid="employment-rate-improvement-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-muted">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 제고 실적 관리
        </h1>
      </div>
      {denied ? (
        <PermissionState
          title="접근 권한이 없습니다"
          message="역할·기능권한·데이터 범위를 확인하세요."
        />
      ) : null}
      {error ? <ErrorState title="실적 처리 오류" message={error} /> : null}
      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="improvement-search-panel"
      >
        <div className="grid gap-4 md:grid-cols-4">
          {(
            [
              ["managementNo", "관리번호"],
              ["teacherName", "성명"],
              ["achievementStatus", "상태"],
            ] as const
          ).map(([key, label]) => (
            <label key={key}>
              {label}
              <input
                data-testid={`improvement-filter-${key}`}
                value={filter[key]}
                onChange={(event) =>
                  setFilter({ ...filter, [key]: event.target.value })
                }
              />
            </label>
          ))}
          <button
            data-testid="improvement-search"
            onClick={() => {
              setPage(0);
              setQuery({ ...filter });
            }}
          >
            조회
          </button>
        </div>
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="improvement-list-panel"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">
            실적 목록 ({results?.totalElements ?? 0}건)
          </h2>
          <label>
            표시 건수
            <select
              data-testid="improvement-page-size"
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
          <button
            data-testid="improvement-new"
            disabled={!results?.canCreate || busy}
            onClick={() => {
              setSelected(null);
              setForm(empty);
              setErrors({});
              setSuccess(null);
            }}
          >
            신규 입력
          </button>
        </div>
        {loading ? (
          <LoadingState />
        ) : results?.achievements.length ? (
          <div className="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>성명</th>
                  <th>관리항목</th>
                  <th>발생일</th>
                  <th>상태</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {results.achievements.map((row) => (
                  <tr
                    key={row.achievementId}
                    data-testid={`improvement-row-${row.achievementId}`}
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.teacherName}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.achievementDate}</td>
                    <td>{row.achievementStatus}</td>
                    <td>
                      <button
                        data-testid={`improvement-detail-${row.achievementId}`}
                        disabled={busy}
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
        ) : !error ? (
          <EmptyState title="조회된 실적이 없습니다" />
        ) : null}
        <div className="mt-4 flex gap-3">
          <button
            data-testid="improvement-previous"
            disabled={page === 0 || loading}
            onClick={() => setPage(page - 1)}
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            data-testid="improvement-next"
            disabled={
              loading || (page + 1) * pageSize >= (results?.totalElements ?? 0)
            }
            onClick={() => setPage(page + 1)}
          >
            다음
          </button>
        </div>
      </section>
      <form
        className="rounded-md border border-ld bg-white p-5"
        data-testid="improvement-detail-panel"
        onSubmit={(event) => {
          event.preventDefault();
          void save();
        }}
      >
        <h2 className="text-lg font-semibold">
          {selected ? "실적 상세" : "신규 실적"}
        </h2>
        {selected ? (
          <p>
            관리번호 {selected.managementNo} / 성명 {selected.teacherName} /{" "}
            {selected.achievementStatus}
          </p>
        ) : null}
        {!editable ? (
          <p data-testid="improvement-lock">
            제출·인증·평가확정된 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        {!owns ? <p>본인의 실적만 수정할 수 있습니다.</p> : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <label>
            관리항목 *
            <select
              data-testid="improvement-management-item"
              required
              value={form.managementItemCode}
              disabled={!canWrite || busy || loading}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            >
              <option value="">관리항목 선택</option>
              {results?.managementItemCodes.map((code) => (
                <option key={code} value={code}>
                  {code}
                </option>
              ))}
            </select>
            {errors.managementItemCode ? (
              <span className="text-error">{errors.managementItemCode}</span>
            ) : null}
          </label>
          {fields.map((field) => (
            <label key={field.key}>
              {field.label}
              {field.required ? " *" : ""}
              <input
                data-testid={`improvement-${field.key}`}
                type={field.type}
                required={field.required}
                disabled={
                  !canWrite ||
                  busy ||
                  loading ||
                  (field.key === "evaluationYear" && !!selected)
                }
                value={form[field.key]}
                onChange={(event) =>
                  setForm({ ...form, [field.key]: event.target.value })
                }
              />
              {errors[field.key] ? (
                <span className="text-error">{errors[field.key]}</span>
              ) : null}
            </label>
          ))}
        </div>
        <div className="mt-4 flex gap-3">
          <button
            className="rounded-md bg-primary px-4 py-2 text-white"
            data-testid="improvement-save"
            disabled={!canWrite || loading || busy || denied}
            type="submit"
          >
            저장
          </button>
          {selected && editable ? (
            <button
              data-testid="improvement-submit"
              disabled={!canWrite || loading || busy || denied}
              type="button"
              onClick={() => void save(true)}
            >
              제출
            </button>
          ) : null}
        </div>
      </form>
    </section>
  );
}

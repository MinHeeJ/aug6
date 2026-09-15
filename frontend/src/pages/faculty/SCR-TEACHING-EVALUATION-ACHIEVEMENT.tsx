import { useEffect, useState, type ReactNode } from "react";
import { ApiClientError, type ApiErrorField } from "../../api/apiClient";
import {
  teachingEvaluationAchievementApi,
  type TeachingEvaluationAchievement,
} from "../../api/achievementApi";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  evaluationYear: string;
  academicYear: string;
  semester: string;
  courseCode: string;
  courseName: string;
  evaluationScore: string;
  dynamicFields: string;
  attachmentRefs: string;
  changeReason: string;
};

const emptyForm: FormState = {
  evaluationYear: "",
  academicYear: "",
  semester: "",
  courseCode: "",
  courseName: "",
  evaluationScore: "",
  dynamicFields: "",
  attachmentRefs: "",
  changeReason: "",
};

export function TeachingEvaluationAchievementPage() {
  const [evaluationYear, setEvaluationYear] = useState("");
  const [academicYear, setAcademicYear] = useState("");
  const [semester, setSemester] = useState("");
  const [keyword, setKeyword] = useState("");
  const [rows, setRows] = useState<TeachingEvaluationAchievement[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState<20 | 50 | 100>(20);
  const [total, setTotal] = useState(0);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await teachingEvaluationAchievementApi.list({
        page,
        size,
        evaluationYear: evaluationYear.trim() || undefined,
        academicYear: academicYear.trim() || undefined,
        semester: semester.trim() || undefined,
        courseKeyword: keyword.trim() || undefined,
      });
      setRows(response.data?.teachingEvaluationAchievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const save = async () => {
    if (!window.confirm("강의평가 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const dynamicFields = parseDynamicFields(form.dynamicFields);
      const attachmentRefs = form.attachmentRefs
        .split("\n")
        .map((value) => value.trim())
        .filter(Boolean);
      await teachingEvaluationAchievementApi.save({
        evaluationYear: form.evaluationYear.trim(),
        academicYear: form.academicYear.trim(),
        semester: form.semester.trim(),
        courseCode: form.courseCode.trim(),
        courseName: form.courseName.trim(),
        evaluationScore: Number(form.evaluationScore),
        dynamicFields,
        attachmentRefs,
        changeReason: form.changeReason.trim(),
      });
      setSuccess("저장되었습니다");
      setForm(emptyForm);
      setPage(0);
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setFieldErrors(toFieldErrors(caught.apiError?.fields ?? []));
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "강의평가 실적을 처리하지 못했습니다.",
    );
  };

  const updateForm = (field: keyof FormState, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-TEACHING-EVALUATION-ACHIEVEMENT"
      data-testid="teaching-evaluation-achievement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-muted">교육영역 · 강의평가</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          강의평가 실적 관리
        </h1>
      </header>

      <section
        className="rounded-md border border-ld bg-white p-6"
        data-testid="teaching-evaluation-search-panel"
      >
        <h2 className="text-lg font-semibold text-dark">검색조건</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Field label="평가연도">
            <input
              data-testid="teaching-evaluation-year-filter"
              value={evaluationYear}
              onChange={(event) => setEvaluationYear(event.target.value)}
            />
          </Field>
          <Field label="학사연도">
            <input
              data-testid="teaching-evaluation-academic-year-filter"
              value={academicYear}
              onChange={(event) => setAcademicYear(event.target.value)}
            />
          </Field>
          <Field label="학기">
            <input
              data-testid="teaching-evaluation-semester-filter"
              value={semester}
              onChange={(event) => setSemester(event.target.value)}
            />
          </Field>
          <Field label="강좌 검색">
            <input
              data-testid="teaching-evaluation-course-filter"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </Field>
        </div>
        <button
          className="mt-4 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
          data-testid="teaching-evaluation-search-button"
          type="button"
          onClick={() => {
            setPage(0);
            void load();
          }}
        >
          검색
        </button>
      </section>

      {permissionDenied && (
        <PermissionState
          title="권한이 없습니다"
          message="강의평가 실적 조회 권한을 확인해 주세요."
        />
      )}
      {error && <ErrorState title="처리 오류" message={error} />}
      {success && <SuccessState title="저장 완료" message={success} />}

      <section className="rounded-md border border-ld bg-white p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            강의평가 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수{" "}
            <select
              data-testid="teaching-evaluation-page-size"
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value) as 20 | 50 | 100);
              }}
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState
            title="목록을 불러오는 중"
            message="강의평가 실적을 조회하고 있습니다."
          />
        ) : rows.length === 0 ? (
          <EmptyState
            title="등록된 강의평가 실적이 없습니다"
            message="검색조건을 변경하거나 상세 입력에서 새 실적을 저장하세요."
          />
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-ld text-muted">
                  <th className="p-2">평가연도</th>
                  <th className="p-2">강좌코드</th>
                  <th className="p-2">강좌명</th>
                  <th className="p-2">점수</th>
                  <th className="p-2">상태</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    className="border-b border-ld"
                    data-testid={`teaching-evaluation-row-${row.achievementId}`}
                    key={row.achievementId}
                  >
                    <td className="p-2">{row.evaluationYear}</td>
                    <td className="p-2">{row.courseCode}</td>
                    <td className="p-2">{row.courseName}</td>
                    <td className="p-2">{row.evaluationScore}</td>
                    <td className="p-2">{row.evaluationStatus}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="mt-4 flex items-center gap-3 text-sm">
          <button
            data-testid="teaching-evaluation-prev-page"
            disabled={page === 0}
            type="button"
            onClick={() => setPage((value) => Math.max(0, value - 1))}
          >
            이전
          </button>
          <span>
            {page + 1} / {Math.max(1, Math.ceil(total / size))}
          </span>
          <button
            data-testid="teaching-evaluation-next-page"
            disabled={(page + 1) * size >= total}
            type="button"
            onClick={() => setPage((value) => value + 1)}
          >
            다음
          </button>
        </div>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-6"
        data-testid="teaching-evaluation-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">상세 입력</h2>
        <p className="mt-1 text-sm text-muted">
          * 필수 입력 항목을 확인해 주세요.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <FormInput
            field="evaluationYear"
            form={form}
            label="평가연도 *"
            onChange={updateForm}
            error={fieldErrors.evaluationYear}
          />
          <FormInput
            field="academicYear"
            form={form}
            label="학사연도 *"
            onChange={updateForm}
            error={fieldErrors.academicYear}
          />
          <FormInput
            field="semester"
            form={form}
            label="학기 *"
            onChange={updateForm}
            error={fieldErrors.semester}
          />
          <FormInput
            field="courseCode"
            form={form}
            label="강좌코드 *"
            onChange={updateForm}
            error={fieldErrors.courseCode}
          />
          <FormInput
            field="courseName"
            form={form}
            label="강좌명 *"
            onChange={updateForm}
            error={fieldErrors.courseName}
          />
          <FormInput
            field="evaluationScore"
            form={form}
            label="강의평가 점수 *"
            onChange={updateForm}
            error={fieldErrors.evaluationScore}
            type="number"
          />
          <FormInput
            field="changeReason"
            form={form}
            label="변경 사유 *"
            onChange={updateForm}
            error={fieldErrors.changeReason}
          />
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field label="동적 필드 (키:값, 줄바꿈 구분)">
            <textarea
              data-testid="teaching-evaluation-dynamic-fields"
              value={form.dynamicFields}
              onChange={(event) =>
                updateForm("dynamicFields", event.target.value)
              }
            />
          </Field>
          <Field label="첨부파일 (보안 참조값, 줄바꿈 구분)">
            <textarea
              data-testid="teaching-evaluation-attachments"
              value={form.attachmentRefs}
              onChange={(event) =>
                updateForm("attachmentRefs", event.target.value)
              }
            />
          </Field>
        </div>
        <button
          className="mt-5 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
          data-testid="teaching-evaluation-save-button"
          disabled={saving}
          type="button"
          onClick={() => void save()}
        >
          {saving ? "저장 중" : "저장"}
        </button>
      </section>
    </section>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="grid gap-1 text-sm font-medium text-dark">
      <span>{label}</span>
      {children}
    </label>
  );
}
function FormInput({
  field,
  form,
  label,
  onChange,
  error,
  type = "text",
}: {
  field: keyof FormState;
  form: FormState;
  label: string;
  onChange: (field: keyof FormState, value: string) => void;
  error?: string;
  type?: string;
}) {
  return (
    <Field label={label}>
      <input
        data-testid={`teaching-evaluation-${field}`}
        type={type}
        value={form[field]}
        onChange={(event) => onChange(field, event.target.value)}
      />
      {error && <span className="text-sm text-error">{error}</span>}
    </Field>
  );
}
function toFieldErrors(errors: ApiErrorField[]) {
  return Object.fromEntries(
    errors.map((error) => [error.field, error.message]),
  );
}
function parseDynamicFields(value: string): Record<string, string> {
  const fields: Record<string, string> = {};
  for (const line of value.split("\n")) {
    const [key, ...rest] = line.split(":");
    if (key?.trim() && rest.length) fields[key.trim()] = rest.join(":").trim();
  }
  return fields;
}

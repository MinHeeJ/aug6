import { useEffect, useState, type ReactNode } from "react";
import { ApiClientError, type ApiErrorField } from "../../api/apiClient";
import {
  graduateAchievementApi,
  type GraduateAchievement,
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
  studentNo: string;
  studentName: string;
  degreeType: "MASTER" | "DOCTOR";
  thesisTitle: string;
  awardDate: string;
  changeReason: string;
};
const emptyForm: FormState = {
  evaluationYear: "",
  academicYear: "",
  semester: "",
  studentNo: "",
  studentName: "",
  degreeType: "MASTER",
  thesisTitle: "",
  awardDate: "",
  changeReason: "",
};

export function GraduateAchievementPage() {
  const [filters, setFilters] = useState({
    studentKeyword: "",
    degreeType: "",
  });
  const [rows, setRows] = useState<GraduateAchievement[]>([]);
  const [size, setSize] = useState<20 | 50 | 100>(20);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [editingAchievementId, setEditingAchievementId] = useState<
    number | undefined
  >();
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
      const response = await graduateAchievementApi.list({
        studentKeyword: filters.studentKeyword,
        degreeType: filters.degreeType,
        size,
      });
      setRows(response.data?.graduateAchievements ?? []);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [size]);
  const save = async () => {
    if (!window.confirm("석·박사 배출 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await graduateAchievementApi.save({
        achievementId: editingAchievementId,
        ...form,
        dynamicFields: {},
        attachmentRefs: [],
      });
      setSuccess("지도학생 세부내역이 저장되었습니다.");
      setForm(emptyForm);
      setEditingAchievementId(undefined);
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
        : "석·박사 배출 실적을 처리하지 못했습니다.",
    );
  };
  const update = (field: keyof FormState, value: string) =>
    setForm((current) => ({ ...current, [field]: value }) as FormState);
  const selectRow = (row: GraduateAchievement) => {
    setEditingAchievementId(row.achievementId);
    setForm({
      evaluationYear: row.evaluationYear,
      academicYear: row.academicYear,
      semester: row.semester,
      studentNo: row.studentNo,
      studentName: row.studentName,
      degreeType: row.degreeType,
      thesisTitle: row.thesisTitle,
      awardDate: row.awardDate,
      changeReason: "석·박사 배출 실적 수정",
    });
  };

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-GRADUATE-ACHIEVEMENT"
      data-testid="graduate-achievement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-muted">교육영역 · 석·박사 배출</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          석·박사 배출 실적 관리
        </h1>
      </header>
      <section
        className="rounded-md border border-ld bg-white p-6"
        data-testid="graduate-achievement-search-panel"
      >
        <h2 className="text-lg font-semibold text-dark">검색조건</h2>
        <div className="mt-4 flex flex-wrap gap-3">
          <input
            data-testid="graduate-achievement-student-filter"
            placeholder="학번 또는 학생명"
            value={filters.studentKeyword}
            onChange={(event) =>
              setFilters((current) => ({
                ...current,
                studentKeyword: event.target.value,
              }))
            }
          />
          <select
            data-testid="graduate-achievement-degree-filter"
            value={filters.degreeType}
            onChange={(event) =>
              setFilters((current) => ({
                ...current,
                degreeType: event.target.value,
              }))
            }
          >
            <option value="">전체 학위</option>
            <option value="MASTER">석사</option>
            <option value="DOCTOR">박사</option>
          </select>
          <button
            data-testid="graduate-achievement-search-button"
            className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            type="button"
            onClick={() => void load()}
          >
            검색
          </button>
        </div>
      </section>
      {permissionDenied && (
        <PermissionState
          title="권한이 없습니다"
          message="석·박사 배출 실적 조회 권한을 확인해 주세요."
        />
      )}
      {error && <ErrorState title="처리 오류" message={error} />}
      {success && <SuccessState title="저장 완료" message={success} />}
      <section className="rounded-md border border-ld bg-white p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            석·박사 배출 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수{" "}
            <select
              data-testid="graduate-achievement-page-size"
              value={size}
              onChange={(event) =>
                setSize(Number(event.target.value) as 20 | 50 | 100)
              }
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
            message="석·박사 배출 실적을 조회하고 있습니다."
          />
        ) : rows.length === 0 ? (
          <EmptyState
            title="등록된 석·박사 배출 실적이 없습니다"
            message="상세 입력에서 지도학생 세부내역을 저장하세요."
          />
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-ld text-muted">
                  <th className="p-2">학위구분</th>
                  <th className="p-2">학생</th>
                  <th className="p-2">논문제목</th>
                  <th className="p-2">수여일</th>
                  <th className="p-2">상태</th>
                  <th className="p-2">선택</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    data-testid={`graduate-achievement-row-${row.achievementId}`}
                    className="border-b border-ld"
                    key={row.achievementId}
                  >
                    <td className="p-2">
                      {row.degreeType === "MASTER" ? "석사" : "박사"}
                    </td>
                    <td className="p-2">
                      {row.studentNo} {row.studentName}
                    </td>
                    <td className="p-2">{row.thesisTitle}</td>
                    <td className="p-2">{row.awardDate}</td>
                    <td className="p-2">{row.achievementStatus}</td>
                    <td className="p-2">
                      <button
                        data-testid={`graduate-achievement-edit-${row.achievementId}`}
                        type="button"
                        onClick={() => selectRow(row)}
                      >
                        수정
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <section
        className="rounded-md border border-ld bg-white p-6"
        data-testid="graduate-achievement-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">지도학생 상세 입력</h2>
        <p className="mt-1 text-sm text-muted">
          * 필수 입력 항목을 확인해 주세요.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field label="평가연도 *">
            <input
              data-testid="graduate-achievement-evaluation-year"
              value={form.evaluationYear}
              onChange={(event) => update("evaluationYear", event.target.value)}
            />
            {fieldErrors.evaluationYear && (
              <ErrorText value={fieldErrors.evaluationYear} />
            )}
          </Field>
          <Field label="학사연도 *">
            <input
              data-testid="graduate-achievement-academic-year"
              value={form.academicYear}
              onChange={(event) => update("academicYear", event.target.value)}
            />
          </Field>
          <Field label="학기 *">
            <input
              data-testid="graduate-achievement-semester"
              value={form.semester}
              onChange={(event) => update("semester", event.target.value)}
            />
          </Field>
          <Field label="학번 *">
            <input
              data-testid="graduate-achievement-student-no"
              value={form.studentNo}
              onChange={(event) => update("studentNo", event.target.value)}
            />
          </Field>
          <Field label="학생명 *">
            <input
              data-testid="graduate-achievement-student-name"
              value={form.studentName}
              onChange={(event) => update("studentName", event.target.value)}
            />
          </Field>
          <Field label="학위구분 *">
            <select
              data-testid="graduate-achievement-degree-type"
              value={form.degreeType}
              onChange={(event) => update("degreeType", event.target.value)}
            >
              <option value="MASTER">석사</option>
              <option value="DOCTOR">박사</option>
            </select>
            {fieldErrors.degreeType && (
              <ErrorText value={fieldErrors.degreeType} />
            )}
          </Field>
          <Field label="논문제목 *">
            <input
              data-testid="graduate-achievement-thesis-title"
              value={form.thesisTitle}
              onChange={(event) => update("thesisTitle", event.target.value)}
            />
            {fieldErrors.thesisTitle && (
              <ErrorText value={fieldErrors.thesisTitle} />
            )}
          </Field>
          <Field label="수여일 *">
            <input
              data-testid="graduate-achievement-award-date"
              type="date"
              value={form.awardDate}
              onChange={(event) => update("awardDate", event.target.value)}
            />
            {fieldErrors.awardDate && (
              <ErrorText value={fieldErrors.awardDate} />
            )}
          </Field>
          <Field label="변경 사유 *">
            <input
              data-testid="graduate-achievement-change-reason"
              value={form.changeReason}
              onChange={(event) => update("changeReason", event.target.value)}
            />
          </Field>
        </div>
        <button
          data-testid="graduate-achievement-save-button"
          className="mt-5 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
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
function ErrorText({ value }: { value: string }) {
  return <span className="text-sm text-error">{value}</span>;
}
function toFieldErrors(errors: ApiErrorField[]) {
  return Object.fromEntries(
    errors.map((error) => [error.field, error.message]),
  );
}

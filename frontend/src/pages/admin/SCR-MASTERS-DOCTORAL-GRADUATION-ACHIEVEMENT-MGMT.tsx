import { Download, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  degreeCompletionAchievementApi,
  type DegreeCompletionAchievement,
  type DegreeCompletionStudentInput,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type Filters = {
  managementNo: string;
  teacherName: string;
  certificationStatus: string;
};
type Form = {
  managementItemCode: string;
  attachmentRef: string;
  students: DegreeCompletionStudentInput[];
};
const emptyFilters: Filters = {
  managementNo: "",
  teacherName: "",
  certificationStatus: "",
};
const emptyStudent = (): DegreeCompletionStudentInput => ({
  degreeType: "MASTER",
  studentName: "",
  thesisTitle: "",
  degreeAwardedDate: "",
});
const emptyForm = (): Form => ({
  managementItemCode: "",
  attachmentRef: "",
  students: [emptyStudent()],
});

/** R01/R02/R04 석·박사 배출 실적의 검색, 상세 지도학생 편집, 저장 흐름을 제공한다. */
export function DegreeCompletionAchievementManagementPage() {
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [rows, setRows] = useState<DegreeCompletionAchievement[]>([]);
  const [selected, setSelected] = useState<DegreeCompletionAchievement | null>(
    null,
  );
  const [form, setForm] = useState<Form>(emptyForm);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState<PageSize>(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const finalized = selected?.certificationStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await degreeCompletionAchievementApi.list({
        ...filters,
        page,
        size,
      });
      setRows(response.data?.achievements ?? []);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectRow = (row: DegreeCompletionAchievement) => {
    setSelected(row);
    setForm({
      managementItemCode: row.managementItemCode,
      attachmentRef: row.attachmentRef ?? "",
      students: row.students.map((student) => ({
        degreeType: student.degreeType,
        studentName: student.studentName,
        thesisTitle: student.thesisTitle,
        degreeAwardedDate: student.degreeAwardedDate,
      })),
    });
    setFieldErrors({});
    setSuccess(null);
  };

  const updateStudent = (
    index: number,
    field: keyof DegreeCompletionStudentInput,
    value: string,
  ) => {
    setForm((current) => ({
      ...current,
      students: current.students.map((student, currentIndex) =>
        currentIndex === index
          ? ({ ...student, [field]: value } as DegreeCompletionStudentInput)
          : student,
      ),
    }));
  };

  const save = async (actionType?: "SUBMIT") => {
    if (finalized) {
      setError("평가확정된 실적은 수정하거나 삭제할 수 없습니다.");
      return;
    }
    const errors = validate(form);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    if (
      !window.confirm(
        actionType
          ? "석·박사 배출 실적을 제출하시겠습니까?"
          : "석·박사 배출 실적을 저장하시겠습니까?",
      )
    )
      return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await degreeCompletionAchievementApi.save({
        achievementId: selected?.achievementId,
        managementItemCode: form.managementItemCode.trim(),
        attachmentRef: form.attachmentRef.trim() || undefined,
        students: form.students,
        actionType,
      });
      if (response.data) selectRow(response.data);
      setSuccess("저장되었습니다. 지도학생 세부내역을 재조회했습니다.");
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
      setFieldErrors(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((item) => [
            item.field,
            item.message,
          ]),
        ),
      );
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "석·박사 배출 실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-testid="degree-completion-page">
        <PermissionState
          title="석·박사 배출 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 역할과 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT"
      data-testid="degree-completion-page"
    >
      <header className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 &gt; 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              석·박사 배출 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              배출 실적과 지도학생 세부내역을 함께 저장합니다.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 text-sm font-semibold text-primary"
              data-testid="degree-completion-excel-button"
              onClick={() => {
                downloadCsv("degree-completion-achievements.csv", rows, [
                  { header: "관리번호", value: (row) => row.managementNo },
                  { header: "성명", value: (row) => row.teacherName },
                  {
                    header: "관리항목",
                    value: (row) => row.managementItemCode,
                  },
                  {
                    header: "인증상태",
                    value: (row) => row.certificationStatus,
                  },
                ]);
                setSuccess("엑셀 다운로드 파일을 생성했습니다.");
              }}
              type="button"
            >
              <Download size={16} /> 엑셀 다운로드
            </button>
            <button
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
              data-testid="degree-completion-refresh-button"
              onClick={() => void load()}
              type="button"
            >
              <RefreshCw size={16} /> 새로고침
            </button>
          </div>
        </div>
      </header>
      {success ? (
        <SuccessState
          title={success}
          message="목록을 최신 저장값으로 갱신했습니다."
        />
      ) : null}
      {error ? (
        <ErrorState title="석·박사 배출 실적 처리 오류" message={error} />
      ) : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <div className="grid gap-4 md:grid-cols-4">
          <TextField
            label="관리번호"
            testId="degree-completion-management-no-input"
            value={filters.managementNo}
            onChange={(value) =>
              setFilters({ ...filters, managementNo: value })
            }
          />
          <TextField
            label="성명"
            testId="degree-completion-teacher-name-input"
            value={filters.teacherName}
            onChange={(value) => setFilters({ ...filters, teacherName: value })}
          />
          <TextField
            label="인증상태"
            testId="degree-completion-status-filter"
            value={filters.certificationStatus}
            onChange={(value) =>
              setFilters({ ...filters, certificationStatus: value })
            }
          />
          <button
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            data-testid="degree-completion-search-button"
            onClick={() => {
              setPage(0);
              void load();
            }}
            type="button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>
      <section className="grid gap-6 xl:grid-cols-5">
        <section className="rounded-md border border-ld bg-white p-6 xl:col-span-3">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark">목록</h2>
            <select
              data-testid="degree-completion-page-size-select"
              onChange={(event) => {
                setSize(Number(event.target.value) as PageSize);
                setPage(0);
              }}
              value={size}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </div>
          {loading ? (
            <LoadingState title="석·박사 배출 실적을 조회하고 있습니다" />
          ) : null}
          {!loading && rows.length === 0 ? (
            <EmptyState title="조회된 석·박사 배출 실적이 없습니다" />
          ) : null}
          {!loading && rows.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-ld text-muted">
                    <th>관리번호</th>
                    <th>성명</th>
                    <th>관리항목</th>
                    <th>지도학생</th>
                    <th>인증상태</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr
                      className="border-b border-ld"
                      data-testid={`degree-completion-row-${row.achievementId}`}
                      key={row.achievementId}
                    >
                      <td>{row.managementNo}</td>
                      <td>{row.teacherName}</td>
                      <td>{row.managementItemCode}</td>
                      <td>{row.students.length}명</td>
                      <td>{row.certificationStatus}</td>
                      <td>
                        <button
                          className="text-primary"
                          data-testid={`degree-completion-detail-${row.achievementId}`}
                          onClick={() => selectRow(row)}
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
        </section>
        <section
          className="rounded-md border border-ld bg-white p-6 xl:col-span-2"
          data-testid="degree-completion-detail-panel"
        >
          <h2 className="text-lg font-semibold text-dark">상세</h2>
          <div className="mt-4 space-y-4">
            <TextField
              error={fieldErrors.managementItemCode}
              label="관리항목 *"
              testId="degree-completion-management-item-input"
              value={form.managementItemCode}
              onChange={(value) =>
                setForm({ ...form, managementItemCode: value })
              }
            />
            <TextField
              label="첨부파일 참조"
              testId="degree-completion-attachment-ref-input"
              value={form.attachmentRef}
              onChange={(value) => setForm({ ...form, attachmentRef: value })}
            />
            <h3 className="text-base font-semibold text-dark">
              지도학생 세부내역
            </h3>
            {form.students.map((student, index) => (
              <div
                className="grid gap-2 rounded-md border border-ld p-3"
                data-testid={`degree-completion-student-row-${index}`}
                key={index}
              >
                <label className="text-sm font-medium text-dark">
                  학위구분 *
                  <select
                    className="mt-1 h-10 w-full rounded-md border border-ld px-3"
                    data-testid={`degree-completion-degree-type-${index}`}
                    onChange={(event) =>
                      updateStudent(index, "degreeType", event.target.value)
                    }
                    value={student.degreeType}
                  >
                    <option value="MASTER">석사</option>
                    <option value="DOCTOR">박사</option>
                  </select>
                </label>
                <TextField
                  label="학생명 *"
                  testId={`degree-completion-student-name-${index}`}
                  value={student.studentName}
                  onChange={(value) =>
                    updateStudent(index, "studentName", value)
                  }
                />
                <TextField
                  label="논문 제목 *"
                  testId={`degree-completion-thesis-title-${index}`}
                  value={student.thesisTitle}
                  onChange={(value) =>
                    updateStudent(index, "thesisTitle", value)
                  }
                />
                <label className="text-sm font-medium text-dark">
                  학위수여일 *
                  <input
                    className="mt-1 h-10 w-full rounded-md border border-ld px-3"
                    data-testid={`degree-completion-awarded-date-${index}`}
                    onChange={(event) =>
                      updateStudent(
                        index,
                        "degreeAwardedDate",
                        event.target.value,
                      )
                    }
                    type="date"
                    value={student.degreeAwardedDate}
                  />
                </label>
                {form.students.length > 1 ? (
                  <button
                    className="text-left text-sm text-error"
                    data-testid={`degree-completion-student-remove-${index}`}
                    onClick={() =>
                      setForm({
                        ...form,
                        students: form.students.filter(
                          (_, currentIndex) => currentIndex !== index,
                        ),
                      })
                    }
                    type="button"
                  >
                    학생 삭제
                  </button>
                ) : null}
              </div>
            ))}
            <button
              className="text-sm font-semibold text-primary"
              data-testid="degree-completion-student-add-button"
              onClick={() =>
                setForm({
                  ...form,
                  students: [...form.students, emptyStudent()],
                })
              }
              type="button"
            >
              + 지도학생 추가
            </button>
            {Object.values(fieldErrors).length > 0 ? (
              <p className="text-sm text-error">
                {Object.values(fieldErrors)[0]}
              </p>
            ) : null}
            <div className="flex flex-wrap gap-2">
              <button
                className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white disabled:opacity-60"
                data-testid="degree-completion-save-button"
                disabled={saving || finalized}
                onClick={() => void save()}
                type="button"
              >
                <Save size={16} />
                {saving ? "저장 중" : "저장"}
              </button>
              <button
                className="inline-flex h-10 items-center rounded-md border border-primary px-4 text-sm font-semibold text-primary disabled:opacity-60"
                data-testid="degree-completion-submit-button"
                disabled={saving || finalized}
                onClick={() => void save("SUBMIT")}
                type="button"
              >
                제출
              </button>
            </div>
          </div>
        </section>
      </section>
    </section>
  );
}
function TextField({
  label,
  value,
  onChange,
  testId,
  error,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  error?: string;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      {label}
      <input
        className="mt-1 h-10 w-full rounded-md border border-ld px-3"
        data-testid={testId}
        onChange={(event) => onChange(event.target.value)}
        value={value}
      />
      {error ? (
        <span className="mt-1 block text-sm text-error">{error}</span>
      ) : null}
    </label>
  );
}
function validate(form: Form): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!form.managementItemCode.trim())
    errors.managementItemCode = "관리항목은 필수입니다.";
  form.students.forEach((student) => {
    if (!student.degreeType) errors.degreeType = "학위구분은 필수입니다.";
    if (!student.studentName.trim())
      errors.studentName = "학생명은 필수입니다.";
    if (!student.thesisTitle.trim())
      errors.thesisTitle = "논문 제목은 필수입니다.";
    if (!student.degreeAwardedDate)
      errors.degreeAwardedDate = "학위수여일은 필수입니다.";
  });
  return errors;
}

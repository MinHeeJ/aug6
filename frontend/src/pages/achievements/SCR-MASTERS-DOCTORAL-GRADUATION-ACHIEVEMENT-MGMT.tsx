import { Plus, RefreshCw, Save, Search, Trash2 } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type DegreeStudent = {
  degreeCompletionStudentId?: number;
  degreeType: "MASTER" | "DOCTORAL" | "";
  studentName: string;
  thesisTitle: string;
  degreeAwardedDate: string;
};

type DegreeAchievement = {
  achievementId: number;
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  certificationStatus: string;
  attachmentRef?: string | null;
  students: DegreeStudent[];
};

type SearchResponse = {
  achievements: DegreeAchievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type Form = {
  achievementId?: number;
  managementItemCode: string;
  attachmentRef: string;
  students: DegreeStudent[];
};

const emptyStudent = (): DegreeStudent => ({
  degreeType: "",
  studentName: "",
  thesisTitle: "",
  degreeAwardedDate: "",
});

const emptyForm = (): Form => ({
  managementItemCode: "",
  attachmentRef: "",
  students: [emptyStudent()],
});

export function MastersDoctoralGraduationAchievementManagementPage() {
  const [managementNo, setManagementNo] = useState("");
  const [teacherName, setTeacherName] = useState("");
  const [certificationStatus, setCertificationStatus] = useState("");
  const [rows, setRows] = useState<DegreeAchievement[]>([]);
  const [form, setForm] = useState<Form>(emptyForm);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const confirmedSelected =
    form.achievementId !== undefined &&
    rows.some(
      (row) =>
        row.achievementId === form.achievementId &&
        row.certificationStatus === "EVALUATION_CONFIRMED",
    );

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<SearchResponse>(
        listPath(
          managementNo,
          teacherName,
          certificationStatus,
          page,
          pageSize,
        ),
      );
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleError(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const save = async () => {
    if (!window.confirm("석·박사 배출 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await apiRequest<DegreeAchievement>(
        "/api/business/degree-completion-achievements",
        {
          method: "POST",
          body: JSON.stringify({
            achievementId: form.achievementId,
            managementItemCode: form.managementItemCode.trim(),
            attachmentRef: form.attachmentRef.trim() || undefined,
            students: form.students,
          }),
        },
      );
      if (response.data) selectRow(response.data);
      setSuccess("저장되었습니다. 지도학생 세부내역을 다시 조회했습니다.");
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const selectRow = (row: DegreeAchievement) => {
    setForm({
      achievementId: row.achievementId,
      managementItemCode: row.managementItemCode,
      attachmentRef: row.attachmentRef ?? "",
      students: row.students.length ? row.students : [emptyStudent()],
    });
    setFieldErrors({});
    setSuccess(null);
  };

  const updateStudent = (
    index: number,
    key: keyof DegreeStudent,
    value: string,
  ) => {
    setForm((current) => ({
      ...current,
      students: current.students.map((student, studentIndex) =>
        studentIndex === index
          ? ({ ...student, [key]: value } as DegreeStudent)
          : student,
      ),
    }));
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      setFieldErrors(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
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
      <section
        data-screen-id="SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT"
        data-testid="degree-completion-achievement-page"
      >
        <PermissionState
          title="석·박사 배출 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 해당 데이터 범위가 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT"
      data-testid="degree-completion-achievement-page"
    >
      <section className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              석·박사 배출 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              실적 헤더와 지도학생 세부내역을 함께 저장하고 인증상태를
              확인합니다.
            </p>
          </div>
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="degree-completion-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </section>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="석·박사 배출 실적 오류" message={error} />
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="degree-completion-search-panel"
      >
        <div className="grid gap-4 md:grid-cols-4">
          <InputField label="관리번호">
            <input
              data-testid="degree-completion-management-no-input"
              value={managementNo}
              onChange={(event) => setManagementNo(event.target.value)}
            />
          </InputField>
          <InputField label="성명">
            <input
              data-testid="degree-completion-teacher-name-input"
              value={teacherName}
              onChange={(event) => setTeacherName(event.target.value)}
            />
          </InputField>
          <InputField label="인증상태">
            <input
              data-testid="degree-completion-status-input"
              value={certificationStatus}
              onChange={(event) => setCertificationStatus(event.target.value)}
            />
          </InputField>
          <button
            className="mt-7 inline-flex h-10 items-center justify-center gap-2 rounded-md border
              border-primary px-4 text-sm font-semibold text-primary"
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

      <section className="rounded-md border border-ld bg-white p-5 shadow-sm">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            석·박사 배출 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="degree-completion-page-size-select"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value) as 20 | 50 | 100);
                setPage(0);
              }}
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="석·박사 배출 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 석·박사 배출 실적이 없습니다"
            message="검색조건을 변경하거나 상세 영역에서 새 실적을 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리번호</th>
                  <th className="px-3 py-2">성명</th>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">인증상태</th>
                  <th className="px-3 py-2">지도학생</th>
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    data-testid="degree-completion-achievement-row"
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementNo}</td>
                    <td className="px-3 py-2">{row.teacherName}</td>
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.certificationStatus}</td>
                    <td className="px-3 py-2">{row.students.length}명</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid="degree-completion-detail-button"
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
        <p className="mt-3 text-xs text-muted">
          총 {total}건 / {page + 1}페이지
        </p>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="degree-completion-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">석·박사 배출 상세</h2>
        {confirmedSelected ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="degree-completion-confirmed-lock-message"
          >
            평가확정 실적은 수정하거나 첨부를 변경할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <InputField
            label="관리항목"
            required
            error={fieldErrors.managementItemCode}
          >
            <input
              data-testid="degree-completion-management-item-input"
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            />
          </InputField>
          <InputField label="첨부 참조">
            <input
              data-testid="degree-completion-attachment-ref-input"
              value={form.attachmentRef}
              onChange={(event) =>
                setForm({ ...form, attachmentRef: event.target.value })
              }
            />
          </InputField>
        </div>
        <div
          className="mt-6 overflow-x-auto"
          data-testid="degree-completion-student-subtable"
        >
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-base font-semibold text-dark">
              지도학생 세부내역
            </h3>
            <button
              className="inline-flex items-center gap-1 rounded border border-primary px-3 py-2
                text-sm font-semibold text-primary"
              data-testid="degree-completion-add-student-button"
              disabled={confirmedSelected}
              onClick={() =>
                setForm({
                  ...form,
                  students: [...form.students, emptyStudent()],
                })
              }
              type="button"
            >
              <Plus size={16} /> 학생 추가
            </button>
          </div>
          <table className="min-w-full divide-y divide-ld text-sm">
            <thead className="bg-lightsecondary text-left text-muted">
              <tr>
                <th className="px-3 py-2">학위구분 *</th>
                <th className="px-3 py-2">학생명 *</th>
                <th className="px-3 py-2">논문 제목 *</th>
                <th className="px-3 py-2">학위수여일 *</th>
                <th className="px-3 py-2">삭제</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {form.students.map((student, index) => (
                <tr
                  data-testid="degree-completion-student-row"
                  key={`${student.degreeCompletionStudentId ?? "new"}-${index}`}
                >
                  <td className="px-3 py-2">
                    <select
                      data-testid={`degree-completion-degree-type-${index}`}
                      disabled={confirmedSelected}
                      value={student.degreeType}
                      onChange={(event) =>
                        updateStudent(index, "degreeType", event.target.value)
                      }
                    >
                      <option value="">선택</option>
                      <option value="MASTER">석사</option>
                      <option value="DOCTORAL">박사</option>
                    </select>
                  </td>
                  <td className="px-3 py-2">
                    <input
                      data-testid={`degree-completion-student-name-${index}`}
                      disabled={confirmedSelected}
                      value={student.studentName}
                      onChange={(event) =>
                        updateStudent(index, "studentName", event.target.value)
                      }
                    />
                  </td>
                  <td className="px-3 py-2">
                    <input
                      data-testid={`degree-completion-thesis-title-${index}`}
                      disabled={confirmedSelected}
                      value={student.thesisTitle}
                      onChange={(event) =>
                        updateStudent(index, "thesisTitle", event.target.value)
                      }
                    />
                  </td>
                  <td className="px-3 py-2">
                    <input
                      data-testid={`degree-completion-awarded-date-${index}`}
                      disabled={confirmedSelected}
                      type="date"
                      value={student.degreeAwardedDate}
                      onChange={(event) =>
                        updateStudent(
                          index,
                          "degreeAwardedDate",
                          event.target.value,
                        )
                      }
                    />
                  </td>
                  <td className="px-3 py-2">
                    <button
                      className="text-error disabled:opacity-50"
                      data-testid={`degree-completion-remove-student-${index}`}
                      disabled={form.students.length === 1 || confirmedSelected}
                      onClick={() =>
                        setForm({
                          ...form,
                          students: form.students.filter(
                            (_, studentIndex) => studentIndex !== index,
                          ),
                        })
                      }
                      type="button"
                    >
                      <Trash2 size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {fieldErrors.degreeType ? (
            <p className="mt-2 text-xs text-error">{fieldErrors.degreeType}</p>
          ) : null}
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm
              font-semibold text-white disabled:opacity-60"
            data-testid="degree-completion-save-button"
            disabled={saving || confirmedSelected}
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

function listPath(
  managementNo: string,
  teacherName: string,
  certificationStatus: string,
  page: number,
  pageSize: number,
) {
  const query = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  if (managementNo.trim()) query.set("managementNo", managementNo.trim());
  if (teacherName.trim()) query.set("teacherName", teacherName.trim());
  if (certificationStatus.trim()) {
    query.set("certificationStatus", certificationStatus.trim());
  }
  return `/api/business/degree-completion-achievements?${query.toString()}` as `/api/${string}`;
}

function InputField({
  label,
  required = false,
  error,
  children,
}: {
  label: string;
  required?: boolean;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ml-1 text-error">*</span> : null}
      <span
        className="mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border
          [&_input]:border-ld [&_input]:px-3 [&_input]:py-2 [&_input]:text-sm
          [&_select]:w-full [&_select]:rounded-md [&_select]:border
          [&_select]:border-ld [&_select]:px-3 [&_select]:py-2 [&_select]:text-sm"
      >
        {children}
      </span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

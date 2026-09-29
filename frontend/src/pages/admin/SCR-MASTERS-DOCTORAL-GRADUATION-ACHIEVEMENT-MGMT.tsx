import { Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  degreeCompletionAchievementApi,
  type DegreeCompletionAchievement,
  type DegreeCompletionStudent,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

const blankStudent = (): DegreeCompletionStudent => ({
  degreeType: "MASTER",
  studentName: "",
  thesisTitle: "",
  degreeAwardedDate: "",
});

/** Search, detail review, and atomic header/student registration UI for degree-completion achievements. */
export function MastersDoctoralGraduationAchievementManagementPage() {
  const [managementNo, setManagementNo] = useState("");
  const [teacherName, setTeacherName] = useState("");
  const [rows, setRows] = useState<DegreeCompletionAchievement[]>([]);
  const [selected, setSelected] = useState<DegreeCompletionAchievement | null>(
    null,
  );
  const [managementItemCode, setManagementItemCode] = useState("");
  const [occurredDate, setOccurredDate] = useState("");
  const [students, setStudents] = useState<DegreeCompletionStudent[]>([
    blankStudent(),
  ]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await degreeCompletionAchievementApi.list({
        managementNo,
        teacherName,
        page: 0,
        pageSize: 20,
      });
      setRows(response.data?.degreeCompletionAchievements ?? []);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, []);

  const select = (row: DegreeCompletionAchievement) => {
    setSelected(row);
    setManagementItemCode(row.managementItemCode);
    setOccurredDate(row.occurredDate);
    setStudents(row.students.length ? row.students : [blankStudent()]);
  };
  const updateStudent = (
    index: number,
    patch: Partial<DegreeCompletionStudent>,
  ) =>
    setStudents((current) =>
      current.map((student, position) =>
        position === index ? { ...student, ...patch } : student,
      ),
    );
  const save = async () => {
    if (
      !managementItemCode.trim() ||
      students.some(
        (student) =>
          !student.studentName.trim() ||
          !student.thesisTitle.trim() ||
          !student.degreeAwardedDate,
      )
    ) {
      setError("관리항목과 모든 지도학생 필수 항목을 입력하세요.");
      return;
    }
    if (!window.confirm("석·박사 배출 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      await degreeCompletionAchievementApi.save({
        managementItemCode: managementItemCode.trim(),
        occurredDate: occurredDate || undefined,
        achievementDetail: {},
        students,
      });
      setSuccess("석·박사 배출 실적이 저장되었습니다.");
      setSelected(null);
      setManagementItemCode("");
      setOccurredDate("");
      setStudents([blankStudent()]);
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };
  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403)
      setPermissionDenied(true);
    setError(
      caught instanceof Error
        ? caught.message
        : "석·박사 배출 실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied)
    return (
      <section
        data-screen-id="SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT"
        data-testid="degree-completion-page"
      >
        <PermissionState
          title="석·박사 배출 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한이 필요합니다."
        />
      </section>
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT"
      data-testid="degree-completion-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">
          업적 입력 관리 / 교육영역 / 석·박사 배출 실적 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          석·박사 배출 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          실적 헤더와 지도학생 세부내역을 함께 저장합니다.
        </p>
      </div>
      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="석·박사 배출 실적 오류" message={error} />
      ) : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <div className="grid gap-4 md:grid-cols-3">
          <Field
            label="관리번호"
            value={managementNo}
            onChange={setManagementNo}
            testId="degree-completion-management-no-filter"
          />
          <Field
            label="성명"
            value={teacherName}
            onChange={setTeacherName}
            testId="degree-completion-teacher-filter"
          />
          <button
            type="button"
            className="btn-secondary mt-7"
            onClick={() => void load()}
            data-testid="degree-completion-search-button"
          >
            <Search size={16} />
            조회
          </button>
        </div>
      </section>
      <section className="rounded-md border border-ld bg-white p-6">
        <h2 className="text-lg font-semibold text-dark">
          석·박사 배출 실적 목록
        </h2>
        {loading ? <LoadingState title="석·박사 배출 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 실적이 없습니다"
            message="검색조건을 변경하거나 새 실적을 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>성명</th>
                  <th>관리항목</th>
                  <th>업적발생일</th>
                  <th>인증상태</th>
                  <th>지도학생 수</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    key={row.achievementId}
                    onClick={() => select(row)}
                    data-testid="degree-completion-row"
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.teacherName}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.occurredDate}</td>
                    <td>{row.certificationStatus}</td>
                    <td>{row.students.length}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
      <section className="rounded-md border border-ld bg-white p-6">
        <h2 className="text-lg font-semibold text-dark">
          {selected ? "석·박사 배출 실적 상세" : "석·박사 배출 실적 등록"}
        </h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            label="관리항목"
            value={managementItemCode}
            onChange={setManagementItemCode}
            testId="degree-completion-management-item-input"
            required
          />
          <label className="text-sm font-semibold text-dark">
            업적발생일
            <input
              type="date"
              className="mt-2 w-full border border-ld px-3 py-2 text-sm"
              value={occurredDate}
              onChange={(event) => setOccurredDate(event.target.value)}
              data-testid="degree-completion-occurred-date-input"
            />
          </label>
        </div>
        <h3 className="mt-6 text-base font-semibold text-dark">
          지도학생 세부내역
        </h3>
        <div className="mt-3 space-y-3">
          {students.map((student, index) => (
            <div
              key={index}
              className="grid gap-3 rounded border border-ld p-3 md:grid-cols-5"
              data-testid="degree-completion-student-row"
            >
              <label className="text-sm">
                학위구분
                <select
                  value={student.degreeType}
                  onChange={(event) =>
                    updateStudent(index, {
                      degreeType: event.target
                        .value as DegreeCompletionStudent["degreeType"],
                    })
                  }
                  data-testid={`degree-type-${index}`}
                >
                  <option value="MASTER">석사</option>
                  <option value="DOCTOR">박사</option>
                </select>
              </label>
              <Field
                label="학생명"
                value={student.studentName}
                onChange={(value) =>
                  updateStudent(index, { studentName: value })
                }
                testId={`student-name-${index}`}
                required
              />
              <Field
                label="논문제목"
                value={student.thesisTitle}
                onChange={(value) =>
                  updateStudent(index, { thesisTitle: value })
                }
                testId={`thesis-title-${index}`}
                required
              />
              <label className="text-sm">
                학위수여일
                <input
                  type="date"
                  value={student.degreeAwardedDate}
                  onChange={(event) =>
                    updateStudent(index, {
                      degreeAwardedDate: event.target.value,
                    })
                  }
                  data-testid={`degree-awarded-date-${index}`}
                />
              </label>
              <button
                type="button"
                className="btn-secondary self-end"
                onClick={() =>
                  setStudents((current) =>
                    current.length > 1
                      ? current.filter((_, position) => position !== index)
                      : current,
                  )
                }
                data-testid={`degree-student-remove-${index}`}
              >
                삭제
              </button>
            </div>
          ))}
        </div>
        <button
          type="button"
          className="btn-secondary mt-3"
          onClick={() => setStudents((current) => [...current, blankStudent()])}
          data-testid="degree-student-add-button"
        >
          지도학생 추가
        </button>
        <button
          type="button"
          className="btn-primary mt-5 ml-2"
          onClick={() => void save()}
          disabled={saving}
          data-testid="degree-completion-save-button"
        >
          <Save size={16} />
          {saving ? "저장 중" : "저장"}
        </button>
      </section>
    </section>
  );
}
function Field({
  label,
  value,
  onChange,
  testId,
  required,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  required?: boolean;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ms-1 text-error">*</span> : null}
      <input
        className="mt-2 w-full border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
      />
    </label>
  );
}

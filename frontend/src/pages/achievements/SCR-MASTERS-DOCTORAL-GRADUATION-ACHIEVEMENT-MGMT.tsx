import { Download, Plus, Save, Search, Trash2 } from "lucide-react";
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

type FormState = {
  achievementId?: number;
  managementItemCode: string;
  organizationCode: string;
  occurredDate: string;
  achievementDetail: string;
  attachmentRef: string;
  certificationStatus: string;
  changeReason: string;
  students: DegreeCompletionStudent[];
};

const blankStudent = (): DegreeCompletionStudent => ({
  degreeType: "MASTER",
  studentName: "",
  thesisTitle: "",
  degreeAwardedDate: "",
});
const blankForm: FormState = {
  managementItemCode: "",
  organizationCode: "",
  occurredDate: "",
  achievementDetail: "",
  attachmentRef: "",
  certificationStatus: "DRAFTING",
  changeReason: "",
  students: [blankStudent()],
};

/** 석·박사 배출 실적의 검색, 저장, 지도학생 반복 상세 입력을 제공하는 교육영역 업무 화면이다. */
export function MastersDoctoralGraduationAchievementManagementPage() {
  const [managementNo, setManagementNo] = useState("");
  const [teacherName, setTeacherName] = useState("");
  const [certificationStatus, setCertificationStatus] = useState("");
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [rows, setRows] = useState<DegreeCompletionAchievement[]>([]);
  const [form, setForm] = useState<FormState>(blankForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const confirmed = form.certificationStatus === "EVALUATION_CONFIRMED";

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403)
      setPermissionDenied(true);
    setError(
      caught instanceof Error
        ? caught.message
        : "석·박사 배출 실적을 처리하지 못했습니다.",
    );
  };
  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await degreeCompletionAchievementApi.list({
        managementNo: managementNo || undefined,
        teacherName: teacherName || undefined,
        certificationStatus: certificationStatus || undefined,
        pageSize,
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
  }, [pageSize]);
  const select = (row: DegreeCompletionAchievement) =>
    setForm({
      achievementId: row.achievementId,
      managementItemCode: row.managementItemCode,
      organizationCode: row.organizationCode,
      occurredDate: row.occurredDate,
      achievementDetail: row.achievementDetail,
      attachmentRef: row.attachmentRef ?? "",
      certificationStatus: row.certificationStatus,
      changeReason: "",
      students: row.students.length ? row.students : [blankStudent()],
    });
  const updateStudent = (
    index: number,
    field: keyof DegreeCompletionStudent,
    value: string,
  ) =>
    setForm((current) => ({
      ...current,
      students: current.students.map((student, studentIndex) =>
        studentIndex === index ? { ...student, [field]: value } : student,
      ),
    }));
  const addStudent = () =>
    setForm((current) => ({
      ...current,
      students: [...current.students, blankStudent()],
    }));
  const removeStudent = (index: number) =>
    setForm((current) => ({
      ...current,
      students:
        current.students.length === 1
          ? current.students
          : current.students.filter(
              (_, studentIndex) => studentIndex !== index,
            ),
    }));
  const save = async () => {
    if (!window.confirm("석·박사 배출 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      await degreeCompletionAchievementApi.save({
        ...form,
        attachmentRef: form.attachmentRef || undefined,
      });
      setSuccess(
        "저장 후 지도학생 세부내역을 포함해 목록을 다시 조회했습니다.",
      );
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };
  const download = () => {
    const csv = [
      "관리번호,성명,관리항목,업적발생일,인증상태,지도학생수",
      ...rows.map((row) =>
        [
          row.managementNo,
          row.teacherName,
          row.managementItemCode,
          row.occurredDate,
          row.certificationStatus,
          row.students.length,
        ].join(","),
      ),
    ].join("\n");
    const url = URL.createObjectURL(
      new Blob(["\ufeff" + csv], { type: "text/csv;charset=utf-8" }),
    );
    const link = document.createElement("a");
    link.href = url;
    link.download = "degree-completion-achievements.csv";
    link.click();
    URL.revokeObjectURL(url);
  };

  if (permissionDenied)
    return (
      <section
        data-screen-id="SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT"
        data-testid="degree-completion-page"
      >
        <PermissionState
          title="석·박사 배출 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 메뉴 접근 권한이 필요합니다."
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
        <h1 className="mt-2 text-xl font-semibold">석·박사 배출 실적 관리</h1>
      </div>
      {success ? <SuccessState title="저장 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="석·박사 배출 실적 오류" message={error} />
      ) : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <div className="grid gap-4 md:grid-cols-4">
          <TextField
            label="관리번호"
            value={managementNo}
            onChange={setManagementNo}
            testId="degree-completion-management-no-filter"
          />
          <TextField
            label="성명"
            value={teacherName}
            onChange={setTeacherName}
            testId="degree-completion-teacher-name-filter"
          />
          <label>
            인증상태
            <select
              className="form-input mt-2 w-full"
              value={certificationStatus}
              onChange={(event) => setCertificationStatus(event.target.value)}
              data-testid="degree-completion-status-filter"
            >
              <option value="">전체</option>
              <option value="DRAFTING">작성중</option>
              <option value="SUBMITTED">제출</option>
              <option value="EVALUATION_CONFIRMED">평가확정</option>
            </select>
          </label>
          <button
            className="btn-primary mt-6"
            type="button"
            onClick={() => void load()}
            data-testid="degree-completion-search-button"
          >
            <Search size={16} />
            조회
          </button>
        </div>
      </section>
      <section className="rounded-md border border-ld bg-white p-6">
        <div className="mb-4 flex flex-wrap justify-between gap-3">
          <h2 className="text-lg font-semibold">석·박사 배출 실적 목록</h2>
          <div>
            <select
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
              data-testid="degree-completion-page-size-select"
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
            <button
              className="btn-secondary ml-2"
              type="button"
              onClick={download}
              data-testid="degree-completion-excel-button"
            >
              <Download size={16} />
              Excel 다운로드
            </button>
          </div>
        </div>
        {loading ? (
          <LoadingState title="석·박사 배출 실적 조회 중" />
        ) : rows.length === 0 ? (
          <EmptyState title="조회된 석·박사 배출 실적이 없습니다" />
        ) : (
          <div className="overflow-x-auto">
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
        )}
      </section>
      <section
        className="rounded-md border border-ld bg-white p-6"
        data-testid="degree-completion-detail-panel"
      >
        <h2 className="text-lg font-semibold">상세</h2>
        {confirmed ? (
          <p
            className="mt-3 text-sm text-lighterror"
            role="status"
            data-testid="degree-completion-confirmed-lock-message"
          >
            평가확정 실적은 수정·삭제하거나 첨부파일을 삭제할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <TextField
            label="관리항목 *"
            value={form.managementItemCode}
            onChange={(value) =>
              setForm({ ...form, managementItemCode: value })
            }
            testId="degree-completion-management-item-input"
          />
          <TextField
            label="소속 조직 *"
            value={form.organizationCode}
            onChange={(value) => setForm({ ...form, organizationCode: value })}
            testId="degree-completion-organization-input"
          />
          <label>
            업적발생일 *
            <input
              type="date"
              className="form-input mt-2 w-full"
              value={form.occurredDate}
              onChange={(event) =>
                setForm({ ...form, occurredDate: event.target.value })
              }
              data-testid="degree-completion-occurred-date-input"
            />
          </label>
          <TextField
            label="첨부파일 식별자"
            value={form.attachmentRef}
            onChange={(value) => setForm({ ...form, attachmentRef: value })}
            testId="degree-completion-attachment-input"
          />
          <label className="md:col-span-2">
            실적 내용
            <textarea
              className="form-input mt-2 w-full"
              value={form.achievementDetail}
              onChange={(event) =>
                setForm({ ...form, achievementDetail: event.target.value })
              }
              data-testid="degree-completion-detail-input"
            />
          </label>
        </div>
        <div className="mt-6" data-testid="degree-completion-students-table">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="font-semibold">지도학생 세부내역</h3>
            <button
              className="btn-secondary"
              type="button"
              onClick={addStudent}
              data-testid="degree-completion-add-student-button"
            >
              <Plus size={16} />
              지도학생 추가
            </button>
          </div>
          <div className="overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th>학위구분 *</th>
                  <th>학생명 *</th>
                  <th>논문 제목 *</th>
                  <th>학위 수여일 *</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {form.students.map((student, index) => (
                  <tr
                    key={index}
                    data-testid={`degree-completion-student-row-${index}`}
                  >
                    <td>
                      <select
                        className="form-input"
                        value={student.degreeType}
                        onChange={(event) =>
                          updateStudent(index, "degreeType", event.target.value)
                        }
                        data-testid={`degree-completion-student-degree-type-${index}`}
                      >
                        <option value="MASTER">석사</option>
                        <option value="DOCTORAL">박사</option>
                      </select>
                    </td>
                    <td>
                      <input
                        className="form-input"
                        value={student.studentName}
                        onChange={(event) =>
                          updateStudent(
                            index,
                            "studentName",
                            event.target.value,
                          )
                        }
                        data-testid={`degree-completion-student-name-${index}`}
                      />
                    </td>
                    <td>
                      <input
                        className="form-input"
                        value={student.thesisTitle}
                        onChange={(event) =>
                          updateStudent(
                            index,
                            "thesisTitle",
                            event.target.value,
                          )
                        }
                        data-testid={`degree-completion-student-thesis-title-${index}`}
                      />
                    </td>
                    <td>
                      <input
                        type="date"
                        className="form-input"
                        value={student.degreeAwardedDate}
                        onChange={(event) =>
                          updateStudent(
                            index,
                            "degreeAwardedDate",
                            event.target.value,
                          )
                        }
                        data-testid={`degree-completion-student-awarded-date-${index}`}
                      />
                    </td>
                    <td>
                      <button
                        className="btn-secondary"
                        type="button"
                        disabled={form.students.length === 1}
                        onClick={() => removeStudent(index)}
                        data-testid={`degree-completion-remove-student-${index}`}
                      >
                        <Trash2 size={16} />
                        삭제
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        <button
          className="btn-primary mt-4"
          disabled={saving || confirmed}
          type="button"
          onClick={() => void save()}
          data-testid="degree-completion-save-button"
        >
          <Save size={16} />
          {saving ? "저장 중" : "저장"}
        </button>
      </section>
    </section>
  );
}

function TextField({
  label,
  value,
  onChange,
  testId,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
}) {
  return (
    <label>
      {label}
      <input
        className="form-input mt-2 w-full"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
      />
    </label>
  );
}

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
} from "../../components/States";

const emptyStudent = (): DegreeCompletionStudent => ({
  degreeType: "MASTER",
  studentName: "",
  thesisTitle: "",
  degreeAwardedDate: "",
});
const emptyForm = () => ({
  evaluationYear: "",
  organizationCode: "",
  managementItemCode: "",
  occurredDate: "",
  attachmentReference: "",
  changeReason: "",
  students: [emptyStudent()],
});

/** Evaluation-confirmed achievements are immutable on both the UI and server mutation boundaries. */
export function canEditDegreeCompletionAchievement(
  achievement: DegreeCompletionAchievement | null,
) {
  return achievement?.certificationStatus !== "EVALUATION_CONFIRMED";
}

/** 석·박사 배출 실적과 지도학생 sub-table을 함께 저장·재조회하는 FR-028 화면이다. */
export function MastersDoctoralGraduationAchievementManagementPage() {
  const [rows, setRows] = useState<DegreeCompletionAchievement[]>([]);
  const [form, setForm] = useState(emptyForm());
  const [selected, setSelected] = useState<DegreeCompletionAchievement | null>(
    null,
  );
  const [size, setSize] = useState<20 | 50 | 100>(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [permission, setPermission] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermission(false);
      const result = await degreeCompletionAchievementApi.list({
        size,
        evaluationYear: form.evaluationYear,
        organizationCode: form.organizationCode,
        managementItemCode: form.managementItemCode,
      });
      setRows(result.data?.achievements ?? []);
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 403)
        setPermission(true);
      else
        setError(
          "석·박사 배출 실적을 불러오지 못했습니다. 검색 조건을 확인한 뒤 다시 시도하세요.",
        );
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [size]);
  const updateStudent = (
    index: number,
    key: keyof DegreeCompletionStudent,
    value: string,
  ) =>
    setForm((current) => ({
      ...current,
      students: current.students.map((student, rowIndex) =>
        rowIndex === index
          ? ({ ...student, [key]: value } as DegreeCompletionStudent)
          : student,
      ),
    }));
  const select = (row: DegreeCompletionAchievement) => {
    setSelected(row);
    setForm({
      evaluationYear: row.evaluationYear,
      organizationCode: row.organizationCode,
      managementItemCode: row.managementItemCode,
      occurredDate: row.occurredDate,
      attachmentReference: row.attachmentReference ?? "",
      changeReason: "석·박사 배출 실적 수정",
      students: row.students.length ? row.students : [emptyStudent()],
    });
  };
  const save = async () => {
    if (!canEditDegreeCompletionAchievement(selected)) {
      setError("평가확정된 실적은 수정할 수 없습니다.");
      return;
    }
    if (
      !form.evaluationYear ||
      !form.organizationCode ||
      !form.managementItemCode ||
      !form.occurredDate ||
      !form.changeReason ||
      form.students.some(
        (student) =>
          !student.degreeType ||
          !student.studentName ||
          !student.thesisTitle ||
          !student.degreeAwardedDate,
      )
    ) {
      setError("필수 입력 항목과 지도학생 상세를 모두 입력하세요.");
      return;
    }
    if (!window.confirm("석·박사 배출 실적을 저장하시겠습니까?")) return;
    try {
      const result = await degreeCompletionAchievementApi.save({
        ...form,
        achievementId: selected?.achievementId,
        achievementDetail: {},
      });
      setMessage(
        `저장되었습니다. 지도학생 ${result.data?.students.length ?? 0}건을 재조회했습니다.`,
      );
      await load();
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "저장에 실패했습니다.",
      );
    }
  };
  if (permission)
    return (
      <section data-testid="masters-doctoral-graduation-achievement-page">
        <PermissionState
          title="석·박사 배출 실적 접근 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 데이터 범위가 필요합니다."
        />
      </section>
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT"
      data-testid="masters-doctoral-graduation-achievement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <h1 className="text-xl font-semibold text-dark">
          석·박사 배출 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          교육영역 실적과 지도학생의 학위·논문·수여일을 함께 관리합니다.
        </p>
      </header>
      {error ? (
        <ErrorState title="석·박사 배출 실적 오류" message={error} />
      ) : null}
      {message ? (
        <p
          role="status"
          className="rounded-md bg-lightsuccess p-4 text-sm text-success"
        >
          {message}
        </p>
      ) : null}
      <section className="grid gap-3 rounded-md border border-ld bg-white p-5 md:grid-cols-5">
        <label>
          평가연도 *
          <input
            data-testid="degree-completion-year-input"
            value={form.evaluationYear}
            onChange={(event) =>
              setForm({ ...form, evaluationYear: event.target.value })
            }
          />
        </label>
        <label>
          소속 조직 *
          <input
            data-testid="degree-completion-organization-input"
            value={form.organizationCode}
            onChange={(event) =>
              setForm({ ...form, organizationCode: event.target.value })
            }
          />
        </label>
        <label>
          관리항목 *
          <input
            data-testid="degree-completion-item-input"
            value={form.managementItemCode}
            onChange={(event) =>
              setForm({ ...form, managementItemCode: event.target.value })
            }
          />
        </label>
        <label>
          업적발생일 *
          <input
            data-testid="degree-completion-date-input"
            type="date"
            value={form.occurredDate}
            onChange={(event) =>
              setForm({ ...form, occurredDate: event.target.value })
            }
          />
        </label>
        <button
          data-testid="degree-completion-search-button"
          type="button"
          onClick={() => void load()}
        >
          조회
        </button>
      </section>
      <section className="rounded-md border border-ld bg-white p-5">
        <h2>석·박사 배출 목록</h2>
        <select
          data-testid="degree-completion-page-size-select"
          value={size}
          onChange={(event) =>
            setSize(Number(event.target.value) as 20 | 50 | 100)
          }
        >
          <option value={20}>20건</option>
          <option value={50}>50건</option>
          <option value={100}>100건</option>
        </select>
        {loading ? (
          <LoadingState
            title="석·박사 배출 실적 조회 중"
            message="목록을 불러오고 있습니다."
          />
        ) : rows.length === 0 ? (
          <EmptyState
            title="조회된 석·박사 배출 실적이 없습니다"
            message="검색 조건을 변경하거나 새 실적을 등록하세요."
          />
        ) : (
          <table>
            <thead>
              <tr>
                <th>관리항목</th>
                <th>발생일</th>
                <th>상태</th>
                <th>학생수</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr
                  key={row.achievementId}
                  data-testid={`degree-completion-row-${row.achievementId}`}
                  onClick={() => select(row)}
                >
                  <td>{row.managementItemCode}</td>
                  <td>{row.occurredDate}</td>
                  <td>{row.certificationStatus}</td>
                  <td>{row.students.length}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="degree-completion-detail-panel"
      >
        <h2>상세 입력</h2>
        <label>
          첨부 참조
          <input
            data-testid="degree-completion-attachment-reference-input"
            value={form.attachmentReference}
            onChange={(event) =>
              setForm({ ...form, attachmentReference: event.target.value })
            }
          />
        </label>
        <h3>지도학생 상세</h3>
        <table data-testid="degree-completion-student-table">
          <thead>
            <tr>
              <th>학위구분</th>
              <th>학생명</th>
              <th>논문제목</th>
              <th>수여일</th>
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
                    data-testid={`degree-completion-degree-type-${index}`}
                    value={student.degreeType}
                    onChange={(event) =>
                      updateStudent(index, "degreeType", event.target.value)
                    }
                  >
                    <option value="MASTER">석사</option>
                    <option value="DOCTORAL">박사</option>
                  </select>
                </td>
                <td>
                  <input
                    data-testid={`degree-completion-student-name-${index}`}
                    value={student.studentName}
                    onChange={(event) =>
                      updateStudent(index, "studentName", event.target.value)
                    }
                  />
                </td>
                <td>
                  <input
                    data-testid={`degree-completion-thesis-title-${index}`}
                    value={student.thesisTitle}
                    onChange={(event) =>
                      updateStudent(index, "thesisTitle", event.target.value)
                    }
                  />
                </td>
                <td>
                  <input
                    data-testid={`degree-completion-awarded-date-${index}`}
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
                <td>
                  <button
                    type="button"
                    data-testid={`degree-completion-student-remove-${index}`}
                    disabled={form.students.length === 1}
                    onClick={() =>
                      setForm({
                        ...form,
                        students: form.students.filter(
                          (_, rowIndex) => rowIndex !== index,
                        ),
                      })
                    }
                  >
                    삭제
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <button
          type="button"
          data-testid="degree-completion-student-add-button"
          onClick={() =>
            setForm({ ...form, students: [...form.students, emptyStudent()] })
          }
        >
          지도학생 추가
        </button>
        <label>
          변경 사유 *
          <input
            data-testid="degree-completion-change-reason-input"
            value={form.changeReason}
            onChange={(event) =>
              setForm({ ...form, changeReason: event.target.value })
            }
          />
        </label>
        <button
          data-testid="degree-completion-save-button"
          type="button"
          onClick={() => void save()}
        >
          {selected ? "수정 저장" : "저장"}
        </button>
      </section>
    </section>
  );
}

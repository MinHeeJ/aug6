import { useEffect, useState } from "react";
import { Plus, Save, Search, Trash2 } from "lucide-react";
import { useAuth } from "../app/AuthProvider";
import { ApiClientError, apiRequest } from "../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../components/States";

type DegreeStudent = {
  degreeType: "MASTER" | "DOCTOR";
  studentName: string;
  thesisTitle: string;
  degreeAwardedDate: string;
};

type DegreeAchievement = {
  managementNo: string;
  evaluationYear: string;
  managementItemCode: string;
  occurredDate: string;
  certificationStatus: string;
  attachmentAvailable: boolean;
  students: DegreeStudent[];
};

type DegreeAchievementList = {
  items: DegreeAchievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type ScreenStatus = "idle" | "loading" | "error" | "success";

const initialStudent: DegreeStudent = {
  degreeType: "MASTER",
  studentName: "",
  thesisTitle: "",
  degreeAwardedDate: "",
};

function messageFor(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.status === 403) return "석·박사 배출 실적 관리 권한이 없습니다.";
    if (error.status === 409) return "평가확정 실적은 수정할 수 없습니다.";
    return (
      error.apiError?.fields.map((field) => field.message).join(" ") ||
      error.message
    );
  }
  return error instanceof Error
    ? error.message
    : "처리 중 오류가 발생했습니다.";
}

/**
 * R01/R02/R04가 석·박사 배출 실적과 지도학생 반복 상세를 저장하고 재조회하는 화면이다.
 * 학생 상세는 API 응답을 유일한 원천으로 사용하며, 저장 이후 목록을 다시 불러온다.
 */
export function DegreeCompletionAchievementPage() {
  const auth = useAuth();
  const [status, setStatus] = useState<ScreenStatus>("loading");
  const [message, setMessage] = useState("");
  const [rows, setRows] = useState<DegreeAchievement[]>([]);
  const [managementNoFilter, setManagementNoFilter] = useState("");
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [managementNo, setManagementNo] = useState("");
  const [evaluationYear, setEvaluationYear] = useState("2026");
  const [managementItemCode, setManagementItemCode] = useState(
    "EDU_DEGREE_COMPLETION",
  );
  const [occurredDate, setOccurredDate] = useState("");
  const [students, setStudents] = useState<DegreeStudent[]>([
    { ...initialStudent },
  ]);

  const hasRole =
    auth.user?.roles.some((role) => ["R01", "R02", "R04"].includes(role)) ??
    false;

  const load = async (filter = managementNoFilter) => {
    if (!hasRole) return;
    setStatus("loading");
    try {
      const query = new URLSearchParams({
        page: "0",
        pageSize: String(pageSize),
      });
      if (filter.trim()) query.set("managementNo", filter.trim());
      const response = await apiRequest<DegreeAchievementList>(
        `/api/business/degree-completion-achievements?${query.toString()}` as `/api/${string}`,
      );
      setRows(response.data?.items ?? []);
      setStatus("idle");
    } catch (error) {
      setStatus("error");
      setMessage(messageFor(error));
    }
  };

  useEffect(() => {
    void load();
    // The reload must follow the selected pagination size, not a stale query value.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageSize, hasRole]);

  const updateStudent = (
    index: number,
    field: keyof DegreeStudent,
    value: string,
  ) => {
    setStudents((current) =>
      current.map((student, studentIndex) =>
        studentIndex === index ? { ...student, [field]: value } : student,
      ),
    );
  };

  const save = async () => {
    if (
      !managementNo.trim() ||
      !managementItemCode.trim() ||
      !occurredDate ||
      students.some(
        (student) =>
          !student.studentName.trim() ||
          !student.thesisTitle.trim() ||
          !student.degreeAwardedDate,
      )
    ) {
      setStatus("error");
      setMessage(
        "관리번호, 관리항목, 업적발생일과 모든 지도학생 상세는 필수입니다.",
      );
      return;
    }
    if (
      !window.confirm("석·박사 배출 실적과 지도학생 상세를 저장하시겠습니까?")
    )
      return;
    setStatus("loading");
    try {
      const response = await apiRequest<DegreeAchievement>(
        "/api/business/degree-completion-achievements",
        {
          method: "POST",
          body: JSON.stringify({
            managementNo,
            evaluationYear,
            managementItemCode,
            occurredDate,
            students,
          }),
        },
      );
      const saved = response.data;
      if (saved) {
        setManagementNo(saved.managementNo);
        setStudents(saved.students);
      }
      await load(managementNo);
      setStatus("success");
      setMessage(
        "석·박사 배출 실적이 저장되었고 지도학생 상세를 다시 조회했습니다.",
      );
    } catch (error) {
      setStatus("error");
      setMessage(messageFor(error));
    }
  };

  if (!hasRole) {
    return (
      <PermissionState
        title="권한이 없습니다"
        message="석·박사 배출 실적 관리는 R01, R02, R04 역할만 사용할 수 있습니다."
      />
    );
  }

  return (
    <main
      className="space-y-6"
      data-testid="degree-completion-achievement-screen"
    >
      <section className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm font-semibold text-primary">
          업적 입력 관리 &gt; 교육영역 &gt; 석·박사 배출 실적 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          석·박사 배출 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          실적 헤더와 지도학생 세부내역을 함께 저장합니다.
        </p>
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_auto_auto]">
          <label className="grid gap-1 text-sm font-medium text-link">
            관리번호 검색
            <input
              data-testid="degree-completion-search-input"
              className="form-input"
              value={managementNoFilter}
              onChange={(event) => setManagementNoFilter(event.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm font-medium text-link">
            표시 건수
            <select
              data-testid="degree-completion-page-size-select"
              className="form-select"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
            >
              <option value="20">20</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </select>
          </label>
          <button
            data-testid="degree-completion-search-button"
            className="btn-secondary self-end"
            type="button"
            onClick={() => void load()}
          >
            <Search size={16} /> 조회
          </button>
        </div>
        <div className="mt-4 overflow-x-auto">
          <table className="table">
            <thead>
              <tr>
                <th>관리번호</th>
                <th>관리항목</th>
                <th>업적발생일</th>
                <th>인증상태</th>
                <th>첨부</th>
                <th>지도학생</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr
                  key={row.managementNo}
                  data-testid="degree-completion-achievement-row"
                >
                  <td>{row.managementNo}</td>
                  <td>{row.managementItemCode}</td>
                  <td>{row.occurredDate}</td>
                  <td>{row.certificationStatus}</td>
                  <td>{row.attachmentAvailable ? "있음" : "없음"}</td>
                  <td>{row.students.length}명</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {status === "idle" && rows.length === 0 ? (
          <EmptyState
            title="조회 결과가 없습니다"
            message="검색 조건을 변경하거나 새 실적을 등록하세요."
          />
        ) : null}
      </section>

      <section
        className="rounded-md bg-white p-6 shadow-md"
        data-testid="degree-completion-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">실적 상세</h2>
        <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <label className="grid gap-1 text-sm font-medium text-link">
            관리번호 <span className="text-error">*</span>
            <input
              data-testid="degree-completion-management-no-input"
              className="form-input"
              value={managementNo}
              onChange={(event) => setManagementNo(event.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm font-medium text-link">
            평가연도 <span className="text-error">*</span>
            <input
              data-testid="degree-completion-evaluation-year-input"
              className="form-input"
              value={evaluationYear}
              onChange={(event) => setEvaluationYear(event.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm font-medium text-link">
            관리항목 <span className="text-error">*</span>
            <input
              data-testid="degree-completion-management-item-input"
              className="form-input"
              value={managementItemCode}
              onChange={(event) => setManagementItemCode(event.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm font-medium text-link">
            업적발생일 <span className="text-error">*</span>
            <input
              data-testid="degree-completion-occurred-date-input"
              className="form-input"
              type="date"
              value={occurredDate}
              onChange={(event) => setOccurredDate(event.target.value)}
            />
          </label>
        </div>
        <div className="mt-6 flex items-center justify-between gap-3">
          <h3 className="text-base font-semibold text-dark">
            지도학생 세부내역
          </h3>
          <button
            data-testid="degree-completion-add-student-button"
            className="btn-secondary"
            type="button"
            onClick={() =>
              setStudents((current) => [...current, { ...initialStudent }])
            }
          >
            <Plus size={16} /> 학생 추가
          </button>
        </div>
        <div className="mt-3 overflow-x-auto">
          <table
            className="table"
            data-testid="degree-completion-student-table"
          >
            <thead>
              <tr>
                <th>학위구분</th>
                <th>학생명</th>
                <th>논문제목</th>
                <th>학위수여일</th>
                <th>삭제</th>
              </tr>
            </thead>
            <tbody>
              {students.map((student, index) => (
                <tr
                  key={`${index}-${student.studentName}`}
                  data-testid="degree-completion-student-row"
                >
                  <td>
                    <select
                      data-testid={`degree-completion-degree-type-${index}`}
                      className="form-select"
                      value={student.degreeType}
                      onChange={(event) =>
                        updateStudent(index, "degreeType", event.target.value)
                      }
                    >
                      <option value="MASTER">석사</option>
                      <option value="DOCTOR">박사</option>
                    </select>
                  </td>
                  <td>
                    <input
                      data-testid={`degree-completion-student-name-${index}`}
                      className="form-input"
                      value={student.studentName}
                      onChange={(event) =>
                        updateStudent(index, "studentName", event.target.value)
                      }
                    />
                  </td>
                  <td>
                    <input
                      data-testid={`degree-completion-thesis-title-${index}`}
                      className="form-input"
                      value={student.thesisTitle}
                      onChange={(event) =>
                        updateStudent(index, "thesisTitle", event.target.value)
                      }
                    />
                  </td>
                  <td>
                    <input
                      data-testid={`degree-completion-awarded-date-${index}`}
                      className="form-input"
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
                      data-testid={`degree-completion-remove-student-${index}`}
                      className="btn-secondary"
                      type="button"
                      disabled={students.length === 1}
                      onClick={() =>
                        setStudents((current) =>
                          current.filter(
                            (_, studentIndex) => studentIndex !== index,
                          ),
                        )
                      }
                    >
                      <Trash2 size={16} />
                      <span className="sr-only">학생 삭제</span>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mt-6 flex justify-end">
          <button
            data-testid="degree-completion-save-button"
            className="btn-primary"
            type="button"
            onClick={() => void save()}
          >
            <Save size={16} /> 저장
          </button>
        </div>
      </section>
      {status === "loading" ? (
        <LoadingState
          title="처리 중"
          message="석·박사 배출 실적을 조회하거나 저장하고 있습니다."
        />
      ) : null}
      {status === "error" ? (
        <ErrorState title="처리 오류" message={message} />
      ) : null}
      {status === "success" ? (
        <SuccessState title="저장 완료" message={message} />
      ) : null}
    </main>
  );
}

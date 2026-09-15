import { useEffect, useState } from "react";
import {
  studentGuidanceAchievementApi,
  type SaveStudentGuidanceAchievement,
  type StudentGuidanceAchievement,
} from "../../api/achievementApi";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  SuccessState,
} from "../../components/States";

const initial: SaveStudentGuidanceAchievement = {
  evaluationYear: "",
  academicYear: "",
  semester: "",
  studentNo: "",
  studentName: "",
  guidanceType: "ADVISORY",
  guidanceDate: "",
  guidanceContent: "",
  dynamicFields: {},
  attachmentRefs: [],
  changeReason: "학생지도 실적 입력",
};
export function StudentGuidanceAchievementPage() {
  const [rows, setRows] = useState<StudentGuidanceAchievement[]>([]);
  const [form, setForm] = useState(initial);
  const [keyword, setKeyword] = useState("");
  const [size, setSize] = useState<20 | 50 | 100>(20);
  const [file, setFile] = useState<File | null>(null);
  const [state, setState] = useState<
    "loading" | "ready" | "empty" | "error" | "success"
  >("loading");
  const [message, setMessage] = useState("");
  const load = async () => {
    setState("loading");
    try {
      const response = await studentGuidanceAchievementApi.list({
        studentKeyword: keyword,
        size,
      });
      const values = response.data?.studentGuidanceAchievements ?? [];
      setRows(values);
      setState(values.length ? "ready" : "empty");
    } catch {
      setMessage("학생지도 실적을 조회하지 못했습니다. 검색조건을 확인하세요.");
      setState("error");
    }
  };
  useEffect(() => {
    void load();
  }, [size]);
  const save = async () => {
    if (
      !form.evaluationYear ||
      !form.academicYear ||
      !form.semester ||
      !form.studentNo ||
      !form.studentName ||
      !form.guidanceDate ||
      !form.guidanceContent
    ) {
      setMessage("필수 입력 항목을 확인하세요.");
      setState("error");
      return;
    }
    if (!window.confirm("학생지도 실적을 저장하시겠습니까?")) return;
    try {
      await studentGuidanceAchievementApi.save(form);
      setMessage("학생지도 실적이 저장되었습니다.");
      setState("success");
      await load();
    } catch {
      setMessage("저장에 실패했습니다. 입력값을 확인하세요.");
      setState("error");
    }
  };
  const upload = async () => {
    if (!file) {
      setMessage("Excel 파일을 선택하세요.");
      setState("error");
      return;
    }
    if (!window.confirm("파일 검증과 전체 반영을 실행하시겠습니까?")) return;
    try {
      const result = await studentGuidanceAchievementApi.upload(file);
      const data = result.data;
      setMessage(
        data?.errorCount
          ? `오류 ${data.errorCount}건으로 전체 반영이 차단되었습니다.`
          : `${data?.savedCount ?? 0}건이 반영되었습니다.`,
      );
      setState(data?.errorCount ? "error" : "success");
      await load();
    } catch {
      setMessage("일괄등록에 실패했습니다. 양식과 오류 결과를 확인하세요.");
      setState("error");
    }
  };
  return (
    <main
      className="space-y-6"
      data-screen-id="SCR-STUDENT-GUIDANCE-ACHIEVEMENT"
      data-testid="student-guidance-page"
    >
      <section className="rounded-md bg-white p-6 shadow-md">
        <h1 className="text-xl font-semibold text-dark">학생지도 실적 관리</h1>
        <div className="mt-4 flex flex-wrap gap-3">
          <input
            data-testid="student-guidance-search-input"
            className="form-input"
            placeholder="학번 또는 학생명"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
          <select
            data-testid="student-guidance-page-size-select"
            className="form-input"
            value={size}
            onChange={(e) => setSize(Number(e.target.value) as 20 | 50 | 100)}
          >
            {[20, 50, 100].map((v) => (
              <option key={v}>{v}</option>
            ))}
          </select>
          <button
            data-testid="student-guidance-search-button"
            className="btn-secondary"
            onClick={() => void load()}
          >
            조회
          </button>
        </div>
        <div className="mt-5 grid gap-3 md:grid-cols-3">
          {(
            [
              "evaluationYear",
              "academicYear",
              "semester",
              "studentNo",
              "studentName",
              "guidanceDate",
            ] as const
          ).map((key) => (
            <input
              key={key}
              data-testid={`student-guidance-${key}-input`}
              className="form-input"
              type={key === "guidanceDate" ? "date" : "text"}
              placeholder={`${key} *`}
              value={form[key]}
              onChange={(e) => setForm({ ...form, [key]: e.target.value })}
            />
          ))}
          <select
            data-testid="student-guidance-type-select"
            className="form-input"
            value={form.guidanceType}
            onChange={(e) => setForm({ ...form, guidanceType: e.target.value })}
          >
            <option value="ADVISORY">상담지도</option>
            <option value="CAREER">진로지도</option>
            <option value="ACADEMIC">학업지도</option>
            <option value="OTHER">기타</option>
          </select>
          <textarea
            data-testid="student-guidance-content-input"
            className="form-input"
            placeholder="지도내용 *"
            value={form.guidanceContent}
            onChange={(e) =>
              setForm({ ...form, guidanceContent: e.target.value })
            }
          />
          <button
            data-testid="student-guidance-save-button"
            className="btn-primary"
            onClick={() => void save()}
          >
            저장
          </button>
        </div>
      </section>
      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold">Excel 일괄등록</h2>
        <p className="mt-2 text-sm text-muted">
          양식 다운로드 후 파일 업로드, 검증 결과 확인 및 전체 원자 반영을
          진행합니다.
        </p>
        <div className="mt-3 flex flex-wrap gap-3">
          <a
            data-testid="student-guidance-template-download"
            className="btn-secondary"
            href="/api/admin/excel-upload-templates/STUDENT-GUIDANCE-V1/file"
          >
            양식 다운로드
          </a>
          <input
            data-testid="student-guidance-upload-input"
            type="file"
            accept=".csv"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
          <button
            data-testid="student-guidance-upload-button"
            className="btn-primary"
            onClick={() => void upload()}
          >
            검증 및 반영
          </button>
        </div>
      </section>
      {state === "loading" && <LoadingState title="조회 중" />}
      {state === "empty" && <EmptyState title="학생지도 실적 없음" />}
      {state === "error" && <ErrorState title="처리 오류" message={message} />}
      {state === "success" && (
        <SuccessState title="처리 완료" message={message} />
      )}
      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold">학생지도 실적 목록</h2>
        <table className="table mt-4">
          <thead>
            <tr>
              <th>학생</th>
              <th>지도유형</th>
              <th>지도일자</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr data-testid="student-guidance-row" key={row.achievementId}>
                <td>
                  {row.studentNo} {row.studentName}
                </td>
                <td>{row.guidanceType}</td>
                <td>{row.guidanceDate}</td>
                <td>{row.achievementStatus}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </main>
  );
}

import { Save } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiClientError } from "../../api/apiClient";
import {
  educationAchievementApi,
  type EducationAchievement,
} from "../../api/educationAchievementApi";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

/** Renders individual student-guidance registration and the reachable Excel-registration entry point. */
export function StudentGuidanceAchievementPage() {
  const [items, setItems] = useState<EducationAchievement[]>([]);
  const [managementItemCode, setManagementItemCode] = useState("");
  const [occurrenceDate, setOccurrenceDate] = useState("");
  const [studentName, setStudentName] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [studentCount, setStudentCount] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const load = async () => {
    try {
      setLoading(true);
      const response = await educationAchievementApi.list({
        achievementType: "STUDENT_GUIDANCE",
      });
      setItems(response.data?.items ?? []);
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "학생지도 실적을 조회하지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, []);
  const save = async () => {
    if (
      !managementItemCode ||
      !occurrenceDate ||
      !studentName ||
      !startDate ||
      !endDate ||
      studentCount < 1
    ) {
      setError("필수 항목을 모두 입력하세요.");
      return;
    }
    if (!window.confirm("학생지도 실적을 저장하시겠습니까?")) return;
    try {
      await educationAchievementApi.save({
        achievementType: "STUDENT_GUIDANCE",
        managementItemCode,
        occurrenceDate,
        studentGuidanceDetails: [
          {
            guidanceStudentName: studentName,
            guidanceStartDate: startDate,
            guidanceEndDate: endDate,
            studentCount,
          },
        ],
      });
      setSuccess("저장되었습니다. 목록을 재조회했습니다.");
      await load();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "학생지도 실적을 저장하지 못했습니다.",
      );
    }
  };
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-STUDENT-GUIDANCE-ACHIEVEMENT"
      data-testid="student-guidance-achievement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          학생지도 실적 관리
        </h1>
      </header>
      {success ? <SuccessState title="저장 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="학생지도 실적 처리 오류" message={error} />
      ) : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <h2 className="text-lg font-semibold text-dark">개별 등록</h2>
        <div className="mt-4 grid gap-3 md:grid-cols-3">
          <input
            className="form-input"
            data-testid="student-guidance-management-item-input"
            placeholder="관리항목"
            value={managementItemCode}
            onChange={(e) => setManagementItemCode(e.target.value)}
          />
          <input
            className="form-input"
            data-testid="student-guidance-occurrence-date-input"
            type="date"
            value={occurrenceDate}
            onChange={(e) => setOccurrenceDate(e.target.value)}
          />
          <input
            className="form-input"
            data-testid="student-guidance-student-name-input"
            placeholder="지도학생"
            value={studentName}
            onChange={(e) => setStudentName(e.target.value)}
          />
          <input
            className="form-input"
            data-testid="student-guidance-start-date-input"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />
          <input
            className="form-input"
            data-testid="student-guidance-end-date-input"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
          <input
            className="form-input"
            data-testid="student-guidance-count-input"
            type="number"
            min="1"
            value={studentCount}
            onChange={(e) => setStudentCount(Number(e.target.value))}
          />
          <button
            type="button"
            className="btn-primary"
            onClick={() => void save()}
            data-testid="student-guidance-save-button"
          >
            <Save size={16} /> 저장
          </button>
        </div>
      </section>
      <section className="rounded-md border border-ld bg-white p-6">
        <h2 className="text-lg font-semibold text-dark">Excel 일괄등록</h2>
        <p className="mt-2 text-sm text-muted">
          R07 권한으로 표준 양식을 내려받아 업로드·검증한 뒤 오류가 없을 때만
          반영합니다.
        </p>
        <a
          className="btn-secondary mt-4 inline-flex"
          href="/api/admin/excel-upload-templates/STUDENT_GUIDANCE_ACHIEVEMENT/file"
          data-testid="student-guidance-excel-template-link"
        >
          템플릿 다운로드
        </a>
      </section>
      <section className="rounded-md border border-ld bg-white p-6">
        <h2 className="text-lg font-semibold text-dark">학생지도 실적 목록</h2>
        {loading ? (
          <LoadingState title="학생지도 실적 조회 중" />
        ) : items.length === 0 ? (
          <EmptyState title="조회된 학생지도 실적이 없습니다" />
        ) : (
          <table className="mt-4">
            <tbody>
              {items.map((item) => (
                <tr key={item.achievementId} data-testid="student-guidance-row">
                  <td>{item.managementItemCode}</td>
                  <td>
                    {item.studentGuidanceDetails
                      ?.map((detail) => detail.guidanceStudentName)
                      .join(", ")}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </section>
  );
}

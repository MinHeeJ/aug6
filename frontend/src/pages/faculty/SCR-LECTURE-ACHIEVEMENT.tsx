import { RefreshCw, Save } from "lucide-react";
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

const statusLabel = (status: string) =>
  ({
    DRAFTING: "작성중",
    SUBMITTED: "제출",
    DEPARTMENT_CONFIRMED: "학과장확인",
    DEPARTMENT_REJECTED: "학과장미승인",
    CERTIFIED: "인증",
    CERTIFICATION_RETURNED: "인증반려",
    EVALUATION_CONFIRMED: "평가확정",
  })[status] ?? status;

/** Renders the lecture-achievement list and registration workflow using the shared education API. */
export function LectureAchievementPage() {
  const [items, setItems] = useState<EducationAchievement[]>([]);
  const [managementItemCode, setManagementItemCode] = useState("");
  const [occurrenceDate, setOccurrenceDate] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await educationAchievementApi.list({
        achievementType: "LECTURE_ACHIEVEMENT",
      });
      setItems(response.data?.items ?? []);
    } catch (caught) {
      handleError(caught);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, []);

  const save = async () => {
    if (!managementItemCode.trim() || !occurrenceDate) {
      setError("관리항목과 발생일을 입력하세요.");
      return;
    }
    if (!window.confirm("강의실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      await educationAchievementApi.save({
        achievementType: "LECTURE_ACHIEVEMENT",
        managementItemCode: managementItemCode.trim(),
        occurrenceDate,
      });
      setManagementItemCode("");
      setOccurrenceDate("");
      setSuccess("저장되었습니다. 목록을 재조회했습니다.");
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
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "강의실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied)
    return (
      <section
        data-screen-id="SCR-LECTURE-ACHIEVEMENT"
        data-testid="lecture-achievement-page"
      >
        <PermissionState
          title="강의실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-ACHIEVEMENT"
      data-testid="lecture-achievement-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              강의실적을 등록하고 인증상태를 확인합니다.
            </p>
          </div>
          <button
            type="button"
            className="btn-secondary"
            onClick={() => void load()}
            data-testid="lecture-achievement-refresh-button"
          >
            <RefreshCw size={16} /> 재조회
          </button>
        </div>
      </div>
      {success ? <SuccessState title="저장 완료" message={success} /> : null}
      {error ? <ErrorState title="강의실적 처리 오류" message={error} /> : null}
      <section className="rounded-md border border-ld bg-white p-6">
        <h2 className="text-lg font-semibold text-dark">강의실적 상세 입력</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <label className="text-sm font-semibold text-dark">
            관리항목<span className="ml-1 text-error">*</span>
            <input
              className="form-input mt-2 w-full"
              data-testid="lecture-achievement-management-item-input"
              value={managementItemCode}
              onChange={(event) => setManagementItemCode(event.target.value)}
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            발생일<span className="ml-1 text-error">*</span>
            <input
              type="date"
              className="form-input mt-2 w-full"
              data-testid="lecture-achievement-occurrence-date-input"
              value={occurrenceDate}
              onChange={(event) => setOccurrenceDate(event.target.value)}
            />
          </label>
          <button
            type="button"
            disabled={saving}
            className="btn-primary mt-7"
            onClick={() => void save()}
            data-testid="lecture-achievement-save-button"
          >
            <Save size={16} />
            {saving ? "저장 중" : "저장"}
          </button>
        </div>
      </section>
      <section className="rounded-md border border-ld bg-white p-6">
        <h2 className="text-lg font-semibold text-dark">강의실적 목록</h2>
        {loading ? (
          <LoadingState
            title="강의실적 조회 중"
            message="목록을 불러오고 있습니다."
          />
        ) : items.length === 0 ? (
          <EmptyState
            title="조회된 강의실적이 없습니다"
            message="필수 항목을 입력하여 새 실적을 등록하세요."
          />
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th>관리항목</th>
                  <th>발생일</th>
                  <th>인증상태</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr
                    key={item.achievementId}
                    data-testid="lecture-achievement-row"
                  >
                    <td>{item.managementItemCode}</td>
                    <td>{item.occurrenceDate}</td>
                    <td>{statusLabel(item.certificationStatus)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </section>
  );
}

import { useEffect, useState } from "react";
import {
  ApiClientError,
  lectureEvaluationAchievementApi,
  type LectureEvaluationAchievement,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

/** 강의평가 실적의 검색, 상세 입력 및 저장을 제공하는 교육영역 화면이다. */
export function LectureEvaluationAchievementManagementPage() {
  const [items, setItems] = useState<LectureEvaluationAchievement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [managementItemCode, setManagementItemCode] = useState("");
  const [occurredDate, setOccurredDate] = useState("");
  const [detail, setDetail] = useState("{}");
  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await lectureEvaluationAchievementApi.list();
      setItems(response.data?.items ?? []);
    } catch (e) {
      if (e instanceof ApiClientError && e.status === 403) setDenied(true);
      else setError("목록을 불러오지 못했습니다. 다시 시도하세요.");
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, []);
  const save = async () => {
    if (!managementItemCode || !occurredDate) {
      setError("관리항목과 업적발생일을 입력하세요.");
      return;
    }
    try {
      await lectureEvaluationAchievementApi.save({
        managementItemCode,
        occurredDate,
        achievementDetail: JSON.parse(detail),
      });
      setSuccess("강의평가 실적을 저장했습니다.");
      await load();
    } catch (e) {
      setError(
        e instanceof ApiClientError ? e.message : "저장에 실패했습니다.",
      );
    }
  };
  if (denied) return <PermissionState />;
  return (
    <main
      className="page-content"
      data-testid="lecture-evaluation-achievement-page"
    >
      <h1>강의평가 실적 관리</h1>
      {success && <SuccessState message={success} />}{" "}
      {error && <ErrorState message={error} />}
      <section className="card">
        <h2>강의평가 실적 입력</h2>
        <label>
          관리항목
          <input
            data-testid="lecture-evaluation-management-item"
            value={managementItemCode}
            onChange={(e) => setManagementItemCode(e.target.value)}
          />
        </label>
        <label>
          업적발생일
          <input
            data-testid="lecture-evaluation-occurred-date"
            type="date"
            value={occurredDate}
            onChange={(e) => setOccurredDate(e.target.value)}
          />
        </label>
        <label>
          상세 입력(JSON)
          <textarea
            data-testid="lecture-evaluation-detail"
            value={detail}
            onChange={(e) => setDetail(e.target.value)}
          />
        </label>
        <button
          data-testid="lecture-evaluation-save-button"
          onClick={() => void save()}
        >
          저장
        </button>
      </section>
      <section className="card">
        <h2>목록</h2>
        {loading ? (
          <LoadingState />
        ) : items.length === 0 ? (
          <EmptyState />
        ) : (
          <table>
            <thead>
              <tr>
                <th>관리번호</th>
                <th>관리항목</th>
                <th>업적발생일</th>
                <th>인증상태</th>
                <th>첨부</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr
                  data-testid={`lecture-evaluation-row-${item.achievementId}`}
                  key={item.achievementId}
                >
                  <td>{item.managementNo}</td>
                  <td>{item.managementItemCode}</td>
                  <td>{item.occurredDate}</td>
                  <td>{item.certificationStatus}</td>
                  <td>{item.attachmentCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </main>
  );
}

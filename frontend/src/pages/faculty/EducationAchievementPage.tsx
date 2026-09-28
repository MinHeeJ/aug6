import { useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  SuccessState,
} from "../../components/States";

type Achievement = {
  achievementId: number;
  managementItemCode: string;
  achievementOccurredOn: string;
  certificationStatus: string;
  achievementType: string;
};
type Result = { achievements: Achievement[]; totalElements: number };

/** Reusable faculty education-achievement list and draft-entry screen for the four approved types. */
export function EducationAchievementPage({
  type,
  title,
  screenId,
}: {
  type: string;
  title: string;
  screenId: string;
}) {
  const [rows, setRows] = useState<Achievement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [managementItemCode, setManagementItemCode] = useState("");
  const [occurredOn, setOccurredOn] = useState("");
  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await apiRequest<Result>(
        `/api/faculty/education-achievements?page=0&size=20&achievementType=${encodeURIComponent(type)}` as `/api/${string}`,
      );
      setRows(response.data?.achievements ?? []);
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "목록을 불러오지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [type]);
  const save = async () => {
    if (!managementItemCode.trim() || !occurredOn) {
      setError("관리항목과 업적발생일은 필수입니다.");
      return;
    }
    if (!window.confirm("실적을 저장하시겠습니까?")) return;
    try {
      await apiRequest("/api/faculty/education-achievements", {
        method: "POST",
        body: JSON.stringify({
          achievementType: type,
          managementItemCode: managementItemCode.trim(),
          achievementOccurredOn: occurredOn,
        }),
      });
      setManagementItemCode("");
      setOccurredOn("");
      setSuccess("교육영역 실적을 저장했습니다.");
      await load();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "저장하지 못했습니다.",
      );
    }
  };
  return (
    <section
      className="space-y-6"
      data-screen-id={screenId}
      data-testid="education-achievement-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">교원 포털 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">{title}</h1>
      </div>
      {success && <SuccessState title="처리 완료" message={success} />}
      {error && <ErrorState title="처리 오류" message={error} />}
      {loading ? (
        <LoadingState
          title="조회 중"
          message="교육영역 실적을 불러오고 있습니다."
        />
      ) : (
        <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>관리항목</th>
                  <th>업적발생일</th>
                  <th>인증상태</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    key={row.achievementId}
                    data-testid="education-achievement-row"
                  >
                    <td>{row.achievementId}</td>
                    <td>{row.managementItemCode}</td>
                    <td>{row.achievementOccurredOn}</td>
                    <td>{row.certificationStatus}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {rows.length === 0 && (
            <EmptyState
              title="등록된 실적이 없습니다"
              message="아래에서 교육영역 실적을 등록하세요."
            />
          )}
        </section>
      )}
      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">실적 등록</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <label>
            관리항목
            <input
              data-testid="education-management-item-input"
              className="form-input mt-2 w-full"
              value={managementItemCode}
              onChange={(event) => setManagementItemCode(event.target.value)}
            />
          </label>
          <label>
            업적발생일
            <input
              data-testid="education-occurred-on-input"
              type="date"
              className="form-input mt-2 w-full"
              value={occurredOn}
              onChange={(event) => setOccurredOn(event.target.value)}
            />
          </label>
          <button
            data-testid="education-save-button"
            className="btn-primary self-end"
            onClick={() => void save()}
          >
            저장
          </button>
        </div>
      </section>
    </section>
  );
}

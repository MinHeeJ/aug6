import { useEffect, useState } from "react";
import {
  ApiClientError,
  lectureAchievementApi,
  type LectureAchievement,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

/** 강의 실적의 검색, 상세 입력, 상태·첨부 수량 저장을 제공하는 교육영역 화면이다. */
export function LectureAchievementManagementPage() {
  const [items, setItems] = useState<LectureAchievement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [denied, setDenied] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [managementNo, setManagementNo] = useState("");
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [selectedId, setSelectedId] = useState<number | undefined>();
  const [selectedConfirmed, setSelectedConfirmed] = useState(false);
  const [managementItemCode, setManagementItemCode] = useState("");
  const [occurredDate, setOccurredDate] = useState("");
  const [detail, setDetail] = useState("{}");
  const [attachmentCount, setAttachmentCount] = useState(0);
  const [nextStatus, setNextStatus] = useState("");

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await lectureAchievementApi.list({
        managementNo,
        pageSize,
      });
      setItems(response.data?.items ?? []);
    } catch (requestError) {
      if (
        requestError instanceof ApiClientError &&
        requestError.status === 403
      ) {
        setDenied(true);
      } else {
        setError("목록을 불러오지 못했습니다. 다시 시도하세요.");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const selectItem = (item: LectureAchievement) => {
    setSelectedId(item.achievementId);
    setSelectedConfirmed(item.certificationStatus === "EVALUATION_CONFIRMED");
    setManagementItemCode(item.managementItemCode);
    setOccurredDate(item.occurredDate);
    setDetail(JSON.stringify(item.achievementDetail, null, 2));
    setAttachmentCount(item.attachmentCount);
    setNextStatus("");
    setSuccess(null);
  };

  const save = async () => {
    if (selectedConfirmed) {
      setError("평가확정된 실적은 수정하거나 첨부를 변경할 수 없습니다.");
      return;
    }
    if (!managementItemCode.trim() || !occurredDate) {
      setError("관리항목과 업적발생일을 입력하세요.");
      return;
    }
    let achievementDetail: Record<string, unknown>;
    try {
      achievementDetail = JSON.parse(detail) as Record<string, unknown>;
    } catch {
      setError("상세 입력은 올바른 JSON 형식으로 입력하세요.");
      return;
    }
    if (!window.confirm("강의 실적을 저장하시겠습니까?")) return;

    try {
      await lectureAchievementApi.save({
        achievementId: selectedId,
        managementItemCode: managementItemCode.trim(),
        occurredDate,
        achievementDetail,
        attachmentCount,
        nextStatus: nextStatus || undefined,
      });
      setSuccess("강의 실적을 저장했습니다.");
      setSelectedId(undefined);
      setNextStatus("");
      await load();
    } catch (requestError) {
      setError(
        requestError instanceof ApiClientError
          ? requestError.message
          : "저장에 실패했습니다. 입력값을 확인하세요.",
      );
    }
  };

  if (denied) return <PermissionState />;

  return (
    <main
      className="page-content space-y-6"
      data-testid="lecture-achievement-page"
    >
      <section className="rounded-md bg-lightsecondary p-6 shadow-none">
        <h1 className="text-xl font-semibold text-dark">강의 실적 관리</h1>
        <p className="mt-2 text-sm text-muted">교육영역 / 강의실적</p>
      </section>
      {success && <SuccessState message={success} />}
      {error && <ErrorState message={error} />}

      <section
        className="card space-y-4"
        data-testid="lecture-achievement-search-panel"
      >
        <h2>검색조건</h2>
        <div className="flex flex-wrap items-end gap-3">
          <label>
            관리번호
            <input
              data-testid="lecture-management-no-search"
              value={managementNo}
              onChange={(event) => setManagementNo(event.target.value)}
            />
          </label>
          <label>
            표시 건수
            <select
              data-testid="lecture-page-size"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
          <button
            data-testid="lecture-search-button"
            onClick={() => void load()}
          >
            조회
          </button>
        </div>
      </section>

      <section
        className="card space-y-4"
        data-testid="lecture-achievement-detail-panel"
      >
        <h2>{selectedId ? "강의 실적 수정" : "강의 실적 입력"}</h2>
        {selectedConfirmed && (
          <p data-testid="lecture-confirmed-lock-message" role="alert">
            평가확정된 실적은 수정·삭제·첨부 변경이 불가합니다.
          </p>
        )}
        <label>
          관리항목 <span aria-label="필수">*</span>
          <input
            data-testid="lecture-management-item"
            disabled={selectedConfirmed}
            value={managementItemCode}
            onChange={(event) => setManagementItemCode(event.target.value)}
          />
        </label>
        <label>
          업적발생일 <span aria-label="필수">*</span>
          <input
            data-testid="lecture-occurred-date"
            type="date"
            value={occurredDate}
            onChange={(event) => setOccurredDate(event.target.value)}
          />
        </label>
        <label>
          첨부 수
          <input
            data-testid="lecture-attachment-count"
            min="0"
            type="number"
            value={attachmentCount}
            onChange={(event) =>
              setAttachmentCount(Math.max(0, Number(event.target.value)))
            }
          />
        </label>
        <label>
          상태 전이
          <select
            data-testid="lecture-next-status"
            value={nextStatus}
            onChange={(event) => setNextStatus(event.target.value)}
          >
            <option value="">상태 변경 없음</option>
            <option value="SUBMITTED">제출</option>
          </select>
        </label>
        <label>
          상세 입력(JSON)
          <textarea
            data-testid="lecture-detail"
            value={detail}
            onChange={(event) => setDetail(event.target.value)}
          />
        </label>
        <button
          data-testid="lecture-save-button"
          disabled={selectedConfirmed}
          onClick={() => void save()}
        >
          저장
        </button>
      </section>

      <section className="card" data-testid="lecture-achievement-list-panel">
        <h2>목록</h2>
        <table>
          <thead>
            <tr>
              <th>관리번호</th>
              <th>관리항목</th>
              <th>업적발생일</th>
              <th>인증상태</th>
              <th>첨부</th>
              <th>선택</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6}>
                  <LoadingState />
                </td>
              </tr>
            ) : items.length === 0 ? (
              <tr>
                <td colSpan={6}>
                  <EmptyState />
                </td>
              </tr>
            ) : (
              items.map((item) => (
                <tr
                  data-testid={`lecture-row-${item.achievementId}`}
                  key={item.achievementId}
                >
                  <td>{item.managementNo}</td>
                  <td>{item.managementItemCode}</td>
                  <td>{item.occurredDate}</td>
                  <td>{item.certificationStatus}</td>
                  <td>{item.attachmentCount}</td>
                  <td>
                    <button
                      data-testid={`lecture-select-${item.achievementId}`}
                      onClick={() => selectItem(item)}
                    >
                      상세
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>
    </main>
  );
}

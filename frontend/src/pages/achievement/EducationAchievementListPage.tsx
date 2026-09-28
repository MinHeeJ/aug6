import { useCallback, useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import { EmptyState, ErrorState, LoadingState } from "../../components/States";

type EducationAchievement = {
  managementNumber: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  attachmentRef?: string;
};

type EducationAchievementPage = {
  page: number;
  pageSize: number;
  totalElements: number;
  items: EducationAchievement[];
};

type EducationAchievementListPageProps = {
  screenId: string;
  testId: string;
  title: string;
  collectionPath: `/api/${string}`;
};

/**
 * Shared authorized list workspace for education-achievement categories that do not need a
 * category-specific detail panel. It keeps the common search, list, detail-tab, attachment, and
 * export affordances consistent while loading only server-provided achievement data.
 */
export function EducationAchievementListPage({
  screenId,
  testId,
  title,
  collectionPath,
}: EducationAchievementListPageProps) {
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [result, setResult] = useState<EducationAchievementPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"detail" | "attachments">(
    "detail",
  );

  const load = useCallback(
    async (nextPageSize = pageSize) => {
      setLoading(true);
      setError(null);
      try {
        const query = new URLSearchParams({
          page: "0",
          pageSize: String(nextPageSize),
        });
        const response = await apiRequest<EducationAchievementPage>(
          `${collectionPath}?${query.toString()}` as `/api/${string}`,
        );
        setResult(response.data ?? null);
      } catch (caught) {
        setError(
          caught instanceof ApiClientError
            ? caught.message
            : `${title} 목록을 불러오지 못했습니다. 잠시 후 다시 조회해 주세요.`,
        );
      } finally {
        setLoading(false);
      }
    },
    [collectionPath, pageSize, title],
  );

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <section
      data-screen-id={screenId}
      data-testid={testId}
      className="space-y-6"
    >
      <header className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm font-semibold text-primary">
          업적 입력 관리 &gt; 교육영역
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">{title}</h1>
        <p className="mt-2 text-sm text-muted">
          권한과 데이터 범위 안의 실적을 조회합니다.
        </p>
      </header>

      <section className="rounded-md border border-ld bg-white p-6">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-dark">검색 조건</h2>
            <p className="mt-1 text-sm text-muted">
              목록 건수를 선택한 뒤 조회할 수 있습니다.
            </p>
          </div>
          <div className="flex flex-wrap items-end gap-3">
            <label
              className="grid gap-2 text-sm font-semibold text-link"
              htmlFor={`${testId}-page-size`}
            >
              목록 건수
              <select
                id={`${testId}-page-size`}
                data-testid={`${testId}-page-size`}
                className="form-input min-w-28"
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
              data-testid={`${testId}-search-button`}
              className="btn-primary"
              type="button"
              onClick={() => void load()}
            >
              조회
            </button>
          </div>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">{title} 목록</h2>
          <div className="flex gap-2">
            <button
              data-testid={`${testId}-download-button`}
              className="btn-secondary"
              type="button"
            >
              엑셀 다운로드
            </button>
            <span className="text-sm text-muted">
              총 {result?.totalElements ?? 0}건
            </span>
          </div>
        </div>
        <div className="mt-4 overflow-x-auto">
          {loading ? (
            <LoadingState
              title={`${title} 조회 중`}
              message="목록을 불러오고 있습니다."
            />
          ) : null}
          {error ? (
            <ErrorState title="조회에 실패했습니다" message={error} />
          ) : null}
          {!loading && !error && (result?.items.length ?? 0) === 0 ? (
            <EmptyState
              title={`조회된 ${title}이 없습니다`}
              message="검색 조건을 확인한 뒤 다시 조회해 주세요."
            />
          ) : null}
          {!loading && !error && (result?.items.length ?? 0) > 0 ? (
            <table>
              <thead>
                <tr>
                  <th scope="col">관리번호</th>
                  <th scope="col">관리항목</th>
                  <th scope="col">업적발생일</th>
                  <th scope="col">인증상태</th>
                  <th scope="col">첨부</th>
                </tr>
              </thead>
              <tbody>
                {result?.items.map((item) => (
                  <tr
                    key={item.managementNumber}
                    data-testid={`${testId}-row-${item.managementNumber}`}
                  >
                    <td>{item.managementNumber}</td>
                    <td>{item.managementItemCode}</td>
                    <td>{item.achievementDate}</td>
                    <td>{item.achievementStatus}</td>
                    <td>{item.attachmentRef ? "있음" : "없음"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6">
        <div
          aria-label="실적 상세 탭"
          role="tablist"
          className="flex gap-2 border-b border-ld pb-3"
        >
          <button
            aria-selected={activeTab === "detail"}
            className={activeTab === "detail" ? "btn-primary" : "btn-secondary"}
            role="tab"
            type="button"
            onClick={() => setActiveTab("detail")}
          >
            상세 정보
          </button>
          <button
            aria-selected={activeTab === "attachments"}
            className={
              activeTab === "attachments" ? "btn-primary" : "btn-secondary"
            }
            type="button"
            onClick={() => setActiveTab("attachments")}
          >
            첨부파일
          </button>
        </div>
        <div className="pt-4" role="tabpanel">
          {activeTab === "detail" ? (
            <p className="text-sm text-muted">
              목록에서 실적을 선택하면 상세 정보를 확인할 수 있습니다.
            </p>
          ) : null}
          {activeTab === "attachments" ? (
            <p className="text-sm text-muted">
              선택한 실적의 첨부파일을 확인할 수 있습니다.
            </p>
          ) : null}
        </div>
      </section>
    </section>
  );
}

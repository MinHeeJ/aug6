import { useCallback, useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import { EmptyState, ErrorState, LoadingState } from "../../components/States";

type GraduateDegreeStudent = {
  degreeType: "MASTER" | "DOCTOR";
  studentName: string;
  thesisTitle?: string;
  degreeAwardedDate: string;
};

type GraduateDegreeAchievement = {
  managementNumber: string;
  managementItemCode: string;
  achievementDate: string;
  achievementStatus: string;
  students: GraduateDegreeStudent[];
};

type GraduateDegreePage = {
  page: number;
  pageSize: number;
  totalElements: number;
  items: GraduateDegreeAchievement[];
};

const COLLECTION_PATH = "/api/business/graduate-degree-achievements" as const;

/**
 * FR-028 entry screen for the authorized graduate-degree achievement list and its selected
 * student detail. The page renders only API data and preserves the established list page sizes.
 */
export function GraduateDegreeAchievementPage() {
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [result, setResult] = useState<GraduateDegreePage | null>(null);
  const [selected, setSelected] = useState<GraduateDegreeAchievement | null>(
    null,
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    async (nextPageSize = pageSize) => {
      setLoading(true);
      setError(null);
      try {
        const query = new URLSearchParams({
          page: "0",
          pageSize: String(nextPageSize),
        });
        const response = await apiRequest<GraduateDegreePage>(
          `${COLLECTION_PATH}?${query.toString()}` as `/api/${string}`,
        );
        const nextResult = response.data ?? null;
        setResult(nextResult);
        setSelected(
          (current) =>
            nextResult?.items.find(
              (item) => item.managementNumber === current?.managementNumber,
            ) ?? null,
        );
      } catch (caught) {
        setError(
          caught instanceof ApiClientError
            ? caught.message
            : "석·박사 배출 실적 목록을 불러오지 못했습니다. 잠시 후 다시 조회해 주세요.",
        );
      } finally {
        setLoading(false);
      }
    },
    [pageSize],
  );

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <section
      data-screen-id="SCR-GRADUATE-DEGREE-ACHIEVEMENT"
      data-testid="SCR-GRADUATE-DEGREE-ACHIEVEMENT"
      className="space-y-6"
    >
      <header className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm font-semibold text-primary">
          업적 입력 관리 &gt; 교육영역
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          석·박사 배출 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          권한과 데이터 범위 안의 석·박사 배출 실적을 조회합니다.
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
              htmlFor="graduate-degree-page-size"
            >
              목록 건수
              <select
                id="graduate-degree-page-size"
                data-testid="graduate-degree-page-size"
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
              data-testid="graduate-degree-search-button"
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
        <div
          aria-label="석·박사 배출 상세 탭"
          role="tablist"
          className="flex gap-2 border-b border-ld pb-3"
        >
          <button
            className="btn-primary"
            role="tab"
            aria-selected="true"
            type="button"
          >
            상세 정보
          </button>
          <button className="btn-secondary" aria-selected="false" type="button">
            첨부파일
          </button>
        </div>
        <p className="pt-4 text-sm text-muted">
          목록에서 실적을 선택하면 상세 정보와 첨부파일을 확인할 수 있습니다.
        </p>
      </section>

      <section className="rounded-md border border-ld bg-white p-6">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            석·박사 배출 실적 목록
          </h2>
          <div className="flex items-center gap-3">
            <button
              data-testid="graduate-degree-download-button"
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
              title="석·박사 배출 실적 조회 중"
              message="목록을 불러오고 있습니다."
            />
          ) : null}
          {error ? (
            <ErrorState title="조회에 실패했습니다" message={error} />
          ) : null}
          {!loading && !error && (result?.items.length ?? 0) === 0 ? (
            <EmptyState
              title="조회된 석·박사 배출 실적이 없습니다"
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
                </tr>
              </thead>
              <tbody>
                {result?.items.map((item) => (
                  <tr
                    key={item.managementNumber}
                    data-testid={`graduate-degree-row-${item.managementNumber}`}
                    className="cursor-pointer"
                    onClick={() => setSelected(item)}
                  >
                    <td>{item.managementNumber}</td>
                    <td>{item.managementItemCode}</td>
                    <td>{item.achievementDate}</td>
                    <td>{item.achievementStatus}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}
        </div>
      </section>

      <section
        data-testid="graduate-degree-student-detail"
        className="rounded-md border border-ld bg-white p-6"
      >
        <h2 className="text-lg font-semibold text-dark">지도학생 상세</h2>
        {!selected ? (
          <p className="mt-3 text-sm text-muted">
            실적을 선택하면 지도학생 세부내역을 확인할 수 있습니다.
          </p>
        ) : null}
        {selected ? (
          <div className="mt-4 overflow-x-auto">
            <table>
              <thead>
                <tr>
                  <th scope="col">학위구분</th>
                  <th scope="col">학생명</th>
                  <th scope="col">논문제목</th>
                  <th scope="col">수여일</th>
                </tr>
              </thead>
              <tbody>
                {selected.students.map((student) => (
                  <tr
                    key={`${student.degreeType}-${student.studentName}-${student.degreeAwardedDate}`}
                  >
                    <td>{student.degreeType === "MASTER" ? "석사" : "박사"}</td>
                    <td>{student.studentName}</td>
                    <td>{student.thesisTitle ?? "-"}</td>
                    <td>{student.degreeAwardedDate}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </section>
  );
}

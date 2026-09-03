import type React from "react";
import { AlertTriangle, RefreshCw, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  evaluationBatchResultApi,
  type EvaluationBatchResultErrorRow,
  type EvaluationBatchResultRow,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
} from "../../components/States";

type Filters = {
  batchId: string;
  jobType: string;
  targetCondition: string;
};

const initialFilters: Filters = {
  batchId: "",
  jobType: "",
  targetCondition: "",
};

type ErrorPanel = {
  batchId: string;
  loading: boolean;
  error: string | null;
  rows: EvaluationBatchResultErrorRow[];
  totalElements: number;
};

export function EvaluationBatchResultPage() {
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [results, setResults] = useState<EvaluationBatchResultRow[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorPanel, setErrorPanel] = useState<ErrorPanel | null>(null);

  const search = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await evaluationBatchResultApi.list({
        ...filters,
        page,
        size: pageSize,
      });
      setResults(response.data?.results ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void search();
  }, [page, pageSize]);

  const openErrors = async (batchId: string) => {
    try {
      setErrorPanel({
        batchId,
        loading: true,
        error: null,
        rows: [],
        totalElements: 0,
      });
      const response = await evaluationBatchResultApi.errors(batchId, {
        page: 0,
        size: 20,
      });
      setErrorPanel({
        batchId,
        loading: false,
        error: null,
        rows: response.data?.errors ?? [],
        totalElements: response.data?.totalElements ?? 0,
      });
    } catch (caught) {
      const message =
        caught instanceof ApiClientError
          ? caught.message
          : "오류 상세를 조회하지 못했습니다.";
      setErrorPanel({
        batchId,
        loading: false,
        error: message,
        rows: [],
        totalElements: 0,
      });
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      setError(caught.message);
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "처리 결과를 조회하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVALUATION-BATCH-RESULT"
        data-testid="evaluation-batch-result-page"
      >
        <PermissionState
          title="처리 결과 조회 권한이 없습니다"
          message="R09 권한 또는 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVALUATION-BATCH-RESULT"
      data-testid="evaluation-batch-result-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 평가 일괄처리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              처리 결과 조회
            </h1>
            <p className="mt-2 text-sm text-muted">
              평가자료 생성·삭제, 점수 재계산, 최종평가 확정 batchId별 결과와
              오류 상세를 조회합니다. 이 화면은 일괄작업을 실행하거나 재실행하지
              않습니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="evaluation-batch-result-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </div>

      {error ? (
        <ErrorState title="처리 결과 조회 오류" message={error} />
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-6 shadow-md"
        data-testid="evaluation-batch-result-filter-panel"
      >
        <div className="grid gap-4 md:grid-cols-3">
          <Field label="배치ID">
            <input
              id="batchResultBatchId"
              className="field-input"
              value={filters.batchId}
              onChange={(event) =>
                setFilters({ ...filters, batchId: event.target.value })
              }
              placeholder="B45-GENERATION-..."
              data-testid="evaluation-batch-result-batch-id-input"
            />
          </Field>
          <Field label="작업유형">
            <select
              id="batchResultJobType"
              className="field-input"
              value={filters.jobType}
              onChange={(event) =>
                setFilters({ ...filters, jobType: event.target.value })
              }
              data-testid="evaluation-batch-result-job-type-select"
            >
              <option value="">전체</option>
              <option value="GENERATION">생성</option>
              <option value="DELETION">삭제</option>
              <option value="RECALCULATION">재계산</option>
              <option value="CONFIRMATION">확정</option>
            </select>
          </Field>
          <Field label="대상조건">
            <input
              id="batchResultTargetCondition"
              className="field-input"
              value={filters.targetCondition}
              onChange={(event) =>
                setFilters({ ...filters, targetCondition: event.target.value })
              }
              placeholder="평가연도, 영역, 대상자 등"
              data-testid="evaluation-batch-result-target-condition-input"
            />
          </Field>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">
            일괄처리 결과 목록
          </h2>
          <button
            type="button"
            className="inline-flex items-center gap-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="evaluation-batch-result-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
        {loading ? (
          <LoadingState
            title="처리 결과 조회 중"
            message="batchId별 총건수, 성공, 실패, 제외 건수를 조회합니다."
          />
        ) : null}
        {!loading && results.length === 0 ? (
          <EmptyState
            title="처리 결과가 없습니다"
            message="배치ID·작업유형·대상조건을 변경해 보세요."
          />
        ) : null}
        {!loading && results.length > 0 ? (
          <div className="overflow-x-auto">
            <table
              className="min-w-full divide-y divide-ld text-left text-sm"
              data-testid="evaluation-batch-result-table"
            >
              <thead className="bg-lightsecondary text-xs uppercase text-muted">
                <tr>
                  <th className="px-4 py-3">배치ID</th>
                  <th className="px-4 py-3">작업유형</th>
                  <th className="px-4 py-3">상태</th>
                  <th className="px-4 py-3">총</th>
                  <th className="px-4 py-3">성공</th>
                  <th className="px-4 py-3">실패</th>
                  <th className="px-4 py-3">제외</th>
                  <th className="px-4 py-3">요청ID</th>
                  <th className="px-4 py-3">완료일시</th>
                  <th className="px-4 py-3">오류</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {results.map((row) => (
                  <tr
                    key={row.batchId}
                    data-testid="evaluation-batch-result-row"
                  >
                    <td className="px-4 py-3 font-semibold text-primary">
                      {row.batchId}
                    </td>
                    <td className="px-4 py-3">
                      {row.jobTypeName || jobTypeName(row.jobType)}
                    </td>
                    <td className="px-4 py-3">{row.requestStatus}</td>
                    <td className="px-4 py-3">{row.totalCount}</td>
                    <td className="px-4 py-3 text-success">
                      {row.successCount}
                    </td>
                    <td className="px-4 py-3 text-error">{row.failureCount}</td>
                    <td className="px-4 py-3">{row.excludedCount}</td>
                    <td className="px-4 py-3">{row.requestId}</td>
                    <td className="px-4 py-3">{row.completedAt ?? "-"}</td>
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        className="inline-flex items-center gap-1 rounded-md border border-ld px-3 py-2 text-xs font-semibold text-ld disabled:opacity-50"
                        onClick={() => void openErrors(row.batchId)}
                        disabled={row.failureCount === 0}
                        data-testid="evaluation-batch-result-errors-button"
                      >
                        <AlertTriangle size={14} /> 상세
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <div className="mt-4 flex items-center justify-between text-sm text-muted">
          <span>총 {totalElements}건</span>
          <select
            className="field-input w-28"
            value={pageSize}
            onChange={(event) => {
              setPage(0);
              setPageSize(Number(event.target.value) as PageSize);
            }}
            data-testid="evaluation-batch-result-size-select"
          >
            <option value={20}>20건</option>
            <option value={50}>50건</option>
            <option value={100}>100건</option>
          </select>
        </div>
      </section>

      {errorPanel ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          role="dialog"
          aria-modal="true"
          data-testid="evaluation-batch-result-error-modal"
        >
          <section className="w-full max-w-3xl rounded-md bg-white p-6 shadow-md">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="text-lg font-semibold text-dark">오류 상세</h2>
                <p className="mt-2 text-sm text-muted">
                  {errorPanel.batchId} 실패 대상 식별정보와 오류 상세입니다.
                </p>
              </div>
              <button
                type="button"
                className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-ld"
                onClick={() => setErrorPanel(null)}
                data-testid="evaluation-batch-result-error-close-button"
              >
                닫기
              </button>
            </div>
            {errorPanel.loading ? (
              <LoadingState
                title="오류 상세 조회 중"
                message="실패 대상 정보를 조회합니다."
              />
            ) : null}
            {errorPanel.error ? (
              <ErrorState
                title="오류 상세 조회 실패"
                message={errorPanel.error}
              />
            ) : null}
            {!errorPanel.loading &&
            !errorPanel.error &&
            errorPanel.rows.length === 0 ? (
              <EmptyState
                title="오류 상세가 없습니다"
                message="이 batchId에 연결된 실패 상세가 없습니다."
              />
            ) : null}
            {!errorPanel.loading &&
            !errorPanel.error &&
            errorPanel.rows.length > 0 ? (
              <div className="mt-4 overflow-x-auto">
                <table
                  className="min-w-full divide-y divide-ld text-left text-sm"
                  data-testid="evaluation-batch-result-error-table"
                >
                  <thead className="bg-lightsecondary text-xs uppercase text-muted">
                    <tr>
                      <th className="px-4 py-3">대상</th>
                      <th className="px-4 py-3">대상명</th>
                      <th className="px-4 py-3">오류코드</th>
                      <th className="px-4 py-3">메시지</th>
                      <th className="px-4 py-3">상세</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ld">
                    {errorPanel.rows.map((row) => (
                      <tr
                        key={`${row.batchId}-${row.targetKey}-${row.errorCode}`}
                        data-testid="evaluation-batch-result-error-row"
                      >
                        <td className="px-4 py-3 font-semibold text-primary">
                          {row.targetKey}
                        </td>
                        <td className="px-4 py-3">{row.targetName ?? "-"}</td>
                        <td className="px-4 py-3 text-error">
                          {row.errorCode}
                        </td>
                        <td className="px-4 py-3">{row.message}</td>
                        <td className="px-4 py-3">{row.detail ?? "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <p className="mt-3 text-sm text-muted">
                  오류 총 {errorPanel.totalElements}건
                </p>
              </div>
            ) : null}
          </section>
        </div>
      ) : null}
    </section>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block text-sm font-semibold text-ld">
      {label}
      <div className="mt-2">{children}</div>
    </label>
  );
}

function jobTypeName(jobType: string) {
  switch (jobType) {
    case "GENERATION":
      return "생성";
    case "DELETION":
      return "삭제";
    case "RECALCULATION":
      return "재계산";
    case "CONFIRMATION":
      return "확정";
    default:
      return jobType;
  }
}

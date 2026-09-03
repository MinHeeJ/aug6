import type React from "react";
import { RotateCcw, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  batchProcessingResultApi,
  type ApiErrorField,
  type BatchProcessingJobType,
  type BatchProcessingResultErrorRow,
  type BatchProcessingResultRow,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
} from "../../components/States";

type BatchResultFilters = {
  batchId: string;
  batchType: "" | BatchProcessingJobType;
  targetCondition: string;
};

const initialFilters: BatchResultFilters = {
  batchId: "",
  batchType: "",
  targetCondition: "",
};

export function BatchProcessingResultPage() {
  const [filters, setFilters] = useState<BatchResultFilters>(initialFilters);
  const [rows, setRows] = useState<BatchProcessingResultRow[]>([]);
  const [errors, setErrors] = useState<BatchProcessingResultErrorRow[]>([]);
  const [selectedBatchId, setSelectedBatchId] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const selectedRow = useMemo(
    () =>
      rows.find((row) => row.batchId === selectedBatchId) ?? rows[0] ?? null,
    [rows, selectedBatchId],
  );

  const conditionSummary = useMemo(
    () =>
      `배치ID ${filters.batchId || "전체"} / 작업유형 ${filters.batchType || "전체"} / 대상조건 ${filters.targetCondition || "전체"}`,
    [filters],
  );

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response =
        await batchProcessingResultApi.listBatchProcessingResults({
          batchId: filters.batchId,
          batchType: filters.batchType,
          targetCondition: filters.targetCondition,
          page,
          size: pageSize,
        });
      const nextRows = response.data?.results ?? [];
      setRows(nextRows);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelectedBatchId((current) => current ?? nextRows[0]?.batchId ?? null);
      setFieldErrors({});
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  useEffect(() => {
    if (!selectedRow?.batchId) {
      setErrors([]);
      return;
    }
    void loadErrors(selectedRow.batchId);
  }, [selectedRow?.batchId]);

  const search = async () => {
    setPage(0);
    setSelectedBatchId(null);
    await load();
  };

  const reset = () => {
    setFilters(initialFilters);
    setSelectedBatchId(null);
    setErrors([]);
    setFieldErrors({});
    setError(null);
  };

  const loadErrors = async (batchId: string) => {
    try {
      setDetailLoading(true);
      setError(null);
      const response =
        await batchProcessingResultApi.listBatchProcessingResultErrors(batchId);
      setErrors(response.data ?? []);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setDetailLoading(false);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "처리 결과 조회를 완료하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVAL-BATCH-RESULT"
        data-testid="batch-processing-result-page"
      >
        <PermissionState
          title="처리 결과 조회 권한이 없습니다"
          message="R09 또는 일괄처리 결과 조회 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVAL-BATCH-RESULT"
      data-testid="batch-processing-result-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 일괄처리 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              처리 결과 조회
            </h1>
            <p className="mt-2 text-sm text-muted">
              평가자료 생성·삭제, 점수 재계산, 평가 확정 작업의 배치ID별 결과와
              오류 상세를 확인합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="batch-processing-result-refresh-button"
          >
            <RotateCcw size={16} />
            새로고침
          </button>
        </div>
      </div>

      {error ? (
        <ErrorState title="처리 결과 조회 오류" message={error} />
      ) : null}

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">처리 결과 검색 조건</h2>
        <p className="mt-2 text-sm text-muted">검색 조건: {conditionSummary}</p>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <Field label="배치ID" error={fieldErrors.batchId}>
            <input
              className="form-input"
              value={filters.batchId}
              onChange={update("batchId")}
              data-testid="batch-processing-batch-id-input"
            />
          </Field>
          <Field label="작업유형" error={fieldErrors.batchType}>
            <select
              className="form-select"
              value={filters.batchType}
              onChange={updateSelect("batchType")}
              data-testid="batch-processing-type-select"
            >
              <option value="">전체</option>
              <option value="GENERATION">평가자료 생성</option>
              <option value="DELETION">평가자료 삭제</option>
              <option value="RECALCULATION">점수 재계산</option>
              <option value="CONFIRMATION">평가확정·취소</option>
            </select>
          </Field>
          <Field label="대상조건" error={fieldErrors.targetCondition}>
            <input
              className="form-input"
              value={filters.targetCondition}
              onChange={update("targetCondition")}
              data-testid="batch-processing-target-condition-input"
            />
          </Field>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="batch-processing-search-button"
          >
            <Search size={16} />
            조회
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
            onClick={reset}
            data-testid="batch-processing-reset-button"
          >
            입력 초기화
          </button>
        </div>
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              배치ID별 처리 결과
            </h2>
            <p className="mt-1 text-sm text-muted">
              이 화면은 조회 전용이며 신규 처리 버튼을 제공하지 않습니다.
            </p>
          </div>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="form-select ml-2"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as PageSize)
              }
              data-testid="batch-processing-page-size-select"
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState title="처리 결과를 불러오는 중입니다" />
        ) : null}
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 처리 결과가 없습니다" />
        ) : null}
        <div className="mt-4 overflow-x-auto">
          <table
            className="min-w-full divide-y divide-ld text-sm"
            data-testid="batch-processing-result-table"
          >
            <thead className="bg-lightgray text-left text-muted">
              <tr>
                <th className="px-4 py-3">선택</th>
                <th className="px-4 py-3">배치ID</th>
                <th className="px-4 py-3">작업유형</th>
                <th className="px-4 py-3">대상조건</th>
                <th className="px-4 py-3">총건수</th>
                <th className="px-4 py-3">성공</th>
                <th className="px-4 py-3">실패</th>
                <th className="px-4 py-3">제외</th>
                <th className="px-4 py-3">상태</th>
                <th className="px-4 py-3">요청ID</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr key={row.batchId} data-testid="batch-processing-result-row">
                  <td className="px-4 py-3">
                    <input
                      type="radio"
                      name="batch-processing-result"
                      checked={
                        (selectedBatchId ?? rows[0]?.batchId) === row.batchId
                      }
                      onChange={() => setSelectedBatchId(row.batchId)}
                      data-testid="batch-processing-result-radio"
                    />
                  </td>
                  <td className="px-4 py-3 font-medium text-dark">
                    {row.batchId}
                  </td>
                  <td className="px-4 py-3">{batchTypeLabel(row.batchType)}</td>
                  <td className="px-4 py-3">{row.targetConditionSummary}</td>
                  <td className="px-4 py-3">{row.totalCount}</td>
                  <td className="px-4 py-3 text-success">{row.successCount}</td>
                  <td className="px-4 py-3 text-error">{row.failureCount}</td>
                  <td className="px-4 py-3 text-warning">
                    {row.excludedCount}
                  </td>
                  <td className="px-4 py-3">{row.jobStatus}</td>
                  <td className="px-4 py-3">{row.requestId}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mt-4 flex items-center justify-between text-sm text-muted">
          <span>총 {totalElements}건</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => Math.max(value - 1, 0))}
            data-testid="batch-processing-prev-page-button"
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => value + 1)}
            data-testid="batch-processing-next-page-button"
          >
            다음
          </button>
        </div>
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">오류 상세</h2>
        <p className="mt-2 text-sm text-muted">
          선택 배치ID: {selectedRow?.batchId ?? "미선택"}
        </p>
        {detailLoading ? (
          <LoadingState title="오류 상세를 불러오는 중입니다" />
        ) : null}
        {!detailLoading && !selectedRow ? (
          <EmptyState title="상세를 볼 배치 결과를 선택하세요" />
        ) : null}
        {!detailLoading && selectedRow && errors.length === 0 ? (
          <EmptyState title="오류 또는 제외 상세가 없습니다" />
        ) : null}
        {!detailLoading && errors.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table
              className="min-w-full divide-y divide-ld text-sm"
              data-testid="batch-processing-error-table"
            >
              <thead className="bg-lightgray text-left text-muted">
                <tr>
                  <th className="px-4 py-3">대상 식별정보</th>
                  <th className="px-4 py-3">결과</th>
                  <th className="px-4 py-3">오류코드</th>
                  <th className="px-4 py-3">오류 상세</th>
                  <th className="px-4 py-3">처리일시</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {errors.map((item) => (
                  <tr
                    key={item.batchJobItemId}
                    data-testid="batch-processing-error-row"
                  >
                    <td className="px-4 py-3">{item.targetRef}</td>
                    <td className="px-4 py-3">
                      {item.resultStatus === "FAILURE" ? "실패" : "제외"}
                    </td>
                    <td className="px-4 py-3">{item.errorCode ?? "-"}</td>
                    <td className="px-4 py-3">
                      {item.errorMessage ?? item.excludedReason ?? "-"}
                    </td>
                    <td className="px-4 py-3">
                      {formatDate(item.processedAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </section>
  );

  function update(field: "batchId" | "targetCondition") {
    return (event: React.ChangeEvent<HTMLInputElement>) => {
      setFilters((current) => ({ ...current, [field]: event.target.value }));
      setFieldErrors({});
    };
  }

  function updateSelect(field: "batchType") {
    return (event: React.ChangeEvent<HTMLSelectElement>) => {
      setFilters((current) => ({
        ...current,
        [field]: event.target.value as BatchResultFilters[typeof field],
      }));
      setFieldErrors({});
    };
  }
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      {label}
      <div className="mt-1">{children}</div>
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function formatDate(value?: string | null) {
  return value ? value.replace("T", " ").slice(0, 16) : "-";
}

function batchTypeLabel(type: string) {
  switch (type) {
    case "GENERATION":
      return "평가자료 생성";
    case "DELETION":
      return "평가자료 삭제";
    case "RECALCULATION":
      return "점수 재계산";
    case "CONFIRMATION":
      return "평가확정·취소";
    default:
      return type;
  }
}

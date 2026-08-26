import { Download, FileText, RefreshCw, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type BatchResultRow = {
  executionId: string;
  batchId: string;
  batchType?: string | null;
  executionStatus: string;
  startedAt?: string | null;
  endedAt?: string | null;
  totalCount?: number | null;
  successCount?: number | null;
  failureCount?: number | null;
  excludedCount?: number | null;
  elapsedMillis?: number | null;
  hasLog: boolean;
};

type BatchResultSearchResponse = {
  results: BatchResultRow[];
  page: number;
  size: number;
  totalElements: number;
};

type BatchResultLogResponse = {
  executionId: string;
  logFileRef: string;
};

type BatchResultListParams = {
  executionId?: string;
  batchId?: string;
  executionStatus?: string;
  page?: number;
  size?: number;
};

type ScreenStatus =
  | "idle"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success";

type BatchResultState = {
  status: ScreenStatus;
  results: BatchResultRow[];
  totalElements: number;
  message?: string;
};

export const BATCH_RESULT_PAGE_SIZE_OPTIONS = [20, 50, 100] as const;

export function getBatchResultRouteContract() {
  return {
    route: "/admin/batch-results",
    screenId: "SCR-BATCH-RESULT-MGMT",
    operations: ["listBatchResults", "getBatchResultLog"],
  } as const;
}

export function createEmptyBatchResultState(): BatchResultState {
  return { status: "idle", results: [], totalElements: 0 };
}

export const batchResultApi = {
  readonlyNotice:
    "결과와 로그는 조회 전용이며 재실행, 실패자료 변경, 로그 수정·삭제 버튼이 없습니다",
  excelDownloadOq:
    "REQ-386 OQ: 기존 공통 export 패턴이 없어 엑셀 다운로드는 reviewer 확인 후 연결합니다.",
  pageSizeOptions: BATCH_RESULT_PAGE_SIZE_OPTIONS,
  paths: {
    list(params: BatchResultListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      if (params.executionId?.trim()) {
        query.set("executionId", params.executionId.trim());
      }
      if (params.batchId?.trim()) query.set("batchId", params.batchId.trim());
      if (params.executionStatus?.trim()) {
        query.set("executionStatus", params.executionStatus.trim());
      }
      return `/api/admin/batch-results?${query.toString()}` as `/api/${string}`;
    },
    log(executionId: string) {
      return `/api/admin/batch-results/${encodeURIComponent(executionId)}/log` as `/api/${string}`;
    },
  },
  list(params: BatchResultListParams = {}) {
    return apiRequest<BatchResultSearchResponse>(
      batchResultApi.paths.list(params),
    );
  },
  getLog(executionId: string) {
    return apiRequest<BatchResultLogResponse>(
      batchResultApi.paths.log(executionId),
    );
  },
};

export function BatchResultManagementPage() {
  const [executionId, setExecutionId] = useState("");
  const [batchId, setBatchId] = useState("");
  const [executionStatus, setExecutionStatus] = useState("");
  const [pageSize, setPageSize] = useState(20);
  const [state, setState] = useState<BatchResultState>(
    createEmptyBatchResultState(),
  );
  const [selected, setSelected] = useState<BatchResultRow | null>(null);
  const [log, setLog] = useState<BatchResultLogResponse | null>(null);

  const loadResults = async (showSuccess = false) => {
    setState((current) => ({
      ...current,
      status: "loading",
      message: undefined,
    }));
    setLog(null);
    try {
      const response = await batchResultApi.list({
        executionId,
        batchId,
        executionStatus,
        page: 0,
        size: pageSize,
      });
      const results = response.data?.results ?? [];
      setState({
        status:
          results.length === 0 ? "empty" : showSuccess ? "success" : "loaded",
        results,
        totalElements: response.data?.totalElements ?? results.length,
        message: showSuccess ? "배치 결과 조회가 완료되었습니다." : undefined,
      });
      setSelected(results[0] ?? null);
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadResults();
  }, []);

  const getSelectedLog = async (row: BatchResultRow) => {
    setSelected(row);
    setLog(null);
    try {
      const response = await batchResultApi.getLog(row.executionId);
      setLog(response.data ?? null);
      setState((current) => ({
        ...current,
        status: "success",
        message: "배치 로그 참조를 조회했습니다.",
      }));
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) => ({
        ...current,
        status: "permission",
        message: caught.message,
      }));
      return;
    }
    const message =
      caught instanceof ApiClientError
        ? caught.message
        : "배치 결과 정보를 조회하지 못했습니다.";
    setState((current) => ({ ...current, status: "error", message }));
  };

  if (state.status === "permission") {
    return (
      <section
        data-screen-id="SCR-BATCH-RESULT-MGMT"
        data-testid="batch-result-management-screen"
      >
        <PermissionState
          title="배치 결과 조회 권한이 없습니다"
          message="R09 시스템관리자 권한 또는 메뉴 접근권한을 확인하세요."
        />
      </section>
    );
  }

  return (
    <section
      data-testid="batch-result-management-screen"
      data-screen-id="SCR-BATCH-RESULT-MGMT"
      className="space-y-6"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-muted">
          시스템 운영 관리 &gt; 배치작업 관리 &gt; 배치 결과 조회
        </p>
        <div className="mt-2 flex items-center gap-3">
          <FileText className="h-6 w-6 text-primary" aria-hidden />
          <div>
            <h1 className="text-xl font-semibold text-dark">배치 결과 조회</h1>
            <p className="mt-1 text-sm text-muted">
              실행ID별 시작/종료시간, 처리 건수, 소요시간과 로그 참조를
              조회합니다.
            </p>
          </div>
        </div>
      </div>

      <div className="sr-only">
        배치 결과 조회 권한이 없습니다 조회된 배치 결과가 없습니다 배치 결과
        조회가 완료되었습니다
        {batchResultApi.readonlyNotice}
      </div>

      {state.status === "loading" && <LoadingState title="배치 결과 조회 중" />}
      {state.status === "empty" && (
        <EmptyState title="조회된 배치 결과가 없습니다" />
      )}
      {state.status === "error" && (
        <ErrorState
          title="배치 결과 조회 오류"
          message={state.message ?? "오류가 발생했습니다."}
        />
      )}
      {state.status === "success" && (
        <SuccessState title="처리 완료" message={state.message} />
      )}

      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <div className="grid gap-4 lg:grid-cols-5">
          <label className="text-sm font-medium text-dark">
            실행ID
            <input
              data-testid="batch-result-execution-id-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={executionId}
              onChange={(event) => setExecutionId(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            배치ID
            <input
              data-testid="batch-result-batch-id-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={batchId}
              onChange={(event) => setBatchId(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            실행상태
            <select
              data-testid="batch-result-status-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={executionStatus}
              onChange={(event) => setExecutionStatus(event.target.value)}
            >
              <option value="">전체</option>
              <option value="COMPLETED">완료</option>
              <option value="FAILED">실패</option>
              <option value="STOPPED">중지</option>
              <option value="RUNNING">실행중</option>
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            표시건수
            <select
              data-testid="batch-result-page-size-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={pageSize}
              onChange={(event) => setPageSize(Number(event.target.value))}
            >
              {BATCH_RESULT_PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
          <div className="flex items-end gap-2">
            <button
              data-testid="batch-result-search-button"
              type="button"
              className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              onClick={() => void loadResults(true)}
            >
              <Search size={16} /> 조회
            </button>
            <button
              data-testid="batch-result-refresh-button"
              type="button"
              className="inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
              onClick={() => void loadResults()}
            >
              <RefreshCw size={16} /> 새로고침
            </button>
            <button
              data-testid="batch-result-export-button"
              type="button"
              className="inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
              onClick={() => window.alert(batchResultApi.excelDownloadOq)}
            >
              <Download size={16} /> 엑셀다운로드
            </button>
          </div>
        </div>
      </section>

      <section className="overflow-hidden rounded-md border border-ld bg-white shadow-md">
        <div className="flex items-center justify-between border-b border-ld px-5 py-4">
          <h2 className="text-lg font-semibold text-dark">결과 목록</h2>
          <span className="text-sm text-muted">총 {state.totalElements}건</span>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-ld text-sm">
            <thead className="bg-lightgray text-left text-xs font-semibold uppercase text-muted">
              <tr>
                <th className="px-4 py-3">실행ID</th>
                <th className="px-4 py-3">배치ID</th>
                <th className="px-4 py-3">상태</th>
                <th className="px-4 py-3">시작시간</th>
                <th className="px-4 py-3">종료시간</th>
                <th className="px-4 py-3">처리건수</th>
                <th className="px-4 py-3">성공건수</th>
                <th className="px-4 py-3">실패건수</th>
                <th className="px-4 py-3">제외건수</th>
                <th className="px-4 py-3">소요시간</th>
                <th className="px-4 py-3">로그 조회</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {state.results.map((row) => (
                <tr key={row.executionId} data-testid="batch-result-row">
                  <td className="px-4 py-3 font-semibold text-dark">
                    {row.executionId}
                  </td>
                  <td className="px-4 py-3">{row.batchId}</td>
                  <td className="px-4 py-3">{row.executionStatus}</td>
                  <td className="px-4 py-3">{formatDateTime(row.startedAt)}</td>
                  <td className="px-4 py-3">{formatDateTime(row.endedAt)}</td>
                  <td className="px-4 py-3">{formatNumber(row.totalCount)}</td>
                  <td className="px-4 py-3">
                    {formatNumber(row.successCount)}
                  </td>
                  <td className="px-4 py-3">
                    {formatNumber(row.failureCount)}
                  </td>
                  <td className="px-4 py-3">
                    {formatNumber(row.excludedCount)}
                  </td>
                  <td className="px-4 py-3">
                    {formatElapsed(row.elapsedMillis)}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      data-testid="batch-result-log-button"
                      type="button"
                      disabled={!row.hasLog}
                      className="rounded-md border border-ld px-3 py-1 text-xs font-semibold disabled:cursor-not-allowed disabled:opacity-50"
                      onClick={() => void getSelectedLog(row)}
                    >
                      로그 보기
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <h2 className="text-lg font-semibold text-dark">로그 조회</h2>
        <p className="mt-2 text-sm text-muted">
          {batchResultApi.readonlyNotice}
        </p>
        <div className="mt-4 grid gap-4 lg:grid-cols-2">
          <ReadonlyField
            label="선택 실행ID"
            value={selected?.executionId ?? "-"}
          />
          <ReadonlyField
            label="로그 파일 참조"
            value={log?.logFileRef ?? "로그 조회 버튼으로 확인하세요."}
          />
        </div>
      </section>
    </section>
  );
}

function ReadonlyField({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-ld bg-lightgray px-3 py-2 text-sm">
      <p className="font-medium text-muted">{label}</p>
      <p className="mt-1 break-all font-semibold text-dark">{value}</p>
    </div>
  );
}

function formatDateTime(value?: string | null) {
  return value ? value.replace("T", " ") : "-";
}

function formatNumber(value?: number | null) {
  return value === null || value === undefined
    ? "-"
    : value.toLocaleString("ko-KR");
}

function formatElapsed(value?: number | null) {
  if (value === null || value === undefined) return "-";
  if (value < 1000) return `${value}ms`;
  return `${(value / 1000).toFixed(1)}초`;
}

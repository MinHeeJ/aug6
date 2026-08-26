import type React from "react";
import { useEffect, useState } from "react";
import { Download, RefreshCw, RotateCcw, Search } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type BatchRetryTargetRow = {
  originalExecutionId: string;
  batchId: string;
  executionStatus: string;
  failedItemKey?: string | null;
  failureReason?: string | null;
  startedAt?: string | null;
  endedAt?: string | null;
};

type BatchRetryTargetSearchResponse = {
  targets: BatchRetryTargetRow[];
  page: number;
  size: number;
  totalElements: number;
};

export type BatchRetryResultRow = {
  retryExecutionId: string;
  originalExecutionId: string;
  failedItemKey?: string | null;
  retryReason: string;
  requestId?: string;
  createdAt?: string;
  createdBy?: number;
};

type BatchRetryListParams = {
  originalExecutionId?: string;
  failedItemKey?: string;
  page?: number;
  size?: number;
};

type BatchRetryPayload = {
  originalExecutionId: string;
  failedItemKey?: string;
  retryReason: string;
};

type ScreenStatus =
  | "idle"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success"
  | "progress";

type BatchRetryState = {
  status: ScreenStatus;
  targets: BatchRetryTargetRow[];
  message?: string;
};

type BatchRetryAction =
  | { type: "loading" }
  | { type: "loaded"; targets: BatchRetryTargetRow[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string }
  | { type: "progress"; message: string };

export const BATCH_RETRY_PAGE_SIZE_OPTIONS = [20, 50, 100] as const;

export function getBatchRetryRouteContract() {
  return {
    route: "/admin/batch-retries",
    screenId: "SCR-BATCH-RETRY-MGMT",
    operations: ["listBatchRetryTargets", "createBatchRetry"],
  } as const;
}

export function createEmptyBatchRetryState(): BatchRetryState {
  return { status: "idle", targets: [] };
}

export function reduceBatchRetryState(
  state: BatchRetryState,
  action: BatchRetryAction,
): BatchRetryState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.targets.length === 0 ? "empty" : "loaded",
        targets: action.targets,
        message: undefined,
      };
    case "error":
      return { ...state, status: "error", message: action.message };
    case "permission":
      return { ...state, status: "permission", message: "권한 없음" };
    case "success":
      return { ...state, status: "success", message: action.message };
    case "progress":
      return { ...state, status: "progress", message: action.message };
    default:
      return state;
  }
}

export const batchRetryApi = {
  uiMessages: {
    retryConfirm(originalExecutionId: string) {
      return `${originalExecutionId} 실패 대상을 재처리하시겠습니까?`;
    },
    retrySuccess: "재처리 요청이 등록되었습니다.",
    progress: "10초 이상 소요될 수 있어 재처리 진행상황을 표시합니다.",
    reasonRequired: "재처리 사유를 입력하세요.",
    targetRequired: "실패 대상을 선택하세요.",
  },
  excelDownloadOq:
    "REQ-386 OQ: 기존 공통 export 패턴이 없어 엑셀 다운로드는 reviewer 확인 후 연결합니다.",
  pageSizeOptions: BATCH_RETRY_PAGE_SIZE_OPTIONS,
  validateRetry(target: BatchRetryTargetRow | null, retryReason: string) {
    const errors: Record<string, string> = {};
    if (!target) errors.target = batchRetryApi.uiMessages.targetRequired;
    if (!retryReason.trim())
      errors.retryReason = batchRetryApi.uiMessages.reasonRequired;
    return errors;
  },
  paths: {
    targets(params: BatchRetryListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      if (params.originalExecutionId?.trim()) {
        query.set("originalExecutionId", params.originalExecutionId.trim());
      }
      if (params.failedItemKey?.trim()) {
        query.set("failedItemKey", params.failedItemKey.trim());
      }
      return `/api/admin/batch-retries/targets?${query.toString()}` as `/api/${string}`;
    },
    create() {
      return "/api/admin/batch-retries" as const;
    },
  },
  toCreatePayload(
    target: BatchRetryTargetRow,
    retryReason: string,
  ): BatchRetryPayload {
    return {
      originalExecutionId: target.originalExecutionId,
      failedItemKey: target.failedItemKey?.trim() || undefined,
      retryReason: retryReason.trim(),
    };
  },
  listTargets(params: BatchRetryListParams = {}) {
    return apiRequest<BatchRetryTargetSearchResponse>(
      batchRetryApi.paths.targets(params),
    );
  },
  createRetry(payload: BatchRetryPayload) {
    return apiRequest<BatchRetryResultRow>(batchRetryApi.paths.create(), {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};

export function BatchRetryManagementPage() {
  const [originalExecutionId, setOriginalExecutionId] = useState("");
  const [failedItemKey, setFailedItemKey] = useState("");
  const [pageSize, setPageSize] = useState(20);
  const [state, setState] = useState<BatchRetryState>(
    createEmptyBatchRetryState(),
  );
  const [selected, setSelected] = useState<BatchRetryTargetRow | null>(null);
  const [retryReason, setRetryReason] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [lastResult, setLastResult] = useState<BatchRetryResultRow | null>(
    null,
  );
  const [processing, setProcessing] = useState(false);

  const loadTargets = async () => {
    setState((current) => reduceBatchRetryState(current, { type: "loading" }));
    setFieldErrors({});
    try {
      const response = await batchRetryApi.listTargets({
        originalExecutionId,
        failedItemKey,
        page: 0,
        size: pageSize,
      });
      const targets = response.data?.targets ?? [];
      setState((current) =>
        reduceBatchRetryState(current, { type: "loaded", targets }),
      );
      if (selected) {
        setSelected(
          targets.find(
            (target) =>
              target.originalExecutionId === selected.originalExecutionId &&
              (target.failedItemKey ?? "") === (selected.failedItemKey ?? ""),
          ) ?? null,
        );
      }
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadTargets();
  }, []);

  const handleRetry = async () => {
    const errors = batchRetryApi.validateRetry(selected, retryReason);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0 || !selected) return;
    if (
      !window.confirm(
        batchRetryApi.uiMessages.retryConfirm(selected.originalExecutionId),
      )
    ) {
      return;
    }
    setProcessing(true);
    const progressTimer = window.setTimeout(() => {
      setState((current) =>
        reduceBatchRetryState(current, {
          type: "progress",
          message: batchRetryApi.uiMessages.progress,
        }),
      );
    }, 10000);
    try {
      const response = await batchRetryApi.createRetry(
        batchRetryApi.toCreatePayload(selected, retryReason),
      );
      setLastResult(response.data ?? null);
      await loadTargets();
      setState((current) =>
        reduceBatchRetryState(current, {
          type: "success",
          message: batchRetryApi.uiMessages.retrySuccess,
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    } finally {
      window.clearTimeout(progressTimer);
      setProcessing(false);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) {
        setState((current) =>
          reduceBatchRetryState(current, { type: "permission" }),
        );
        return;
      }
      const fields = Object.fromEntries(
        (caught.apiError?.fields ?? []).map((field) => [
          field.field,
          field.message,
        ]),
      );
      setFieldErrors(fields);
      setState((current) =>
        reduceBatchRetryState(current, {
          type: "error",
          message:
            caught.apiError?.message ??
            "배치 재처리 정보를 처리하지 못했습니다.",
        }),
      );
      return;
    }
    setState((current) =>
      reduceBatchRetryState(current, {
        type: "error",
        message: "배치 재처리 정보를 처리하지 못했습니다.",
      }),
    );
  };

  return (
    <section data-testid="batch-retry-page" className="space-y-6">
      <header className="rounded-md bg-lightsecondary p-6 shadow-none dark:bg-white/5">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm font-semibold text-primary">
              시스템 운영 관리 &gt; 배치작업 관리
            </p>
            <h1 className="mt-2 text-2xl font-bold text-dark dark:text-white">
              배치 오류 재처리
            </h1>
            <p className="mt-2 text-sm text-muted dark:text-white/60">
              실패 배치 또는 개별 실패 건을 선택하고 사유를 입력해 원실행ID와
              연결된 별도 재처리 결과를 생성합니다.
            </p>
          </div>
          <RotateCcw className="text-primary" size={34} aria-hidden />
        </div>
      </header>

      <section
        className="rounded-md bg-white p-5 shadow-md dark:bg-white/5"
        aria-label="재처리 대상 검색"
      >
        <div className="grid gap-3 lg:grid-cols-[1fr_1fr_160px_auto]">
          <TextInput
            label="원실행ID"
            value={originalExecutionId}
            onChange={setOriginalExecutionId}
            testId="batch-retry-original-filter"
          />
          <TextInput
            label="실패 대상"
            value={failedItemKey}
            onChange={setFailedItemKey}
            testId="batch-retry-item-filter"
          />
          <label className="text-sm font-semibold text-link dark:text-white/80">
            표시건수
            <select
              data-testid="batch-retry-page-size-select"
              className="mt-2 w-full rounded-xl border border-ld px-3 py-2"
              value={pageSize}
              onChange={(event) => setPageSize(Number(event.target.value))}
            >
              {BATCH_RETRY_PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
          <button
            data-testid="batch-retry-search-button"
            type="button"
            onClick={() => void loadTargets()}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white shadow-btn-shadow"
          >
            <Search size={16} /> 조회
          </button>
          <button
            data-testid="batch-retry-export-button"
            type="button"
            onClick={() => window.alert(batchRetryApi.excelDownloadOq)}
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-ld px-4 py-2 text-sm font-semibold text-dark"
          >
            <Download size={16} /> 엑셀다운로드
          </button>
        </div>
      </section>

      {state.status === "loading" ? (
        <LoadingState title="재처리 대상 조회 중" />
      ) : null}
      {state.status === "empty" ? (
        <EmptyState
          title="재처리 대상 없음"
          message="FAILED 상태의 실패 대상만 표시됩니다."
        />
      ) : null}
      {state.status === "error" ? (
        <ErrorState title="배치 재처리 오류" message={state.message} />
      ) : null}
      {state.status === "permission" ? (
        <PermissionState
          title="권한 없음"
          message="배치 재처리는 R09 운영자만 수행할 수 있습니다."
        />
      ) : null}
      {state.status === "success" ? (
        <SuccessState title="처리 완료" message={state.message} />
      ) : null}
      {state.status === "progress" ? (
        <LoadingState title="배치 재처리 진행 중" message={state.message} />
      ) : null}

      <section className="overflow-hidden rounded-md bg-white shadow-md dark:bg-white/5">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-ld text-sm dark:divide-white/10">
            <thead className="bg-lightgray text-left text-xs font-bold uppercase text-muted dark:bg-white/5">
              <tr>
                <th className="px-4 py-3">선택</th>
                <th className="px-4 py-3">원실행ID</th>
                <th className="px-4 py-3">batchId</th>
                <th className="px-4 py-3">상태</th>
                <th className="px-4 py-3">실패 건</th>
                <th className="px-4 py-3">실패 사유</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld dark:divide-white/10">
              {state.targets.map((target) => {
                const selectedKey = `${selected?.originalExecutionId ?? ""}:${selected?.failedItemKey ?? ""}`;
                const rowKey = `${target.originalExecutionId}:${target.failedItemKey ?? ""}`;
                return (
                  <tr
                    key={rowKey}
                    data-testid="batch-retry-target-row"
                    className="hover:bg-lightprimary/50"
                  >
                    <td className="px-4 py-3">
                      <button
                        data-testid="batch-retry-select-button"
                        type="button"
                        onClick={() => setSelected(target)}
                        className="rounded-lg border border-primary px-3 py-1 text-xs font-semibold text-primary hover:bg-primary hover:text-white"
                      >
                        {selectedKey === rowKey ? "선택됨" : "선택"}
                      </button>
                    </td>
                    <td className="px-4 py-3 font-semibold text-dark dark:text-white">
                      {target.originalExecutionId}
                    </td>
                    <td className="px-4 py-3">{target.batchId}</td>
                    <td className="px-4 py-3">{target.executionStatus}</td>
                    <td className="px-4 py-3">
                      {target.failedItemKey ?? "전체 실패 배치"}
                    </td>
                    <td className="px-4 py-3">{target.failureReason ?? "-"}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>

      <section
        className="rounded-md bg-white p-5 shadow-md dark:bg-white/5"
        aria-label="재처리 실행"
      >
        <div className="grid gap-4 lg:grid-cols-[1fr_2fr_auto]">
          <div className="rounded-xl bg-lightgray p-4 text-sm dark:bg-white/5">
            <p className="font-semibold text-dark dark:text-white">선택 대상</p>
            <p className="mt-2 text-muted dark:text-white/60">
              {selected
                ? selected.originalExecutionId
                : "선택된 실패 대상이 없습니다."}
            </p>
            {fieldErrors.target ? (
              <p className="mt-1 text-xs text-error">{fieldErrors.target}</p>
            ) : null}
          </div>
          <label className="text-sm font-semibold text-link dark:text-white/80">
            재처리 사유 <span className="text-error">*</span>
            <input
              data-testid="batch-retry-reason-input"
              className="mt-2 w-full rounded-xl border border-ld px-3 py-2"
              value={retryReason}
              onChange={(event) => setRetryReason(event.target.value)}
              aria-invalid={Boolean(fieldErrors.retryReason)}
            />
            {fieldErrors.retryReason ? (
              <span className="mt-1 block text-xs text-error">
                {fieldErrors.retryReason}
              </span>
            ) : null}
          </label>
          <button
            data-testid="batch-retry-submit-button"
            type="button"
            onClick={() => void handleRetry()}
            disabled={processing}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white shadow-btn-shadow disabled:cursor-not-allowed disabled:opacity-60"
          >
            <RefreshCw size={16} className={processing ? "animate-spin" : ""} />
            {processing ? "재처리 진행 중" : "재처리 실행"}
          </button>
        </div>
        {lastResult ? (
          <div
            data-testid="batch-retry-result-panel"
            className="mt-4 rounded-xl border border-success/30 bg-success/10 p-4 text-sm text-dark dark:text-white"
          >
            결과: retryExecutionId {lastResult.retryExecutionId} /
            originalExecutionId {lastResult.originalExecutionId} 연결 표시
          </div>
        ) : null}
      </section>
    </section>
  );
}

function TextInput({
  label,
  value,
  onChange,
  testId,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  testId: string;
}) {
  return (
    <label className="text-sm font-semibold text-link dark:text-white/80">
      {label}
      <input
        data-testid={testId}
        className="mt-2 w-full rounded-xl border border-ld px-3 py-2"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}

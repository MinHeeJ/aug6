import type React from "react";
import { useEffect, useState } from "react";
import {
  Download,
  PauseCircle,
  PlayCircle,
  RefreshCw,
  RotateCw,
  Search,
} from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type BatchExecutionRow = {
  executionId: string;
  batchId: string;
  batchType?: string | null;
  executionStatus: "WAITING" | "RUNNING" | "STOPPED" | "COMPLETED" | "FAILED";
  processType: "MANUAL_RUN" | "STOP" | "RERUN";
  reason: string;
  operatorUserId: number;
  operatorName?: string | null;
  originalExecutionId?: string | null;
  requestId?: string | null;
  createdAt?: string;
  updatedAt?: string;
  parameters?: Record<string, unknown>;
};

type BatchExecutionSearchResponse = {
  executions: BatchExecutionRow[];
  page: number;
  size: number;
  totalElements: number;
};

type BatchExecutionForm = {
  batchId: string;
  executionId: string;
  parametersText: string;
  reason: string;
};

type BatchExecutionPayload = {
  batchId?: string;
  parameters: Record<string, unknown>;
  reason: string;
};

type BatchExecutionListParams = {
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
  | "success"
  | "progress";

type BatchExecutionState = {
  status: ScreenStatus;
  executions: BatchExecutionRow[];
  message?: string;
};

type BatchExecutionAction =
  | { type: "loading" }
  | { type: "loaded"; executions: BatchExecutionRow[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "progress"; message: string }
  | { type: "success"; message: string };

export function getBatchExecutionRouteContract() {
  return {
    route: "/admin/batch-executions",
    screenId: "SCR-BATCH-EXECUTION-MGMT",
    operations: [
      "listBatchExecutions",
      "createBatchExecution",
      "updateBatchExecutionStatus",
      "createBatchRerun",
    ],
  } as const;
}

export function createEmptyBatchExecutionState(): BatchExecutionState {
  return { status: "idle", executions: [] };
}

export function reduceBatchExecutionState(
  state: BatchExecutionState,
  action: BatchExecutionAction,
): BatchExecutionState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.executions.length === 0 ? "empty" : "loaded",
        executions: action.executions,
        message: undefined,
      };
    case "error":
      return { ...state, status: "error", message: action.message };
    case "permission":
      return { ...state, status: "permission", message: "권한 없음" };
    case "progress":
      return { ...state, status: "progress", message: action.message };
    case "success":
      return { ...state, status: "success", message: action.message };
    default:
      return state;
  }
}

const emptyForm: BatchExecutionForm = {
  batchId: "",
  executionId: "",
  parametersText: "{}",
  reason: "",
};

export const BATCH_EXECUTION_PAGE_SIZE_OPTIONS = [20, 50, 100] as const;
const EXECUTION_STATUS_OPTIONS = [
  "",
  "WAITING",
  "RUNNING",
  "STOPPED",
  "COMPLETED",
  "FAILED",
] as const;

export const batchExecutionApi = {
  uiMessages: {
    runConfirm(batchId: string) {
      return `${batchId.trim()} 배치를 수동실행하시겠습니까?`;
    },
    stopConfirm(executionId: string) {
      return `${executionId.trim()} 실행을 중지하시겠습니까?`;
    },
    rerunConfirm(executionId: string) {
      return `${executionId.trim()} 실행을 재실행하시겠습니까?`;
    },
    runSuccess: "배치 수동실행이 요청되었습니다.",
    stopSuccess: "배치 실행이 중지되었습니다.",
    rerunSuccess: "배치 재실행이 요청되었습니다.",
    progress: "10초 이상 소요될 수 있어 진행상황을 표시합니다.",
    error: "배치 실행 정보를 처리하지 못했습니다.",
  },
  excelDownloadOq:
    "REQ-386 OQ: 기존 공통 export 패턴이 없어 엑셀 다운로드는 reviewer 확인 후 연결합니다.",
  pageSizeOptions: BATCH_EXECUTION_PAGE_SIZE_OPTIONS,
  validateForm(form: BatchExecutionForm, mode: "run" | "stop" | "rerun") {
    const next: Record<string, string> = {};
    if (mode === "run" && !form.batchId.trim())
      next.batchId = "배치ID는 필수입니다.";
    if ((mode === "stop" || mode === "rerun") && !form.executionId.trim())
      next.executionId = "실행ID를 선택하세요.";
    if (!form.reason.trim()) next.reason = "사유는 필수입니다.";
    try {
      JSON.parse(form.parametersText.trim() || "{}");
    } catch {
      next.parametersText = "실행 파라미터는 올바른 JSON이어야 합니다.";
    }
    return next;
  },
  paths: {
    list(params: BatchExecutionListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      if (params.batchId?.trim()) query.set("batchId", params.batchId.trim());
      if (params.executionStatus?.trim())
        query.set("executionStatus", params.executionStatus.trim());
      return `/api/admin/batch-executions?${query.toString()}` as `/api/${string}`;
    },
    create() {
      return "/api/admin/batch-executions" as const;
    },
    status(executionId: string) {
      return `/api/admin/batch-executions/${encodeURIComponent(executionId)}/status` as `/api/${string}`;
    },
    rerun(executionId: string) {
      return `/api/admin/batch-executions/${encodeURIComponent(executionId)}/rerun` as `/api/${string}`;
    },
  },
  toPayload(
    form: BatchExecutionForm,
    includeBatchId: boolean,
  ): BatchExecutionPayload {
    const parsedParameters = form.parametersText.trim()
      ? (JSON.parse(form.parametersText) as Record<string, unknown>)
      : {};
    return {
      ...(includeBatchId ? { batchId: form.batchId.trim() } : {}),
      parameters: parsedParameters,
      reason: form.reason.trim(),
    };
  },
  list(params: BatchExecutionListParams = {}) {
    return apiRequest<BatchExecutionSearchResponse>(
      batchExecutionApi.paths.list(params),
    );
  },
  create(payload: BatchExecutionPayload) {
    return apiRequest<BatchExecutionRow>(batchExecutionApi.paths.create(), {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  stop(executionId: string, reason: string) {
    return apiRequest<BatchExecutionRow>(
      batchExecutionApi.paths.status(executionId),
      {
        method: "PATCH",
        body: JSON.stringify({ targetStatus: "STOPPED", reason }),
      },
    );
  },
  rerun(executionId: string, payload: BatchExecutionPayload) {
    return apiRequest<BatchExecutionRow>(
      batchExecutionApi.paths.rerun(executionId),
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export function BatchExecutionManagementPage() {
  const [batchIdFilter, setBatchIdFilter] = useState("");
  const [executionStatus, setExecutionStatus] = useState("");
  const [pageSize, setPageSize] = useState(20);
  const [state, setState] = useState<BatchExecutionState>(
    createEmptyBatchExecutionState(),
  );
  const [selected, setSelected] = useState<BatchExecutionRow | null>(null);
  const [form, setForm] = useState<BatchExecutionForm>(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadExecutions = async () => {
    setState((current) =>
      reduceBatchExecutionState(current, { type: "loading" }),
    );
    setFieldErrors({});
    try {
      const response = await batchExecutionApi.list({
        batchId: batchIdFilter,
        executionStatus,
        page: 0,
        size: pageSize,
      });
      const executions = response.data?.executions ?? [];
      setState((current) =>
        reduceBatchExecutionState(current, { type: "loaded", executions }),
      );
      if (selected) {
        const refreshed =
          executions.find((row) => row.executionId === selected.executionId) ??
          null;
        if (refreshed) applySelected(refreshed);
      }
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadExecutions();
  }, []);

  const applySelected = (row: BatchExecutionRow) => {
    setSelected(row);
    setForm({
      batchId: row.batchId,
      executionId: row.executionId,
      parametersText: JSON.stringify(row.parameters ?? {}, null, 2),
      reason: "",
    });
    setFieldErrors({});
  };

  const validateLocal = (mode: "run" | "stop" | "rerun") => {
    const next = batchExecutionApi.validateForm(form, mode);
    setFieldErrors(next);
    return Object.keys(next).length === 0;
  };

  const runExecution = async () => {
    if (!validateLocal("run")) return;
    if (!window.confirm(batchExecutionApi.uiMessages.runConfirm(form.batchId)))
      return;
    await executeWithProgress(
      async () =>
        batchExecutionApi.create(batchExecutionApi.toPayload(form, true)),
      batchExecutionApi.uiMessages.runSuccess,
    );
  };

  const stopExecution = async () => {
    if (!validateLocal("stop")) return;
    if (
      !window.confirm(
        batchExecutionApi.uiMessages.stopConfirm(form.executionId),
      )
    )
      return;
    await executeWithProgress(
      async () => batchExecutionApi.stop(form.executionId, form.reason.trim()),
      batchExecutionApi.uiMessages.stopSuccess,
    );
  };

  const rerunExecution = async () => {
    if (!validateLocal("rerun")) return;
    if (
      !window.confirm(
        batchExecutionApi.uiMessages.rerunConfirm(form.executionId),
      )
    )
      return;
    await executeWithProgress(
      async () =>
        batchExecutionApi.rerun(
          form.executionId,
          batchExecutionApi.toPayload(form, false),
        ),
      batchExecutionApi.uiMessages.rerunSuccess,
    );
  };

  const executeWithProgress = async (
    action: () => Promise<{ data?: BatchExecutionRow }>,
    successMessage: string,
  ) => {
    const progressTimer = window.setTimeout(() => {
      setState((current) =>
        reduceBatchExecutionState(current, {
          type: "progress",
          message: batchExecutionApi.uiMessages.progress,
        }),
      );
    }, 10000);
    try {
      setFieldErrors({});
      const response = await action();
      if (response.data) applySelected(response.data);
      setState((current) =>
        reduceBatchExecutionState(current, {
          type: "success",
          message: successMessage,
        }),
      );
      window.alert(successMessage);
      await loadExecutions();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      window.clearTimeout(progressTimer);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceBatchExecutionState(current, { type: "permission" }),
      );
      return;
    }
    if (caught instanceof ApiClientError && caught.apiError?.fields) {
      setFieldErrors(
        Object.fromEntries(
          caught.apiError.fields.map((field) => [field.field, field.message]),
        ),
      );
    }
    const message =
      caught instanceof ApiClientError
        ? caught.message
        : batchExecutionApi.uiMessages.error;
    setState((current) =>
      reduceBatchExecutionState(current, { type: "error", message }),
    );
  };

  if (state.status === "permission") {
    return (
      <PermissionState
        title="배치 실행 관리 권한이 없습니다"
        message="R09 시스템관리자 권한 또는 메뉴 접근권한을 확인하세요."
      />
    );
  }

  return (
    <section
      data-testid="batch-execution-management-screen"
      data-screen-id="SCR-BATCH-EXECUTION-MGMT"
      className="space-y-6"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-muted">
          시스템 운영 관리 &gt; 배치작업 관리 &gt; 배치 실행 관리
        </p>
        <div className="mt-2 flex items-center gap-3">
          <PlayCircle className="h-6 w-6 text-primary" aria-hidden />
          <div>
            <h1 className="text-xl font-semibold text-dark">배치 실행 관리</h1>
            <p className="mt-1 text-sm text-muted">
              배치를 수동실행, 중지, 재실행하고 사유와 운영자를 기록합니다.
            </p>
          </div>
        </div>
      </div>

      {state.status === "loading" && <LoadingState title="배치 실행 조회 중" />}
      {state.status === "empty" && (
        <EmptyState title="조회된 배치 실행이 없습니다" />
      )}
      {state.status === "error" && (
        <ErrorState
          title="배치 실행 처리 오류"
          message={state.message ?? "오류가 발생했습니다."}
        />
      )}
      {state.status === "progress" && (
        <LoadingState title="배치 실행 처리 중" message={state.message} />
      )}
      {state.status === "success" && (
        <SuccessState title="처리 완료" message={state.message} />
      )}

      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <div className="grid gap-4 lg:grid-cols-4">
          <label className="text-sm font-medium text-dark">
            실행상태
            <select
              data-testid="batch-execution-status-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={executionStatus}
              onChange={(event) => setExecutionStatus(event.target.value)}
            >
              {EXECUTION_STATUS_OPTIONS.map((status) => (
                <option key={status || "ALL"} value={status}>
                  {status || "전체"}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            batchId
            <input
              data-testid="batch-execution-batch-id-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={batchIdFilter}
              onChange={(event) => setBatchIdFilter(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시건수
            <select
              data-testid="batch-execution-page-size-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={pageSize}
              onChange={(event) => setPageSize(Number(event.target.value))}
            >
              {BATCH_EXECUTION_PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
          <div className="flex items-end gap-2">
            <button
              data-testid="batch-execution-search-button"
              type="button"
              className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              onClick={() => void loadExecutions()}
            >
              <Search size={16} /> 조회
            </button>
            <button
              data-testid="batch-execution-export-button"
              type="button"
              className="inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
              onClick={() => window.alert(batchExecutionApi.excelDownloadOq)}
            >
              <Download size={16} /> 엑셀다운로드
            </button>
          </div>
        </div>
      </section>

      <section className="overflow-hidden rounded-md border border-ld bg-white shadow-md">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-ld text-sm">
            <thead className="bg-lightgray text-left text-xs font-semibold uppercase text-muted">
              <tr>
                <th className="px-4 py-3">executionId</th>
                <th className="px-4 py-3">batchId</th>
                <th className="px-4 py-3">현재상태</th>
                <th className="px-4 py-3">마지막실행</th>
                <th className="px-4 py-3">담당자</th>
                <th className="px-4 py-3">작업</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {state.executions.map((row) => (
                <tr key={row.executionId} data-testid="batch-execution-row">
                  <td className="px-4 py-3 font-semibold text-dark">
                    {row.executionId}
                  </td>
                  <td className="px-4 py-3">{row.batchId}</td>
                  <td className="px-4 py-3">{row.executionStatus}</td>
                  <td className="px-4 py-3">
                    {row.updatedAt ?? row.createdAt ?? "-"}
                  </td>
                  <td className="px-4 py-3">
                    {row.operatorName ?? row.operatorUserId}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      data-testid="batch-execution-select-button"
                      type="button"
                      className="rounded-md border border-ld px-3 py-1 text-xs font-semibold"
                      onClick={() => applySelected(row)}
                    >
                      선택
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <h2 className="text-lg font-semibold text-dark">선택 실행 처리</h2>
        <div className="mt-4 grid gap-4 lg:grid-cols-4">
          <Field label="선택 배치ID*" error={fieldErrors.batchId}>
            <input
              data-testid="batch-execution-batch-id-input"
              className="mt-2 w-full rounded-md border border-ld bg-lightgray px-3 py-2"
              value={form.batchId}
              onChange={(event) =>
                setForm({ ...form, batchId: event.target.value })
              }
            />
          </Field>
          <Field label="선택 실행ID" error={fieldErrors.executionId}>
            <input
              data-testid="batch-execution-execution-id-input"
              className="mt-2 w-full rounded-md border border-ld bg-lightgray px-3 py-2"
              value={form.executionId}
              readOnly
            />
          </Field>
          <Field label="사유*" error={fieldErrors.reason}>
            <input
              data-testid="batch-execution-reason-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={form.reason}
              onChange={(event) =>
                setForm({ ...form, reason: event.target.value })
              }
            />
          </Field>
          <label className="text-sm font-medium text-dark lg:col-span-4">
            실행 파라미터 JSON
            <textarea
              data-testid="batch-execution-parameters-textarea"
              className="mt-2 min-h-28 w-full rounded-md border border-ld px-3 py-2 font-mono text-sm"
              value={form.parametersText}
              onChange={(event) =>
                setForm({ ...form, parametersText: event.target.value })
              }
            />
            {fieldErrors.parametersText && (
              <p className="mt-1 text-xs text-error">
                {fieldErrors.parametersText}
              </p>
            )}
          </label>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            data-testid="batch-execution-run-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void runExecution()}
          >
            <PlayCircle size={16} /> 실행 확인
          </button>
          <button
            data-testid="batch-execution-stop-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md border border-warning px-4 py-2 text-sm font-semibold text-warning"
            onClick={() => void stopExecution()}
            disabled={selected?.executionStatus !== "RUNNING"}
          >
            <PauseCircle size={16} /> 중지 확인
          </button>
          <button
            data-testid="batch-execution-rerun-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
            onClick={() => void rerunExecution()}
            disabled={!selected}
          >
            <RotateCw size={16} /> 재실행 확인
          </button>
          <button
            data-testid="batch-execution-reset-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
            onClick={() => {
              setSelected(null);
              setForm(emptyForm);
              setFieldErrors({});
            }}
          >
            <RefreshCw size={16} /> 취소
          </button>
        </div>
      </section>
    </section>
  );
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
    <label className="text-sm font-medium text-dark">
      {label}
      {children}
      {error && <p className="mt-1 text-xs text-error">{error}</p>}
    </label>
  );
}

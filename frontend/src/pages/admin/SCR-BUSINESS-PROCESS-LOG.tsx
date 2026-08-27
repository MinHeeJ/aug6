import type React from "react";
import { useEffect, useMemo, useState } from "react";
import { RefreshCw, Search } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
} from "../../components/States";

export type BusinessProcessLogRow = {
  auditLogId: number;
  actionType: BusinessActionType;
  targetKey: string;
  beforeState?: string | null;
  afterState?: string | null;
  actorUserId: number;
  actorLoginId?: string | null;
  actorName?: string | null;
  resultStatus: BusinessResultStatus;
  requestId: string;
  createdAt: string;
};

type BusinessProcessLogSearchResponse = {
  logs: BusinessProcessLogRow[];
  page: number;
  size: number;
  totalElements: number;
};

type BusinessProcessLogListParams = {
  filter?: string;
  actionType?: string;
  targetKey?: string;
  actorUserId?: string;
  resultStatus?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
};

type ScreenStatus =
  | "idle"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission";
type BusinessActionType =
  | "CREATE"
  | "UPDATE"
  | "DELETE"
  | "CONFIRM"
  | "AUTH"
  | "APPROVE"
  | "CANCEL"
  | "BATCH"
  | "SESSION_TERMINATE";
type BusinessResultStatus = "SUCCESS" | "FAILURE";

type BusinessProcessLogState = {
  status: ScreenStatus;
  logs: BusinessProcessLogRow[];
  message?: string;
};

type BusinessProcessLogAction =
  | { type: "loading" }
  | { type: "loaded"; logs: BusinessProcessLogRow[] }
  | { type: "error"; message: string }
  | { type: "permission" };

export const BUSINESS_PROCESS_LOG_PAGE_SIZE_OPTIONS = [20, 50, 100] as const;
export const BUSINESS_ACTION_TYPE_OPTIONS = [
  "",
  "CREATE",
  "UPDATE",
  "DELETE",
  "CONFIRM",
  "AUTH",
  "APPROVE",
  "CANCEL",
  "BATCH",
  "SESSION_TERMINATE",
] as const;
export const BUSINESS_RESULT_STATUS_OPTIONS = [
  "",
  "SUCCESS",
  "FAILURE",
] as const;

export function getBusinessProcessLogRouteContract() {
  return {
    route: "/admin/audit/business-process-logs",
    screenId: "SCR-BUSINESS-PROCESS-LOG",
    operations: ["listBusinessProcessLogs"],
  } as const;
}

export function createEmptyBusinessProcessLogState(): BusinessProcessLogState {
  return { status: "idle", logs: [] };
}

export function reduceBusinessProcessLogState(
  state: BusinessProcessLogState,
  action: BusinessProcessLogAction,
): BusinessProcessLogState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.logs.length === 0 ? "empty" : "loaded",
        logs: action.logs,
        message: undefined,
      };
    case "error":
      return { ...state, status: "error", message: action.message };
    case "permission":
      return { ...state, status: "permission", message: "권한 없음" };
    default:
      return state;
  }
}

export const businessProcessLogApi = {
  pageSizeOptions: BUSINESS_PROCESS_LOG_PAGE_SIZE_OPTIONS,
  actionTypeOptions: BUSINESS_ACTION_TYPE_OPTIONS,
  resultStatusOptions: BUSINESS_RESULT_STATUS_OPTIONS,
  uiMessages: {
    error: "업무처리 로그를 조회하지 못했습니다.",
    permission: "R09 시스템관리자만 업무처리 로그를 조회할 수 있습니다.",
  },
  paths: {
    list(params: BusinessProcessLogListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      if (params.filter?.trim()) query.set("filter", params.filter.trim());
      if (params.actionType?.trim())
        query.set("actionType", params.actionType.trim());
      if (params.targetKey?.trim())
        query.set("targetKey", params.targetKey.trim());
      if (params.actorUserId?.trim())
        query.set("actorUserId", params.actorUserId.trim());
      if (params.resultStatus?.trim())
        query.set("resultStatus", params.resultStatus.trim());
      if (params.fromDate?.trim())
        query.set("fromDate", params.fromDate.trim());
      if (params.toDate?.trim()) query.set("toDate", params.toDate.trim());
      return `/api/admin/audit/business-process-logs?${query.toString()}` as `/api/${string}`;
    },
  },
  list(params: BusinessProcessLogListParams = {}) {
    return apiRequest<BusinessProcessLogSearchResponse>(
      businessProcessLogApi.paths.list(params),
    );
  },
};

export function BusinessProcessLogPage() {
  const [state, setState] = useState<BusinessProcessLogState>(
    createEmptyBusinessProcessLogState(),
  );
  const [filter, setFilter] = useState("");
  const [actionType, setActionType] = useState("");
  const [targetKey, setTargetKey] = useState("");
  const [actorUserId, setActorUserId] = useState("");
  const [resultStatus, setResultStatus] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [pageSize, setPageSize] =
    useState<(typeof BUSINESS_PROCESS_LOG_PAGE_SIZE_OPTIONS)[number]>(20);

  const rows = useMemo(() => state.logs, [state.logs]);

  const load = async () => {
    setState((current) =>
      reduceBusinessProcessLogState(current, { type: "loading" }),
    );
    try {
      const response = await businessProcessLogApi.list({
        filter,
        actionType,
        targetKey,
        actorUserId,
        resultStatus,
        fromDate,
        toDate,
        size: pageSize,
      });
      setState((current) =>
        reduceBusinessProcessLogState(current, {
          type: "loaded",
          logs: response.data?.logs ?? [],
        }),
      );
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 403) {
        setState((current) =>
          reduceBusinessProcessLogState(current, { type: "permission" }),
        );
      } else {
        setState((current) =>
          reduceBusinessProcessLogState(current, {
            type: "error",
            message:
              caught instanceof Error
                ? caught.message
                : businessProcessLogApi.uiMessages.error,
          }),
        );
      }
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const submitSearch = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void load();
  };

  return (
    <section
      data-testid="business-process-log-screen"
      data-screen-id="SCR-BUSINESS-PROCESS-LOG"
      className="space-y-6"
    >
      <header className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm font-semibold text-primary">
          보안·감사 관리 &gt; 감사로그 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">업무처리 로그</h1>
        <p className="mt-2 text-sm text-muted">
          등록·수정·삭제·확인·인증·승인·취소·일괄처리 로그의 전후상태와
          처리결과를 조회합니다. 원업무 재실행·취소·로그삭제 CTA는 제공하지
          않습니다.
        </p>
      </header>

      <form
        data-testid="business-process-log-search-form"
        onSubmit={submitSearch}
        className="rounded-md border border-ld bg-white p-5 shadow-md"
      >
        <div className="grid gap-4 xl:grid-cols-[auto_1fr_1fr_auto_auto_auto_auto_auto] xl:items-end">
          <label className="text-sm font-medium text-dark">
            행위유형
            <select
              data-testid="business-process-action-type-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={actionType}
              onChange={(event) => setActionType(event.target.value)}
            >
              <option value="">전체</option>
              {BUSINESS_ACTION_TYPE_OPTIONS.filter(Boolean).map((value) => (
                <option key={value} value={value}>
                  {formatActionType(value as BusinessActionType)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            대상키
            <input
              data-testid="business-process-target-key-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={targetKey}
              onChange={(event) => setTargetKey(event.target.value)}
              placeholder="업무 대상키"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            처리자 검색
            <input
              data-testid="business-process-filter-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              placeholder="처리자, 요청ID"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            처리자 ID
            <input
              data-testid="business-process-actor-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              inputMode="numeric"
              value={actorUserId}
              onChange={(event) =>
                setActorUserId(event.target.value.replace(/[^0-9]/g, ""))
              }
              placeholder="user_id"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            기간 시작
            <input
              data-testid="business-process-from-date-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="date"
              value={fromDate}
              onChange={(event) => setFromDate(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            기간 종료
            <input
              data-testid="business-process-to-date-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="date"
              value={toDate}
              onChange={(event) => setToDate(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              data-testid="business-process-size-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={pageSize}
              onChange={(event) =>
                setPageSize(
                  Number(
                    event.target.value,
                  ) as (typeof BUSINESS_PROCESS_LOG_PAGE_SIZE_OPTIONS)[number],
                )
              }
            >
              {BUSINESS_PROCESS_LOG_PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            결과
            <select
              data-testid="business-process-result-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={resultStatus}
              onChange={(event) => setResultStatus(event.target.value)}
            >
              <option value="">전체</option>
              <option value="SUCCESS">성공</option>
              <option value="FAILURE">실패</option>
            </select>
          </label>
          <button
            data-testid="business-process-search-button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            type="submit"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </form>

      {state.status === "loading" && (
        <LoadingState
          title="업무처리 로그 조회 중"
          message="업무처리 감사로그를 불러오고 있습니다."
        />
      )}
      {state.status === "permission" && (
        <PermissionState
          title="권한이 없습니다"
          message={businessProcessLogApi.uiMessages.permission}
        />
      )}
      {state.status === "error" && (
        <ErrorState
          title="업무처리 로그 오류"
          message={state.message ?? businessProcessLogApi.uiMessages.error}
        />
      )}
      {state.status === "empty" && (
        <EmptyState
          title="업무처리 로그 없음"
          message="조건에 맞는 업무처리 로그가 없습니다."
        />
      )}

      <section className="overflow-hidden rounded-md border border-ld bg-white shadow-md">
        <div className="flex items-center justify-between border-b border-ld px-5 py-4">
          <h2 className="text-lg font-semibold text-dark">
            업무처리 로그 목록
          </h2>
          <button
            data-testid="business-process-refresh-button"
            className="inline-flex items-center gap-2 rounded-md border border-ld px-3 py-2 text-sm text-dark"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={15} /> 새로고침
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-ld text-sm">
            <thead className="bg-lightgray text-left text-muted">
              <tr>
                <th className="px-4 py-3">행위유형</th>
                <th className="px-4 py-3">대상키</th>
                <th className="px-4 py-3">처리 전 상태</th>
                <th className="px-4 py-3">처리 후 상태</th>
                <th className="px-4 py-3">처리자</th>
                <th className="px-4 py-3">일시</th>
                <th className="px-4 py-3">결과</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr key={row.auditLogId} data-testid="business-process-log-row">
                  <td className="px-4 py-3">
                    <span className="rounded-full bg-lightprimary px-2 py-1 text-xs font-semibold text-primary">
                      {formatActionType(row.actionType)}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-mono text-xs">
                    {row.targetKey}
                    <br />
                    <span className="text-muted">{row.requestId}</span>
                  </td>
                  <td className="max-w-xs px-4 py-3">
                    <code className="break-words text-xs text-muted">
                      {compactJson(row.beforeState)}
                    </code>
                  </td>
                  <td className="max-w-xs px-4 py-3">
                    <code className="break-words text-xs text-muted">
                      {compactJson(row.afterState)}
                    </code>
                  </td>
                  <td className="px-4 py-3">
                    <strong className="text-dark">
                      {row.actorName ?? row.actorLoginId ?? row.actorUserId}
                    </strong>
                    <br />
                    <span className="text-xs text-muted">
                      {row.actorLoginId ?? `user:${row.actorUserId}`}
                    </span>
                  </td>
                  <td className="px-4 py-3">{formatDateTime(row.createdAt)}</td>
                  <td className="px-4 py-3">
                    <span
                      className={
                        row.resultStatus === "SUCCESS"
                          ? "text-success"
                          : "text-error"
                      }
                    >
                      {formatResult(row.resultStatus)}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function formatActionType(value: BusinessActionType) {
  const labels = {
    CREATE: "등록",
    UPDATE: "수정",
    DELETE: "삭제",
    CONFIRM: "확인",
    AUTH: "인증",
    APPROVE: "승인",
    CANCEL: "취소",
    BATCH: "일괄처리",
    SESSION_TERMINATE: "세션강제종료",
  } satisfies Record<BusinessActionType, string>;
  return labels[value];
}

function formatResult(value: BusinessResultStatus) {
  return value === "SUCCESS" ? "성공" : "실패";
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
}

function compactJson(value?: string | null) {
  if (!value) return "-";
  try {
    return JSON.stringify(JSON.parse(value));
  } catch {
    return value;
  }
}

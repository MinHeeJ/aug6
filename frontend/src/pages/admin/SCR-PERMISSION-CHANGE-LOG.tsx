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

export type PermissionChangeLogRow = {
  permissionHistoryId: number;
  targetType: PermissionChangeTargetType;
  targetId: string;
  beforeValue?: string | null;
  afterValue?: string | null;
  approverUserId?: number | null;
  approverLoginId?: string | null;
  approverName?: string | null;
  changedBy: number;
  changerLoginId?: string | null;
  changerName?: string | null;
  reason: string;
  changedAt: string;
};

type PermissionChangeLogSearchResponse = {
  logs: PermissionChangeLogRow[];
  page: number;
  size: number;
  totalElements: number;
};

type PermissionChangeLogListParams = {
  targetType?: string;
  targetId?: string;
  approverUserId?: string;
  changedBy?: string;
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
type PermissionChangeTargetType =
  | "ROLE"
  | "MENU"
  | "FUNCTION"
  | "DATA_SCOPE"
  | "TEMPORARY";

type PermissionChangeLogState = {
  status: ScreenStatus;
  logs: PermissionChangeLogRow[];
  message?: string;
};

type PermissionChangeLogAction =
  | { type: "loading" }
  | { type: "loaded"; logs: PermissionChangeLogRow[] }
  | { type: "error"; message: string }
  | { type: "permission" };

export const PERMISSION_CHANGE_LOG_PAGE_SIZE_OPTIONS = [20, 50, 100] as const;
export const PERMISSION_CHANGE_TARGET_TYPE_OPTIONS = [
  "",
  "ROLE",
  "MENU",
  "FUNCTION",
  "DATA_SCOPE",
  "TEMPORARY",
] as const;

export function getPermissionChangeLogRouteContract() {
  return {
    route: "/admin/audit/permission-change-logs",
    screenId: "SCR-PERMISSION-CHANGE-LOG",
    operations: ["listPermissionChangeLogs"],
  } as const;
}

export function createEmptyPermissionChangeLogState(): PermissionChangeLogState {
  return { status: "idle", logs: [] };
}

export function reducePermissionChangeLogState(
  state: PermissionChangeLogState,
  action: PermissionChangeLogAction,
): PermissionChangeLogState {
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

export const permissionChangeLogApi = {
  pageSizeOptions: PERMISSION_CHANGE_LOG_PAGE_SIZE_OPTIONS,
  targetTypeOptions: PERMISSION_CHANGE_TARGET_TYPE_OPTIONS,
  uiMessages: {
    error: "권한변경 로그를 조회하지 못했습니다.",
    permission: "R09 시스템관리자만 권한변경 로그를 조회할 수 있습니다.",
  },
  paths: {
    list(params: PermissionChangeLogListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      if (params.targetType?.trim())
        query.set("targetType", params.targetType.trim());
      if (params.targetId?.trim())
        query.set("targetId", params.targetId.trim());
      if (params.approverUserId?.trim())
        query.set("approverUserId", params.approverUserId.trim());
      if (params.changedBy?.trim())
        query.set("changedBy", params.changedBy.trim());
      if (params.fromDate?.trim())
        query.set("fromDate", params.fromDate.trim());
      if (params.toDate?.trim()) query.set("toDate", params.toDate.trim());
      return `/api/admin/audit/permission-change-logs?${query.toString()}` as `/api/${string}`;
    },
  },
  list(params: PermissionChangeLogListParams = {}) {
    return apiRequest<PermissionChangeLogSearchResponse>(
      permissionChangeLogApi.paths.list(params),
    );
  },
};

export function PermissionChangeLogPage() {
  const [state, setState] = useState<PermissionChangeLogState>(
    createEmptyPermissionChangeLogState(),
  );
  const [targetType, setTargetType] = useState("");
  const [targetId, setTargetId] = useState("");
  const [approverUserId, setApproverUserId] = useState("");
  const [changedBy, setChangedBy] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [pageSize, setPageSize] =
    useState<(typeof PERMISSION_CHANGE_LOG_PAGE_SIZE_OPTIONS)[number]>(20);

  const rows = useMemo(() => state.logs, [state.logs]);

  const load = async () => {
    setState((current) =>
      reducePermissionChangeLogState(current, { type: "loading" }),
    );
    try {
      const response = await permissionChangeLogApi.list({
        targetType,
        targetId,
        approverUserId,
        changedBy,
        fromDate,
        toDate,
        size: pageSize,
      });
      setState((current) =>
        reducePermissionChangeLogState(current, {
          type: "loaded",
          logs: response.data?.logs ?? [],
        }),
      );
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 403) {
        setState((current) =>
          reducePermissionChangeLogState(current, { type: "permission" }),
        );
      } else {
        setState((current) =>
          reducePermissionChangeLogState(current, {
            type: "error",
            message:
              caught instanceof Error
                ? caught.message
                : permissionChangeLogApi.uiMessages.error,
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
      data-testid="permission-change-log-screen"
      data-screen-id="SCR-PERMISSION-CHANGE-LOG"
      className="space-y-6"
    >
      <header className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm font-semibold text-primary">
          보안·감사 관리 &gt; 감사로그 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">권한변경 로그</h1>
        <p className="mt-2 text-sm text-muted">
          권한유형·변경대상·승인자·처리자·기간 조건으로 권한 변경 전후값과
          사유를 조회합니다. 권한 부여·변경·회수 CTA와 현재 권한 변경 기능은
          제공하지 않습니다.
        </p>
      </header>

      <form
        data-testid="permission-change-log-search-form"
        onSubmit={submitSearch}
        className="rounded-md border border-ld bg-white p-5 shadow-md"
      >
        <div className="grid gap-4 lg:grid-cols-[auto_1fr_auto_auto_auto_auto_auto] lg:items-end">
          <label className="text-sm font-medium text-dark">
            권한유형
            <select
              data-testid="permission-change-target-type-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={targetType}
              onChange={(event) => setTargetType(event.target.value)}
            >
              <option value="">전체</option>
              {PERMISSION_CHANGE_TARGET_TYPE_OPTIONS.filter(Boolean).map(
                (value) => (
                  <option key={value} value={value}>
                    {formatTargetType(value as PermissionChangeTargetType)}
                  </option>
                ),
              )}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            변경대상
            <input
              data-testid="permission-change-target-id-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={targetId}
              onChange={(event) => setTargetId(event.target.value)}
              placeholder="대상 식별자"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            승인자 ID
            <input
              data-testid="permission-change-approver-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              inputMode="numeric"
              value={approverUserId}
              onChange={(event) =>
                setApproverUserId(event.target.value.replace(/[^0-9]/g, ""))
              }
              placeholder="user_id"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            처리자 ID
            <input
              data-testid="permission-change-changed-by-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              inputMode="numeric"
              value={changedBy}
              onChange={(event) =>
                setChangedBy(event.target.value.replace(/[^0-9]/g, ""))
              }
              placeholder="user_id"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            기간 시작
            <input
              data-testid="permission-change-from-date-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="date"
              value={fromDate}
              onChange={(event) => setFromDate(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            기간 종료
            <input
              data-testid="permission-change-to-date-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="date"
              value={toDate}
              onChange={(event) => setToDate(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              data-testid="permission-change-size-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={pageSize}
              onChange={(event) =>
                setPageSize(
                  Number(
                    event.target.value,
                  ) as (typeof PERMISSION_CHANGE_LOG_PAGE_SIZE_OPTIONS)[number],
                )
              }
            >
              {PERMISSION_CHANGE_LOG_PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
          <button
            data-testid="permission-change-search-button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            type="submit"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </form>

      {state.status === "loading" && (
        <LoadingState
          title="권한변경 로그 조회 중"
          message="권한변경 감사로그를 불러오고 있습니다."
        />
      )}
      {state.status === "permission" && (
        <PermissionState
          title="권한이 없습니다"
          message={permissionChangeLogApi.uiMessages.permission}
        />
      )}
      {state.status === "error" && (
        <ErrorState
          title="권한변경 로그 오류"
          message={state.message ?? permissionChangeLogApi.uiMessages.error}
        />
      )}
      {state.status === "empty" && (
        <EmptyState
          title="권한변경 로그 없음"
          message="조건에 맞는 권한변경 로그가 없습니다."
        />
      )}

      <section className="overflow-hidden rounded-md border border-ld bg-white shadow-md">
        <div className="flex items-center justify-between border-b border-ld px-5 py-4">
          <h2 className="text-lg font-semibold text-dark">
            권한변경 로그 목록
          </h2>
          <button
            data-testid="permission-change-refresh-button"
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
                <th className="px-4 py-3">권한유형</th>
                <th className="px-4 py-3">변경대상</th>
                <th className="px-4 py-3">변경 전 값</th>
                <th className="px-4 py-3">변경 후 값</th>
                <th className="px-4 py-3">승인자</th>
                <th className="px-4 py-3">처리자</th>
                <th className="px-4 py-3">사유</th>
                <th className="px-4 py-3">일시</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr
                  key={row.permissionHistoryId}
                  data-testid="permission-change-log-row"
                >
                  <td className="px-4 py-3">
                    <span className="rounded-full bg-lightprimary px-2 py-1 text-xs font-semibold text-primary">
                      {formatTargetType(row.targetType)}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-dark">
                    {row.targetId}
                  </td>
                  <td className="max-w-xs px-4 py-3">
                    <pre className="max-h-28 overflow-auto whitespace-pre-wrap rounded-md bg-lightsecondary p-2 text-xs text-muted">
                      {row.beforeValue ?? "-"}
                    </pre>
                  </td>
                  <td className="max-w-xs px-4 py-3">
                    <pre className="max-h-28 overflow-auto whitespace-pre-wrap rounded-md bg-lightsecondary p-2 text-xs text-muted">
                      {row.afterValue ?? "-"}
                    </pre>
                  </td>
                  <td className="px-4 py-3">
                    {formatUser(
                      row.approverName,
                      row.approverLoginId,
                      row.approverUserId,
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {formatUser(
                      row.changerName,
                      row.changerLoginId,
                      row.changedBy,
                    )}
                  </td>
                  <td className="max-w-sm px-4 py-3 text-muted">
                    {row.reason}
                  </td>
                  <td className="px-4 py-3">{formatDateTime(row.changedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function formatTargetType(value: PermissionChangeTargetType) {
  const labels = {
    ROLE: "역할",
    MENU: "메뉴",
    FUNCTION: "기능",
    DATA_SCOPE: "데이터범위",
    TEMPORARY: "임시권한",
  } satisfies Record<PermissionChangeTargetType, string>;
  return labels[value];
}

function formatUser(
  name?: string | null,
  loginId?: string | null,
  userId?: number | null,
) {
  if (!userId && !name && !loginId) return "-";
  return (
    <span>
      <strong className="text-dark">{name ?? loginId ?? userId}</strong>
      <br />
      <span className="text-xs text-muted">{loginId ?? `user:${userId}`}</span>
    </span>
  );
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
}

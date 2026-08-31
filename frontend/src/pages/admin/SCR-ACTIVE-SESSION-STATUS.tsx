import type React from "react";
import { useEffect, useMemo, useState } from "react";
import { RefreshCw, Search, ShieldX } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type ActiveSessionRow = {
  sessionId: string;
  userId: number;
  loginId: string;
  employeeNo?: string | null;
  userName?: string | null;
  loginAt?: string | null;
  lastAccessedAt?: string | null;
  ipAddress?: string | null;
  status: "ACTIVE" | "EXPIRED" | "LOGGED_OUT" | "TERMINATED";
  terminatedBy?: number | null;
  terminatedAt?: string | null;
  terminationReason?: string | null;
};

type ActiveSessionSearchResponse = {
  sessions: ActiveSessionRow[];
  page: number;
  size: number;
  totalElements: number;
};

type ActiveSessionListParams = {
  filter?: string;
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

type ActiveSessionState = {
  status: ScreenStatus;
  sessions: ActiveSessionRow[];
  message?: string;
};

type ActiveSessionAction =
  | { type: "loading" }
  | { type: "loaded"; sessions: ActiveSessionRow[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string; sessions: ActiveSessionRow[] };

export const ACTIVE_SESSION_PAGE_SIZE_OPTIONS = [20, 50, 100] as const;

export function getActiveSessionRouteContract() {
  return {
    route: "/admin/security/active-sessions",
    screenId: "SCR-ACTIVE-SESSION-STATUS",
    operations: ["listActiveSessions", "terminateActiveSession"],
  } as const;
}

export function createEmptyActiveSessionState(): ActiveSessionState {
  return { status: "idle", sessions: [] };
}

export function reduceActiveSessionState(
  state: ActiveSessionState,
  action: ActiveSessionAction,
): ActiveSessionState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.sessions.length === 0 ? "empty" : "loaded",
        sessions: action.sessions,
        message: undefined,
      };
    case "error":
      return { ...state, status: "error", message: action.message };
    case "permission":
      return { ...state, status: "permission", message: "권한 없음" };
    case "success":
      return {
        ...state,
        status: "success",
        sessions: action.sessions,
        message: action.message,
      };
    default:
      return state;
  }
}

export const activeSessionApi = {
  pageSizeOptions: ACTIVE_SESSION_PAGE_SIZE_OPTIONS,
  uiMessages: {
    terminateConfirm(sessionId: string) {
      return `${sessionId.trim()} 세션을 강제종료하시겠습니까?`;
    },
    terminateSuccess: "세션 강제종료가 완료되었습니다.",
    error: "접속현황 정보를 처리하지 못했습니다.",
  },
  paths: {
    list(params: ActiveSessionListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      if (params.filter?.trim()) query.set("filter", params.filter.trim());
      return `/api/admin/security/active-sessions?${query.toString()}` as `/api/${string}`;
    },
    terminate(sessionId: string) {
      return `/api/admin/security/active-sessions/${encodeURIComponent(sessionId)}/terminate` as `/api/${string}`;
    },
  },
  validateReason(reason: string) {
    return reason.trim() ? {} : { reason: "강제종료 사유는 필수입니다." };
  },
  list(params: ActiveSessionListParams = {}) {
    return apiRequest<ActiveSessionSearchResponse>(
      activeSessionApi.paths.list(params),
    );
  },
  terminate(sessionId: string, reason: string) {
    return apiRequest<ActiveSessionRow>(
      activeSessionApi.paths.terminate(sessionId),
      {
        method: "POST",
        headers: { "X-Request-Id": `ui-${Date.now()}` },
        body: JSON.stringify({ reason: reason.trim() }),
      },
    );
  },
};

export function ActiveSessionStatusPage() {
  const [state, setState] = useState<ActiveSessionState>(
    createEmptyActiveSessionState(),
  );
  const [filter, setFilter] = useState("");
  const [pageSize, setPageSize] =
    useState<(typeof ACTIVE_SESSION_PAGE_SIZE_OPTIONS)[number]>(20);
  const [selected, setSelected] = useState<ActiveSessionRow | null>(null);
  const [reason, setReason] = useState("");
  const [fieldError, setFieldError] = useState<string | null>(null);

  const rows = useMemo(() => state.sessions, [state.sessions]);

  const load = async () => {
    setState((current) =>
      reduceActiveSessionState(current, { type: "loading" }),
    );
    try {
      const response = await activeSessionApi.list({ filter, size: pageSize });
      setState((current) =>
        reduceActiveSessionState(current, {
          type: "loaded",
          sessions: response.data?.sessions ?? [],
        }),
      );
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 403) {
        setState((current) =>
          reduceActiveSessionState(current, { type: "permission" }),
        );
      } else {
        setState((current) =>
          reduceActiveSessionState(current, {
            type: "error",
            message:
              caught instanceof Error
                ? caught.message
                : activeSessionApi.uiMessages.error,
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

  const terminateSelected = async () => {
    if (!selected) return;
    const errors = activeSessionApi.validateReason(reason);
    if ("reason" in errors) {
      setFieldError(errors.reason ?? null);
      return;
    }
    if (
      !window.confirm(
        activeSessionApi.uiMessages.terminateConfirm(selected.sessionId),
      )
    )
      return;
    try {
      const response = await activeSessionApi.terminate(
        selected.sessionId,
        reason,
      );
      const remaining = rows.filter(
        (row) => row.sessionId !== response.data?.sessionId,
      );
      setState((current) =>
        reduceActiveSessionState(current, {
          type: "success",
          message: activeSessionApi.uiMessages.terminateSuccess,
          sessions: remaining,
        }),
      );
      setSelected(null);
      setReason("");
      setFieldError(null);
      void load();
    } catch (caught) {
      setState((current) =>
        reduceActiveSessionState(current, {
          type:
            caught instanceof ApiClientError && caught.status === 403
              ? "permission"
              : "error",
          message:
            caught instanceof Error
              ? caught.message
              : activeSessionApi.uiMessages.error,
        } as ActiveSessionAction),
      );
    }
  };

  return (
    <section
      data-testid="active-session-status-screen"
      data-screen-id="SCR-ACTIVE-SESSION-STATUS"
      className="space-y-6"
    >
      <header className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm font-semibold text-primary">
          보안·감사 관리 &gt; 접속기록 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">접속현황 조회</h1>
        <p className="mt-2 text-sm text-muted">
          현재 활성 세션의 사용자, 로그인시각, 최종활동시각, IP, 상태를 조회하고
          R09 관리자가 사유를 남겨 강제종료합니다.
        </p>
      </header>

      <form
        data-testid="active-session-search-form"
        onSubmit={submitSearch}
        className="rounded-md border border-ld bg-white p-5 shadow-md"
      >
        <div className="grid gap-4 md:grid-cols-[1fr_auto_auto] md:items-end">
          <label className="text-sm font-medium text-dark">
            사용자·IP 검색
            <input
              data-testid="active-session-filter-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              placeholder="사용자 ID, 이름, IP"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              data-testid="active-session-size-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={pageSize}
              onChange={(event) =>
                setPageSize(
                  Number(
                    event.target.value,
                  ) as (typeof ACTIVE_SESSION_PAGE_SIZE_OPTIONS)[number],
                )
              }
            >
              {ACTIVE_SESSION_PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
          <button
            data-testid="active-session-search-button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            type="submit"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </form>

      {state.status === "loading" && (
        <LoadingState
          title="접속현황 조회 중"
          message="활성 세션 목록을 불러오고 있습니다."
        />
      )}
      {state.status === "permission" && (
        <PermissionState
          title="권한이 없습니다"
          message="R09 시스템관리자만 접속현황을 조회하고 강제종료할 수 있습니다."
        />
      )}
      {state.status === "error" && (
        <ErrorState
          title="접속현황 오류"
          message={state.message ?? activeSessionApi.uiMessages.error}
        />
      )}
      {state.status === "empty" && (
        <EmptyState
          title="활성 세션 없음"
          message="현재 표시할 활성 세션이 없습니다."
        />
      )}
      {state.status === "success" && (
        <SuccessState title="처리 완료" message={state.message} />
      )}

      <section className="overflow-hidden rounded-md border border-ld bg-white shadow-md">
        <div className="flex items-center justify-between border-b border-ld px-5 py-4">
          <h2 className="text-lg font-semibold text-dark">활성 세션 목록</h2>
          <button
            data-testid="active-session-refresh-button"
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
                <th className="px-4 py-3">사용자</th>
                <th className="px-4 py-3">로그인시각</th>
                <th className="px-4 py-3">최종활동시각</th>
                <th className="px-4 py-3">IP</th>
                <th className="px-4 py-3">세션상태</th>
                <th className="px-4 py-3">처리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr key={row.sessionId} data-testid="active-session-row">
                  <td className="px-4 py-3">
                    <strong className="text-dark">
                      {row.userName ?? row.loginId}
                    </strong>
                    <br />
                    <span className="text-xs text-muted">{row.loginId}</span>
                  </td>
                  <td className="px-4 py-3">{formatDateTime(row.loginAt)}</td>
                  <td className="px-4 py-3">
                    {formatDateTime(row.lastAccessedAt)}
                  </td>
                  <td className="px-4 py-3">{row.ipAddress ?? "-"}</td>
                  <td className="px-4 py-3">
                    <span className="rounded-full bg-lightsuccess px-2 py-1 text-xs font-semibold text-success">
                      {row.status}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      data-testid="active-session-select-terminate-button"
                      className="inline-flex items-center gap-2 rounded-md border border-error px-3 py-2 text-xs font-semibold text-error"
                      onClick={() => setSelected(row)}
                      type="button"
                    >
                      <ShieldX size={14} /> 강제종료
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {selected && (
        <section
          data-testid="active-session-terminate-panel"
          className="rounded-md border border-ld bg-white p-5 shadow-md"
        >
          <h2 className="text-lg font-semibold text-dark">
            강제종료 사유 입력
          </h2>
          <p className="mt-1 text-sm text-muted">
            대상 세션: {selected.sessionId}
          </p>
          <label className="mt-4 block text-sm font-medium text-dark">
            사유 <span className="text-error">*</span>
            <textarea
              data-testid="active-session-reason-textarea"
              className="mt-2 min-h-[96px] w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={reason}
              onChange={(event) => {
                setReason(event.target.value);
                setFieldError(null);
              }}
              placeholder="강제종료 사유를 입력하세요."
            />
          </label>
          {fieldError && (
            <p className="mt-2 text-sm text-error">{fieldError}</p>
          )}
          <div className="mt-4 flex gap-2">
            <button
              data-testid="active-session-terminate-submit-button"
              className="rounded-md bg-error px-4 py-2 text-sm font-semibold text-white"
              onClick={() => void terminateSelected()}
              type="button"
            >
              강제종료 실행
            </button>
            <button
              data-testid="active-session-terminate-cancel-button"
              className="rounded-md border border-ld px-4 py-2 text-sm"
              onClick={() => setSelected(null)}
              type="button"
            >
              취소
            </button>
          </div>
        </section>
      )}
    </section>
  );
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
}

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

export type SessionTerminationHistoryRow = {
  historyId: number;
  sessionId: string;
  userId: number;
  loginId: string;
  employeeNo?: string | null;
  userName?: string | null;
  terminationType:
    | "LOGOUT"
    | "IDLE_TIMEOUT"
    | "ABSOLUTE_TIMEOUT"
    | "ADMIN_TERMINATED";
  terminationReason?: string | null;
  terminatedAt: string;
};

type SessionTerminationHistorySearchResponse = {
  histories: SessionTerminationHistoryRow[];
  page: number;
  size: number;
  totalElements: number;
};

type SessionTerminationHistoryListParams = {
  filter?: string;
  terminationType?: string;
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

type SessionTerminationHistoryState = {
  status: ScreenStatus;
  histories: SessionTerminationHistoryRow[];
  message?: string;
};

type SessionTerminationHistoryAction =
  | { type: "loading" }
  | { type: "loaded"; histories: SessionTerminationHistoryRow[] }
  | { type: "error"; message: string }
  | { type: "permission" };

export const SESSION_TERMINATION_PAGE_SIZE_OPTIONS = [20, 50, 100] as const;
export const SESSION_TERMINATION_TYPE_OPTIONS = [
  "",
  "LOGOUT",
  "IDLE_TIMEOUT",
  "ABSOLUTE_TIMEOUT",
  "ADMIN_TERMINATED",
] as const;

export function getSessionTerminationHistoryRouteContract() {
  return {
    route: "/admin/security/session-termination-histories",
    screenId: "SCR-SESSION-TERMINATION-HISTORY",
    operations: ["listSessionTerminationHistories"],
  } as const;
}

export function createEmptySessionTerminationHistoryState(): SessionTerminationHistoryState {
  return { status: "idle", histories: [] };
}

export function reduceSessionTerminationHistoryState(
  state: SessionTerminationHistoryState,
  action: SessionTerminationHistoryAction,
): SessionTerminationHistoryState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.histories.length === 0 ? "empty" : "loaded",
        histories: action.histories,
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

export const sessionTerminationHistoryApi = {
  pageSizeOptions: SESSION_TERMINATION_PAGE_SIZE_OPTIONS,
  terminationTypeOptions: SESSION_TERMINATION_TYPE_OPTIONS,
  uiMessages: {
    error: "세션 종료 이력을 조회하지 못했습니다.",
    permission: "R09 시스템관리자만 로그아웃·만료 이력을 조회할 수 있습니다.",
  },
  paths: {
    list(params: SessionTerminationHistoryListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      if (params.filter?.trim()) query.set("filter", params.filter.trim());
      if (params.terminationType?.trim())
        query.set("terminationType", params.terminationType.trim());
      if (params.fromDate?.trim())
        query.set("fromDate", params.fromDate.trim());
      if (params.toDate?.trim()) query.set("toDate", params.toDate.trim());
      return `/api/admin/security/session-termination-histories?${query.toString()}` as `/api/${string}`;
    },
  },
  list(params: SessionTerminationHistoryListParams = {}) {
    return apiRequest<SessionTerminationHistorySearchResponse>(
      sessionTerminationHistoryApi.paths.list(params),
    );
  },
};

export function SessionTerminationHistoryPage() {
  const [state, setState] = useState<SessionTerminationHistoryState>(
    createEmptySessionTerminationHistoryState(),
  );
  const [filter, setFilter] = useState("");
  const [terminationType, setTerminationType] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [pageSize, setPageSize] =
    useState<(typeof SESSION_TERMINATION_PAGE_SIZE_OPTIONS)[number]>(20);

  const rows = useMemo(() => state.histories, [state.histories]);

  const load = async () => {
    setState((current) =>
      reduceSessionTerminationHistoryState(current, { type: "loading" }),
    );
    try {
      const response = await sessionTerminationHistoryApi.list({
        filter,
        terminationType,
        fromDate,
        toDate,
        size: pageSize,
      });
      setState((current) =>
        reduceSessionTerminationHistoryState(current, {
          type: "loaded",
          histories: response.data?.histories ?? [],
        }),
      );
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 403) {
        setState((current) =>
          reduceSessionTerminationHistoryState(current, { type: "permission" }),
        );
      } else {
        setState((current) =>
          reduceSessionTerminationHistoryState(current, {
            type: "error",
            message:
              caught instanceof Error
                ? caught.message
                : sessionTerminationHistoryApi.uiMessages.error,
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
      data-testid="session-termination-history-screen"
      data-screen-id="SCR-SESSION-TERMINATION-HISTORY"
      className="space-y-6"
    >
      <header className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm font-semibold text-primary">
          보안·감사 관리 &gt; 접속기록 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          로그아웃·만료 이력
        </h1>
        <p className="mt-2 text-sm text-muted">
          사용자, 기간, 종료유형 조건으로 로그아웃·유휴만료·절대만료·관리자
          강제종료 이력을 조회합니다. 이력 수정·삭제 CTA는 제공하지 않습니다.
        </p>
      </header>

      <form
        data-testid="session-termination-search-form"
        onSubmit={submitSearch}
        className="rounded-md border border-ld bg-white p-5 shadow-md"
      >
        <div className="grid gap-4 lg:grid-cols-[1fr_auto_auto_auto_auto_auto] lg:items-end">
          <label className="text-sm font-medium text-dark">
            사용자·세션 검색
            <input
              data-testid="session-termination-filter-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              placeholder="사용자 ID, 이름, 교번, 세션"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            기간 시작
            <input
              data-testid="session-termination-from-date-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="date"
              value={fromDate}
              onChange={(event) => setFromDate(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            기간 종료
            <input
              data-testid="session-termination-to-date-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="date"
              value={toDate}
              onChange={(event) => setToDate(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            종료유형
            <select
              data-testid="session-termination-type-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={terminationType}
              onChange={(event) => setTerminationType(event.target.value)}
            >
              <option value="">전체</option>
              <option value="LOGOUT">로그아웃</option>
              <option value="IDLE_TIMEOUT">유휴만료</option>
              <option value="ABSOLUTE_TIMEOUT">절대만료</option>
              <option value="ADMIN_TERMINATED">관리자 강제종료</option>
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              data-testid="session-termination-size-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={pageSize}
              onChange={(event) =>
                setPageSize(
                  Number(
                    event.target.value,
                  ) as (typeof SESSION_TERMINATION_PAGE_SIZE_OPTIONS)[number],
                )
              }
            >
              {SESSION_TERMINATION_PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
          <button
            data-testid="session-termination-search-button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            type="submit"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </form>

      {state.status === "loading" && (
        <LoadingState
          title="세션 종료 이력 조회 중"
          message="로그아웃·만료 이력을 불러오고 있습니다."
        />
      )}
      {state.status === "permission" && (
        <PermissionState
          title="권한이 없습니다"
          message={sessionTerminationHistoryApi.uiMessages.permission}
        />
      )}
      {state.status === "error" && (
        <ErrorState
          title="세션 종료 이력 오류"
          message={
            state.message ?? sessionTerminationHistoryApi.uiMessages.error
          }
        />
      )}
      {state.status === "empty" && (
        <EmptyState
          title="세션 종료 이력 없음"
          message="조건에 맞는 로그아웃·만료 이력이 없습니다."
        />
      )}

      <section className="overflow-hidden rounded-md border border-ld bg-white shadow-md">
        <div className="flex items-center justify-between border-b border-ld px-5 py-4">
          <h2 className="text-lg font-semibold text-dark">
            세션 종료 이력 목록
          </h2>
          <button
            data-testid="session-termination-refresh-button"
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
                <th className="px-4 py-3">세션</th>
                <th className="px-4 py-3">종료유형</th>
                <th className="px-4 py-3">종료일시</th>
                <th className="px-4 py-3">종료사유</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr
                  key={row.historyId}
                  data-testid="session-termination-history-row"
                >
                  <td className="px-4 py-3">
                    <strong className="text-dark">
                      {row.userName ?? row.loginId}
                    </strong>
                    <br />
                    <span className="text-xs text-muted">{row.loginId}</span>
                  </td>
                  <td className="px-4 py-3 font-mono text-xs">
                    {row.sessionId}
                  </td>
                  <td className="px-4 py-3">
                    <span className="rounded-full bg-lightprimary px-2 py-1 text-xs font-semibold text-primary">
                      {formatTerminationType(row.terminationType)}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    {formatDateTime(row.terminatedAt)}
                  </td>
                  <td className="px-4 py-3">{row.terminationReason ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}

function formatTerminationType(
  value: SessionTerminationHistoryRow["terminationType"],
) {
  const labels = {
    LOGOUT: "로그아웃",
    IDLE_TIMEOUT: "유휴만료",
    ABSOLUTE_TIMEOUT: "절대만료",
    ADMIN_TERMINATED: "관리자 강제종료",
  } satisfies Record<SessionTerminationHistoryRow["terminationType"], string>;
  return labels[value];
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
}

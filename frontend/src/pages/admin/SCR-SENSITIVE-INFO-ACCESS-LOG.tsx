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

export type SensitiveInformationAccessLogRow = {
  accessLogId: number;
  informationType: SensitiveInformationType;
  viewerUserId: number;
  viewerLoginId?: string | null;
  viewerName?: string | null;
  targetScope: string;
  accessPurpose: string;
  purposeSource: SensitivePurposeSource;
  accessResult: SensitiveAccessResult;
  requestId: string;
  accessedAt: string;
};

type SensitiveInformationAccessLogSearchResponse = {
  logs: SensitiveInformationAccessLogRow[];
  page: number;
  size: number;
  totalElements: number;
};

type SensitiveInformationAccessLogListParams = {
  filter?: string;
  informationType?: string;
  viewerUserId?: string;
  accessResult?: string;
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
type SensitiveInformationType =
  | "PERSONAL_EVALUATION_RESULT"
  | "SCORE_CALCULATION"
  | "PERSONAL_INFORMATION"
  | "ACCOUNT_INFORMATION";
type SensitivePurposeSource = "USER_INPUT" | "SYSTEM_CONTEXT" | "POLICY_RULE";
type SensitiveAccessResult = "SUCCESS" | "FAILURE";

type SensitiveInformationAccessLogState = {
  status: ScreenStatus;
  logs: SensitiveInformationAccessLogRow[];
  message?: string;
};

type SensitiveInformationAccessLogAction =
  | { type: "loading" }
  | { type: "loaded"; logs: SensitiveInformationAccessLogRow[] }
  | { type: "error"; message: string }
  | { type: "permission" };

export const SENSITIVE_INFORMATION_ACCESS_LOG_PAGE_SIZE_OPTIONS = [
  20, 50, 100,
] as const;
export const SENSITIVE_INFORMATION_TYPE_OPTIONS = [
  "",
  "PERSONAL_EVALUATION_RESULT",
  "SCORE_CALCULATION",
  "PERSONAL_INFORMATION",
  "ACCOUNT_INFORMATION",
] as const;
export const SENSITIVE_ACCESS_RESULT_OPTIONS = [
  "",
  "SUCCESS",
  "FAILURE",
] as const;

export function getSensitiveInformationAccessLogRouteContract() {
  return {
    route: "/admin/audit/sensitive-information-access-logs",
    screenId: "SCR-SENSITIVE-INFO-ACCESS-LOG",
    operations: ["listSensitiveInformationAccessLogs"],
  } as const;
}

export function createEmptySensitiveInformationAccessLogState(): SensitiveInformationAccessLogState {
  return { status: "idle", logs: [] };
}

export function reduceSensitiveInformationAccessLogState(
  state: SensitiveInformationAccessLogState,
  action: SensitiveInformationAccessLogAction,
): SensitiveInformationAccessLogState {
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

export const sensitiveInformationAccessLogApi = {
  pageSizeOptions: SENSITIVE_INFORMATION_ACCESS_LOG_PAGE_SIZE_OPTIONS,
  informationTypeOptions: SENSITIVE_INFORMATION_TYPE_OPTIONS,
  accessResultOptions: SENSITIVE_ACCESS_RESULT_OPTIONS,
  uiMessages: {
    error: "중요정보 조회 로그를 조회하지 못했습니다.",
    permission: "R09 시스템관리자만 중요정보 조회 로그를 조회할 수 있습니다.",
  },
  paths: {
    list(params: SensitiveInformationAccessLogListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      if (params.filter?.trim()) query.set("filter", params.filter.trim());
      if (params.informationType?.trim())
        query.set("informationType", params.informationType.trim());
      if (params.viewerUserId?.trim())
        query.set("viewerUserId", params.viewerUserId.trim());
      if (params.accessResult?.trim())
        query.set("accessResult", params.accessResult.trim());
      if (params.fromDate?.trim())
        query.set("fromDate", params.fromDate.trim());
      if (params.toDate?.trim()) query.set("toDate", params.toDate.trim());
      return `/api/admin/audit/sensitive-information-access-logs?${query.toString()}` as `/api/${string}`;
    },
  },
  list(params: SensitiveInformationAccessLogListParams = {}) {
    return apiRequest<SensitiveInformationAccessLogSearchResponse>(
      sensitiveInformationAccessLogApi.paths.list(params),
    );
  },
};

export function SensitiveInformationAccessLogPage() {
  const [state, setState] = useState<SensitiveInformationAccessLogState>(
    createEmptySensitiveInformationAccessLogState(),
  );
  const [filter, setFilter] = useState("");
  const [informationType, setInformationType] = useState("");
  const [viewerUserId, setViewerUserId] = useState("");
  const [accessResult, setAccessResult] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [pageSize, setPageSize] =
    useState<
      (typeof SENSITIVE_INFORMATION_ACCESS_LOG_PAGE_SIZE_OPTIONS)[number]
    >(20);

  const rows = useMemo(() => state.logs, [state.logs]);

  const load = async () => {
    setState((current) =>
      reduceSensitiveInformationAccessLogState(current, { type: "loading" }),
    );
    try {
      const response = await sensitiveInformationAccessLogApi.list({
        filter,
        informationType,
        viewerUserId,
        accessResult,
        fromDate,
        toDate,
        size: pageSize,
      });
      setState((current) =>
        reduceSensitiveInformationAccessLogState(current, {
          type: "loaded",
          logs: response.data?.logs ?? [],
        }),
      );
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 403) {
        setState((current) =>
          reduceSensitiveInformationAccessLogState(current, {
            type: "permission",
          }),
        );
      } else {
        setState((current) =>
          reduceSensitiveInformationAccessLogState(current, {
            type: "error",
            message:
              caught instanceof Error
                ? caught.message
                : sensitiveInformationAccessLogApi.uiMessages.error,
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
      data-testid="sensitive-information-access-log-screen"
      data-screen-id="SCR-SENSITIVE-INFO-ACCESS-LOG"
      className="space-y-6"
    >
      <header className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm font-semibold text-primary">
          보안·감사 관리 &gt; 감사로그 관리
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          중요정보 조회 로그
        </h1>
        <p className="mt-2 text-sm text-muted">
          중요정보 유형별 조회자·대상범위·조회목적·조회결과를 조회합니다.
          중요정보 원문·계좌 원문·평가결과 원문 표시 영역과 수정·삭제 CTA는
          제공하지 않습니다.
        </p>
      </header>

      <form
        data-testid="sensitive-information-access-log-search-form"
        onSubmit={submitSearch}
        className="rounded-md border border-ld bg-white p-5 shadow-md"
      >
        <div className="grid gap-4 lg:grid-cols-[auto_1fr_auto_auto_auto_auto_auto] lg:items-end">
          <label className="text-sm font-medium text-dark">
            정보유형
            <select
              data-testid="sensitive-information-type-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={informationType}
              onChange={(event) => setInformationType(event.target.value)}
            >
              <option value="">전체</option>
              {SENSITIVE_INFORMATION_TYPE_OPTIONS.filter(Boolean).map(
                (value) => (
                  <option key={value} value={value}>
                    {formatInformationType(value as SensitiveInformationType)}
                  </option>
                ),
              )}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            조회자 검색
            <input
              data-testid="sensitive-information-filter-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              placeholder="조회자, 목적, 요청ID"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            조회자 ID
            <input
              data-testid="sensitive-information-viewer-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              inputMode="numeric"
              value={viewerUserId}
              onChange={(event) =>
                setViewerUserId(event.target.value.replace(/[^0-9]/g, ""))
              }
              placeholder="user_id"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            기간 시작
            <input
              data-testid="sensitive-information-from-date-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="date"
              value={fromDate}
              onChange={(event) => setFromDate(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            기간 종료
            <input
              data-testid="sensitive-information-to-date-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              type="date"
              value={toDate}
              onChange={(event) => setToDate(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              data-testid="sensitive-information-size-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={pageSize}
              onChange={(event) =>
                setPageSize(
                  Number(
                    event.target.value,
                  ) as (typeof SENSITIVE_INFORMATION_ACCESS_LOG_PAGE_SIZE_OPTIONS)[number],
                )
              }
            >
              {SENSITIVE_INFORMATION_ACCESS_LOG_PAGE_SIZE_OPTIONS.map(
                (size) => (
                  <option key={size} value={size}>
                    {size}건
                  </option>
                ),
              )}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            조회결과
            <select
              data-testid="sensitive-information-result-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={accessResult}
              onChange={(event) => setAccessResult(event.target.value)}
            >
              <option value="">전체</option>
              <option value="SUCCESS">성공</option>
              <option value="FAILURE">실패</option>
            </select>
          </label>
          <button
            data-testid="sensitive-information-search-button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            type="submit"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </form>

      {state.status === "loading" && (
        <LoadingState
          title="중요정보 조회 로그 조회 중"
          message="중요정보 조회 감사로그를 불러오고 있습니다."
        />
      )}
      {state.status === "permission" && (
        <PermissionState
          title="권한이 없습니다"
          message={sensitiveInformationAccessLogApi.uiMessages.permission}
        />
      )}
      {state.status === "error" && (
        <ErrorState
          title="중요정보 조회 로그 오류"
          message={
            state.message ?? sensitiveInformationAccessLogApi.uiMessages.error
          }
        />
      )}
      {state.status === "empty" && (
        <EmptyState
          title="중요정보 조회 로그 없음"
          message="조건에 맞는 중요정보 조회 로그가 없습니다."
        />
      )}

      <section className="overflow-hidden rounded-md border border-ld bg-white shadow-md">
        <div className="flex items-center justify-between border-b border-ld px-5 py-4">
          <h2 className="text-lg font-semibold text-dark">
            중요정보 조회 로그 목록
          </h2>
          <button
            data-testid="sensitive-information-refresh-button"
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
                <th className="px-4 py-3">정보유형</th>
                <th className="px-4 py-3">조회자</th>
                <th className="px-4 py-3">대상범위</th>
                <th className="px-4 py-3">조회목적</th>
                <th className="px-4 py-3">목적 출처</th>
                <th className="px-4 py-3">조회일시</th>
                <th className="px-4 py-3">조회결과</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr
                  key={row.accessLogId}
                  data-testid="sensitive-information-access-log-row"
                >
                  <td className="px-4 py-3">
                    <span className="rounded-full bg-lightprimary px-2 py-1 text-xs font-semibold text-primary">
                      {formatInformationType(row.informationType)}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <strong className="text-dark">
                      {row.viewerName ?? row.viewerLoginId ?? row.viewerUserId}
                    </strong>
                    <br />
                    <span className="text-xs text-muted">
                      {row.viewerLoginId ?? `user:${row.viewerUserId}`}
                    </span>
                  </td>
                  <td className="max-w-sm px-4 py-3 text-muted">
                    {row.targetScope}
                  </td>
                  <td className="max-w-sm px-4 py-3 text-muted">
                    {row.accessPurpose}
                    <br />
                    <span className="font-mono text-xs">{row.requestId}</span>
                  </td>
                  <td className="px-4 py-3">
                    {formatPurposeSource(row.purposeSource)}
                  </td>
                  <td className="px-4 py-3">
                    {formatDateTime(row.accessedAt)}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={
                        row.accessResult === "SUCCESS"
                          ? "text-success"
                          : "text-error"
                      }
                    >
                      {formatResult(row.accessResult)}
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

function formatInformationType(value: SensitiveInformationType) {
  const labels = {
    PERSONAL_EVALUATION_RESULT: "개인평가결과",
    SCORE_CALCULATION: "점수산정",
    PERSONAL_INFORMATION: "개인정보",
    ACCOUNT_INFORMATION: "계좌정보",
  } satisfies Record<SensitiveInformationType, string>;
  return labels[value];
}

function formatPurposeSource(value: SensitivePurposeSource) {
  const labels = {
    USER_INPUT: "사용자 입력",
    SYSTEM_CONTEXT: "시스템 맥락",
    POLICY_RULE: "정책 규칙",
  } satisfies Record<SensitivePurposeSource, string>;
  return labels[value] ?? value;
}

function formatResult(value: SensitiveAccessResult) {
  return value === "SUCCESS" ? "성공" : "실패";
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
}

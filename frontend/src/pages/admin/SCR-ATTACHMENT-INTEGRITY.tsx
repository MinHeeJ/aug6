import { useEffect, useState } from "react";
import { Download, FileSearch, Play, RefreshCw, Search } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type IntegrityAnomalyType =
  | "MISSING_BUSINESS_REF"
  | "MISSING_STORAGE_FILE"
  | "DUPLICATE_FILE";

export type AttachmentIntegrityCheck = {
  checkId: number;
  status: "RUNNING" | "COMPLETED" | "FAILED";
  startedBy: number;
  startedAt: string;
  completedAt?: string | null;
  findingCount: number;
  anomalyTypes: IntegrityAnomalyType[];
};

export type AttachmentIntegrityFinding = {
  findingId: number;
  checkId: number;
  fileId?: number | null;
  storageObjectRef?: string | null;
  anomalyType: IntegrityAnomalyType;
  resultMessage: string;
  createdAt: string;
};

type AttachmentIntegritySearchResponse = {
  results: AttachmentIntegrityFinding[];
  page: number;
  size: 20 | 50 | 100;
  totalElements: number;
};

type ScreenStatus =
  | "idle"
  | "running"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success";

export type AttachmentIntegrityState = {
  status: ScreenStatus;
  check?: AttachmentIntegrityCheck;
  results: AttachmentIntegrityFinding[];
  page: number;
  size: 20 | 50 | 100;
  totalElements: number;
  startedAtMs?: number;
  showProgress: boolean;
  message?: string;
};

type AttachmentIntegrityAction =
  | { type: "running"; startedAtMs: number }
  | { type: "tick" }
  | { type: "completed"; check: AttachmentIntegrityCheck }
  | {
      type: "loaded";
      results: AttachmentIntegrityFinding[];
      page: number;
      size: number;
      totalElements: number;
    }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "downloaded"; message: string };

type FilterParams = {
  checkId?: number | string;
  anomalyType?: IntegrityAnomalyType | "";
  page?: number;
  size?: 20 | 50 | 100;
};

export function getAttachmentIntegrityRouteContract() {
  return {
    route: "/admin/attachment-integrity",
    screenId: "SCR-ATTACHMENT-INTEGRITY",
    operations: [
      "createAttachmentIntegrityCheck",
      "listAttachmentIntegrityResults",
      "downloadAttachmentIntegrityExcel",
    ],
  } as const;
}

export function createEmptyAttachmentIntegrityState(): AttachmentIntegrityState {
  return {
    status: "idle",
    results: [],
    page: 0,
    size: 20,
    totalElements: 0,
    showProgress: false,
  };
}

export function reduceAttachmentIntegrityState(
  state: AttachmentIntegrityState,
  action: AttachmentIntegrityAction,
): AttachmentIntegrityState {
  switch (action.type) {
    case "running":
      return {
        ...state,
        status: "running",
        startedAtMs: action.startedAtMs,
        showProgress: false,
        message: "정합성 점검을 실행하고 있습니다.",
      };
    case "tick": {
      const elapsed = state.startedAtMs ? Date.now() - state.startedAtMs : 0;
      return {
        ...state,
        showProgress: elapsed >= 10_000,
        message:
          elapsed >= 10_000
            ? "10초 이상 진행 중입니다. 완료되면 결과를 표시합니다."
            : state.message,
      };
    }
    case "completed":
      return {
        ...state,
        status: "success",
        check: action.check,
        showProgress: false,
        message: `정합성 점검이 완료되었습니다. 이상 결과 ${action.check.findingCount}건`,
      };
    case "loaded":
      return {
        ...state,
        status: action.results.length === 0 ? "empty" : "loaded",
        results: action.results,
        page: action.page,
        size: normalizePageSize(action.size),
        totalElements: action.totalElements,
      };
    case "error":
      return {
        ...state,
        status: "error",
        showProgress: false,
        message: action.message,
      };
    case "permission":
      return {
        ...state,
        status: "permission",
        showProgress: false,
        message: "파일 정합성 점검 권한이 없습니다.",
      };
    case "downloaded":
      return { ...state, status: "success", message: action.message };
    default:
      return state;
  }
}

export const attachmentIntegrityApi = {
  paths: {
    createCheck() {
      return "/api/admin/attachment-integrity-checks" as `/api/${string}`;
    },
    listResults(params: FilterParams = {}) {
      const query = new URLSearchParams();
      if (params.checkId !== undefined && String(params.checkId).trim()) {
        query.set("checkId", String(params.checkId).trim());
      }
      if (params.anomalyType) query.set("anomalyType", params.anomalyType);
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      return `/api/admin/attachment-integrity-results?${query.toString()}` as `/api/${string}`;
    },
    downloadExcel(params: Pick<FilterParams, "checkId" | "anomalyType"> = {}) {
      const query = new URLSearchParams();
      if (params.checkId !== undefined && String(params.checkId).trim()) {
        query.set("checkId", String(params.checkId).trim());
      }
      if (params.anomalyType) query.set("anomalyType", params.anomalyType);
      const suffix = query.toString() ? `?${query.toString()}` : "";
      return `/api/admin/attachment-integrity-results/excel${suffix}` as `/api/${string}`;
    },
  },
  createCheck() {
    return apiRequest<AttachmentIntegrityCheck>(
      attachmentIntegrityApi.paths.createCheck(),
      {
        method: "POST",
      },
    );
  },
  listResults(params: FilterParams = {}) {
    return apiRequest<AttachmentIntegritySearchResponse>(
      attachmentIntegrityApi.paths.listResults(params),
    );
  },
  async downloadExcel(
    params: Pick<FilterParams, "checkId" | "anomalyType"> = {},
  ) {
    const response = await fetch(
      attachmentIntegrityApi.paths.downloadExcel(params),
      {
        credentials: "include",
      },
    );
    if (!response.ok) {
      const body = await tryReadApiError(response);
      throw new ApiClientError(
        response.status,
        body?.error?.message ?? "엑셀 다운로드에 실패했습니다.",
        body?.error,
      );
    }
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "attachment-integrity-results.csv";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.URL.revokeObjectURL(url);
  },
};

export function AttachmentIntegrityPage() {
  const [checkId, setCheckId] = useState("");
  const [anomalyType, setAnomalyType] = useState<IntegrityAnomalyType | "">("");
  const [state, setState] = useState<AttachmentIntegrityState>(
    createEmptyAttachmentIntegrityState(),
  );

  useEffect(() => {
    if (state.status !== "running") return undefined;
    const interval = window.setInterval(() => {
      setState((current) =>
        reduceAttachmentIntegrityState(current, { type: "tick" }),
      );
    }, 1000);
    return () => window.clearInterval(interval);
  }, [state.status]);

  const filters = (page = state.page, size: 20 | 50 | 100 = state.size) => ({
    checkId,
    anomalyType,
    page,
    size,
  });

  const runCheck = async () => {
    setState((current) =>
      reduceAttachmentIntegrityState(current, {
        type: "running",
        startedAtMs: Date.now(),
      }),
    );
    try {
      const response = await attachmentIntegrityApi.createCheck();
      const check = response.data;
      if (check) {
        setCheckId(String(check.checkId));
        setState((current) =>
          reduceAttachmentIntegrityState(current, { type: "completed", check }),
        );
        await loadResults(0, state.size, String(check.checkId));
      }
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const loadResults = async (
    page = 0,
    size: 20 | 50 | 100 = state.size,
    overrideCheckId?: string,
  ) => {
    setState((current) => ({ ...current, status: "loading" }));
    try {
      const response = await attachmentIntegrityApi.listResults({
        checkId: overrideCheckId ?? checkId,
        anomalyType,
        page,
        size,
      });
      const data = response.data ?? {
        results: [],
        page,
        size,
        totalElements: 0,
      };
      setState((current) =>
        reduceAttachmentIntegrityState(current, {
          type: "loaded",
          results: data.results,
          page: data.page,
          size: data.size,
          totalElements: data.totalElements,
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const downloadExcel = async () => {
    try {
      await attachmentIntegrityApi.downloadExcel({ checkId, anomalyType });
      setState((current) =>
        reduceAttachmentIntegrityState(current, {
          type: "downloaded",
          message: "엑셀 다운로드를 시작했습니다.",
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceAttachmentIntegrityState(current, { type: "permission" }),
      );
      return;
    }
    setState((current) =>
      reduceAttachmentIntegrityState(current, {
        type: "error",
        message:
          caught instanceof Error
            ? caught.message
            : "파일 정합성 처리 중 오류가 발생했습니다.",
      }),
    );
  };

  if (state.status === "permission") {
    return (
      <section data-testid="attachment-integrity-page" className="space-y-6">
        <PermissionState
          title="파일 정합성 점검 권한이 없습니다"
          message="R09 시스템관리자 권한을 확인하세요."
        />
      </section>
    );
  }

  return (
    <section
      data-testid="attachment-integrity-page"
      data-screen-id="SCR-ATTACHMENT-INTEGRITY"
      className="space-y-6"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm font-semibold text-primary">
          파일·데이터 관리 &gt; 첨부파일 관리 &gt; 파일 저장소 정합성 점검
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          파일 저장소 정합성 점검
        </h1>
        <p className="mt-2 text-sm text-muted">
          DB 첨부 메타정보와 실제 저장소 객체를 비교하고 연결자료 없음, 실제파일
          없음, 중복파일을 분류합니다. 점검은 파일이나 업무자료를 자동 삭제하지
          않습니다.
        </p>
      </div>

      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="grid flex-1 gap-4 md:grid-cols-3">
            <label
              className="text-sm font-semibold text-ld"
              htmlFor="integrityCheckId"
            >
              점검ID
              <input
                id="integrityCheckId"
                data-testid="integrity-check-id-input"
                className="mt-2 flex h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm text-ld focus-visible:border-primary focus-visible:outline-0"
                value={checkId}
                onChange={(event) => setCheckId(event.target.value)}
                placeholder="최근 점검ID 또는 전체"
              />
            </label>
            <label
              className="text-sm font-semibold text-ld"
              htmlFor="integrityAnomalyType"
            >
              이상유형
              <select
                id="integrityAnomalyType"
                data-testid="integrity-anomaly-type-select"
                className="mt-2 h-10 w-full rounded-lg border border-ld bg-white px-3 text-sm text-ld"
                value={anomalyType}
                onChange={(event) =>
                  setAnomalyType(
                    event.target.value as IntegrityAnomalyType | "",
                  )
                }
              >
                <option value="">전체</option>
                <option value="MISSING_BUSINESS_REF">연결자료 없음</option>
                <option value="MISSING_STORAGE_FILE">실제파일 없음</option>
                <option value="DUPLICATE_FILE">중복파일</option>
              </select>
            </label>
            <label
              className="text-sm font-semibold text-ld"
              htmlFor="integrityPageSize"
            >
              표시 건수
              <select
                id="integrityPageSize"
                data-testid="integrity-page-size-select"
                className="mt-2 h-10 w-full rounded-lg border border-ld bg-white px-3 text-sm text-ld"
                value={state.size}
                onChange={(event) =>
                  void loadResults(
                    0,
                    Number(event.target.value) as 20 | 50 | 100,
                  )
                }
              >
                <option value={20}>20</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
              </select>
            </label>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              data-testid="integrity-run-button"
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-white shadow-btn-shadow transition-colors hover:bg-secondary disabled:opacity-50"
              onClick={() => void runCheck()}
              disabled={state.status === "running"}
            >
              <Play size={16} /> 점검 실행
            </button>
            <button
              data-testid="integrity-search-button"
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-medium text-primary transition-colors hover:bg-lightprimary"
              onClick={() => void loadResults(0)}
            >
              <Search size={16} /> 결과 조회
            </button>
            <button
              data-testid="integrity-excel-button"
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-success px-4 py-2 text-sm font-medium text-success transition-colors hover:bg-lightsuccess"
              onClick={() => void downloadExcel()}
            >
              <Download size={16} /> 엑셀 다운로드
            </button>
            <button
              data-testid="integrity-reset-button"
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-medium text-muted transition-colors hover:bg-lightgray"
              onClick={() => {
                setCheckId("");
                setAnomalyType("");
                setState(createEmptyAttachmentIntegrityState());
              }}
            >
              <RefreshCw size={16} /> 초기화
            </button>
          </div>
        </div>
      </section>

      {state.status === "running" ? (
        <LoadingState title="정합성 점검 실행 중" message={state.message} />
      ) : null}
      {state.showProgress ? (
        <div
          data-testid="integrity-progress-panel"
          role="status"
          aria-live="polite"
          className="rounded-md border border-primary bg-lightprimary p-4 text-sm font-medium text-primary"
        >
          장시간 점검 진행 중입니다. 저장소 비교가 완료되면 결과 목록을
          갱신합니다.
        </div>
      ) : null}
      {state.status === "loading" ? (
        <LoadingState
          title="결과 조회 중"
          message="정합성 점검 결과를 불러오고 있습니다."
        />
      ) : null}
      {state.status === "empty" ? (
        <EmptyState
          title="점검 결과 없음"
          message="현재 필터에 해당하는 이상 결과가 없습니다."
        />
      ) : null}
      {state.status === "error" ? (
        <ErrorState title="정합성 점검 오류" message={state.message} />
      ) : null}
      {state.status === "success" ? (
        <SuccessState title="처리 완료" message={state.message} />
      ) : null}

      <section className="overflow-hidden rounded-md border border-ld bg-white shadow-md">
        <div className="flex items-center justify-between border-b border-ld px-6 py-4">
          <div className="flex items-center gap-2">
            <FileSearch className="text-primary" size={18} />
            <h2 className="text-lg font-semibold text-dark">
              정합성 점검 결과
            </h2>
          </div>
          <p className="text-sm text-muted">총 {state.totalElements}건</p>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-ld text-sm">
            <thead className="bg-lightsecondary text-left text-xs font-semibold uppercase text-muted">
              <tr>
                <th className="px-4 py-3">점검ID</th>
                <th className="px-4 py-3">파일ID</th>
                <th className="px-4 py-3">이상유형</th>
                <th className="px-4 py-3">결과 메시지</th>
                <th className="px-4 py-3">저장소 객체</th>
                <th className="px-4 py-3">생성일시</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {state.results.map((row) => (
                <tr data-testid="integrity-result-row" key={row.findingId}>
                  <td className="px-4 py-3 text-muted">{row.checkId}</td>
                  <td className="px-4 py-3 text-muted">{row.fileId ?? "-"}</td>
                  <td className="px-4 py-3 font-medium text-dark">
                    {anomalyLabel(row.anomalyType)}
                  </td>
                  <td className="px-4 py-3 text-muted">{row.resultMessage}</td>
                  <td className="px-4 py-3 text-muted">
                    {row.storageObjectRef ?? "-"}
                  </td>
                  <td className="px-4 py-3 text-muted">
                    {formatDate(row.createdAt)}
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

async function tryReadApiError(response: Response) {
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) return undefined;
  return (await response.json()) as {
    error?: {
      code: string;
      message: string;
      fields: { field: string; message: string }[];
    };
  };
}

function normalizePageSize(size: number): 20 | 50 | 100 {
  return size === 50 || size === 100 ? size : 20;
}

function anomalyLabel(type: IntegrityAnomalyType): string {
  const labels: Record<IntegrityAnomalyType, string> = {
    MISSING_BUSINESS_REF: "연결자료 없음",
    MISSING_STORAGE_FILE: "실제파일 없음",
    DUPLICATE_FILE: "중복파일",
  };
  return labels[type];
}

function formatDate(value: string): string {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
}

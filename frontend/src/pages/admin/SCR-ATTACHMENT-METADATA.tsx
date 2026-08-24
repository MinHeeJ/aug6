import { useEffect, useState } from "react";
import { Download, FileText, RefreshCw, Search } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type AttachmentMetadataRow = {
  fileId: number;
  businessType: string;
  businessRecordId: string;
  businessRecordStatus: string;
  originalFilename: string;
  extension: string;
  fileSizeBytes: number;
  uploadedBy: number;
  uploadedAt: string;
  malwareScanStatus: string;
  deletedAt?: string | null;
};

type AttachmentSearchResponse = {
  attachments: AttachmentMetadataRow[];
  page: number;
  size: number;
  totalElements: number;
};

type ScreenStatus =
  | "idle"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success";

type AttachmentMetadataState = {
  status: ScreenStatus;
  attachments: AttachmentMetadataRow[];
  page: number;
  size: 20 | 50 | 100;
  totalElements: number;
  message?: string;
};

type AttachmentMetadataAction =
  | { type: "loading" }
  | {
      type: "loaded";
      attachments: AttachmentMetadataRow[];
      page: number;
      size: number;
      totalElements: number;
    }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "downloaded"; message: string };

type ListParams = {
  businessRecordId: string;
  page?: number;
  size?: 20 | 50 | 100;
};

export function getAttachmentMetadataRouteContract() {
  return {
    route: "/admin/attachments",
    screenId: "SCR-ATTACHMENT-METADATA",
    operations: ["listAttachments", "getAttachmentDownload"],
  } as const;
}

export function createEmptyAttachmentMetadataState(): AttachmentMetadataState {
  return {
    status: "idle",
    attachments: [],
    page: 0,
    size: 20,
    totalElements: 0,
  };
}

export function reduceAttachmentMetadataState(
  state: AttachmentMetadataState,
  action: AttachmentMetadataAction,
): AttachmentMetadataState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.attachments.length === 0 ? "empty" : "loaded",
        attachments: action.attachments,
        page: action.page,
        size: normalizePageSize(action.size),
        totalElements: action.totalElements,
        message: undefined,
      };
    case "error":
      return { ...state, status: "error", message: action.message };
    case "permission":
      return {
        ...state,
        status: "permission",
        message: "첨부파일 조회 권한이 없습니다.",
      };
    case "downloaded":
      return { ...state, status: "success", message: action.message };
    default:
      return state;
  }
}

export const attachmentMetadataApi = {
  paths: {
    list(params: ListParams) {
      const query = new URLSearchParams();
      query.set("businessRecordId", params.businessRecordId.trim());
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      return `/api/admin/attachments?${query.toString()}` as `/api/${string}`;
    },
    download(fileId: number) {
      return `/api/admin/attachments/${encodeURIComponent(String(fileId))}/download` as `/api/${string}`;
    },
  },
  publicRowKeys() {
    return [
      "fileId",
      "businessType",
      "businessRecordId",
      "businessRecordStatus",
      "originalFilename",
      "extension",
      "fileSizeBytes",
      "uploadedBy",
      "uploadedAt",
      "malwareScanStatus",
      "deletedAt",
    ] as const;
  },
  list(params: ListParams) {
    return apiRequest<AttachmentSearchResponse>(
      attachmentMetadataApi.paths.list(params),
    );
  },
  async download(
    file: Pick<AttachmentMetadataRow, "fileId" | "originalFilename">,
  ) {
    const response = await fetch(
      attachmentMetadataApi.paths.download(file.fileId),
      {
        credentials: "include",
      },
    );
    if (!response.ok) {
      const body = await tryReadApiError(response);
      throw new ApiClientError(
        response.status,
        body?.error?.message ?? "첨부파일 다운로드에 실패했습니다.",
        body?.error,
      );
    }
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = file.originalFilename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.URL.revokeObjectURL(url);
  },
};

export function AttachmentMetadataPage() {
  const [businessRecordId, setBusinessRecordId] = useState("");
  const [filenameFilter, setFilenameFilter] = useState("");
  const [state, setState] = useState<AttachmentMetadataState>(
    createEmptyAttachmentMetadataState(),
  );

  const loadAttachments = async (
    page = 0,
    size: 20 | 50 | 100 = state.size,
  ) => {
    if (!businessRecordId.trim()) {
      setState((current) =>
        reduceAttachmentMetadataState(current, {
          type: "error",
          message: "업무자료 식별자를 입력하세요.",
        }),
      );
      return;
    }
    setState((current) =>
      reduceAttachmentMetadataState(current, { type: "loading" }),
    );
    try {
      const response = await attachmentMetadataApi.list({
        businessRecordId,
        page,
        size,
      });
      const data = response.data ?? {
        attachments: [],
        page,
        size,
        totalElements: 0,
      };
      setState((current) =>
        reduceAttachmentMetadataState(current, {
          type: "loaded",
          attachments: filterByFilename(data.attachments, filenameFilter),
          page: data.page,
          size: data.size,
          totalElements: data.totalElements,
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    setState(createEmptyAttachmentMetadataState());
  }, []);

  const download = async (file: AttachmentMetadataRow) => {
    try {
      await attachmentMetadataApi.download(file);
      setState((current) =>
        reduceAttachmentMetadataState(current, {
          type: "downloaded",
          message: "다운로드를 시작했습니다.",
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceAttachmentMetadataState(current, { type: "permission" }),
      );
      return;
    }
    setState((current) =>
      reduceAttachmentMetadataState(current, {
        type: "error",
        message:
          caught instanceof Error
            ? caught.message
            : "첨부파일 조회 중 오류가 발생했습니다.",
      }),
    );
  };

  const reset = () => {
    setBusinessRecordId("");
    setFilenameFilter("");
    setState(createEmptyAttachmentMetadataState());
  };

  if (state.status === "permission") {
    return (
      <section data-testid="attachment-metadata-page" className="space-y-6">
        <PermissionState
          title="첨부파일 조회 권한이 없습니다"
          message="첨부파일 메타정보 조회 또는 다운로드 권한을 확인하세요."
        />
      </section>
    );
  }

  return (
    <section
      data-testid="attachment-metadata-page"
      data-screen-id="SCR-ATTACHMENT-METADATA"
      className="space-y-6"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm font-semibold text-primary">
          파일·데이터 관리 &gt; 첨부파일 관리 &gt; 첨부파일 조회
        </p>
        <h1 className="mt-2 text-xl font-semibold text-dark">첨부파일 조회</h1>
        <p className="mt-2 text-sm text-muted">
          업무자료별 첨부파일 메타정보를 조회하고 다운로드 시 권한을
          재검증합니다. 저장 경로와 실제 파일명은 표시하지 않습니다.
        </p>
      </div>

      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <div className="grid gap-4 lg:grid-cols-[1fr_1fr_auto]">
          <label
            className="text-sm font-semibold text-ld"
            htmlFor="businessRecordId"
          >
            업무자료<span className="ms-1 text-error">*</span>
            <input
              id="businessRecordId"
              data-testid="attachment-business-record-input"
              className="mt-2 flex h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm text-ld focus-visible:border-primary focus-visible:outline-0"
              value={businessRecordId}
              onChange={(event) => setBusinessRecordId(event.target.value)}
              placeholder="업무자료 식별자를 입력하세요"
            />
          </label>
          <label
            className="text-sm font-semibold text-ld"
            htmlFor="filenameFilter"
          >
            파일명
            <input
              id="filenameFilter"
              data-testid="attachment-filename-filter-input"
              className="mt-2 flex h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm text-ld focus-visible:border-primary focus-visible:outline-0"
              value={filenameFilter}
              onChange={(event) => setFilenameFilter(event.target.value)}
              placeholder="원본 파일명 검색"
            />
          </label>
          <div className="flex items-end gap-2">
            <button
              data-testid="attachment-search-button"
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-white shadow-btn-shadow transition-colors hover:bg-secondary disabled:opacity-50"
              onClick={() => void loadAttachments(0)}
              disabled={state.status === "loading"}
            >
              <Search size={16} /> 조회
            </button>
            <button
              data-testid="attachment-reset-button"
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-medium text-primary transition-colors hover:bg-lightprimary"
              onClick={reset}
            >
              <RefreshCw size={16} /> 초기화
            </button>
          </div>
        </div>
        <div className="mt-4 flex items-center gap-2 text-sm text-muted">
          <span>기본 20건</span>
          <select
            data-testid="attachment-page-size-select"
            className="h-9 rounded-lg border border-ld bg-white px-3 text-sm text-ld"
            value={state.size}
            onChange={(event) =>
              void loadAttachments(
                0,
                Number(event.target.value) as 20 | 50 | 100,
              )
            }
          >
            <option value={20}>20</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
          <span>총 {state.totalElements}건</span>
        </div>
      </section>

      {state.status === "loading" ? (
        <LoadingState
          title="첨부파일 조회 중"
          message="연결 첨부 목록을 불러오고 있습니다."
        />
      ) : null}
      {state.status === "empty" ? (
        <EmptyState
          title="첨부파일 없음"
          message="선택한 업무자료에 연결된 첨부파일이 없습니다."
        />
      ) : null}
      {state.status === "error" ? (
        <ErrorState title="첨부파일 조회 오류" message={state.message} />
      ) : null}
      {state.status === "success" ? (
        <SuccessState title="처리 완료" message={state.message} />
      ) : null}

      <section className="overflow-hidden rounded-md border border-ld bg-white shadow-md">
        <div className="flex items-center gap-2 border-b border-ld px-6 py-4">
          <FileText className="text-primary" size={18} />
          <h2 className="text-lg font-semibold text-dark">
            첨부 메타정보 목록
          </h2>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-ld text-sm">
            <thead className="bg-lightsecondary text-left text-xs font-semibold uppercase text-muted">
              <tr>
                <th className="px-4 py-3">원본명</th>
                <th className="px-4 py-3">확장자</th>
                <th className="px-4 py-3">크기</th>
                <th className="px-4 py-3">등록자</th>
                <th className="px-4 py-3">등록일시</th>
                <th className="px-4 py-3">악성검사결과</th>
                <th className="px-4 py-3">다운로드</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {state.attachments.map((file) => (
                <tr data-testid="attachment-metadata-row" key={file.fileId}>
                  <td className="px-4 py-3 font-medium text-dark">
                    {file.originalFilename}
                  </td>
                  <td className="px-4 py-3 text-muted">{file.extension}</td>
                  <td className="px-4 py-3 text-muted">
                    {formatBytes(file.fileSizeBytes)}
                  </td>
                  <td className="px-4 py-3 text-muted">{file.uploadedBy}</td>
                  <td className="px-4 py-3 text-muted">
                    {formatDate(file.uploadedAt)}
                  </td>
                  <td className="px-4 py-3 text-muted">
                    {scanLabel(file.malwareScanStatus)}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      data-testid="attachment-download-button"
                      type="button"
                      className="inline-flex h-9 items-center gap-2 rounded-md border border-primary px-3 text-sm font-medium text-primary transition-colors hover:bg-lightprimary"
                      onClick={() => void download(file)}
                    >
                      <Download size={15} /> 다운로드
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="border-t border-ld px-6 py-3 text-xs text-muted">
          내부 저장 경로(storage_path)와 실제 저장 파일명(stored_filename)은
          API/UI에 노출하지 않습니다.
        </p>
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

function filterByFilename(
  attachments: AttachmentMetadataRow[],
  filter: string,
): AttachmentMetadataRow[] {
  const normalized = filter.trim().toLowerCase();
  if (!normalized) return attachments;
  return attachments.filter((file) =>
    file.originalFilename.toLowerCase().includes(normalized),
  );
}

function normalizePageSize(size: number): 20 | 50 | 100 {
  return size === 50 || size === 100 ? size : 20;
}

function formatBytes(bytes: number): string {
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${bytes} B`;
}

function formatDate(value: string): string {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
}

function scanLabel(status: string): string {
  const labels: Record<string, string> = {
    CLEAN: "정상",
    INFECTED: "감염",
    FAILED: "검사실패",
    TIMEOUT: "시간초과",
    PENDING: "검사대기",
  };
  return labels[status] ?? status;
}

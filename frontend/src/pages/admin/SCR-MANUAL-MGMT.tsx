import { useEffect, useState } from "react";
import { BookOpen, Download, RefreshCw, Save, Search } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type ManualType = "USER" | "ADMIN";

export type ManualRow = {
  manualId: number;
  manualType: ManualType;
  version: string;
  targetUser: string;
  effectiveDate: string;
  originalFileName: string;
  latest: boolean;
  createdAt?: string;
  createdBy?: number;
  updatedAt?: string;
  updatedBy?: number;
};

type ManualSearchResponse = {
  manuals: ManualRow[];
  page: number;
  size: number;
  totalElements: number;
};

type ManualForm = {
  manualType: ManualType;
  version: string;
  targetUser: string;
  effectiveDate: string;
  originalFileName: string;
  fileContent: string;
  changeReason: string;
};

type ManualListParams = {
  manualType?: string;
  targetUser?: string;
  effectiveDate?: string;
  page?: number;
  size?: number;
};

type ManualCreatePayload = {
  manualType: ManualType;
  version: string;
  targetUser: string;
  effectiveDate: string;
  originalFileName: string;
  fileContent: string;
  changeReason: string;
};

type ScreenStatus =
  | "idle"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success";

type ManualManagementState = {
  status: ScreenStatus;
  manuals: ManualRow[];
  message?: string;
};

type ManualManagementAction =
  | { type: "loading" }
  | { type: "loaded"; manuals: ManualRow[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string };

const emptyManual: ManualForm = {
  manualType: "USER",
  version: "",
  targetUser: "",
  effectiveDate: "",
  originalFileName: "",
  fileContent: "",
  changeReason: "",
};

export function getManualManagementRouteContract() {
  return {
    route: "/admin/manuals",
    screenId: "SCR-MANUAL-MGMT",
    operations: ["listManuals", "createManual", "downloadManualFile"],
  } as const;
}

export function createEmptyManualManagementState(): ManualManagementState {
  return { status: "idle", manuals: [] };
}

export function reduceManualManagementState(
  state: ManualManagementState,
  action: ManualManagementAction,
): ManualManagementState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.manuals.length === 0 ? "empty" : "loaded",
        manuals: action.manuals,
        message: undefined,
      };
    case "error":
      return { ...state, status: "error", message: action.message };
    case "permission":
      return { ...state, status: "permission", message: "권한 없음" };
    case "success":
      return { ...state, status: "success", message: action.message };
    default:
      return state;
  }
}

export const manualManagementApi = {
  uiMessages: {
    createConfirm(manualType: string, version: string) {
      return `${manualType} ${version.trim()} 매뉴얼을 등록하시겠습니까?`;
    },
    createSuccess: "매뉴얼이 등록되었습니다.",
    downloadSuccess: "매뉴얼 파일 다운로드를 시작했습니다.",
    error: "매뉴얼 정보를 처리하지 못했습니다.",
  },
  paths: {
    list(params: ManualListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      if (params.manualType?.trim())
        query.set("manualType", params.manualType.trim());
      if (params.targetUser?.trim())
        query.set("targetUser", params.targetUser.trim());
      if (params.effectiveDate?.trim())
        query.set("effectiveDate", params.effectiveDate.trim());
      return `/api/admin/manuals?${query.toString()}` as `/api/${string}`;
    },
    create() {
      return "/api/admin/manuals" as `/api/${string}`;
    },
    download(manualId: number) {
      return `/api/admin/manuals/${encodeURIComponent(String(manualId))}/download` as `/api/${string}`;
    },
  },
  toCreatePayload(form: ManualForm) {
    return {
      manualType: form.manualType,
      version: form.version.trim(),
      targetUser: form.targetUser.trim(),
      effectiveDate: form.effectiveDate,
      originalFileName: form.originalFileName.trim(),
      fileContent: form.fileContent,
      changeReason: form.changeReason.trim(),
    } satisfies ManualCreatePayload;
  },
  list(params: ManualListParams = {}) {
    return apiRequest<ManualSearchResponse>(
      manualManagementApi.paths.list(params),
    );
  },
  create(payload: ManualCreatePayload) {
    return apiRequest<ManualRow>(manualManagementApi.paths.create(), {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  async download(row: Pick<ManualRow, "manualId" | "originalFileName">) {
    const response = await fetch(
      manualManagementApi.paths.download(row.manualId),
      {
        credentials: "include",
      },
    );
    if (!response.ok) {
      throw new ApiClientError(
        response.status,
        "매뉴얼 파일 다운로드에 실패했습니다.",
      );
    }
    return { fileName: row.originalFileName, blob: await response.blob() };
  },
};

export function ManualManagementPage() {
  const [manualType, setManualType] = useState("");
  const [targetUser, setTargetUser] = useState("");
  const [effectiveDate, setEffectiveDate] = useState("");
  const [pageSize, setPageSize] = useState(20);
  const [state, setState] = useState<ManualManagementState>(
    createEmptyManualManagementState(),
  );
  const [form, setForm] = useState<ManualForm>(emptyManual);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadManuals = async () => {
    setState((current) =>
      reduceManualManagementState(current, { type: "loading" }),
    );
    setFieldErrors({});
    try {
      const response = await manualManagementApi.list({
        manualType,
        targetUser,
        effectiveDate,
        page: 0,
        size: pageSize,
      });
      setState((current) =>
        reduceManualManagementState(current, {
          type: "loaded",
          manuals: response.data?.manuals ?? [],
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadManuals();
  }, []);

  const resetFilters = () => {
    setManualType("");
    setTargetUser("");
    setEffectiveDate("");
    setPageSize(20);
    setState(createEmptyManualManagementState());
  };

  const validateLocal = () => {
    const next: Record<string, string> = {};
    if (!form.version.trim()) next.version = "버전은 필수입니다.";
    if (!form.targetUser.trim()) next.targetUser = "대상 사용자는 필수입니다.";
    if (!form.effectiveDate) next.effectiveDate = "시행일은 필수입니다.";
    if (!form.originalFileName.trim())
      next.originalFileName = "매뉴얼 파일명은 필수입니다.";
    if (!form.fileContent.trim())
      next.fileContent = "매뉴얼 파일 내용은 필수입니다.";
    if (!form.changeReason.trim())
      next.changeReason = "변경 사유는 필수입니다.";
    setFieldErrors(next);
    return Object.keys(next).length === 0;
  };

  const createManual = async () => {
    if (!validateLocal()) return;
    if (
      !window.confirm(
        manualManagementApi.uiMessages.createConfirm(
          form.manualType,
          form.version,
        ),
      )
    )
      return;
    try {
      setFieldErrors({});
      await manualManagementApi.create(
        manualManagementApi.toCreatePayload(form),
      );
      setForm(emptyManual);
      setState((current) =>
        reduceManualManagementState(current, {
          type: "success",
          message: manualManagementApi.uiMessages.createSuccess,
        }),
      );
      await loadManuals();
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const downloadManual = async (row: ManualRow) => {
    try {
      const { blob, fileName } = await manualManagementApi.download(row);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = fileName;
      anchor.click();
      URL.revokeObjectURL(url);
      setState((current) =>
        reduceManualManagementState(current, {
          type: "success",
          message: manualManagementApi.uiMessages.downloadSuccess,
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const handleManualFileChange = async (file: File | undefined) => {
    if (!file) return;
    setForm({
      ...form,
      originalFileName: file.name,
      fileContent: await file.text(),
    });
    setFieldErrors((current) => ({
      ...current,
      originalFileName: "",
      fileContent: "",
    }));
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceManualManagementState(current, { type: "permission" }),
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
        : manualManagementApi.uiMessages.error;
    setState((current) =>
      reduceManualManagementState(current, { type: "error", message }),
    );
  };

  if (state.status === "permission") {
    return (
      <PermissionState
        title="매뉴얼 관리 권한이 없습니다"
        message="R09 시스템관리자 권한 또는 메뉴 접근권한을 확인하세요."
      />
    );
  }

  return (
    <section
      data-testid="manual-management-page"
      data-screen-id="SCR-MANUAL-MGMT"
      className="space-y-6"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex items-center gap-3">
          <BookOpen className="h-6 w-6 text-primary" aria-hidden="true" />
          <div>
            <h1 className="text-xl font-semibold text-dark">매뉴얼 관리</h1>
            <p className="mt-1 text-sm text-muted">
              시스템 관리 &gt; 공지·도움말 관리 &gt; 매뉴얼 관리
            </p>
          </div>
        </div>
      </div>

      <div className="rounded-md bg-white p-5 shadow-md">
        <div className="grid gap-4 md:grid-cols-5">
          <label className="text-sm font-medium text-dark">
            유형
            <select
              data-testid="manual-type-filter"
              className="mt-1 w-full rounded-md border border-border px-3 py-2"
              value={manualType}
              onChange={(event) => setManualType(event.target.value)}
            >
              <option value="">전체</option>
              <option value="USER">사용자</option>
              <option value="ADMIN">관리자</option>
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            대상 사용자
            <input
              data-testid="manual-target-filter"
              className="mt-1 w-full rounded-md border border-border px-3 py-2"
              value={targetUser}
              onChange={(event) => setTargetUser(event.target.value)}
              placeholder="역할코드 또는 사용자 유형"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            기준 시행일
            <input
              data-testid="manual-effective-date-filter"
              type="date"
              className="mt-1 w-full rounded-md border border-border px-3 py-2"
              value={effectiveDate}
              onChange={(event) => setEffectiveDate(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              data-testid="manual-page-size-select"
              className="mt-1 w-full rounded-md border border-border px-3 py-2"
              value={pageSize}
              onChange={(event) => setPageSize(Number(event.target.value))}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
          <div className="flex items-end gap-2">
            <button
              data-testid="manual-search-button"
              className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              type="button"
              onClick={() => void loadManuals()}
            >
              <Search className="h-4 w-4" aria-hidden="true" /> 조회
            </button>
            <button
              data-testid="manual-reset-button"
              className="inline-flex items-center gap-2 rounded-md border border-border px-4 py-2 text-sm font-semibold text-dark"
              type="button"
              onClick={resetFilters}
            >
              <RefreshCw className="h-4 w-4" aria-hidden="true" /> 초기화
            </button>
          </div>
        </div>
      </div>

      {state.status === "loading" && (
        <LoadingState
          title="매뉴얼 조회 중"
          message="등록된 매뉴얼과 최신 버전을 확인하고 있습니다."
        />
      )}
      {state.status === "empty" && (
        <EmptyState
          title="조회된 매뉴얼이 없습니다"
          message="검색 조건을 변경하거나 신규 매뉴얼을 등록하세요."
        />
      )}
      {state.status === "error" && (
        <ErrorState
          title="매뉴얼 처리 오류"
          message={state.message ?? "입력값을 확인한 뒤 다시 시도하세요."}
        />
      )}
      {state.status === "success" && (
        <SuccessState
          title="처리 완료"
          message={state.message ?? "요청이 처리되었습니다."}
        />
      )}

      <div className="overflow-hidden rounded-md bg-white shadow-md">
        <table className="min-w-full divide-y divide-border text-sm">
          <thead className="bg-lightsurface text-left text-muted">
            <tr>
              <th className="px-4 py-3">유형</th>
              <th className="px-4 py-3">버전</th>
              <th className="px-4 py-3">대상 사용자</th>
              <th className="px-4 py-3">시행일</th>
              <th className="px-4 py-3">파일 원본명</th>
              <th className="px-4 py-3">상태</th>
              <th className="px-4 py-3">작업</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {state.manuals.map((row) => (
              <tr
                data-testid={`manual-row-${row.manualId}`}
                key={row.manualId}
                className="hover:bg-lightsecondary"
              >
                <td className="px-4 py-3 font-medium text-dark">
                  {row.manualType}
                </td>
                <td className="px-4 py-3">{row.version}</td>
                <td className="px-4 py-3">{row.targetUser}</td>
                <td className="px-4 py-3">{row.effectiveDate}</td>
                <td className="px-4 py-3">{row.originalFileName}</td>
                <td className="px-4 py-3">
                  {row.latest ? "최신" : "이전 버전"}
                </td>
                <td className="px-4 py-3">
                  <button
                    data-testid={`manual-download-${row.manualId}`}
                    type="button"
                    className="inline-flex items-center gap-1 rounded-md border border-border px-3 py-1.5 text-xs font-semibold text-dark"
                    onClick={() => void downloadManual(row)}
                  >
                    <Download className="h-4 w-4" aria-hidden="true" /> 다운로드
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="rounded-md bg-white p-5 shadow-md">
        <h2 className="text-lg font-semibold text-dark">매뉴얼 등록</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <label className="text-sm font-medium text-dark">
            매뉴얼 유형*
            <select
              data-testid="manual-form-type"
              className="mt-1 w-full rounded-md border border-border px-3 py-2"
              value={form.manualType}
              onChange={(event) =>
                setForm({
                  ...form,
                  manualType: event.target.value as ManualType,
                })
              }
            >
              <option value="USER">사용자</option>
              <option value="ADMIN">관리자</option>
            </select>
          </label>
          <TextField
            testId="manual-form-version"
            label="버전*"
            value={form.version}
            error={fieldErrors.version}
            onChange={(value) => setForm({ ...form, version: value })}
          />
          <TextField
            testId="manual-form-target-user"
            label="대상 사용자*"
            value={form.targetUser}
            error={fieldErrors.targetUser}
            onChange={(value) => setForm({ ...form, targetUser: value })}
          />
          <TextField
            testId="manual-form-effective-date"
            label="시행일*"
            type="date"
            value={form.effectiveDate}
            error={fieldErrors.effectiveDate}
            onChange={(value) => setForm({ ...form, effectiveDate: value })}
          />
          <TextField
            testId="manual-form-original-file-name"
            label="매뉴얼 파일 원본명*"
            value={form.originalFileName}
            error={fieldErrors.originalFileName}
            onChange={(value) => setForm({ ...form, originalFileName: value })}
          />
          <TextField
            testId="manual-form-change-reason"
            label="변경 사유*"
            value={form.changeReason}
            error={fieldErrors.changeReason}
            onChange={(value) => setForm({ ...form, changeReason: value })}
          />
          <label className="text-sm font-medium text-dark md:col-span-2">
            매뉴얼 파일*
            <input
              data-testid="manual-form-file-input"
              type="file"
              className="mt-1 w-full rounded-md border border-border px-3 py-2"
              onChange={(event) =>
                void handleManualFileChange(event.target.files?.[0])
              }
            />
            <span className="mt-1 block text-xs text-muted">
              선택한 파일의 원본명과 내용을 보존하고 내부 저장 식별자는 화면에
              표시하지 않습니다.
            </span>
            {fieldErrors.fileContent && (
              <span className="mt-1 block text-xs text-error">
                {fieldErrors.fileContent}
              </span>
            )}
          </label>
        </div>
        <div className="mt-4 flex gap-2">
          <button
            data-testid="manual-create-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void createManual()}
          >
            <Save className="h-4 w-4" aria-hidden="true" /> 등록
          </button>
          <button
            data-testid="manual-cancel-button"
            type="button"
            className="rounded-md border border-border px-4 py-2 text-sm font-semibold text-dark"
            onClick={() => {
              setForm(emptyManual);
              setFieldErrors({});
            }}
          >
            취소
          </button>
        </div>
      </div>
    </section>
  );
}

function TextField({
  testId,
  label,
  value,
  error,
  type = "text",
  onChange,
}: {
  testId: string;
  label: string;
  value: string;
  error?: string;
  type?: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="text-sm font-medium text-dark">
      {label}
      <input
        data-testid={testId}
        type={type}
        className="mt-1 w-full rounded-md border border-border px-3 py-2"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      {error && <span className="mt-1 block text-xs text-error">{error}</span>}
    </label>
  );
}

import type React from "react";
import { useEffect, useState } from "react";
import { CalendarClock, Download, RefreshCw, Save, Search } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type BatchDefinitionRow = {
  batchId: string;
  batchType?: string | null;
  scheduleCycle: string;
  maxExecutionSeconds?: number | null;
  ownerUserId: number;
  ownerName?: string | null;
  predecessorBatchIds: string[];
  successorBatchIds: string[];
  parameters?: Record<string, unknown>;
  updatedAt?: string;
  updatedBy?: number;
};

type BatchDefinitionSearchResponse = {
  definitions: BatchDefinitionRow[];
  page: number;
  size: number;
  totalElements: number;
};

type BatchDefinitionForm = {
  batchId: string;
  batchType: string;
  scheduleCycle: string;
  ownerUserId: string;
  maxExecutionSeconds: string;
  predecessorBatchIds: string;
  successorBatchIds: string;
  parametersText: string;
};

type BatchDefinitionPayload = {
  batchId: string;
  batchType?: string;
  scheduleCycle: string;
  predecessorBatchIds: string[];
  successorBatchIds: string[];
  parameters: Record<string, unknown>;
  maxExecutionSeconds?: number;
  ownerUserId: number;
};

type BatchDefinitionListParams = {
  batchId?: string;
  batchType?: string;
  scheduleCycle?: string;
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

type BatchDefinitionState = {
  status: ScreenStatus;
  definitions: BatchDefinitionRow[];
  message?: string;
};

type BatchDefinitionAction =
  | { type: "loading" }
  | { type: "loaded"; definitions: BatchDefinitionRow[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string };

export function getBatchDefinitionRouteContract() {
  return {
    route: "/admin/batch-definitions",
    screenId: "SCR-BATCH-DEFINITION-MGMT",
    operations: ["listBatchDefinitions", "saveBatchDefinition"],
  } as const;
}

export function createEmptyBatchDefinitionState(): BatchDefinitionState {
  return { status: "idle", definitions: [] };
}

export function reduceBatchDefinitionState(
  state: BatchDefinitionState,
  action: BatchDefinitionAction,
): BatchDefinitionState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.definitions.length === 0 ? "empty" : "loaded",
        definitions: action.definitions,
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

const emptyForm: BatchDefinitionForm = {
  batchId: "",
  batchType: "",
  scheduleCycle: "",
  ownerUserId: "",
  maxExecutionSeconds: "",
  predecessorBatchIds: "",
  successorBatchIds: "",
  parametersText: "{}",
};

export const BATCH_PAGE_SIZE_OPTIONS = [20, 50, 100] as const;

export const batchDefinitionApi = {
  uiMessages: {
    saveConfirm(batchId: string) {
      return `${batchId.trim()} 배치 정의를 저장하시겠습니까?`;
    },
    saveSuccess: "배치 정의가 저장되었습니다.",
    error: "배치 정의 정보를 처리하지 못했습니다.",
  },
  excelDownloadOq:
    "REQ-386 OQ: 기존 공통 export 패턴이 없어 엑셀 다운로드는 reviewer 확인 후 연결합니다.",
  pageSizeOptions: BATCH_PAGE_SIZE_OPTIONS,
  validateForm(form: BatchDefinitionForm) {
    const next: Record<string, string> = {};
    if (!form.batchId.trim()) next.batchId = "배치ID는 필수입니다.";
    if (!form.batchType.trim()) next.batchType = "업무유형은 필수입니다.";
    if (!form.scheduleCycle.trim())
      next.scheduleCycle = "실행주기는 필수입니다.";
    if (!form.ownerUserId.trim()) next.ownerUserId = "담당자는 필수입니다.";
    if (form.ownerUserId.trim() && Number.isNaN(Number(form.ownerUserId))) {
      next.ownerUserId = "담당자는 숫자 사용자 ID로 입력하세요.";
    }
    if (
      form.maxExecutionSeconds.trim() &&
      Number.isNaN(Number(form.maxExecutionSeconds))
    ) {
      next.maxExecutionSeconds = "최대실행시간은 숫자로 입력하세요.";
    }
    try {
      JSON.parse(form.parametersText.trim() || "{}");
    } catch {
      next.parametersText = "실행 파라미터는 올바른 JSON이어야 합니다.";
    }
    return next;
  },
  paths: {
    list(params: BatchDefinitionListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      if (params.batchId?.trim()) query.set("batchId", params.batchId.trim());
      if (params.batchType?.trim())
        query.set("batchType", params.batchType.trim());
      if (params.scheduleCycle?.trim())
        query.set("scheduleCycle", params.scheduleCycle.trim());
      return `/api/admin/batch-definitions?${query.toString()}` as `/api/${string}`;
    },
    save() {
      return "/api/admin/batch-definitions" as const;
    },
  },
  parseIdList(value: string) {
    return value
      .split(/[\n,]/)
      .map((item) => item.trim())
      .filter(Boolean);
  },
  toSavePayload(form: BatchDefinitionForm): BatchDefinitionPayload {
    const parsedParameters = form.parametersText.trim()
      ? (JSON.parse(form.parametersText) as Record<string, unknown>)
      : {};
    return {
      batchId: form.batchId.trim(),
      batchType: form.batchType.trim() || undefined,
      scheduleCycle: form.scheduleCycle.trim(),
      predecessorBatchIds: batchDefinitionApi.parseIdList(
        form.predecessorBatchIds,
      ),
      successorBatchIds: batchDefinitionApi.parseIdList(form.successorBatchIds),
      parameters: parsedParameters,
      maxExecutionSeconds: form.maxExecutionSeconds.trim()
        ? Number(form.maxExecutionSeconds)
        : undefined,
      ownerUserId: Number(form.ownerUserId),
    };
  },
  list(params: BatchDefinitionListParams = {}) {
    return apiRequest<BatchDefinitionSearchResponse>(
      batchDefinitionApi.paths.list(params),
    );
  },
  save(payload: BatchDefinitionPayload) {
    return apiRequest<BatchDefinitionRow>(batchDefinitionApi.paths.save(), {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};

export function BatchDefinitionManagementPage() {
  const [batchId, setBatchId] = useState("");
  const [batchType, setBatchType] = useState("");
  const [pageSize, setPageSize] = useState(20);
  const [state, setState] = useState<BatchDefinitionState>(
    createEmptyBatchDefinitionState(),
  );
  const [selected, setSelected] = useState<BatchDefinitionRow | null>(null);
  const [form, setForm] = useState<BatchDefinitionForm>(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadDefinitions = async () => {
    setState((current) =>
      reduceBatchDefinitionState(current, { type: "loading" }),
    );
    setFieldErrors({});
    try {
      const response = await batchDefinitionApi.list({
        batchId,
        batchType,
        page: 0,
        size: pageSize,
      });
      const definitions = response.data?.definitions ?? [];
      setState((current) =>
        reduceBatchDefinitionState(current, { type: "loaded", definitions }),
      );
      if (selected) {
        const refreshed =
          definitions.find((row) => row.batchId === selected.batchId) ?? null;
        if (refreshed) applySelected(refreshed);
      }
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadDefinitions();
  }, []);

  const applySelected = (row: BatchDefinitionRow) => {
    setSelected(row);
    setForm({
      batchId: row.batchId,
      batchType: row.batchType ?? "",
      scheduleCycle: row.scheduleCycle,
      ownerUserId: String(row.ownerUserId),
      maxExecutionSeconds:
        row.maxExecutionSeconds === null ||
        row.maxExecutionSeconds === undefined
          ? ""
          : String(row.maxExecutionSeconds),
      predecessorBatchIds: row.predecessorBatchIds.join(", "),
      successorBatchIds: row.successorBatchIds.join(", "),
      parametersText: JSON.stringify(row.parameters ?? {}, null, 2),
    });
    setFieldErrors({});
  };

  const resetForm = () => {
    setSelected(null);
    setForm(emptyForm);
    setFieldErrors({});
  };

  const validateLocal = () => {
    const next = batchDefinitionApi.validateForm(form);
    setFieldErrors(next);
    return Object.keys(next).length === 0;
  };

  const saveDefinition = async () => {
    if (!validateLocal()) return;
    if (
      !window.confirm(batchDefinitionApi.uiMessages.saveConfirm(form.batchId))
    )
      return;
    try {
      setFieldErrors({});
      const response = await batchDefinitionApi.save(
        batchDefinitionApi.toSavePayload(form),
      );
      if (response.data) applySelected(response.data);
      setState((current) =>
        reduceBatchDefinitionState(current, {
          type: "success",
          message: batchDefinitionApi.uiMessages.saveSuccess,
        }),
      );
      await loadDefinitions();
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceBatchDefinitionState(current, { type: "permission" }),
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
        : batchDefinitionApi.uiMessages.error;
    setState((current) =>
      reduceBatchDefinitionState(current, { type: "error", message }),
    );
  };

  if (state.status === "permission") {
    return (
      <PermissionState
        title="배치 정의 관리 권한이 없습니다"
        message="R09 시스템관리자 권한 또는 메뉴 접근권한을 확인하세요."
      />
    );
  }

  return (
    <section
      data-testid="batch-definition-management-screen"
      data-screen-id="SCR-BATCH-DEFINITION-MGMT"
      className="space-y-6"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-muted">
          시스템 운영 관리 &gt; 배치작업 관리 &gt; 배치 정의 관리
        </p>
        <div className="mt-2 flex items-center gap-3">
          <CalendarClock className="h-6 w-6 text-primary" aria-hidden />
          <div>
            <h1 className="text-xl font-semibold text-dark">배치 정의 관리</h1>
            <p className="mt-1 text-sm text-muted">
              배치ID, 업무유형, 실행주기, 선후행 관계, 파라미터와 담당자를
              저장하고 재조회합니다.
            </p>
          </div>
        </div>
      </div>

      {state.status === "loading" && <LoadingState title="배치 정의 조회 중" />}
      {state.status === "empty" && (
        <EmptyState title="조회된 배치 정의가 없습니다" />
      )}
      {state.status === "error" && (
        <ErrorState
          title="배치 정의 처리 오류"
          message={state.message ?? "오류가 발생했습니다."}
        />
      )}
      {state.status === "success" && (
        <SuccessState title="처리 완료" message={state.message} />
      )}

      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <div className="grid gap-4 lg:grid-cols-4">
          <label className="text-sm font-medium text-dark">
            검색 batchId
            <input
              data-testid="batch-definition-batch-id-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={batchId}
              onChange={(event) => setBatchId(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            업무유형
            <input
              data-testid="batch-definition-type-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={batchType}
              onChange={(event) => setBatchType(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시건수
            <select
              data-testid="batch-definition-page-size-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={pageSize}
              onChange={(event) => setPageSize(Number(event.target.value))}
            >
              {BATCH_PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
          </label>
          <div className="flex items-end gap-2">
            <button
              data-testid="batch-definition-search-button"
              type="button"
              className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              onClick={() => void loadDefinitions()}
            >
              <Search size={16} /> 조회
            </button>
            <button
              data-testid="batch-definition-export-button"
              type="button"
              className="inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
              onClick={() => window.alert(batchDefinitionApi.excelDownloadOq)}
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
                <th className="px-4 py-3">batchId</th>
                <th className="px-4 py-3">업무유형</th>
                <th className="px-4 py-3">실행주기</th>
                <th className="px-4 py-3">선행/후행</th>
                <th className="px-4 py-3">최대실행시간</th>
                <th className="px-4 py-3">담당자</th>
                <th className="px-4 py-3">선택</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {state.definitions.map((row) => (
                <tr key={row.batchId} data-testid="batch-definition-row">
                  <td className="px-4 py-3 font-semibold text-dark">
                    {row.batchId}
                  </td>
                  <td className="px-4 py-3">{row.batchType ?? "-"}</td>
                  <td className="px-4 py-3">{row.scheduleCycle}</td>
                  <td className="px-4 py-3">
                    선행 {row.predecessorBatchIds.length} / 후행{" "}
                    {row.successorBatchIds.length}
                  </td>
                  <td className="px-4 py-3">
                    {row.maxExecutionSeconds ?? "-"}
                  </td>
                  <td className="px-4 py-3">
                    {row.ownerName ?? row.ownerUserId}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      data-testid="batch-definition-select-button"
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
        <h2 className="text-lg font-semibold text-dark">
          {selected ? "배치 정의 수정" : "배치 정의 등록"}
        </h2>
        <div className="mt-4 grid gap-4 lg:grid-cols-4">
          <Field label="배치ID*" error={fieldErrors.batchId}>
            <input
              data-testid="batch-definition-batch-id-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={form.batchId}
              onChange={(event) =>
                setForm({ ...form, batchId: event.target.value })
              }
            />
          </Field>
          <Field label="업무유형*" error={fieldErrors.batchType}>
            <input
              data-testid="batch-definition-type-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={form.batchType}
              onChange={(event) =>
                setForm({ ...form, batchType: event.target.value })
              }
            />
          </Field>
          <Field label="실행주기*" error={fieldErrors.scheduleCycle}>
            <input
              data-testid="batch-definition-schedule-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={form.scheduleCycle}
              onChange={(event) =>
                setForm({ ...form, scheduleCycle: event.target.value })
              }
            />
          </Field>
          <Field label="담당자*" error={fieldErrors.ownerUserId}>
            <input
              data-testid="batch-definition-owner-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={form.ownerUserId}
              onChange={(event) =>
                setForm({ ...form, ownerUserId: event.target.value })
              }
            />
          </Field>
          <Field
            label="최대실행시간(초)"
            error={fieldErrors.maxExecutionSeconds}
          >
            <input
              data-testid="batch-definition-max-seconds-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={form.maxExecutionSeconds}
              onChange={(event) =>
                setForm({ ...form, maxExecutionSeconds: event.target.value })
              }
            />
          </Field>
          <Field label="선행 배치">
            <input
              data-testid="batch-definition-predecessor-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={form.predecessorBatchIds}
              onChange={(event) =>
                setForm({ ...form, predecessorBatchIds: event.target.value })
              }
            />
          </Field>
          <Field label="후행 배치">
            <input
              data-testid="batch-definition-successor-input"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={form.successorBatchIds}
              onChange={(event) =>
                setForm({ ...form, successorBatchIds: event.target.value })
              }
            />
          </Field>
          <label className="text-sm font-medium text-dark lg:col-span-4">
            실행 파라미터 JSON
            <textarea
              data-testid="batch-definition-parameters-textarea"
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
            data-testid="batch-definition-save-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void saveDefinition()}
          >
            <Save size={16} /> 저장
          </button>
          <button
            data-testid="batch-definition-cancel-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
            onClick={resetForm}
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

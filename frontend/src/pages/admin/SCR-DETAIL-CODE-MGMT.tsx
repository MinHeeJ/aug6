import type React from "react";
import { useEffect, useMemo, useState } from "react";
import {
  GitBranch,
  ListChecks,
  PlusCircle,
  RefreshCw,
  Save,
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

export type DetailCodeSummary = {
  groupId: string;
  codeValue: string;
  codeName: string;
  parentCodeValue?: string | null;
  sortOrder: number;
  additionalAttributes?: string | null;
  systemUseYn: "Y" | "N";
  validStartDate?: string | null;
  validEndDate?: string | null;
  status: string;
  createdAt?: string;
  updatedAt?: string;
};

type DetailCodeForm = {
  codeValue: string;
  codeName: string;
  parentCodeValue: string;
  sortOrder: string;
  additionalAttributesText: string;
  systemUseYn: "Y" | "N";
  validStartDate: string;
  validEndDate: string;
  changeReason: string;
};

type DetailCodePayload = {
  codeValue: string;
  codeName: string;
  parentCodeValue?: string;
  sortOrder: number;
  additionalAttributes?: Record<string, unknown>;
  systemUseYn: "Y" | "N";
  validStartDate?: string;
  validEndDate?: string;
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

type DetailCodeManagementState = {
  status: ScreenStatus;
  detailCodes: DetailCodeSummary[];
  message?: string;
};

type DetailCodeManagementAction =
  | { type: "loading" }
  | { type: "loaded"; detailCodes: DetailCodeSummary[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string };

export function getDetailCodeManagementRouteContract() {
  return {
    route: "/admin/detail-codes",
    screenId: "SCR-DETAIL-CODE-MGMT",
    operations: ["listDetailCodes", "createDetailCode", "updateDetailCode"],
  } as const;
}

export function createEmptyDetailCodeManagementState(): DetailCodeManagementState {
  return { status: "idle", detailCodes: [] };
}

export function reduceDetailCodeManagementState(
  state: DetailCodeManagementState,
  action: DetailCodeManagementAction,
): DetailCodeManagementState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.detailCodes.length === 0 ? "empty" : "loaded",
        detailCodes: action.detailCodes,
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

const emptyForm: DetailCodeForm = {
  codeValue: "",
  codeName: "",
  parentCodeValue: "",
  sortOrder: "0",
  additionalAttributesText: "",
  systemUseYn: "Y",
  validStartDate: "",
  validEndDate: "",
  changeReason: "",
};

export const detailCodeManagementApi = {
  paths: {
    list(
      groupId: string,
      params: { page?: number; size?: number; filter?: string } = {},
    ) {
      const query = new URLSearchParams();
      if (params.page !== undefined) query.set("page", String(params.page));
      if (params.size !== undefined) query.set("size", String(params.size));
      if (params.filter?.trim()) query.set("filter", params.filter.trim());
      const suffix = query.toString();
      return `/api/admin/code-groups/${encodeURIComponent(groupId)}/codes${suffix ? `?${suffix}` : ""}` as `/api/${string}`;
    },

    create(groupId: string) {
      return `/api/admin/code-groups/${encodeURIComponent(groupId)}/codes` as `/api/${string}`;
    },
    update(groupId: string, codeValue: string) {
      return `/api/admin/code-groups/${encodeURIComponent(groupId)}/codes/${encodeURIComponent(codeValue)}` as `/api/${string}`;
    },
  },
  toPayload(form: DetailCodeForm): DetailCodePayload {
    return {
      codeValue: form.codeValue.trim().toUpperCase(),
      codeName: form.codeName,
      parentCodeValue: form.parentCodeValue.trim()
        ? form.parentCodeValue.trim().toUpperCase()
        : undefined,
      sortOrder: Number.parseInt(form.sortOrder || "0", 10),
      // REQ-062 OQ: 추가속성 구조·개수·형식이 확정되지 않아 화면에서 임의 payload를 전송하지 않는다.
      additionalAttributes: undefined,
      systemUseYn: form.systemUseYn,
      validStartDate: form.validStartDate || undefined,
      validEndDate: form.validEndDate || undefined,
      changeReason: form.changeReason,
    };
  },
  list(
    groupId: string,
    params: { page?: number; size?: number; filter?: string } = {},
  ) {
    return apiRequest<DetailCodeSummary[]>(
      detailCodeManagementApi.paths.list(groupId, params),
    );
  },
  create(groupId: string, payload: DetailCodePayload) {
    return apiRequest<DetailCodeSummary>(
      detailCodeManagementApi.paths.create(groupId),
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
  update(groupId: string, codeValue: string, payload: DetailCodePayload) {
    return apiRequest<DetailCodeSummary>(
      detailCodeManagementApi.paths.update(groupId, codeValue),
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
};

export function DetailCodeManagementPage() {
  const initialGroupId = useMemo(() => {
    if (typeof window === "undefined") return "";
    return new URLSearchParams(window.location.search).get("groupId") ?? "";
  }, []);
  const [groupId, setGroupId] = useState(initialGroupId);
  const [filter, setFilter] = useState("");
  const [state, setState] = useState<DetailCodeManagementState>(
    createEmptyDetailCodeManagementState(),
  );
  const [selectedCode, setSelectedCode] = useState<DetailCodeSummary | null>(
    null,
  );
  const [formMode, setFormMode] = useState<"create" | "update">("create");
  const [form, setForm] = useState<DetailCodeForm>(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadDetailCodes = async (nextGroupId = groupId) => {
    setState((current) =>
      reduceDetailCodeManagementState(current, { type: "loading" }),
    );
    setFieldErrors({});
    try {
      const response = await detailCodeManagementApi.list(
        nextGroupId.trim().toUpperCase(),
        { page: 0, size: 100, filter },
      );
      const detailCodes = response.data ?? [];
      setState((current) =>
        reduceDetailCodeManagementState(current, {
          type: "loaded",
          detailCodes,
        }),
      );
      if (selectedCode) {
        const refreshed =
          detailCodes.find(
            (code) => code.codeValue === selectedCode.codeValue,
          ) ?? null;
        if (refreshed) applySelectedCode(refreshed);
      }
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    if (initialGroupId.trim()) {
      void loadDetailCodes(initialGroupId);
    }
  }, []);

  const parentOptions = state.detailCodes.filter(
    (code) => selectedCode?.codeValue !== code.codeValue,
  );

  const applySelectedCode = (code: DetailCodeSummary) => {
    setSelectedCode(code);
    setFormMode("update");
    setForm({
      codeValue: code.codeValue,
      codeName: code.codeName,
      parentCodeValue: code.parentCodeValue ?? "",
      sortOrder: String(code.sortOrder),
      additionalAttributesText: code.additionalAttributes ?? "",
      systemUseYn: code.systemUseYn,
      validStartDate: code.validStartDate ?? "",
      validEndDate: code.validEndDate ?? "",
      changeReason: "",
    });
    setFieldErrors({});
  };

  const startCreate = () => {
    if (!groupId.trim()) {
      setFieldErrors({ groupId: "코드그룹을 먼저 지정하세요." });
      setState((current) =>
        reduceDetailCodeManagementState(current, {
          type: "error",
          message: "코드그룹을 먼저 지정하세요.",
        }),
      );
      return;
    }
    setSelectedCode(null);
    setFormMode("create");
    setForm(emptyForm);
    setFieldErrors({});
  };

  const cancelEdit = () => {
    if (selectedCode) {
      applySelectedCode(selectedCode);
      return;
    }
    setFormMode("create");
    setForm(emptyForm);
    setFieldErrors({});
  };

  const resetFilter = () => {
    setGroupId("");
    setFilter("");
    setSelectedCode(null);
    setFormMode("create");
    setForm(emptyForm);
    setFieldErrors({});
    setState(createEmptyDetailCodeManagementState());
  };

  const saveDetailCode = async () => {
    const normalizedGroupId = groupId.trim().toUpperCase();
    if (!normalizedGroupId) {
      setFieldErrors({ groupId: "코드그룹을 먼저 지정하세요." });
      setState((current) =>
        reduceDetailCodeManagementState(current, {
          type: "error",
          message: "코드그룹을 먼저 지정하세요.",
        }),
      );
      return;
    }
    const label = formMode === "create" ? "등록" : "수정";
    const confirmed = window.confirm(
      `${normalizedGroupId}/${form.codeValue || "신규 상세코드"}를 ${label}하시겠습니까?`,
    );
    if (!confirmed) return;
    try {
      setFieldErrors({});
      const payload = detailCodeManagementApi.toPayload(form);
      const response =
        formMode === "create"
          ? await detailCodeManagementApi.create(normalizedGroupId, payload)
          : await detailCodeManagementApi.update(
              normalizedGroupId,
              selectedCode?.codeValue ?? payload.codeValue,
              payload,
            );
      const saved = response.data;
      if (saved) applySelectedCode(saved);
      await loadDetailCodes(normalizedGroupId);
      setState((current) =>
        reduceDetailCodeManagementState(current, {
          type: "success",
          message: `상세코드가 ${label}되었습니다.`,
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceDetailCodeManagementState(current, { type: "permission" }),
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
      caught instanceof Error
        ? caught.message
        : "상세코드 정보를 처리하지 못했습니다.";
    setState((current) =>
      reduceDetailCodeManagementState(current, { type: "error", message }),
    );
  };

  if (state.status === "permission") {
    return (
      <PermissionState
        title="상세코드 관리 권한 없음"
        message="R09 시스템관리자 또는 상세코드 관리 메뉴 접근 권한이 필요합니다."
      />
    );
  }

  return (
    <section data-screen-id="SCR-DETAIL-CODE-MGMT" className="space-y-6">
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              시스템 관리 · 공통코드 관리 · 상세코드 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              상세코드 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              코드그룹별 코드값, 코드명, 상위코드, 정렬순서, 사용여부와
              유효기간을 조회·등록·수정합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            onClick={() => void loadDetailCodes()}
            disabled={state.status === "loading"}
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      {state.status === "error" ? (
        <ErrorState title="상세코드 관리 오류" message={state.message} />
      ) : null}
      {state.status === "success" ? (
        <SuccessState title="저장 완료" message={state.message} />
      ) : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <div className="grid grid-cols-12 gap-5 md:gap-6">
          <label className="col-span-12 block text-sm font-semibold text-link md:col-span-4">
            코드그룹 ID
            <div className="relative mt-2">
              <Search
                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                size={16}
              />
              <input
                className="h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 pl-10 text-sm focus-visible:border-primary focus-visible:outline-0"
                value={groupId}
                onChange={(event) =>
                  setGroupId(event.target.value.toUpperCase())
                }
                placeholder="COMMON_STATUS"
              />
            </div>
          </label>
          <label className="col-span-12 block text-sm font-semibold text-link md:col-span-5">
            검색어
            <div className="relative mt-2">
              <Search
                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                size={16}
              />
              <input
                className="h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 pl-10 text-sm focus-visible:border-primary focus-visible:outline-0"
                value={filter}
                onChange={(event) => setFilter(event.target.value)}
                placeholder="코드값/코드명/상위코드"
              />
            </div>
          </label>
          <div className="col-span-12 flex items-end gap-2 md:col-span-3">
            <button
              type="button"
              className="h-10 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary hover:bg-primary hover:text-white"
              onClick={resetFilter}
            >
              초기화
            </button>
            <button
              type="button"
              className="h-10 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              onClick={() => void loadDetailCodes()}
              disabled={state.status === "loading"}
            >
              조회
            </button>
          </div>
        </div>
      </section>

      <div className="grid grid-cols-12 gap-6">
        <section className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md xl:col-span-8">
          <div className="flex items-center justify-between">
            <h2 className="flex items-center gap-2 text-lg font-semibold text-dark">
              <ListChecks size={18} /> 상세코드 목록
            </h2>
            <div className="flex items-center gap-2">
              <span className="rounded-full bg-lightprimary px-3 py-1 text-sm font-semibold text-primary">
                {state.detailCodes.length}건
              </span>
              <button
                type="button"
                className="inline-flex h-9 items-center gap-2 rounded-md bg-primary px-3 py-2 text-sm font-semibold text-white"
                onClick={startCreate}
              >
                <PlusCircle size={16} /> 신규 등록
              </button>
            </div>
          </div>
          {state.status === "loading" ? (
            <div className="mt-4">
              <LoadingState
                title="상세코드 조회 중"
                message="선택 코드그룹의 상세코드 목록을 조회하고 있습니다."
              />
            </div>
          ) : null}
          {state.status === "empty" ? (
            <div className="mt-4">
              <EmptyState
                title="조회된 상세코드가 없습니다"
                message="코드그룹 ID를 확인하거나 신규 상세코드를 등록하세요."
              />
            </div>
          ) : null}
          {state.detailCodes.length > 0 ? (
            <div className="mt-4 overflow-x-auto rounded-md border border-border">
              <table className="w-full caption-bottom text-sm">
                <thead className="border-b border-ld bg-lightgray text-left text-xs font-semibold uppercase text-lightmuted">
                  <tr>
                    <th className="px-4 py-3">코드값</th>
                    <th className="px-4 py-3">코드명</th>
                    <th className="px-4 py-3">상위코드</th>
                    <th className="px-4 py-3">정렬</th>
                    <th className="px-4 py-3">추가속성</th>
                    <th className="px-4 py-3">사용</th>
                    <th className="px-4 py-3">유효기간</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {state.detailCodes.map((code) => (
                    <tr
                      key={`${code.groupId}-${code.codeValue}`}
                      className={`cursor-pointer transition-colors hover:bg-lightprimary ${selectedCode?.codeValue === code.codeValue ? "bg-lightsecondary" : ""}`}
                      onClick={() => applySelectedCode(code)}
                    >
                      <td className="px-4 py-3 font-semibold text-primary">
                        {code.codeValue}
                      </td>
                      <td className="px-4 py-3 text-link">{code.codeName}</td>
                      <td className="px-4 py-3 text-muted">
                        {code.parentCodeValue ?? "-"}
                      </td>
                      <td className="px-4 py-3 text-muted">{code.sortOrder}</td>
                      <td
                        className="max-w-[180px] truncate px-4 py-3 text-muted"
                        title={code.additionalAttributes ?? ""}
                      >
                        {code.additionalAttributes ?? "-"}
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`rounded-full px-2.5 py-0.5 text-xs font-semibold text-white ${code.systemUseYn === "Y" ? "bg-success" : "bg-warning"}`}
                        >
                          {code.systemUseYn === "Y" ? "사용" : "미사용"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-muted">
                        {code.validStartDate ?? "-"} ~{" "}
                        {code.validEndDate ?? "-"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>

        <aside className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md xl:col-span-4">
          <h2 className="flex items-center gap-2 text-lg font-semibold text-dark">
            <Save size={18} />{" "}
            {formMode === "create"
              ? "신규 상세코드 등록"
              : "선택 상세코드 수정"}
          </h2>
          <p className="mt-2 text-xs text-muted">
            REQ-062: 추가속성 구조가 확정되지 않아 임의 매핑 payload는 전송하지
            않습니다. 코드값은 등록 후 수정할 수 없습니다.
          </p>
          <div className="mt-5 space-y-4">
            <Field label="코드값" error={fieldErrors.codeValue} required>
              <input
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm disabled:bg-lightgray"
                value={form.codeValue}
                onChange={(event) =>
                  setForm({
                    ...form,
                    codeValue: event.target.value.toUpperCase(),
                  })
                }
                disabled={formMode === "update"}
              />
            </Field>
            <Field label="코드명" error={fieldErrors.codeName} required>
              <input
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                value={form.codeName}
                onChange={(event) =>
                  setForm({ ...form, codeName: event.target.value })
                }
              />
            </Field>
            <Field label="상위코드" error={fieldErrors.parentCodeValue}>
              <select
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                value={form.parentCodeValue}
                onChange={(event) =>
                  setForm({ ...form, parentCodeValue: event.target.value })
                }
              >
                <option value="">상위코드 없음</option>
                {parentOptions.map((code) => (
                  <option key={code.codeValue} value={code.codeValue}>
                    {code.codeValue} · {code.codeName}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="정렬순서" error={fieldErrors.sortOrder} required>
              <input
                type="number"
                min="0"
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                value={form.sortOrder}
                onChange={(event) =>
                  setForm({ ...form, sortOrder: event.target.value })
                }
              />
            </Field>
            <Field label="사용여부" error={fieldErrors.systemUseYn} required>
              <select
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                value={form.systemUseYn}
                onChange={(event) =>
                  setForm({
                    ...form,
                    systemUseYn: event.target.value as "Y" | "N",
                  })
                }
              >
                <option value="Y">사용</option>
                <option value="N">미사용</option>
              </select>
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="유효 시작일" error={fieldErrors.validStartDate}>
                <input
                  type="date"
                  className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  value={form.validStartDate}
                  onChange={(event) =>
                    setForm({ ...form, validStartDate: event.target.value })
                  }
                />
              </Field>
              <Field label="유효 종료일" error={fieldErrors.validEndDate}>
                <input
                  type="date"
                  className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  value={form.validEndDate}
                  onChange={(event) =>
                    setForm({ ...form, validEndDate: event.target.value })
                  }
                />
              </Field>
            </div>
            <Field
              label="추가속성 보기"
              error={fieldErrors.additionalAttributes}
            >
              <textarea
                className="min-h-20 w-full rounded-lg border border-ld bg-lightgray px-3 py-2 text-sm"
                value={form.additionalAttributesText}
                onChange={(event) =>
                  setForm({
                    ...form,
                    additionalAttributesText: event.target.value,
                  })
                }
                placeholder="REQ-062 확정 전 저장 제외"
                readOnly
              />
            </Field>
            <div className="rounded-md bg-lightwarning p-3 text-xs text-warning">
              <GitBranch className="mr-2 inline" size={14} />
              상세코드 계층은 같은 코드그룹 안의 등록된 코드값만 상위코드로
              선택할 수 있습니다.
            </div>
            <Field label="변경 사유" error={fieldErrors.changeReason} required>
              <input
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                value={form.changeReason}
                onChange={(event) =>
                  setForm({ ...form, changeReason: event.target.value })
                }
              />
            </Field>
          </div>
          <div className="mt-6 flex gap-2">
            <button
              type="button"
              className="h-10 flex-1 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              onClick={() => void saveDetailCode()}
              disabled={state.status === "loading"}
            >
              저장
            </button>
            <button
              type="button"
              className="h-10 flex-1 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary hover:bg-primary hover:text-white"
              onClick={cancelEdit}
            >
              취소
            </button>
          </div>
        </aside>
      </div>
    </section>
  );
}

function Field({
  label,
  error,
  required,
  children,
}: {
  label: string;
  error?: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <label className="block text-sm font-semibold text-link">
      {label}
      {required ? <span className="ml-1 text-error">*</span> : null}
      <div className="mt-2">{children}</div>
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

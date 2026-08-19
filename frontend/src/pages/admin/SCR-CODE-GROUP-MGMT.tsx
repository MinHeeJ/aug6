import type React from "react";
import { useEffect, useState } from "react";
import {
  ListChecks,
  PlusCircle,
  RefreshCw,
  Save,
  Search,
  Tags,
} from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type CodeGroupSummary = {
  groupId: string;
  groupName: string;
  description?: string;
  managingDepartment: string;
  systemUseYn: "Y" | "N";
  status: string;
  detailCodeCount: number;
  createdAt?: string;
  updatedAt?: string;
};

type CodeGroupForm = {
  groupId: string;
  groupName: string;
  description: string;
  managingDepartment: string;
  systemUseYn: "Y" | "N";
  changeReason: string;
};

type CodeGroupPayload = CodeGroupForm;

type CodeGroupListParams = {
  groupIdFilter?: string;
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

type CodeGroupManagementState = {
  status: ScreenStatus;
  codeGroups: CodeGroupSummary[];
  message?: string;
};

type CodeGroupManagementAction =
  | { type: "loading" }
  | { type: "loaded"; codeGroups: CodeGroupSummary[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string };

export function getCodeGroupManagementRouteContract() {
  return {
    route: "/admin/code-groups",
    screenId: "SCR-CODE-GROUP-MGMT",
    operations: ["listCodeGroups", "createCodeGroup", "updateCodeGroup"],
  } as const;
}

export function createEmptyCodeGroupManagementState(): CodeGroupManagementState {
  return { status: "idle", codeGroups: [] };
}

export function reduceCodeGroupManagementState(
  state: CodeGroupManagementState,
  action: CodeGroupManagementAction,
): CodeGroupManagementState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.codeGroups.length === 0 ? "empty" : "loaded",
        codeGroups: action.codeGroups,
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

const emptyForm: CodeGroupForm = {
  groupId: "",
  groupName: "",
  description: "",
  managingDepartment: "",
  systemUseYn: "Y",
  changeReason: "",
};

export const codeGroupManagementApi = {
  paths: {
    list(params: CodeGroupListParams = {}) {
      const query = new URLSearchParams();
      if (params.groupIdFilter?.trim())
        query.set("groupIdFilter", params.groupIdFilter.trim());
      if (params.filter?.trim()) query.set("filter", params.filter.trim());
      if (params.page !== undefined) query.set("page", String(params.page));
      if (params.size !== undefined) query.set("size", String(params.size));
      const suffix = query.toString();
      return `/api/admin/code-groups${suffix ? `?${suffix}` : ""}` as `/api/${string}`;
    },
    create() {
      return "/api/admin/code-groups" as `/api/${string}`;
    },
    update(groupId: string) {
      return `/api/admin/code-groups/${encodeURIComponent(groupId)}` as `/api/${string}`;
    },
    detailCodes(groupId: string) {
      return `/admin/detail-codes?groupId=${encodeURIComponent(groupId)}`;
    },
  },
  toPayload(form: CodeGroupForm): CodeGroupPayload {
    return {
      groupId: form.groupId.trim().toUpperCase(),
      groupName: form.groupName,
      description: form.description,
      managingDepartment: form.managingDepartment,
      systemUseYn: form.systemUseYn,
      changeReason: form.changeReason,
    };
  },
  list(params: CodeGroupListParams = {}) {
    return apiRequest<CodeGroupSummary[]>(
      codeGroupManagementApi.paths.list(params),
    );
  },
  create(payload: CodeGroupPayload) {
    return apiRequest<CodeGroupSummary>(codeGroupManagementApi.paths.create(), {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  update(groupId: string, payload: CodeGroupPayload) {
    return apiRequest<CodeGroupSummary>(
      codeGroupManagementApi.paths.update(groupId),
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
};

export function CodeGroupManagementPage() {
  const [groupIdFilter, setGroupIdFilter] = useState("");
  const [state, setState] = useState<CodeGroupManagementState>(
    createEmptyCodeGroupManagementState(),
  );
  const [selectedGroup, setSelectedGroup] = useState<CodeGroupSummary | null>(
    null,
  );
  const [formMode, setFormMode] = useState<"create" | "update">("create");
  const [form, setForm] = useState<CodeGroupForm>(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadCodeGroups = async (nextGroupIdFilter = groupIdFilter) => {
    setState((current) =>
      reduceCodeGroupManagementState(current, { type: "loading" }),
    );
    setFieldErrors({});
    try {
      const response = await codeGroupManagementApi.list({
        groupIdFilter: nextGroupIdFilter,
        page: 0,
        size: 20,
      });
      const codeGroups = response.data ?? [];
      setState((current) =>
        reduceCodeGroupManagementState(current, { type: "loaded", codeGroups }),
      );
      if (selectedGroup) {
        const refreshed =
          codeGroups.find((group) => group.groupId === selectedGroup.groupId) ??
          null;
        if (refreshed) applySelectedGroup(refreshed);
      }
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadCodeGroups("");
  }, []);

  const applySelectedGroup = (group: CodeGroupSummary) => {
    setSelectedGroup(group);
    setFormMode("update");
    setForm({
      groupId: group.groupId,
      groupName: group.groupName,
      description: group.description ?? "",
      managingDepartment: group.managingDepartment,
      systemUseYn: group.systemUseYn,
      changeReason: "",
    });
    setFieldErrors({});
  };

  const startCreate = () => {
    setSelectedGroup(null);
    setFormMode("create");
    setForm(emptyForm);
    setFieldErrors({});
  };

  const resetFilter = () => {
    setGroupIdFilter("");
    setState(createEmptyCodeGroupManagementState());
    startCreate();
  };

  const cancelEdit = () => {
    if (selectedGroup) {
      applySelectedGroup(selectedGroup);
      return;
    }
    startCreate();
  };

  const saveCodeGroup = async () => {
    const label = formMode === "create" ? "등록" : "수정";
    const confirmed = window.confirm(
      `${form.groupId || "신규 코드그룹"}을 ${label}하시겠습니까?`,
    );
    if (!confirmed) return;
    try {
      setFieldErrors({});
      const payload = codeGroupManagementApi.toPayload(form);
      const response =
        formMode === "create"
          ? await codeGroupManagementApi.create(payload)
          : await codeGroupManagementApi.update(
              selectedGroup?.groupId ?? payload.groupId,
              payload,
            );
      const saved = response.data;
      if (saved) applySelectedGroup(saved);
      await loadCodeGroups();
      setState((current) =>
        reduceCodeGroupManagementState(current, {
          type: "success",
          message: `코드그룹이 ${label}되었습니다.`,
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const goDetailCodes = (groupId: string) => {
    if (typeof window !== "undefined") {
      window.history.pushState(
        {},
        "",
        codeGroupManagementApi.paths.detailCodes(groupId),
      );
      window.dispatchEvent(new PopStateEvent("popstate"));
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceCodeGroupManagementState(current, { type: "permission" }),
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
        : "코드그룹 정보를 처리하지 못했습니다.";
    setState((current) =>
      reduceCodeGroupManagementState(current, { type: "error", message }),
    );
  };

  if (state.status === "permission") {
    return (
      <PermissionState
        title="코드그룹 관리 권한 없음"
        message="R09 시스템관리자 또는 코드그룹 관리 메뉴 접근 권한이 필요합니다."
      />
    );
  }

  return (
    <section data-screen-id="SCR-CODE-GROUP-MGMT" className="space-y-6">
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              시스템 관리 · 공통코드 관리 · 코드그룹 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              코드그룹 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              평가영역·처리상태·인증구분 등 코드 묶음의 그룹ID, 명칭, 설명,
              관리부서를 조회·등록·수정합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            onClick={() => void loadCodeGroups()}
            disabled={state.status === "loading"}
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      {state.status === "error" ? (
        <ErrorState title="코드그룹 관리 오류" message={state.message} />
      ) : null}
      {state.status === "success" ? (
        <SuccessState title="저장 완료" message={state.message} />
      ) : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <label className="block flex-1 text-sm font-semibold text-link">
            그룹ID 검색조건
            <div className="relative mt-2">
              <Search
                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                size={16}
              />
              <input
                className="h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 pl-10 text-sm focus-visible:border-primary focus-visible:outline-0"
                value={groupIdFilter}
                onChange={(event) => setGroupIdFilter(event.target.value)}
                placeholder="EVAL_AREA, PROC_STATUS"
              />
            </div>
          </label>
          <div className="flex gap-2">
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
              onClick={() => void loadCodeGroups()}
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
              <Tags size={18} /> 코드그룹 목록
            </h2>
            <div className="flex items-center gap-2">
              <span className="rounded-full bg-lightprimary px-3 py-1 text-sm font-semibold text-primary">
                {state.codeGroups.length}건
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
                title="코드그룹 조회 중"
                message="코드그룹 목록을 조회하고 있습니다."
              />
            </div>
          ) : null}
          {state.status === "empty" ? (
            <div className="mt-4">
              <EmptyState
                title="조회된 코드그룹이 없습니다"
                message="검색조건을 변경하거나 신규 등록을 진행하세요."
              />
            </div>
          ) : null}
          {state.codeGroups.length > 0 ? (
            <div className="mt-4 overflow-x-auto rounded-md border border-border">
              <table className="w-full caption-bottom text-sm">
                <thead className="border-b border-ld bg-lightgray text-left text-xs font-semibold uppercase text-lightmuted">
                  <tr>
                    <th className="px-4 py-3">그룹ID</th>
                    <th className="px-4 py-3">명칭</th>
                    <th className="px-4 py-3">설명</th>
                    <th className="px-4 py-3">관리부서</th>
                    <th className="px-4 py-3">상세코드</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {state.codeGroups.map((group) => (
                    <tr
                      key={group.groupId}
                      className={`cursor-pointer transition-colors hover:bg-lightprimary ${selectedGroup?.groupId === group.groupId ? "bg-lightsecondary" : ""}`}
                      onClick={() => applySelectedGroup(group)}
                    >
                      <td className="px-4 py-3 font-semibold text-primary">
                        {group.groupId}
                      </td>
                      <td className="px-4 py-3 text-link">{group.groupName}</td>
                      <td className="px-4 py-3 text-muted">
                        {group.description ?? "-"}
                      </td>
                      <td className="px-4 py-3 text-muted">
                        {group.managingDepartment}
                      </td>
                      <td className="px-4 py-3">
                        <button
                          type="button"
                          className="inline-flex items-center gap-1 rounded-md border border-primary px-3 py-1 text-xs font-semibold text-primary hover:bg-primary hover:text-white"
                          onClick={(event) => {
                            event.stopPropagation();
                            goDetailCodes(group.groupId);
                          }}
                          disabled={state.status === "loading"}
                        >
                          <ListChecks size={14} /> {group.detailCodeCount}개
                        </button>
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
              ? "신규 코드그룹 등록"
              : "선택 코드그룹 수정"}
          </h2>
          <p className="mt-2 text-xs text-muted">
            OQ-UI-090/REQ-056: 그룹ID 수정 가능 여부가 확정되지 않아 수정 시
            URL의 그룹ID와 같은 값만 저장합니다.
          </p>
          <div className="mt-5 space-y-4">
            <Field label="그룹ID" error={fieldErrors.groupId} required>
              <input
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm disabled:bg-lightgray"
                value={form.groupId}
                onChange={(event) =>
                  setForm({
                    ...form,
                    groupId: event.target.value.toUpperCase(),
                  })
                }
                disabled={formMode === "update"}
              />
            </Field>
            <Field label="명칭" error={fieldErrors.groupName} required>
              <input
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                value={form.groupName}
                onChange={(event) =>
                  setForm({ ...form, groupName: event.target.value })
                }
              />
            </Field>
            <Field label="설명" error={fieldErrors.description}>
              <textarea
                className="min-h-20 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                value={form.description}
                onChange={(event) =>
                  setForm({ ...form, description: event.target.value })
                }
              />
            </Field>
            <Field
              label="관리부서"
              error={fieldErrors.managingDepartment}
              required
            >
              <input
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                value={form.managingDepartment}
                onChange={(event) =>
                  setForm({ ...form, managingDepartment: event.target.value })
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
              onClick={() => void saveCodeGroup()}
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

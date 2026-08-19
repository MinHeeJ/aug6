import { useEffect, useState } from "react";
import { Edit3, RefreshCw, Save, Search, ShieldCheck } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type RoleSummary = {
  roleCode: string;
  roleName: string;
  purpose: string;
  assignmentCriteria: string;
  defaultDataScope: string;
  systemUseYn: "Y" | "N";
  status: string;
  updatedAt?: string;
};

type RoleForm = RoleSummary & {
  changeReason: string;
};

type RoleUpdatePayload = {
  roleName: string;
  purpose: string;
  assignmentCriteria: string;
  defaultDataScope: string;
  changeReason: string;
};

type RoleListParams = {
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

type RoleManagementState = {
  status: ScreenStatus;
  roles: RoleSummary[];
  selectedRoleCode?: string;
  message?: string;
};

type RoleManagementAction =
  | { type: "loading" }
  | { type: "loaded"; roles: RoleSummary[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string };

export function getRoleManagementRouteContract() {
  return {
    route: "/admin/roles",
    screenId: "SCR-ROLE-MGMT",
    operations: ["listRoles", "updateRole"],
  } as const;
}

export function createEmptyRoleManagementState(): RoleManagementState {
  return { status: "idle", roles: [] };
}

export function reduceRoleManagementState(
  state: RoleManagementState,
  action: RoleManagementAction,
): RoleManagementState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.roles.length === 0 ? "empty" : "loaded",
        roles: action.roles,
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

const emptyRole: RoleForm = {
  roleCode: "",
  roleName: "",
  purpose: "",
  assignmentCriteria: "",
  defaultDataScope: "",
  systemUseYn: "Y",
  status: "ACTIVE",
  updatedAt: "",
  changeReason: "",
};

export const roleManagementApi = {
  paths: {
    list(params: RoleListParams = {}) {
      const query = new URLSearchParams();
      if (params.filter?.trim()) query.set("filter", params.filter.trim());
      if (params.page !== undefined) query.set("page", String(params.page));
      if (params.size !== undefined) query.set("size", String(params.size));
      const suffix = query.toString();
      return `/api/admin/roles${suffix ? `?${suffix}` : ""}` as `/api/${string}`;
    },
    update(roleCode: string) {
      return `/api/admin/roles/${encodeURIComponent(roleCode)}` as `/api/${string}`;
    },
  },
  toUpdatePayload(
    form: Pick<
      RoleForm,
      | "roleName"
      | "purpose"
      | "assignmentCriteria"
      | "defaultDataScope"
      | "changeReason"
    > &
      Partial<Pick<RoleForm, "roleCode">>,
  ) {
    return {
      roleName: form.roleName,
      purpose: form.purpose,
      assignmentCriteria: form.assignmentCriteria,
      defaultDataScope: form.defaultDataScope,
      changeReason: form.changeReason,
    } satisfies RoleUpdatePayload;
  },
  list(params: RoleListParams = {}) {
    return apiRequest<RoleSummary[]>(roleManagementApi.paths.list(params));
  },
  update(roleCode: string, payload: RoleUpdatePayload) {
    return apiRequest<RoleSummary>(roleManagementApi.paths.update(roleCode), {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },
};

export function RoleManagementPage() {
  const [filter, setFilter] = useState("");
  const [state, setState] = useState<RoleManagementState>(
    createEmptyRoleManagementState(),
  );
  const [selectedRole, setSelectedRole] = useState<RoleSummary | null>(null);
  const [form, setForm] = useState<RoleForm>(emptyRole);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadRoles = async (nextFilter = filter) => {
    setState((current) =>
      reduceRoleManagementState(current, { type: "loading" }),
    );
    setFieldErrors({});
    try {
      const response = await roleManagementApi.list({
        filter: nextFilter,
        page: 0,
        size: 20,
      });
      const roles = response.data ?? [];
      setState((current) =>
        reduceRoleManagementState(current, { type: "loaded", roles }),
      );
      if (selectedRole) {
        const refreshed =
          roles.find((role) => role.roleCode === selectedRole.roleCode) ?? null;
        if (refreshed) {
          applySelectedRole(refreshed);
        }
      }
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadRoles("");
  }, []);

  const applySelectedRole = (role: RoleSummary) => {
    setSelectedRole(role);
    setForm({ ...role, changeReason: "" });
    setFieldErrors({});
  };

  const resetFilter = () => {
    setFilter("");
    setSelectedRole(null);
    setForm(emptyRole);
    setState(createEmptyRoleManagementState());
  };

  const cancelEdit = () => {
    if (selectedRole) {
      applySelectedRole(selectedRole);
    }
    setFieldErrors({});
  };

  const saveRole = async () => {
    if (!selectedRole) return;
    const confirmed = window.confirm(
      `${selectedRole.roleCode} 역할 기준정보를 저장하시겠습니까?`,
    );
    if (!confirmed) return;
    try {
      setFieldErrors({});
      const response = await roleManagementApi.update(
        selectedRole.roleCode,
        roleManagementApi.toUpdatePayload(form),
      );
      const updated = response.data ?? selectedRole;
      applySelectedRole(updated);
      setState((current) =>
        reduceRoleManagementState(current, {
          type: "success",
          message: "역할 정보가 저장되었습니다.",
        }),
      );
      await loadRoles();
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceRoleManagementState(current, { type: "permission" }),
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
        : "역할 정보를 처리하지 못했습니다.";
    setState((current) =>
      reduceRoleManagementState(current, { type: "error", message }),
    );
  };

  return (
    <section data-screen-id="SCR-ROLE-MGMT" className="space-y-6">
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">
              시스템 관리 · 역할·권한 관리 · 역할 관리
            </p>
            <h1 className="mt-2 text-xl font-semibold text-dark">역할 관리</h1>
            <p className="mt-2 text-sm text-muted">
              R01~R09 기준 역할의 목적, 부여 기준, 데이터 범위 기본값을
              관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            onClick={() => void loadRoles()}
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      {state.status === "permission" ? (
        <PermissionState
          title="역할 관리 권한 없음"
          message="R09 시스템관리자 또는 역할 관리 메뉴 접근 권한이 필요합니다."
        />
      ) : (
        <>
          {state.status === "error" ? (
            <ErrorState title="역할 관리 오류" message={state.message} />
          ) : null}
          {state.status === "success" ? (
            <SuccessState title="저장 완료" message={state.message} />
          ) : null}

          <section className="rounded-md border border-ld bg-white p-6 shadow-md">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <label className="block flex-1 text-sm font-semibold text-link">
                검색조건
                <div className="relative mt-2">
                  <Search
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                    size={16}
                  />
                  <input
                    className="h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 pl-10 text-sm focus-visible:border-primary focus-visible:outline-0"
                    value={filter}
                    onChange={(event) => setFilter(event.target.value)}
                    placeholder="역할코드, 역할명, 목적"
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
                  className="h-10 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
                  onClick={() => void loadRoles()}
                >
                  조회
                </button>
              </div>
            </div>
            <p className="mt-3 text-xs text-muted">
              OQ-UI-041: 검색조건 세부 확정 전까지 역할코드·역할명·목적 통합
              검색만 제공합니다.
            </p>
          </section>

          <div className="grid grid-cols-12 gap-6">
            <section className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md xl:col-span-8">
              <div className="flex items-center justify-between">
                <h2 className="flex items-center gap-2 text-lg font-semibold text-dark">
                  <ShieldCheck size={18} /> 역할 목록
                </h2>
                <span className="rounded-full bg-lightprimary px-3 py-1 text-sm font-semibold text-primary">
                  {state.roles.length}건
                </span>
              </div>
              {state.status === "loading" ? (
                <div className="mt-4">
                  <LoadingState
                    title="역할 조회 중"
                    message="R01~R09 역할 목록을 조회하고 있습니다."
                  />
                </div>
              ) : null}
              {state.status === "empty" ? (
                <div className="mt-4">
                  <EmptyState
                    title="조회된 역할이 없습니다"
                    message="검색조건을 변경하거나 초기화 후 다시 조회하세요."
                  />
                </div>
              ) : null}
              {state.roles.length > 0 ? (
                <div className="mt-4 overflow-x-auto rounded-md border border-border">
                  <table className="w-full caption-bottom text-sm">
                    <thead className="border-b border-ld bg-lightgray text-left text-xs font-semibold uppercase text-lightmuted">
                      <tr>
                        <th className="px-4 py-3">역할코드</th>
                        <th className="px-4 py-3">역할명</th>
                        <th className="px-4 py-3">목적</th>
                        <th className="px-4 py-3">데이터 범위</th>
                        <th className="px-4 py-3">상태</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {state.roles.map((role) => (
                        <tr
                          key={role.roleCode}
                          className={`cursor-pointer transition-colors hover:bg-lightprimary ${selectedRole?.roleCode === role.roleCode ? "bg-lightsecondary" : ""}`}
                          onClick={() => applySelectedRole(role)}
                        >
                          <td className="whitespace-nowrap px-4 py-3 font-semibold text-dark">
                            {role.roleCode}
                          </td>
                          <td className="whitespace-nowrap px-4 py-3">
                            {role.roleName}
                          </td>
                          <td className="px-4 py-3">{role.purpose}</td>
                          <td className="whitespace-nowrap px-4 py-3">
                            {role.defaultDataScope}
                          </td>
                          <td className="whitespace-nowrap px-4 py-3">
                            <span className="rounded-full bg-lightsuccess px-2.5 py-0.5 text-xs font-semibold text-success">
                              {role.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </section>

            <section className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md xl:col-span-4">
              <div className="flex items-center gap-3">
                <span className="rounded bg-lightprimary p-3 text-primary">
                  <Edit3 size={20} />
                </span>
                <div>
                  <h2 className="text-lg font-semibold text-dark">상세/편집</h2>
                  <p className="text-sm text-muted">
                    역할코드는 읽기 전용이며 신규 역할코드 추가는 제외됩니다.
                  </p>
                </div>
              </div>
              {!selectedRole ? (
                <div className="mt-4">
                  <EmptyState
                    title="역할을 선택하세요"
                    message="목록에서 R01~R09 역할을 선택하면 편집할 수 있습니다."
                  />
                </div>
              ) : (
                <div className="mt-5 space-y-4">
                  <ReadonlyField label="역할코드" value={form.roleCode} />
                  <TextField
                    label="역할명"
                    value={form.roleName}
                    error={fieldErrors.roleName}
                    onChange={(value) => setForm({ ...form, roleName: value })}
                  />
                  <TextAreaField
                    label="역할 목적"
                    value={form.purpose}
                    error={fieldErrors.purpose}
                    onChange={(value) => setForm({ ...form, purpose: value })}
                  />
                  <TextAreaField
                    label="부여 기준"
                    value={form.assignmentCriteria}
                    error={fieldErrors.assignmentCriteria}
                    onChange={(value) =>
                      setForm({ ...form, assignmentCriteria: value })
                    }
                  />
                  <TextField
                    label="데이터 범위 기본값"
                    value={form.defaultDataScope}
                    error={fieldErrors.defaultDataScope}
                    onChange={(value) =>
                      setForm({ ...form, defaultDataScope: value })
                    }
                  />
                  <ReadonlyField
                    label="사용여부/상태"
                    value={`${form.systemUseYn === "Y" ? "사용" : "미사용"} / ${form.status}`}
                  />
                  <TextAreaField
                    label="변경 사유"
                    value={form.changeReason}
                    error={fieldErrors.changeReason}
                    onChange={(value) =>
                      setForm({ ...form, changeReason: value })
                    }
                  />
                  {fieldErrors.roleCode ? (
                    <p className="rounded bg-lighterror p-3 text-sm text-error">
                      {fieldErrors.roleCode}
                    </p>
                  ) : null}
                  <div className="grid gap-2 sm:grid-cols-2">
                    <button
                      type="button"
                      className="inline-flex items-center justify-center gap-2 rounded bg-primary px-4 py-2 text-sm font-semibold text-white"
                      onClick={() => void saveRole()}
                    >
                      <Save size={16} /> 저장
                    </button>
                    <button
                      type="button"
                      className="rounded border border-ld px-4 py-2 text-sm text-link"
                      onClick={cancelEdit}
                    >
                      취소
                    </button>
                  </div>
                </div>
              )}
            </section>
          </div>
        </>
      )}
    </section>
  );
}

function TextField({
  label,
  value,
  error,
  onChange,
}: {
  label: string;
  value: string;
  error?: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="block text-sm font-semibold text-link">
      {label}
      <input
        className="mt-2 h-10 w-full rounded border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function TextAreaField({
  label,
  value,
  error,
  onChange,
}: {
  label: string;
  value: string;
  error?: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="block text-sm font-semibold text-link">
      {label}
      <textarea
        className="mt-2 min-h-20 w-full rounded border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function ReadonlyField({ label, value }: { label: string; value?: string }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase text-lightmuted">{label}</p>
      <p className="mt-1 rounded bg-lightgray px-3 py-2 text-sm text-link">
        {value ?? "-"}
      </p>
    </div>
  );
}

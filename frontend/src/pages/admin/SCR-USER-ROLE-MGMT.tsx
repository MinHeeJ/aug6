import { useEffect, useState } from "react";
import {
  CheckCircle2,
  RefreshCw,
  RotateCcw,
  Save,
  Search,
  UserCheck,
} from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type UserRoleAssignmentSummary = {
  assignmentId: number;
  userId: number;
  loginId: string;
  employeeNo?: string;
  name?: string;
  roleCode: string;
  roleName: string;
  assignmentType: "POSITION" | "MANUAL";
  validStartDate: string;
  validEndDate?: string;
  approverUserId?: number;
  approverName?: string;
  status: "ACTIVE" | "REVOKED" | "INACTIVE";
  revokedAt?: string;
  revokedBy?: number;
  updatedAt?: string;
  changeReason?: string;
};

type UserRoleAssignmentSearchResponse = {
  assignments: UserRoleAssignmentSummary[];
  page: number;
  size: number;
  totalElements: number;
};

type SearchFilters = {
  roleCodeFilter: string;
  filter: string;
};

type AssignmentForm = {
  userId: string;
  roleCode: string;
  assignmentType: "MANUAL" | "POSITION";
  validStartDate: string;
  validEndDate: string;
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

type UserRoleManagementState = {
  status: ScreenStatus;
  assignments: UserRoleAssignmentSummary[];
  message?: string;
};

type UserRoleManagementAction =
  | { type: "loading" }
  | { type: "loaded"; assignments: UserRoleAssignmentSummary[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string };

const emptyFilters: SearchFilters = { roleCodeFilter: "", filter: "" };
const emptyForm: AssignmentForm = {
  userId: "",
  roleCode: "R01",
  assignmentType: "MANUAL",
  validStartDate: "",
  validEndDate: "",
  changeReason: "",
};
const roles = ["R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09"];

export function getUserRoleManagementRouteContract() {
  return {
    route: "/admin/user-roles",
    screenId: "SCR-USER-ROLE-MGMT",
    operations: [
      "assignUserRole",
      "updateUserRole",
      "revokeUserRole",
      "listCurrentUserRoles",
    ],
  } as const;
}

export function createEmptyUserRoleManagementState(): UserRoleManagementState {
  return { status: "idle", assignments: [] };
}

export function reduceUserRoleManagementState(
  state: UserRoleManagementState,
  action: UserRoleManagementAction,
): UserRoleManagementState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.assignments.length === 0 ? "empty" : "loaded",
        assignments: action.assignments,
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

export const userRoleManagementApi = {
  paths: {
    list(filters: Partial<SearchFilters> = {}) {
      const params = new URLSearchParams();
      Object.entries(filters).forEach(([key, value]) => {
        if (
          value !== undefined &&
          value !== null &&
          String(value).trim() !== ""
        ) {
          params.set(key, String(value));
        }
      });
      const query = params.toString();
      return `/api/admin/user-roles${query ? `?${query}` : ""}` as `/api/${string}`;
    },
    listCurrentUserRoles(userId: number) {
      return `/api/admin/users/${userId}/roles` as `/api/${string}`;
    },
    assign() {
      return "/api/admin/user-roles" as `/api/${string}`;
    },
    update(assignmentId: number) {
      return `/api/admin/user-roles/${assignmentId}` as `/api/${string}`;
    },
    revoke(assignmentId: number) {
      return `/api/admin/user-roles/${assignmentId}` as `/api/${string}`;
    },
  },
  list(filters: Partial<SearchFilters>) {
    return apiRequest<UserRoleAssignmentSearchResponse>(
      userRoleManagementApi.paths.list(filters),
    );
  },
  listCurrentUserRoles(userId: number) {
    return apiRequest<UserRoleAssignmentSearchResponse>(
      userRoleManagementApi.paths.listCurrentUserRoles(userId),
    );
  },
  assign(payload: AssignmentForm) {
    return apiRequest<UserRoleAssignmentSummary>(
      userRoleManagementApi.paths.assign(),
      {
        method: "POST",
        body: JSON.stringify(normalizePayload(payload)),
      },
    );
  },
  update(assignmentId: number, payload: AssignmentForm) {
    return apiRequest<UserRoleAssignmentSummary>(
      userRoleManagementApi.paths.update(assignmentId),
      {
        method: "PUT",
        body: JSON.stringify(normalizePayload(payload)),
      },
    );
  },
  revoke(assignmentId: number, changeReason: string) {
    return apiRequest<UserRoleAssignmentSummary>(
      userRoleManagementApi.paths.revoke(assignmentId),
      {
        method: "DELETE",
        body: JSON.stringify({ changeReason }),
      },
    );
  },
};

export function UserRoleManagementPage() {
  const [filters, setFilters] = useState<SearchFilters>(emptyFilters);
  const [state, setState] = useState<UserRoleManagementState>(
    createEmptyUserRoleManagementState(),
  );
  const [selected, setSelected] = useState<UserRoleAssignmentSummary | null>(
    null,
  );
  const [form, setForm] = useState<AssignmentForm>(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadAssignments = async (nextFilters = filters) => {
    setState((current) =>
      reduceUserRoleManagementState(current, { type: "loading" }),
    );
    setFieldErrors({});
    try {
      const response = await userRoleManagementApi.list(nextFilters);
      const data = response.data ?? {
        assignments: [],
        page: 0,
        size: 20,
        totalElements: 0,
      };
      setState((current) =>
        reduceUserRoleManagementState(current, {
          type: "loaded",
          assignments: data.assignments,
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadAssignments(emptyFilters);
  }, []);

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceUserRoleManagementState(current, { type: "permission" }),
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
        : "요청 처리 중 오류가 발생했습니다.";
    setState((current) =>
      reduceUserRoleManagementState(current, { type: "error", message }),
    );
  };

  const selectAssignment = (assignment: UserRoleAssignmentSummary) => {
    setSelected(assignment);
    setForm({
      userId: String(assignment.userId),
      roleCode: assignment.roleCode,
      assignmentType: assignment.assignmentType,
      validStartDate: assignment.validStartDate ?? "",
      validEndDate: assignment.validEndDate ?? "",
      changeReason: assignment.changeReason ?? "",
    });
    setFieldErrors({});
  };

  const saveAssignment = async () => {
    try {
      if (selected) {
        await userRoleManagementApi.update(selected.assignmentId, form);
      } else {
        await userRoleManagementApi.assign(form);
      }
      setState((current) =>
        reduceUserRoleManagementState(current, {
          type: "success",
          message: "사용자 역할이 저장되었습니다.",
        }),
      );
      setSelected(null);
      setForm(emptyForm);
      await loadAssignments();
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const revokeSelected = async () => {
    if (!selected) {
      return;
    }
    if (selected.assignmentType === "POSITION") {
      setFieldErrors({
        assignmentType: "보직 기반 역할은 보직 기준으로만 관리됩니다.",
      });
      return;
    }
    try {
      await userRoleManagementApi.revoke(
        selected.assignmentId,
        form.changeReason,
      );
      setState((current) =>
        reduceUserRoleManagementState(current, {
          type: "success",
          message: "사용자 역할이 회수되었습니다.",
        }),
      );
      setSelected(null);
      setForm(emptyForm);
      await loadAssignments();
    } catch (caught) {
      handleApiError(caught);
    }
  };

  return (
    <section data-screen-id="SCR-USER-ROLE-MGMT" className="space-y-6">
      <div className="mb-6 overflow-hidden rounded-md border-none bg-lightsecondary py-4 px-6 shadow-none">
        <h1 className="text-xl font-semibold text-dark">사용자 역할 관리</h1>
        <p className="mt-2 text-sm text-link">
          시스템 관리 · 역할·권한 관리 · 사용자 역할 관리
        </p>
      </div>

      {state.status === "permission" ? (
        <PermissionState
          title="사용자 역할 관리 권한 없음"
          message="R09 시스템관리자 권한 또는 메뉴 접근 권한이 필요합니다."
        />
      ) : (
        <>
          {state.status === "error" ? (
            <ErrorState title="사용자 역할 관리 오류" message={state.message} />
          ) : null}
          {state.status === "success" ? (
            <SuccessState title="처리 완료" message={state.message} />
          ) : null}

          <div className="grid grid-cols-12 gap-6">
            <section className="col-span-12 rounded-md bg-white p-6 shadow-md">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-lg font-semibold text-dark">검색조건</h2>
                  <p className="text-sm text-muted">
                    사용자별 현재 역할을 조회하고 수동 역할만
                    부여·변경·회수합니다.
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    className="rounded border border-ld px-4 py-2 text-sm text-link hover:text-primary"
                    onClick={() => setFilters(emptyFilters)}
                  >
                    조건 초기화
                  </button>
                  <button
                    type="button"
                    className="inline-flex items-center gap-2 rounded bg-primary px-4 py-2 text-sm font-semibold text-white shadow-btn-shadow"
                    onClick={() => void loadAssignments()}
                  >
                    <Search size={16} /> 조회
                  </button>
                </div>
              </div>
              <div className="mt-5 grid gap-3 md:grid-cols-3">
                <FilterInput
                  label="사용자/교번/성명"
                  value={filters.filter}
                  onChange={(value) =>
                    setFilters({ ...filters, filter: value })
                  }
                />
                <SelectInput
                  label="역할"
                  value={filters.roleCodeFilter}
                  onChange={(value) =>
                    setFilters({ ...filters, roleCodeFilter: value })
                  }
                  options={["", ...roles].map((role) => [role, role || "전체"])}
                />
              </div>
            </section>

            <section className="col-span-12 rounded-md bg-white p-6 shadow-md xl:col-span-8">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-dark">
                  사용자 역할 목록
                </h2>
                <span className="rounded bg-lightprimary px-3 py-1 text-sm font-semibold text-primary">
                  {state.assignments.length}건
                </span>
              </div>
              {state.status === "loading" ? (
                <div className="mt-4">
                  <LoadingState title="사용자 역할 조회 중" />
                </div>
              ) : null}
              {state.status === "empty" ? (
                <div className="mt-4">
                  <EmptyState
                    title="사용자 역할 없음"
                    message="조건에 맞는 사용자 역할이 없습니다."
                  />
                </div>
              ) : null}
              {state.assignments.length > 0 ? (
                <div className="mt-4 overflow-x-auto rounded-md border border-ld">
                  <table className="min-w-full divide-y divide-ld text-sm">
                    <thead className="bg-lightgray text-left text-xs font-semibold uppercase text-lightmuted">
                      <tr>
                        <th className="px-4 py-3">사용자</th>
                        <th className="px-4 py-3">역할</th>
                        <th className="px-4 py-3">구분</th>
                        <th className="px-4 py-3">유효기간</th>
                        <th className="px-4 py-3">승인자</th>
                        <th className="px-4 py-3">상태</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-ld">
                      {state.assignments.map((assignment) => (
                        <tr
                          key={assignment.assignmentId}
                          className={`cursor-pointer hover:bg-lightprimary ${selected?.assignmentId === assignment.assignmentId ? "bg-lightsecondary" : ""}`}
                          onClick={() => selectAssignment(assignment)}
                        >
                          <td className="px-4 py-3 font-semibold text-dark">
                            {assignment.employeeNo}
                            <br />
                            <span className="text-muted">
                              {assignment.name ?? assignment.loginId}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            {assignment.roleCode}
                            <br />
                            <span className="text-muted">
                              {assignment.roleName}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            <AssignmentTypeBadge
                              value={assignment.assignmentType}
                            />
                          </td>
                          <td className="px-4 py-3">
                            {assignment.validStartDate} ~{" "}
                            {assignment.validEndDate ?? "현재"}
                          </td>
                          <td className="px-4 py-3">
                            {assignment.approverName ??
                              assignment.approverUserId ??
                              "-"}
                          </td>
                          <td className="px-4 py-3">
                            {assignment.status === "ACTIVE"
                              ? "활성"
                              : assignment.status === "REVOKED"
                                ? "회수"
                                : "비활성"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </section>

            <section className="col-span-12 rounded-md bg-white p-6 shadow-md xl:col-span-4">
              <div className="flex items-center gap-3">
                <span className="rounded bg-lightprimary p-3 text-primary">
                  <UserCheck size={20} />
                </span>
                <div>
                  <h2 className="text-lg font-semibold text-dark">
                    부여/변경/회수
                  </h2>
                  <p className="text-sm text-muted">
                    승인자는 로그인 관리자로 자동 기록됩니다.
                  </p>
                </div>
              </div>
              <div className="mt-5 space-y-4">
                <FilterInput
                  label="사용자 ID"
                  value={form.userId}
                  onChange={(value) => setForm({ ...form, userId: value })}
                  error={fieldErrors.userId}
                />
                <SelectInput
                  label="역할코드"
                  value={form.roleCode}
                  onChange={(value) => setForm({ ...form, roleCode: value })}
                  options={roles.map((role) => [role, role])}
                  error={fieldErrors.roleCode}
                />
                <SelectInput
                  label="역할 구분"
                  value={form.assignmentType}
                  onChange={(value) =>
                    setForm({
                      ...form,
                      assignmentType: value as AssignmentForm["assignmentType"],
                    })
                  }
                  options={[
                    ["MANUAL", "수동"],
                    ["POSITION", "보직 기반(읽기 전용)"],
                  ]}
                  error={fieldErrors.assignmentType}
                />
                {form.assignmentType === "POSITION" ? (
                  <div className="rounded bg-lightwarning p-3 text-sm text-link">
                    보직 기반 역할은 조회로 구분만 표시하며 직접
                    부여·변경·회수하지 않습니다.
                  </div>
                ) : null}
                <FilterInput
                  label="유효 시작일"
                  type="date"
                  value={form.validStartDate}
                  onChange={(value) =>
                    setForm({ ...form, validStartDate: value })
                  }
                  error={fieldErrors.validStartDate}
                />
                <FilterInput
                  label="유효 종료일"
                  type="date"
                  value={form.validEndDate}
                  onChange={(value) =>
                    setForm({ ...form, validEndDate: value })
                  }
                  error={fieldErrors.validEndDate}
                />
                <label className="block text-sm font-semibold text-link">
                  변경 사유
                  <textarea
                    className="mt-2 w-full rounded border border-ld px-3 py-2 text-sm"
                    value={form.changeReason}
                    onChange={(event) =>
                      setForm({ ...form, changeReason: event.target.value })
                    }
                  />
                  {fieldErrors.changeReason ? (
                    <span className="mt-1 block text-xs text-error">
                      {fieldErrors.changeReason}
                    </span>
                  ) : null}
                </label>
                {selected ? (
                  <div className="rounded bg-lightprimary p-3 text-sm text-primary">
                    <CheckCircle2 className="mr-2 inline" size={16} />
                    선택: #{selected.assignmentId}{" "}
                    {selected.name ?? selected.loginId}
                  </div>
                ) : null}
                <div className="grid gap-2 sm:grid-cols-2">
                  <button
                    type="button"
                    className="inline-flex items-center justify-center gap-2 rounded bg-primary px-4 py-2 text-sm font-semibold text-white"
                    onClick={() => void saveAssignment()}
                  >
                    <Save size={16} /> {selected ? "변경 저장" : "역할 부여"}
                  </button>
                  <button
                    type="button"
                    className="inline-flex items-center justify-center gap-2 rounded bg-lighterror px-4 py-2 text-sm font-semibold text-error hover:bg-error hover:text-white"
                    onClick={() => void revokeSelected()}
                    disabled={!selected}
                  >
                    <RotateCcw size={16} /> 역할 회수
                  </button>
                  <button
                    type="button"
                    className="rounded border border-ld px-4 py-2 text-sm text-link sm:col-span-2"
                    onClick={() => {
                      setSelected(null);
                      setForm(emptyForm);
                      setFieldErrors({});
                    }}
                  >
                    <RefreshCw className="mr-2 inline" size={16} />
                    취소/신규
                  </button>
                </div>
              </div>
            </section>
          </div>
        </>
      )}
    </section>
  );
}

function normalizePayload(payload: AssignmentForm) {
  return {
    userId: payload.userId,
    roleCode: payload.roleCode,
    assignmentType: payload.assignmentType,
    validStartDate: payload.validStartDate || undefined,
    validEndDate: payload.validEndDate || undefined,
    changeReason: payload.changeReason || undefined,
  };
}

function FilterInput({
  label,
  value,
  onChange,
  type = "text",
  error,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  error?: string;
}) {
  return (
    <label className="block text-sm font-semibold text-link">
      {label}
      <input
        type={type}
        className="mt-2 w-full rounded border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function SelectInput({
  label,
  value,
  onChange,
  options,
  error,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: string[][];
  error?: string;
}) {
  return (
    <label className="block text-sm font-semibold text-link">
      {label}
      <select
        className="mt-2 w-full rounded border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        {options.map(([key, text]) => (
          <option key={key} value={key}>
            {text}
          </option>
        ))}
      </select>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function AssignmentTypeBadge({ value }: { value: "POSITION" | "MANUAL" }) {
  const className =
    value === "POSITION"
      ? "bg-lightsecondary text-secondary"
      : "bg-lightprimary text-primary";
  return (
    <span
      className={`rounded-full px-2.5 py-1 text-xs font-semibold ${className}`}
    >
      {value === "POSITION" ? "보직 기반" : "수동"}
    </span>
  );
}

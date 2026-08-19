import { useEffect, useMemo, useState } from "react";
import { RefreshCw, Save, Search, ShieldAlert, UserCog } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type UserSummary = {
  userId: number;
  loginId: string;
  employeeNo?: string;
  name?: string;
  organizationCode?: string;
  organizationName?: string;
  rankName?: string;
  employmentStatus?: string;
  positionName?: string;
  retirementDate?: string;
  lastSyncedAt?: string;
  systemUseYn: "Y" | "N";
  status: string;
  roleCodes: string[];
};

export type AvailableRole = {
  roleCode: string;
  roleName: string;
};

type UserSearchResponse = {
  users: UserSummary[];
  availableRoles: AvailableRole[];
  page: number;
  size: number;
  totalElements: number;
};

type SearchFilters = {
  employeeNo: string;
  name: string;
  organizationCodeFilter: string;
  rankName: string;
  employmentStatus: string;
  roleCodeFilter: string;
  systemUseYn: string;
};

type ScreenStatus =
  | "idle"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success";

type UserManagementState = {
  status: ScreenStatus;
  users: UserSummary[];
  selectedUserId?: number;
  message?: string;
};

type UserManagementAction =
  | { type: "loading" }
  | { type: "loaded"; users: UserSummary[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string };

const emptyFilters: SearchFilters = {
  employeeNo: "",
  name: "",
  organizationCodeFilter: "",
  rankName: "",
  employmentStatus: "",
  roleCodeFilter: "",
  systemUseYn: "",
};

export function getUserManagementRouteContract() {
  return {
    route: "/admin/users",
    screenId: "SCR-USER-MGMT",
    operations: ["searchUsers", "updateUserAccount", "updateUserRoles"],
  } as const;
}

export function createEmptyUserManagementState(): UserManagementState {
  return { status: "idle", users: [] };
}

export function reduceUserManagementState(
  state: UserManagementState,
  action: UserManagementAction,
): UserManagementState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.users.length === 0 ? "empty" : "loaded",
        users: action.users,
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

export const userManagementApi = {
  paths: {
    search(filters: Partial<SearchFilters> = {}) {
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
      return `/api/admin/users${query ? `?${query}` : ""}` as `/api/${string}`;
    },
    updateAccount(userId: number) {
      return `/api/admin/users/${userId}/account` as `/api/${string}`;
    },
    updateRoles(userId: number) {
      return `/api/admin/users/${userId}/roles` as `/api/${string}`;
    },
  },
  search(filters: Partial<SearchFilters>) {
    return apiRequest<UserSearchResponse>(
      userManagementApi.paths.search(filters),
    );
  },
  updateAccount(userId: number, systemUseYn: "Y" | "N", changeReason: string) {
    return apiRequest<UserSummary>(
      userManagementApi.paths.updateAccount(userId),
      {
        method: "PATCH",
        body: JSON.stringify({ systemUseYn, changeReason }),
      },
    );
  },
  updateRoles(
    userId: number,
    roleCodes: string[],
    validStartDate: string,
    validEndDate: string,
    changeReason: string,
  ) {
    return apiRequest(userManagementApi.paths.updateRoles(userId), {
      method: "PATCH",
      body: JSON.stringify({
        roleCodes,
        validStartDate: validStartDate || undefined,
        validEndDate: validEndDate || undefined,
        changeReason,
      }),
    });
  },
};

export function UserManagementPage() {
  const [filters, setFilters] = useState<SearchFilters>(emptyFilters);
  const [state, setState] = useState<UserManagementState>(
    createEmptyUserManagementState(),
  );
  const [roles, setRoles] = useState<AvailableRole[]>([]);
  const [selectedUser, setSelectedUser] = useState<UserSummary | null>(null);
  const [systemUseYn, setSystemUseYn] = useState<"Y" | "N">("Y");
  const [roleCodes, setRoleCodes] = useState<string[]>([]);
  const [validStartDate, setValidStartDate] = useState("");
  const [validEndDate, setValidEndDate] = useState("");
  const [changeReason, setChangeReason] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const selectedRoleLabels = useMemo(
    () => roleCodes.join(", ") || "선택 없음",
    [roleCodes],
  );

  const loadUsers = async (nextFilters = filters) => {
    setState((current) =>
      reduceUserManagementState(current, { type: "loading" }),
    );
    setFieldErrors({});
    try {
      const response = await userManagementApi.search(nextFilters);
      const data = response.data ?? {
        users: [],
        availableRoles: [],
        page: 0,
        size: 20,
        totalElements: 0,
      };
      setRoles(data.availableRoles);
      setState((current) =>
        reduceUserManagementState(current, {
          type: "loaded",
          users: data.users,
        }),
      );
      if (selectedUser) {
        const refreshed =
          data.users.find((user) => user.userId === selectedUser.userId) ??
          null;
        if (refreshed) {
          applySelectedUser(refreshed);
        }
      }
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadUsers(emptyFilters);
  }, []);

  const applySelectedUser = (user: UserSummary) => {
    setSelectedUser(user);
    setSystemUseYn(user.systemUseYn);
    setRoleCodes(user.roleCodes ?? []);
    setValidStartDate("");
    setValidEndDate("");
    setChangeReason("");
    setFieldErrors({});
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceUserManagementState(current, { type: "permission" }),
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
      reduceUserManagementState(current, { type: "error", message }),
    );
  };

  const saveAccount = async () => {
    if (!selectedUser) {
      return;
    }
    try {
      await userManagementApi.updateAccount(
        selectedUser.userId,
        systemUseYn,
        changeReason,
      );
      setState((current) =>
        reduceUserManagementState(current, {
          type: "success",
          message: "사용자 정보가 저장되었습니다.",
        }),
      );
      await loadUsers();
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const saveRoles = async () => {
    if (!selectedUser) {
      return;
    }
    try {
      await userManagementApi.updateRoles(
        selectedUser.userId,
        roleCodes,
        validStartDate,
        validEndDate,
        changeReason,
      );
      setState((current) =>
        reduceUserManagementState(current, {
          type: "success",
          message: "사용자 정보가 저장되었습니다.",
        }),
      );
      await loadUsers();
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const resetFilters = () => {
    setFilters(emptyFilters);
    setState(createEmptyUserManagementState());
  };

  const cancelEdits = () => {
    if (selectedUser) {
      applySelectedUser(selectedUser);
    }
  };

  return (
    <section data-screen-id="SCR-USER-MGMT" className="space-y-6">
      <div className="mb-6 overflow-hidden rounded-md border-none bg-lightsecondary py-4 px-6 shadow-none">
        <h1 className="text-xl font-semibold text-dark">사용자 관리</h1>
        <p className="mt-2 text-sm text-link">
          시스템 관리 · 사용자·조직 관리 · 사용자 관리
        </p>
      </div>

      {state.status === "permission" ? (
        <PermissionState
          title="사용자 관리 권한 없음"
          message="R09 시스템관리자 권한 또는 메뉴 접근 권한이 필요합니다."
        />
      ) : (
        <>
          {state.status === "error" ? (
            <ErrorState title="사용자 관리 오류" message={state.message} />
          ) : null}
          {state.status === "success" ? (
            <SuccessState title="저장 완료" message={state.message} />
          ) : null}

          <div className="grid grid-cols-12 gap-6">
            <section className="col-span-12 rounded bg-white p-6 shadow-md">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-lg font-semibold text-dark">검색조건</h2>
                  <p className="text-sm text-muted">
                    KORUS 원천 인사정보는 조회 전용이며 로컬 DB의 사용여부와
                    업무 역할만 저장합니다.
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    className="rounded border border-ld px-4 py-2 text-sm text-link hover:text-primary"
                    onClick={resetFilters}
                  >
                    조건 초기화
                  </button>
                  <button
                    type="button"
                    className="inline-flex items-center gap-2 rounded bg-primary px-4 py-2 text-sm font-semibold text-white shadow-btn-shadow"
                    onClick={() => void loadUsers()}
                  >
                    <Search size={16} /> 조회
                  </button>
                </div>
              </div>
              <div className="mt-5 grid gap-3 md:grid-cols-4">
                <FilterInput
                  label="교번"
                  value={filters.employeeNo}
                  onChange={(value) =>
                    setFilters({ ...filters, employeeNo: value })
                  }
                />
                <FilterInput
                  label="성명"
                  value={filters.name}
                  onChange={(value) => setFilters({ ...filters, name: value })}
                />
                <FilterInput
                  label="소속 조직코드"
                  value={filters.organizationCodeFilter}
                  onChange={(value) =>
                    setFilters({ ...filters, organizationCodeFilter: value })
                  }
                />
                <FilterInput
                  label="직급"
                  value={filters.rankName}
                  onChange={(value) =>
                    setFilters({ ...filters, rankName: value })
                  }
                />
                <SelectInput
                  label="재직상태"
                  value={filters.employmentStatus}
                  onChange={(value) =>
                    setFilters({ ...filters, employmentStatus: value })
                  }
                  options={[
                    ["", "전체"],
                    ["ACTIVE", "재직"],
                    ["LEAVE", "휴직"],
                    ["RETIRED", "퇴직"],
                  ]}
                />
                <SelectInput
                  label="역할"
                  value={filters.roleCodeFilter}
                  onChange={(value) =>
                    setFilters({ ...filters, roleCodeFilter: value })
                  }
                  options={[
                    ["", "전체"],
                    ...roles.map((role) => [
                      role.roleCode,
                      `${role.roleCode} ${role.roleName}`,
                    ]),
                  ]}
                />
                <SelectInput
                  label="사용여부"
                  value={filters.systemUseYn}
                  onChange={(value) =>
                    setFilters({ ...filters, systemUseYn: value })
                  }
                  options={[
                    ["", "전체"],
                    ["Y", "사용"],
                    ["N", "미사용"],
                  ]}
                />
              </div>
            </section>

            <section className="col-span-12 rounded bg-white p-6 shadow-md xl:col-span-8">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-dark">사용자 목록</h2>
                <span className="rounded bg-lightprimary px-3 py-1 text-sm font-semibold text-primary">
                  {state.users.length}건
                </span>
              </div>
              {state.status === "loading" ? (
                <div className="mt-4">
                  <LoadingState title="사용자 조회 중" />
                </div>
              ) : null}
              {state.status === "empty" ? (
                <div className="mt-4">
                  <EmptyState
                    title="사용자 없음"
                    message="조건에 맞는 사용자가 없습니다."
                  />
                </div>
              ) : null}
              {state.users.length > 0 ? (
                <div className="mt-4 overflow-x-auto">
                  <table className="min-w-full divide-y divide-ld text-sm">
                    <thead className="bg-lightgray text-left text-xs font-semibold uppercase text-lightmuted">
                      <tr>
                        <th className="px-4 py-3">교번/성명</th>
                        <th className="px-4 py-3">소속/직급</th>
                        <th className="px-4 py-3">보직</th>
                        <th className="px-4 py-3">재직/퇴직일자</th>
                        <th className="px-4 py-3">역할</th>
                        <th className="px-4 py-3">사용</th>
                        <th className="px-4 py-3">동기화</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-ld">
                      {state.users.map((user) => (
                        <tr
                          key={user.userId}
                          className={`cursor-pointer hover:bg-lightprimary ${selectedUser?.userId === user.userId ? "bg-lightsecondary" : ""}`}
                          onClick={() => applySelectedUser(user)}
                        >
                          <td className="px-4 py-3 font-semibold text-dark">
                            {user.employeeNo}
                            <br />
                            <span className="text-muted">{user.name}</span>
                          </td>
                          <td className="px-4 py-3">
                            {user.organizationName ?? user.organizationCode}
                            <br />
                            <span className="text-muted">{user.rankName}</span>
                          </td>
                          <td className="px-4 py-3">
                            {user.positionName ?? "-"}
                          </td>
                          <td className="px-4 py-3">
                            {employmentLabel(user.employmentStatus)}
                            <br />
                            <span className="text-muted">
                              {user.retirementDate ?? "-"}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            {user.roleCodes?.join(", ") || "-"}
                          </td>
                          <td className="px-4 py-3">
                            {user.systemUseYn === "Y" ? "사용" : "미사용"}
                          </td>
                          <td className="px-4 py-3 text-muted">
                            {formatDateTime(user.lastSyncedAt)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </section>

            <section className="col-span-12 rounded bg-white p-6 shadow-md xl:col-span-4">
              <div className="flex items-center gap-3">
                <span className="rounded bg-lightprimary p-3 text-primary">
                  <UserCog size={20} />
                </span>
                <div>
                  <h2 className="text-lg font-semibold text-dark">상세/편집</h2>
                  <p className="text-sm text-muted">
                    시스템 사용여부와 업무 역할만 수정 가능
                  </p>
                </div>
              </div>
              {!selectedUser ? (
                <div className="mt-4">
                  <EmptyState
                    title="사용자를 선택하세요"
                    message="목록 행을 선택하면 상세 정보와 편집 폼이 표시됩니다."
                  />
                </div>
              ) : (
                <div className="mt-5 space-y-4">
                  <ReadonlyField
                    label="KORUS 교번"
                    value={selectedUser.employeeNo}
                  />
                  <ReadonlyField label="KORUS 성명" value={selectedUser.name} />
                  <ReadonlyField
                    label="KORUS 소속"
                    value={
                      selectedUser.organizationName ??
                      selectedUser.organizationCode
                    }
                  />
                  <ReadonlyField
                    label="KORUS 직급/재직"
                    value={`${selectedUser.rankName ?? "-"} / ${employmentLabel(selectedUser.employmentStatus)}`}
                  />
                  <div className="rounded bg-lightwarning p-3 text-sm text-link">
                    <ShieldAlert
                      className="mr-2 inline text-warning"
                      size={16}
                    />
                    KORUS 원천 필드는 읽기 전용입니다.
                  </div>
                  <SelectInput
                    label="시스템 사용여부"
                    value={systemUseYn}
                    onChange={(value) => setSystemUseYn(value as "Y" | "N")}
                    options={[
                      ["Y", "사용"],
                      ["N", "미사용"],
                    ]}
                    error={fieldErrors.systemUseYn}
                  />
                  <div>
                    <p className="mb-2 text-sm font-semibold text-link">
                      업무 역할
                    </p>
                    <div className="grid gap-2 rounded border border-ld p-3">
                      {roles.map((role) => (
                        <label
                          key={role.roleCode}
                          className="flex items-center gap-2 text-sm text-link"
                        >
                          <input
                            type="checkbox"
                            checked={roleCodes.includes(role.roleCode)}
                            onChange={(event) => {
                              setRoleCodes((current) =>
                                event.target.checked
                                  ? [...current, role.roleCode]
                                  : current.filter(
                                      (code) => code !== role.roleCode,
                                    ),
                              );
                            }}
                          />
                          {role.roleCode} {role.roleName}
                        </label>
                      ))}
                      <p className="text-xs text-muted">
                        선택: {selectedRoleLabels}
                      </p>
                      {fieldErrors.roleCodes ? (
                        <p className="text-xs text-error">
                          {fieldErrors.roleCodes}
                        </p>
                      ) : null}
                    </div>
                  </div>
                  <FilterInput
                    label="역할 유효 시작일"
                    type="date"
                    value={validStartDate}
                    onChange={setValidStartDate}
                    error={fieldErrors.validStartDate}
                  />
                  <FilterInput
                    label="역할 유효 종료일"
                    type="date"
                    value={validEndDate}
                    onChange={setValidEndDate}
                    error={fieldErrors.validEndDate}
                  />
                  <label className="block text-sm font-semibold text-link">
                    변경 사유
                    <textarea
                      className="mt-2 w-full rounded border border-ld px-3 py-2 text-sm"
                      value={changeReason}
                      onChange={(event) => setChangeReason(event.target.value)}
                    />
                    {fieldErrors.changeReason ? (
                      <span className="mt-1 block text-xs text-error">
                        {fieldErrors.changeReason}
                      </span>
                    ) : null}
                  </label>
                  <div className="grid gap-2 sm:grid-cols-2">
                    <button
                      type="button"
                      className="inline-flex items-center justify-center gap-2 rounded bg-primary px-4 py-2 text-sm font-semibold text-white"
                      onClick={() => void saveAccount()}
                    >
                      <Save size={16} /> 사용여부 저장
                    </button>
                    <button
                      type="button"
                      className="inline-flex items-center justify-center gap-2 rounded bg-secondary px-4 py-2 text-sm font-semibold text-white"
                      onClick={() => void saveRoles()}
                    >
                      <RefreshCw size={16} /> 역할 저장
                    </button>
                    <button
                      type="button"
                      className="rounded border border-ld px-4 py-2 text-sm text-link sm:col-span-2"
                      onClick={cancelEdits}
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

function employmentLabel(status?: string) {
  return status === "ACTIVE"
    ? "재직"
    : status === "LEAVE"
      ? "휴직"
      : status === "RETIRED"
        ? "퇴직"
        : "-";
}

function formatDateTime(value?: string) {
  return value ? value.replace("T", " ").slice(0, 16) : "-";
}

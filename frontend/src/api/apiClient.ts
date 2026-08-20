export type ApiErrorField = {
  field: string;
  message: string;
};

export type ApiError = {
  code: string;
  message: string;
  fields: ApiErrorField[];
};

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: ApiError;
  meta: Record<string, unknown>;
};

export class ApiClientError extends Error {
  readonly status: number;
  readonly apiError?: ApiError;

  constructor(status: number, message: string, apiError?: ApiError) {
    super(message);
    this.status = status;
    this.apiError = apiError;
  }
}

export async function apiRequest<T>(
  path: `/api/${string}`,
  init: RequestInit = {},
): Promise<ApiResponse<T>> {
  if (!path.startsWith("/api/")) {
    throw new Error("API 요청은 /api/ 상대경로만 허용됩니다.");
  }
  const response = await fetch(path, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(init.headers ?? {}),
    },
  });
  const contentType = response.headers.get("content-type") ?? "";
  const body = contentType.includes("application/json")
    ? ((await response.json()) as ApiResponse<T>)
    : ({ success: response.ok, meta: {}, data: undefined } as ApiResponse<T>);

  if (!response.ok || body.success === false) {
    throw new ApiClientError(
      response.status,
      body.error?.message ?? "API 요청에 실패했습니다.",
      body.error,
    );
  }
  return body;
}

export const authApi = {
  login(loginId: string, password: string) {
    return apiRequest<CurrentUser>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ loginId, password }),
    });
  },
  me() {
    return apiRequest<CurrentUser>("/api/auth/me");
  },
  logout() {
    return apiRequest<void>("/api/auth/logout", { method: "POST" });
  },
  health() {
    return apiRequest<HealthStatus>("/api/health");
  },
};

export type HealthStatus = {
  status: string;
  service?: string;
};

export type MenuItem = {
  menuId: number;
  parentMenuId?: number;
  menuName: string;
  screenId?: string;
  url?: string;
  icon?: string;
  displayOrder: number;
  children: MenuItem[];
};

export type CurrentUser = {
  userId: number;
  loginId: string;
  employeeNo?: string;
  name: string;
  roles: string[];
  menus: MenuItem[];
};

export type Organization = {
  organizationCode: string;
  organizationName: string;
  organizationType: string;
  systemUseYn: string;
  status: string;
  parentOrganizationCode?: string | null;
  effectiveStartDate?: string | null;
  effectiveEndDate?: string | null;
  updatedAt?: string;
};

export type OrganizationTreeNode = Organization & {
  children: OrganizationTreeNode[];
};

export type OrganizationParentRelationPayload = {
  parentOrganizationCode: string;
  effectiveStartDate: string;
  effectiveEndDate?: string | null;
  changeReason: string;
};

export type OrganizationRelationHistory = {
  historyId: number;
  relationId?: number;
  organizationCode: string;
  beforeParentOrganizationCode?: string | null;
  afterParentOrganizationCode?: string | null;
  beforeEffectiveStartDate?: string | null;
  beforeEffectiveEndDate?: string | null;
  afterEffectiveStartDate?: string | null;
  afterEffectiveEndDate?: string | null;
  changedAt?: string;
  changedBy?: number;
  changeReason?: string;
};

export const organizationApi = {
  searchOrganizations(
    params: {
      organizationCodeFilter?: string;
      page?: number;
      size?: number;
    } = {},
  ) {
    const query = new URLSearchParams();
    if (params.organizationCodeFilter)
      query.set("organizationCodeFilter", params.organizationCodeFilter);
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 10));
    return apiRequest<Organization[]>(
      `/api/admin/organizations?${query.toString()}` as `/api/${string}`,
    );
  },
  getOrganizationTree() {
    return apiRequest<OrganizationTreeNode[]>("/api/admin/organizations/tree");
  },
  saveOrganizationParentRelation(
    organizationCode: string,
    payload: OrganizationParentRelationPayload,
  ) {
    return apiRequest<Organization>(
      `/api/admin/organizations/${encodeURIComponent(organizationCode)}/parent-relations` as `/api/${string}`,
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
  listOrganizationParentRelationHistory(organizationCode: string) {
    const query = new URLSearchParams({ page: "0", size: "10" });
    return apiRequest<OrganizationRelationHistory[]>(
      `/api/admin/organizations/${encodeURIComponent(organizationCode)}/parent-relations/history?${query.toString()}` as `/api/${string}`,
    );
  },
};

export type MenuPermission = {
  permissionId: number;
  targetType: "ROLE" | "ORGANIZATION" | "USER";
  targetId: string;
  targetName?: string;
  menuId: number;
  topMenuName?: string;
  middleMenuName?: string;
  screenMenuName: string;
  screenId?: string;
  url?: string;
  accessAllowed: "ALLOW" | "DENY";
  status: string;
  changeReason?: string;
  updatedAt?: string;
};

export type MenuPermissionSearchResponse = {
  permissions: MenuPermission[];
  page: number;
  size: number;
  totalElements: number;
};

export type MenuPermissionPayload = {
  targetType: "ROLE" | "ORGANIZATION" | "USER";
  targetId: string;
  menuId: number;
  accessAllowed: "ALLOW" | "DENY";
  changeReason: string;
};

export const menuPermissionApi = {
  listMenuPermissions(
    params: {
      targetType?: string;
      targetId?: string;
      filter?: string;
      accessAllowed?: "ALLOW" | "DENY";
      page?: number;
      size?: number;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 10));
    if (params.targetType) query.set("targetType", params.targetType);
    if (params.targetId) query.set("targetId", params.targetId);
    if (params.filter) query.set("filter", params.filter);
    if (params.accessAllowed) query.set("accessAllowed", params.accessAllowed);
    return apiRequest<MenuPermissionSearchResponse>(
      `/api/admin/menu-permissions?${query.toString()}` as `/api/${string}`,
    );
  },
  saveMenuPermissions(payload: MenuPermissionPayload) {
    return apiRequest<MenuPermission>("/api/admin/menu-permissions", {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },
};

export type MenuTreeNode = {
  menuId: number;
  parentMenuId?: number | null;
  menuType: string;
  menuName: string;
  displayOrder: number;
  screenId?: string | null;
  url?: string | null;
  icon?: string | null;
  businessCategory?: string | null;
  description?: string | null;
  systemUseYn: string;
  status: string;
  changeReason?: string | null;
  updatedAt?: string;
  children: MenuTreeNode[];
};

export type MenuParentPayload = {
  parentMenuId: number | null;
  changeReason: string;
};

export type MenuReorderPayload = {
  parentMenuId: number | null;
  orderedMenuIds: number[];
  changeReason?: string;
};

export const menuStructureApi = {
  getMenuTree(params: { filter?: string } = {}) {
    const query = new URLSearchParams();
    if (params.filter) query.set("filter", params.filter);
    const suffix = query.toString() ? `?${query.toString()}` : "";
    return apiRequest<MenuTreeNode[]>(
      `/api/admin/menus/tree${suffix}` as `/api/${string}`,
    );
  },
  updateMenuParent(menuId: number, payload: MenuParentPayload) {
    return apiRequest<MenuTreeNode>(
      `/api/admin/menus/${encodeURIComponent(String(menuId))}/parent` as `/api/${string}`,
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
  reorderMenus(payload: MenuReorderPayload) {
    return apiRequest<MenuTreeNode[]>("/api/admin/menus/reorder", {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },
};

export type MenuUsageSetting = {
  menuId: number;
  parentMenuId?: number | null;
  topMenuName?: string | null;
  middleMenuName?: string | null;
  menuName: string;
  screenId?: string | null;
  url?: string | null;
  systemUseYn: "Y" | "N";
  exposureStartAt: string;
  exposureEndAt: string;
  status: string;
  changeReason?: string | null;
  updatedBy?: number | null;
  updatedAt?: string;
};

export type MenuUsageSearchResponse = {
  settings: MenuUsageSetting[];
  page: number;
  size: number;
  totalElements: number;
};

export type MenuUsagePayload = {
  items: Array<{
    menuId: number;
    systemUseYn: "Y" | "N";
    exposureStartAt: string;
    exposureEndAt: string;
    changeReason: string;
  }>;
};

export const menuUsageApi = {
  listMenuUsageSettings(
    params: {
      filter?: string;
      systemUseYn?: "Y" | "N";
      page?: number;
      size?: number;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 10));
    if (params.filter) query.set("filter", params.filter);
    if (params.systemUseYn) query.set("systemUseYn", params.systemUseYn);
    return apiRequest<MenuUsageSearchResponse>(
      `/api/admin/menus/usage-settings?${query.toString()}` as `/api/${string}`,
    );
  },
  saveMenuUsageSettings(payload: MenuUsagePayload) {
    return apiRequest<MenuUsageSetting[]>("/api/admin/menus/usage-settings", {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },
};

export type DetailCodeUsageSetting = {
  groupId: string;
  codeValue: string;
  codeName: string;
  systemUseYn: "Y" | "N";
  validStartDate?: string | null;
  validEndDate?: string | null;
  status: string;
  changeReason?: string | null;
  updatedBy?: number | null;
  updatedAt?: string;
  selectableForNewInput: boolean;
};

export type DetailCodeUsageSearchResponse = {
  settings: DetailCodeUsageSetting[];
  selectableOptions: DetailCodeUsageSetting[];
  page: number;
  size: number;
  totalElements: number;
};

export type DetailCodeUsagePayload = {
  items: Array<{
    codeValue: string;
    systemUseYn: "Y" | "N";
    validStartDate?: string | null;
    validEndDate?: string | null;
    changeReason: string;
  }>;
};

export const codeUsageApi = {
  listDetailCodeUsageSettings(
    groupId: string,
    params: { page?: number; size?: number } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 10));
    return apiRequest<DetailCodeUsageSearchResponse>(
      `/api/admin/code-groups/${encodeURIComponent(groupId)}/codes/usage-settings?${query.toString()}` as `/api/${string}`,
    );
  },
  saveDetailCodeUsageSettings(
    groupId: string,
    payload: DetailCodeUsagePayload,
  ) {
    return apiRequest<DetailCodeUsageSetting[]>(
      `/api/admin/code-groups/${encodeURIComponent(groupId)}/codes/usage-settings` as `/api/${string}`,
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type CommonSystemSettingKey =
  | "SESSION_IDLE_MINUTES"
  | "PAGE_SIZE"
  | "DEFAULT_SEARCH_PERIOD"
  | "BULK_QUERY_THRESHOLD"
  | "LONG_TASK_NOTICE_THRESHOLD";

export type CommonSystemSetting = {
  settingKey: CommonSystemSettingKey;
  settingValue: string;
  unit: string;
  changeReason?: string | null;
  updatedBy?: number | null;
  updatedAt?: string;
};

export type CommonSystemSettingsResponse = {
  settings: CommonSystemSetting[];
};

export type CommonSystemSettingsPayload = {
  settings: Array<{
    settingKey: CommonSystemSettingKey;
    settingValue: string;
    unit: string;
    changeReason: string;
  }>;
};

export const commonSystemSettingsApi = {
  getCommonSystemSettings() {
    return apiRequest<CommonSystemSettingsResponse>(
      "/api/admin/system-settings/common",
    );
  },
  saveCommonSystemSettings(payload: CommonSystemSettingsPayload) {
    return apiRequest<CommonSystemSettingsResponse>(
      "/api/admin/system-settings/common",
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type EvaluationYearPreparation = {
  targetYear: number;
  copyRequestedYn: "Y" | "N";
  resetRequestedYn: "Y" | "N";
  updatedBy?: number | null;
  updatedAt?: string;
  changeReason?: string | null;
};

export type EvaluationYearSettings = {
  currentEvaluationYear?: number | null;
  defaultSearchYear?: number | null;
  preparations: EvaluationYearPreparation[];
  updatedBy?: number | null;
  updatedAt?: string;
  changeReason?: string | null;
};

export type EvaluationYearPayload = {
  currentEvaluationYear: number;
  defaultSearchYear: number;
  changeReason: string;
  preparations: Array<{
    targetYear: number;
    copyRequestedYn: "Y" | "N";
    resetRequestedYn: "Y" | "N";
    changeReason: string;
  }>;
};

export const evaluationYearApi = {
  getEvaluationYearSettings() {
    return apiRequest<EvaluationYearSettings>(
      "/api/admin/system-settings/evaluation-years",
    );
  },
  saveEvaluationYearSettings(payload: EvaluationYearPayload) {
    return apiRequest<EvaluationYearSettings>(
      "/api/admin/system-settings/evaluation-years",
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type MenuExecution = {
  menuId: number;
  parentMenuId?: number | null;
  menuType: string;
  menuName: string;
  screenId: string;
  url: string;
  icon?: string;
  businessCategory?: string;
  description?: string;
  status: string;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type MenuExecutionPayload = {
  menuName: string;
  screenId: string;
  url: string;
  icon?: string;
  businessCategory?: string;
  description?: string;
  changeReason?: string;
};

export const menuExecutionApi = {
  getMenuExecution(menuId: number) {
    return apiRequest<MenuExecution>(
      `/api/admin/menus/${encodeURIComponent(String(menuId))}/execution` as `/api/${string}`,
    );
  },
  updateMenuExecution(menuId: number, payload: MenuExecutionPayload) {
    return apiRequest<MenuExecution>(
      `/api/admin/menus/${encodeURIComponent(String(menuId))}/execution` as `/api/${string}`,
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
};

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

export type DataScope = "SELF" | "DEPARTMENT" | "COLLEGE" | "BUSINESS" | "ALL";

export type EvaluationOrganizationMapping = {
  mappingId: number;
  userId: number;
  loginId?: string;
  userName?: string;
  organizationCode: string;
  organizationName?: string;
  businessType: BusinessType;
  dataScope: DataScope;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type EvaluationOrganizationMappingSearchResponse = {
  mappings: EvaluationOrganizationMapping[];
  page: number;
  size: number;
  totalElements: number;
};

export type EvaluationOrganizationMappingPayload = {
  userId: number;
  organizationCode: string;
  businessType: BusinessType;
  dataScope: DataScope;
  changeReason: string;
};

export const evaluationOrganizationMappingApi = {
  listEvaluationOrganizationMappings(
    params: {
      businessType?: BusinessType | "";
      organizationCode?: string;
      userId?: number;
      page?: number;
      size?: number;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.businessType) query.set("businessType", params.businessType);
    if (params.organizationCode?.trim()) {
      query.set("organizationCode", params.organizationCode.trim());
    }
    if (params.userId !== undefined && Number.isFinite(params.userId)) {
      query.set("userId", String(params.userId));
    }
    return apiRequest<EvaluationOrganizationMappingSearchResponse>(
      `/api/business/evaluation-organization-mappings?${query.toString()}` as `/api/${string}`,
    );
  },
  saveEvaluationOrganizationMapping(
    payload: EvaluationOrganizationMappingPayload,
  ) {
    return apiRequest<EvaluationOrganizationMapping>(
      "/api/business/evaluation-organization-mappings",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type FunctionType = "READ" | "CREATE" | "UPDATE" | "DELETE" | "EXECUTE";
export type PermissionAllowed = "ALLOW" | "DENY";

export type FunctionPermission = {
  functionPermissionId: number;
  screenId: string;
  screenName?: string;
  roleCode: string;
  roleName?: string;
  functionType: FunctionType;
  permissionAllowed: PermissionAllowed;
  changeReason?: string;
  updatedAt?: string;
};

export type FunctionPermissionSearchResponse = {
  permissions: FunctionPermission[];
  page: number;
  size: number;
  totalElements: number;
};

export type FunctionPermissionPayload = {
  screenId: string;
  roleCode: string;
  functionType: FunctionType;
  permissionAllowed: PermissionAllowed;
  changeReason: string;
};

export type FunctionPermissionEvaluatePayload = {
  screenId: string;
  roleCode: string;
  functionType: FunctionType;
  targetDataStatus: string;
  dataScopeRef?: string;
};

export type FunctionPermissionEvaluateResult =
  FunctionPermissionEvaluatePayload & {
    allowed: boolean;
    reason: string;
  };

export const functionPermissionApi = {
  listFunctionPermissions(
    params: {
      screenId?: string;
      roleCode?: string;
      page?: number;
      size?: number;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 10));
    if (params.screenId) query.set("screenId", params.screenId);
    if (params.roleCode) query.set("roleCode", params.roleCode);
    return apiRequest<FunctionPermissionSearchResponse>(
      `/api/admin/function-permissions?${query.toString()}` as `/api/${string}`,
    );
  },
  saveFunctionPermissions(payload: FunctionPermissionPayload) {
    return apiRequest<FunctionPermission>(
      "/api/admin/function-permissions-save",
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
  evaluateFunctionPermission(payload: FunctionPermissionEvaluatePayload) {
    return apiRequest<FunctionPermissionEvaluateResult>(
      "/api/admin/function-permissions/evaluate",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type PrivacyGrade = "PUBLIC" | "PERSONAL" | "SENSITIVE" | "ACCOUNT";
export type YesNo = "Y" | "N";

export type PrivacyFieldPolicy = {
  policyId: number;
  fieldKey: string;
  privacyGrade: PrivacyGrade;
  encryptionRequiredYn: YesNo;
  maskingRule?: string | null;
  logExclusionYn: YesNo;
  changeReason?: string;
  updatedAt?: string;
  updatedBy?: number;
};

export type PrivacyFieldPolicySearchResponse = {
  policies: PrivacyFieldPolicy[];
  page: number;
  size: number;
  totalElements: number;
};

export type PrivacyFieldPolicyPayload = {
  fieldKey: string;
  privacyGrade: PrivacyGrade;
  encryptionRequiredYn: YesNo;
  maskingRule?: string | null;
  logExclusionYn: YesNo;
  changeReason: string;
};

export const privacyPolicyApi = {
  listPrivacyFieldPolicies(
    params: {
      fieldKey?: string;
      privacyGrade?: string;
      encryptionRequiredYn?: string;
      page?: number;
      size?: number;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.fieldKey) query.set("fieldKey", params.fieldKey);
    if (params.privacyGrade) query.set("privacyGrade", params.privacyGrade);
    if (params.encryptionRequiredYn)
      query.set("encryptionRequiredYn", params.encryptionRequiredYn);
    return apiRequest<PrivacyFieldPolicySearchResponse>(
      `/api/admin/privacy/policies?${query.toString()}` as `/api/${string}`,
    );
  },
  savePrivacyFieldPolicies(payload: PrivacyFieldPolicyPayload[]) {
    return apiRequest<PrivacyFieldPolicy[]>(
      "/api/admin/privacy/policies-save",
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type PrivacyProcessType = "VIEW" | "PRINT" | "DOWNLOAD";
export type PrivacyProcessResult = "SUCCESS" | "DENIED" | "FAILED";

export type PrivacyAccessLog = {
  historyId: number;
  processType: PrivacyProcessType;
  actorUserId: number;
  actorLoginId?: string | null;
  targetRef: string;
  processPurpose: string;
  processedAt: string;
  requestIp: string;
  processResult: PrivacyProcessResult;
};

export type PrivacyAccessLogSearchResponse = {
  logs: PrivacyAccessLog[];
  page: number;
  size: number;
  totalElements: number;
};

export const privacyAccessLogApi = {
  searchPrivacyAccessLogs(
    params: {
      actorUserId?: number;
      targetRef?: string;
      processType?: PrivacyProcessType | "";
      processedFrom?: string;
      processedTo?: string;
      page?: number;
      size?: number;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.actorUserId !== undefined)
      query.set("actorUserId", String(params.actorUserId));
    if (params.targetRef?.trim())
      query.set("targetRef", params.targetRef.trim());
    if (params.processType) query.set("processType", params.processType);
    if (params.processedFrom) query.set("processedFrom", params.processedFrom);
    if (params.processedTo) query.set("processedTo", params.processedTo);
    return apiRequest<PrivacyAccessLogSearchResponse>(
      `/api/admin/privacy/access-logs?${query.toString()}` as `/api/${string}`,
    );
  },
  getPrivacyAccessLog(historyId: number) {
    return apiRequest<PrivacyAccessLog>(
      `/api/admin/privacy/access-logs/${encodeURIComponent(String(historyId))}` as `/api/${string}`,
    );
  },
};

export type PrivacyAccessType =
  | "RAW_VIEW"
  | "MASKED_VIEW"
  | "EXPORT"
  | "ACCOUNT_VIEW";

export type PrivacyAccessPermission = {
  permissionId: number;
  roleCode: string;
  roleName?: string;
  fieldKey: string;
  rawViewAllowedYn: YesNo;
  maskedViewAllowedYn: YesNo;
  exportAllowedYn: YesNo;
  accountViewAllowedYn: YesNo;
  changeReason?: string;
  updatedAt?: string;
  updatedBy?: number;
};

export type PrivacyAccessPermissionSearchResponse = {
  permissions: PrivacyAccessPermission[];
  page: number;
  size: number;
  totalElements: number;
};

export type PrivacyAccessPermissionPayload = {
  roleCode: string;
  fieldKey: string;
  rawViewAllowedYn: YesNo;
  maskedViewAllowedYn: YesNo;
  exportAllowedYn: YesNo;
  accountViewAllowedYn: YesNo;
  changeReason: string;
};

export type PrivacyAccessEvaluatePayload = {
  roleCode: string;
  fieldKey: string;
  accessType: PrivacyAccessType;
  processPurpose: string;
};

export type PrivacyAccessEvaluateResult = {
  roleCode: string;
  fieldKey: string;
  accessType: PrivacyAccessType;
  allowed: boolean;
  reason: string;
  rawValueExposed: boolean;
};

export const privacyPermissionApi = {
  listPrivacyAccessPermissions(
    params: {
      roleCode?: string;
      fieldKey?: string;
      page?: number;
      size?: number;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.roleCode) query.set("roleCode", params.roleCode);
    if (params.fieldKey) query.set("fieldKey", params.fieldKey);
    return apiRequest<PrivacyAccessPermissionSearchResponse>(
      `/api/admin/privacy/permissions?${query.toString()}` as `/api/${string}`,
    );
  },
  savePrivacyAccessPermissions(payload: PrivacyAccessPermissionPayload[]) {
    return apiRequest<PrivacyAccessPermission[]>(
      "/api/admin/privacy/permissions-save",
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
  evaluatePrivacyAccessPermission(payload: PrivacyAccessEvaluatePayload) {
    return apiRequest<PrivacyAccessEvaluateResult>(
      "/api/admin/privacy/permissions/evaluate",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type PeriodState = "BEFORE" | "ACTIVE" | "AFTER";

export type PeriodPermission = {
  periodPermissionLinkId: number;
  businessPeriodId: string;
  functionPermissionId: number;
  screenId: string;
  screenName?: string;
  roleCode: string;
  roleName?: string;
  functionType: FunctionType;
  permissionAllowed: PermissionAllowed;
  effectiveStartAt: string;
  effectiveEndAt?: string | null;
  periodState: PeriodState;
  effectiveAllowed: boolean;
  changeReason?: string;
  updatedAt?: string;
};

export type PeriodPermissionSearchResponse = {
  links: PeriodPermission[];
  page: number;
  size: number;
  totalElements: number;
};

export type PeriodPermissionPayload = {
  businessPeriodId: string;
  functionPermissionId: number;
  effectiveStartAt: string;
  effectiveEndAt?: string | null;
  changeReason: string;
};

export const periodPermissionApi = {
  listPeriodPermissions(
    params: { businessPeriodId?: string; page?: number; size?: number } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 10));
    if (params.businessPeriodId)
      query.set("businessPeriodId", params.businessPeriodId);
    return apiRequest<PeriodPermissionSearchResponse>(
      `/api/admin/period-permissions?${query.toString()}` as `/api/${string}`,
    );
  },
  savePeriodPermissions(payload: PeriodPermissionPayload) {
    return apiRequest<PeriodPermission>("/api/admin/period-permissions-save", {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },
};

export type TemporaryPermissionStatus = "ACTIVE" | "REVOKED" | "EXPIRED";

export type TemporaryPermission = {
  temporaryPermissionId: number;
  userId: number;
  userName?: string;
  workDataRef: string;
  functionType: FunctionType;
  validStartAt: string;
  validEndAt: string;
  status: TemporaryPermissionStatus;
  changeReason?: string;
  updatedAt?: string;
};

export type TemporaryPermissionSearchResponse = {
  permissions: TemporaryPermission[];
  page: number;
  size: number;
  totalElements: number;
};

export type TemporaryPermissionPayload = {
  userId: number;
  workDataRef: string;
  functionType: FunctionType;
  validStartAt: string;
  validEndAt: string;
  changeReason: string;
};

export const temporaryPermissionApi = {
  listTemporaryPermissions(
    params: { userId?: number; page?: number; size?: number } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 10));
    if (params.userId !== undefined) query.set("userId", String(params.userId));
    return apiRequest<TemporaryPermissionSearchResponse>(
      `/api/admin/temporary-permissions?${query.toString()}` as `/api/${string}`,
    );
  },
  createTemporaryPermission(payload: TemporaryPermissionPayload) {
    return apiRequest<TemporaryPermission>(
      "/api/admin/temporary-permissions-create",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type PermissionHistoryTargetType =
  | "ROLE"
  | "MENU"
  | "FUNCTION"
  | "DATA_SCOPE"
  | "TEMPORARY";

export type PermissionChangeHistory = {
  permissionHistoryId: number;
  targetType: PermissionHistoryTargetType;
  targetId: string;
  beforeValue?: string | null;
  afterValue?: string | null;
  changedBy: number;
  reason: string;
  changedAt: string;
};

export type PermissionChangeHistorySearchResponse = {
  history: PermissionChangeHistory[];
  page: number;
  size: number;
  total: number;
};

export const permissionHistoryApi = {
  listPermissionChangeHistory(
    params: {
      targetType?: PermissionHistoryTargetType | "";
      targetId?: string;
      page?: number;
      size?: number;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 10));
    if (params.targetType) query.set("targetType", params.targetType);
    if (params.targetId?.trim()) query.set("targetId", params.targetId.trim());
    return apiRequest<PermissionChangeHistorySearchResponse>(
      `/api/admin/permission-history?${query.toString()}` as `/api/${string}`,
    );
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

export type PageSize = 20 | 50 | 100;

export type BusinessType =
  | "FACULTY_ACHIEVEMENT"
  | "ACADEMIC_GRANT"
  | "OBJECTION";
export type DefinitionVersion = "DRAFT" | "CONFIRMED" | "DISCARDED";
export type SystemUseYn = "Y" | "N";

export type BusinessStatusCode = {
  statusCodeId: number;
  definitionVersion: DefinitionVersion;
  businessType: BusinessType;
  statusCode: string;
  displayName: string;
  systemUseYn: SystemUseYn;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type BusinessStatusCodeSearchResponse = {
  statusCodes: BusinessStatusCode[];
  page: number;
  size: number;
  totalElements: number;
};

export type BusinessStatusCodePayload = {
  definitionVersion: DefinitionVersion;
  businessType: BusinessType;
  statusCode: string;
  displayName: string;
  systemUseYn: SystemUseYn;
  changeReason: string;
};

export const businessStatusCodeApi = {
  listBusinessStatusCodes(
    params: {
      page?: number;
      size?: PageSize;
      businessType?: BusinessType;
      definitionVersion?: DefinitionVersion;
      statusCode?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.businessType) query.set("businessType", params.businessType);
    if (params.definitionVersion)
      query.set("definitionVersion", params.definitionVersion);
    if (params.statusCode?.trim())
      query.set("statusCode", params.statusCode.trim());
    return apiRequest<BusinessStatusCodeSearchResponse>(
      `/api/admin/business-status-codes?${query.toString()}` as `/api/${string}`,
    );
  },
  saveBusinessStatusCode(payload: BusinessStatusCodePayload) {
    return apiRequest<BusinessStatusCode>("/api/admin/business-status-codes", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};

export type EvaluationRuleVersionStatus = "DRAFT" | "CONFIRMED" | "DISCARDED";
export type ActiveYn = "Y" | "N";

export type EvaluationDateSetting = {
  settingId: number;
  evaluationYear: string;
  areaCode?: string | null;
  organizationCode?: string | null;
  userTypeCode?: string | null;
  startAt: string;
  endAt: string;
  baseDate?: string | null;
  activeYn: ActiveYn;
  createdBy?: number | null;
  updatedBy?: number | null;
  createdAt?: string;
  updatedAt?: string;
  changeReason?: string;
};

export type EvaluationDateSearchResponse = {
  evaluationDates: EvaluationDateSetting[];
  page: number;
  pageSize: PageSize;
  totalElements: number;
};

export type EvaluationDatePayload = {
  settingId?: number | null;
  evaluationYear: string;
  areaCode?: string | null;
  organizationCode: string;
  userTypeCode?: string | null;
  startAt: string;
  endAt: string;
  baseDate: string;
  activeYn: ActiveYn;
  changeReason: string;
};

export const evaluationDateApi = {
  listEvaluationDates(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      areaCode?: string;
      organizationCode?: string;
      userTypeCode?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim()) {
      query.set("evaluationYear", params.evaluationYear.trim());
    }
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.organizationCode?.trim()) {
      query.set("organizationCode", params.organizationCode.trim());
    }
    if (params.userTypeCode?.trim()) {
      query.set("userTypeCode", params.userTypeCode.trim());
    }
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<EvaluationDateSearchResponse>(
      `/api/admin/evaluation-dates?${query.toString()}` as `/api/${string}`,
    );
  },
  saveEvaluationDate(payload: EvaluationDatePayload) {
    return apiRequest<EvaluationDateSetting>(
      "/api/admin/evaluation-dates/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type AppealPeriodSetting = {
  settingId: number;
  evaluationYear: string;
  collegeOrganizationCode: string;
  departmentOrganizationCode?: string | null;
  appealStartAt: string;
  appealEndAt: string;
  handlerUserId: number;
  activeYn: ActiveYn;
  createdBy?: number | null;
  updatedBy?: number | null;
  createdAt?: string;
  updatedAt?: string;
  changeReason?: string;
};

export type AppealPeriodSearchResponse = {
  appealPeriods: AppealPeriodSetting[];
  page: number;
  pageSize: PageSize;
  totalElements: number;
};

export type AppealPeriodPayload = {
  settingId?: number | null;
  evaluationYear: string;
  collegeOrganizationCode: string;
  departmentOrganizationCode?: string | null;
  appealStartAt: string;
  appealEndAt: string;
  handlerUserId: number;
  activeYn: ActiveYn;
  changeReason: string;
};

export const appealPeriodApi = {
  listAppealPeriods(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      collegeOrganizationCode?: string;
      departmentOrganizationCode?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim())
      query.set("evaluationYear", params.evaluationYear.trim());
    if (params.collegeOrganizationCode?.trim())
      query.set(
        "collegeOrganizationCode",
        params.collegeOrganizationCode.trim(),
      );
    if (params.departmentOrganizationCode?.trim())
      query.set(
        "departmentOrganizationCode",
        params.departmentOrganizationCode.trim(),
      );
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<AppealPeriodSearchResponse>(
      `/api/admin/appeal-periods?${query.toString()}` as `/api/${string}`,
    );
  },
  saveAppealPeriod(payload: AppealPeriodPayload) {
    return apiRequest<AppealPeriodSetting>("/api/admin/appeal-periods/save", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};

export type VisibilityScope =
  | "SELF"
  | "DEPARTMENT"
  | "COLLEGE"
  | "BUSINESS"
  | "ALL";

export type ResultViewPeriodSetting = {
  settingId: number;
  evaluationYear: string;
  collegeOrganizationCode: string;
  departmentOrganizationCode?: string | null;
  viewStartAt: string;
  viewEndAt: string;
  visibilityScope: VisibilityScope;
  activeYn: ActiveYn;
  createdBy?: number | null;
  updatedBy?: number | null;
  createdAt?: string;
  updatedAt?: string;
  changeReason?: string;
};

export type ResultViewPeriodSearchResponse = {
  resultViewPeriods: ResultViewPeriodSetting[];
  page: number;
  pageSize: PageSize;
  totalElements: number;
};

export type ResultViewPeriodPayload = {
  settingId?: number | null;
  evaluationYear: string;
  collegeOrganizationCode: string;
  departmentOrganizationCode?: string | null;
  viewStartAt: string;
  viewEndAt: string;
  visibilityScope: VisibilityScope;
  activeYn: ActiveYn;
  changeReason: string;
};

export const resultViewPeriodApi = {
  listResultViewPeriods(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      collegeOrganizationCode?: string;
      departmentOrganizationCode?: string;
      visibilityScope?: VisibilityScope | "";
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim())
      query.set("evaluationYear", params.evaluationYear.trim());
    if (params.collegeOrganizationCode?.trim())
      query.set(
        "collegeOrganizationCode",
        params.collegeOrganizationCode.trim(),
      );
    if (params.departmentOrganizationCode?.trim())
      query.set(
        "departmentOrganizationCode",
        params.departmentOrganizationCode.trim(),
      );
    if (params.visibilityScope)
      query.set("visibilityScope", params.visibilityScope);
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<ResultViewPeriodSearchResponse>(
      `/api/admin/result-view-periods?${query.toString()}` as `/api/${string}`,
    );
  },
  saveResultViewPeriod(payload: ResultViewPeriodPayload) {
    return apiRequest<ResultViewPeriodSetting>(
      "/api/admin/result-view-periods/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type TargetFunctionCode = "MODIFY_ACHIEVEMENT" | "DELETE_ACHIEVEMENT";

export type ExceptionPeriodSetting = {
  settingId: number;
  evaluationYear: string;
  teacherUserId: number;
  teacherName?: string | null;
  areaCode: string;
  targetFunctionCode: TargetFunctionCode;
  exceptionStartAt: string;
  exceptionEndAt: string;
  approvalReason: string;
  activeYn: ActiveYn;
  createdBy?: number | null;
  updatedBy?: number | null;
  createdAt?: string;
  updatedAt?: string;
  changeReason?: string;
};

export type ExceptionPeriodSearchResponse = {
  exceptionPeriods: ExceptionPeriodSetting[];
  page: number;
  pageSize: PageSize;
  totalElements: number;
};

export type ExceptionPeriodPayload = {
  settingId?: number | null;
  evaluationYear: string;
  teacherUserId: number;
  areaCode: string;
  targetFunctionCode: TargetFunctionCode;
  exceptionStartAt: string;
  exceptionEndAt: string;
  approvalReason: string;
  activeYn: ActiveYn;
  changeReason: string;
};

export const exceptionPeriodApi = {
  listExceptionPeriods(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      teacherUserId?: string | number;
      areaCode?: string;
      targetFunctionCode?: TargetFunctionCode | "";
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim())
      query.set("evaluationYear", params.evaluationYear.trim());
    if (
      params.teacherUserId !== undefined &&
      String(params.teacherUserId).trim()
    )
      query.set("teacherUserId", String(params.teacherUserId).trim());
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.targetFunctionCode)
      query.set("targetFunctionCode", params.targetFunctionCode);
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<ExceptionPeriodSearchResponse>(
      `/api/admin/exception-periods?${query.toString()}` as `/api/${string}`,
    );
  },
  saveExceptionPeriod(payload: ExceptionPeriodPayload) {
    return apiRequest<ExceptionPeriodSetting>(
      "/api/admin/exception-periods/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type InputPeriodSetting = EvaluationDateSetting;

export type InputPeriodSearchResponse = {
  inputPeriods: InputPeriodSetting[];
  page: number;
  pageSize: PageSize;
  totalElements: number;
};

export type InputPeriodPayload = EvaluationDatePayload;

export const inputPeriodApi = {
  listInputPeriods(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      areaCode?: string;
      organizationCode?: string;
      userTypeCode?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim()) {
      query.set("evaluationYear", params.evaluationYear.trim());
    }
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.organizationCode?.trim()) {
      query.set("organizationCode", params.organizationCode.trim());
    }
    if (params.userTypeCode?.trim()) {
      query.set("userTypeCode", params.userTypeCode.trim());
    }
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<InputPeriodSearchResponse>(
      `/api/admin/input-periods?${query.toString()}` as `/api/${string}`,
    );
  },
  saveInputPeriod(payload: InputPeriodPayload) {
    return apiRequest<InputPeriodSetting>("/api/admin/input-periods/save", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};

export type ModificationPeriodSetting = EvaluationDateSetting;

export type ModificationPeriodSearchResponse = {
  modificationPeriods: ModificationPeriodSetting[];
  page: number;
  pageSize: PageSize;
  totalElements: number;
};

export type ModificationPeriodPayload = EvaluationDatePayload;

export const modificationPeriodApi = {
  listModificationPeriods(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      areaCode?: string;
      organizationCode?: string;
      userTypeCode?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim()) {
      query.set("evaluationYear", params.evaluationYear.trim());
    }
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.organizationCode?.trim()) {
      query.set("organizationCode", params.organizationCode.trim());
    }
    if (params.userTypeCode?.trim()) {
      query.set("userTypeCode", params.userTypeCode.trim());
    }
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<ModificationPeriodSearchResponse>(
      `/api/admin/modification-periods?${query.toString()}` as `/api/${string}`,
    );
  },
  saveModificationPeriod(payload: ModificationPeriodPayload) {
    return apiRequest<ModificationPeriodSetting>(
      "/api/admin/modification-periods/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type DepartmentChairConfirmPeriodSetting = EvaluationDateSetting;

export type DepartmentChairConfirmPeriodSearchResponse = {
  departmentChairConfirmPeriods: DepartmentChairConfirmPeriodSetting[];
  page: number;
  pageSize: PageSize;
  totalElements: number;
};

export type DepartmentChairConfirmPeriodPayload = EvaluationDatePayload;

export const departmentChairConfirmPeriodApi = {
  listDepartmentChairConfirmPeriods(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      areaCode?: string;
      organizationCode?: string;
      userTypeCode?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim()) {
      query.set("evaluationYear", params.evaluationYear.trim());
    }
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.organizationCode?.trim()) {
      query.set("organizationCode", params.organizationCode.trim());
    }
    if (params.userTypeCode?.trim()) {
      query.set("userTypeCode", params.userTypeCode.trim());
    }
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<DepartmentChairConfirmPeriodSearchResponse>(
      `/api/admin/department-chair-confirm-periods?${query.toString()}` as `/api/${string}`,
    );
  },
  saveDepartmentChairConfirmPeriod(
    payload: DepartmentChairConfirmPeriodPayload,
  ) {
    return apiRequest<DepartmentChairConfirmPeriodSetting>(
      "/api/admin/department-chair-confirm-periods/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type BusinessPeriodSetting = EvaluationDateSetting;

export type BusinessPeriodSearchResponse = {
  businessPeriods: BusinessPeriodSetting[];
  page: number;
  pageSize: PageSize;
  totalElements: number;
};

export type BusinessPeriodPayload = EvaluationDatePayload;

export const businessPeriodApi = {
  listBusinessPeriods(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      areaCode?: string;
      organizationCode?: string;
      userTypeCode?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim()) {
      query.set("evaluationYear", params.evaluationYear.trim());
    }
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.organizationCode?.trim()) {
      query.set("organizationCode", params.organizationCode.trim());
    }
    if (params.userTypeCode?.trim()) {
      query.set("userTypeCode", params.userTypeCode.trim());
    }
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<BusinessPeriodSearchResponse>(
      `/api/admin/business-periods?${query.toString()}` as `/api/${string}`,
    );
  },
  saveBusinessPeriod(payload: BusinessPeriodPayload) {
    return apiRequest<BusinessPeriodSetting>(
      "/api/admin/business-periods/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type EvaluationArea = {
  areaId: number;
  ruleVersionId: number;
  versionCode: string;
  versionStatus: EvaluationRuleVersionStatus;
  areaCode: string;
  areaName: string;
  sortOrder: number;
  activeYn: ActiveYn;
  periodApplyMethod: string;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type EvaluationAreaSearchResponse = {
  evaluationAreas: EvaluationArea[];
  page: number;
  size: number;
  totalElements: number;
};

export type EvaluationAreaPayload = {
  ruleVersionId: number;
  areaCode: string;
  areaName: string;
  sortOrder: number;
  activeYn: ActiveYn;
  periodApplyMethod: string;
  changeReason: string;
};

export const evaluationAreaApi = {
  listEvaluationAreas(
    params: {
      page?: number;
      size?: PageSize;
      ruleVersionId?: number;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.ruleVersionId)
      query.set("ruleVersionId", String(params.ruleVersionId));
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<EvaluationAreaSearchResponse>(
      `/api/admin/evaluation-areas?${query.toString()}` as `/api/${string}`,
    );
  },
  saveEvaluationArea(payload: EvaluationAreaPayload) {
    return apiRequest<EvaluationArea>("/api/admin/evaluation-areas/save", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};

export type EvaluationItem = {
  itemId: number;
  areaId: number;
  ruleVersionId: number;
  versionCode: string;
  versionStatus: EvaluationRuleVersionStatus;
  areaCode: string;
  areaName: string;
  itemCode: string;
  itemName: string;
  parentItemCode?: string | null;
  sortOrder: number;
  activeYn: ActiveYn;
  scoreApplyMethod: string;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type EvaluationItemSearchResponse = {
  evaluationItems: EvaluationItem[];
  page: number;
  size: number;
  totalElements: number;
};

export type EvaluationItemPayload = {
  ruleVersionId: number;
  areaCode: string;
  itemCode: string;
  itemName: string;
  parentItemCode?: string | null;
  sortOrder: number;
  activeYn: ActiveYn;
  scoreApplyMethod: string;
  changeReason: string;
};

export const evaluationItemApi = {
  listEvaluationItems(
    params: {
      page?: number;
      size?: PageSize;
      ruleVersionId?: number;
      areaCode?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.ruleVersionId)
      query.set("ruleVersionId", String(params.ruleVersionId));
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<EvaluationItemSearchResponse>(
      `/api/admin/evaluation-items?${query.toString()}` as `/api/${string}`,
    );
  },
  saveEvaluationItem(payload: EvaluationItemPayload) {
    return apiRequest<EvaluationItem>("/api/admin/evaluation-items/save", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};

export type EvaluationElement = {
  elementId: number;
  itemId: number;
  areaId: number;
  ruleVersionId: number;
  versionCode: string;
  versionStatus: EvaluationRuleVersionStatus;
  areaCode: string;
  areaName: string;
  itemCode: string;
  itemName: string;
  evaluationYear: string;
  elementCode: string;
  elementName: string;
  sortOrder: number;
  activeYn: ActiveYn;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type EvaluationElementSearchResponse = {
  evaluationElements: EvaluationElement[];
  page: number;
  size: number;
  totalElements: number;
};

export type EvaluationElementPayload = {
  ruleVersionId: number;
  areaCode: string;
  itemCode: string;
  evaluationYear: string;
  elementCode: string;
  elementName: string;
  sortOrder: number;
  activeYn: ActiveYn;
  changeReason: string;
};

export const evaluationElementApi = {
  listEvaluationElements(
    params: {
      page?: number;
      size?: PageSize;
      ruleVersionId?: number;
      areaCode?: string;
      itemCode?: string;
      evaluationYear?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.ruleVersionId)
      query.set("ruleVersionId", String(params.ruleVersionId));
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.itemCode?.trim()) query.set("itemCode", params.itemCode.trim());
    if (params.evaluationYear?.trim())
      query.set("evaluationYear", params.evaluationYear.trim());
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<EvaluationElementSearchResponse>(
      `/api/admin/evaluation-elements?${query.toString()}` as `/api/${string}`,
    );
  },
  saveEvaluationElement(payload: EvaluationElementPayload) {
    return apiRequest<EvaluationElement>(
      "/api/admin/evaluation-elements/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type EvaluationManagementItem = EvaluationElement & {
  managementItemId: number;
  managementItemCode: string;
  managementItemName: string;
  teacherEditableYn: ActiveYn;
  requiredYn: ActiveYn;
  dataType: "TEXT" | "NUMBER" | "DATE" | "BOOLEAN" | "CODE" | "FILE";
};

export type EvaluationManagementItemSearchResponse = {
  evaluationManagementItems: EvaluationManagementItem[];
  page: number;
  size: number;
  totalElements: number;
};

export type EvaluationManagementItemPayload = Omit<
  EvaluationElementPayload,
  "elementName"
> & {
  managementItemCode: string;
  managementItemName: string;
  teacherEditableYn: ActiveYn;
  requiredYn: ActiveYn;
  dataType: EvaluationManagementItem["dataType"];
};

export const evaluationManagementItemApi = {
  listEvaluationManagementItems(
    params: {
      page?: number;
      size?: PageSize;
      ruleVersionId?: number;
      areaCode?: string;
      itemCode?: string;
      evaluationYear?: string;
      elementCode?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.ruleVersionId)
      query.set("ruleVersionId", String(params.ruleVersionId));
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.itemCode?.trim()) query.set("itemCode", params.itemCode.trim());
    if (params.evaluationYear?.trim())
      query.set("evaluationYear", params.evaluationYear.trim());
    if (params.elementCode?.trim())
      query.set("elementCode", params.elementCode.trim());
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<EvaluationManagementItemSearchResponse>(
      `/api/admin/evaluation-management-items?${query.toString()}` as `/api/${string}`,
    );
  },
  saveEvaluationManagementItem(payload: EvaluationManagementItemPayload) {
    return apiRequest<EvaluationManagementItem>(
      "/api/admin/evaluation-management-items/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type EvaluationScore = {
  scoreRuleId: number;
  ruleVersionId: number;
  versionCode: string;
  versionStatus: EvaluationRuleVersionStatus;
  managementItemId: number;
  areaCode: string;
  areaName: string;
  itemCode: string;
  itemName: string;
  evaluationYear: string;
  elementCode: string;
  elementName: string;
  managementItemCode: string;
  managementItemName: string;
  organizationCode: string;
  organizationName?: string | null;
  baseScore: number;
  maxScore?: number | null;
  effectiveStartDate: string;
  effectiveEndDate: string;
  activeYn: ActiveYn;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type EvaluationScoreSearchResponse = {
  evaluationScores: EvaluationScore[];
  page: number;
  pageSize: number;
  totalElements: number;
};

export type EvaluationScorePayload = {
  ruleVersionId: number;
  managementItemId: number;
  organizationCode: string;
  evaluationYear: string;
  baseScore: number;
  maxScore?: number | null;
  effectiveStartDate: string;
  effectiveEndDate: string;
  activeYn: ActiveYn;
  changeReason: string;
};

export const evaluationScoreApi = {
  listEvaluationScores(
    params: {
      page?: number;
      pageSize?: PageSize;
      ruleVersionId?: number;
      managementItemId?: number;
      areaCode?: string;
      itemCode?: string;
      evaluationYear?: string;
      elementCode?: string;
      managementItemCode?: string;
      organizationCode?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("pageSize", String(params.pageSize ?? 20));
    if (params.ruleVersionId)
      query.set("ruleVersionId", String(params.ruleVersionId));
    if (params.managementItemId)
      query.set("managementItemId", String(params.managementItemId));
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.itemCode?.trim()) query.set("itemCode", params.itemCode.trim());
    if (params.evaluationYear?.trim())
      query.set("evaluationYear", params.evaluationYear.trim());
    if (params.elementCode?.trim())
      query.set("elementCode", params.elementCode.trim());
    if (params.managementItemCode?.trim())
      query.set("managementItemCode", params.managementItemCode.trim());
    if (params.organizationCode?.trim())
      query.set("organizationCode", params.organizationCode.trim());
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<EvaluationScoreSearchResponse>(
      `/api/admin/evaluation-scores?${query.toString()}` as `/api/${string}`,
    );
  },
  saveEvaluationScore(payload: EvaluationScorePayload) {
    return apiRequest<EvaluationScore>("/api/admin/evaluation-scores/save", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};

export type ParticipationRate = Omit<
  EvaluationScore,
  | "scoreRuleId"
  | "organizationCode"
  | "organizationName"
  | "baseScore"
  | "maxScore"
> & {
  participationRateRuleId: number;
  researcherCount: number;
  participationType: string;
  participationTypeName?: string | null;
  distributionRate: number;
};

export type ParticipationRateSearchResponse = {
  participationRates: ParticipationRate[];
  page: number;
  pageSize: number;
  totalElements: number;
};

export type ParticipationRatePayload = {
  ruleVersionId: number;
  managementItemId: number;
  researcherCount: number;
  participationType: string;
  distributionRate: number;
  effectiveStartDate: string;
  effectiveEndDate: string;
  activeYn: ActiveYn;
  changeReason: string;
};

export const participationRateApi = {
  listParticipationRates(
    params: {
      page?: number;
      pageSize?: PageSize;
      ruleVersionId?: number;
      managementItemId?: number;
      areaCode?: string;
      itemCode?: string;
      evaluationYear?: string;
      elementCode?: string;
      managementItemCode?: string;
      researcherCount?: number;
      participationType?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("pageSize", String(params.pageSize ?? 20));
    if (params.ruleVersionId)
      query.set("ruleVersionId", String(params.ruleVersionId));
    if (params.managementItemId)
      query.set("managementItemId", String(params.managementItemId));
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.itemCode?.trim()) query.set("itemCode", params.itemCode.trim());
    if (params.evaluationYear?.trim())
      query.set("evaluationYear", params.evaluationYear.trim());
    if (params.elementCode?.trim())
      query.set("elementCode", params.elementCode.trim());
    if (params.managementItemCode?.trim())
      query.set("managementItemCode", params.managementItemCode.trim());
    if (params.researcherCount)
      query.set("researcherCount", String(params.researcherCount));
    if (params.participationType?.trim())
      query.set("participationType", params.participationType.trim());
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<ParticipationRateSearchResponse>(
      `/api/admin/participation-rates?${query.toString()}` as `/api/${string}`,
    );
  },
  saveParticipationRate(payload: ParticipationRatePayload) {
    return apiRequest<ParticipationRate>(
      "/api/admin/participation-rates/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type CalculationType =
  | "FIXED_SCORE"
  | "DISTRIBUTION_RATE"
  | "CAP"
  | "LADDER";

export type CalculationFormula = {
  formulaVersionId: number;
  ruleVersionId: number;
  versionCode: string;
  versionStatus: EvaluationRuleVersionStatus;
  formulaCode: string;
  calculationType: CalculationType;
  calculationTypeName?: string | null;
  variableDefinition: string;
  roundingRule: string;
  lowerBoundScore?: number | null;
  upperBoundScore?: number | null;
  evaluationYear: string;
  effectiveStartDate: string;
  effectiveEndDate: string;
  activeYn: ActiveYn;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type CalculationFormulaSearchResponse = {
  calculationFormulas: CalculationFormula[];
  page: number;
  pageSize: number;
  totalElements: number;
};

export type CalculationFormulaPayload = {
  ruleVersionId: number;
  formulaCode: string;
  calculationType: CalculationType;
  variableDefinition: string;
  roundingRule: string;
  lowerBoundScore?: number | null;
  upperBoundScore?: number | null;
  evaluationYear: string;
  effectiveStartDate: string;
  effectiveEndDate: string;
  activeYn: ActiveYn;
  changeReason: string;
};

export const calculationFormulaApi = {
  listCalculationFormulas(
    params: {
      page?: number;
      pageSize?: PageSize;
      ruleVersionId?: number;
      formulaCode?: string;
      calculationType?: CalculationType | "";
      evaluationYear?: string;
      roundingRule?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("pageSize", String(params.pageSize ?? 20));
    if (params.ruleVersionId)
      query.set("ruleVersionId", String(params.ruleVersionId));
    if (params.formulaCode?.trim())
      query.set("formulaCode", params.formulaCode.trim());
    if (params.calculationType)
      query.set("calculationType", params.calculationType);
    if (params.evaluationYear?.trim())
      query.set("evaluationYear", params.evaluationYear.trim());
    if (params.roundingRule?.trim())
      query.set("roundingRule", params.roundingRule.trim());
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<CalculationFormulaSearchResponse>(
      `/api/admin/calculation-formulas?${query.toString()}` as `/api/${string}`,
    );
  },
  saveCalculationFormula(payload: CalculationFormulaPayload) {
    return apiRequest<CalculationFormula>(
      "/api/admin/calculation-formulas/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type EvaluationRuleSetStatus = "DRAFT" | "CONFIRMED" | "DISCARDED";

export type EvaluationRuleSet = {
  ruleSetId: number;
  ruleVersionId: number;
  versionCode: string;
  versionStatus: EvaluationRuleVersionStatus;
  targetScope: string;
  ruleSetName: string;
  ruleSetStatus: EvaluationRuleSetStatus;
  activeYn: ActiveYn;
  effectiveStartDate: string;
  effectiveEndDate: string;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type EvaluationRuleSetSearchResponse = {
  evaluationRuleSets: EvaluationRuleSet[];
  page: number;
  pageSize: number;
  totalElements: number;
};

export type EvaluationRuleSetPayload = {
  ruleVersionId: number;
  targetScope: string;
  ruleSetName: string;
  ruleSetStatus: EvaluationRuleSetStatus;
  activeYn: ActiveYn;
  effectiveStartDate: string;
  effectiveEndDate: string;
  changeReason: string;
};

export const evaluationRuleSetApi = {
  listEvaluationRuleSets(
    params: {
      page?: number;
      pageSize?: PageSize;
      ruleVersionId?: number;
      targetScope?: string;
      ruleSetName?: string;
      ruleSetStatus?: EvaluationRuleSetStatus | "";
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("pageSize", String(params.pageSize ?? 20));
    if (params.ruleVersionId)
      query.set("ruleVersionId", String(params.ruleVersionId));
    if (params.targetScope?.trim())
      query.set("targetScope", params.targetScope.trim());
    if (params.ruleSetName?.trim())
      query.set("ruleSetName", params.ruleSetName.trim());
    if (params.ruleSetStatus) query.set("ruleSetStatus", params.ruleSetStatus);
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<EvaluationRuleSetSearchResponse>(
      `/api/admin/evaluation-rule-sets?${query.toString()}` as `/api/${string}`,
    );
  },
  saveEvaluationRuleSet(payload: EvaluationRuleSetPayload) {
    return apiRequest<EvaluationRuleSet>(
      "/api/admin/evaluation-rule-sets/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type JournalIndexingType =
  | "KCI"
  | "CANDIDATE"
  | "INTERNATIONAL"
  | "OTHER";

export type JournalIndexingInfo = {
  journalIndexingInfoId: number;
  ruleVersionId: number;
  versionCode: string;
  versionStatus: EvaluationRuleVersionStatus;
  issn: string;
  journalName: string;
  indexingType: JournalIndexingType;
  indexingTypeName?: string | null;
  publicationCountry: string;
  validStartDate: string;
  validEndDate: string;
  sourceName: string;
  sourceUpdatedAt: string;
  activeYn: ActiveYn;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type JournalIndexingInfoSearchResponse = {
  journalIndexingInfos: JournalIndexingInfo[];
  page: number;
  pageSize: number;
  totalElements: number;
};

export type JournalIndexingInfoPayload = {
  ruleVersionId: number;
  issn: string;
  journalName: string;
  indexingType: JournalIndexingType;
  publicationCountry: string;
  validStartDate: string;
  validEndDate: string;
  sourceName: string;
  sourceUpdatedAt: string;
  activeYn: ActiveYn;
  changeReason: string;
};

export const journalIndexingInfoApi = {
  listJournalIndexingInfos(
    params: {
      page?: number;
      pageSize?: PageSize;
      ruleVersionId?: number;
      issn?: string;
      journalName?: string;
      indexingType?: JournalIndexingType | "";
      publicationCountry?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("pageSize", String(params.pageSize ?? 20));
    if (params.ruleVersionId)
      query.set("ruleVersionId", String(params.ruleVersionId));
    if (params.issn?.trim()) query.set("issn", params.issn.trim());
    if (params.journalName?.trim())
      query.set("journalName", params.journalName.trim());
    if (params.indexingType) query.set("indexingType", params.indexingType);
    if (params.publicationCountry?.trim())
      query.set("publicationCountry", params.publicationCountry.trim());
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<JournalIndexingInfoSearchResponse>(
      `/api/admin/journal-indexing-infos?${query.toString()}` as `/api/${string}`,
    );
  },
  saveJournalIndexingInfo(payload: JournalIndexingInfoPayload) {
    return apiRequest<JournalIndexingInfo>(
      "/api/admin/journal-indexing-infos/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type AreaElementSystem = EvaluationElement & {
  systemSettingId: number;
  targetScope: string;
};

export type AreaElementSystemSearchResponse = {
  areaElementSystems: AreaElementSystem[];
  page: number;
  size: number;
  totalElements: number;
};

export type AreaElementSystemPayload = {
  ruleVersionId: number;
  areaCode: string;
  itemCode: string;
  evaluationYear: string;
  elementCode: string;
  targetScope: string;
  activeYn: ActiveYn;
  changeReason: string;
};

export const areaElementSystemApi = {
  listAreaElementSystems(
    params: {
      page?: number;
      size?: PageSize;
      ruleVersionId?: number;
      areaCode?: string;
      itemCode?: string;
      evaluationYear?: string;
      elementCode?: string;
      activeYn?: ActiveYn | "";
      keyword?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.ruleVersionId)
      query.set("ruleVersionId", String(params.ruleVersionId));
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.itemCode?.trim()) query.set("itemCode", params.itemCode.trim());
    if (params.evaluationYear?.trim())
      query.set("evaluationYear", params.evaluationYear.trim());
    if (params.elementCode?.trim())
      query.set("elementCode", params.elementCode.trim());
    if (params.activeYn) query.set("activeYn", params.activeYn);
    if (params.keyword?.trim()) query.set("keyword", params.keyword.trim());
    return apiRequest<AreaElementSystemSearchResponse>(
      `/api/admin/area-element-systems?${query.toString()}` as `/api/${string}`,
    );
  },
  saveAreaElementSystem(payload: AreaElementSystemPayload) {
    return apiRequest<AreaElementSystem>(
      "/api/admin/area-element-systems/save",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type BusinessStatusTransition = {
  transitionId: number;
  definitionVersion: DefinitionVersion;
  businessType: BusinessType;
  fromStatusCode: string;
  toStatusCode: string;
  executorRoleCode: ExecutorRoleCode;
  opinionRequiredYn: SystemUseYn;
  attachmentRequiredYn: SystemUseYn;
  cancellableYn: SystemUseYn;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type ExecutorRoleCode =
  | "R01"
  | "R02"
  | "R03"
  | "R04"
  | "R05"
  | "R06"
  | "R07"
  | "R08"
  | "R09";

export type BusinessStatusTransitionSearchResponse = {
  transitions: BusinessStatusTransition[];
  page: number;
  size: number;
  totalElements: number;
};

export type BusinessStatusTransitionPayload = {
  definitionVersion: DefinitionVersion;
  businessType: BusinessType;
  fromStatusCode: string;
  toStatusCode: string;
  executorRoleCode: ExecutorRoleCode;
  opinionRequiredYn: SystemUseYn;
  attachmentRequiredYn: SystemUseYn;
  cancellableYn: SystemUseYn;
  changeReason: string;
};

export const businessStatusTransitionApi = {
  listBusinessStatusTransitions(
    params: {
      page?: number;
      size?: PageSize;
      businessType?: BusinessType;
      fromStatusCode?: string;
      executorRoleCode?: ExecutorRoleCode;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.businessType) query.set("businessType", params.businessType);
    if (params.fromStatusCode?.trim())
      query.set("fromStatusCode", params.fromStatusCode.trim());
    if (params.executorRoleCode)
      query.set("executorRoleCode", params.executorRoleCode);
    return apiRequest<BusinessStatusTransitionSearchResponse>(
      `/api/admin/business-status-transitions?${query.toString()}` as `/api/${string}`,
    );
  },
  saveBusinessStatusTransition(payload: BusinessStatusTransitionPayload) {
    return apiRequest<BusinessStatusTransition>(
      "/api/admin/business-status-transitions",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type DepartmentChairConfirmationTarget = {
  confirmationId: number;
  achievementId: number;
  evaluationYear: string;
  departmentOrganizationCode: string;
  areaCode: string;
  confirmStatus: "DEPARTMENT_CONFIRMED" | "DEPARTMENT_REJECTED";
  previousStatus: string;
  nextStatus: string;
  opinion?: string | null;
  reasonCode?: string | null;
  processedBy: number;
  processedAt: string;
  changeReason?: string;
};

export type DepartmentChairConfirmationSearchResponse = {
  targets: DepartmentChairConfirmationTarget[];
  page: number;
  size: number;
  totalElements: number;
};

export type DepartmentChairConfirmationTransitionPayload = {
  actionType: "CONFIRM" | "REJECT";
  reasonCode?: string;
  opinion?: string;
};

export const departmentChairConfirmationApi = {
  listDepartmentChairConfirmTargets(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      areaCode?: string;
      certificationStatus?: string;
      attachmentYn?: SystemUseYn | "";
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim()) {
      query.set("evaluationYear", params.evaluationYear.trim());
    }
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.certificationStatus?.trim()) {
      query.set("certificationStatus", params.certificationStatus.trim());
    }
    if (params.attachmentYn) query.set("attachmentYn", params.attachmentYn);
    return apiRequest<DepartmentChairConfirmationSearchResponse>(
      `/api/business/department-chair-confirmations?${query.toString()}` as `/api/${string}`,
    );
  },
  saveDepartmentChairConfirmTargetsTransition(
    targetId: number,
    payload: DepartmentChairConfirmationTransitionPayload,
  ) {
    return apiRequest<DepartmentChairConfirmationTarget>(
      `/api/business/department-chair-confirmations/${encodeURIComponent(String(targetId))}/transition` as `/api/${string}`,
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type AchievementVerificationTarget = {
  verificationId: number;
  achievementId: number;
  evaluationYear: string;
  handlerUserId?: number | null;
  actionType: "CERTIFY" | "RETURN" | "CANCEL_CERTIFICATION";
  previousStatus: string;
  nextStatus: string;
  opinion?: string | null;
  evidenceRef?: string | null;
  reasonCode?: string | null;
  processedBy: number;
  processedAt: string;
  changeReason?: string;
};

export type AchievementVerificationSearchResponse = {
  targets: AchievementVerificationTarget[];
  page: number;
  size: number;
  totalElements: number;
};

export type AchievementVerificationTransitionPayload = {
  actionType: "CERTIFY" | "RETURN" | "CANCEL_CERTIFICATION";
  reasonCode?: string;
  opinion?: string;
  evidenceRef?: string;
};

export const achievementVerificationApi = {
  listAchievementVerificationTargets(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      areaCode?: string;
      verificationStatus?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim()) {
      query.set("evaluationYear", params.evaluationYear.trim());
    }
    if (params.areaCode?.trim()) query.set("areaCode", params.areaCode.trim());
    if (params.verificationStatus?.trim()) {
      query.set("verificationStatus", params.verificationStatus.trim());
    }
    return apiRequest<AchievementVerificationSearchResponse>(
      `/api/business/achievement-verifications?${query.toString()}` as `/api/${string}`,
    );
  },
  saveAchievementVerificationTargetsTransition(
    targetId: number,
    payload: AchievementVerificationTransitionPayload,
  ) {
    return apiRequest<AchievementVerificationTarget>(
      `/api/business/achievement-verifications/${encodeURIComponent(String(targetId))}/transition` as `/api/${string}`,
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type GrantPaymentApprovalTarget = {
  approvalId: number;
  grantApplicationId: number;
  linkedAchievementId?: number | null;
  evaluationYear: string;
  approvalStatus: "APPROVED" | "REJECTED" | "APPROVAL_CANCELLED";
  previousStatus: string;
  nextStatus: string;
  requestedAmountSnapshot: number;
  paymentAmountSnapshot: number;
  accountSnapshotRef: string;
  reasonCode?: string | null;
  opinion?: string | null;
  processedBy: number;
  processedAt: string;
  changeReason?: string;
};

export type GrantPaymentApprovalSearchResponse = {
  approvals: GrantPaymentApprovalTarget[];
  page: number;
  size: number;
  totalElements: number;
};

export type GrantPaymentApprovalTransitionPayload = {
  actionType: "APPROVE" | "REJECT" | "CANCEL_APPROVAL";
  reasonCode?: string;
  opinion?: string;
};

export const grantPaymentApprovalApi = {
  listGrantPaymentApprovals(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      approvalStatus?: string;
      applicantName?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim()) {
      query.set("evaluationYear", params.evaluationYear.trim());
    }
    if (params.approvalStatus?.trim()) {
      query.set("approvalStatus", params.approvalStatus.trim());
    }
    if (params.applicantName?.trim())
      query.set("applicantName", params.applicantName.trim());
    return apiRequest<GrantPaymentApprovalSearchResponse>(
      `/api/business/grant-payment-approvals?${query.toString()}` as `/api/${string}`,
    );
  },
  saveGrantPaymentApprovalsTransition(
    targetId: number,
    payload: GrantPaymentApprovalTransitionPayload,
  ) {
    return apiRequest<GrantPaymentApprovalTarget>(
      `/api/business/grant-payment-approvals/${encodeURIComponent(String(targetId))}/transition` as `/api/${string}`,
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type RejectionReason = {
  rejectionReasonId: number;
  businessType: BusinessType;
  reasonCode: string;
  standardMessage: string;
  additionalOpinionAllowedYn: SystemUseYn;
  changeReason?: string;
  updatedBy?: number;
  updatedAt?: string;
};

export type RejectionReasonSearchResponse = {
  reasons: RejectionReason[];
  page: number;
  size: number;
  totalElements: number;
};

export type RejectionReasonPayload = {
  businessType: BusinessType;
  reasonCode: string;
  standardMessage: string;
  additionalOpinionAllowedYn: SystemUseYn;
  changeReason: string;
};

export const rejectionReasonApi = {
  listRejectionReasons(
    params: {
      page?: number;
      size?: PageSize;
      businessType?: BusinessType;
      reasonCode?: string;
      additionalOpinionAllowedYn?: SystemUseYn | "";
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.businessType) query.set("businessType", params.businessType);
    if (params.reasonCode?.trim())
      query.set("reasonCode", params.reasonCode.trim());
    if (params.additionalOpinionAllowedYn) {
      query.set(
        "additionalOpinionAllowedYn",
        params.additionalOpinionAllowedYn,
      );
    }
    return apiRequest<RejectionReasonSearchResponse>(
      `/api/admin/rejection-reasons?${query.toString()}` as `/api/${string}`,
    );
  },
  saveRejectionReason(payload: RejectionReasonPayload) {
    return apiRequest<RejectionReason>("/api/admin/rejection-reasons", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
};

export type ChangeType = "CREATE" | "UPDATE" | "DELETE";

export type DataChangeHistory = {
  historyId: number;
  targetBusiness: string;
  targetKey: string;
  changeType: ChangeType;
  fieldName: string;
  beforeValue?: string | null;
  afterValue?: string | null;
  changedBy: number;
  changedByLoginId?: string;
  changedByName?: string;
  changedAt: string;
  changeReason: string;
};

export type DataChangeHistorySearchResponse = {
  histories: DataChangeHistory[];
  page: number;
  size: number;
  totalElements: number;
};

export const dataChangeHistoryApi = {
  listDataChangeHistories(
    params: {
      page?: number;
      size?: PageSize;
      targetBusiness?: string;
      targetKey?: string;
      changedBy?: string;
      changedAtFrom?: string;
      changedAtTo?: string;
      changeType?: ChangeType | "";
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.targetBusiness?.trim())
      query.set("targetBusiness", params.targetBusiness.trim());
    if (params.targetKey?.trim())
      query.set("targetKey", params.targetKey.trim());
    if (params.changedBy?.trim())
      query.set("changedBy", params.changedBy.trim());
    if (params.changedAtFrom?.trim())
      query.set("changedAtFrom", params.changedAtFrom.trim());
    if (params.changedAtTo?.trim())
      query.set("changedAtTo", params.changedAtTo.trim());
    if (params.changeType) query.set("changeType", params.changeType);
    return apiRequest<DataChangeHistorySearchResponse>(
      `/api/admin/data-change-histories?${query.toString()}` as `/api/${string}`,
    );
  },
};

export type AchievementDataHistory = {
  historyId: number;
  achievementType: string;
  achievementKey: string;
  changeType: ChangeType;
  fieldName: string;
  beforeValue?: string | null;
  afterValue?: string | null;
  changedBy: number;
  changedByLoginId?: string;
  changedByName?: string;
  changedAt: string;
  changeReason: string;
};

export type AchievementDataAsOf = {
  snapshotId: number;
  achievementType: string;
  achievementKey: string;
  employeeNo?: string | null;
  achievementTitle: string;
  achievementStatus: string;
  snapshotValue: string;
  baseAt: string;
  capturedAt: string;
};

export type AchievementDataHistorySearchResponse = {
  histories: AchievementDataHistory[];
  page: number;
  size: number;
  totalElements: number;
};

export type AchievementDataAsOfSearchResponse = {
  snapshots: AchievementDataAsOf[];
  page: number;
  size: number;
  totalElements: number;
};

export const achievementDataHistoryApi = {
  listHistories(
    params: {
      page?: number;
      size?: PageSize;
      achievementType?: string;
      achievementKey?: string;
      employeeNo?: string;
      changedAtFrom?: string;
      changedAtTo?: string;
      changeType?: ChangeType | "";
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.achievementType?.trim())
      query.set("achievementType", params.achievementType.trim());
    if (params.achievementKey?.trim())
      query.set("achievementKey", params.achievementKey.trim());
    if (params.employeeNo?.trim())
      query.set("employeeNo", params.employeeNo.trim());
    if (params.changedAtFrom?.trim())
      query.set("changedAtFrom", params.changedAtFrom.trim());
    if (params.changedAtTo?.trim())
      query.set("changedAtTo", params.changedAtTo.trim());
    if (params.changeType) query.set("changeType", params.changeType);
    return apiRequest<AchievementDataHistorySearchResponse>(
      `/api/admin/achievement-data-histories?${query.toString()}` as `/api/${string}`,
    );
  },
  listAsOf(
    params: {
      page?: number;
      size?: PageSize;
      achievementType?: string;
      achievementKey?: string;
      employeeNo?: string;
      asOfAt?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.achievementType?.trim())
      query.set("achievementType", params.achievementType.trim());
    if (params.achievementKey?.trim())
      query.set("achievementKey", params.achievementKey.trim());
    if (params.employeeNo?.trim())
      query.set("employeeNo", params.employeeNo.trim());
    if (params.asOfAt?.trim()) query.set("asOfAt", params.asOfAt.trim());
    return apiRequest<AchievementDataAsOfSearchResponse>(
      `/api/admin/achievement-data-as-of?${query.toString()}` as `/api/${string}`,
    );
  },
};

export type DeletedBusinessData = {
  deletedDataId: number;
  businessType: BusinessType;
  originalKey: string;
  deletedBy: number;
  deletedByLoginId?: string;
  deletedByName?: string;
  deletedAt: string;
  deleteReason: string;
  recoverableYn: SystemUseYn;
};

export type DeletedBusinessDataSearchResponse = {
  deletedData: DeletedBusinessData[];
  page: number;
  size: number;
  totalElements: number;
};

export const deletedBusinessDataApi = {
  listDeletedBusinessData(
    params: {
      page?: number;
      size?: PageSize;
      businessType?: BusinessType | "";
      originalKey?: string;
      deletedBy?: string;
      deletedAtFrom?: string;
      deletedAtTo?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.businessType) query.set("businessType", params.businessType);
    if (params.originalKey?.trim())
      query.set("originalKey", params.originalKey.trim());
    if (params.deletedBy?.trim())
      query.set("deletedBy", params.deletedBy.trim());
    if (params.deletedAtFrom?.trim())
      query.set("deletedAtFrom", params.deletedAtFrom.trim());
    if (params.deletedAtTo?.trim())
      query.set("deletedAtTo", params.deletedAtTo.trim());
    return apiRequest<DeletedBusinessDataSearchResponse>(
      `/api/admin/deleted-business-data?${query.toString()}` as `/api/${string}`,
    );
  },
};

export type PositionAssignment = {
  positionAssignmentId: number;
  positionCode: string;
  userId: number;
  userName?: string;
  organizationCode: string;
  organizationName?: string;
  effectiveStartDate: string;
  effectiveEndDate?: string | null;
  status: string;
  confirmedAt?: string;
  changeReason?: string;
  updatedAt?: string;
};

export type PositionAssignmentPayload = {
  positionCode: string;
  userId: string;
  organizationCode: string;
  effectiveStartDate: string;
  effectiveEndDate?: string | null;
  changeReason: string;
};

export type PositionAssignmentSearchResponse = {
  assignments: PositionAssignment[];
  page: number;
  size: number;
  totalElements: number;
};

export type DutyAssignment = {
  dutyAssignmentId: number;
  dutyOrganization: string;
  userId: number;
  userName?: string;
  dutyArea: string;
  validStartDate: string;
  validEndDate?: string | null;
  dataScopeType: DataScopeType;
  processingPermission: string;
  status: string;
  confirmedAt?: string;
  changeReason?: string;
  updatedAt?: string;
};

export type DataScopeType = "SELF" | "DEPARTMENT" | "COLLEGE" | "DUTY" | "ALL";

export type DutyAssignmentPayload = {
  dutyOrganization: string;
  userId: string;
  dutyArea: string;
  validStartDate: string;
  validEndDate?: string | null;
  dataScopeType: DataScopeType;
  processingPermission: string;
  changeReason: string;
};

export type DutyAssignmentSearchResponse = {
  assignments: DutyAssignment[];
  page: number;
  size: number;
  totalElements: number;
};

export type DataScopeRule = {
  dataScopeRuleId: number;
  roleCode: string;
  roleName?: string;
  dataScopeType: DataScopeType;
  organizationCode?: string | null;
  organizationName?: string | null;
  dutyArea?: string | null;
  changeReason?: string | null;
  updatedAt?: string;
};

export type DataScopeRulePayload = {
  roleCode: string;
  dataScopeType: DataScopeType;
  organizationCode?: string | null;
  dutyArea?: string | null;
  changeReason?: string;
};

export type DataScopeRulesSearchResponse = {
  rules: DataScopeRule[];
  page: number;
  size: number;
  totalElements: number;
};

function assignmentQuery(
  params: {
    page?: number;
    size?: PageSize;
    referenceDate?: string;
    filter?: string;
  } = {},
) {
  const query = new URLSearchParams();
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 20));
  if (params.referenceDate) query.set("referenceDate", params.referenceDate);
  if (params.filter?.trim()) query.set("filter", params.filter.trim());
  return query.toString();
}

export const operationsApi = {
  searchPositionAssignments(
    params: {
      page?: number;
      size?: PageSize;
      referenceDate?: string;
      filter?: string;
    } = {},
  ) {
    return apiRequest<PositionAssignmentSearchResponse>(
      `/api/admin/position-assignments?${assignmentQuery(params)}` as `/api/${string}`,
    );
  },
  savePositionAssignment(payload: PositionAssignmentPayload) {
    return apiRequest<PositionAssignment>("/api/admin/position-assignments", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  updatePositionAssignment(
    positionAssignmentId: number,
    payload: PositionAssignmentPayload,
  ) {
    return apiRequest<PositionAssignment>(
      `/api/admin/position-assignments/${encodeURIComponent(String(positionAssignmentId))}` as `/api/${string}`,
      { method: "PUT", body: JSON.stringify(payload) },
    );
  },
  searchDutyAssignments(
    params: {
      page?: number;
      size?: PageSize;
      referenceDate?: string;
      filter?: string;
    } = {},
  ) {
    return apiRequest<DutyAssignmentSearchResponse>(
      `/api/admin/duty-assignments?${assignmentQuery(params)}` as `/api/${string}`,
    );
  },
  saveDutyAssignment(payload: DutyAssignmentPayload) {
    return apiRequest<DutyAssignment>("/api/admin/duty-assignments", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  updateDutyAssignment(
    dutyAssignmentId: number,
    payload: DutyAssignmentPayload,
  ) {
    return apiRequest<DutyAssignment>(
      `/api/admin/duty-assignments/${encodeURIComponent(String(dutyAssignmentId))}` as `/api/${string}`,
      { method: "PUT", body: JSON.stringify(payload) },
    );
  },
  searchDataScopeRules(
    params: {
      page?: number;
      size?: PageSize;
      referenceDate?: string;
      filter?: string;
    } = {},
  ) {
    return apiRequest<DataScopeRulesSearchResponse>(
      `/api/admin/data-scope-rules?${assignmentQuery(params)}` as `/api/${string}`,
    );
  },
  saveDataScopeRules(payload: DataScopeRulePayload) {
    return apiRequest<DataScopeRule>("/api/admin/data-scope-rules", {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },
};

export type FullTimeFacultyStatus = {
  employeeNo: string;
  name: string;
  collegeCode?: string | null;
  collegeName?: string | null;
  departmentCode?: string | null;
  departmentName?: string | null;
  rankName?: string | null;
  retirementDate?: string | null;
};

export type FullTimeFacultyStatusSearchResponse = {
  statuses: FullTimeFacultyStatus[];
  page: number;
  pageSize: number;
  totalElements: number;
  baseYear: number;
};

export const fullTimeFacultyStatusApi = {
  listStatuses(params: {
    page?: number;
    size?: PageSize;
    baseYear: number | string;
    organizationCode?: string;
    employeeNo?: string;
    name?: string;
  }) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    query.set("baseYear", String(params.baseYear));
    if (params.organizationCode?.trim())
      query.set("organizationCode", params.organizationCode.trim());
    if (params.employeeNo?.trim())
      query.set("employeeNo", params.employeeNo.trim());
    if (params.name?.trim()) query.set("name", params.name.trim());
    return apiRequest<FullTimeFacultyStatusSearchResponse>(
      `/api/admin/full-time-faculty-statuses?${query.toString()}` as `/api/${string}`,
    );
  },
};

export type KorusFacultySyncStatus = "SUCCESS" | "FAILED";

export type KorusFacultySyncResult = {
  resultId: number;
  runId: number;
  requestId: string;
  employeeNo: string;
  name: string;
  organizationCode: string;
  rankName?: string | null;
  appointmentId: string;
  syncStatus: KorusFacultySyncStatus;
  errorMessage?: string | null;
  retryOfResultId?: number | null;
  createdAt: string;
};

export type KorusFacultySyncRun = {
  runId: number;
  requestId: string;
  runType: "MANUAL" | "RETRY" | "SCHEDULED";
  targetStartDate: string;
  targetEndDate: string;
  runStatus: "RUNNING" | "SUCCESS" | "FAILED" | "PARTIAL";
  totalCount: number;
  successCount: number;
  failureCount: number;
  createdBy?: number;
  startedAt?: string;
  finishedAt?: string;
  failureReason?: string | null;
};

export type KorusFacultySyncSearchResponse = {
  results: KorusFacultySyncResult[];
  page: number;
  pageSize: number;
  totalElements: number;
};

export const korusFacultySyncApi = {
  listResults(
    params: {
      page?: number;
      size?: PageSize;
      targetStartDate?: string;
      targetEndDate?: string;
      syncStatus?: KorusFacultySyncStatus | "";
      requestId?: string;
      employeeNo?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.targetStartDate)
      query.set("targetStartDate", params.targetStartDate);
    if (params.targetEndDate) query.set("targetEndDate", params.targetEndDate);
    if (params.syncStatus) query.set("syncStatus", params.syncStatus);
    if (params.requestId?.trim())
      query.set("requestId", params.requestId.trim());
    if (params.employeeNo?.trim())
      query.set("employeeNo", params.employeeNo.trim());
    return apiRequest<KorusFacultySyncSearchResponse>(
      `/api/admin/korus-faculty-sync-results?${query.toString()}` as `/api/${string}`,
    );
  },
  createRun(payload: { targetStartDate: string; targetEndDate: string }) {
    return apiRequest<KorusFacultySyncRun>(
      "/api/admin/korus-faculty-sync-runs",
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
  retryResult(resultId: number) {
    return apiRequest<KorusFacultySyncRun>(
      `/api/admin/korus-faculty-sync-results/${encodeURIComponent(String(resultId))}/retry` as `/api/${string}`,
      { method: "POST" },
    );
  },
};

export type ObjectionOpinionTarget = {
  objectionOpinionId: number;
  objectionId: number;
  evaluationYear: string;
  applicantUserId: number;
  applicantOpinionSnapshot: string;
  objectionContentSnapshot: string;
  reviewerOpinion: string;
  decisionResult: "ACCEPTED" | "REJECTED" | "NEEDS_REVIEW";
  reasonCode?: string | null;
  processedBy: number;
  processedAt: string;
  changeReason?: string;
};

export type ObjectionOpinionSearchResponse = {
  opinions: ObjectionOpinionTarget[];
  page: number;
  size: number;
  totalElements: number;
};

export type ObjectionOpinionTransitionPayload = {
  decisionResult: "ACCEPTED" | "REJECTED" | "NEEDS_REVIEW";
  reviewerOpinion: string;
  reasonCode?: string;
};

export const objectionOpinionApi = {
  listObjectionOpinions(
    params: {
      page?: number;
      size?: PageSize;
      evaluationYear?: string;
      decisionResult?: string;
      applicantName?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.evaluationYear?.trim()) {
      query.set("evaluationYear", params.evaluationYear.trim());
    }
    if (params.decisionResult?.trim()) {
      query.set("decisionResult", params.decisionResult.trim());
    }
    if (params.applicantName?.trim())
      query.set("applicantName", params.applicantName.trim());
    return apiRequest<ObjectionOpinionSearchResponse>(
      `/api/business/objection-opinions?${query.toString()}` as `/api/${string}`,
    );
  },
  saveObjectionOpinionsTransition(
    targetId: number,
    payload: ObjectionOpinionTransitionPayload,
  ) {
    return apiRequest<ObjectionOpinionTarget>(
      `/api/business/objection-opinions/${encodeURIComponent(String(targetId))}/transition` as `/api/${string}`,
      {
        method: "POST",
        body: JSON.stringify(payload),
      },
    );
  },
};

export type ResearcherProfileSummary = {
  employeeNo: string;
  name: string;
  organizationCode: string;
  organizationName?: string | null;
  rankName?: string | null;
  appointmentId?: string | null;
  contact?: string | null;
  researcherRegistrationNo?: string | null;
  externalProvisionYn?: YesNo;
  informationPublicYn?: YesNo;
  finalDegreeType?: "BACHELOR" | "MASTER" | "DOCTOR" | null;
  degreePrerequisiteMissing: boolean;
  updatedAt?: string | null;
};

export type ResearcherResearchField = {
  researchFieldId?: number;
  employeeNo?: string;
  majorName: string;
  detailMajorName?: string | null;
  majorSeries?: string | null;
  changedBy?: number;
  changedAt?: string | null;
};

export type ResearcherCareer = {
  careerId?: number;
  employeeNo?: string;
  workStartYm: string;
  workEndYm?: string | null;
  workplace: string;
  positionName?: string | null;
  duty?: string | null;
  changedBy?: number;
  changedAt?: string | null;
};

export type ResearcherDegree = {
  degreeId?: number;
  employeeNo?: string;
  degreeType: "BACHELOR" | "MASTER" | "DOCTOR" | "";
  universityName: string;
  startYm?: string | null;
  acquiredYm?: string | null;
  countryName?: string | null;
  collegeName?: string | null;
  advisorName?: string | null;
  changedBy?: number;
  changedAt?: string | null;
};

export type ResearcherCertification = {
  certificationId?: number;
  employeeNo?: string;
  acquiredYm?: string | null;
  certificationName: string;
  issuingOrganizationName?: string | null;
  changedBy?: number;
  changedAt?: string | null;
};

export type ResearcherProfileDetail = ResearcherProfileSummary & {
  researchFields: ResearcherResearchField[];
  careers: ResearcherCareer[];
  degrees: ResearcherDegree[];
  certifications: ResearcherCertification[];
};

export type ResearcherProfileSearchResponse = {
  profiles: ResearcherProfileSummary[];
  page: number;
  pageSize: number;
  totalElements: number;
};

export type ResearcherProfileSaveResponse = {
  profile: ResearcherProfileDetail;
  warnings: string[];
};

export const researcherProfileApi = {
  listProfiles(
    params: {
      page?: number;
      size?: PageSize;
      employeeNo?: string;
      name?: string;
      organizationCode?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.employeeNo?.trim())
      query.set("employeeNo", params.employeeNo.trim());
    if (params.name?.trim()) query.set("name", params.name.trim());
    if (params.organizationCode?.trim())
      query.set("organizationCode", params.organizationCode.trim());
    return apiRequest<ResearcherProfileSearchResponse>(
      `/api/researcher-profiles?${query.toString()}` as `/api/${string}`,
    );
  },
  getProfile(employeeNo: string) {
    return apiRequest<ResearcherProfileDetail>(
      `/api/researcher-profiles/${encodeURIComponent(employeeNo)}` as `/api/${string}`,
    );
  },
  saveResearchFields(
    employeeNo: string,
    items: ResearcherResearchField[],
    changeReason: string,
  ) {
    return apiRequest<ResearcherProfileSaveResponse>(
      `/api/researcher-profiles/${encodeURIComponent(employeeNo)}/research-fields` as `/api/${string}`,
      {
        method: "PUT",
        body: JSON.stringify({ items, changeReason }),
      },
    );
  },
  saveCareers(
    employeeNo: string,
    items: ResearcherCareer[],
    changeReason: string,
  ) {
    return apiRequest<ResearcherProfileSaveResponse>(
      `/api/researcher-profiles/${encodeURIComponent(employeeNo)}/careers` as `/api/${string}`,
      {
        method: "PUT",
        body: JSON.stringify({ items, changeReason }),
      },
    );
  },
  saveDegrees(
    employeeNo: string,
    items: ResearcherDegree[],
    changeReason: string,
  ) {
    return apiRequest<ResearcherProfileSaveResponse>(
      `/api/researcher-profiles/${encodeURIComponent(employeeNo)}/degrees` as `/api/${string}`,
      {
        method: "PUT",
        body: JSON.stringify({ items, changeReason }),
      },
    );
  },
  saveCertifications(
    employeeNo: string,
    items: ResearcherCertification[],
    changeReason: string,
  ) {
    return apiRequest<ResearcherProfileSaveResponse>(
      `/api/researcher-profiles/${encodeURIComponent(employeeNo)}/certifications` as `/api/${string}`,
      {
        method: "PUT",
        body: JSON.stringify({ items, changeReason }),
      },
    );
  },
  listDegreePrerequisiteMissing(
    params: {
      page?: number;
      size?: PageSize;
      employeeNo?: string;
      name?: string;
      organizationCode?: string;
    } = {},
  ) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.employeeNo?.trim())
      query.set("employeeNo", params.employeeNo.trim());
    if (params.name?.trim()) query.set("name", params.name.trim());
    if (params.organizationCode?.trim())
      query.set("organizationCode", params.organizationCode.trim());
    return apiRequest<ResearcherProfileSearchResponse>(
      `/api/admin/researcher-profiles/degree-prerequisite-missing?${query.toString()}` as `/api/${string}`,
    );
  },
};

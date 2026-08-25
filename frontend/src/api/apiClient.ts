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

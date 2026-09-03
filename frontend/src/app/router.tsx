import { useEffect, useState } from "react";
import { useAuth } from "./AuthProvider";
import { AdminShell } from "../components/layout/AdminShell";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../components/States";
import { authApi, type CurrentUser } from "../api/apiClient";
import {
  ADMIN_ROUTES,
  LoginPage,
  canAccessAdminRoute,
} from "../pages/LoginPage";
import { OrganizationManagementPage } from "../pages/admin/SCR-ORG-MGMT";
import { RoleManagementPage } from "../pages/admin/SCR-ROLE-MGMT";
import { MenuPermissionManagementPage } from "../pages/admin/SCR-MENU-PERMISSION-MGMT";
import { FunctionPermissionManagementPage } from "../pages/admin/SCR-FUNCTION-PERMISSION-MGMT";
import { PrivacyPolicyManagementPage } from "../pages/admin/SCR-PRIVACY-POLICY-MGMT";
import { PrivacyAccessLogPage } from "../pages/admin/SCR-PRIVACY-ACCESS-LOG";
import { PrivacyPermissionManagementPage } from "../pages/admin/SCR-PRIVACY-ACCESS-PERMISSION-MGMT";
import { PeriodPermissionManagementPage } from "../pages/admin/SCR-PERIOD-PERMISSION-MGMT";
import { TemporaryPermissionManagementPage } from "../pages/admin/SCR-TEMPORARY-PERMISSION-MGMT";
import { PermissionHistoryPage } from "../pages/admin/SCR-PERMISSION-HISTORY";
import { MenuInfoManagementPage } from "../pages/admin/SCR-MENU-INFO-MGMT";
import { MenuStructureManagementPage } from "../pages/admin/SCR-MENU-STRUCTURE-MGMT";
import { UserRoleManagementPage } from "../pages/admin/SCR-USER-ROLE-MGMT";
import { UserManagementPage } from "../pages/admin/SCR-USER-MGMT";
import { CodeGroupManagementPage } from "../pages/admin/SCR-CODE-GROUP-MGMT";
import { DetailCodeManagementPage } from "../pages/admin/SCR-DETAIL-CODE-MGMT";
import { MessageManagementPage } from "../pages/admin/SCR-MESSAGE-MGMT";
import { NoticeManagementPage } from "../pages/admin/SCR-NOTICE-MGMT";
import { ManualManagementPage } from "../pages/admin/SCR-MANUAL-MGMT";
import { HelpContentManagementPage } from "../pages/admin/SCR-HELP-MGMT";
import { BatchDefinitionManagementPage } from "../pages/admin/SCR-BATCH-DEFINITION-MGMT";
import { BatchExecutionManagementPage } from "../pages/admin/SCR-BATCH-EXECUTION-MGMT";
import { BatchResultManagementPage } from "../pages/admin/SCR-BATCH-RESULT-MGMT";
import { BatchRetryManagementPage } from "../pages/admin/SCR-BATCH-RETRY-MGMT";
import { ActiveSessionStatusPage } from "../pages/admin/SCR-ACTIVE-SESSION-STATUS";
import { SessionTerminationHistoryPage } from "../pages/admin/SCR-SESSION-TERMINATION-HISTORY";
import { BusinessProcessLogPage } from "../pages/admin/SCR-BUSINESS-PROCESS-LOG";
import { SensitiveInformationAccessLogPage } from "../pages/admin/SCR-SENSITIVE-INFO-ACCESS-LOG";
import { PermissionChangeLogPage } from "../pages/admin/SCR-PERMISSION-CHANGE-LOG";
import { EvaluationOrganizationMappingPage } from "../pages/admin/SCR-EVALUATION-ORG-MAPPING";
import { BusinessStatusCodePage } from "../pages/admin/SCR-BUSINESS-STATUS-CODE";
import { EvaluationAreaManagementPage } from "../pages/admin/SCR-EVALUATION-AREA-MGMT";
import { EvaluationItemManagementPage } from "../pages/admin/SCR-EVALUATION-ITEM-MGMT";
import { EvaluationElementManagementPage } from "../pages/admin/SCR-EVALUATION-ELEMENT-MGMT";
import { EvaluationManagementItemManagementPage } from "../pages/admin/SCR-EVALUATION-MANAGEMENT-ITEM-MGMT";
import { EvaluationScoreManagementPage } from "../pages/admin/SCR-EVAL-SCORE-MGMT";
import { ParticipationRateManagementPage } from "../pages/admin/SCR-PARTICIPATION-RATE-MGMT";
import { CalculationFormulaManagementPage } from "../pages/admin/SCR-CALC-FORMULA-MGMT";
import { EvaluationRuleSetManagementPage } from "../pages/admin/SCR-EVAL-RULE-SET-MGMT";
import { JournalIndexingInfoManagementPage } from "../pages/admin/SCR-JOURNAL-INDEXING-MGMT";
import { AreaElementSystemManagementPage } from "../pages/admin/SCR-AREA-ELEMENT-SYSTEM-MGMT";
import { EvaluationDateManagementPage } from "../pages/admin/SCR-EVALUATION-DATE-MGMT";
import { InputPeriodManagementPage } from "../pages/admin/SCR-INPUT-PERIOD-MGMT";
import { ModificationPeriodManagementPage } from "../pages/admin/SCR-MODIFICATION-PERIOD-MGMT";
import { AppealPeriodManagementPage } from "../pages/admin/SCR-APPEAL-PERIOD-MGMT";
import { ResultViewPeriodManagementPage } from "../pages/admin/SCR-RESULT-VIEW-PERIOD-MGMT";
import { ExceptionPeriodManagementPage } from "../pages/admin/SCR-EXCEPTION-PERIOD-MGMT";
import { DepartmentChairConfirmPeriodManagementPage } from "../pages/admin/SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT";
import { DepartmentChairConfirmationManagementPage } from "../pages/admin/SCR-DEPARTMENT-CHAIR-CONFIRM-MGMT";
import { AchievementVerificationManagementPage } from "../pages/admin/SCR-ACHIEVEMENT-VERIFICATION-MGMT";
import { GrantPaymentApprovalManagementPage } from "../pages/admin/SCR-GRANT-PAYMENT-APPROVAL-MGMT";
import { ObjectionOpinionManagementPage } from "../pages/admin/SCR-OBJECTION-OPINION-MGMT";
import { EvaluationMaterialGenerationPage } from "../pages/admin/SCR-EVALUATION-MATERIAL-GENERATION";
import { EvaluationMaterialDeletionPage } from "../pages/admin/SCR-EVALUATION-MATERIAL-DELETION";
import { ScoreRecalculationPage } from "../pages/admin/SCR-SCORE-RECALCULATION";
import { FinalEvaluationConfirmationPage } from "../pages/admin/SCR-FINAL-EVALUATION-CONFIRMATION";
import { EvaluationBatchResultPage } from "../pages/admin/SCR-EVALUATION-BATCH-RESULT";
import { BusinessPeriodManagementPage } from "../pages/admin/SCR-BUSINESS-PERIOD-INTEGRATED-MGMT";
import { BusinessStatusTransitionPage } from "../pages/admin/SCR-BUSINESS-STATUS-TRANSITION";
import { RejectionReasonPage } from "../pages/admin/SCR-REJECTION-REASON";
import { DataChangeHistoryPage } from "../pages/admin/SCR-DATA-CHANGE-HISTORY";
import { DeletedBusinessDataPage } from "../pages/admin/SCR-DELETED-BUSINESS-DATA";
import { KorusFacultySyncPage } from "../pages/admin/SCR-KORUS-FACULTY-SYNC";
import { FullTimeFacultyStatusPage } from "../pages/admin/SCR-FULL-TIME-FACULTY-STATUS";
import {
  DegreePrerequisiteMissingPage,
  ResearcherProfileDetailPage,
  ResearcherProfileListPage,
} from "../pages/admin/SCR-RESEARCHER-PROFILES";
import {
  AchievementDataAsOfPage,
  AchievementDataHistoryPage,
} from "../pages/admin/SCR-ACHIEVEMENT-DATA-HISTORY";
import {
  ExcelDownloadManagementPage,
  ExcelUploadErrorManagementPage,
  ExcelUploadHistoryManagementPage,
  ExcelUploadManagementPage,
  UploadTemplateManagementPage,
} from "../pages/admin/ExcelOperationsPages";
import {
  BaseYearManagementPage,
  CommonSettingsPage,
  DetailCodeUsageManagementPage,
  MenuUsageManagementPage,
} from "../pages/admin/CommonOperationsPages";
import {
  DataScopeRuleManagementPage,
  DutyAssignmentManagementPage,
  PositionAssignmentManagementPage,
} from "../pages/admin/OperationsAssignmentScreens";

export function AppRouter() {
  const auth = useAuth();
  const path = usePathname();
  const adminRoute = ADMIN_ROUTES.find((route) =>
    routeMatchesPath(route.path, path),
  );

  if (auth.status === "loading") {
    return (
      <AdminShell>
        <LoadingState
          title="인증 확인 중"
          message="세션 정보를 확인하고 있습니다."
        />
      </AdminShell>
    );
  }

  if (auth.status === "anonymous") {
    return (
      <LoginPage
        onLogin={auth.login}
        onHealth={async () => (await authApi.health()).data ?? { status: "UP" }}
        onLoginSuccess={() => {
          if (
            typeof window !== "undefined" &&
            window.location.pathname === "/login"
          ) {
            window.history.replaceState({}, "", "/admin/users");
          }
        }}
      />
    );
  }

  if (auth.status === "error") {
    return (
      <AdminShell>
        <ErrorState
          title="인증 오류"
          message={auth.error ?? "인증 처리 중 오류가 발생했습니다."}
        />
      </AdminShell>
    );
  }

  if (adminRoute && !canAccessAdminRoute(auth.user, adminRoute.path)) {
    return (
      <AdminShell>
        <PermissionState
          title="권한이 없습니다"
          message={`${adminRoute.label} 화면 접근 권한이 없습니다.`}
        />
      </AdminShell>
    );
  }

  const routedPage = renderAdminPage(adminRoute?.path, auth.user);
  if (routedPage) {
    return <AdminShell>{routedPage}</AdminShell>;
  }

  if (adminRoute) {
    return (
      <AdminShell>
        <AdminRoutePage route={adminRoute} user={auth.user} />
      </AdminShell>
    );
  }

  return (
    <AdminShell>
      <section className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <h1 className="text-xl font-semibold text-dark">Dashboard</h1>
        <p className="mt-2 text-sm text-muted">
          한국교원대학교 교수업적평가시스템 공통기능 기반
        </p>
      </section>
      <section className="grid grid-cols-12 gap-6">
        <DashboardCard
          title="권한 역할"
          value={auth.user?.roles.join(", ") ?? "-"}
        />
        <DashboardCard
          title="시스템 관리 메뉴"
          value={`${countMenus(auth.user?.menus ?? [])}개`}
        />
        <DashboardCard title="세션 상태" value="인증됨" />
        <section className="col-span-12 rounded-md bg-white p-6 shadow-md">
          <h2 className="text-lg font-semibold text-dark">
            공통 상태 컴포넌트
          </h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <EmptyState />
            <PermissionState />
            <SuccessState />
          </div>
        </section>
      </section>
    </AdminShell>
  );
}

function renderAdminPage(path: string | undefined, user: CurrentUser | null) {
  switch (path) {
    case "/admin/users":
      return <UserManagementPage />;
    case "/admin/organizations":
      return <OrganizationManagementPage />;
    case "/admin/roles":
      return <RoleManagementPage />;
    case "/admin/user-roles":
      return <UserRoleManagementPage />;
    case "/admin/menu-permissions":
      return <MenuPermissionManagementPage />;
    case "/admin/function-permissions":
      return <FunctionPermissionManagementPage />;
    case "/admin/privacy/policies":
      return <PrivacyPolicyManagementPage />;
    case "/admin/privacy/permissions":
      return <PrivacyPermissionManagementPage />;
    case "/admin/privacy/access-logs":
      return <PrivacyAccessLogPage />;
    case "/admin/period-permissions":
      return <PeriodPermissionManagementPage />;
    case "/admin/temporary-permissions":
      return <TemporaryPermissionManagementPage />;
    case "/admin/permission-history":
      return <PermissionHistoryPage />;
    case "/admin/menu-info":
      return <MenuInfoManagementPage />;
    case "/admin/menu-structure":
      return <MenuStructureManagementPage />;
    case "/admin/code-groups":
      return <CodeGroupManagementPage />;
    case "/admin/detail-codes":
      return <DetailCodeManagementPage />;
    case "/admin/messages":
      return <MessageManagementPage />;
    case "/admin/notices":
      return <NoticeManagementPage />;
    case "/admin/manuals":
      return <ManualManagementPage />;
    case "/admin/help-contents":
      return <HelpContentManagementPage />;
    case "/admin/batch-definitions":
      return <BatchDefinitionManagementPage />;
    case "/admin/batch-executions":
      return <BatchExecutionManagementPage />;
    case "/admin/batch-results":
      return <BatchResultManagementPage />;
    case "/admin/batch-retries":
      return <BatchRetryManagementPage />;
    case "/admin/security/active-sessions":
      return <ActiveSessionStatusPage />;
    case "/admin/security/session-termination-histories":
      return <SessionTerminationHistoryPage />;
    case "/admin/audit/business-process-logs":
      return <BusinessProcessLogPage />;
    case "/admin/audit/sensitive-information-access-logs":
      return <SensitiveInformationAccessLogPage />;
    case "/admin/audit/permission-change-logs":
      return <PermissionChangeLogPage />;
    case "/admin/evaluation-organization-mappings":
      return <EvaluationOrganizationMappingPage />;
    case "/admin/business-status-codes":
      return <BusinessStatusCodePage />;
    case "/admin/evaluation-areas":
      return <EvaluationAreaManagementPage />;
    case "/admin/evaluation-items":
      return <EvaluationItemManagementPage />;
    case "/admin/evaluation-elements":
      return <EvaluationElementManagementPage />;
    case "/admin/evaluation-management-items":
      return <EvaluationManagementItemManagementPage />;
    case "/admin/evaluation-scores":
      return <EvaluationScoreManagementPage />;
    case "/admin/participation-rates":
      return <ParticipationRateManagementPage />;
    case "/admin/calculation-formulas":
      return <CalculationFormulaManagementPage />;
    case "/admin/evaluation-rule-sets":
      return <EvaluationRuleSetManagementPage />;
    case "/admin/journal-indexing-infos":
      return <JournalIndexingInfoManagementPage />;
    case "/admin/area-element-systems":
      return <AreaElementSystemManagementPage />;
    case "/admin/evaluation-dates":
      return <EvaluationDateManagementPage />;
    case "/admin/input-periods":
      return <InputPeriodManagementPage />;
    case "/admin/modification-periods":
      return <ModificationPeriodManagementPage />;
    case "/admin/appeal-periods":
      return <AppealPeriodManagementPage />;
    case "/admin/result-view-periods":
      return <ResultViewPeriodManagementPage />;
    case "/admin/exception-periods":
      return <ExceptionPeriodManagementPage />;
    case "/admin/department-chair-confirm-periods":
      return <DepartmentChairConfirmPeriodManagementPage />;
    case "/admin/business-periods":
      return <BusinessPeriodManagementPage />;
    case "/admin/department-chair-confirmations":
      return <DepartmentChairConfirmationManagementPage />;
    case "/admin/achievement-verifications":
      return <AchievementVerificationManagementPage />;
    case "/admin/grant-payment-approvals":
      return <GrantPaymentApprovalManagementPage />;
    case "/admin/objection-opinions":
      return <ObjectionOpinionManagementPage />;
    case "/admin/evaluation-material-generations":
      return <EvaluationMaterialGenerationPage />;
    case "/admin/evaluation-material-deletions":
      return <EvaluationMaterialDeletionPage />;
    case "/admin/score-recalculations":
      return <ScoreRecalculationPage />;
    case "/admin/final-evaluation-confirmations":
      return (
        <FinalEvaluationConfirmationPage currentRoles={user?.roles ?? []} />
      );
    case "/admin/evaluation-batch-results":
      return <EvaluationBatchResultPage />;
    case "/admin/business-status-transitions":
      return <BusinessStatusTransitionPage />;
    case "/admin/rejection-reasons":
      return <RejectionReasonPage />;
    case "/admin/data-change-histories":
      return <DataChangeHistoryPage />;
    case "/admin/deleted-business-data":
      return <DeletedBusinessDataPage />;
    case "/admin/korus-faculty-sync":
      return <KorusFacultySyncPage />;
    case "/admin/full-time-faculty-statuses":
      return <FullTimeFacultyStatusPage />;
    case "/researcher-profiles":
      return <ResearcherProfileListPage />;
    case "/researcher-profiles/{employeeNo}":
      return <ResearcherProfileDetailPage />;
    case "/admin/researcher-profiles/degree-prerequisite-missing":
      return <DegreePrerequisiteMissingPage />;
    case "/admin/achievement-data-histories":
      return <AchievementDataHistoryPage />;
    case "/admin/achievement-data-as-of":
      return <AchievementDataAsOfPage />;
    case "/admin/excel-upload-templates":
      return <UploadTemplateManagementPage />;
    case "/admin/excel-uploads":
      return <ExcelUploadManagementPage />;
    case "/admin/excel-upload-histories":
      return <ExcelUploadHistoryManagementPage />;
    case "/admin/excel-upload-errors":
      return <ExcelUploadErrorManagementPage />;
    case "/admin/excel-downloads":
      return <ExcelDownloadManagementPage />;
    case "/admin/menu-usage":
      return <MenuUsageManagementPage />;
    case "/admin/detail-code-usage":
      return <DetailCodeUsageManagementPage />;
    case "/admin/common-settings":
      return <CommonSettingsPage />;
    case "/admin/base-years":
      return <BaseYearManagementPage />;
    case "/admin/position-assignments":
      return <PositionAssignmentManagementPage />;
    case "/admin/duty-assignments":
      return <DutyAssignmentManagementPage />;
    case "/admin/data-scope-rules":
      return <DataScopeRuleManagementPage />;
    default:
      return null;
  }
}

function usePathname() {
  const [path, setPath] = useState(() =>
    typeof window === "undefined" ? "/login" : window.location.pathname,
  );

  useEffect(() => {
    const syncPath = () => setPath(window.location.pathname);
    window.addEventListener("popstate", syncPath);
    return () => window.removeEventListener("popstate", syncPath);
  }, []);

  return path;
}

function AdminRoutePage({
  route,
  user,
}: {
  route: (typeof ADMIN_ROUTES)[number];
  user: CurrentUser | null;
}) {
  return (
    <section data-screen-id={route.screenId} className="space-y-6">
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <h1 className="text-xl font-semibold text-dark">{route.label}</h1>
        <p className="mt-2 text-sm text-muted">{route.menuPath}</p>
      </div>
      <div className="grid grid-cols-12 gap-6">
        <section className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-8">
          <p className="text-sm font-semibold text-primary">{route.screenId}</p>
          <h2 className="mt-2 text-lg font-semibold text-dark">
            보호 route placeholder
          </h2>
          <p className="mt-3 text-sm text-muted">
            이 route는 로그인한 사용자가 메뉴 권한 확인 후 업무 화면에 접근할 수
            있음을 검증하기 위한 shell placeholder입니다.
          </p>
          <div
            className="mt-5 rounded-md bg-lightsuccess p-4 text-sm text-success"
            role="status"
          >
            접근 가능: {user?.loginId ?? "-"} / {user?.roles.join(", ") ?? "-"}
          </div>
        </section>
        <aside className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-4">
          <h3 className="text-lg font-semibold text-dark">상태</h3>
          <ul className="mt-4 space-y-2 text-sm text-muted">
            <li>loading: route guard 인증 확인</li>
            <li>permission: R09 또는 메뉴 권한 없음</li>
            <li>success: 현재 보호 route 렌더링</li>
          </ul>
        </aside>
      </div>
    </section>
  );
}

function DashboardCard({ title, value }: { title: string; value: string }) {
  return (
    <section className="col-span-12 rounded-md bg-white p-6 shadow-md md:col-span-4">
      <p className="text-sm text-muted">{title}</p>
      <p className="mt-3 text-2xl font-semibold text-dark">{value}</p>
    </section>
  );
}

function countMenus(items: Array<{ children: unknown[] }>): number {
  return items.reduce(
    (sum, item) =>
      sum + 1 + countMenus(item.children as Array<{ children: unknown[] }>),
    0,
  );
}

function routeMatchesPath(routePath: string, actualPath: string): boolean {
  if (routePath === actualPath) {
    return true;
  }
  if (!routePath.includes("{")) {
    return false;
  }
  const pattern = new RegExp(
    `^${routePath.replace(/[.*+?^${}()|[\]\\]/g, "\\$&").replace(/\\\{[^/]+\\\}/g, "[^/]+")}$`,
  );
  return pattern.test(actualPath);
}

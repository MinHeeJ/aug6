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
  const adminRoute = ADMIN_ROUTES.find((route) => route.path === path);

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

  const routedPage = renderAdminPage(adminRoute?.path);
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

function renderAdminPage(path: string | undefined) {
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
            US-01 관리자 접근 검증
          </h2>
          <p className="mt-3 text-sm text-muted">
            이 route는 시드 R09 관리자가 로그인 후 1차 목표 관리 화면에 접근할
            수 있음을 독립 검증하기 위한 보호 화면입니다.
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

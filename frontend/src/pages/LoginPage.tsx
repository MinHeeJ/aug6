import type React from "react";
import { useState } from "react";
import { ApiClientError, type CurrentUser } from "../api/apiClient";

export type AdminRoute = {
  path: string;
  label: string;
  screenId: string;
  menuPath: string;
};

export const ADMIN_ROUTES: AdminRoute[] = [
  {
    path: "/admin/users",
    label: "사용자 관리",
    screenId: "SCR-USER-MGMT",
    menuPath: "시스템 관리 > 사용자·조직 관리 > 사용자 관리",
  },
  {
    path: "/admin/organizations",
    label: "조직 관리",
    screenId: "SCR-ORG-MGMT",
    menuPath: "시스템 관리 > 사용자·조직 관리 > 조직 관리",
  },
  {
    path: "/admin/roles",
    label: "역할 관리",
    screenId: "SCR-ROLE-MGMT",
    menuPath: "시스템 관리 > 역할·권한 관리 > 역할 관리",
  },
  {
    path: "/admin/user-roles",
    label: "사용자 역할 관리",
    screenId: "SCR-USER-ROLE-MGMT",
    menuPath: "시스템 관리 > 역할·권한 관리 > 사용자 역할 관리",
  },
  {
    path: "/admin/menu-permissions",
    label: "메뉴 권한 관리",
    screenId: "SCR-MENU-PERMISSION-MGMT",
    menuPath: "시스템 관리 > 역할·권한 관리 > 메뉴 권한 관리",
  },
  {
    path: "/admin/function-permissions",
    label: "기능 권한 관리",
    screenId: "SCR-FUNCTION-PERMISSION-MGMT",
    menuPath: "시스템 관리 > 역할·권한 관리 > 기능 권한 관리",
  },
  {
    path: "/admin/privacy/policies",
    label: "개인정보 항목 관리",
    screenId: "SCR-PRIVACY-POLICY-MGMT",
    menuPath: "시스템 관리 > 개인정보 관리 > 개인정보 항목 관리",
  },
  {
    path: "/admin/privacy/permissions",
    label: "개인정보 조회권한",
    screenId: "SCR-PRIVACY-ACCESS-PERMISSION-MGMT",
    menuPath: "시스템 관리 > 개인정보 관리 > 개인정보 조회권한",
  },
  {
    path: "/admin/privacy/access-logs",
    label: "개인정보 처리이력",
    screenId: "SCR-PRIVACY-ACCESS-LOG",
    menuPath: "시스템 관리 > 개인정보 관리 > 개인정보 처리이력",
  },
  {
    path: "/admin/period-permissions",
    label: "기간별 권한 관리",
    screenId: "SCR-PERIOD-PERMISSION-MGMT",
    menuPath: "시스템 관리 > 역할·권한 관리 > 기간별 권한 관리",
  },
  {
    path: "/admin/temporary-permissions",
    label: "임시 권한 관리",
    screenId: "SCR-TEMPORARY-PERMISSION-MGMT",
    menuPath: "시스템 관리 > 역할·권한 관리 > 임시 권한 관리",
  },
  {
    path: "/admin/permission-history",
    label: "권한 변경 이력 조회",
    screenId: "SCR-PERMISSION-HISTORY",
    menuPath: "시스템 관리 > 역할·권한 관리 > 권한 변경 이력 조회",
  },
  {
    path: "/admin/menu-structure",
    label: "메뉴 구조 관리",
    screenId: "SCR-MENU-STRUCTURE-MGMT",
    menuPath: "시스템 관리 > 메뉴 관리 > 메뉴 구조 관리",
  },
  {
    path: "/admin/menu-info",
    label: "메뉴 정보 관리",
    screenId: "SCR-MENU-INFO-MGMT",
    menuPath: "시스템 관리 > 메뉴 관리 > 메뉴 정보 관리",
  },
  {
    path: "/admin/code-groups",
    label: "코드그룹 관리",
    screenId: "SCR-CODE-GROUP-MGMT",
    menuPath: "시스템 관리 > 공통코드 관리 > 코드그룹 관리",
  },
  {
    path: "/admin/detail-codes",
    label: "상세코드 관리",
    screenId: "SCR-DETAIL-CODE-MGMT",
    menuPath: "시스템 관리 > 공통코드 관리 > 상세코드 관리",
  },
  {
    path: "/admin/messages",
    label: "메시지 관리",
    screenId: "SCR-MESSAGE-MGMT",
    menuPath: "시스템 관리 > 시스템 환경설정 > 메시지 관리",
  },
  {
    path: "/admin/notices",
    label: "공지사항 관리",
    screenId: "SCR-NOTICE-MGMT",
    menuPath: "시스템 관리 > 공지·도움말 관리 > 공지사항 관리",
  },
  {
    path: "/admin/manuals",
    label: "매뉴얼 관리",
    screenId: "SCR-MANUAL-MGMT",
    menuPath: "시스템 관리 > 공지·도움말 관리 > 매뉴얼 관리",
  },
  {
    path: "/admin/help-contents",
    label: "도움말 관리",
    screenId: "SCR-HELP-MGMT",
    menuPath: "시스템 관리 > 공지·도움말 관리 > 도움말 관리",
  },
  {
    path: "/admin/batch-definitions",
    label: "배치 정의 관리",
    screenId: "SCR-BATCH-DEFINITION-MGMT",
    menuPath: "시스템 운영 관리 > 배치작업 관리 > 배치 정의 관리",
  },
  {
    path: "/admin/batch-executions",
    label: "배치 실행 관리",
    screenId: "SCR-BATCH-EXECUTION-MGMT",
    menuPath: "시스템 운영 관리 > 배치작업 관리 > 배치 실행 관리",
  },
  {
    path: "/admin/batch-results",
    label: "배치 결과 조회",
    screenId: "SCR-BATCH-RESULT-MGMT",
    menuPath: "시스템 운영 관리 > 배치작업 관리 > 배치 결과 조회",
  },
  {
    path: "/admin/batch-retries",
    label: "배치 오류 재처리",
    screenId: "SCR-BATCH-RETRY-MGMT",
    menuPath: "시스템 운영 관리 > 배치작업 관리 > 배치 오류 재처리",
  },
  {
    path: "/admin/excel-upload-templates",
    label: "업로드 양식 관리",
    screenId: "SCR-UPLOAD-TEMPLATE-MGMT",
    menuPath: "파일·데이터 관리 > 엑셀 관리 > 업로드 양식 관리",
  },
  {
    path: "/admin/excel-uploads",
    label: "엑셀 업로드",
    screenId: "SCR-EXCEL-UPLOAD-MGMT",
    menuPath: "파일·데이터 관리 > 엑셀 관리 > 엑셀 업로드",
  },
  {
    path: "/admin/excel-upload-histories",
    label: "업로드 이력",
    screenId: "SCR-UPLOAD-HISTORY-MGMT",
    menuPath: "파일·데이터 관리 > 엑셀 관리 > 업로드 이력",
  },
  {
    path: "/admin/excel-upload-errors",
    label: "업로드 오류 관리",
    screenId: "SCR-UPLOAD-ERROR-MGMT",
    menuPath: "파일·데이터 관리 > 엑셀 관리 > 업로드 오류 관리",
  },
  {
    path: "/admin/excel-downloads",
    label: "엑셀 다운로드",
    screenId: "SCR-EXCEL-DOWNLOAD-MGMT",
    menuPath: "파일·데이터 관리 > 엑셀 관리 > 엑셀 다운로드",
  },
  {
    path: "/admin/menu-usage",
    label: "메뉴 사용 관리",
    screenId: "SCR-MENU-USAGE-MGMT",
    menuPath: "시스템 관리 > 역할·권한 관리 > 메뉴 사용 관리",
  },
  {
    path: "/admin/detail-code-usage",
    label: "코드 사용 관리",
    screenId: "SCR-CODE-USAGE-MGMT",
    menuPath: "시스템 관리 > 공통코드 관리 > 코드 사용 관리",
  },
  {
    path: "/admin/common-settings",
    label: "공통 환경설정",
    screenId: "SCR-COMMON-SETTINGS",
    menuPath: "시스템 관리 > 공통 운영 관리 > 공통 환경설정",
  },
  {
    path: "/admin/base-years",
    label: "기준연도 관리",
    screenId: "SCR-BASE-YEAR-MGMT",
    menuPath: "시스템 관리 > 공통 운영 관리 > 기준연도 관리",
  },
  {
    path: "/admin/position-assignments",
    label: "보직 관리",
    screenId: "SCR-POSITION-ASSIGNMENT-MGMT",
    menuPath: "시스템 관리 > 사용자·조직 관리 > 보직 관리",
  },
  {
    path: "/admin/duty-assignments",
    label: "업무담당자 관리",
    screenId: "SCR-DUTY-ASSIGNMENT-MGMT",
    menuPath: "시스템 관리 > 사용자·조직 관리 > 업무담당자 관리",
  },
  {
    path: "/admin/data-scope-rules",
    label: "데이터 범위 권한",
    screenId: "SCR-DATA-SCOPE-RULE-MGMT",
    menuPath: "시스템 관리 > 역할·권한 관리 > 데이터 범위 권한",
  },
  {
    path: "/admin/evaluation-organization-mappings",
    label: "평가조직 매핑",
    screenId: "SCR-EVALUATION-ORG-MAPPING",
    menuPath: "업무 운영 관리 > 업무권한 관리 > 평가조직 매핑",
  },
  {
    path: "/admin/evaluation-areas",
    label: "평가영역 관리",
    screenId: "SCR-EVALUATION-AREA-MGMT",
    menuPath: "평가 기준 관리 > 평가 기준정보 관리 > 평가영역 관리",
  },
  {
    path: "/admin/evaluation-items",
    label: "평가항목 관리",
    screenId: "SCR-EVALUATION-ITEM-MGMT",
    menuPath: "평가 기준 관리 > 평가 기준정보 관리 > 평가항목 관리",
  },
  {
    path: "/admin/evaluation-elements",
    label: "평가요소 관리",
    screenId: "SCR-EVALUATION-ELEMENT-MGMT",
    menuPath: "평가 기준 관리 > 평가 기준정보 관리 > 평가요소 관리",
  },
  {
    path: "/admin/evaluation-management-items",
    label: "관리항목 관리",
    screenId: "SCR-EVALUATION-MANAGEMENT-ITEM-MGMT",
    menuPath: "평가 기준 관리 > 평가 기준정보 관리 > 관리항목 관리",
  },
  {
    path: "/admin/area-element-systems",
    label: "영역별 평가요소 체계 관리",
    screenId: "SCR-AREA-ELEMENT-SYSTEM-MGMT",
    menuPath: "평가 기준 관리 > 평가 기준정보 관리 > 영역별 평가요소 체계 관리",
  },
  {
    path: "/admin/evaluation-dates",
    label: "평가일자 관리",
    screenId: "SCR-EVALUATION-DATE-MGMT",
    menuPath: "평가 기준 관리 > 업무기간 관리 > 평가일자 관리",
  },
  {
    path: "/admin/input-periods",
    label: "입력기간 관리",
    screenId: "SCR-INPUT-PERIOD-MGMT",
    menuPath: "평가 기준 관리 > 업무기간 관리 > 입력기간 관리",
  },
  {
    path: "/admin/modification-periods",
    label: "수정기간 관리",
    screenId: "SCR-MODIFICATION-PERIOD-MGMT",
    menuPath: "평가 기준 관리 > 업무기간 관리 > 수정기간 관리",
  },
  {
    path: "/admin/business-status-codes",
    label: "상태코드 관리",
    screenId: "SCR-BUSINESS-STATUS-CODE",
    menuPath: "업무 운영 관리 > 업무상태 관리 > 상태코드 관리",
  },
  {
    path: "/admin/business-status-transitions",
    label: "상태 전이 관리",
    screenId: "SCR-BUSINESS-STATUS-TRANSITION",
    menuPath: "업무 운영 관리 > 업무상태 관리 > 상태 전이 관리",
  },
  {
    path: "/admin/rejection-reasons",
    label: "반려사유 관리",
    screenId: "SCR-REJECTION-REASON",
    menuPath: "업무 운영 관리 > 의견·반려 관리 > 반려사유 관리",
  },
  {
    path: "/admin/data-change-histories",
    label: "데이터 변경 이력",
    screenId: "SCR-DATA-CHANGE-HISTORY",
    menuPath: "파일·데이터 관리 > 데이터 이력 관리 > 데이터 변경 이력",
  },
  {
    path: "/admin/deleted-business-data",
    label: "삭제자료 관리",
    screenId: "SCR-DELETED-BUSINESS-DATA",
    menuPath: "파일·데이터 관리 > 데이터 이력 관리 > 삭제자료 관리",
  },
  {
    path: "/admin/evaluation-scores",
    label: "평가점수 관리",
    screenId: "SCR-EVAL-SCORE-MGMT",
    menuPath: "평가 기준 관리 > 평가 기준정보 관리 > 평가점수 관리",
  },
  {
    path: "/admin/participation-rates",
    label: "참여구분·배분율 관리",
    screenId: "SCR-PARTICIPATION-RATE-MGMT",
    menuPath: "평가 기준 관리 > 평가 기준정보 관리 > 참여구분·배분율 관리",
  },
  {
    path: "/admin/calculation-formulas",
    label: "계산식 관리",
    screenId: "SCR-CALC-FORMULA-MGMT",
    menuPath: "평가 기준 관리 > 평가 기준정보 관리 > 계산식 관리",
  },
  {
    path: "/admin/department-chair-confirm-periods",
    label: "학과장 확인기간 관리",
    screenId: "SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT",
    menuPath: "평가 기준 관리 > 업무기간 관리 > 학과장 확인기간 관리",
  },
  {
    path: "/admin/business-periods",
    label: "평가·업적입력 기간 관리",
    screenId: "SCR-BUSINESS-PERIOD-INTEGRATED-MGMT",
    menuPath: "평가 기준 관리 > 업무기간 관리 > 평가·업적입력 기간 관리",
  },
  {
    path: "/admin/evaluation-rule-sets",
    label: "업적평가 기준·점수규칙 관리",
    screenId: "SCR-EVAL-RULE-SET-MGMT",
    menuPath:
      "평가 기준 관리 > 평가 기준정보 관리 > 업적평가 기준·점수규칙 관리",
  },
  {
    path: "/admin/journal-indexing-infos",
    label: "학술지·후보지 등재정보 관리",
    screenId: "SCR-JOURNAL-INDEXING-MGMT",
    menuPath:
      "평가 기준 관리 > 평가 기준정보 관리 > 학술지·후보지 등재정보 관리",
  },
  {
    path: "/admin/security/active-sessions",
    label: "접속현황 조회",
    screenId: "SCR-ACTIVE-SESSION-STATUS",
    menuPath: "보안·감사 관리 > 접속기록 관리 > 접속현황 조회",
  },
  {
    path: "/admin/security/session-termination-histories",
    label: "로그아웃·만료 이력",
    screenId: "SCR-SESSION-TERMINATION-HISTORY",
    menuPath: "보안·감사 관리 > 접속기록 관리 > 로그아웃·만료 이력",
  },
  {
    path: "/admin/audit/business-process-logs",
    label: "업무처리 로그",
    screenId: "SCR-BUSINESS-PROCESS-LOG",
    menuPath: "보안·감사 관리 > 감사로그 관리 > 업무처리 로그",
  },
  {
    path: "/admin/audit/sensitive-information-access-logs",
    label: "중요정보 조회 로그",
    screenId: "SCR-SENSITIVE-INFO-ACCESS-LOG",
    menuPath: "보안·감사 관리 > 감사로그 관리 > 중요정보 조회 로그",
  },
  {
    path: "/admin/audit/permission-change-logs",
    label: "권한변경 로그",
    screenId: "SCR-PERMISSION-CHANGE-LOG",
    menuPath: "보안·감사 관리 > 감사로그 관리 > 권한변경 로그",
  },
  {
    path: "/admin/korus-faculty-sync",
    label: "KORUS 교원 동기화",
    screenId: "SCR-KORUS-FACULTY-SYNC",
    menuPath: "교수업적평가 파일럿 > KORUS 연계 > KORUS 교원 동기화",
  },
  {
    path: "/admin/full-time-faculty-statuses",
    label: "전임교원 현황",
    screenId: "SCR-FULL-TIME-FACULTY-STATUS",
    menuPath: "교수업적평가 파일럿 > 교원 현황 > 전임교원 현황",
  },
  {
    path: "/researcher-profiles",
    label: "연구자 프로필 목록",
    screenId: "SCR-RESEARCHER-PROFILE-LIST",
    menuPath: "교수업적평가 파일럿 > 연구자 프로필 > 프로필 목록",
  },
  {
    path: "/researcher-profiles/{employeeNo}",
    label: "연구자 프로필 상세",
    screenId: "SCR-RESEARCHER-PROFILE-DETAIL",
    menuPath: "교수업적평가 파일럿 > 연구자 프로필 > 프로필 상세",
  },
  {
    path: "/admin/researcher-profiles/degree-prerequisite-missing",
    label: "선행학위 미충족",
    screenId: "SCR-DEGREE-PREREQ-MISSING",
    menuPath: "교수업적평가 파일럿 > 연구자 프로필 > 선행학위 미충족",
  },
  {
    path: "/admin/achievement-data-histories",
    label: "업적데이터 변경이력",
    screenId: "SCR-ACHIEVEMENT-DATA-HISTORY",
    menuPath: "교수업적평가 파일럿 > 업적 데이터 이력 > 변경이력 조회",
  },
  {
    path: "/admin/achievement-data-as-of",
    label: "업적데이터 기준시점",
    screenId: "SCR-ACHIEVEMENT-DATA-AS-OF",
    menuPath: "교수업적평가 파일럿 > 업적 데이터 이력 > 기준시점 조회",
  },
];

export type LoginValidationErrors = Partial<
  Record<"loginId" | "password", string>
>;

export function validateLoginInput(
  loginId: string,
  password: string,
): LoginValidationErrors {
  const errors: LoginValidationErrors = {};
  if (!loginId.trim()) {
    errors.loginId = "사용자 ID를 입력하세요.";
  }
  if (!password.trim()) {
    errors.password = "비밀번호를 입력하세요.";
  }
  return errors;
}

export function describeLoginFailure(caught: unknown): string {
  if (caught instanceof ApiClientError && caught.status === 401) {
    return "아이디 또는 비밀번호가 올바르지 않습니다.";
  }
  if (
    typeof caught === "object" &&
    caught !== null &&
    "status" in caught &&
    (caught as { status: number }).status === 401
  ) {
    return "아이디 또는 비밀번호가 올바르지 않습니다.";
  }
  return caught instanceof Error
    ? caught.message
    : "로그인 중 오류가 발생했습니다.";
}

export function canAccessAdminRoute(
  user: CurrentUser | null | undefined,
  path: string,
): boolean {
  if (!user) {
    return false;
  }
  return hasMenuUrl(user.menus, path);
}

function hasMenuUrl(menus: CurrentUser["menus"], path: string): boolean {
  return menus.some(
    (menu) =>
      menuUrlMatchesPath(menu.url, path) || hasMenuUrl(menu.children, path),
  );
}

function menuUrlMatchesPath(
  menuUrl: string | undefined,
  path: string,
): boolean {
  if (!menuUrl) {
    return false;
  }
  if (menuUrl === path) {
    return true;
  }
  if (!menuUrl.includes("{")) {
    return false;
  }
  const pattern = new RegExp(
    `^${menuUrl.replace(/[.*+?^${}()|[\]\\]/g, "\\$&").replace(/\\\{[^/]+\\\}/g, "[^/]+")}$`,
  );
  return pattern.test(path);
}

type LoginPageProps = {
  onLogin: (loginId: string, password: string) => Promise<void>;
  onHealth: () => Promise<{ status?: string; service?: string }>;
  onLoginSuccess?: () => void;
};

export function LoginPage({
  onLogin,
  onHealth,
  onLoginSuccess,
}: LoginPageProps) {
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<LoginValidationErrors>({});
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [healthMessage, setHealthMessage] =
    useState<string>("아직 확인하지 않았습니다.");
  const [submitting, setSubmitting] = useState(false);
  const [checkingHealth, setCheckingHealth] = useState(false);

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors = validateLoginInput(loginId, password);
    setFieldErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      setStatusMessage("입력값을 확인하세요.");
      return;
    }

    try {
      setSubmitting(true);
      setStatusMessage("인증 요청 중입니다.");
      await onLogin(loginId, password);
      setStatusMessage(
        "R09 시스템관리자 확인 후 시스템 관리 화면으로 이동합니다.",
      );
      onLoginSuccess?.();
    } catch (caught) {
      setStatusMessage(describeLoginFailure(caught));
    } finally {
      setSubmitting(false);
    }
  };

  const checkHealth = async () => {
    try {
      setCheckingHealth(true);
      const health = await onHealth();
      setHealthMessage(`/api/health ${health.status ?? "UP"} 확인`);
    } catch (caught) {
      setHealthMessage(
        caught instanceof Error ? caught.message : "서비스 상태 확인 실패",
      );
    } finally {
      setCheckingHealth(false);
    }
  };

  const resetMessages =
    (setter: (value: string) => void) =>
    (event: React.ChangeEvent<HTMLInputElement>) => {
      setter(event.target.value);
      setFieldErrors({});
      setStatusMessage(null);
    };

  return (
    <main className="min-h-screen bg-lightgray px-5 py-[30px] text-link">
      <section className="mx-auto max-w-5xl overflow-hidden rounded-md bg-white shadow-md">
        <header className="border-b border-ld bg-lightsecondary px-6 py-5">
          <p className="text-sm font-semibold text-primary">
            한국교원대학교 교수업적평가시스템
          </p>
          <h1 className="mt-2 text-2xl font-semibold text-dark">
            공통기능 1차 범위
          </h1>
        </header>
        <div className="grid grid-cols-12 gap-6 p-6">
          <form
            className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-6"
            onSubmit={submit}
          >
            <p className="card-subtitle">SCR-LOGIN</p>
            <h2 className="card-title mb-4 text-lg font-semibold text-dark">
              로그인
            </h2>
            <label
              className="mb-4 block text-sm font-semibold text-ld"
              htmlFor="loginId"
            >
              사용자 ID<span className="ms-1 text-error">*</span>
              <input
                id="loginId"
                name="loginId"
                className="mt-2 flex h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm text-ld focus-visible:border-primary focus-visible:outline-0"
                value={loginId}
                onChange={resetMessages(setLoginId)}
                aria-invalid={Boolean(fieldErrors.loginId)}
              />
              {fieldErrors.loginId ? (
                <span className="mt-1 block text-xs text-error">
                  {fieldErrors.loginId}
                </span>
              ) : null}
            </label>
            <label
              className="mb-4 block text-sm font-semibold text-ld"
              htmlFor="password"
            >
              비밀번호<span className="ms-1 text-error">*</span>
              <input
                id="password"
                name="password"
                type="password"
                className="mt-2 flex h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm text-ld focus-visible:border-primary focus-visible:outline-0"
                value={password}
                onChange={resetMessages(setPassword)}
                aria-invalid={Boolean(fieldErrors.password)}
              />
              {fieldErrors.password ? (
                <span className="mt-1 block text-xs text-error">
                  {fieldErrors.password}
                </span>
              ) : null}
            </label>
            <button
              className="inline-flex h-10 w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-white shadow-btn-shadow transition-colors hover:bg-secondary disabled:pointer-events-none disabled:opacity-50"
              type="submit"
              disabled={submitting}
            >
              {submitting ? "처리 중" : "로그인"}
            </button>
            <div
              className="mt-4 rounded-md bg-lightprimary p-4 text-sm text-primary"
              role="status"
            >
              {statusMessage ?? "admin / admin 계정으로 로그인하세요."}
            </div>
            <p className="mt-4 text-xs text-muted">
              README 또는 quickstart에서 실행·로그인·주요 화면 검증 방법을
              확인합니다.
            </p>
          </form>

          <aside className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-6">
            <h2 className="card-title mb-4 text-lg font-semibold text-dark">
              로컬 검증 안내
            </h2>
            <div className="rounded-md bg-lightsecondary p-4 text-sm text-link">
              <p className="font-semibold">시드 관리자 계정</p>
              <p className="mt-2">- loginId: admin</p>
              <p>- password: admin</p>
              <p className="mt-3 text-muted">
                Docker Compose 실행 직후 로그인 가능해야 합니다.
              </p>
            </div>
            <button
              className="mt-4 inline-flex h-10 items-center justify-center rounded-md border border-primary bg-transparent px-4 py-2 text-sm font-medium text-primary transition-colors hover:bg-primary hover:text-white disabled:pointer-events-none disabled:opacity-50"
              type="button"
              disabled={checkingHealth}
              onClick={() => void checkHealth()}
            >
              {checkingHealth ? "확인 중" : "서비스 상태 확인"}
            </button>
            <div className="mt-4 rounded-md bg-lightsuccess p-4 text-sm text-success">
              결과: {healthMessage}
            </div>
            <div className="mt-6 rounded-md border border-ld p-4 text-sm text-muted">
              <p className="font-semibold text-link">성공 흐름</p>
              <p className="mt-2">
                login 성공 → getCurrentUser 확인 → 시스템 관리 shell 이동
              </p>
              <p className="mt-2">
                R09 또는 메뉴 권한이 없으면 permission 상태를 표시합니다.
              </p>
            </div>
          </aside>
        </div>
      </section>
    </main>
  );
}

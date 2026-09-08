import type React from "react";
import i18next from "i18next";
import { I18nextProvider } from "react-i18next";
import { initReactI18next } from "react-i18next";

export type SupportedLanguage = "ko" | "en";

export const LANGUAGE_SESSION_KEY = "app.language";

const supportedLanguages: SupportedLanguage[] = ["ko", "en"];

export const resources = {
  ko: {
    translation: {},
  },
  en: {
    translation: {
      "시스템 관리 콘솔": "System Administration Console",
      언어: "Language",
      "모바일 메뉴 닫기": "Close mobile menu",
      닫기: "Close",
      "표시 가능한 메뉴가 없습니다.": "No menus are available.",
      로그아웃: "Logout",
      "모바일 메뉴": "Mobile menu",
      "메뉴 펼치기": "Expand menu",
      "메뉴 접기": "Collapse menu",
      "헤더 주요 메뉴": "Header main menu",
      "메뉴 또는 화면 검색": "Search menu or screen",
      "메뉴 검색어": "Menu search keyword",
      "메뉴명, 화면ID, 경로를 검색하세요":
        "Search by menu name, screen ID, or path",
      "검색 결과가 없습니다.": "No search results.",
      "라이트 모드": "Light mode",
      "다크 모드": "Dark mode",
      알림: "Notifications",
      "사용자 관리": "User Management",
      "시스템 관리 · 사용자·조직 관리 · 사용자 관리":
        "System Management · User/Organization Management · User Management",
      "사용자 관리 권한 없음": "No User Management Permission",
      "R09 시스템관리자 권한 또는 메뉴 접근 권한이 필요합니다.":
        "R09 system administrator permission or menu access permission is required.",
      "사용자 관리 오류": "User Management Error",
      "저장 완료": "Save Complete",
      검색조건: "Search Criteria",
      "KORUS 원천 인사정보는 조회 전용이며 로컬 DB의 사용여부와 업무 역할만 저장합니다.":
        "KORUS source HR information is read-only; only local system usage and business roles are saved.",
      "조건 초기화": "Reset Conditions",
      조회: "Search",
      교번: "Employee No.",
      성명: "Name",
      "소속 조직코드": "Organization Code",
      직급: "Rank",
      재직상태: "Employment Status",
      역할: "Role",
      사용여부: "Use Status",
      전체: "All",
      재직: "Active",
      휴직: "Leave",
      퇴직: "Retired",
      "사용자 목록": "User List",
      "사용자 조회 중": "Loading Users",
      "사용자 없음": "No Users",
      "조건에 맞는 사용자가 없습니다.": "No users match the conditions.",
      "교번/성명": "Employee No./Name",
      "소속/직급": "Organization/Rank",
      보직: "Position",
      "재직/퇴직일자": "Employment/Retirement Date",
      사용: "Use",
      동기화: "Sync",
      미사용: "Not Used",
      "상세/편집": "Detail/Edit",
      "시스템 사용여부와 업무 역할만 수정 가능":
        "Only system use status and business roles can be edited",
      "사용자를 선택하세요": "Select a user",
      "목록 행을 선택하면 상세 정보와 편집 폼이 표시됩니다.":
        "Select a list row to show details and the edit form.",
      "KORUS 교번": "KORUS Employee No.",
      "KORUS 성명": "KORUS Name",
      "KORUS 소속": "KORUS Organization",
      "KORUS 직급/재직": "KORUS Rank/Employment",
      "KORUS 원천 필드는 읽기 전용입니다.":
        "KORUS source fields are read-only.",
      "시스템 사용여부": "System Use Status",
      "업무 역할": "Business Role",
      "선택:": "Selected:",
      "역할 유효 시작일": "Role Valid Start Date",
      "역할 유효 종료일": "Role Valid End Date",
      "변경 사유": "Change Reason",
      "사용여부 저장": "Save Use Status",
      "역할 저장": "Save Roles",
      취소: "Cancel",
      "사용자 정보가 저장되었습니다.": "User information has been saved.",
      "권한 없음": "No Permission",
      "요청 처리 중 오류가 발생했습니다.":
        "An error occurred while processing the request.",
      "선택 없음": "None selected",
      "불러오는 중": "Loading",
      "잠시만 기다려 주세요.": "Please wait.",
      "데이터 없음": "No Data",
      "조회 조건에 맞는 결과가 없습니다.":
        "No results match the search conditions.",
      "오류 발생": "Error Occurred",
      "요청을 처리하지 못했습니다.": "Could not process the request.",
      "이 화면에 접근할 권한이 없습니다.":
        "You do not have permission to access this screen.",
      "처리 완료": "Complete",
      "변경사항이 저장되었습니다.": "Changes have been saved.",
      "인증 확인 중": "Checking Authentication",
      "세션 정보를 확인하고 있습니다.": "Checking session information.",
      "인증 오류": "Authentication Error",
      "인증 처리 중 오류가 발생했습니다.":
        "An error occurred while processing authentication.",
      "권한이 없습니다": "No Permission",
      "화면 접근 권한이 없습니다.": "screen access permission is required.",
      "한국교원대학교 교수업적평가시스템 공통기능 기반":
        "Korea National University of Education faculty achievement evaluation common foundation",
      "권한 역할": "Permission Roles",
      "시스템 관리 메뉴": "System Management Menus",
      "세션 상태": "Session Status",
      인증됨: "Authenticated",
      "공통 상태 컴포넌트": "Common State Components",
      "보호 route placeholder": "Protected route placeholder",
      "이 route는 로그인한 사용자가 메뉴 권한 확인 후 업무 화면에 접근할 수 있음을 검증하기 위한 shell placeholder입니다.":
        "This shell placeholder verifies that an authenticated user can access the business screen after menu permission checks.",
      상태: "Status",
      "접근 가능:": "Accessible:",
      "loading: route guard 인증 확인":
        "loading: route guard authentication check",
      "permission: R09 또는 메뉴 권한 없음":
        "permission: no R09 or menu permission",
      "success: 현재 보호 route 렌더링":
        "success: render current protected route",
    },
  },
} as const;

function normalizeLanguage(language?: string | null): SupportedLanguage {
  return language === "en" ? "en" : "ko";
}

export function getInitialLanguage(): SupportedLanguage {
  if (typeof window === "undefined") return "ko";
  return normalizeLanguage(window.sessionStorage.getItem(LANGUAGE_SESSION_KEY));
}

export const i18n = i18next.createInstance();

void i18n.use(initReactI18next).init({
  lng: getInitialLanguage(),
  fallbackLng: "ko",
  supportedLngs: supportedLanguages,
  resources,
  interpolation: {
    escapeValue: false,
  },
  returnEmptyString: false,
  parseMissingKeyHandler: (key) => key,
});

i18n.on("languageChanged", (language) => {
  const normalized = normalizeLanguage(language);
  if (typeof window !== "undefined") {
    window.sessionStorage.setItem(LANGUAGE_SESSION_KEY, normalized);
  }
});

export function setApplicationLanguage(language: string): SupportedLanguage {
  const normalized = normalizeLanguage(language);
  if (typeof window !== "undefined") {
    window.sessionStorage.setItem(LANGUAGE_SESSION_KEY, normalized);
  }
  void i18n.changeLanguage(normalized);
  return normalized;
}

export function I18nProvider({ children }: { children: React.ReactNode }) {
  return <I18nextProvider i18n={i18n}>{children}</I18nextProvider>;
}

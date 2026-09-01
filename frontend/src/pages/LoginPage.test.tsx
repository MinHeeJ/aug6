import { describe, expect, it } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import {
  ADMIN_ROUTES,
  LoginPage,
  canAccessAdminRoute,
  describeLoginFailure,
  validateLoginInput,
} from "./LoginPage";
import type { CurrentUser } from "../api/apiClient";

describe("LoginPage", () => {
  it("shows field-level validation errors when login id or password is missing", () => {
    expect(validateLoginInput("", "")).toEqual({
      loginId: "사용자 ID를 입력하세요.",
      password: "비밀번호를 입력하세요.",
    });
  });

  it("maps authentication failure to a Korean 401 message without hiding the seed account guide", () => {
    expect(
      describeLoginFailure({ status: 401, message: "UNAUTHENTICATED" }),
    ).toBe("아이디 또는 비밀번호가 올바르지 않습니다.");
    const html = renderToStaticMarkup(
      <LoginPage
        onLogin={async () => undefined}
        onHealth={async () => ({ status: "UP" })}
      />,
    );
    expect(html).toContain("시드 관리자 계정");
    expect(html).toContain("loginId: admin");
    expect(html).toContain("password: admin");
    expect(html).not.toContain('name="loginId" value="admin"');
    expect(html).not.toContain('name="password" type="password" value="admin"');
  });

  it("describes BASIC-19 menu paths with menu screens under roles and privacy screens under system management", () => {
    expect(routePath("/admin/menu-structure")).toBe(
      "시스템 관리 > 메뉴 관리 > 메뉴 구조 관리",
    );
    expect(routePath("/admin/menu-info")).toBe(
      "시스템 관리 > 메뉴 관리 > 메뉴 정보 관리",
    );
    expect(routePath("/admin/menu-usage")).toBe(
      "시스템 관리 > 역할·권한 관리 > 메뉴 사용 관리",
    );
    expect(routePath("/admin/privacy/policies")).toBe(
      "시스템 관리 > 개인정보 관리 > 개인정보 항목 관리",
    );
    expect(routePath("/admin/privacy/permissions")).toBe(
      "시스템 관리 > 개인정보 관리 > 개인정보 조회권한",
    );
    expect(routePath("/admin/privacy/access-logs")).toBe(
      "시스템 관리 > 개인정보 관리 > 개인정보 처리이력",
    );
    expect(routePath("/admin/audit/sensitive-information-access-logs")).toBe(
      "보안·감사 관리 > 감사로그 관리 > 중요정보 조회 로그",
    );
    expect(routePath("/admin/audit/permission-change-logs")).toBe(
      "보안·감사 관리 > 감사로그 관리 > 권한변경 로그",
    );
  });

  it("allows seed R09 administrator to access all configured admin routes after login", () => {
    const adminUser: CurrentUser = {
      userId: 1,
      loginId: "admin",
      employeeNo: "E0001",
      name: "시스템 관리자",
      roles: ["R09"],
      menus: ADMIN_ROUTES.map((route, index) => ({
        menuId: index + 1,
        parentMenuId: 100,
        menuName: route.label,
        screenId: route.screenId,
        url: route.path,
        icon: "settings",
        displayOrder: index + 1,
        children: [],
      })),
    };

    expect(ADMIN_ROUTES).toHaveLength(69);
    expect(
      ADMIN_ROUTES.every((route) => canAccessAdminRoute(adminUser, route.path)),
    ).toBe(true);
  });

  it("registers BASIC-36 business route placeholders without replacing the existing shell", () => {
    expect(routePath("/admin/korus-faculty-sync")).toBe(
      "교수업적평가 파일럿 > KORUS 연계 > KORUS 교원 동기화",
    );
    expect(routePath("/admin/full-time-faculty-statuses")).toBe(
      "교수업적평가 파일럿 > 교원 현황 > 전임교원 현황",
    );
    expect(routePath("/researcher-profiles")).toBe(
      "교수업적평가 파일럿 > 연구자 프로필 > 프로필 목록",
    );
    expect(routePath("/researcher-profiles/{employeeNo}")).toBe(
      "교수업적평가 파일럿 > 연구자 프로필 > 프로필 상세",
    );
    expect(
      routePath("/admin/researcher-profiles/degree-prerequisite-missing"),
    ).toBe("교수업적평가 파일럿 > 연구자 프로필 > 선행학위 미충족");
    expect(routePath("/admin/achievement-data-histories")).toBe(
      "교수업적평가 파일럿 > 업적 데이터 이력 > 변경이력 조회",
    );
    expect(routePath("/admin/achievement-data-as-of")).toBe(
      "교수업적평가 파일럿 > 업적 데이터 이력 > 기준시점 조회",
    );
  });

  it("uses menu URLs rather than role codes alone as the frontend route guard", () => {
    const r09WithoutTargetMenu: CurrentUser = {
      userId: 1,
      loginId: "admin",
      employeeNo: "E0001",
      name: "시스템 관리자",
      roles: ["R09"],
      menus: [
        {
          menuId: 100,
          menuName: "시스템 관리",
          displayOrder: 1,
          children: [
            {
              menuId: 111,
              parentMenuId: 100,
              menuName: "사용자 관리",
              screenId: "SCR-USER-MGMT",
              url: "/admin/users",
              displayOrder: 1,
              children: [],
            },
          ],
        },
      ],
    };

    expect(canAccessAdminRoute(r09WithoutTargetMenu, "/admin/users")).toBe(
      true,
    );
    expect(canAccessAdminRoute(r09WithoutTargetMenu, "/admin/roles")).toBe(
      false,
    );
  });

  it("allows domain-data researcher profile detail URLs through the placeholder pattern", () => {
    const researcherUser: CurrentUser = {
      userId: 2,
      loginId: "teacher",
      employeeNo: "E10001",
      name: "교원",
      roles: ["R01"],
      menus: [
        {
          menuId: 701,
          menuName: "연구자 프로필 상세",
          screenId: "SCR-RESEARCHER-PROFILE-DETAIL",
          url: "/researcher-profiles/{employeeNo}",
          displayOrder: 1,
          children: [],
        },
      ],
    };

    expect(
      canAccessAdminRoute(researcherUser, "/researcher-profiles/E10001"),
    ).toBe(true);
    expect(canAccessAdminRoute(researcherUser, "/researcher-profiles")).toBe(
      false,
    );
  });
});

function routePath(path: string) {
  const route = ADMIN_ROUTES.find((candidate) => candidate.path === path);
  if (!route) throw new Error(`Missing route: ${path}`);
  return route.menuPath;
}

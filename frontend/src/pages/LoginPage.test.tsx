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

  it("allows seed R09 administrator to access all nine first-scope admin routes after login", () => {
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

    expect(ADMIN_ROUTES).toHaveLength(9);
    expect(
      ADMIN_ROUTES.every((route) => canAccessAdminRoute(adminUser, route.path)),
    ).toBe(true);
  });
});

import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AdminShell } from "./AdminShell";
import type { CurrentUser } from "../../api/apiClient";

const authState = vi.hoisted(() => ({
  user: null as CurrentUser | null,
}));

vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: authState.user,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  }),
}));

function menuNode(
  menuId: number,
  menuName: string,
  children = [] as CurrentUser["menus"],
  url?: string,
  parentMenuId?: number,
): CurrentUser["menus"][number] {
  return {
    menuId,
    parentMenuId,
    menuName,
    url,
    displayOrder: menuId,
    children,
  };
}

describe("AdminShell sidebar menu", () => {
  beforeEach(() => {
    window.history.pushState({}, "", "/admin/users");
    authState.user = {
      userId: 1,
      loginId: "admin",
      name: "관리자",
      roles: ["R09"],
      menus: [
        menuNode(1, "시스템 관리", [
          menuNode(
            2,
            "역할권한관리",
            [
              menuNode(
                3,
                "개인정보 처리이력",
                [
                  menuNode(
                    4,
                    "개인정보 처리이력 상세 다운로드 승인 관리",
                    [],
                    "/admin/privacy/access-logs/download-approval",
                    3,
                  ),
                ],
                "/admin/privacy/access-logs",
                2,
              ),
            ],
            undefined,
            1,
          ),
        ]),
      ],
    };
  });

  it("opens fourth-level menus even when the third-level parent also has a route", () => {
    render(<AdminShell>본문</AdminShell>);

    fireEvent.click(
      screen.getByRole("button", { name: "역할권한관리 하위 메뉴 펼치기" }),
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: "개인정보 처리이력 하위 메뉴 펼치기",
      }),
    );

    const fourthLevelMenu = screen.getByRole("link", {
      name: "개인정보 처리이력 상세 다운로드 승인 관리",
    });
    expect(fourthLevelMenu.getAttribute("href")).toBe(
      "/admin/privacy/access-logs/download-approval",
    );
  });

  it("exposes full menu names and stable level markers for readable hierarchy", () => {
    render(<AdminShell>본문</AdminShell>);

    const systemMenu = screen.getByTestId("sidebar-menu-item-1");
    expect(systemMenu.getAttribute("data-menu-depth")).toBe("0");
    expect(systemMenu.getAttribute("title")).toBe("시스템 관리");

    fireEvent.click(
      screen.getByRole("button", { name: "역할권한관리 하위 메뉴 펼치기" }),
    );
    const privacyMenu = screen.getByTestId("sidebar-menu-item-3");
    expect(privacyMenu.getAttribute("data-menu-depth")).toBe("2");
    expect(privacyMenu.getAttribute("title")).toBe("개인정보 처리이력");
  });
});

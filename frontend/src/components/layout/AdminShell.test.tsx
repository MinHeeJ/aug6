import { fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AdminShell } from "./AdminShell";
import type { CurrentUser } from "../../api/apiClient";

const menus: CurrentUser["menus"] = [
  {
    menuId: 1,
    menuName: "교수업적평가",
    displayOrder: 1,
    children: [
      {
        menuId: 11,
        parentMenuId: 1,
        menuName: "연구자 프로필 관리",
        displayOrder: 1,
        children: [
          {
            menuId: 111,
            parentMenuId: 11,
            menuName: "연구자 프로필 목록",
            screenId: "SCR-RESEARCHER-PROFILE-LIST",
            url: "/researcher-profiles",
            displayOrder: 1,
            children: [],
          },
          {
            menuId: 112,
            parentMenuId: 11,
            menuName: "선행학위 미충족",
            screenId: "SCR-DEGREE-PREREQ-MISSING",
            url: "/admin/researcher-profiles/degree-prerequisite-missing",
            displayOrder: 2,
            children: [],
          },
        ],
      },
    ],
  },
  {
    menuId: 2,
    menuName: "파일·데이터 관리",
    displayOrder: 2,
    children: [
      {
        menuId: 21,
        parentMenuId: 2,
        menuName: "엑셀 관리",
        displayOrder: 1,
        children: [
          {
            menuId: 211,
            parentMenuId: 21,
            menuName: "업로드 양식 관리",
            screenId: "SCR-UPLOAD-TEMPLATE-MGMT",
            url: "/admin/excel-upload-templates",
            displayOrder: 1,
            children: [],
          },
        ],
      },
    ],
  },
];

vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: {
      userId: 1,
      loginId: "admin",
      name: "관리자",
      roles: ["R09"],
      menus,
    },
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  }),
}));

afterEach(() => {
  window.history.replaceState({}, "", "/admin/users");
});

describe("BASIC-38 AdminShell 메뉴 탐색", () => {
  it("대메뉴를 헤더 메뉴바에 노출하고 hover/focus로 하위 메뉴를 확인할 수 있다", () => {
    render(<AdminShell>본문</AdminShell>);

    const headerNav = screen.getByTestId("header-menu-bar");
    expect(
      within(headerNav).getByRole("button", { name: "교수업적평가" }),
    ).toBeInTheDocument();
    expect(
      within(headerNav).getByRole("button", { name: "파일·데이터 관리" }),
    ).toBeInTheDocument();

    fireEvent.mouseEnter(
      within(headerNav).getByRole("button", { name: "교수업적평가" }),
    );

    expect(
      screen.getByRole("link", { name: /연구자 프로필 목록/ }),
    ).toHaveAttribute("href", "/researcher-profiles");
    expect(
      screen.getByRole("link", { name: /선행학위 미충족/ }),
    ).toHaveAttribute(
      "href",
      "/admin/researcher-profiles/degree-prerequisite-missing",
    );
  });

  it("메뉴 검색 입력으로 실제 메뉴를 필터링하고 선택 시 해당 route로 이동한다", () => {
    render(<AdminShell>본문</AdminShell>);

    fireEvent.click(
      screen.getByRole("button", { name: /메뉴 또는 화면 검색/ }),
    );
    const input = screen.getByTestId("menu-search-input");
    fireEvent.change(input, { target: { value: "업로드" } });

    expect(input).toHaveValue("업로드");
    const result = screen.getByRole("link", { name: /업로드 양식 관리/ });
    expect(result).toHaveAttribute("href", "/admin/excel-upload-templates");

    fireEvent.click(result);
    expect(window.location.pathname).toBe("/admin/excel-upload-templates");
  });
});

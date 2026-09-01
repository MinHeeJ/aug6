import { fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { CurrentUser } from "../../api/apiClient";
import { AdminShell } from "./AdminShell";

const adminUser: CurrentUser = {
  userId: 1,
  loginId: "admin",
  name: "관리자",
  roles: ["R09"],
  menus: [
    {
      menuId: 10,
      menuName: "시스템 관리",
      displayOrder: 1,
      children: [
        {
          menuId: 11,
          parentMenuId: 10,
          menuName: "사용자·조직 관리",
          displayOrder: 1,
          children: [
            {
              menuId: 12,
              parentMenuId: 11,
              menuName: "사용자 관리",
              screenId: "SCR-USER-MGMT",
              url: "/admin/users",
              displayOrder: 1,
              children: [],
            },
          ],
        },
      ],
    },
    {
      menuId: 20,
      menuName: "파일·데이터 관리",
      displayOrder: 2,
      children: [
        {
          menuId: 21,
          parentMenuId: 20,
          menuName: "엑셀 관리",
          displayOrder: 1,
          children: [
            {
              menuId: 22,
              parentMenuId: 21,
              menuName: "업로드 양식 관리",
              screenId: "SCR-UPLOAD-TEMPLATE-MGMT",
              url: "/admin/excel-upload-templates",
              displayOrder: 1,
              children: [],
            },
          ],
        },
        {
          menuId: 30,
          parentMenuId: 20,
          menuName: "배치작업 관리",
          displayOrder: 2,
          children: [
            {
              menuId: 31,
              parentMenuId: 30,
              menuName: "배치 결과 조회",
              screenId: "SCR-BATCH-RESULT-MGMT",
              url: "/admin/batch-results",
              displayOrder: 1,
              children: [],
            },
          ],
        },
      ],
    },
  ],
};

vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: adminUser,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  }),
}));

afterEach(() => {
  window.history.replaceState({}, "", "/admin/users");
});

describe("SCR-COMMON-MENU-SEARCH", () => {
  it("allows keyword entry and filters only accessible menu leaves by name and menu path", () => {
    render(
      <AdminShell>
        <div>본문</div>
      </AdminShell>,
    );

    fireEvent.click(screen.getByTestId("header-menu-search-toggle"));

    const input = screen.getByTestId("header-menu-search-input");
    fireEvent.change(input, { target: { value: "배치" } });

    expect(input).toHaveValue("배치");
    const results = screen.getByTestId("header-menu-search-results");
    expect(
      within(results).getByRole("link", { name: /배치 결과 조회/ }),
    ).toHaveAttribute("href", "/admin/batch-results");
    expect(
      within(results).getByText(
        /파일·데이터 관리 > 배치작업 관리 > 배치 결과 조회/,
      ),
    ).toBeVisible();
    expect(screen.queryByText("급여 관리")).not.toBeInTheDocument();
  });

  it("navigates to the selected result route and closes inaccessible-menu-only results", () => {
    render(
      <AdminShell>
        <div>본문</div>
      </AdminShell>,
    );

    fireEvent.click(screen.getByTestId("header-menu-search-toggle"));
    fireEvent.change(screen.getByTestId("header-menu-search-input"), {
      target: { value: "엑셀" },
    });
    fireEvent.click(screen.getByRole("link", { name: /업로드 양식 관리/ }));

    expect(window.location.pathname).toBe("/admin/excel-upload-templates");
    expect(
      screen.queryByTestId("header-menu-search-results"),
    ).not.toBeInTheDocument();
  });
});

describe("SCR-COMMON-HEADER-NAV", () => {
  it("header top menu hover opens middle and leaf menu panel for accessible menus", () => {
    render(
      <AdminShell>
        <div>본문</div>
      </AdminShell>,
    );

    const headerNav = screen.getByTestId("common-header-nav");
    expect(headerNav).toBeInTheDocument();

    fireEvent.mouseEnter(screen.getByTestId("header-nav-top-20"));

    const panel = screen.getByTestId("header-nav-panel");
    expect(panel).toBeVisible();
    expect(within(panel).getByText("엑셀 관리")).toBeVisible();
    expect(
      within(panel).getByRole("link", { name: "업로드 양식 관리" }),
    ).toHaveAttribute("href", "/admin/excel-upload-templates");
  });

  it("header leaf click navigates inside the SPA without hardcoded sample ids", () => {
    render(
      <AdminShell>
        <div>본문</div>
      </AdminShell>,
    );

    fireEvent.mouseEnter(screen.getByTestId("header-nav-top-10"));
    fireEvent.click(
      within(screen.getByTestId("header-nav-panel")).getByRole("link", {
        name: "사용자 관리",
      }),
    );

    expect(window.location.pathname).toBe("/admin/users");
  });
});

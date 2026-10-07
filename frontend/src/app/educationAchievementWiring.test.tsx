import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppRouter } from "./router";
import { useAuth } from "./AuthProvider";
import type { CurrentUser } from "../api/apiClient";
import {
  ADMIN_ROUTES,
  canAccessAdminRoute,
  getLoginLandingPath,
} from "../pages/LoginPage";

vi.mock("./AuthProvider", () => ({ useAuth: vi.fn() }));

const routes = [
  {
    path: "/faculty/employment-rate-improvement-achievements",
    label: "취업률 제고 실적 관리",
    screenId: "SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT",
    testId: "employment-rate-improvement-page",
    api: "/api/business/employment-rate-improvements",
  },
  {
    path: "/faculty/course-offering-operation-achievements",
    label: "강좌 개설·운영 실적 관리",
    screenId: "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT",
    testId: "course-operations-page",
    api: "/api/business/course-operations",
  },
  {
    path: "/faculty/teaching-improvement-achievements",
    label: "강의개선 실적 관리",
    screenId: "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT",
    testId: "lecture-improvements-page",
    api: "/api/business/lecture-improvements",
  },
  {
    path: "/faculty/employment-rate-achievements",
    label: "취업률 실적 관리",
    screenId: "SCR-EMPLOYMENT-RATE-ACHIEVEMENT",
    testId: "employment-rate-achievement-page",
    api: "/api/business/employment-rate-achievements",
  },
];

const fetchMock = vi.fn();
function userFor(route = routes[0], roles = ["R01"]): CurrentUser {
  return {
    userId: 101,
    loginId: "faculty",
    name: "교원",
    roles,
    menus: [
      {
        menuId: 101,
        menuName: route.label,
        screenId: route.screenId,
        url: route.path,
        displayOrder: 1,
        children: [],
      },
    ],
  };
}
function session(user: CurrentUser) {
  vi.mocked(useAuth).mockReturnValue({
    status: "authenticated",
    user,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  });
}

beforeEach(() => {
  fetchMock.mockReset().mockImplementation(
    async () =>
      new Response(
        JSON.stringify({
          success: true,
          data: {
            achievements: [],
            page: 0,
            pageSize: 20,
            totalElements: 0,
            templates: [],
            histories: [],
          },
          meta: {},
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
  );
  vi.stubGlobal("fetch", fetchMock);
  window.history.replaceState({}, "", "/");
});
afterEach(() => {
  window.history.replaceState({}, "", "/");
  vi.unstubAllGlobals();
});

describe("education achievement shared wiring", () => {
  it.each(routes)(
    "session menu reaches $screenId and its real list client",
    async (route) => {
      session(userFor(route));
      render(<AppRouter />);
      fireEvent.click(
        screen.getByRole("button", { name: /메뉴 또는 화면 검색/ }),
      );
      fireEvent.change(screen.getByTestId("menu-search-input"), {
        target: { value: route.label },
      });
      const link = screen.getByRole("link", { name: new RegExp(route.label) });
      expect(link).toHaveAttribute("href", route.path);
      fireEvent.click(link);
      expect(window.location.pathname).toBe(route.path);
      await screen.findByTestId(route.testId);
      await waitFor(() =>
        expect(fetchMock).toHaveBeenCalledWith(
          `${route.api}?page=0&pageSize=20`,
          expect.objectContaining({ credentials: "include" }),
        ),
      );
      expect(ADMIN_ROUTES.filter((entry) => entry.path === route.path)).toEqual(
        [
          expect.objectContaining({
            screenId: route.screenId,
            label: route.label,
          }),
        ],
      );
    },
  );

  it.each(routes)(
    "blocks direct $screenId access without a session menu",
    (route) => {
      session({ ...userFor(), menus: [] });
      window.history.replaceState({}, "", route.path);
      render(<AppRouter />);
      expect(screen.getByText("권한이 없습니다")).toBeInTheDocument();
      expect(fetchMock).not.toHaveBeenCalled();
    },
  );

  it.each(routes.slice(0, 3))(
    "does not turn R07 menu access into CRUD admission for $screenId",
    (route) => {
      session(userFor(route, ["R07"]));
      window.history.replaceState({}, "", route.path);
      render(<AppRouter />);
      expect(screen.getByText("권한이 없습니다")).toBeInTheDocument();
      expect(fetchMock).not.toHaveBeenCalled();
    },
  );

  it("R07 reaches only the employment upload surface, not the single-record list", async () => {
    const route = routes[3];
    session(userFor(route, ["R07"]));
    window.history.replaceState({}, "", route.path);
    render(<AppRouter />);
    await screen.findByTestId("employment-upload-panel");
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    expect(
      fetchMock.mock.calls.some(([path]) =>
        String(path).startsWith(`${route.api}?`),
      ),
    ).toBe(false);
    expect(
      screen.queryByTestId("employment-records-tab"),
    ).not.toBeInTheDocument();
  });

  it.each(routes)(
    "keeps the requested R09 bypass on $screenId even without menus",
    async (route) => {
      const user = { ...userFor(route, ["R09"]), menus: [] };
      expect(canAccessAdminRoute(user, route.path)).toBe(true);
      session(user);
      window.history.replaceState({}, "", route.path);
      render(<AppRouter />);
      await screen.findByTestId(route.testId);
      await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    },
  );

  it("lands a faculty login on the first authorized nested session menu", async () => {
    const user = userFor(routes[1]);
    user.menus = [
      {
        menuId: 100,
        menuName: "교육영역",
        displayOrder: 0,
        children: user.menus,
      },
    ];
    session(user);
    window.history.replaceState({}, "", "/login");
    render(<AppRouter />);
    await screen.findByTestId(routes[1].testId);
    expect(window.location.pathname).toBe(routes[1].path);
  });

  it("preserves administrator landing and safely handles empty or template menus", () => {
    const user = userFor();
    user.menus[0].url = "/admin/users";
    user.roles = ["R09"];
    expect(getLoginLandingPath(user)).toBe("/admin/users");
    expect(getLoginLandingPath({ ...user, menus: [] })).toBe("/");
    user.menus[0].url = "/researcher-profiles/{employeeNo}";
    expect(getLoginLandingPath(user)).toBe("/");
    expect(canAccessAdminRoute(null, routes[0].path)).toBe(false);
  });
});

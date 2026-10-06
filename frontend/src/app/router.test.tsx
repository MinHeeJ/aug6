import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest, type CurrentUser } from "../api/apiClient";
import { FACULTY_ACHIEVEMENT_ROUTES } from "../pages/LoginPage";
import { AppRouter } from "./router";

const session = vi.hoisted(() => ({ user: null as CurrentUser | null }));
vi.mock("./AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: session.user,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
  }),
}));
vi.mock("../api/apiClient", async () => {
  const actual =
    await vi.importActual<typeof import("../api/apiClient")>(
      "../api/apiClient",
    );
  return { ...actual, apiRequest: vi.fn() };
});

const destinations = [
  {
    path: "/faculty/employment-rate-improvement-achievements",
    label: "취업률 제고 실적 관리",
    api: "/api/business/employment-rate-improvements",
  },
  {
    path: "/faculty/course-offering-operation-achievements",
    label: "강좌 개설·운영 실적 관리",
    api: "/api/business/course-operations",
  },
  {
    path: "/faculty/teaching-improvement-achievements",
    label: "강의개선 실적 관리",
    api: "/api/business/lecture-improvements",
  },
  {
    path: "/faculty/employment-rate-achievements",
    label: "취업률 실적 관리",
    api: "/api/business/employment-rate-achievements",
  },
];

function user(roles: string[], withMenus = true): CurrentUser {
  return {
    userId: 101,
    loginId: "route-test",
    name: "교원",
    roles,
    menus: withMenus
      ? FACULTY_ACHIEVEMENT_ROUTES.map((route, index) => ({
          menuId: index + 1,
          menuName: route.label,
          screenId: route.screenId,
          url: route.path,
          displayOrder: index,
          children: [],
        }))
      : [],
  };
}

beforeEach(() => {
  session.user = user(["R01"]);
  window.history.replaceState({}, "", "/");
  vi.mocked(apiRequest).mockReset();
  vi.mocked(apiRequest).mockImplementation(async (path) => ({
    success: true,
    meta: {},
    data: path.endsWith("/histories")
      ? []
      : {
          achievements: [],
          page: 0,
          pageSize: 20,
          totalElements: 0,
          managementItems: [],
          academicYears: [],
          semesters: [],
          canCreate: true,
          canUpdate: true,
        },
  }));
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  window.history.replaceState({}, "", "/");
});

describe("교육영역 canonical route 연결", () => {
  it.each(destinations)(
    "session menu reaches $path and its real API-backed page",
    async (destination) => {
      render(<AppRouter />);
      expect(
        screen.getByRole("heading", { name: "Dashboard" }),
      ).toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: "모바일 메뉴" }));
      fireEvent.click(
        screen.getAllByRole("link", { name: destination.label })[0],
      );
      expect(window.location.pathname).toBe(destination.path);
      expect(
        await screen.findByRole("heading", { name: destination.label }),
      ).toBeInTheDocument();
      await waitFor(() =>
        expect(
          vi
            .mocked(apiRequest)
            .mock.calls.some(([path]) =>
              path.startsWith(`${destination.api}?`),
            ),
        ).toBe(true),
      );
      expect(
        screen.queryByText("보호 route placeholder"),
      ).not.toBeInTheDocument();
    },
  );

  it.each(destinations)(
    "denies $path without its session menu before any API request",
    (destination) => {
      session.user = user(["R01"], false);
      window.history.replaceState({}, "", destination.path);
      render(<AppRouter />);
      expect(
        screen.getByText(`${destination.label} 화면 접근 권한이 없습니다.`),
      ).toBeInTheDocument();
      expect(apiRequest).not.toHaveBeenCalled();
    },
  );

  it.each(destinations)(
    "does not treat R09 menu bypass as a business role for $path",
    (destination) => {
      session.user = user(["R09"]);
      window.history.replaceState({}, "", destination.path);
      render(<AppRouter />);
      expect(
        screen.getByText(`${destination.label} 화면 접근 권한이 없습니다.`),
      ).toBeInTheDocument();
      expect(apiRequest).not.toHaveBeenCalled();
    },
  );

  it.each(destinations.slice(0, 3))(
    "denies R07 access to $path",
    (destination) => {
      session.user = user(["R07"]);
      window.history.replaceState({}, "", destination.path);
      render(<AppRouter />);
      expect(
        screen.getByText(`${destination.label} 화면 접근 권한이 없습니다.`),
      ).toBeInTheDocument();
      expect(apiRequest).not.toHaveBeenCalled();
    },
  );

  it("allows R07 through the existing menu to FR032 Excel without requesting individual CRUD", async () => {
    session.user = user(["R07"]);
    render(<AppRouter />);
    fireEvent.click(screen.getByRole("button", { name: "모바일 메뉴" }));
    fireEvent.click(
      screen.getAllByRole("link", { name: "취업률 실적 관리" })[0],
    );
    expect(
      await screen.findByRole("heading", { name: "취업률 실적 관리" }),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/employment-rate-achievements/excel-uploads/histories",
      ),
    );
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([path]) =>
          path.startsWith("/api/business/employment-rate-achievements?"),
        ),
    ).toBe(false);
    expect(
      screen.queryByRole("tab", { name: "개별 실적" }),
    ).not.toBeInTheDocument();
  });
});

import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppRouter } from "./router";
import { useAuth } from "./AuthProvider";
import type { CurrentUser } from "../api/apiClient";
import { ADMIN_ROUTES, canAccessAdminRoute } from "../pages/LoginPage";

vi.mock("./AuthProvider", () => ({ useAuth: vi.fn() }));

const features = [
  {
    path: "/faculty/employment-rate-improvement-achievements",
    alias: "/faculty/education/employment-rate-improvements",
    screenId: "SCR-EMPLOYMENT-RATE-IMPROVEMENTS",
    label: "취업률 제고 실적 관리",
    api: "/api/business/employment-rate-improvements",
  },
  {
    path: "/faculty/course-offering-operation-achievements",
    alias: "/faculty/education/course-operations",
    screenId: "SCR-COURSE-OPERATIONS",
    label: "강좌 개설·운영 실적 관리",
    api: "/api/business/course-operations",
  },
  {
    path: "/faculty/teaching-improvement-achievements",
    alias: "/faculty/education/lecture-improvements",
    screenId: "SCR-LECTURE-IMPROVEMENTS",
    label: "강의개선 실적 관리",
    api: "/api/business/lecture-improvements",
  },
  {
    path: "/faculty/employment-rate-achievements",
    alias: "/faculty/education/employment-rate-achievements",
    screenId: "SCR-EMPLOYMENT-RATE-ACHIEVEMENTS",
    label: "취업률 실적 관리",
    api: "/api/business/employment-rate-achievements",
  },
];
const fetchMock = vi.fn();

function session(
  feature: (typeof features)[number],
  roles = ["R01"],
  menu = true,
) {
  const user: CurrentUser = {
    userId: 101,
    loginId: "faculty",
    name: "교원",
    roles,
    menus: menu
      ? [
          {
            menuId: 501,
            menuName: feature.label,
            screenId: feature.screenId,
            url: feature.path,
            displayOrder: 1,
            children: [],
          },
        ]
      : [],
  };
  vi.mocked(useAuth).mockReturnValue({
    status: "authenticated",
    user,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  });
  return user;
}

beforeEach(() => {
  window.history.replaceState({}, "", "/");
  fetchMock.mockReset();
  fetchMock.mockImplementation(async () => ({
    ok: true,
    headers: { get: () => "application/json" },
    json: async () => ({
      success: true,
      data: {
        achievements: [],
        managementItems: [],
        page: 0,
        pageSize: 20,
        totalElements: 0,
      },
      meta: { requestId: "wiring-test" },
    }),
  }));
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.clearAllMocks();
});

describe.each(features)("$screenId route wiring", (feature) => {
  it("reaches the real screen from the session navigation and calls the shared API client", async () => {
    session(feature);
    render(<AppRouter />);
    fireEvent.click(
      screen.getByRole("button", { name: "모바일 메뉴", exact: true }),
    );
    fireEvent.click(
      screen.getByRole("link", { name: feature.label, exact: true }),
    );
    await screen.findByRole("heading", { name: feature.label, exact: true });
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    expect(window.location.pathname).toBe(feature.path);
    expect(fetchMock.mock.calls[0][0]).toBe(
      `${feature.api}?page=0&pageSize=20`,
    );
    expect(fetchMock.mock.calls[0][1]).toMatchObject({
      credentials: "include",
    });
  });

  it.each(["R01", "R02", "R04"])(
    "allows %s through the canonical menu and alias",
    async (role) => {
      session(feature, [role]);
      window.history.replaceState({}, "", feature.alias);
      render(<AppRouter />);
      await screen.findByRole("heading", { name: feature.label, exact: true });
      await waitFor(() => expect(fetchMock).toHaveBeenCalled());
      expect(fetchMock.mock.calls[0][0]).toContain(feature.api);
    },
  );

  it.each(["primary", "alias"])(
    "denies missing menu at %s before any feature request",
    (entry) => {
      session(feature, ["R01"], false);
      window.history.replaceState(
        {},
        "",
        entry === "primary" ? feature.path : feature.alias,
      );
      render(<AppRouter />);
      expect(screen.getByText("권한이 없습니다")).toBeInTheDocument();
      expect(fetchMock).not.toHaveBeenCalled();
    },
  );

  it("does not grant a business role merely because a menu is present", () => {
    session(feature, ["R03"]);
    window.history.replaceState({}, "", feature.alias);
    render(<AppRouter />);
    expect(screen.getByText("권한이 없습니다")).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("preserves the requested R09 route override without a menu", async () => {
    session(feature, ["R09"], false);
    window.history.replaceState({}, "", feature.alias);
    render(<AppRouter />);
    await screen.findByRole("heading", { name: feature.label, exact: true });
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
  });

  it("registers exactly one primary navigation entry and no alias menu", () => {
    expect(
      ADMIN_ROUTES.filter((route) => route.screenId === feature.screenId),
    ).toHaveLength(1);
    expect(
      ADMIN_ROUTES.find((route) => route.path === feature.alias),
    ).toBeUndefined();
    expect(canAccessAdminRoute(null, feature.alias)).toBe(false);
  });

  it("keeps R07 on the upload/bulk surface only", async () => {
    session(feature, ["R07"]);
    window.history.replaceState({}, "", feature.alias);
    render(<AppRouter />);
    if (feature.screenId === "SCR-EMPLOYMENT-RATE-ACHIEVEMENTS") {
      await screen.findByRole("heading", { name: feature.label, exact: true });
      expect(screen.getByTestId("excel-tab")).toBeInTheDocument();
      expect(screen.getByTestId("bulk-tab")).toBeInTheDocument();
      expect(screen.queryByTestId("individual-tab")).not.toBeInTheDocument();
    } else {
      expect(screen.getByText("권한이 없습니다")).toBeInTheDocument();
    }
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

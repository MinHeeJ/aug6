import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppRouter } from "./router";
import { apiRequest, type CurrentUser } from "../api/apiClient";
import { ADMIN_ROUTES } from "../pages/LoginPage";

const session = vi.hoisted(() => ({ user: null as CurrentUser | null }));
vi.mock("../api/apiClient", async () => {
  const actual =
    await vi.importActual<typeof import("../api/apiClient")>(
      "../api/apiClient",
    );
  return { ...actual, apiRequest: vi.fn() };
});
vi.mock("./AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: session.user,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  }),
}));

const screens = [
  {
    resource: "employment-rate-improvements",
    label: "취업률 제고 실적 관리",
    screenId: "SCR-EMPLOYMENT-RATE-IMPROVEMENTS",
    testId: "employment-improvements-page",
  },
  {
    resource: "course-operations",
    label: "강좌 개설·운영 실적 관리",
    screenId: "SCR-COURSE-OPERATIONS",
    testId: "course-operations-page",
  },
  {
    resource: "lecture-improvements",
    label: "강의개선 실적 관리",
    screenId: "SCR-LECTURE-IMPROVEMENTS",
    testId: "lecture-improvements-page",
  },
  {
    resource: "employment-rate-achievements",
    label: "취업률 실적 관리",
    screenId: "SCR-EMPLOYMENT-RATE-ACHIEVEMENTS",
    testId: "employment-rate-page",
  },
];

function user(roles: string[], withMenus = true): CurrentUser {
  return {
    userId: 101,
    loginId: "faculty",
    name: "교원",
    roles,
    menus: withMenus
      ? [
          {
            menuId: 80,
            menuName: "교육영역",
            displayOrder: 1,
            children: screens.map((entry, index) => ({
              menuId: 81 + index,
              menuName: entry.label,
              screenId: entry.screenId,
              url: `/faculty/education/${entry.resource}`,
              displayOrder: index,
              children: [],
            })),
          },
        ]
      : [],
  };
}

beforeEach(() => {
  vi.clearAllMocks();
  session.user = user(["R01"]);
  vi.mocked(apiRequest).mockResolvedValue({
    success: true,
    data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
    meta: {},
  });
});
afterEach(() => window.history.replaceState({}, "", "/"));

describe.each(screens)("$screenId navigation", (entry) => {
  const path = `/faculty/education/${entry.resource}`;

  it("registers one matching screen and reaches the real API-backed page from session menu search", async () => {
    expect(ADMIN_ROUTES.filter((route) => route.path === path)).toEqual([
      {
        path,
        label: entry.label,
        screenId: entry.screenId,
        menuPath: `업적 입력 관리 > 교육영역 > ${entry.label}`,
      },
    ]);
    window.history.replaceState({}, "", "/");
    render(<AppRouter />);
    fireEvent.click(
      screen.getByRole("button", { name: /메뉴 또는 화면 검색/ }),
    );
    fireEvent.change(screen.getByTestId("menu-search-input"), {
      target: { value: entry.label },
    });
    const link = screen.getByRole("link", { name: new RegExp(entry.label) });
    expect(link).toHaveAttribute("href", path);
    fireEvent.click(link);
    expect(await screen.findByTestId(entry.testId)).toBeVisible();
    expect(window.location.pathname).toBe(path);
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        expect.stringMatching(`^/api/business/${entry.resource}\\?`),
      ),
    );
  });

  it.each(["R02", "R04"])(
    "allows %s readers through their assigned menu",
    async (role) => {
      session.user = user([role]);
      window.history.replaceState({}, "", path);
      render(<AppRouter />);
      expect(await screen.findByTestId(entry.testId)).toBeVisible();
      await waitFor(() => expect(apiRequest).toHaveBeenCalled());
    },
  );

  it("blocks an unrelated role even if a menu is supplied, without a feature API request", () => {
    session.user = user(["R03"]);
    window.history.replaceState({}, "", path);
    render(<AppRouter />);
    expect(
      screen.getByText(`${entry.label} 화면 접근 권한이 없습니다.`),
    ).toBeVisible();
    expect(screen.queryByTestId(entry.testId)).not.toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("requires an assigned menu for a business role", () => {
    session.user = user(["R01"], false);
    window.history.replaceState({}, "", path);
    render(<AppRouter />);
    expect(
      screen.getByText(`${entry.label} 화면 접근 권한이 없습니다.`),
    ).toBeVisible();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("preserves the administrator override even without a menu", async () => {
    session.user = user(["R09"], false);
    window.history.replaceState({}, "", path);
    render(<AppRouter />);
    expect(await screen.findByTestId(entry.testId)).toBeVisible();
    await waitFor(() => expect(apiRequest).toHaveBeenCalled());
  });

  if (entry.resource !== "employment-rate-achievements") {
    it("blocks an Excel operator from single-achievement screens", () => {
      session.user = user(["R07"]);
      window.history.replaceState({}, "", path);
      render(<AppRouter />);
      expect(screen.queryByTestId(entry.testId)).not.toBeInTheDocument();
      expect(apiRequest).not.toHaveBeenCalled();
    });
  }
});

it("opens the R07 Excel entry without calling the forbidden collection or detail APIs", async () => {
  session.user = user(["R07"]);
  window.history.replaceState(
    {},
    "",
    "/faculty/education/employment-rate-achievements",
  );
  render(<AppRouter />);
  expect(screen.getByTestId("excel-panel")).toBeVisible();
  expect(screen.queryByTestId("records-tab")).not.toBeInTheDocument();
  expect(apiRequest).not.toHaveBeenCalled();
  vi.mocked(apiRequest).mockResolvedValue({
    success: true,
    data: [],
    meta: {},
  });
  fireEvent.click(screen.getByTestId("histories-button"));
  await waitFor(() =>
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/excel-uploads/histories",
    ),
  );
});

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest, type CurrentUser } from "../api/apiClient";
import { ADMIN_ROUTES } from "../pages/LoginPage";
import { useAuth } from "./AuthProvider";
import { AppRouter } from "./router";

vi.mock("../api/apiClient", async () => {
  const actual =
    await vi.importActual<typeof import("../api/apiClient")>(
      "../api/apiClient",
    );
  return { ...actual, apiRequest: vi.fn() };
});
vi.mock("./AuthProvider", () => ({ useAuth: vi.fn() }));

const screens = [
  {
    path: "/faculty/education/employment-rate-improvements",
    api: "/api/business/employment-rate-improvements",
    label: "취업률 제고 실적 관리",
    screenId: "SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT",
  },
  {
    path: "/faculty/education/course-operations",
    api: "/api/business/course-operations",
    label: "강좌 개설·운영 실적 관리",
    screenId: "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT",
  },
  {
    path: "/faculty/education/lecture-improvements",
    api: "/api/business/lecture-improvements",
    label: "강의개선 실적 관리",
    screenId: "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT",
  },
  {
    path: "/faculty/education/employment-rate-achievements",
    api: "/api/business/employment-rate-achievements",
    label: "취업률 실적 관리",
    screenId: "SCR-EMPLOYMENT-RATE-ACHIEVEMENT",
  },
];

function authenticate(role: string, withMenus = true) {
  const user: CurrentUser = {
    userId: 101,
    loginId: "faculty",
    employeeNo: "E0101",
    name: "교원",
    roles: [role],
    menus: withMenus
      ? screens.map((item, index) => ({
          menuId: index + 1,
          menuName: item.label,
          screenId: item.screenId,
          url: item.path,
          displayOrder: index + 1,
          children: [],
        }))
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
}

beforeEach(() => {
  vi.mocked(apiRequest).mockReset();
  vi.mocked(apiRequest).mockResolvedValue({
    success: true,
    data: {
      achievements: [],
      page: 0,
      pageSize: 20,
      totalElements: 0,
      managementItems: [],
      managementItemCodes: [],
      semesters: [],
      canCreate: false,
      canUpdate: false,
    },
    meta: {},
  } as never);
  window.history.replaceState({}, "", "/");
  authenticate("R01");
});

describe("교육영역 화면 연결", () => {
  for (const item of screens) {
    it(`${item.label}: session menu navigation reaches the real screen and its API`, async () => {
      expect(
        ADMIN_ROUTES.filter((route) => route.path === item.path),
      ).toHaveLength(1);
      expect(
        ADMIN_ROUTES.find((route) => route.path === item.path)?.screenId,
      ).toBe(item.screenId);
      render(<AppRouter />);
      fireEvent.click(
        screen.getByRole("button", { name: "모바일 메뉴", exact: true }),
      );
      fireEvent.click(
        screen.getByRole("link", { name: item.label, exact: true }),
      );
      await waitFor(() => expect(window.location.pathname).toBe(item.path));
      expect(
        await screen.findByRole("heading", { name: item.label, exact: true }),
      ).toBeInTheDocument();
      await waitFor(() =>
        expect(
          vi
            .mocked(apiRequest)
            .mock.calls.some(([path]) => path.startsWith(`${item.api}?`)),
        ).toBe(true),
      );
    });

    it(`${item.label}: a business role without the session menu cannot reach the page`, () => {
      authenticate("R01", false);
      window.history.replaceState({}, "", item.path);
      render(<AppRouter />);
      expect(screen.getByText("권한이 없습니다")).toBeInTheDocument();
      expect(apiRequest).not.toHaveBeenCalled();
    });

    it(`${item.label}: an unrelated role cannot use a granted menu to bypass business admission`, () => {
      authenticate("R08");
      window.history.replaceState({}, "", item.path);
      render(<AppRouter />);
      expect(
        screen.queryByRole("heading", { name: item.label, exact: true }),
      ).not.toBeInTheDocument();
      expect(apiRequest).not.toHaveBeenCalled();
    });

    for (const role of ["R02", "R04", "R09"]) {
      it(`${item.label}: ${role} can reach the registered screen`, async () => {
        authenticate(role);
        window.history.replaceState({}, "", item.path);
        render(<AppRouter />);
        expect(
          await screen.findByRole("heading", { name: item.label, exact: true }),
        ).toBeInTheDocument();
        await waitFor(() => expect(apiRequest).toHaveBeenCalled());
      });
    }
  }

  it("R07 enters the employment Excel screen without calling the individual list", async () => {
    authenticate("R07");
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: [],
      meta: {},
    });
    window.history.replaceState({}, "", screens[3].path);
    render(<AppRouter />);
    expect(
      await screen.findByTestId("employment-rate-page"),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/employment-rate-achievements/excel-uploads/histories",
      ),
    );
    expect(screen.queryByTestId("individual-tab")).not.toBeInTheDocument();
    expect(
      vi.mocked(apiRequest).mock.calls.some(([path]) => path.includes("?")),
    ).toBe(false);
  });

  it("client-side navigation rechecks menu permission when leaving an authorized page", async () => {
    authenticate("R01");
    window.history.replaceState({}, "", screens[0].path);
    render(<AppRouter />);
    await screen.findByRole("heading", { name: screens[0].label, exact: true });
    authenticate("R01", false);
    vi.mocked(apiRequest).mockClear();
    window.history.pushState({}, "", screens[2].path);
    fireEvent(window, new PopStateEvent("popstate"));
    expect(screen.getByText("권한이 없습니다")).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });
});

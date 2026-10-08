import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppRouter } from "../../app/router";
import { apiRequest } from "../../api/apiClient";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: {
      userId: 101,
      loginId: "faculty",
      name: "교원",
      roles: ["R01"],
      menus: [
        {
          menuId: 81,
          menuName: "취업률 제고 실적 관리",
          displayOrder: 1,
          screenId: "SCR-EMPLOYMENT-RATE-IMPROVEMENTS",
          children: [],
          url: "/faculty/education/employment-rate-improvements",
        },
      ],
    },
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  }),
}));

afterEach(() => window.history.replaceState({}, "", "/"));

describe("취업률 제고 실적 메뉴 연결", () => {
  it("uses the real session menu search to reach the registered product screen", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: {
        achievements: [],
        page: 0,
        pageSize: 20,
        totalElements: 0,
      },
      meta: {},
    });
    window.history.replaceState({}, "", "/");
    render(<AppRouter />);
    fireEvent.click(
      screen.getByRole("button", { name: /메뉴 또는 화면 검색/ }),
    );
    fireEvent.change(screen.getByTestId("menu-search-input"), {
      target: { value: "취업률 제고" },
    });
    const entry = screen.getByRole("link", { name: /취업률 제고 실적 관리/ });
    expect(entry).toHaveAttribute(
      "href",
      "/faculty/education/employment-rate-improvements",
    );
    fireEvent.click(entry);
    expect(
      await screen.findByTestId("employment-improvements-page"),
    ).toBeVisible();
    expect(window.location.pathname).toBe(
      "/faculty/education/employment-rate-improvements",
    );
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/employment-rate-improvements?page=0&pageSize=20",
    );
  });
});

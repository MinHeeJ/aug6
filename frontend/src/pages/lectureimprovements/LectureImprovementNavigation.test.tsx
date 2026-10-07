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
          menuId: 101,
          menuName: "강의개선 실적 관리",
          displayOrder: 1,
          screenId: "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT",
          url: "/faculty/teaching-improvement-achievements",
          children: [],
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

describe("강의개선 정본 경로 등록", () => {
  it("authorized existing menu search navigates to the real API-backed page", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], totalElements: 0 },
      meta: {},
    });
    window.history.replaceState({}, "", "/");
    render(<AppRouter />);
    fireEvent.click(
      screen.getByRole("button", { name: /메뉴 또는 화면 검색/ }),
    );
    fireEvent.change(screen.getByTestId("menu-search-input"), {
      target: { value: "강의개선" },
    });
    const link = screen.getByRole("link", { name: /강의개선 실적 관리/ });
    expect(link).toHaveAttribute(
      "href",
      "/faculty/teaching-improvement-achievements",
    );
    fireEvent.click(link);
    expect(window.location.pathname).toBe(
      "/faculty/teaching-improvement-achievements",
    );
    await screen.findByTestId("lecture-improvements-page");
    await screen.findByText("등록된 실적이 없습니다.");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/lecture-improvements?page=0&pageSize=20",
    );
  });
});

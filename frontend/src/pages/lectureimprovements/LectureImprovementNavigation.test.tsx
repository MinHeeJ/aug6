import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest, type CurrentUser } from "../../api/apiClient";
import { AppRouter } from "../../app/router";

const session = vi.hoisted(() => ({
  status: "authenticated",
  user: null as CurrentUser | null,
}));
vi.mock("../../app/AuthProvider", () => ({ useAuth: () => session }));
vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

afterEach(() => {
  cleanup();
  window.history.replaceState({}, "", "/");
  vi.resetAllMocks();
});

describe("강의개선 메뉴 진입", () => {
  it("reaches the registered screen from the real session menu and requests its list", async () => {
    const url = "/faculty/education/lecture-improvements";
    session.user = {
      userId: 101,
      loginId: "faculty",
      name: "교원",
      roles: ["R01"],
      menus: [
        {
          menuId: 101,
          menuName: "강의개선 실적 관리",
          screenId: "SCR-LECTURE-IMPROVEMENTS",
          url,
          displayOrder: 1,
          children: [],
        },
      ],
    };
    window.history.replaceState({}, "", "/");
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: {},
    });
    render(<AppRouter />);
    // Existing mobile shell hides the sidebar until its ordinary menu button opens it.
    fireEvent.click(screen.getByRole("button", { name: "모바일 메뉴" }));
    fireEvent.click(screen.getByRole("link", { name: "강의개선 실적 관리" }));
    expect(
      await screen.findByRole("heading", { name: "강의개선 실적 관리" }),
    ).toBeInTheDocument();
    expect(window.location.pathname).toBe(url);
    expect(screen.getByTestId("lecture-improvements-page")).toHaveAttribute(
      "data-screen-id",
      "SCR-LECTURE-IMPROVEMENTS",
    );
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/lecture-improvements?page=0&pageSize=20",
    );
  });
});

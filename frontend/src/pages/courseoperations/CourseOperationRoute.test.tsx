import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";
import { AppRouter } from "../../app/router";

const session = vi.hoisted(() => ({ roles: ["R01"] }));
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: {
      userId: 101,
      loginId: "faculty",
      name: "교원",
      roles: session.roles,
      menus: [
        {
          menuId: 701,
          menuName: "강좌 운영",
          url: "/faculty/course-offering-operation-achievements",
          children: [],
        },
      ],
    },
    logout: vi.fn(),
    refresh: vi.fn(),
    login: vi.fn(),
  }),
}));
vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

describe("course operation registered direct route", () => {
  beforeEach(() => {
    session.roles = ["R01"];
    vi.mocked(apiRequest).mockReset();
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { achievements: [], managementItems: [], totalElements: 0 },
      meta: {},
    });
    window.history.replaceState(
      {},
      "",
      "/faculty/course-offering-operation-achievements",
    );
  });

  it("authorized direct entry renders the real page inside the existing shell", async () => {
    render(<AppRouter />);
    expect(
      await screen.findByRole("heading", { name: "강좌 개설·운영 실적 관리" }),
    ).toBeInTheDocument();
    await screen.findByText("조회된 실적이 없습니다");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/course-operations?page=0&pageSize=20",
    );
  });

  it("unauthorized direct entry does not load business data", async () => {
    session.roles = ["R07"];
    render(<AppRouter />);
    expect(
      await screen.findByText("강좌 운영 실적 접근 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });
});

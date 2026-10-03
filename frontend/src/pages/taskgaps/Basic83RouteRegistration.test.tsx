import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { CurrentUser } from "../../api/apiClient";

vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: {
      userId: 101,
      loginId: "faculty",
      name: "교원",
      roles: ["R01"],
      menus: [],
    } satisfies CurrentUser,
  }),
}));

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );

  return {
    ...actual,
    apiRequest: vi.fn().mockResolvedValue({
      success: true,
      data: {
        achievements: [],
        page: 0,
        pageSize: 20,
        totalElements: 0,
      },
      meta: {},
    }),
  };
});

import { AppRouter } from "../../app/router";

afterEach(() => {
  window.history.replaceState({}, "", "/");
});

describe("BASIC-83 route registration", () => {
  it.each([
    {
      heading: "취업률 제고 실적 관리",
      route: "/faculty/employment-rate-improvement-achievements",
    },
    {
      heading: "강좌 개설·운영 실적 관리",
      route: "/faculty/course-offering-operation-achievements",
    },
    {
      heading: "강의개선 실적 관리",
      route: "/faculty/teaching-improvement-achievements",
    },
    {
      heading: "취업률 실적 관리",
      route: "/faculty/employment-rate-achievements",
    },
  ])(
    "allows an authorized faculty member to reach $route",
    ({ heading, route }) => {
      window.history.replaceState({}, "", route);

      render(<AppRouter />);

      expect(
        screen.getByRole("heading", { name: heading }),
      ).toBeInTheDocument();
    },
  );
});

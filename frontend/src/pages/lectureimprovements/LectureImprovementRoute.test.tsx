import { render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AppRouter } from "../../app/router";

vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: {
      userId: 101,
      loginId: "faculty",
      name: "교원",
      roles: ["R01"],
      menus: [],
    },
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
        totalElements: 0,
        academicYears: [],
        semesters: [],
        managementItems: [],
      },
      meta: {},
    }),
  };
});

afterEach(() => window.history.replaceState({}, "", "/"));

it("the intended faculty route reaches the API-backed teaching-improvement page in the existing shell", async () => {
  window.history.replaceState(
    {},
    "",
    "/faculty/teaching-improvement-achievements",
  );
  render(<AppRouter />);
  await screen.findByRole("heading", { name: "강의개선 실적 관리" });
  expect(await screen.findByText("총 0건")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "신규 입력" })).toBeInTheDocument();
});

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppRouter } from "./router";

const session = vi.hoisted(() => ({ roles: ["R01"] }));
const routes = [
  [
    "/faculty/employment-rate-improvement-achievements",
    "취업률 제고 실적 관리",
    "employment-rate-improvements",
  ],
  [
    "/faculty/course-offering-operation-achievements",
    "강좌 개설·운영 실적 관리",
    "course-operations",
  ],
  [
    "/faculty/teaching-improvement-achievements",
    "강의개선 실적 관리",
    "lecture-improvements",
  ],
  [
    "/faculty/employment-rate-achievements",
    "취업률 실적 관리",
    "employment-rate-achievements",
  ],
] as const;

vi.mock("./AuthProvider", () => ({
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
          menuName: "취업률 제고 실적 관리",
          url: "/faculty/employment-rate-improvement-achievements",
          children: [],
        },
        {
          menuId: 702,
          menuName: "강좌 개설·운영 실적 관리",
          url: "/faculty/course-offering-operation-achievements",
          children: [],
        },
        {
          menuId: 703,
          menuName: "강의개선 실적 관리",
          url: "/faculty/teaching-improvement-achievements",
          children: [],
        },
        {
          menuId: 704,
          menuName: "취업률 실적 관리",
          url: "/faculty/employment-rate-achievements",
          children: [],
        },
      ],
    },
    logout: vi.fn(),
    refresh: vi.fn(),
    login: vi.fn(),
  }),
}));

beforeEach(() => {
  session.roles = ["R01"];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(async (path: string) => ({
      ok: true,
      status: 200,
      headers: { get: () => "application/json" },
      json: async () => ({
        success: true,
        meta: {},
        data: path.includes("/histories")
          ? []
          : {
              achievements: [],
              managementItems: [],
              academicYears: [],
              semesters: [],
              totalElements: 0,
              page: 0,
              pageSize: 20,
            },
      }),
    })),
  );
});
afterEach(() => {
  window.history.replaceState({}, "", "/");
  vi.unstubAllGlobals();
});

describe.each(routes)("registered screen %s", (path, title, resource) => {
  it.each(["R01", "R02", "R04", "R09"])(
    "direct entry by %s reaches the real API-backed page",
    async (role) => {
      session.roles = [role];
      window.history.replaceState({}, "", path);
      render(<AppRouter />);
      expect(
        await screen.findByRole("heading", { name: title }),
      ).toBeInTheDocument();
      await waitFor(() =>
        expect(fetch).toHaveBeenCalledWith(
          expect.stringContaining(`/api/business/${resource}?`),
          expect.objectContaining({ credentials: "include" }),
        ),
      );
    },
  );

  it("existing server-provided navigation reaches the destination", async () => {
    window.history.replaceState({}, "", "/");
    render(<AppRouter />);
    fireEvent.click(screen.getByRole("button", { name: "모바일 메뉴" }));
    fireEvent.click(screen.getByRole("link", { name: title }));
    expect(
      await screen.findByRole("heading", { name: title }),
    ).toBeInTheDocument();
    expect(window.location.pathname).toBe(path);
  });

  it("an unrelated role cannot load business data on direct entry", async () => {
    session.roles = ["R08"];
    window.history.replaceState({}, "", path);
    render(<AppRouter />);
    expect(
      (await screen.findAllByText(/권한이 없습니다/)).length,
    ).toBeGreaterThan(0);
    expect(fetch).not.toHaveBeenCalled();
  });
});

it("R07 entry reaches only the Excel panel without calling individual list", async () => {
  session.roles = ["R07"];
  window.history.replaceState({}, "", "/faculty/employment-rate-achievements");
  render(<AppRouter />);
  expect(
    await screen.findByRole("tab", { name: "Excel 등록·일괄 결과" }),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole("tab", { name: "개별 실적" }),
  ).not.toBeInTheDocument();
  await waitFor(() =>
    expect(fetch).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/excel-uploads/histories",
      expect.anything(),
    ),
  );
  expect(
    vi
      .mocked(fetch)
      .mock.calls.every(([path]) => String(path).includes("/excel-uploads/")),
  ).toBe(true);
});

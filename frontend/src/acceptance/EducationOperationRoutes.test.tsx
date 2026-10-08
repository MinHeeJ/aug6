import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppRouter } from "../app/router";

// Preserve the repository's route-test setup; mock only the authenticated session and network boundary.
// The router, all four feature modules and shared transport remain real to detect import/wiring regressions.
const session = vi.hoisted(() => ({ roles: ["R01"] }));
vi.mock("../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user: {
      userId: 101,
      loginId: "faculty",
      name: "교원",
      roles: session.roles,
      menus: [],
    },
    logout: vi.fn(),
    refresh: vi.fn(),
    login: vi.fn(),
  }),
}));

const screens = [
  {
    route: "/faculty/employment-rate-improvement-achievements",
    title: "취업률 제고 실적 관리",
    api: "/api/business/employment-rate-improvements",
    save: "improvement-save",
    pageSize: "improvement-page-size",
  },
  {
    route: "/faculty/course-offering-operation-achievements",
    title: "강좌 개설·운영 실적 관리",
    api: "/api/business/course-operations",
    save: "course-save",
    pageSize: "course-page-size",
  },
  {
    route: "/faculty/teaching-improvement-achievements",
    title: "강의개선 실적 관리",
    api: "/api/business/lecture-improvements",
    save: undefined,
    pageSize: undefined,
  },
  {
    route: "/faculty/employment-rate-achievements",
    title: "취업률 실적 관리",
    api: "/api/business/employment-rate-achievements",
    save: "employment-rate-save",
    pageSize: "employment-rate-page-size",
  },
];

const listing = {
  achievements: [],
  totalElements: 0,
  page: 0,
  pageSize: 20,
  managementItems: [],
  academicYears: [],
  semesters: [],
};
let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  session.roles = ["R01"];
  fetchMock = vi.fn().mockImplementation(async (path: string) => ({
    ok: true,
    status: 200,
    headers: { get: () => "application/json" },
    json: async () => ({
      success: true,
      data: path.includes("/histories") ? [] : listing,
      meta: { requestId: "route-test" },
    }),
  }));
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, "", "/");
});

function enter(route: string) {
  window.history.replaceState({}, "", route);
  render(<AppRouter />);
}

describe("merged education operation routes through real API clients", () => {
  it.each(screens)(
    "R01 direct entry reaches $route and its canonical API",
    async ({ route, title, api }) => {
      enter(route);
      expect(
        await screen.findByRole("heading", { name: title }),
      ).toBeInTheDocument();
      await waitFor(() =>
        expect(fetchMock).toHaveBeenCalledWith(
          expect.stringContaining(`${api}?page=0&pageSize=20`),
          expect.objectContaining({ credentials: "include" }),
        ),
      );
      expect(
        fetchMock.mock.calls.every(([path]) =>
          String(path).startsWith("/api/"),
        ),
      ).toBe(true);
    },
  );

  it.each(screens)(
    "R08 entry to $route cannot fetch protected business data",
    async ({ route }) => {
      session.roles = ["R08"];
      enter(route);
      expect(
        await screen.findByRole("heading", { name: /권한이 없습니다/ }),
      ).toBeInTheDocument();
      expect(fetchMock).not.toHaveBeenCalled();
    },
  );

  it.each(screens.filter((item) => item.save))(
    "R02 cannot save on $route",
    async ({ route, title, save }) => {
      session.roles = ["R02"];
      enter(route);
      await screen.findByRole("heading", { name: title });
      await waitFor(() => expect(fetchMock).toHaveBeenCalled());
      const control = screen.queryByTestId(save!);
      if (control) expect(control).toBeDisabled();
      else expect(control).not.toBeInTheDocument();
      expect(
        fetchMock.mock.calls.every(
          ([, init]) => !init.method || init.method === "GET",
        ),
      ).toBe(true);
    },
  );

  it.each(screens.filter((item) => item.pageSize))(
    "page-size changes on $route refresh canonical list",
    async (item) => {
      enter(item.route);
      await screen.findByRole("heading", { name: item.title });
      const size = await screen.findByTestId(item.pageSize!);
      fireEvent.change(size, { target: { value: "50" } });
      await waitFor(() =>
        expect(fetchMock).toHaveBeenCalledWith(
          expect.stringContaining(`${item.api}?page=0&pageSize=50`),
          expect.any(Object),
        ),
      );
    },
  );

  it("R07 sees validation/history instead of individual CRUD and cannot execute unapproved bulk", async () => {
    session.roles = ["R07"];
    enter("/faculty/employment-rate-achievements");
    await screen.findByText("업로드 이력이 없습니다.");
    expect(
      screen.queryByTestId("employment-rate-individual-tab"),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId("employment-rate-bulk-execute")).toBeDisabled();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/excel-uploads/histories",
      expect.any(Object),
    );
    expect(
      fetchMock.mock.calls.some(([path]) => String(path).includes("?page=")),
    ).toBe(false);
  });
});

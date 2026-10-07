import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest, type CurrentUser } from "../api/apiClient";
import { ADMIN_ROUTES } from "../pages/LoginPage";
import { AppRouter } from "./router";

const session = vi.hoisted(() => ({
  status: "authenticated",
  user: null as CurrentUser | null,
}));
vi.mock("./AuthProvider", () => ({ useAuth: () => session }));

const screens = [
  [
    "employment-rate-improvements",
    "취업률 제고 실적 관리",
    "SCR-EMPLOYMENT-RATE-IMPROVEMENTS",
  ],
  ["course-operations", "강좌 개설·운영 실적 관리", "SCR-COURSE-OPERATIONS"],
  ["lecture-improvements", "강의개선 실적 관리", "SCR-LECTURE-IMPROVEMENTS"],
  [
    "employment-rate-achievements",
    "취업률 실적 관리",
    "SCR-EMPLOYMENT-RATE-ACHIEVEMENTS",
  ],
] as const;

function principal(
  slug: string,
  label: string,
  screenId: string,
  role = "R01",
): CurrentUser {
  return {
    userId: 101,
    loginId: "faculty",
    name: "교원",
    roles: [role],
    menus: [
      {
        menuId: 101,
        menuName: label,
        screenId,
        url: `/faculty/education/${slug}`,
        displayOrder: 1,
        children: [],
      },
    ],
  };
}

function server() {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    headers: { get: () => "application/json" },
    json: async () => ({
      success: true,
      data: { achievements: [], page: 0, pageSize: 20, totalElements: 0 },
      meta: { requestId: "navigation-test" },
    }),
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  cleanup();
  window.history.replaceState({}, "", "/");
  session.user = null;
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("교육영역 공유 route registry", () => {
  it.each(screens)(
    "session menu reaches %s and its real relative API client",
    async (slug, label, id) => {
      session.user = principal(slug, label, id);
      const fetchMock = server();
      window.history.replaceState({}, "", "/");
      render(<AppRouter />);
      fireEvent.click(screen.getByRole("button", { name: "모바일 메뉴" }));
      fireEvent.click(screen.getByRole("link", { name: label }));
      const heading = await screen.findByRole("heading", { name: label });
      expect(heading.closest("[data-screen-id]")).toHaveAttribute(
        "data-screen-id",
        id,
      );
      expect(window.location.pathname).toBe(`/faculty/education/${slug}`);
      await waitFor(() =>
        expect(fetchMock).toHaveBeenCalledWith(
          `/api/business/${slug}?page=0&pageSize=20`,
          expect.objectContaining({ credentials: "include" }),
        ),
      );
      expect(
        ADMIN_ROUTES.find((route) => route.path === window.location.pathname),
      ).toMatchObject({
        screenId: id,
        menuPath: `업적 입력 관리 > 교육영역 > ${label}`,
      });
    },
  );

  it.each(screens)(
    "rejects direct %s access without a session menu",
    async (slug, label, id) => {
      session.user = { ...principal(slug, label, id), menus: [] };
      const fetchMock = server();
      window.history.replaceState({}, "", `/faculty/education/${slug}`);
      render(<AppRouter />);
      expect(
        await screen.findByRole("heading", { name: "권한이 없습니다" }),
      ).toBeInTheDocument();
      expect(fetchMock).not.toHaveBeenCalled();
    },
  );

  it.each(screens)(
    "does not let R09 bypass %s business roles",
    async (slug, label, id) => {
      session.user = principal(slug, label, id, "R09");
      const fetchMock = server();
      window.history.replaceState({}, "", `/faculty/education/${slug}`);
      render(<AppRouter />);
      expect(
        await screen.findByRole("heading", { name: "권한이 없습니다" }),
      ).toBeInTheDocument();
      expect(fetchMock).not.toHaveBeenCalled();
    },
  );

  it("R07 reaches only the employment-rate Excel surface", async () => {
    const [slug, label, id] = screens[3];
    session.user = principal(slug, label, id, "R07");
    const fetchMock = server();
    window.history.replaceState({}, "", `/faculty/education/${slug}`);
    render(<AppRouter />);
    expect(
      await screen.findByRole("heading", { name: label }),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("employment-rate-section-19"),
    ).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(
      expect.stringContaining("?page="),
      expect.anything(),
    );
  });
});

describe("공유 API client transport", () => {
  it("keeps JSON defaults and session cookies for business writes", async () => {
    const fetchMock = server();
    await apiRequest("/api/business/course-operations", {
      method: "POST",
      body: JSON.stringify({ performanceDetails: "등록 내용" }),
    });
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/business/course-operations",
      expect.objectContaining({
        credentials: "include",
        method: "POST",
        headers: { "Content-Type": "application/json" },
      }),
    );
  });

  it("lets the browser encode Excel multipart boundaries and preserves Headers input", async () => {
    const fetchMock = server();
    const body = new FormData();
    body.append("file", new File(["upload"], "achievements.xlsx"));
    await apiRequest(
      "/api/business/employment-rate-achievements/excel-uploads",
      {
        method: "POST",
        body,
        headers: new Headers({ "X-Request-Id": "upload-test" }),
      },
    );
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/business/employment-rate-achievements/excel-uploads",
      expect.objectContaining({
        body,
        credentials: "include",
        headers: { "x-request-id": "upload-test" },
      }),
    );
  });

  it("preserves status and validation fields instead of hiding a business error", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        headers: { get: () => "application/json" },
        json: async () => ({
          success: false,
          error: {
            code: "VALIDATION_ERROR",
            message: "입력 오류",
            fields: [{ field: "managementItemCode", message: "필수 입력" }],
          },
          meta: {},
        }),
      }),
    );
    await expect(
      apiRequest("/api/business/lecture-improvements"),
    ).rejects.toMatchObject({
      status: 400,
      apiError: {
        fields: [{ field: "managementItemCode", message: "필수 입력" }],
      },
    });
  });
});

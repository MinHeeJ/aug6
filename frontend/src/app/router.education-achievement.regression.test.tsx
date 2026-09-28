import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppRouter } from "./router";

const educationRoutes = [
  {
    route: "/achievement/lecture-evaluations",
    screenId: "SCR-LECTURE-EVALUATION-ACHIEVEMENT",
    title: "강의평가 실적 관리",
    collectionPath: "/api/business/lecture-evaluation-achievements",
  },
  {
    route: "/achievement/lecture-performances",
    screenId: "SCR-LECTURE-PERFORMANCE-ACHIEVEMENT",
    title: "강의실적 관리",
    collectionPath: "/api/business/lecture-performance-achievements",
  },
  {
    route: "/achievement/student-guidance",
    screenId: "SCR-STUDENT-GUIDANCE-ACHIEVEMENT",
    title: "학생지도 실적 관리",
    collectionPath: "/api/business/student-guidance-achievements",
  },
  {
    route: "/achievement/graduate-degrees",
    screenId: "SCR-GRADUATE-DEGREE-ACHIEVEMENT",
    title: "석·박사 배출 실적 관리",
    collectionPath: "/api/business/graduate-degree-achievements",
  },
] as const;

vi.mock("./AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
    user: {
      userId: 101,
      loginId: "faculty-member",
      employeeNo: "E0101",
      name: "교원",
      roles: ["R01"],
      menus: educationRoutes.map((route, index) => ({
        menuId: index + 1,
        menuName: route.title,
        screenId: route.screenId,
        url: route.route,
        displayOrder: index + 1,
        children: [],
      })),
    },
  }),
}));

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

/**
 * Regression contract for CMN-701 through CMN-703 and CMN-708: every education-achievement
 * route must expose the same authorized, API-backed search/list/detail workspace. Removing a
 * route, a supported page size, detail tabs, attachment access, or Excel download must fail this
 * test rather than leaving a user on an unrelated dashboard or a partial screen.
 */
describe("education achievement shared screen regression", () => {
  it.each(educationRoutes)(
    "$title provides the shared list, detail, attachment, and export workspace",
    async ({ route, screenId, title, collectionPath }) => {
      window.history.replaceState({}, "", route);
      vi.stubGlobal(
        "fetch",
        vi.fn(async (input: RequestInfo | URL) => {
          const requestedUrl = String(input);
          expect(requestedUrl).toContain(collectionPath);
          expect(requestedUrl).toContain("page=0");
          expect(requestedUrl).toContain("pageSize=20");
          return new Response(
            JSON.stringify({
              success: true,
              data: {
                page: 0,
                pageSize: 20,
                totalElements: 0,
                items: [],
              },
              meta: { requestId: "REQ-B74-REGRESSION" },
            }),
            { headers: { "Content-Type": "application/json" } },
          );
        }),
      );

      render(<AppRouter />);

      const workspace = await screen.findByTestId(screenId);
      expect(workspace).toBeVisible();
      expect(screen.getByRole("heading", { name: title })).toBeVisible();
      expect(screen.getByLabelText("목록 건수")).toHaveValue("20");
      expect(screen.getByRole("option", { name: "20건" })).toBeVisible();
      expect(screen.getByRole("option", { name: "50건" })).toBeVisible();
      expect(screen.getByRole("option", { name: "100건" })).toBeVisible();
      expect(screen.getByRole("button", { name: "조회" })).toBeVisible();
      expect(screen.getByRole("tablist")).toBeVisible();
      expect(
        screen.getByRole("button", { name: "엑셀 다운로드" }),
      ).toBeVisible();
      expect(screen.getByRole("button", { name: /첨부파일/ })).toBeVisible();
    },
  );
});

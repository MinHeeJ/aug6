import { expect, test, type Page } from "@playwright/test";

const unauthenticatedApis = [
  "/api/business/teaching-evaluation-achievements?page=0&size=20",
  "/api/business/teaching-achievements?page=0&size=20",
  "/api/business/student-guidance-achievements?page=0&size=20",
  "/api/business/graduate-achievements?page=0&size=20",
] as const;

const facultyRoutes = [
  {
    route: "/faculty/teaching-evaluation-achievements",
    screenId: "SCR-TEACHING-EVALUATION-ACHIEVEMENT",
  },
  {
    route: "/faculty/teaching-achievements",
    screenId: "SCR-TEACHING-ACHIEVEMENT",
  },
  {
    route: "/faculty/student-guidance-achievements",
    screenId: "SCR-STUDENT-GUIDANCE-ACHIEVEMENT",
  },
  {
    route: "/faculty/graduate-achievements",
    screenId: "SCR-GRADUATE-ACHIEVEMENT",
  },
] as const;

const viewports = [
  { name: "desktop", width: 1440, height: 900 },
  { name: "tablet", width: 1024, height: 768 },
] as const;

test.describe("BASIC-65 cross-cutting regression", () => {
  test("all achievement APIs return the safe 401 envelope when no session is supplied", async ({
    request,
  }) => {
    for (const api of unauthenticatedApis) {
      const response = await request.get(api);
      expect(response.status(), api).toBe(401);
      const body = await response.json();
      expect(body.success, api).toBe(false);
      expect(body.error.code, api).toBe("UNAUTHENTICATED");
      expect(JSON.stringify(body), api).not.toMatch(
        /exception|password|secret|sql/i,
      );
    }
  });

  test("faculty routes retain their responsive screen shell at desktop and tablet widths", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    for (const viewport of viewports) {
      await page.setViewportSize(viewport);
      for (const target of facultyRoutes) {
        await page.goto(target.route);
        await expect(
          page.locator(`[data-screen-id="${target.screenId}"]`),
          `${viewport.name} ${target.route}`,
        ).toBeVisible();
        await expect(
          page.getByRole("button", { name: /조회|검색/ }),
          target.route,
        ).toBeVisible();
      }
    }
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

import { expect, test } from "@playwright/test";

const routes = [
  {
    path: "/achievements/education/lecture-evaluations",
    screenId: "SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT",
    title: "강의평가 실적 관리",
    apiPath: "/api/business/lecture-evaluation-achievements",
  },
  {
    path: "/achievements/education/lecture-achievements",
    screenId: "SCR-LECTURE-ACHIEVEMENT-MGMT",
    title: "강의실적 관리",
    apiPath: "/api/business/lecture-achievements",
  },
  {
    path: "/achievements/education/masters-doctoral-graduations",
    screenId: "SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT",
    title: "석·박사 배출 실적 관리",
    apiPath: "/api/business/degree-completion-achievements",
  },
  {
    path: "/achievements/education/student-guidance-uploads",
    screenId: "SCR-STUDENT-GUIDANCE-EXCEL-UPLOAD",
    title: "학생지도 Excel 일괄등록",
    apiPath:
      "/api/business/student-guidance-achievements/excel-uploads/histories",
  },
] as const;

test.describe("BASIC-81 교육실적 브라우저 smoke", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("사용자 ID").fill("admin");
    await page.getByLabel("비밀번호").fill("admin");
    await page.getByRole("button", { name: "로그인" }).click();
    await expect(page.getByText("R09 시스템관리자")).toBeVisible();
  });

  for (const route of routes) {
    test(`${route.title} 화면이 desktop과 tablet에서 보호 shell 안에 렌더링된다`, async ({
      page,
    }) => {
      const listResponse = page.waitForResponse(
        (response) =>
          new URL(response.url()).pathname === route.apiPath &&
          response.request().method() === "GET",
        { timeout: 3_000 },
      );
      await page.goto(route.path);
      expect((await listResponse).status()).toBe(200);
      await expect(
        page.locator(`[data-screen-id="${route.screenId}"]`),
      ).toBeVisible();
      await expect(page.getByText("권한이 없습니다")).toHaveCount(0);

      await page.setViewportSize({ width: 768, height: 1024 });
      await expect(
        page.locator(`[data-screen-id="${route.screenId}"]`),
      ).toBeVisible();
    });
  }
});

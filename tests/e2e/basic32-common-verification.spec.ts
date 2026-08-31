import { expect, test, type Page } from "@playwright/test";

const basic32Routes = [
  {
    route: "/admin/evaluation-organization-mappings",
    screenId: "SCR-EVALUATION-ORG-MAPPING",
    pageSizeSelect: "evaluation-organization-page-size-select",
    lookupApi: "/api/business/evaluation-organization-mappings?page=0&size=20",
  },
  {
    route: "/admin/business-status-codes",
    screenId: "SCR-BUSINESS-STATUS-CODE",
    pageSizeSelect: "business-status-code-page-size-select",
    lookupApi: "/api/admin/business-status-codes?page=0&size=20",
  },
  {
    route: "/admin/business-status-transitions",
    screenId: "SCR-BUSINESS-STATUS-TRANSITION",
    pageSizeSelect: "business-status-transition-page-size-select",
    lookupApi: "/api/admin/business-status-transitions?page=0&size=20",
  },
  {
    route: "/admin/rejection-reasons",
    screenId: "SCR-REJECTION-REASON",
    pageSizeSelect: "rejection-reason-page-size-select",
    lookupApi: "/api/admin/rejection-reasons?page=0&size=20",
  },
  {
    route: "/admin/data-change-histories",
    screenId: "SCR-DATA-CHANGE-HISTORY",
    pageSizeSelect: "data-change-history-page-size-select",
    lookupApi: "/api/admin/data-change-histories?page=0&size=20",
  },
  {
    route: "/admin/deleted-business-data",
    screenId: "SCR-DELETED-BUSINESS-DATA",
    pageSizeSelect: "deleted-business-data-page-size-select",
    lookupApi: "/api/admin/deleted-business-data?page=0&size=20",
  },
] as const;

const viewportSmokeMatrix = [
  { label: "Edge desktop", width: 1280, height: 800 },
  { label: "Chrome desktop", width: 1280, height: 800 },
  { label: "Safari desktop", width: 1440, height: 900 },
  { label: "Opera desktop", width: 1280, height: 800 },
  { label: "Whale desktop", width: 1280, height: 800 },
  { label: "iPadOS tablet", width: 1024, height: 768 },
  { label: "Android tablet", width: 900, height: 720 },
] as const;

test.describe("BASIC-32 common verification smoke", () => {
  test("new business lists default to 20 and expose 20/50/100 page size choices", async ({
    page,
  }) => {
    await loginAsAdmin(page);

    for (const item of basic32Routes) {
      await page.goto(item.route);
      await expect(
        page.locator(`[data-screen-id="${item.screenId}"]`),
        item.route,
      ).toBeVisible();
      await expect(page.getByText("권한이 없습니다")).toHaveCount(0);
      const select = page.getByTestId(item.pageSizeSelect);
      await expect(select).toHaveValue("20");
      for (const size of ["20", "50", "100"]) {
        await select.selectOption(size);
        await expect(select).toHaveValue(size);
      }
    }
  });

  test("business list APIs answer default size 20 envelopes without leaking stack details", async ({
    request,
  }) => {
    for (const item of basic32Routes) {
      const response = await request.get(item.lookupApi);
      expect(response.status(), item.lookupApi).toBeGreaterThanOrEqual(200);
      expect(response.status(), item.lookupApi).toBeLessThan(300);
      const body = await response.json();
      expect(body.success, item.lookupApi).toBe(true);
      expect(body.data.size, item.lookupApi).toBe(20);
      expect(JSON.stringify(body)).not.toContain("Exception");
      expect(JSON.stringify(body)).not.toContain("password");
    }
  });

  test("representative route satisfies tablet and desktop browser smoke viewports", async ({
    page,
  }) => {
    await loginAsAdmin(page);

    for (const target of viewportSmokeMatrix) {
      await page.setViewportSize({
        width: target.width,
        height: target.height,
      });
      await page.goto("/admin/business-status-codes");
      await expect(
        page.locator('[data-screen-id="SCR-BUSINESS-STATUS-CODE"]'),
        target.label,
      ).toBeVisible();
      await expect(page.getByText("권한이 없습니다")).toHaveCount(0);
      await expect(
        page.getByTestId("business-status-code-search-button"),
      ).toBeVisible();
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

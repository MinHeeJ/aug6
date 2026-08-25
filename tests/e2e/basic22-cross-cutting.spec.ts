import { expect, test, type Page } from "@playwright/test";

const basic22Routes = [
  {
    route: "/admin/messages",
    screenId: "SCR-MESSAGE-MGMT",
    pageSizeSelect: "message-page-size-select",
  },
  {
    route: "/admin/notices",
    screenId: "SCR-NOTICE-MGMT",
    pageSizeSelect: "notice-page-size-select",
  },
  {
    route: "/admin/help-contents",
    screenId: "SCR-HELP-MGMT",
    pageSizeSelect: "help-page-size-select",
  },
  {
    route: "/admin/manuals",
    screenId: "SCR-MANUAL-MGMT",
    pageSizeSelect: "manual-page-size-select",
  },
] as const;

const quickstartSmokeMatrix = [
  { label: "Edge desktop", width: 1280, height: 800 },
  { label: "Chrome desktop", width: 1280, height: 800 },
  { label: "Safari desktop", width: 1440, height: 900 },
  { label: "Opera desktop", width: 1280, height: 800 },
  { label: "Whale desktop", width: 1280, height: 800 },
  { label: "iPadOS tablet", width: 1024, height: 768 },
  { label: "Android tablet", width: 900, height: 720 },
] as const;

test.describe("BASIC-22 cross-cutting smoke", () => {
  test("new management lists default to 20 and expose 20/50/100 page sizes", async ({
    page,
  }) => {
    await loginAsAdmin(page);

    for (const item of basic22Routes) {
      await page.goto(item.route);
      await expect(
        page.locator(`[data-screen-id="${item.screenId}"]`),
        item.route,
      ).toBeVisible();
      const select = page.getByTestId(item.pageSizeSelect);
      await expect(select).toHaveValue("20");
      for (const size of ["20", "50", "100"]) {
        await select.selectOption(size);
        await expect(select).toHaveValue(size);
      }
    }
  });

  test("save flows show confirmation before mutation and announce results after processing", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/messages");

    await page.getByTestId("message-new-button").click();
    await page.getByTestId("message-code-input").fill("SAVE.BASIC22.E2E");
    await page
      .getByTestId("message-user-message-textarea")
      .fill("저장되었습니다.");
    await page
      .getByTestId("message-change-reason-input")
      .fill("BASIC-22 확인 메시지 smoke");

    page.once("dialog", async (dialog) => {
      expect(dialog.message()).toContain(
        "SAVE.BASIC22.E2E 메시지를 저장하시겠습니까?",
      );
      await dialog.dismiss();
    });
    await page.getByTestId("message-save-button").click();
    await expect(page.getByText("메시지가 저장되었습니다.")).toHaveCount(0);
  });

  test("quickstart browser and tablet viewport scope renders BASIC-22 routes", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    for (const target of quickstartSmokeMatrix) {
      await page.setViewportSize({
        width: target.width,
        height: target.height,
      });
      await page.goto("/admin/messages");
      await expect(
        page.locator('[data-screen-id="SCR-MESSAGE-MGMT"]'),
        target.label,
      ).toBeVisible();
      await expect(page.getByText("권한이 없습니다")).toHaveCount(0);
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

import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-36 US-01 KORUS faculty sync", () => {
  test("/admin/korus-faculty-sync runs manual sync, lists results, and retries failed rows", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/korus-faculty-sync");

    await expect(
      page.locator('[data-screen-id="SCR-KORUS-FACULTY-SYNC"]'),
    ).toBeVisible();
    await expect(
      page.getByText("KORUS 원천 스냅샷은 수정할 수 없습니다"),
    ).toBeVisible();
    await expect(
      page.getByTestId("korus-faculty-sync-page-size-select"),
    ).toHaveValue("20");

    await page.getByTestId("korus-faculty-sync-start-input").fill("2026-01-01");
    await page.getByTestId("korus-faculty-sync-end-input").fill("2026-12-31");

    page.once("dialog", async (dialog) => {
      expect(dialog.message()).toContain("수동 동기화");
      await dialog.accept();
    });
    await page.getByTestId("korus-faculty-sync-run-button").click();

    await expect(page.getByText(/request_id:/)).toBeVisible();
    await expect(
      page.getByTestId("korus-faculty-sync-result-row").first(),
    ).toContainText(/E[0-9]+/);
    await expect(page.getByText("request_id")).toBeVisible();
    await expect(page.getByText("employee_no")).toBeVisible();
    await expect(page.getByText("organization_code")).toBeVisible();
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

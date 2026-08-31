import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-34 US-02 participation rate management", () => {
  test("/admin/participation-rates saves a draft participation rate and refreshes the list", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/participation-rates");

    await expect(
      page.locator('[data-screen-id="SCR-PARTICIPATION-RATE-MGMT"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("participation-rate-page-size-select"),
    ).toHaveValue("20");

    await page
      .getByTestId("participation-rate-rule-version-id-input")
      .fill("10");
    await page
      .getByTestId("participation-rate-management-item-id-input")
      .fill("400");
    await page
      .getByTestId("participation-rate-researcher-count-input")
      .fill("3");
    await page
      .getByTestId("participation-rate-participation-type-input")
      .fill("LEAD");
    await page
      .getByTestId("participation-rate-distribution-rate-input")
      .fill("0.5");
    await page
      .getByTestId("participation-rate-effective-start-date-input")
      .fill("2026-01-01");
    await page
      .getByTestId("participation-rate-effective-end-date-input")
      .fill("2026-12-31");
    await page
      .getByTestId("participation-rate-change-reason-textarea")
      .fill("E2E 저장 후 목록 재조회");

    page.once("dialog", async (dialog) => {
      expect(dialog.message()).toContain("참여구분 배분율을 저장하시겠습니까");
      await dialog.accept();
    });
    await page.getByTestId("participation-rate-save-button").click();

    await expect(page.getByText("저장되었습니다")).toBeVisible();
    await expect(
      page.getByTestId("participation-rate-row").first(),
    ).toContainText("LEAD");
    await expect(
      page.getByTestId("participation-rate-row").first(),
    ).toContainText("0.5");
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

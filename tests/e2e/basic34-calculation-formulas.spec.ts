import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-34 US-03 calculation formula management", () => {
  test("/admin/calculation-formulas saves a draft formula and refreshes the list", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/calculation-formulas");

    await expect(
      page.locator('[data-screen-id="SCR-CALC-FORMULA-MGMT"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("calculation-formula-page-size-select"),
    ).toHaveValue("20");

    await page
      .getByTestId("calculation-formula-rule-version-id-input")
      .fill("10");
    await page
      .getByTestId("calculation-formula-formula-code-input")
      .fill("RAW_SCORE");
    await page
      .getByTestId("calculation-formula-calculation-type-select")
      .selectOption("FIXED_SCORE");
    await page
      .getByTestId("calculation-formula-variable-definition-textarea")
      .fill('{"baseScore":true}');
    await page
      .getByTestId("calculation-formula-rounding-rule-input")
      .fill("ROUND_HALF_UP");
    await page
      .getByTestId("calculation-formula-lower-bound-score-input")
      .fill("0");
    await page
      .getByTestId("calculation-formula-upper-bound-score-input")
      .fill("100");
    await page
      .getByTestId("calculation-formula-evaluation-year-input")
      .fill("2026");
    await page
      .getByTestId("calculation-formula-effective-start-date-input")
      .fill("2026-01-01");
    await page
      .getByTestId("calculation-formula-effective-end-date-input")
      .fill("2026-12-31");
    await page
      .getByTestId("calculation-formula-change-reason-textarea")
      .fill("E2E 저장 후 목록 재조회");

    page.once("dialog", async (dialog) => {
      expect(dialog.message()).toContain("계산식을 저장하시겠습니까");
      await dialog.accept();
    });
    await page.getByTestId("calculation-formula-save-button").click();

    await expect(page.getByText("저장되었습니다")).toBeVisible();
    await expect(
      page.getByTestId("calculation-formula-row").first(),
    ).toContainText("RAW_SCORE");
    await expect(
      page.getByTestId("calculation-formula-row").first(),
    ).toContainText("ROUND_HALF_UP");
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

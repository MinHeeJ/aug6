import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-34 US-01 evaluation score management", () => {
  test("/admin/evaluation-scores saves a draft evaluation score and refreshes the list", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/evaluation-scores");

    await expect(
      page.locator('[data-screen-id="SCR-EVAL-SCORE-MGMT"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("evaluation-score-page-size-select"),
    ).toHaveValue("20");

    await page.getByTestId("evaluation-score-rule-version-id-input").fill("10");
    await page
      .getByTestId("evaluation-score-management-item-id-input")
      .fill("400");
    await page
      .getByTestId("evaluation-score-organization-code-input")
      .fill("COL-EDU");
    await page
      .getByTestId("evaluation-score-evaluation-year-input")
      .fill("2026");
    await page.getByTestId("evaluation-score-base-score-input").fill("10.5");
    await page.getByTestId("evaluation-score-max-score-input").fill("20");
    await page
      .getByTestId("evaluation-score-effective-start-date-input")
      .fill("2026-01-01");
    await page
      .getByTestId("evaluation-score-effective-end-date-input")
      .fill("2026-12-31");
    await page
      .getByTestId("evaluation-score-change-reason-textarea")
      .fill("E2E 저장 후 목록 재조회");

    page.once("dialog", async (dialog) => {
      expect(dialog.message()).toContain("평가점수를 저장하시겠습니까");
      await dialog.accept();
    });
    await page.getByTestId("evaluation-score-save-button").click();

    await expect(page.getByText("저장되었습니다")).toBeVisible();
    await expect(
      page.getByTestId("evaluation-score-row").first(),
    ).toContainText("COL-EDU");
    await expect(
      page.getByTestId("evaluation-score-row").first(),
    ).toContainText("10.5");
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-34 US-04 evaluation rule set management", () => {
  test("/admin/evaluation-rule-sets saves a draft rule set and refreshes the list", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/evaluation-rule-sets");

    await expect(
      page.locator('[data-screen-id="SCR-EVAL-RULE-SET-MGMT"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("evaluation-rule-set-page-size-select"),
    ).toHaveValue("20");

    await page
      .getByTestId("evaluation-rule-set-rule-version-id-input")
      .fill("10");
    await page
      .getByTestId("evaluation-rule-set-target-scope-input")
      .fill("FACULTY");
    await page
      .getByTestId("evaluation-rule-set-rule-set-name-input")
      .fill("교수업적 기준·점수규칙");
    await page
      .getByTestId("evaluation-rule-set-rule-set-status-select")
      .selectOption("DRAFT");
    await page
      .getByTestId("evaluation-rule-set-active-yn-select")
      .selectOption("Y");
    await page
      .getByTestId("evaluation-rule-set-effective-start-date-input")
      .fill("2026-01-01");
    await page
      .getByTestId("evaluation-rule-set-effective-end-date-input")
      .fill("2026-12-31");
    await page
      .getByTestId("evaluation-rule-set-change-reason-textarea")
      .fill("E2E 저장 후 목록 재조회");

    page.once("dialog", async (dialog) => {
      expect(dialog.message()).toContain(
        "업적평가 기준·점수규칙을 저장하시겠습니까",
      );
      await dialog.accept();
    });
    await page.getByTestId("evaluation-rule-set-save-button").click();

    await expect(page.getByText("저장되었습니다")).toBeVisible();
    await expect(
      page.getByTestId("evaluation-rule-set-row").first(),
    ).toContainText("교수업적 기준·점수규칙");
    await expect(
      page.getByTestId("evaluation-rule-set-row").first(),
    ).toContainText("FACULTY");
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

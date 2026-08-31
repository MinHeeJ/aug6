import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-34 US-05 journal indexing info management", () => {
  test("/admin/journal-indexing-infos saves a draft journal info and refreshes the list", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/journal-indexing-infos");

    await expect(
      page.locator('[data-screen-id="SCR-JOURNAL-INDEXING-MGMT"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("journal-indexing-info-page-size-select"),
    ).toHaveValue("20");

    await page
      .getByTestId("journal-indexing-info-rule-version-id-input")
      .fill("10");
    await page
      .getByTestId("journal-indexing-info-issn-input")
      .fill("1225-6463");
    await page
      .getByTestId("journal-indexing-info-journal-name-input")
      .fill("한국교육학술지");
    await page
      .getByTestId("journal-indexing-info-indexing-type-select")
      .selectOption("KCI");
    await page
      .getByTestId("journal-indexing-info-publication-country-input")
      .fill("KR");
    await page
      .getByTestId("journal-indexing-info-active-yn-select")
      .selectOption("Y");
    await page
      .getByTestId("journal-indexing-info-valid-start-date-input")
      .fill("2026-01-01");
    await page
      .getByTestId("journal-indexing-info-valid-end-date-input")
      .fill("2026-12-31");
    await page
      .getByTestId("journal-indexing-info-source-name-input")
      .fill("파일럿 시드");
    await page
      .getByTestId("journal-indexing-info-source-updated-at-input")
      .fill("2026-08-31T09:00");
    await page
      .getByTestId("journal-indexing-info-change-reason-textarea")
      .fill("E2E 저장 후 목록 재조회");

    page.once("dialog", async (dialog) => {
      expect(dialog.message()).toContain(
        "학술지·후보지 등재정보를 저장하시겠습니까",
      );
      await dialog.accept();
    });
    await page.getByTestId("journal-indexing-info-save-button").click();

    await expect(page.getByText("저장되었습니다")).toBeVisible();
    await expect(
      page.getByTestId("journal-indexing-info-row").first(),
    ).toContainText("1225-6463");
    await expect(
      page.getByTestId("journal-indexing-info-row").first(),
    ).toContainText("한국교육학술지");
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

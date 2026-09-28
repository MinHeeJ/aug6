import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-73 Phase 3 education achievement routes", () => {
  test("R01 reaches the lecture-achievement screen and sees the persisted lecture fixture", async ({
    page,
  }) => {
    await loginAsTeacher(page);

    await page.goto("/faculty/education/lecture-achievements");

    await expect(
      page.locator('[data-screen-id="SCR-LECTURE-ACHIEVEMENT"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("lecture-achievement-management-item-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("lecture-achievement-occurrence-date-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("lecture-achievement-save-button"),
    ).toBeVisible();
    await expect(page.getByTestId("lecture-achievement-row")).toHaveCount(1);
    await expect(page.getByTestId("lecture-achievement-row")).toContainText(
      "EDU-LECTURE-ACHIEVEMENT-01",
    );
  });

  test("R01 reaches degree completion, supplies a student detail, and cannot edit a confirmed row", async ({
    page,
  }) => {
    await loginAsTeacher(page);

    await page.goto("/faculty/education/degree-completion-achievements");

    await expect(
      page.locator('[data-screen-id="SCR-DEGREE-COMPLETION-ACHIEVEMENT"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-degree-type-select"),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-student-name-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-thesis-title-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-awarded-date-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-save-button"),
    ).toBeVisible();

    const confirmedRow = page.getByTestId("degree-completion-row").filter({
      hasText: "평가확정",
    });
    await expect(confirmedRow).toHaveCount(1);
    await confirmedRow.click();
    await expect(
      page.getByText("평가확정된 실적은 수정할 수 없습니다."),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-save-button"),
    ).toBeDisabled();
  });
});

async function loginAsTeacher(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("teacher");
  await page.getByLabel("비밀번호").fill("teacher");
  await page.getByRole("button", { name: "로그인" }).click();
}

import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-73 강의평가 실적", () => {
  test("R01 reaches the lecture-evaluation route and sees the seeded lecture-evaluation list", async ({
    page,
  }) => {
    await loginAsTeacher(page);
    await page.goto("/faculty/education/lecture-evaluation-achievements");

    await expect(
      page.locator('[data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("lecture-evaluation-size-select"),
    ).toHaveValue("20");
    await expect(
      page.getByTestId("lecture-evaluation-achievement-row").first(),
    ).toBeVisible();
  });

  test("lecture-evaluation screen provides query, validation, confirmation, and API failure states", async ({
    page,
  }) => {
    await loginAsTeacher(page);
    await page.goto("/faculty/education/lecture-evaluation-achievements");

    await page.getByTestId("lecture-evaluation-search-button").click();
    await page.getByTestId("lecture-evaluation-create-button").click();
    await page.getByTestId("lecture-evaluation-save-button").click();

    await expect(
      page.getByTestId("lecture-evaluation-management-item-error"),
    ).toBeVisible();
    await expect(
      page.getByTestId("lecture-evaluation-occurrence-date-error"),
    ).toBeVisible();
    await expect(
      page.getByTestId("lecture-evaluation-confirm-dialog"),
    ).toHaveCount(0);
  });

  test("lecture-evaluation list maps API failures and forbidden access to actionable states", async ({
    page,
  }) => {
    await loginAsTeacher(page);
    await page.route(
      "**/api/business/education-achievements**",
      async (route) => {
        await route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({
            success: false,
            error: {
              code: "FORBIDDEN",
              message: "강의평가 실적 조회 권한이 없습니다.",
            },
            meta: {},
          }),
        });
      },
    );

    await page.goto("/faculty/education/lecture-evaluation-achievements");

    await expect(
      page.getByTestId("lecture-evaluation-permission-state"),
    ).toBeVisible();
    await expect(
      page.getByText("강의평가 실적 조회 권한이 없습니다."),
    ).toBeVisible();
  });
});

async function loginAsTeacher(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("teacher");
  await page.getByLabel("비밀번호").fill("teacher");
  await page.getByRole("button", { name: "로그인" }).click();
}

import { expect, test, type Page } from "@playwright/test";

async function loginAsTeacher(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("teacher");
  await page.getByLabel("비밀번호").fill("teacher");
  await page.getByRole("button", { name: "로그인" }).click();
}

async function expectEducationAchievementListResponse(
  page: Page,
  achievementType: "LECTURE_ACHIEVEMENT" | "DEGREE_COMPLETION",
) {
  const response = await page.waitForResponse((candidate) => {
    const url = new URL(candidate.url());
    return (
      url.pathname === "/api/business/education-achievements" &&
      url.searchParams.get("achievementType") === achievementType
    );
  });

  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  expect(body.success).toBe(true);
  expect(body.data.items).toBeInstanceOf(Array);
}

test.describe("BASIC-73 강의실적·석박사 배출 실적", () => {
  test("R01 reaches the lecture-achievement route and loads its API-backed list", async ({
    page,
  }) => {
    await loginAsTeacher(page);
    const listResponse = expectEducationAchievementListResponse(
      page,
      "LECTURE_ACHIEVEMENT",
    );
    await page.goto("/faculty/education/lecture-achievements");

    await listResponse;
    await expect(
      page.locator('[data-screen-id="SCR-LECTURE-ACHIEVEMENT"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("lecture-achievement-size-select"),
    ).toHaveValue("20");
    await expect(
      page.getByTestId("lecture-achievement-create-button"),
    ).toBeVisible();
    await page.getByTestId("lecture-achievement-create-button").click();
    await expect(
      page.getByTestId("lecture-achievement-management-item-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("lecture-achievement-occurrence-date-input"),
    ).toBeVisible();
  });

  test("R01 reaches the degree-completion route and sees an editable student-detail form", async ({
    page,
  }) => {
    await loginAsTeacher(page);
    const listResponse = expectEducationAchievementListResponse(
      page,
      "DEGREE_COMPLETION",
    );
    await page.goto("/faculty/education/degree-completion-achievements");

    await listResponse;
    await expect(
      page.locator('[data-screen-id="SCR-DEGREE-COMPLETION-ACHIEVEMENT"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-create-button"),
    ).toBeVisible();
    await page.getByTestId("degree-completion-create-button").click();
    await expect(
      page.getByTestId("degree-completion-student-detail-row").first(),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-degree-type-select").first(),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-student-name-input").first(),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-thesis-title-input").first(),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-awarded-on-input").first(),
    ).toBeVisible();
  });

  test("confirmed degree-completion rows expose a non-editable locked state", async ({
    page,
  }) => {
    await loginAsTeacher(page);
    await page.route(
      "**/api/business/education-achievements**",
      async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            success: true,
            data: {
              items: [
                {
                  achievementId: 701,
                  achievementType: "DEGREE_COMPLETION",
                  achievementStatus: "CERTIFIED",
                  evaluationYear: "2026",
                  ownerUserId: 1,
                  managementItemCode: "DEGREE-COMPLETION",
                  occurrenceDate: "2026-08-31",
                  evaluationConfirmedYn: "Y",
                  updatedAt: "2026-08-31T09:00:00",
                },
              ],
              page: 0,
              size: 20,
              totalElements: 1,
            },
            meta: {},
          }),
        });
      },
    );
    await page.goto("/faculty/education/degree-completion-achievements");

    await expect(
      page.getByTestId("degree-completion-confirmed-lock-notice"),
    ).toBeVisible();
    await expect(
      page.getByTestId("degree-completion-save-button"),
    ).toBeDisabled();
  });
});

import { expect, test } from "@playwright/test";

test.describe("BASIC-73 교육영역 실적 상태 전이", () => {
  test("R01 등록본은 제출하고 R02 확인과 R04 인증을 거쳐 평가확정 후 수정과 삭제가 차단된다", async ({
    page,
  }) => {
    let currentRole = "R01";
    let currentStatus = "DRAFTING";
    const requestId = "basic73-workflow-request";

    await page.route("**/api/auth/me", async (route) => {
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: {
            userId: 101,
            loginId: "workflow-user",
            name: "업무 검증 사용자",
            roles: [currentRole],
            menus: [],
          },
          meta: {},
        }),
      });
    });
    await page.route(
      "**/api/business/education-achievements?**",
      async (route) => {
        await route.fulfill({
          contentType: "application/json",
          body: JSON.stringify({
            success: true,
            data: {
              items: [
                {
                  achievementId: 701,
                  achievementType: "LECTURE_ACHIEVEMENT",
                  achievementStatus: currentStatus,
                  evaluationYear: "2026",
                  ownerUserId: 101,
                  managementItemCode: "LECTURE-HOURS",
                  occurrenceDate: "2026-03-15",
                  evaluationConfirmedYn:
                    currentStatus === "EVALUATION_CONFIRMED" ? "Y" : "N",
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
    await page.route(
      "**/api/business/education-achievements/701/transition",
      async (route) => {
        const actionType = (
          route.request().postDataJSON() as { actionType: string }
        ).actionType;
        currentStatus =
          actionType === "SUBMIT"
            ? "SUBMITTED"
            : actionType === "REJECT"
              ? "DEPARTMENT_REJECTED"
              : actionType === "CONFIRM"
                ? "DEPARTMENT_CONFIRMED"
                : "CERTIFIED";
        await route.fulfill({
          contentType: "application/json",
          body: JSON.stringify({
            success: true,
            data: { achievementStatus: currentStatus },
            meta: { requestId },
          }),
        });
      },
    );

    await page.goto("/faculty/education/lecture-achievements");
    await expect(
      page.locator('[data-screen-id="SCR-LECTURE-ACHIEVEMENT"]'),
    ).toBeVisible();

    await page.getByTestId("lecture-achievement-submit-button").click();
    await expect(
      page.getByTestId("lecture-achievement-status-DRAFTING"),
    ).toHaveCount(0);
    await expect(
      page.getByTestId("lecture-achievement-status-SUBMITTED"),
    ).toBeVisible();

    currentRole = "R02";
    await page.reload();
    await page
      .getByTestId("lecture-achievement-transition-opinion-input")
      .fill("보완 후 재제출하세요.");
    await page.getByTestId("lecture-achievement-reject-button").click();
    await expect(
      page.getByTestId("lecture-achievement-status-DEPARTMENT-REJECTED"),
    ).toBeVisible();

    currentRole = "R01";
    await page.reload();
    await page.getByTestId("lecture-achievement-resubmit-button").click();
    await expect(
      page.getByTestId("lecture-achievement-status-SUBMITTED"),
    ).toBeVisible();

    currentRole = "R02";
    await page.reload();
    await page.getByTestId("lecture-achievement-confirm-button").click();
    await expect(
      page.getByTestId("lecture-achievement-status-DEPARTMENT-CONFIRMED"),
    ).toBeVisible();

    currentRole = "R04";
    await page.reload();
    await page.getByTestId("lecture-achievement-certify-button").click();
    await expect(
      page.getByTestId("lecture-achievement-status-CERTIFIED"),
    ).toBeVisible();

    currentStatus = "EVALUATION_CONFIRMED";
    await page.reload();
    await expect(
      page.getByTestId("lecture-achievement-edit-button"),
    ).toBeDisabled();
    await expect(
      page.getByTestId("lecture-achievement-delete-button"),
    ).toBeDisabled();
  });
});

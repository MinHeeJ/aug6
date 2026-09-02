import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-43 담당자 인증 관리", () => {
  test("R09 can render route, process fixture, and re-query processed history", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/achievement-verifications");
    await expect(
      page.locator('[data-screen-id="SCR-ACHIEVEMENT-VERIFICATION-MGMT"]'),
    ).toBeVisible();
    await expect(page.getByText("권한이 없습니다")).toHaveCount(0);
    await expect(
      page.getByTestId("achievement-verification-size-select"),
    ).toHaveValue("20");

    const before = await listAchievementVerificationTargets(page);
    expect(before.success).toBe(true);
    expect(before.data.size).toBe(20);
    expect(
      before.data.targets.length,
      "BASIC-43 fixture target is required for T013",
    ).toBeGreaterThan(0);

    const target = before.data.targets[0];
    const transition = await page.evaluate(async (targetId) => {
      const response = await fetch(
        `/api/business/achievement-verifications/${encodeURIComponent(String(targetId))}/transition`,
        {
          method: "POST",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            actionType: "CERTIFY",
            opinion: "Playwright 담당자 인증",
            evidenceRef: "PW-EVIDENCE",
          }),
        },
      );
      return { status: response.status, body: await response.json() };
    }, target.achievementId);

    expect(transition.status).toBe(200);
    expect(transition.body.success).toBe(true);
    expect(transition.body.data.nextStatus).toBe("CERTIFIED");
    expect(transition.body.data.evidenceRef).toBe("PW-EVIDENCE");
    expect(transition.body.data.processedBy).toBeTruthy();
    expect(transition.body.data.processedAt).toBeTruthy();

    const after = await listAchievementVerificationTargets(page);
    const processed = after.data.targets.find(
      (row) => row.achievementId === target.achievementId,
    );
    expect(processed).toBeTruthy();
    expect(processed?.nextStatus).toBe("CERTIFIED");
    expect(processed?.opinion ?? "").toContain("Playwright 담당자 인증");
    expect(processed?.evidenceRef ?? "").toBe("PW-EVIDENCE");
    expect(processed?.processedBy).toBeTruthy();
    expect(processed?.processedAt).toBeTruthy();
  });

  test("non-R09 direct route shows permission denied state", async ({
    page,
  }) => {
    await loginAsTeacher(page);
    await page.goto("/admin/achievement-verifications");
    await expect(page.getByText("권한이 없습니다")).toBeVisible();
  });
});

async function listAchievementVerificationTargets(page: Page) {
  return page.evaluate(async () => {
    const response = await fetch(
      "/api/business/achievement-verifications?page=0&size=20",
      {
        credentials: "include",
      },
    );
    return response.json();
  }) as Promise<{
    success: boolean;
    data: {
      targets: Array<{
        achievementId: number;
        nextStatus: string;
        opinion?: string;
        evidenceRef?: string;
        processedBy?: number;
        processedAt?: string;
      }>;
      size: number;
    };
  }>;
}

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

async function loginAsTeacher(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("teacher");
  await page.getByLabel("비밀번호").fill("teacher");
  await page.getByRole("button", { name: "로그인" }).click();
}

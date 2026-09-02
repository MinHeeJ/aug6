import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-43 지급승인 관리", () => {
  test("R09 can render route, process fixture, and re-query processed history", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/grant-payment-approvals");
    await expect(
      page.locator('[data-screen-id="SCR-GRANT-PAYMENT-APPROVAL-MGMT"]'),
    ).toBeVisible();
    await expect(page.getByText("권한이 없습니다")).toHaveCount(0);
    await expect(
      page.getByTestId("grant-payment-approval-size-select"),
    ).toHaveValue("20");

    const before = await listGrantPaymentApprovals(page);
    expect(before.success).toBe(true);
    expect(before.data.size).toBe(20);
    expect(
      before.data.approvals.length,
      "BASIC-43 fixture target is required for T018",
    ).toBeGreaterThan(0);

    const target = before.data.approvals[0];
    expect(target.requestedAmountSnapshot).toBeTruthy();
    expect(target.paymentAmountSnapshot).toBeTruthy();
    expect(target.accountSnapshotRef).toBeTruthy();
    expect(target.linkedAchievementId).toBeTruthy();

    const transition = await page.evaluate(async (targetId) => {
      const response = await fetch(
        `/api/business/grant-payment-approvals/${encodeURIComponent(String(targetId))}/transition`,
        {
          method: "POST",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            actionType: "APPROVE",
            opinion: "Playwright 지급승인",
          }),
        },
      );
      return { status: response.status, body: await response.json() };
    }, target.grantApplicationId);

    expect(transition.status).toBe(200);
    expect(transition.body.success).toBe(true);
    expect(transition.body.data.approvalStatus).toBe("APPROVED");
    expect(transition.body.data.processedBy).toBeTruthy();
    expect(transition.body.data.processedAt).toBeTruthy();

    const after = await listGrantPaymentApprovals(page);
    const processed = after.data.approvals.find(
      (row) => row.grantApplicationId === target.grantApplicationId,
    );
    expect(processed).toBeTruthy();
    expect(processed?.approvalStatus).toBe("APPROVED");
    expect(processed?.opinion ?? "").toContain("Playwright 지급승인");
    expect(processed?.processedBy).toBeTruthy();
    expect(processed?.processedAt).toBeTruthy();
  });

  test("non-R09 direct route shows permission denied state", async ({
    page,
  }) => {
    await loginAsTeacher(page);
    await page.goto("/admin/grant-payment-approvals");
    await expect(page.getByText("권한이 없습니다")).toBeVisible();
  });
});

async function listGrantPaymentApprovals(page: Page) {
  return page.evaluate(async () => {
    const response = await fetch(
      "/api/business/grant-payment-approvals?page=0&size=20",
      {
        credentials: "include",
      },
    );
    return response.json();
  }) as Promise<{
    success: boolean;
    data: {
      approvals: Array<{
        grantApplicationId: number;
        linkedAchievementId?: number;
        requestedAmountSnapshot: number;
        paymentAmountSnapshot: number;
        accountSnapshotRef: string;
        approvalStatus: string;
        opinion?: string;
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

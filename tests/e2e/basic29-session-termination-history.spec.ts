import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-29 세션 종료 이력 조회", () => {
  test("R09 admin can search immutable logout and expiration histories", async ({
    page,
    request,
  }) => {
    await loginAsAdmin(page);

    await page.goto("/admin/security/session-termination-histories");
    await expect(
      page.locator('[data-screen-id="SCR-SESSION-TERMINATION-HISTORY"]'),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "로그아웃·만료 이력" }),
    ).toBeVisible();
    await expect(
      page.getByTestId("session-termination-filter-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("session-termination-type-select"),
    ).toBeVisible();
    await expect(
      page.getByTestId("session-termination-search-button"),
    ).toBeVisible();
    await expect(page.getByRole("button", { name: /강제종료/ })).toHaveCount(0);
    await expect(page.getByRole("button", { name: /삭제/ })).toHaveCount(0);

    const listResponse = await request.get(
      "/api/admin/security/session-termination-histories?page=0&size=20&terminationType=IDLE_TIMEOUT",
    );
    expect(listResponse.status()).toBe(200);
    const listBody = await listResponse.json();
    expect(listBody.success).toBe(true);
    expect(listBody.data.size).toBe(20);
    expect(
      listBody.data.histories.every(
        (row: { terminationType: string }) =>
          row.terminationType === "IDLE_TIMEOUT",
      ),
    ).toBe(true);
    expect(
      listBody.data.histories.every(
        (row: { terminatedAt?: string; terminationReason?: string }) =>
          Boolean(row.terminatedAt) && "terminationReason" in row,
      ),
    ).toBe(true);
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-29 중요정보 조회 로그 조회", () => {
  test("R09 admin can search sensitive information access logs without protected plaintext", async ({
    page,
    request,
  }) => {
    await loginAsAdmin(page);

    await page.goto("/admin/audit/sensitive-information-access-logs");
    await expect(
      page.locator('[data-screen-id="SCR-SENSITIVE-INFO-ACCESS-LOG"]'),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "중요정보 조회 로그" }),
    ).toBeVisible();
    await expect(
      page.getByTestId("sensitive-information-type-select"),
    ).toBeVisible();
    await expect(
      page.getByTestId("sensitive-information-viewer-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("sensitive-information-search-button"),
    ).toBeVisible();
    await expect(page.getByRole("button", { name: /삭제/ })).toHaveCount(0);
    await expect(page.getByRole("button", { name: /수정/ })).toHaveCount(0);

    const listResponse = await request.get(
      "/api/admin/audit/sensitive-information-access-logs?page=0&size=20&informationType=PERSONAL_INFORMATION&accessResult=SUCCESS",
    );
    expect(listResponse.status()).toBe(200);
    const listBody = await listResponse.json();
    expect(listBody.success).toBe(true);
    expect(listBody.data.size).toBe(20);
    expect(
      listBody.data.logs.every(
        (row: { informationType: string; accessResult: string }) =>
          row.informationType === "PERSONAL_INFORMATION" &&
          row.accessResult === "SUCCESS",
      ),
    ).toBe(true);
    expect(
      listBody.data.logs.every(
        (row: {
          targetScope?: string;
          accessPurpose?: string;
          protectedPlainValue?: string;
        }) =>
          Boolean(row.targetScope) &&
          Boolean(row.accessPurpose) &&
          row.protectedPlainValue === undefined,
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

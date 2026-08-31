import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-29 업무처리 로그 조회", () => {
  test("R09 admin can search immutable business process audit logs", async ({
    page,
    request,
  }) => {
    await loginAsAdmin(page);

    await page.goto("/admin/audit/business-process-logs");
    await expect(
      page.locator('[data-screen-id="SCR-BUSINESS-PROCESS-LOG"]'),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "업무처리 로그" }),
    ).toBeVisible();
    await expect(
      page.getByTestId("business-process-action-type-select"),
    ).toBeVisible();
    await expect(
      page.getByTestId("business-process-target-key-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("business-process-search-button"),
    ).toBeVisible();
    await expect(page.getByRole("button", { name: /재실행/ })).toHaveCount(0);
    await expect(page.getByRole("button", { name: /취소/ })).toHaveCount(0);
    await expect(page.getByRole("button", { name: /삭제/ })).toHaveCount(0);

    const listResponse = await request.get(
      "/api/admin/audit/business-process-logs?page=0&size=20&actionType=UPDATE&resultStatus=SUCCESS",
    );
    expect(listResponse.status()).toBe(200);
    const listBody = await listResponse.json();
    expect(listBody.success).toBe(true);
    expect(listBody.data.size).toBe(20);
    expect(
      listBody.data.logs.every(
        (row: { actionType: string; resultStatus: string }) =>
          row.actionType === "UPDATE" && row.resultStatus === "SUCCESS",
      ),
    ).toBe(true);
    expect(
      listBody.data.logs.every(
        (row: {
          targetKey?: string;
          beforeState?: string;
          afterState?: string;
          requestId?: string;
        }) =>
          Boolean(row.targetKey) &&
          "beforeState" in row &&
          "afterState" in row &&
          Boolean(row.requestId),
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

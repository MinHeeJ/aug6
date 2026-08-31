import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-29 권한변경 로그 조회", () => {
  test("R09 admin can search immutable permission change logs without mutation actions", async ({
    page,
    request,
  }) => {
    await loginAsAdmin(page);

    await page.goto("/admin/audit/permission-change-logs");
    await expect(
      page.locator('[data-screen-id="SCR-PERMISSION-CHANGE-LOG"]'),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "권한변경 로그" }),
    ).toBeVisible();
    await expect(
      page.getByTestId("permission-change-target-type-select"),
    ).toBeVisible();
    await expect(
      page.getByTestId("permission-change-target-id-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("permission-change-approver-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("permission-change-changed-by-input"),
    ).toBeVisible();
    await expect(
      page.getByTestId("permission-change-search-button"),
    ).toBeVisible();
    await expect(page.getByRole("button", { name: /권한 부여/ })).toHaveCount(
      0,
    );
    await expect(page.getByRole("button", { name: /권한 변경/ })).toHaveCount(
      0,
    );
    await expect(page.getByRole("button", { name: /권한 회수/ })).toHaveCount(
      0,
    );
    await expect(page.getByRole("button", { name: /삭제/ })).toHaveCount(0);

    const listResponse = await request.get(
      "/api/admin/audit/permission-change-logs?page=0&size=20&targetType=FUNCTION&approverUserId=1&changedBy=1",
    );
    expect(listResponse.status()).toBe(200);
    const listBody = await listResponse.json();
    expect(listBody.success).toBe(true);
    expect(listBody.data.size).toBe(20);
    expect(
      listBody.data.logs.every(
        (row: {
          targetType: string;
          approverUserId?: number;
          changedBy?: number;
        }) =>
          row.targetType === "FUNCTION" &&
          row.approverUserId === 1 &&
          row.changedBy === 1,
      ),
    ).toBe(true);
    expect(
      listBody.data.logs.every(
        (row: {
          beforeValue?: string;
          afterValue?: string;
          reason?: string;
          changedAt?: string;
        }) =>
          "beforeValue" in row &&
          "afterValue" in row &&
          Boolean(row.reason) &&
          Boolean(row.changedAt),
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

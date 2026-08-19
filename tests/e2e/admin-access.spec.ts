import { expect, test } from "@playwright/test";

const adminRoutes = [
  "/admin/users",
  "/admin/organizations",
  "/admin/roles",
  "/admin/user-roles",
  "/admin/menu-permissions",
  "/admin/menu-structure",
  "/admin/menu-info",
  "/admin/code-groups",
  "/admin/detail-codes",
];

test.describe("seed administrator access", () => {
  test("login screen validates input, rejects invalid credentials, and opens all first-scope routes for admin", async ({
    page,
  }) => {
    await page.goto("/login");
    await expect(page.getByRole("heading", { name: "로그인" })).toBeVisible();

    await page.getByRole("button", { name: "로그인" }).click();
    await expect(page.getByText("사용자 ID를 입력하세요.")).toBeVisible();
    await expect(page.getByText("비밀번호를 입력하세요.")).toBeVisible();

    await page.getByLabel("사용자 ID").fill("admin");
    await page.getByLabel("비밀번호").fill("wrong");
    await page.getByRole("button", { name: "로그인" }).click();
    await expect(
      page.getByText("아이디 또는 비밀번호가 올바르지 않습니다."),
    ).toBeVisible();

    await page.getByLabel("비밀번호").fill("admin");
    await page.getByRole("button", { name: "로그인" }).click();
    await expect(page.getByText("R09 시스템관리자")).toBeVisible();

    for (const route of adminRoutes) {
      await page.goto(route);
      await expect(page.locator("[data-screen-id]")).toBeVisible();
      await expect(page.getByText("권한이 없습니다")).toHaveCount(0);
    }
  });
});

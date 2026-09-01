import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-37 menu search", () => {
  test("search result selection moves to the accessible target route and excludes unauthorized menus", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    const me = await currentUser(page);
    const accessibleLeaves = me.data.menus.flatMap((menu) =>
      collectLeafMenus(menu),
    );
    const target = accessibleLeaves.find(
      (menu) => menu.url === "/admin/batch-results",
    );

    expect(target, "R09 can access batch result menu").toBeTruthy();

    await page.getByTestId("header-menu-search-toggle").click();
    await page.getByTestId("header-menu-search-input").fill("배치");

    await expect(
      page.getByRole("link", { name: /배치 결과 조회/ }),
    ).toBeVisible();
    await expect(page.getByText("급여 관리")).toHaveCount(0);

    await page.getByRole("link", { name: /배치 결과 조회/ }).click();

    await expect(page).toHaveURL(/\/admin\/batch-results$/);
    await expect(page.getByText("권한이 없습니다")).toHaveCount(0);
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

async function currentUser(page: Page) {
  return page.evaluate(async () => {
    const response = await fetch("/api/auth/me", { credentials: "include" });
    if (!response.ok) {
      throw new Error(`/api/auth/me failed with ${response.status}`);
    }
    return response.json() as Promise<{ data: { menus: MenuNode[] } }>;
  });
}

type MenuNode = {
  menuId: number;
  menuName: string;
  url?: string;
  displayOrder: number;
  children: MenuNode[];
};

function collectLeafMenus(menu: MenuNode): MenuNode[] {
  const childLeaves = (menu.children ?? []).flatMap((child) =>
    collectLeafMenus(child),
  );
  return menu.url ? [menu, ...childLeaves] : childLeaves;
}

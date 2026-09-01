import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-37 header navigation reachability", () => {
  test("header hover panel exposes every accessible leaf menu", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    const me = await currentUser(page);
    const topMenus = sortMenus(me.data.menus);
    const leafMenus = topMenus.flatMap((topMenu) => collectLeafMenus(topMenu));

    await expect(page.getByTestId("common-header-nav")).toBeVisible();

    for (const topMenu of topMenus) {
      await page.getByTestId(`header-nav-top-${topMenu.menuId}`).hover();
      await expect(page.getByTestId("header-nav-panel")).toBeVisible();

      for (const leaf of collectLeafMenus(topMenu)) {
        const href = leaf.url ?? "";
        expect(href, `${leaf.menuName} has route`).toMatch(/^\//);
        await expect(
          page.getByTestId(`header-nav-leaf-${leaf.menuId}`),
          `${topMenu.menuName} > ${leaf.menuName}`,
        ).toHaveAttribute("href", href);
      }
    }

    expect(leafMenus.length, "accessible leaf route inventory").toBeGreaterThan(
      0,
    );
  });

  test("header navigation reaches every static accessible route without permission regression", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    const me = await currentUser(page);
    const topMenus = sortMenus(me.data.menus);

    for (const topMenu of topMenus) {
      for (const leaf of collectLeafMenus(topMenu).filter((menu) =>
        isStaticRoute(menu.url),
      )) {
        await page.getByTestId(`header-nav-top-${topMenu.menuId}`).hover();
        await page.getByTestId(`header-nav-leaf-${leaf.menuId}`).click();
        await expect(page).toHaveURL(
          new RegExp(`${escapeRegExp(leaf.url ?? "")}$`),
        );
        await expect(page.getByText("권한이 없습니다")).toHaveCount(0);
        await expect(page.locator("[data-screen-id]").first()).toBeVisible();
      }
    }
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
  const childLeaves = sortMenus(menu.children ?? []).flatMap((child) =>
    collectLeafMenus(child),
  );
  return menu.url ? [menu, ...childLeaves] : childLeaves;
}

function sortMenus(menus: MenuNode[]): MenuNode[] {
  return [...menus].sort(
    (left, right) => left.displayOrder - right.displayOrder,
  );
}

function isStaticRoute(url: string | undefined): url is string {
  return Boolean(url?.startsWith("/") && !url.includes("{"));
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

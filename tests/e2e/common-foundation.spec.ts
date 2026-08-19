import {
  expect,
  test,
  type APIRequestContext,
  type Page,
} from "@playwright/test";

const managementRoutes = [
  {
    route: "/admin/users",
    screenId: "SCR-USER-MGMT",
    lookupApi: "/api/admin/users?page=0&size=5",
  },
  {
    route: "/admin/organizations",
    screenId: "SCR-ORG-MGMT",
    lookupApi: "/api/admin/organizations?page=0&size=5",
  },
  {
    route: "/admin/roles",
    screenId: "SCR-ROLE-MGMT",
    lookupApi: "/api/admin/roles?page=0&size=20",
  },
  {
    route: "/admin/user-roles",
    screenId: "SCR-USER-ROLE-MGMT",
    lookupApi: "/api/admin/user-roles?page=0&size=5",
  },
  {
    route: "/admin/menu-permissions",
    screenId: "SCR-MENU-PERMISSION-MGMT",
    lookupApi: "/api/admin/menu-permissions?page=0&size=5",
  },
  {
    route: "/admin/menu-structure",
    screenId: "SCR-MENU-STRUCTURE-MGMT",
    lookupApi: "/api/admin/menus/tree",
  },
  {
    route: "/admin/menu-info",
    screenId: "SCR-MENU-INFO-MGMT",
    lookupApi: "/api/admin/menus/132/execution",
  },
  {
    route: "/admin/code-groups",
    screenId: "SCR-CODE-GROUP-MGMT",
    lookupApi: "/api/admin/code-groups?page=0&size=5",
  },
  {
    route: "/admin/detail-codes",
    screenId: "SCR-DETAIL-CODE-MGMT",
    lookupApi: "/api/admin/code-groups/COMMON_STATUS/codes?page=0&size=5",
  },
];

test.describe("common foundation integrated smoke", () => {
  test("admin login renders nine management routes and lookup APIs return 2xx", async ({
    page,
    request,
  }) => {
    await loginAsAdmin(page);

    for (const item of managementRoutes) {
      await page.goto(item.route);
      await expect(
        page.locator(`[data-screen-id="${item.screenId}"]`),
      ).toBeVisible();
      await expect(page.getByText("권한이 없습니다")).toHaveCount(0);

      const response = await request.get(item.lookupApi);
      expect(
        response.status(),
        `${item.lookupApi} should be available to seed admin`,
      ).toBeGreaterThanOrEqual(200);
      expect(
        response.status(),
        `${item.lookupApi} should be available to seed admin`,
      ).toBeLessThan(300);
      const body = await response.json();
      expect(body.success).toBe(true);
    }
  });

  test("user-level denied menu is hidden and direct API returns 403", async ({
    page,
    request,
  }) => {
    await loginAsAdmin(page);
    const me = await currentUser(request);
    const codeGroupMenuId = findMenuIdByUrl(
      me.data.menus,
      "/admin/code-groups",
    );
    expect(codeGroupMenuId, "seed menu for /admin/code-groups").toBeTruthy();

    await saveMenuPermission(request, {
      targetType: "USER",
      targetId: String(me.data.userId),
      menuId: codeGroupMenuId,
      accessAllowed: "DENY",
      changeReason: "e2e 권한 숨김 및 403 검증",
    });

    try {
      await page.reload();
      await page.goto("/admin/code-groups");
      await expect(page.getByText("권한이 없습니다")).toBeVisible();

      const denied = await request.get("/api/admin/code-groups?page=0&size=1");
      expect(denied.status()).toBe(403);
      const deniedBody = await denied.json();
      expect(deniedBody.success).toBe(false);
      expect(deniedBody.error.code).toBe("FORBIDDEN");
    } finally {
      await saveMenuPermission(request, {
        targetType: "USER",
        targetId: String(me.data.userId),
        menuId: codeGroupMenuId,
        accessAllowed: "ALLOW",
        changeReason: "e2e 권한 검증 원복",
      });
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

async function currentUser(request: APIRequestContext) {
  const response = await request.get("/api/auth/me");
  expect(response.ok()).toBeTruthy();
  return response.json();
}

async function saveMenuPermission(
  request: APIRequestContext,
  payload: {
    targetType: string;
    targetId: string;
    menuId: number;
    accessAllowed: "ALLOW" | "DENY";
    changeReason: string;
  },
) {
  const response = await request.put("/api/admin/menu-permissions", {
    data: payload,
  });
  expect(
    response.status(),
    `save menu permission ${payload.accessAllowed}`,
  ).toBe(200);
}

type MenuNode = { menuId: number; url?: string; children: MenuNode[] };

function findMenuIdByUrl(nodes: MenuNode[], url: string): number {
  for (const node of nodes) {
    if (node.url === url) return node.menuId;
    const child = findMenuIdByUrl(node.children ?? [], url);
    if (child) return child;
  }
  return 0;
}

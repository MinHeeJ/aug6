import {
  expect,
  test,
  type APIRequestContext,
  type Page,
} from "@playwright/test";

type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: { code: string; message: string; fields?: unknown[] };
  meta?: Record<string, unknown>;
};

type MenuNode = {
  menuId: number;
  menuName: string;
  displayName?: string;
  url?: string | null;
  children?: MenuNode[];
};

type UserRow = {
  name?: string;
  organizationName?: string;
  loginId?: string;
};

type UserSearchResponse = {
  users?: UserRow[];
  rows?: UserRow[];
};

type OrganizationRow = {
  organizationName?: string;
  organizationCode?: string;
};

type CodeGroupRow = {
  groupName?: string;
  groupId?: string;
};

test.describe("BASIC-56 i18n build/runtime smoke", () => {
  test("Docker Compose preview renders English static UI and API-localized sidebar menu names", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    const api = page.context().request;

    const englishMenuResponse = await api.get(
      "/api/admin/menus/localized-tree?lang=en",
    );
    expect(englishMenuResponse.status()).toBe(200);
    const englishMenuBody = (await englishMenuResponse.json()) as ApiResponse<{
      lang: "ko" | "en";
      rows: MenuNode[];
    }>;
    expect(englishMenuBody.success).toBe(true);
    expect(englishMenuBody.data?.lang).toBe("en");

    const englishMenu = findMenuWithEnglishDisplayName(
      englishMenuBody.data?.rows ?? [],
    );
    expect(
      englishMenu,
      "seed menu with a non-Korean English displayName",
    ).toBeTruthy();

    await page.goto("/admin/users");
    await page.getByTestId("header-language-selector").selectOption("en");
    await expect(page.getByTestId("header-language-selector")).toHaveValue(
      "en",
    );
    await expect(
      page.getByRole("heading", { name: "User Management" }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", {
        name: englishMenu!.displayName!,
        exact: true,
      }),
    ).toBeVisible();
    await expect(
      page.getByText(englishMenu!.menuName, { exact: true }),
    ).toHaveCount(0);

    await page.goto("/admin/menu-structure");
    await expect(page.getByTestId("header-language-selector")).toHaveValue(
      "en",
    );
    await expect(
      page.getByRole("button", {
        name: englishMenu!.displayName!,
        exact: true,
      }),
    ).toBeVisible();
  });

  test("English selection keeps user, organization, and code DB values unchanged", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    const api = page.context().request;
    await page.getByTestId("header-language-selector").selectOption("en");

    const userValue = await firstUserDbValue(api);
    await page.goto("/admin/users");
    await expect(page.getByTestId("header-language-selector")).toHaveValue(
      "en",
    );
    await expect(
      page.locator("main").getByText(userValue, { exact: true }).first(),
    ).toBeVisible();
    expect(userValue).not.toMatch(/User Management|Search Criteria|Search/);

    const organizationValue = await firstOrganizationDbValue(api);
    await page.goto("/admin/organizations");
    await expect(page.getByTestId("header-language-selector")).toHaveValue(
      "en",
    );
    await expect(
      page
        .locator("main")
        .getByText(organizationValue, { exact: true })
        .first(),
    ).toBeVisible();

    const codeGroupValue = await firstCodeGroupDbValue(api);
    await page.goto("/admin/code-groups");
    await expect(page.getByTestId("header-language-selector")).toHaveValue(
      "en",
    );
    await expect(
      page.locator("main").getByText(codeGroupValue, { exact: true }).first(),
    ).toBeVisible();
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByTestId("header-language-selector")).toBeVisible();
  await expect(page).toHaveURL(/\/admin\/users/);
}

function flattenMenus(nodes: MenuNode[]): MenuNode[] {
  return nodes.flatMap((node) => [node, ...flattenMenus(node.children ?? [])]);
}

function findMenuWithEnglishDisplayName(
  nodes: MenuNode[],
): MenuNode | undefined {
  return flattenMenus(nodes).find(
    (node) =>
      Boolean(node.displayName?.trim()) &&
      node.displayName !== node.menuName &&
      /[A-Za-z]/.test(node.displayName ?? ""),
  );
}

async function firstUserDbValue(api: APIRequestContext): Promise<string> {
  const response = await api.get("/api/admin/users?page=0&size=5");
  expect(response.status()).toBe(200);
  const body = (await response.json()) as ApiResponse<UserSearchResponse>;
  const row = (body.data?.users ?? body.data?.rows ?? []).find(
    (candidate) => candidate.name?.trim() || candidate.organizationName?.trim(),
  );
  expect(row, "seed user row with DB display values").toBeTruthy();
  return (row?.name?.trim() ||
    row?.organizationName?.trim() ||
    row?.loginId?.trim())!;
}

async function firstOrganizationDbValue(
  api: APIRequestContext,
): Promise<string> {
  const response = await api.get("/api/admin/organizations?page=0&size=5");
  expect(response.status()).toBe(200);
  const body = (await response.json()) as ApiResponse<OrganizationRow[]>;
  const row = (body.data ?? []).find(
    (candidate) =>
      candidate.organizationName?.trim() || candidate.organizationCode?.trim(),
  );
  expect(row, "seed organization row with DB display values").toBeTruthy();
  return (row?.organizationName?.trim() || row?.organizationCode?.trim())!;
}

async function firstCodeGroupDbValue(api: APIRequestContext): Promise<string> {
  const response = await api.get("/api/admin/code-groups?page=0&size=5");
  expect(response.status()).toBe(200);
  const body = (await response.json()) as ApiResponse<CodeGroupRow[]>;
  const row = (body.data ?? []).find(
    (candidate) => candidate.groupName?.trim() || candidate.groupId?.trim(),
  );
  expect(row, "seed code group row with DB display values").toBeTruthy();
  return (row?.groupName?.trim() || row?.groupId?.trim())!;
}

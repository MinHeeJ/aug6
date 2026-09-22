import { expect, test, type Page } from "@playwright/test";

const settingsRoutes = [
  {
    path: "/admin/evaluation-element-management-item-settings",
    title: "평가요소별 관리항목 설정",
    endpoint: "evaluation-element-management-item-settings",
  },
  {
    path: "/admin/participation-allocation-rate-settings",
    title: "참여구분별 배분율 설정",
    endpoint: "participation-allocation-rate-settings",
  },
  {
    path: "/admin/management-item-evaluation-score-settings",
    title: "관리항목별 평가점수 설정",
    endpoint: "management-item-evaluation-score-settings",
  },
] as const;

test("R04 can open all BASIC-70 settings screens in desktop and tablet browser layouts", async ({
  page,
}) => {
  await mockBusinessAdministratorSession(page);

  for (const route of settingsRoutes) {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto(route.path);
    await expect(
      page.getByRole("heading", { name: route.title }),
    ).toBeVisible();
    await expect(
      page.getByTestId(`${testIdPrefix(route.endpoint)}-download-button`),
    ).toBeVisible();
    await expect(page.getByLabel("표시 건수")).toHaveValue("20");

    await page.setViewportSize({ width: 768, height: 1024 });
    await expect(
      page.getByRole("heading", { name: route.title }),
    ).toBeVisible();
    await expect(
      page.getByTestId(`${testIdPrefix(route.endpoint)}-search-button`),
    ).toBeVisible();
  }
});

async function mockBusinessAdministratorSession(page: Page) {
  const menus = settingsRoutes.map((route, index) => ({
    menuId: 710 + index,
    menuName: route.title,
    screenId: `SCR-B70-${index + 1}`,
    url: route.path,
    displayOrder: index + 1,
    children: [],
  }));
  await page.route("**/api/auth/me", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        success: true,
        data: {
          userId: 4,
          loginId: "business-admin",
          employeeNo: "E0004",
          name: "업무담당자",
          roles: ["R04"],
          menus,
        },
        meta: { requestId: "B70-BROWSER-SMOKE" },
      }),
    });
  });
  for (const setting of settingsRoutes) {
    await page.route(`**/api/admin/${setting.endpoint}?**`, async (route) => {
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: {
            [responseKey(setting.endpoint)]: [],
            page: 0,
            pageSize: 20,
            totalElements: 0,
          },
          meta: { requestId: "B70-BROWSER-SMOKE" },
        }),
      });
    });
  }
}

function testIdPrefix(endpoint: string) {
  if (endpoint.startsWith("evaluation-element")) return "element";
  if (endpoint.startsWith("participation")) return "participation";
  return "score";
}

function responseKey(endpoint: string) {
  if (endpoint.startsWith("evaluation-element")) {
    return "evaluationElementManagementItemSettings";
  }
  if (endpoint.startsWith("participation")) {
    return "participationAllocationRateSettings";
  }
  return "managementItemEvaluationScoreSettings";
}

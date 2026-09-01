import { expect, test, type Page, type Response } from "@playwright/test";

type RuntimeBudgetSample = {
  urlPart: string;
  durationMs: number;
};

const pageBudgetBytes = 3 * 1024 * 1024;
const averageQueryBudgetMs = 3_000;
const singleQueryBudgetMs = 5_000;

const listScreens = [
  {
    path: "/admin/korus-faculty-sync",
    screenId: "SCR-KORUS-FACULTY-SYNC",
    pageSizeTestId: "korus-faculty-sync-page-size-select",
    excelTestId: "korus-faculty-sync-excel-button",
    queryPath: "/api/admin/korus-faculty-sync-results",
  },
  {
    path: "/admin/full-time-faculty-statuses",
    screenId: "SCR-FULL-TIME-FACULTY-STATUS",
    pageSizeTestId: "full-time-faculty-status-page-size-select",
    excelTestId: "full-time-faculty-status-excel-button",
    queryPath: "/api/admin/full-time-faculty-statuses",
  },
  {
    path: "/researcher-profiles",
    screenId: "SCR-RESEARCHER-PROFILE-LIST",
    pageSizeTestId: "researcher-profile-list-page-size-select",
    excelTestId: "researcher-profile-list-excel-button",
    queryPath: "/api/researcher-profiles",
  },
  {
    path: "/admin/researcher-profiles/degree-prerequisite-missing",
    screenId: "SCR-DEGREE-PREREQ-MISSING",
    pageSizeTestId: "degree-prerequisite-missing-page-size-select",
    excelTestId: "degree-prerequisite-missing-excel-button",
    queryPath: "/api/admin/researcher-profiles/degree-prerequisite-missing",
  },
  {
    path: "/admin/achievement-data-histories",
    screenId: "SCR-ACHIEVEMENT-DATA-HISTORY",
    pageSizeTestId: "achievement-data-history-page-size-select",
    excelTestId: "achievement-data-history-excel-button",
    queryPath: "/api/admin/achievement-data-histories",
  },
];

test.describe("BASIC-36 cross-cutting verification", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("list routes stay within runtime query and page-weight budgets", async ({
    page,
  }) => {
    const samples: RuntimeBudgetSample[] = [];
    page.on("response", async (response) =>
      collectApiTiming(response, samples),
    );

    for (const screen of listScreens) {
      const response = await page.goto(screen.path);
      expect(response?.ok(), `${screen.path} route should render`).toBeTruthy();
      await expect(
        page.locator(`[data-screen-id=\"${screen.screenId}\"]`),
      ).toBeVisible();
      await expect(page.getByTestId(screen.pageSizeTestId)).toHaveValue("20");
      await expect(page.getByTestId(screen.excelTestId)).toBeVisible();

      const transferBytes = await totalTransferBytes(page);
      expect(
        transferBytes,
        `${screen.path} should stay under 3MB`,
      ).toBeLessThanOrEqual(pageBudgetBytes);
    }

    const matchedSamples = samples.filter((sample) =>
      listScreens.some((screen) => sample.urlPart.includes(screen.queryPath)),
    );
    expect(
      matchedSamples.length,
      "BASIC-36 list API timings should be captured",
    ).toBeGreaterThan(0);
    const average =
      matchedSamples.reduce((sum, sample) => sum + sample.durationMs, 0) /
      matchedSamples.length;
    expect(
      average,
      "average list API response time should be <= 3s",
    ).toBeLessThanOrEqual(averageQueryBudgetMs);
    for (const sample of matchedSamples) {
      expect(
        sample.durationMs,
        `${sample.urlPart} should be <= 5s`,
      ).toBeLessThanOrEqual(singleQueryBudgetMs);
    }
  });

  test("pagination sizes and Excel export scope are explicit per BASIC-36 screen", async ({
    page,
  }) => {
    for (const screen of listScreens) {
      await page.goto(screen.path);
      const pageSize = page.getByTestId(screen.pageSizeTestId);
      await expect(pageSize).toBeVisible();
      await expect(pageSize.locator("option")).toHaveText([
        "20건",
        "50건",
        "100건",
      ]);
      await expect(page.getByTestId(screen.excelTestId)).toHaveText(
        /엑셀 내려받기/,
      );
    }

    await page.goto("/researcher-profiles/E1001");
    await expect(
      page.locator('[data-screen-id="SCR-RESEARCHER-PROFILE-DETAIL"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("researcher-profile-save-button"),
    ).toBeVisible();
    await expect(page.getByText("엑셀 내려받기")).toHaveCount(0);
  });

  test("Docker preview health endpoint is proxied through /api/health", async ({
    page,
  }) => {
    const response = await page.request.get("/api/health");
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.success).toBe(true);
    expect(body.data.status).toBe("UP");
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

async function collectApiTiming(
  response: Response,
  samples: RuntimeBudgetSample[],
) {
  const url = response.url();
  if (!url.includes("/api/")) return;
  const timing = response.request().timing();
  const durationMs = timing.responseEnd > 0 ? timing.responseEnd : 0;
  samples.push({ urlPart: new URL(url).pathname, durationMs });
}

async function totalTransferBytes(page: Page) {
  return page.evaluate(() => {
    return performance
      .getEntriesByType("resource")
      .reduce(
        (sum, entry) =>
          sum + ((entry as PerformanceResourceTiming).transferSize || 0),
        0,
      );
  });
}

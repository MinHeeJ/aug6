import { expect, test, type Page } from "@playwright/test";

type ApiResult<T> = {
  status: number;
  body: {
    success: boolean;
    data?: T;
    error?: { code: string; message: string };
  };
  elapsedMs: number;
};

const basic48Routes = [
  {
    route: "/admin/evaluation-snapshots",
    screenId: "SCR-EVAL-SNAPSHOT-HISTORY",
    api: "/api/business/evaluation-snapshots?evaluationYear=2026&page=0&size=20",
    detailApi: "/api/business/evaluation-snapshots/B48-SNAPSHOT-001",
    excelButton: "evaluation-snapshot-excel-button",
  },
  {
    route: "/admin/score-calculation-histories",
    screenId: "SCR-SCORE-CALC-HISTORY",
    api: "/api/business/score-calculation-histories?evaluationYear=2026&page=0&size=20",
    detailApi: "/api/business/score-calculation-histories/B48-CALC-001",
    excelButton: "score-calculation-excel-button",
  },
  {
    route: "/admin/score-adjustment-histories",
    screenId: "SCR-SCORE-ADJUSTMENT-HISTORY",
    api: "/api/business/score-adjustment-histories?evaluationYear=2026&page=0&size=20",
    detailApi: "/api/business/score-adjustment-histories/B48-ADJ-001",
    excelButton: "score-adjustment-excel-button",
  },
  {
    route: "/admin/score-recalculation-histories",
    screenId: "SCR-SCORE-RECALCULATION-HISTORY",
    api: "/api/business/score-recalculation-histories?evaluationYear=2026&page=0&size=20",
    detailApi: "/api/business/score-recalculation-histories/B48-RECALC-001",
    excelButton: "score-recalculation-excel-button",
  },
] as const;

const viewportSmokeMatrix = [
  { label: "Edge desktop", width: 1280, height: 800 },
  { label: "Chrome desktop", width: 1280, height: 800 },
  { label: "Safari desktop", width: 1440, height: 900 },
  { label: "Opera desktop", width: 1280, height: 800 },
  { label: "Whale desktop", width: 1280, height: 800 },
  { label: "iPadOS tablet", width: 1024, height: 768 },
  { label: "Android tablet", width: 900, height: 720 },
] as const;

test.describe("BASIC-48 cross-cutting verification", () => {
  test("401 envelope is returned for unauthenticated direct API calls", async ({
    request,
  }) => {
    for (const item of basic48Routes) {
      const response = await request.get(item.api);
      expect(response.status(), item.api).toBe(401);
      const body = await response.json();
      expect(body.success, item.api).toBe(false);
      expect(body.error.code, item.api).toBe("UNAUTHENTICATED");
    }
  });

  test("routes satisfy accessibility smoke and browser viewport compatibility", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    for (const target of viewportSmokeMatrix) {
      await page.setViewportSize({
        width: target.width,
        height: target.height,
      });
      for (const item of basic48Routes) {
        await page.goto(item.route);
        await expect(
          page.locator(`[data-screen-id="${item.screenId}"]`),
          `${target.label} ${item.route}`,
        ).toBeVisible();
        await expect(page.getByText("권한이 없습니다"), item.route).toHaveCount(
          0,
        );
        await expect(
          page.getByRole("button", { name: "조회" }),
          item.route,
        ).toBeVisible();
        await expect(
          page.getByTestId(item.excelButton),
          item.route,
        ).toBeVisible();
      }
    }
  });

  test("query and detail APIs respond within 3s average and 5s per request without leaking sensitive errors", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    const timings: number[] = [];
    for (const item of basic48Routes) {
      for (const api of [item.api, item.detailApi]) {
        const result = await getApi<unknown>(page, api);
        timings.push(result.elapsedMs);
        expect(result.status, api).toBe(200);
        expect(result.body.success, api).toBe(true);
        expect(result.elapsedMs, api).toBeLessThan(5000);
        const serialized = JSON.stringify(result.body);
        expect(serialized, api).not.toContain("Exception");
        expect(serialized, api).not.toContain("password");
        expect(serialized, api).not.toContain("secret");
      }
    }
    const average =
      timings.reduce((sum, value) => sum + value, 0) / timings.length;
    expect(average).toBeLessThan(3000);
  });

  test("read-only screens do not expose score generation, score mutation, recalculation execution, or finalization CTAs", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    for (const item of basic48Routes) {
      await page.goto(item.route);
      await expect(
        page.locator(`[data-screen-id="${item.screenId}"]`),
      ).toBeVisible();
      await expect(
        page.getByRole("button", {
          name: /생성|수정|삭제|재계산 실행|확정|확정취소/,
        }),
      ).toHaveCount(0);
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

async function getApi<T>(page: Page, path: string): Promise<ApiResult<T>> {
  return page.evaluate(async (url) => {
    const startedAt = performance.now();
    const response = await fetch(url, { credentials: "include" });
    const elapsedMs = performance.now() - startedAt;
    return { status: response.status, body: await response.json(), elapsedMs };
  }, path) as Promise<ApiResult<T>>;
}

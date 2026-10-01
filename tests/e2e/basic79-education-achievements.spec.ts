import { expect, test, type Page } from "@playwright/test";

type ApiEnvelope<T> = {
  success: boolean;
  data?: T;
  error?: { code: string; message: string };
  meta?: { requestId?: string };
};

type ApiResult<T> = {
  status: number;
  body: ApiEnvelope<T>;
  elapsedMs: number;
};

const achievementRoutes = [
  {
    api: "/api/business/lecture-evaluation-achievements?page=0&size=20",
    screenId: "SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT",
    route: "/achievements/education/lecture-evaluations",
  },
  {
    api: "/api/business/lecture-achievements?page=0&size=20",
    screenId: "SCR-LECTURE-ACHIEVEMENT-MGMT",
    route: "/achievements/education/lecture-achievements",
  },
  {
    api: "/api/business/student-guidance-achievements?page=0&size=20",
    screenId: "SCR-STUDENT-GUIDANCE-ACHIEVEMENT-MGMT",
    route: "/achievements/education/student-guidance-uploads",
  },
  {
    api: "/api/business/degree-completion-achievements?page=0&size=20",
    screenId: "SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT",
    route: "/achievements/education/masters-doctoral-graduations",
  },
] as const;

const responsiveViewports = [
  { height: 900, name: "desktop", width: 1440 },
  { height: 768, name: "tablet", width: 1024 },
] as const;

const teacherCredentials = configuredCredentials("BASIC79_R01");
const departmentChairCredentials = configuredCredentials("BASIC79_R02");
const evaluatorCredentials = configuredCredentials("BASIC79_R04");
const uploadOperatorCredentials = configuredCredentials("BASIC79_R07");

/**
 * BASIC-79 integration checks intentionally use only the seeded, documented administrator account
 * by default. Role-specific checks activate when the runner supplies the R01/R02/R04/R07 fixture
 * credentials.
 */
test.describe("BASIC-79 education achievement cross-cutting verification", () => {
  test("unauthenticated achievement APIs preserve the 401 envelope without sensitive detail", async ({
    request,
  }) => {
    for (const target of achievementRoutes) {
      const response = await request.get(target.api);
      expect(response.status(), target.api).toBe(401);
      const body = (await response.json()) as ApiEnvelope<unknown>;
      expect(body.success, target.api).toBe(false);
      expect(body.error?.code, target.api).toBe("UNAUTHENTICATED");
      assertSafeError(JSON.stringify(body), target.api);
    }
  });

  test("R09 direct achievement API requests remain forbidden and return the safe error envelope", async ({
    page,
  }) => {
    await login(page, { loginId: "admin", password: "admin" }, "R09");
    for (const target of achievementRoutes) {
      const result = await getApi<unknown>(page, target.api);
      expect(result.status, target.api).toBe(403);
      expect(result.body.success, target.api).toBe(false);
      expect(result.body.error?.code, target.api).toBe("FORBIDDEN");
      assertSafeError(JSON.stringify(result.body), target.api);
    }
  });

  test("R01, R02, and R04 can reach their authorized read APIs", async ({
    page,
  }) => {
    test.skip(
      !teacherCredentials ||
        !departmentChairCredentials ||
        !evaluatorCredentials,
      "Runner must provide BASIC79_R01_*, BASIC79_R02_*, and BASIC79_R04_* credentials.",
    );

    for (const [credentials, role] of [
      [teacherCredentials!, "R01"],
      [departmentChairCredentials!, "R02"],
      [evaluatorCredentials!, "R04"],
    ] as const) {
      await login(page, credentials, role);
      for (const target of achievementRoutes) {
        const result = await getApi<unknown>(page, target.api);
        expect(result.status, `${role} ${target.api}`).toBe(200);
        expect(result.body.success, `${role} ${target.api}`).toBe(true);
      }
    }
  });

  test("unauthorized browser routes render a permission state at desktop and tablet widths", async ({
    page,
  }) => {
    await login(page, { loginId: "admin", password: "admin" }, "R09");
    for (const viewport of responsiveViewports) {
      await page.setViewportSize({
        width: viewport.width,
        height: viewport.height,
      });
      for (const target of achievementRoutes) {
        await page.goto(target.route);
        await expect(
          page.getByText(/권한이 없습니다|권한이 필요합니다/),
          `${viewport.name} ${target.route}`,
        ).toBeVisible();
      }
    }
  });

  test("R01 can read only its scoped achievement rows and confirmed rows reject mutation with 409", async ({
    page,
  }) => {
    test.skip(
      !teacherCredentials,
      "Runner must provide BASIC79_R01_LOGIN and BASIC79_R01_PASSWORD.",
    );
    await login(page, teacherCredentials!, "R01");

    const timings: number[] = [];
    for (const target of achievementRoutes) {
      const result = await getApi<unknown>(page, target.api);
      timings.push(result.elapsedMs);
      expect(result.status, target.api).toBe(200);
      expect(result.body.success, target.api).toBe(true);
      expect(result.elapsedMs, target.api).toBeLessThan(5_000);
      assertSafeError(JSON.stringify(result.body), target.api);
    }
    expect(average(timings)).toBeLessThan(3_000);

    const lockedUpdate = await postApi<unknown>(
      page,
      "/api/business/lecture-evaluation-achievements",
      {
        achievementId: 790003,
        managementItemCode: "EDU-LECTURE-EVALUATION",
        occurredDate: "2026-04-20",
        changeReason: "BASIC-79 confirmed-row regression check",
      },
    );
    expect(lockedUpdate.status).toBe(409);
    expect(lockedUpdate.body.success).toBe(false);
    expect(lockedUpdate.body.error?.code).toBe("CONFLICT");
    expect(lockedUpdate.body.error?.message).toContain("CONFIRMED_DATA_LOCKED");

    const readback = await getApi<{
      achievements: Array<{
        achievementId: number;
        certificationStatus: string;
      }>;
    }>(
      page,
      "/api/business/lecture-evaluation-achievements?managementNo=B77-LE-001-03&page=0&size=20",
    );
    expect(readback.status).toBe(200);
    const lockedRow = readback.body.data?.achievements.find(
      (row) => row.achievementId === 790003,
    );
    expect(lockedRow?.certificationStatus).toBe("EVALUATION_CONFIRMED");
  });

  test("R01 transition returns the caller request identifier and R07 owns only the Excel workflow", async ({
    page,
  }) => {
    test.skip(
      !teacherCredentials || !uploadOperatorCredentials,
      "Runner must provide BASIC79_R01_* and BASIC79_R07_* credentials.",
    );

    await login(page, teacherCredentials!, "R01");
    const transition = await postApi<unknown>(
      page,
      "/api/business/lecture-evaluation-achievements/790001/transitions",
      {
        actionType: "SUBMIT",
        changeReason: "BASIC-79 request identifier regression check",
      },
      { "X-Request-Id": "basic79-cross-cutting-request-id" },
    );
    expect(transition.status).toBe(200);
    expect(transition.body.success).toBe(true);
    expect(transition.body.meta?.requestId).toBe(
      "basic79-cross-cutting-request-id",
    );

    await login(page, uploadOperatorCredentials!, "R07");
    const uploadHistory = await getApi<unknown>(
      page,
      "/api/business/student-guidance-achievements/excel-upload-histories",
    );
    expect(uploadHistory.status).toBe(200);
    expect(uploadHistory.body.success).toBe(true);

    const forbiddenRead = await getApi<unknown>(
      page,
      "/api/business/student-guidance-achievements?page=0&size=20",
    );
    expect(forbiddenRead.status).toBe(403);
    expect(forbiddenRead.body.error?.code).toBe("FORBIDDEN");
  });
});

type Credentials = { loginId: string; password: string };

function configuredCredentials(prefix: string): Credentials | undefined {
  const loginId = process.env[`${prefix}_LOGIN`];
  const password = process.env[`${prefix}_PASSWORD`];
  return loginId && password ? { loginId, password } : undefined;
}

async function login(page: Page, credentials: Credentials, role: string) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill(credentials.loginId);
  await page.getByLabel("비밀번호").fill(credentials.password);
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText(new RegExp(role))).toBeVisible();
}

async function getApi<T>(page: Page, path: string): Promise<ApiResult<T>> {
  return page.evaluate(async (url) => {
    const startedAt = performance.now();
    const response = await fetch(url, { credentials: "include" });
    return {
      body: await response.json(),
      elapsedMs: performance.now() - startedAt,
      status: response.status,
    };
  }, path) as Promise<ApiResult<T>>;
}

async function postApi<T>(
  page: Page,
  path: string,
  payload: unknown,
  headers: Record<string, string> = {},
): Promise<ApiResult<T>> {
  return page.evaluate(
    async ({ body, requestHeaders, url }) => {
      const startedAt = performance.now();
      const response = await fetch(url, {
        body: JSON.stringify(body),
        credentials: "include",
        headers: { "Content-Type": "application/json", ...requestHeaders },
        method: "POST",
      });
      return {
        body: await response.json(),
        elapsedMs: performance.now() - startedAt,
        status: response.status,
      };
    },
    { body: payload, requestHeaders: headers, url: path },
  ) as Promise<ApiResult<T>>;
}

function average(values: number[]): number {
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function assertSafeError(serialized: string, label: string) {
  expect(serialized, label).not.toContain("Exception");
  expect(serialized, label).not.toContain("password");
  expect(serialized, label).not.toContain("secret");
  expect(serialized, label).not.toContain("attachment_ref");
}

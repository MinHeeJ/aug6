import { expect, test, type Browser, type Page } from "@playwright/test";

type Achievement = {
  achievementId: number;
  certificationStatus: string;
};

type ApiEnvelope<T> = {
  success: boolean;
  data: T;
  error?: { code: string; message: string };
  meta?: { requestId?: string };
};

type FetchOptions = {
  method?: string;
  headers?: Record<string, string>;
  body?: string;
};

/**
 * Cross-browser and role-handoff regression for the education-achievement workflow.
 * It deliberately uses real browser sessions and HTTP boundaries rather than page mocks.
 */
test.describe("BASIC-73 Phase 5 cross-cutting verification", () => {
  test("education achievement list renders within three seconds on desktop and tablet without leaking an internal error", async ({
    page,
  }) => {
    await login(page, "teacher", "teacher");

    for (const viewport of [
      { name: "desktop", width: 1440, height: 900 },
      { name: "tablet", width: 768, height: 1024 },
    ]) {
      await page.setViewportSize({
        width: viewport.width,
        height: viewport.height,
      });
      const startedAt = Date.now();
      await page.goto("/faculty/education/lecture-evaluation-achievements");
      await expect(page.getByTestId("lecture-evaluation-page")).toBeVisible();
      await expect(page.getByText("Unexpected system error")).toHaveCount(0);
      await expect(page.getByText("Internal Server Error")).toHaveCount(0);
      expect(
        Date.now() - startedAt,
        `${viewport.name} list rendering time`,
      ).toBeLessThan(3000);
      await expect(page.locator("body")).toHaveJSProperty(
        "scrollWidth",
        viewport.width,
      );
    }
  });

  test("unauthenticated education API calls return a safe error envelope rather than implementation details", async ({
    request,
  }) => {
    const response = await request.get(
      "/api/business/education-achievements?achievementType=LECTURE_EVALUATION&page=0&size=20",
    );
    expect(response.status()).toBe(401);
    const body = (await response.json()) as ApiEnvelope<never>;
    expect(body.success).toBe(false);
    expect(body.error?.code).toBe("UNAUTHENTICATED");
    expect(body.error?.message).not.toMatch(
      /exception|postgres|sql|stack trace/i,
    );
  });

  test("R01 submits, R02 confirms, R04 certifies, rejection can be resubmitted, and confirmed data cannot mutate", async ({
    browser,
  }) => {
    const r01 = await authenticatedPage(browser, "teacher", "teacher");
    const r02 = await authenticatedPage(
      browser,
      "department-chair",
      "department-chair",
    );
    const r04 = await authenticatedPage(
      browser,
      "faculty-support",
      "faculty-support",
    );

    try {
      const created = await createLectureEvaluation(r01, "B73-PHASE5-APPROVAL");
      await transition(r01, created.achievementId, "SUBMIT", "R01 제출");
      await transition(r02, created.achievementId, "CONFIRM", "R02 확인");
      const certified = await transition(
        r04,
        created.achievementId,
        "CERTIFY",
        "R04 인증",
      );
      expect(certified.data.certificationStatus).toBe("CERTIFIED");

      const rejected = await createLectureEvaluation(
        r01,
        "B73-PHASE5-RESUBMIT",
      );
      await transition(r01, rejected.achievementId, "SUBMIT", "R01 제출");
      const rejectedByChair = await transition(
        r02,
        rejected.achievementId,
        "REJECT",
        "보완 후 재제출하세요.",
      );
      expect(rejectedByChair.data.certificationStatus).toBe(
        "DEPARTMENT_REJECTED",
      );
      const resubmitted = await transition(
        r01,
        rejected.achievementId,
        "SUBMIT",
        "보완하여 재제출",
      );
      expect(resubmitted.data.certificationStatus).toBe("SUBMITTED");

      const confirmed = await listConfirmedLectureAchievement(r01);
      const updateResponse = await api(
        r01,
        "/api/business/education-achievements",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            achievementId: confirmed.achievementId,
            achievementType: "LECTURE_ACHIEVEMENT",
            managementItemCode: "LOCKED-UPDATE-ATTEMPT",
            occurrenceDate: "2026-03-31",
          }),
        },
      );
      expect(updateResponse.status).toBe(409);
      expect(updateResponse.body.success).toBe(false);
      expect(updateResponse.body.error?.code).toBe("CONFIRMED_DATA_LOCKED");

      const deleteResponse = await api(
        r01,
        `/api/business/education-achievements/${encodeURIComponent(String(confirmed.achievementId))}?deleteReason=${encodeURIComponent("평가확정 삭제 시도")}`,
        { method: "DELETE" },
      );
      expect(deleteResponse.status).toBe(409);
      expect(deleteResponse.body.success).toBe(false);
      expect(deleteResponse.body.error?.code).toBe("CONFIRMED_DATA_LOCKED");
    } finally {
      await Promise.all([
        r01.context().close(),
        r02.context().close(),
        r04.context().close(),
      ]);
    }
  });
});

async function authenticatedPage(
  browser: Browser,
  loginId: string,
  password: string,
) {
  const context = await browser.newContext();
  const page = await context.newPage();
  await login(page, loginId, password);
  return page;
}

async function login(page: Page, loginId: string, password: string) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill(loginId);
  await page.getByLabel("비밀번호").fill(password);
  await page.getByRole("button", { name: "로그인" }).click();
}

async function createLectureEvaluation(page: Page, managementItemCode: string) {
  const result = await api<ApiEnvelope<Achievement>>(
    page,
    "/api/business/education-achievements",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Request-Id": `B73-${managementItemCode}`,
      },
      body: JSON.stringify({
        achievementType: "LECTURE_EVALUATION",
        managementItemCode,
        occurrenceDate: "2026-03-30",
      }),
    },
  );
  expect(result.status).toBe(200);
  expect(result.body.success).toBe(true);
  return result.body.data;
}

async function transition(
  page: Page,
  achievementId: number,
  actionType: string,
  opinion: string,
) {
  const result = await api<ApiEnvelope<Achievement>>(
    page,
    `/api/business/education-achievements/${encodeURIComponent(String(achievementId))}/transition`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ actionType, opinion }),
    },
  );
  expect(result.status).toBe(200);
  expect(result.body.success).toBe(true);
  return result.body;
}

async function listConfirmedLectureAchievement(page: Page) {
  const result = await api<ApiEnvelope<{ items: Achievement[] }>>(
    page,
    "/api/business/education-achievements?achievementType=LECTURE_ACHIEVEMENT&page=0&size=20",
  );
  expect(result.status).toBe(200);
  const confirmed = result.body.data.items.find(
    (item) => item.certificationStatus === "EVALUATION_CONFIRMED",
  );
  expect(confirmed, "evaluation-confirmed fixture").toBeTruthy();
  return confirmed as Achievement;
}

async function api<T>(page: Page, path: string, init: FetchOptions = {}) {
  return page.evaluate(
    async ({ path: requestPath, requestInit }) => {
      const response = await fetch(requestPath, {
        ...requestInit,
        credentials: "include",
      });
      const body = await response.json();
      return { status: response.status, body };
    },
    { path, requestInit: init },
  ) as Promise<{ status: number; body: T }>;
}

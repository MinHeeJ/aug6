import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-43 이의신청 의견 관리", () => {
  test("R09 can render route, process fixture, and re-query processed history", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/objection-opinions");
    await expect(
      page.locator('[data-screen-id="SCR-OBJECTION-OPINION-MGMT"]'),
    ).toBeVisible();
    await expect(page.getByText("권한이 없습니다")).toHaveCount(0);
    await expect(page.getByTestId("objection-opinion-size-select")).toHaveValue(
      "20",
    );

    const before = await listObjectionOpinions(page);
    expect(before.success).toBe(true);
    expect(before.data.size).toBe(20);
    expect(
      before.data.opinions.length,
      "BASIC-43 fixture target is required for T023",
    ).toBeGreaterThan(0);

    const target = before.data.opinions[0];
    expect(target.applicantOpinionSnapshot).toBeTruthy();
    expect(target.objectionContentSnapshot).toBeTruthy();

    const transition = await page.evaluate(async (targetId) => {
      const response = await fetch(
        `/api/business/objection-opinions/${encodeURIComponent(String(targetId))}/transition`,
        {
          method: "POST",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            decisionResult: "ACCEPTED",
            reviewerOpinion: "Playwright 이의신청 의견 인용",
          }),
        },
      );
      return { status: response.status, body: await response.json() };
    }, target.objectionId);

    expect(transition.status).toBe(200);
    expect(transition.body.success).toBe(true);
    expect(transition.body.data.decisionResult).toBe("ACCEPTED");
    expect(transition.body.data.processedBy).toBeTruthy();
    expect(transition.body.data.processedAt).toBeTruthy();

    const after = await listObjectionOpinions(page);
    const processed = after.data.opinions.find(
      (row) => row.objectionId === target.objectionId,
    );
    expect(processed).toBeTruthy();
    expect(processed?.decisionResult).toBe("ACCEPTED");
    expect(processed?.reviewerOpinion ?? "").toContain(
      "Playwright 이의신청 의견 인용",
    );
    expect(processed?.processedBy).toBeTruthy();
    expect(processed?.processedAt).toBeTruthy();
  });

  test("non-R09 direct route shows permission denied state", async ({
    page,
  }) => {
    await loginAsTeacher(page);
    await page.goto("/admin/objection-opinions");
    await expect(page.getByText("권한이 없습니다")).toBeVisible();
  });
});

async function listObjectionOpinions(page: Page) {
  return page.evaluate(async () => {
    const response = await fetch(
      "/api/business/objection-opinions?page=0&size=20",
      {
        credentials: "include",
      },
    );
    return response.json();
  }) as Promise<{
    success: boolean;
    data: {
      opinions: Array<{
        objectionId: number;
        applicantUserId: number;
        applicantOpinionSnapshot: string;
        objectionContentSnapshot: string;
        reviewerOpinion?: string;
        decisionResult: string;
        processedBy?: number;
        processedAt?: string;
      }>;
      size: number;
    };
  }>;
}

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

async function loginAsTeacher(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("teacher");
  await page.getByLabel("비밀번호").fill("teacher");
  await page.getByRole("button", { name: "로그인" }).click();
}

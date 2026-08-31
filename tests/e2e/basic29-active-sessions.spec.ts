import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-29 접속현황 조회 및 강제종료", () => {
  test("R09 admin can list active sessions and force terminate with a required reason", async ({
    page,
    request,
  }) => {
    await loginAsAdmin(page);

    await page.goto("/admin/security/active-sessions");
    await expect(
      page.locator('[data-screen-id="SCR-ACTIVE-SESSION-STATUS"]'),
    ).toBeVisible();
    await expect(page.getByText("접속현황 조회")).toBeVisible();

    const listResponse = await request.get(
      "/api/admin/security/active-sessions?page=0&size=20",
    );
    expect(listResponse.status()).toBe(200);
    const listBody = await listResponse.json();
    expect(listBody.success).toBe(true);
    expect(
      listBody.data.sessions.every(
        (row: { status: string }) => row.status === "ACTIVE",
      ),
    ).toBe(true);

    const target = listBody.data.sessions.find(
      (row: { sessionId: string }) =>
        row.sessionId === "SEED-SESSION-ACTIVE-001",
    );
    test.skip(
      !target,
      "SEED-SESSION-ACTIVE-001 fixture is already terminated in this environment",
    );

    const validation = await request.post(
      `/api/admin/security/active-sessions/${encodeURIComponent(target.sessionId)}/terminate`,
      { data: {} },
    );
    expect(validation.status()).toBe(400);
    const validationBody = await validation.json();
    expect(
      validationBody.error.fields.some(
        (field: { field: string }) => field.field === "reason",
      ),
    ).toBe(true);

    const terminate = await request.post(
      `/api/admin/security/active-sessions/${encodeURIComponent(target.sessionId)}/terminate`,
      {
        headers: { "X-Request-Id": "REQ-E2E-ACTIVE-SESSION" },
        data: { reason: "e2e 접속현황 강제종료 검증" },
      },
    );
    expect(terminate.status()).toBe(200);
    const terminatedBody = await terminate.json();
    expect(terminatedBody.data.status).toBe("TERMINATED");
    expect(terminatedBody.meta.requestId).toBe("REQ-E2E-ACTIVE-SESSION");

    const after = await request.get(
      "/api/admin/security/active-sessions?page=0&size=20",
    );
    const afterBody = await after.json();
    expect(
      afterBody.data.sessions.some(
        (row: { sessionId: string }) => row.sessionId === target.sessionId,
      ),
    ).toBe(false);
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

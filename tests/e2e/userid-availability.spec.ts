import { expect, test } from "@playwright/test";

// Uses the real app/API and the approved testuser1 seed, not request interception.
// The wiring owner must register the public /signup route and login entry before this browser check.
test.describe("anonymous user ID availability", () => {
  test("login entry reaches focus-out checks for available and reserved IDs", async ({
    page,
  }) => {
    await page.goto("/login");
    await page.getByRole("link", { name: "회원가입", exact: true }).click();
    await expect(page).toHaveURL(/\/signup$/);
    await expect(page.getByTestId("signup-page")).toBeVisible();
    const id = page.getByLabel("아이디", { exact: true });
    const next = page.getByLabel("비밀번호", { exact: true });
    const candidate = await page.evaluate(
      () => `avail${crypto.randomUUID().replaceAll("-", "").slice(0, 12)}`,
    );
    await id.fill(candidate);
    const availableResponse = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return (
        url.pathname === "/api/v1/auth/check-userid" &&
        url.searchParams.get("userId") === candidate &&
        response.request().method() === "GET"
      );
    });
    await next.focus();
    const available = await availableResponse;
    expect(available.status()).toBe(200);
    expect((await available.json()).data).toEqual({ available: true });
    expect(available.headers()["set-cookie"]).toBeUndefined();
    await expect(
      page.getByText("사용 가능한 아이디입니다.", { exact: true }),
    ).toBeVisible();

    await id.fill("testuser1");
    await expect(
      page.getByText("사용 가능한 아이디입니다.", { exact: true }),
    ).toHaveCount(0);
    const reservedResponse = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return (
        url.pathname === "/api/v1/auth/check-userid" &&
        url.searchParams.get("userId") === "testuser1" &&
        response.request().method() === "GET"
      );
    });
    await next.focus();
    const reserved = await reservedResponse;
    expect(reserved.status()).toBe(200);
    expect((await reserved.json()).data).toEqual({ available: false });
    expect(reserved.headers()["set-cookie"]).toBeUndefined();
    await expect(
      page.getByText("이미 사용 중인 아이디입니다.", { exact: true }),
    ).toBeVisible();
    const session = await page.context().request.get("/api/auth/me");
    expect(session.status()).toBe(401);
  });

  test("invalid focus-out shows a field error and API validation remains enforced", async ({
    page,
  }) => {
    await page.goto("/signup");
    const id = page.getByLabel("아이디", { exact: true });
    await id.fill("1abc");
    await page.getByLabel("비밀번호", { exact: true }).focus();
    await expect(id).toHaveAttribute("aria-invalid", "true");
    await expect(
      page.getByText(
        "아이디는 영문 소문자로 시작하는 영문 소문자와 숫자 4~20자여야 합니다.",
      ),
    ).toBeVisible();
    await expect(
      page.getByText("사용 가능한 아이디입니다.", { exact: true }),
    ).toHaveCount(0);
    const response = await page
      .context()
      .request.get("/api/v1/auth/check-userid", {
        params: { userId: "1abc" },
      });
    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.success).toBe(false);
    expect(body.error.code).toBe("VALIDATION_ERROR");
    expect(body.error.fields).toEqual([
      expect.objectContaining({ field: "userId", message: expect.any(String) }),
    ]);
    expect(response.headers()["set-cookie"]).toBeUndefined();
  });
});

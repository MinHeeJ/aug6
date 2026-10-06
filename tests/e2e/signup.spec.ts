import { expect, test, type Page } from "@playwright/test";

async function newIdentity(page: Page) {
  return page.evaluate(
    () => `signup${crypto.randomUUID().replaceAll("-", "").slice(0, 12)}`,
  );
}

async function fillSignup(
  page: Page,
  id: string,
  email: string,
  confirm = "TestPass9!",
) {
  await page.getByLabel("아이디", { exact: true }).fill(id);
  await page.getByLabel("비밀번호", { exact: true }).fill("TestPass9!");
  await page.getByLabel("비밀번호 확인", { exact: true }).fill(confirm);
  await page.getByLabel("이메일", { exact: true }).fill(email);
}

// Real HTTP/browser scenarios; no local repository files or production seed credentials are loaded.
// The runner supplies a disposable application/DB and merges public route/link registrations first.
test.describe("anonymous signup", () => {
  test("login entry reaches signup, acknowledges creation and returns to login without a session", async ({
    page,
  }) => {
    await page.goto("/login");
    await page.getByRole("link", { name: "회원가입", exact: true }).click();
    await expect(page).toHaveURL(/\/signup$/);
    await expect(page.getByTestId("signup-page")).toBeVisible();
    const id = await newIdentity(page);
    await fillSignup(page, id, `${id}@example.test`);
    await expect(page.getByText("비밀번호 규칙을 충족합니다.")).toBeVisible();
    const post = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/v1/auth/signup") &&
        response.request().method() === "POST",
    );
    const acknowledgement = page.waitForEvent("dialog").then(async (dialog) => {
      const message = dialog.message();
      await dialog.accept();
      return message;
    });
    await page.getByRole("button", { name: "가입하기", exact: true }).click();
    expect(await acknowledgement).toBe("가입이 완료되었습니다.");
    const created = await post;
    expect(created.status()).toBe(201);
    expect((await created.json()).data).toEqual({
      userId: id,
      message: "가입이 완료되었습니다.",
    });
    expect(created.headers()["set-cookie"]).toBeUndefined();
    await expect(page).toHaveURL(/\/login$/);
    const session = await page.context().request.get("/api/auth/me");
    expect(session.status()).toBe(401);
  });

  test("mismatch renders field error and duplicate email remains a conflict", async ({
    page,
  }) => {
    await page.goto("/signup");
    const id = await newIdentity(page);
    await fillSignup(page, id, `${id}@example.test`, "different");
    await page.getByRole("button", { name: "가입하기", exact: true }).click();
    await expect(
      page.getByLabel("비밀번호 확인", { exact: true }),
    ).toHaveAttribute("aria-invalid", "true");
    await expect(page.getByRole("status")).toHaveText(
      "비밀번호와 비밀번호 확인이 일치하지 않습니다.",
    );
    await expect(page).toHaveURL(/\/signup$/);

    const existing = await newIdentity(page);
    const seed = await page.context().request.post("/api/v1/auth/signup", {
      data: {
        userId: existing,
        password: "TestPass9!",
        passwordConfirm: "TestPass9!",
        email: `${existing}@example.test`,
      },
    });
    expect(seed.status()).toBe(201);
    await fillSignup(page, id, `${existing}@EXAMPLE.TEST`);
    await page.getByRole("button", { name: "가입하기", exact: true }).click();
    await expect(page.getByLabel("이메일", { exact: true })).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    await expect(page.getByRole("status")).toHaveText(
      "이미 등록된 이메일입니다.",
    );
    await expect(page).toHaveURL(/\/signup$/);
  });

  test("focus-out availability is API backed and return link reaches login", async ({
    page,
  }) => {
    await page.goto("/signup");
    const id = await newIdentity(page);
    await page.getByLabel("아이디", { exact: true }).fill(id);
    await page.getByLabel("비밀번호", { exact: true }).focus();
    await expect(page.getByText("사용 가능한 아이디입니다.")).toBeVisible();
    await page
      .getByRole("link", { name: "로그인으로 돌아가기", exact: true })
      .click();
    await expect(page).toHaveURL(/\/login$/);
  });
});

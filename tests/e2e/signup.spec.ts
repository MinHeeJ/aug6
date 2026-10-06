import { randomUUID } from "node:crypto";
import { expect, test } from "@playwright/test";

// Run against a disposable preview database: each test creates a unique account via the real signup API.
function account() {
  const userId = `signup${randomUUID().replaceAll("-", "").slice(0, 14)}`;
  return {
    userId,
    password: `Aa1!${randomUUID()}`,
    email: `${userId}@example.invalid`,
  };
}

test("anonymous login entry reaches signup and successful creation returns to login", async ({
  page,
}) => {
  const input = account();
  await page.goto("/login");
  await page.getByRole("link", { name: "회원가입", exact: true }).click();
  await expect(page).toHaveURL(/\/signup$/);
  await expect(page.getByTestId("signup-page")).toBeVisible();
  await page.getByLabel("아이디", { exact: true }).fill(input.userId);
  await page.getByLabel("비밀번호", { exact: true }).fill(input.password);
  await expect(page.getByText("사용 가능", { exact: true })).toBeVisible();
  await page.getByLabel("비밀번호 확인", { exact: true }).fill(input.password);
  await page.getByLabel("이메일", { exact: true }).fill(input.email);
  const receipt = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/v1/auth/signup") &&
      response.request().method() === "POST",
  );
  await page.getByRole("button", { name: "가입하기", exact: true }).click();
  const response = await receipt;
  expect(response.status()).toBe(201);
  expect(response.headers()["set-cookie"]).toBeUndefined();
  expect((await response.json()).data).toEqual({
    userId: input.userId,
    message: "가입이 완료되었습니다.",
  });
  await expect(page).toHaveURL(/\/login$/);
  await expect(
    page.getByText("가입이 완료되었습니다.", { exact: true }),
  ).toBeVisible();
  await page.getByLabel(/^사용자 ID\s*\*?$/).fill(input.userId);
  await page.getByLabel(/^비밀번호\s*\*?$/).fill(input.password);
  const login = page.waitForResponse((result) =>
    result.url().endsWith("/api/auth/login"),
  );
  await page.getByRole("button", { name: "로그인", exact: true }).click();
  expect((await login).status()).toBe(200);
});

test("focus-out reports occupied identifiers and signup rejects duplicate normalized email", async ({
  page,
  request,
}) => {
  const existing = account();
  const created = await request.post("/api/v1/auth/signup", {
    data: { ...existing, passwordConfirm: existing.password },
  });
  expect(created.status()).toBe(201);
  await page.goto("/signup");
  await page.getByLabel("아이디", { exact: true }).fill(existing.userId);
  await page.getByLabel("이메일", { exact: true }).focus();
  await expect(page.getByText("이미 사용 중", { exact: true })).toBeVisible();
  const next = account();
  await page.getByLabel("아이디", { exact: true }).fill(next.userId);
  await page.getByLabel("비밀번호", { exact: true }).fill(next.password);
  await expect(page.getByText("사용 가능", { exact: true })).toBeVisible();
  await page.getByLabel("비밀번호 확인", { exact: true }).fill(next.password);
  await page
    .getByLabel("이메일", { exact: true })
    .fill(existing.email.toUpperCase());
  await page.getByRole("button", { name: "가입하기", exact: true }).click();
  await expect(page.getByRole("alert")).toHaveText("이미 등록된 이메일입니다.");
  await expect(page.getByLabel("이메일", { exact: true })).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  await expect(page).toHaveURL(/\/signup$/);
  const availability = await request.get("/api/v1/auth/check-userid", {
    params: { userId: next.userId },
  });
  expect((await availability.json()).data.available).toBe(true);
});

test("password mismatch stays on signup and does not create an account", async ({
  page,
  request,
}) => {
  const input = account();
  await page.goto("/signup");
  await page.getByLabel("아이디", { exact: true }).fill(input.userId);
  await page.getByLabel("비밀번호", { exact: true }).fill(input.password);
  await page
    .getByLabel("비밀번호 확인", { exact: true })
    .fill(`${input.password}x`);
  await page.getByLabel("이메일", { exact: true }).fill(input.email);
  await page.getByRole("button", { name: "가입하기", exact: true }).click();
  await expect(
    page.getByText("비밀번호와 비밀번호 확인이 일치하지 않습니다.", {
      exact: true,
    }),
  ).toBeVisible();
  await expect(page).toHaveURL(/\/signup$/);
  const response = await request.get("/api/v1/auth/check-userid", {
    params: { userId: input.userId },
  });
  expect((await response.json()).data.available).toBe(true);
});

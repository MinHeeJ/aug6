import { expect, test, type Page } from "@playwright/test";

const facultyLogin = process.env.E2E_FACULTY_LOGIN;
const facultyPassword = process.env.E2E_FACULTY_PASSWORD;
const operatorLogin = process.env.E2E_OPERATOR_LOGIN;
const operatorPassword = process.env.E2E_OPERATOR_PASSWORD;

const screens = [
  [
    "/faculty/employment-rate-improvement-achievements",
    "취업률 제고 실적 관리",
    "employment-rate-improvements",
  ],
  [
    "/faculty/course-offering-operation-achievements",
    "강좌 개설·운영 실적 관리",
    "course-operations",
  ],
  [
    "/faculty/teaching-improvement-achievements",
    "강의개선 실적 관리",
    "lecture-improvements",
  ],
  [
    "/faculty/employment-rate-achievements",
    "취업률 실적 관리",
    "employment-rate-achievements",
  ],
] as const;

async function login(
  page: Page,
  loginId: string,
  password: string,
  role: string,
) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill(loginId);
  await page.getByLabel("비밀번호").fill(password);
  const response = page.waitForResponse((value) =>
    value.url().endsWith("/api/auth/login"),
  );
  await page.getByRole("button", { name: "로그인" }).click();
  expect((await response).status()).toBe(200);
  const session = await page.request.get("/api/auth/me");
  expect(session.status()).toBe(200);
  const user = (await session.json()).data;
  expect(user.roles).toContain(role);
  expect(user.roles).not.toContain("R09");
}

test.describe("실제 병합 교육영역 화면과 non-admin seeded actor", () => {
  test.skip(
    !facultyLogin || !facultyPassword,
    "E2E_FACULTY_LOGIN/PASSWORD and running migrated app required",
  );

  for (const [route, title, resource] of screens) {
    test(`${title}: canonical direct route, default page, tablet/desktop overflow and mean query budget`, async ({
      page,
    }) => {
      await login(page, facultyLogin!, facultyPassword!, "R01");
      const response = page.waitForResponse(
        (value) =>
          value.url().includes(`/api/business/${resource}?`) &&
          value.request().method() === "GET",
      );
      await page.goto(route);
      await expect(page.getByRole("heading", { name: title })).toBeVisible();
      const list = await response;
      expect(list.status()).toBe(200);
      expect((await list.json()).data.pageSize).toBe(20);
      const timings: number[] = [];
      for (let sample = 0; sample < 5; sample++) {
        const start = Date.now();
        const result = await page.request.get(
          `/api/business/${resource}?page=0&pageSize=20`,
        );
        expect(result.status()).toBe(200);
        timings.push(Date.now() - start);
      }
      expect(
        timings.reduce((sum, value) => sum + value, 0) / timings.length,
      ).toBeLessThanOrEqual(3000);
      // These checks run for each configured browser and tablet project, not jsdom viewport simulation.
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      ).toBe(true);
    });
  }
});

test.describe("R07 Excel wizard and unapproved bulk admission", () => {
  test.skip(
    !operatorLogin || !operatorPassword,
    "E2E_OPERATOR_LOGIN/PASSWORD and running migrated app required",
  );

  test("operator sees history but no CRUD; policy remains non-executable and template is binary", async ({
    page,
  }) => {
    await login(page, operatorLogin!, operatorPassword!, "R07");
    const history = page.waitForResponse((value) =>
      value.url().endsWith("/excel-uploads/histories"),
    );
    await page.goto("/faculty/employment-rate-achievements");
    await expect(
      page.getByRole("heading", { name: "취업률 실적 관리" }),
    ).toBeVisible();
    expect((await history).status()).toBe(200);
    await expect(
      page.getByTestId("employment-rate-individual-tab"),
    ).toHaveCount(0);
    await expect(
      page.getByTestId("employment-rate-bulk-execute"),
    ).toBeDisabled();
    const download = page.waitForEvent("download");
    await page.getByTestId("employment-rate-template").click();
    expect((await download).suggestedFilename()).toMatch(/\.xlsx$/);
    const result = await page.request.get(
      "/api/business/employment-rate-achievements/excel-uploads/template",
    );
    expect(result.status()).toBe(200);
    expect(result.headers()["content-type"]).toContain("spreadsheetml.sheet");
    expect((await result.body()).subarray(0, 2).toString()).toBe("PK");
  });
});

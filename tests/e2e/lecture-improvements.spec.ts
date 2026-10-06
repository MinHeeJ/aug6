import { expect, test } from "@playwright/test";

const route = "/faculty/teaching-improvement-achievements";
const api = "/api/business/lecture-improvements";

// Runtime credentials are supplied by the runner; never use the R09 menu bypass as business acceptance.
test.describe("강의개선 실적 authorized navigation", () => {
  test.skip(
    !process.env.LECTURE_E2E_LOGIN_ID || !process.env.LECTURE_E2E_PASSWORD,
    "Runner must supply a role-scoped account and a running preview; no credentials are embedded.",
  );

  test("session menu search reaches the canonical screen and selected detail", async ({
    page,
  }) => {
    await page.goto("/login");
    await page.getByLabel("사용자 ID").fill(process.env.LECTURE_E2E_LOGIN_ID!);
    await page.getByLabel("비밀번호").fill(process.env.LECTURE_E2E_PASSWORD!);
    await page.getByRole("button", { name: "로그인", exact: true }).click();
    await expect(
      page.getByRole("button", { name: /메뉴 또는 화면 검색/ }),
    ).toBeVisible();
    const current = await page.request.get("/api/auth/me");
    expect(current.status()).toBe(200);
    const identity = (await current.json()).data;
    expect(
      identity.roles.some((role: string) =>
        ["R01", "R02", "R04"].includes(role),
      ),
    ).toBe(true);
    expect(identity.roles).not.toContain("R09");

    await page.getByRole("button", { name: /메뉴 또는 화면 검색/ }).click();
    await page.getByTestId("menu-search-input").fill("강의개선");
    const destination = page.getByRole("link", { name: /강의개선/ });
    await expect(destination).toHaveAttribute("href", route);
    const listResponse = page.waitForResponse(
      (response) =>
        new URL(response.url()).pathname === api &&
        response.request().method() === "GET",
    );
    await destination.click();
    expect((await listResponse).status()).toBe(200);
    await expect(page).toHaveURL(new RegExp(`${route}$`));
    await expect(page.getByTestId("lecture-improvement-page")).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "강의개선 실적 관리" }),
    ).toBeVisible();

    const collection = await page.request.get(`${api}?page=0&pageSize=20`);
    expect(collection.status()).toBe(200);
    const data = (await collection.json()).data;
    if (data.achievements.length) {
      const selected = data.achievements[0];
      const detailResponse = page.waitForResponse(
        (response) =>
          new URL(response.url()).pathname ===
          `${api}/${selected.achievementId}`,
      );
      await page
        .getByTestId(`lecture-improvement-detail-${selected.achievementId}`)
        .click();
      expect((await detailResponse).status()).toBe(200);
      await expect(
        page.getByTestId("lecture-improvement-achievement-content"),
      ).toHaveValue(selected.achievementContent);
      await expect(
        page.getByTestId("lecture-improvement-academic-year"),
      ).toHaveValue(String(selected.academicYear));
      await expect(
        page.getByTestId("lecture-improvement-semester"),
      ).toHaveValue(String(selected.semester));
      if (
        selected.achievementStatus !== "DRAFT" ||
        selected.teacherUserId !== identity.userId
      ) {
        await expect(
          page.getByTestId("lecture-improvement-save-button"),
        ).toBeDisabled();
      }
    } else {
      await expect(
        page.getByText("조회 조건에 맞는 결과가 없습니다."),
      ).toBeVisible();
    }
    await page.setViewportSize({ width: 768, height: 1024 });
    await expect(page.getByTestId("lecture-improvement-page")).toBeVisible();
    await expect(
      page.getByTestId("lecture-improvement-search-button"),
    ).toBeVisible();
  });
});

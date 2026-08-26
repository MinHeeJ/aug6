import { expect, test } from "@playwright/test";

const excelRoutes = [
  {
    path: "/admin/excel-upload-templates",
    testId: "excel-upload-templates-screen",
    label: "업로드 양식 관리",
  },
  {
    path: "/admin/excel-uploads",
    testId: "excel-uploads-screen",
    label: "엑셀 업로드",
  },
  {
    path: "/admin/excel-upload-histories",
    testId: "excel-upload-histories-screen",
    label: "업로드 이력 조회",
  },
  {
    path: "/admin/excel-upload-errors",
    testId: "excel-upload-errors-screen",
    label: "업로드 오류 관리",
  },
  {
    path: "/admin/excel-downloads",
    testId: "excel-downloads-screen",
    label: "엑셀 다운로드",
  },
];

test.describe("BASIC-26 Excel 운영 route smoke", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("사용자 ID").fill("admin");
    await page.getByLabel("비밀번호").fill("admin");
    await page.getByRole("button", { name: "로그인" }).click();
    await expect(page).toHaveURL(/\/admin\//);
  });

  for (const route of excelRoutes) {
    test(`${route.label} 화면이 보호 shell 안에서 렌더링된다`, async ({
      page,
    }) => {
      await page.goto(route.path);
      await expect(page.getByTestId(route.testId)).toBeVisible();
      await expect(page.getByText(route.label).first()).toBeVisible();
    });
  }

  test("업로드 양식 저장 전 확인과 다운로드 CTA가 노출된다", async ({
    page,
  }) => {
    await page.goto("/admin/excel-upload-templates");
    await expect(page.getByTestId("excel-template-save-button")).toBeVisible();
    await expect(
      page.getByTestId("excel-template-search-button"),
    ).toBeVisible();
  });
});

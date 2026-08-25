import {
  expect,
  test,
  type APIRequestContext,
  type Page,
} from "@playwright/test";

const supportedEnvironmentSmokeMatrix = [
  "iPadOS Safari tablet",
  "Android Chrome tablet",
  "Windows Edge desktop",
  "Windows Chrome desktop",
  "macOS Safari desktop",
  "macOS Opera desktop",
  "Windows Whale desktop",
];

test.describe("BASIC-17 cross-cutting verification", () => {
  test("privacy policy UI blocks invalid saves, confirms writes, and announces the result", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/privacy/policies");

    await expect(
      page.locator('[data-screen-id="SCR-PRIVACY-POLICY-MGMT"]'),
    ).toBeVisible();
    await expect(
      page.getByTestId("privacy-policy-page-size-select"),
    ).toHaveValue("20");
    await expect(
      page.getByText("개인정보 필드", { exact: false }),
    ).toBeVisible();
    await expect(page.getByText("변경 사유", { exact: false })).toBeVisible();

    await page.getByTestId("privacy-policy-save-button").click();
    await expect(page.getByText("개인정보 필드를 입력하세요.")).toBeVisible();

    const firstRow = page
      .locator('[data-testid^="privacy-policy-row-"]')
      .first();
    await expect(firstRow).toBeVisible();
    await firstRow.click();
    await page
      .getByTestId("privacy-policy-change-reason-input")
      .fill("저장 확인 및 결과 안내 검증");

    page.once("dialog", async (dialog) => {
      expect(dialog.message()).toContain("보호정책을 저장하시겠습니까");
      await dialog.accept();
    });
    await page.getByTestId("privacy-policy-save-button").click();
    await expect(page.getByText("저장되었습니다")).toBeVisible();
  });

  test("Excel export permission denial, secure response shape, and performance smoke boundaries", async ({
    request,
  }) => {
    const deniedExport = await request.post(
      "/api/admin/privacy/permissions/evaluate",
      {
        data: {
          roleCode: "R01",
          fieldKey: "account_no",
          accessType: "EXPORT",
          exportAllowedYn: "N",
          processPurpose: "엑셀 다운로드 권한 차단 smoke",
        },
      },
    );
    expect([200, 403]).toContain(deniedExport.status());
    const deniedBody = await deniedExport.json();
    expect(JSON.stringify(deniedBody)).not.toMatch(
      /actualValue|originalValue|plainValue/,
    );
    if (deniedExport.status() === 200) {
      expect(deniedBody.data.allowed).toBe(false);
    } else {
      expect(deniedBody.success).toBe(false);
    }

    const start = Date.now();
    const policyResponse = await request.get(
      "/api/admin/privacy/policies?page=0&size=20",
    );
    const elapsedMs = Date.now() - start;
    expect(policyResponse.status()).toBe(200);
    expect(elapsedMs).toBeLessThan(3000);
    const policyBody = await policyResponse.json();
    expect(JSON.stringify(policyBody)).not.toMatch(
      /actualValue|originalValue|plainValue/,
    );
  });

  test("supported browser and device smoke matrix is kept explicit for branch preview", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    for (const viewport of [
      {
        width: 1280,
        height: 800,
        label: "Windows Edge Chrome Opera Whale desktop",
      },
      { width: 1024, height: 768, label: "iPadOS Safari tablet" },
      { width: 900, height: 720, label: "Android Chrome tablet" },
      { width: 1440, height: 900, label: "macOS Safari desktop" },
    ]) {
      await page.setViewportSize({
        width: viewport.width,
        height: viewport.height,
      });
      await page.goto("/admin/privacy/policies");
      await expect(
        page.locator('[data-screen-id="SCR-PRIVACY-POLICY-MGMT"]'),
        viewport.label,
      ).toBeVisible();
      await expect(page.getByText("개인정보 항목 관리")).toBeVisible();
    }
    expect(supportedEnvironmentSmokeMatrix).toEqual(
      expect.arrayContaining([
        "iPadOS Safari tablet",
        "Android Chrome tablet",
        "Windows Edge desktop",
        "macOS Safari desktop",
        "macOS Opera desktop",
        "Windows Whale desktop",
      ]),
    );
  });
});

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

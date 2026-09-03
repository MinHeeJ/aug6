import { expect, test, type Page } from "@playwright/test";

type ApiResult<T> = {
  status: number;
  body: { success: boolean; data: T; error?: { code: string } };
};
type ConfirmationRow = {
  targetUserId: number;
  evaluationYear: string;
  latestRecalculationStatus: string;
  finalStatus: string;
  materialCount: number;
};
type BatchResultRow = {
  batchId: string;
  batchType: string;
  totalCount: number;
  successCount: number;
  failureCount: number;
  excludedCount: number;
};

const evaluationYear = "2026";
const areaCode = "RESEARCH_CREATION";

test.describe("BASIC-46 cross-cutting E2E verification", () => {
  test("047 생성 → 049 재계산 → 050 확정 흐름이 batch result로 추적된다", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await assertBasic46RoutesRender(page);
    const target = await firstConfirmationTarget(page);
    await ensureCertified(page, target.targetUserId, target.evaluationYear);

    const generation = await postApi<{
      generationBatchId: string;
      totalCount: number;
      successCount: number;
      excludedCount: number;
    }>(page, "/api/business/evaluation-material-generations", {
      evaluationYear: target.evaluationYear,
      areaCode,
      targetUserId: target.targetUserId,
      reason: "BASIC-46 E2E 평가자료 생성",
    });
    expect(generation.status).toBe(200);
    expect(generation.body.success).toBe(true);
    expect(generation.body.data.generationBatchId).toContain("B46-GEN-");

    const formulaVersionId = await activeFormulaVersionId(
      page,
      target.evaluationYear,
    );
    const recalculation = await recalculate(
      page,
      target.targetUserId,
      target.evaluationYear,
      formulaVersionId,
      "BASIC-46 E2E 생성 후 재계산",
    );
    expect(recalculation.recalculationBatchId).toContain("B46-RECALC-");

    const confirmation = await postApi<{
      finalStatus: string;
      successCount: number;
      finalizationBatchId: string;
    }>(
      page,
      `/api/business/final-evaluation-confirmations/${target.targetUserId}/transition`,
      {
        actionType: "CONFIRM",
        evaluationYear: target.evaluationYear,
        reason: "BASIC-46 E2E 최종평가 확정",
      },
    );
    expect(confirmation.status).toBe(200);
    expect(confirmation.body.success).toBe(true);
    expect(confirmation.body.data.finalStatus).toBe("EVALUATION_CONFIRMED");
    expect(confirmation.body.data.successCount).toBeGreaterThan(0);

    await expectBatchResult(
      page,
      generation.body.data.generationBatchId,
      "GENERATION",
    );
    await expectBatchResult(
      page,
      recalculation.recalculationBatchId,
      "SCORE_RECALCULATION",
    );
    await expectBatchResult(
      page,
      confirmation.body.data.finalizationBatchId,
      "FINALIZATION",
    );
  });

  test("048 삭제 후 047 재생성 → 049 재계산 → 050 확정 흐름이 동작한다", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    const target = await firstConfirmationTarget(page);
    await ensureCertified(page, target.targetUserId, target.evaluationYear);

    const seedBatchId = await generationBatchForDeletion(
      page,
      target.targetUserId,
      target.evaluationYear,
    );
    const preview = await getApi<{
      previewToken: string;
      deletableCount: number;
    }>(
      page,
      `/api/business/evaluation-material-deletions/preview?evaluationYear=${target.evaluationYear}&areaCode=${areaCode}&generationBatchId=${encodeURIComponent(seedBatchId)}`,
    );
    expect(preview.status).toBe(200);
    expect(preview.body.success).toBe(true);
    expect(preview.body.data.previewToken).toBeTruthy();

    const deletion = await postApi<{
      deletionBatchId: string;
      successCount: number;
      excludedCount: number;
    }>(page, "/api/business/evaluation-material-deletions", {
      evaluationYear: target.evaluationYear,
      areaCode,
      generationBatchId: seedBatchId,
      deletionReason: "BASIC-46 E2E 삭제 후 재생성 검증",
      previewToken: preview.body.data.previewToken,
    });
    expect(deletion.status).toBe(200);
    expect(deletion.body.success).toBe(true);
    expect(deletion.body.data.deletionBatchId).toContain("B46-DEL-");

    const generation = await postApi<{ generationBatchId: string }>(
      page,
      "/api/business/evaluation-material-generations",
      {
        evaluationYear: target.evaluationYear,
        areaCode,
        targetUserId: target.targetUserId,
        reason: "BASIC-46 E2E 삭제 후 재생성",
      },
    );
    expect(generation.status).toBe(200);
    expect(generation.body.success).toBe(true);

    const formulaVersionId = await activeFormulaVersionId(
      page,
      target.evaluationYear,
    );
    const recalculation = await recalculate(
      page,
      target.targetUserId,
      target.evaluationYear,
      formulaVersionId,
      "BASIC-46 E2E 재생성 후 재계산",
    );
    const confirmation = await postApi<{
      finalStatus: string;
      finalizationBatchId: string;
    }>(
      page,
      `/api/business/final-evaluation-confirmations/${target.targetUserId}/transition`,
      {
        actionType: "CONFIRM",
        evaluationYear: target.evaluationYear,
        reason: "BASIC-46 E2E 재생성 후 확정",
      },
    );
    expect(confirmation.status).toBe(200);
    expect(confirmation.body.data.finalStatus).toBe("EVALUATION_CONFIRMED");

    await expectBatchResult(
      page,
      deletion.body.data.deletionBatchId,
      "DELETION",
    );
    await expectBatchResult(
      page,
      generation.body.data.generationBatchId,
      "GENERATION",
    );
    await expectBatchResult(
      page,
      recalculation.recalculationBatchId,
      "SCORE_RECALCULATION",
    );
    await expectBatchResult(
      page,
      confirmation.body.data.finalizationBatchId,
      "FINALIZATION",
    );
  });

  test("050 확정취소 → 049 재계산 → 050 재확정 흐름이 동작한다", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    const target = await firstConfirmationTarget(page);
    await ensureConfirmed(page, target.targetUserId, target.evaluationYear);

    const cancel = await postApi<{
      finalStatus: string;
      finalizationBatchId: string;
      successCount: number;
    }>(
      page,
      `/api/business/final-evaluation-confirmations/${target.targetUserId}/transition`,
      {
        actionType: "CANCEL",
        evaluationYear: target.evaluationYear,
        cancelReason: "BASIC-46 E2E 재확정 전 취소",
      },
    );
    expect(cancel.status).toBe(200);
    expect(cancel.body.success).toBe(true);
    expect(cancel.body.data.finalStatus).toBe("CERTIFIED");
    expect(cancel.body.data.successCount).toBeGreaterThan(0);

    const formulaVersionId = await activeFormulaVersionId(
      page,
      target.evaluationYear,
    );
    const recalculation = await recalculate(
      page,
      target.targetUserId,
      target.evaluationYear,
      formulaVersionId,
      "BASIC-46 E2E 확정취소 후 재계산",
    );
    const reconfirm = await postApi<{
      finalStatus: string;
      finalizationBatchId: string;
    }>(
      page,
      `/api/business/final-evaluation-confirmations/${target.targetUserId}/transition`,
      {
        actionType: "CONFIRM",
        evaluationYear: target.evaluationYear,
        reason: "BASIC-46 E2E 재확정",
      },
    );
    expect(reconfirm.status).toBe(200);
    expect(reconfirm.body.data.finalStatus).toBe("EVALUATION_CONFIRMED");

    await expectBatchResult(
      page,
      cancel.body.data.finalizationBatchId,
      "FINALIZATION_CANCEL",
    );
    await expectBatchResult(
      page,
      recalculation.recalculationBatchId,
      "SCORE_RECALCULATION",
    );
    await expectBatchResult(
      page,
      reconfirm.body.data.finalizationBatchId,
      "FINALIZATION",
    );
  });
});

async function assertBasic46RoutesRender(page: Page) {
  const routes = [
    ["/admin/evaluation-material-generations", "SCR-EVAL-MATERIAL-GENERATION"],
    ["/admin/evaluation-material-deletions", "SCR-EVAL-MATERIAL-DELETION"],
    ["/admin/score-recalculations", "SCR-SCORE-RECALCULATION"],
    ["/admin/final-evaluation-confirmations", "SCR-FINAL-EVAL-CONFIRMATION"],
    ["/admin/batch-processing-results", "SCR-EVAL-BATCH-RESULT"],
  ] as const;
  for (const [route, screenId] of routes) {
    await page.goto(route);
    await expect(page.locator(`[data-screen-id="${screenId}"]`)).toBeVisible();
    await expect(page.getByText("권한이 없습니다")).toHaveCount(0);
  }
}

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

async function firstConfirmationTarget(page: Page): Promise<ConfirmationRow> {
  const response = await getApi<{ confirmations: ConfirmationRow[] }>(
    page,
    `/api/business/final-evaluation-confirmations?evaluationYear=${evaluationYear}&size=20`,
  );
  expect(response.status).toBe(200);
  expect(response.body.success).toBe(true);
  expect(response.body.data.confirmations.length).toBeGreaterThan(0);
  return response.body.data.confirmations[0];
}

async function ensureCertified(page: Page, targetUserId: number, year: string) {
  const current = await confirmationFor(page, targetUserId, year);
  if (current.finalStatus === "EVALUATION_CONFIRMED") {
    const cancel = await postApi<{ finalStatus: string }>(
      page,
      `/api/business/final-evaluation-confirmations/${targetUserId}/transition`,
      {
        actionType: "CANCEL",
        evaluationYear: year,
        cancelReason: "BASIC-46 E2E 선행 상태 원복",
      },
    );
    expect(cancel.status).toBe(200);
    expect(cancel.body.data.finalStatus).toBe("CERTIFIED");
  }
}

async function ensureConfirmed(page: Page, targetUserId: number, year: string) {
  const current = await confirmationFor(page, targetUserId, year);
  if (current.finalStatus !== "EVALUATION_CONFIRMED") {
    const formulaVersionId = await activeFormulaVersionId(page, year);
    await recalculate(
      page,
      targetUserId,
      year,
      formulaVersionId,
      "BASIC-46 E2E 선행 재계산",
    );
    const confirm = await postApi<{ finalStatus: string }>(
      page,
      `/api/business/final-evaluation-confirmations/${targetUserId}/transition`,
      {
        actionType: "CONFIRM",
        evaluationYear: year,
        reason: "BASIC-46 E2E 선행 확정",
      },
    );
    expect(confirm.status).toBe(200);
    expect(confirm.body.data.finalStatus).toBe("EVALUATION_CONFIRMED");
  }
}

async function confirmationFor(
  page: Page,
  targetUserId: number,
  year: string,
): Promise<ConfirmationRow> {
  const response = await getApi<{ confirmations: ConfirmationRow[] }>(
    page,
    `/api/business/final-evaluation-confirmations?evaluationYear=${year}&targetUserId=${targetUserId}&size=20`,
  );
  expect(response.status).toBe(200);
  expect(response.body.data.confirmations.length).toBeGreaterThan(0);
  return response.body.data.confirmations[0];
}

async function activeFormulaVersionId(
  page: Page,
  year: string,
): Promise<string> {
  const response = await getApi<{
    calculationFormulas: Array<{ formulaVersionId: number }>;
  }>(
    page,
    `/api/admin/calculation-formulas?evaluationYear=${year}&activeYn=Y&pageSize=20`,
  );
  expect(response.status).toBe(200);
  expect(response.body.success).toBe(true);
  expect(response.body.data.calculationFormulas.length).toBeGreaterThan(0);
  return String(response.body.data.calculationFormulas[0].formulaVersionId);
}

async function recalculate(
  page: Page,
  targetUserId: number,
  year: string,
  formulaVersionId: string,
  selectionReason: string,
) {
  const response = await postApi<{
    recalculationBatchId: string;
    successCount: number;
    excludedCount: number;
  }>(page, "/api/business/score-recalculations", {
    evaluationYear: year,
    areaCode,
    targetUserId,
    formulaVersionId,
    selectionReason,
  });
  expect(response.status).toBe(200);
  expect(response.body.success).toBe(true);
  expect(response.body.data.successCount).toBeGreaterThan(0);
  return response.body.data;
}

async function generationBatchForDeletion(
  page: Page,
  targetUserId: number,
  year: string,
): Promise<string> {
  const response = await getApi<{
    targets: Array<{
      generationBatchId: string;
      targetUserId: number;
      generationStatus?: string;
    }>;
  }>(
    page,
    `/api/business/evaluation-material-generations?evaluationYear=${year}&areaCode=${areaCode}&targetUserId=${targetUserId}&size=20`,
  );
  expect(response.status).toBe(200);
  expect(response.body.success).toBe(true);
  const generated = response.body.data.targets.find(
    (row) => row.generationBatchId,
  );
  expect(generated?.generationBatchId).toBeTruthy();
  return String(generated?.generationBatchId);
}

async function expectBatchResult(
  page: Page,
  batchId: string,
  batchType: string,
) {
  const response = await getApi<{ results: BatchResultRow[] }>(
    page,
    `/api/business/batch-processing-results?batchId=${encodeURIComponent(batchId)}&size=20`,
  );
  expect(response.status).toBe(200);
  expect(response.body.success).toBe(true);
  const row = response.body.data.results.find(
    (item) => item.batchId === batchId,
  );
  expect(row, `batch result ${batchId}`).toBeTruthy();
  expect(row?.batchType).toBe(batchType);
  expect(row?.totalCount).toBeGreaterThanOrEqual(row?.successCount ?? 0);
}

async function getApi<T>(page: Page, path: string): Promise<ApiResult<T>> {
  return page.evaluate(async (url) => {
    const response = await fetch(url, { credentials: "include" });
    return { status: response.status, body: await response.json() };
  }, path) as Promise<ApiResult<T>>;
}

async function postApi<T>(
  page: Page,
  path: string,
  payload: unknown,
): Promise<ApiResult<T>> {
  return page.evaluate(
    async ({ url, data }) => {
      const response = await fetch(url, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      return { status: response.status, body: await response.json() };
    },
    { url: path, data: payload },
  ) as Promise<ApiResult<T>>;
}

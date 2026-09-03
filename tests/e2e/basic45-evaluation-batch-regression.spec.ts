import { expect, test, type Page } from "@playwright/test";

test.describe("BASIC-45 일괄처리·평가 확정 회귀", () => {
  test("생성 삭제 재생성 재계산 확정 취소 재확정과 결과 조회를 통합 검증한다", async ({
    page,
  }) => {
    await loginAsAdmin(page);

    for (const route of basic45Routes) {
      await page.goto(route.path);
      await expect(
        page.locator(`[data-screen-id="${route.screenId}"]`),
      ).toBeVisible();
      await expect(page.getByText("권한이 없습니다")).toHaveCount(0);
    }

    const initialPreview = await apiGet<GenerationPreview>(
      page,
      "/api/business/evaluation-material-generations/preview?page=0&size=20",
    );
    expect(initialPreview.success).toBe(true);
    expect(
      initialPreview.data.targets.length,
      "BASIC-45 생성 후보 fixture가 필요합니다.",
    ).toBeGreaterThan(0);

    const target = initialPreview.data.targets[0];
    const generationPayload = {
      evaluationYear: target.evaluationYear,
      areaCode: target.areaCode,
      organizationCode: target.organizationCode,
      targetUserId: String(target.targetUserId),
    };

    const generation = await apiPost<BatchCommandResult>(
      page,
      "/api/business/evaluation-material-generations",
      generationPayload,
    );
    expect(generation.success).toBe(true);
    expect(generation.data.batchId).toContain("B45-GENERATION-");
    expect(generation.data.createdCount).toBeGreaterThan(0);

    const deletionPreview = await apiGet<DeletionPreview>(
      page,
      `/api/business/evaluation-material-deletions/preview?${query({
        evaluationYear: target.evaluationYear,
        areaCode: target.areaCode,
        generationBatchId: generation.data.batchId,
        page: "0",
        size: "20",
      })}`,
    );
    expect(deletionPreview.success).toBe(true);
    expect(deletionPreview.data.targets.length).toBeGreaterThan(0);
    expect(
      deletionPreview.data.targets.every(
        (row) => row.generationBatchId === generation.data.batchId,
      ),
    ).toBe(true);

    const deletion = await apiPost<DeletionResult>(
      page,
      "/api/business/evaluation-material-deletions",
      {
        evaluationYear: target.evaluationYear,
        areaCode: target.areaCode,
        generationBatchId: generation.data.batchId,
        deleteReason: "BASIC-45 E2E 삭제 후 재생성 검증",
      },
    );
    expect(deletion.success).toBe(true);
    expect(deletion.data.batchId).toContain("B45-DELETION-");
    expect(deletion.data.deletedCount).toBe(
      deletionPreview.data.targets.length,
    );

    const regeneration = await apiPost<BatchCommandResult>(
      page,
      "/api/business/evaluation-material-generations",
      generationPayload,
    );
    expect(regeneration.success).toBe(true);
    expect(regeneration.data.batchId).toContain("B45-GENERATION-");
    expect(regeneration.data.createdCount).toBeGreaterThan(0);
    expect(regeneration.data.batchId).not.toBe(generation.data.batchId);

    const recalculationPreview = await apiGet<RecalculationPreview>(
      page,
      `/api/business/score-recalculations/preview?${query({
        evaluationYear: target.evaluationYear,
        areaCode: target.areaCode,
        targetUserId: String(target.targetUserId),
        page: "0",
        size: "20",
      })}`,
    );
    expect(recalculationPreview.success).toBe(true);
    expect(recalculationPreview.data.targets.length).toBeGreaterThan(0);
    const recalculationTarget = recalculationPreview.data.targets[0];
    expect(recalculationTarget.beforeScore).toBeDefined();
    expect(recalculationTarget.afterScore).toBeDefined();

    const recalculation = await apiPost<RecalculationResult>(
      page,
      "/api/business/score-recalculations",
      {
        evaluationYear: target.evaluationYear,
        areaCode: target.areaCode,
        targetUserId: String(target.targetUserId),
        formulaVersionId: String(recalculationTarget.formulaVersionId),
      },
    );
    expect(recalculation.success).toBe(true);
    expect(recalculation.data.batchId).toContain("B45-RECALCULATION-");
    expect(recalculation.data.recalculatedCount).toBeGreaterThan(0);

    const confirmation = await apiPost<ConfirmationResult>(
      page,
      `/api/business/final-evaluation-confirmations/${target.targetUserId}/confirm`,
      { evaluationYear: target.evaluationYear },
    );
    expect(confirmation.success).toBe(true);
    expect(confirmation.data.fromStatus).toBe("인증");
    expect(confirmation.data.toStatus).toBe("평가확정");

    const cancellation = await apiPost<ConfirmationResult>(
      page,
      `/api/business/final-evaluation-confirmations/${target.targetUserId}/cancel`,
      {
        evaluationYear: target.evaluationYear,
        cancelReason: "BASIC-45 E2E 확정취소 후 재확정 검증",
      },
    );
    expect(cancellation.success).toBe(true);
    expect(cancellation.data.fromStatus).toBe("평가확정");
    expect(cancellation.data.toStatus).toBe("인증");

    const secondRecalculation = await apiPost<RecalculationResult>(
      page,
      "/api/business/score-recalculations",
      {
        evaluationYear: target.evaluationYear,
        areaCode: target.areaCode,
        targetUserId: String(target.targetUserId),
        formulaVersionId: String(recalculationTarget.formulaVersionId),
      },
    );
    expect(secondRecalculation.success).toBe(true);
    expect(secondRecalculation.data.recalculatedCount).toBeGreaterThan(0);

    const reconfirmation = await apiPost<ConfirmationResult>(
      page,
      `/api/business/final-evaluation-confirmations/${target.targetUserId}/confirm`,
      { evaluationYear: target.evaluationYear },
    );
    expect(reconfirmation.success).toBe(true);
    expect(reconfirmation.data.toStatus).toBe("평가확정");

    const observedBatchIds = [
      generation.data.batchId,
      deletion.data.batchId,
      regeneration.data.batchId,
      recalculation.data.batchId,
      confirmation.data.batchId,
      cancellation.data.batchId,
      secondRecalculation.data.batchId,
      reconfirmation.data.batchId,
    ];

    for (const batchId of observedBatchIds) {
      const result = await apiGet<BatchResultList>(
        page,
        `/api/business/evaluation-batch-results?${query({
          batchId,
          page: "0",
          size: "20",
        })}`,
      );
      expect(result.success).toBe(true);
      expect(result.data.results.length).toBeGreaterThan(0);
      expect(result.data.results[0].batchId).toBe(batchId);
      expect(result.data.results[0].totalCount).toBeGreaterThanOrEqual(
        result.data.results[0].successCount,
      );
    }

    const errors = await apiGet<BatchErrorList>(
      page,
      `/api/business/evaluation-batch-results/${encodeURIComponent(generation.data.batchId)}/errors?page=0&size=20`,
    );
    expect(errors.success).toBe(true);
    expect(errors.data.batchId).toBe(generation.data.batchId);
    expect(Array.isArray(errors.data.errors)).toBe(true);
  });
});

const basic45Routes = [
  {
    path: "/admin/evaluation-material-generations",
    screenId: "SCR-EVALUATION-MATERIAL-GENERATION",
  },
  {
    path: "/admin/evaluation-material-deletions",
    screenId: "SCR-EVALUATION-MATERIAL-DELETION",
  },
  {
    path: "/admin/score-recalculations",
    screenId: "SCR-SCORE-RECALCULATION",
  },
  {
    path: "/admin/final-evaluation-confirmations",
    screenId: "SCR-FINAL-EVALUATION-CONFIRMATION",
  },
  {
    path: "/admin/evaluation-batch-results",
    screenId: "SCR-EVALUATION-BATCH-RESULT",
  },
];

async function loginAsAdmin(page: Page) {
  await page.goto("/login");
  await page.getByLabel("사용자 ID").fill("admin");
  await page.getByLabel("비밀번호").fill("admin");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("R09 시스템관리자")).toBeVisible();
}

async function apiGet<T>(
  page: Page,
  path: `/api/${string}`,
): Promise<ApiResponse<T>> {
  return page.evaluate(async (apiPath) => {
    const response = await fetch(apiPath, { credentials: "include" });
    return response.json();
  }, path);
}

async function apiPost<T>(
  page: Page,
  path: `/api/${string}`,
  data: Record<string, string>,
): Promise<ApiResponse<T>> {
  return page.evaluate(
    async ({ apiPath, payload }) => {
      const response = await fetch(apiPath, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      return response.json();
    },
    { apiPath: path, payload: data },
  );
}

function query(params: Record<string, string>) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value.trim()) search.set(key, value);
  }
  return search.toString();
}

type ApiResponse<T> = {
  success: boolean;
  data: T;
  error?: { code: string; message: string };
};

type GenerationPreview = {
  targets: Array<{
    sourceAchievementId: number;
    evaluationYear: string;
    areaCode: string;
    organizationCode: string;
    targetUserId: number;
    sourceStatus: string;
  }>;
};

type DeletionPreview = {
  targets: Array<{ generationBatchId: string }>;
};

type RecalculationPreview = {
  targets: Array<{
    formulaVersionId: number;
    beforeScore: number;
    afterScore: number;
  }>;
};

type BatchCommandResult = {
  batchId: string;
  targetCount: number;
  createdCount: number;
  excludedCount: number;
};

type DeletionResult = {
  batchId: string;
  targetCount: number;
  deletedCount: number;
  excludedCount: number;
};

type RecalculationResult = {
  batchId: string;
  targetCount: number;
  recalculatedCount: number;
  excludedCount: number;
};

type ConfirmationResult = {
  batchId: string;
  targetId: number;
  fromStatus: string;
  toStatus: string;
  changedCount: number;
};

type BatchResultList = {
  results: Array<{
    batchId: string;
    jobType: string;
    totalCount: number;
    successCount: number;
    failureCount: number;
    excludedCount: number;
  }>;
};

type BatchErrorList = {
  batchId: string;
  errors: Array<unknown>;
};

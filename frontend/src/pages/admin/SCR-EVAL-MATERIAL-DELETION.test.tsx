import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationMaterialDeletionPage } from "./SCR-EVAL-MATERIAL-DELETION";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationMaterialDeletionApi: {
      previewEvaluationMaterialDeletion: vi.fn(async () => ({
        success: true,
        data: {
          targets: [
            {
              evaluationMaterialId: 460001,
              evaluationYear: "2026",
              areaCode: "RESEARCH_CREATION",
              organizationCode: "KNUE-DEPT-COMP",
              targetUserId: 52,
              sourceAchievementId: 846001,
              generationBatchId: "B46-BATCH-GEN-001",
              finalStatus: "CERTIFIED",
              canDelete: true,
              excludedReason: null,
              createdAt: "2026-09-03T09:00:00",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          deletableCount: 1,
          previewToken: "B46-PREVIEW-2026-RESEARCH_CREATION-B46-BATCH-GEN-001",
        },
        meta: {},
      })),
      createEvaluationMaterialDeletion: vi.fn(async () => ({
        success: true,
        data: {
          deletionBatchId: "B46-DEL-0001",
          evaluationYear: "2026",
          areaCode: "RESEARCH_CREATION",
          generationBatchId: "B46-BATCH-GEN-001",
          totalCount: 1,
          successCount: 1,
          failureCount: 0,
          excludedCount: 0,
          requestId: "REQ-B46-DEL-0001",
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-EVAL-MATERIAL-DELETION", () => {
  it("renders deletion preview and result state contract controls", () => {
    const html = renderToStaticMarkup(<EvaluationMaterialDeletionPage />);
    expect(html).toContain('data-screen-id="SCR-EVAL-MATERIAL-DELETION"');
    expect(html).toContain('data-testid="evaluation-material-deletion-page"');
    expect(html).toContain("평가자료 삭제");
    expect(html).toContain("삭제 조건 및 사유");
    expect(html).toContain("평가연도");
    expect(html).toContain("평가영역");
    expect(html).toContain("생성배치ID");
    expect(html).toContain("삭제사유");
    expect(html).toContain("삭제대상 미리보기");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    const source = EvaluationMaterialDeletionPage.toString();
    expect(source).toContain("미리보기 대상");
    expect(source).toContain("원천 실적과 수동 등록 자료는 삭제하지 않습니다");
    expect(source).toContain("평가자료 삭제 권한이 없습니다");
    expect(source).toContain("삭제 전 미리보기를 먼저 실행하세요");
  });

  it("uses relative API client calls for preview and deletion", async () => {
    const { evaluationMaterialDeletionApi } = await import(
      "../../api/apiClient"
    );
    renderToStaticMarkup(<EvaluationMaterialDeletionPage />);
    expect(
      evaluationMaterialDeletionApi.previewEvaluationMaterialDeletion,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const source =
      evaluationMaterialDeletionApi.createEvaluationMaterialDeletion.toString();
    expect(source).not.toContain("http://localhost");
  });
});

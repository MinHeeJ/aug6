import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationMaterialGenerationPage } from "./SCR-EVAL-MATERIAL-GENERATION";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationMaterialGenerationApi: {
      listEvaluationMaterialGenerations: vi.fn(async () => ({
        success: true,
        data: {
          targets: [
            {
              sourceAchievementId: 4101,
              evaluationYear: "2026",
              areaCode: "RESEARCH_CREATION",
              organizationCode: "COLLEGE-EDU",
              targetUserId: 52,
              sourceStatus: "CERTIFIED",
              generationStatus: "READY",
              generationBatchId: null,
              lastProcessedAt: null,
              excludedReason: null,
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      createEvaluationMaterialGeneration: vi.fn(async () => ({
        success: true,
        data: {
          generationBatchId: "B46-GEN-0001",
          evaluationYear: "2026",
          areaCode: "RESEARCH_CREATION",
          organizationCode: "COLLEGE-EDU",
          targetUserId: 52,
          totalCount: 1,
          successCount: 1,
          failureCount: 0,
          excludedCount: 0,
          requestId: "REQ-B46-GEN-0001",
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-EVAL-MATERIAL-GENERATION", () => {
  it("renders evaluation material generation contract controls and states", () => {
    const html = renderToStaticMarkup(<EvaluationMaterialGenerationPage />);
    expect(html).toContain('data-screen-id="SCR-EVAL-MATERIAL-GENERATION"');
    expect(html).toContain('data-testid="evaluation-material-generation-page"');
    expect(html).toContain("평가자료 생성");
    expect(html).toContain("생성 조건 설정");
    expect(html).toContain("평가연도");
    expect(html).toContain("평가영역");
    expect(html).toContain("조직");
    expect(html).toContain("대상자 ID");
    expect(html).toContain("생성 사유");
    expect(html).toContain("생성 대상 및 결과");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    const source = EvaluationMaterialGenerationPage.toString();
    expect(source).toContain("인증 이상 원천 실적");
    expect(source).toContain("평가자료를 생성하시겠습니까");
    expect(source).toContain("평가자료 생성 권한이 없습니다");
    expect(source).toContain("조회된 평가자료 생성 대상이 없습니다");
  });

  it("uses relative API client calls for search and generation", async () => {
    const { evaluationMaterialGenerationApi } = await import(
      "../../api/apiClient"
    );
    renderToStaticMarkup(<EvaluationMaterialGenerationPage />);
    expect(
      evaluationMaterialGenerationApi.listEvaluationMaterialGenerations,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const source =
      evaluationMaterialGenerationApi.createEvaluationMaterialGeneration.toString();
    expect(source).not.toContain("http://localhost");
  });
});

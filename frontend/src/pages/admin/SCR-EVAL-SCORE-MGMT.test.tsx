import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationScoreManagementPage } from "./SCR-EVAL-SCORE-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationScoreApi: {
      listEvaluationScores: vi.fn(async () => ({
        success: true,
        data: {
          evaluationScores: [
            {
              scoreRuleId: 700,
              managementItemId: 400,
              elementId: 300,
              itemId: 200,
              areaId: 100,
              ruleVersionId: 10,
              versionCode: "B34-DRAFT-2026",
              versionStatus: "DRAFT",
              areaCode: "EDUCATION",
              areaName: "교육",
              itemCode: "LECTURE",
              itemName: "강의",
              evaluationYear: "2026",
              elementCode: "ATTENDANCE",
              elementName: "출석",
              managementItemCode: "EVIDENCE",
              managementItemName: "증빙파일",
              organizationCode: "COL-EDU",
              organizationName: "사범대학",
              baseScore: 10.5,
              maxScore: 20,
              effectiveStartDate: "2026-01-01",
              effectiveEndDate: "2026-12-31",
              activeYn: "Y",
              teacherEditableYn: "Y",
              requiredYn: "Y",
              dataType: "FILE",
              changeReason: "평가점수 정비",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveEvaluationScore: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-EVAL-SCORE-MGMT", () => {
  it("renders evaluation score route contract and required save states", () => {
    const html = renderToStaticMarkup(<EvaluationScoreManagementPage />);

    expect(html).toContain('data-screen-id="SCR-EVAL-SCORE-MGMT"');
    expect(html).toContain('data-testid="evaluation-score-page"');
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 평가점수 관리",
    );
    expect(html).toContain("평가점수 관리");
    expect(html).toContain("관리항목 ID");
    expect(html).toContain("소속대학 코드");
    expect(html).toContain("평가점수");
    expect(html).toContain("최대점수");
    expect(html).toContain("적용시작일");
    expect(html).toContain("작성중 규정버전에서만 저장");
    const componentSource = EvaluationScoreManagementPage.toString();
    expect(componentSource).toContain("저장되었습니다");
    expect(componentSource).toContain("조회된 평가점수가 없습니다");
    expect(componentSource).toContain("평가점수 관리 권한이 없습니다");
  });

  it("uses relative evaluation score API and exposes default page size options", async () => {
    const { evaluationScoreApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<EvaluationScoreManagementPage />);

    expect(evaluationScoreApi.listEvaluationScores).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<EvaluationScoreManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

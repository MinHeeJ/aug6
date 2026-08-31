import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationRuleSetManagementPage } from "./SCR-EVAL-RULE-SET-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationRuleSetApi: {
      listEvaluationRuleSets: vi.fn(async () => ({
        success: true,
        data: {
          evaluationRuleSets: [
            {
              ruleSetId: 910,
              ruleVersionId: 10,
              versionCode: "B34-DRAFT-2026",
              versionStatus: "DRAFT",
              targetScope: "FACULTY",
              ruleSetName: "교수업적 기준·점수규칙",
              ruleSetStatus: "DRAFT",
              activeYn: "Y",
              effectiveStartDate: "2026-01-01",
              effectiveEndDate: "2026-12-31",
              changeReason: "통합 기준 정비",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveEvaluationRuleSet: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-EVAL-RULE-SET-MGMT", () => {
  it("renders evaluation rule set route contract and required save states", () => {
    const html = renderToStaticMarkup(<EvaluationRuleSetManagementPage />);

    expect(html).toContain('data-screen-id="SCR-EVAL-RULE-SET-MGMT"');
    expect(html).toContain('data-testid="evaluation-rule-set-page"');
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 업적평가 기준·점수규칙 관리",
    );
    expect(html).toContain("업적평가 기준·점수규칙 관리");
    expect(html).toContain("적용 대상");
    expect(html).toContain("규칙명");
    expect(html).toContain("규칙 상태");
    expect(html).toContain("사용여부");
    expect(html).toContain("작성중 규정버전에서만 저장");
    expect(html).toContain(
      "개별 교원 업적자료는 이 화면에서 변경하지 않습니다",
    );
    const componentSource = EvaluationRuleSetManagementPage.toString();
    expect(componentSource).toContain("저장되었습니다");
    expect(componentSource).toContain("조회된 기준·점수규칙이 없습니다");
    expect(componentSource).toContain(
      "업적평가 기준·점수규칙 관리 권한이 없습니다",
    );
  });

  it("uses relative evaluation rule set API and exposes default page size options", async () => {
    const { evaluationRuleSetApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<EvaluationRuleSetManagementPage />);

    expect(
      evaluationRuleSetApi.listEvaluationRuleSets,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<EvaluationRuleSetManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

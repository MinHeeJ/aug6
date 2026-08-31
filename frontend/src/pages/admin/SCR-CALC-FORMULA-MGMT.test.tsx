import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { CalculationFormulaManagementPage } from "./SCR-CALC-FORMULA-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    calculationFormulaApi: {
      listCalculationFormulas: vi.fn(async () => ({
        success: true,
        data: {
          calculationFormulas: [
            {
              formulaVersionId: 810,
              ruleVersionId: 10,
              versionCode: "B34-DRAFT-2026",
              versionStatus: "DRAFT",
              formulaCode: "RAW_SCORE",
              calculationType: "FIXED_SCORE",
              calculationTypeName: "정액배점",
              variableDefinition: '{"baseScore":true}',
              roundingRule: "ROUND_HALF_UP",
              lowerBoundScore: 0,
              upperBoundScore: 100,
              evaluationYear: "2026",
              effectiveStartDate: "2026-01-01",
              effectiveEndDate: "2026-12-31",
              activeYn: "Y",
              changeReason: "계산식 정비",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveCalculationFormula: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-CALC-FORMULA-MGMT", () => {
  it("renders calculation formula route contract and required save states", () => {
    const html = renderToStaticMarkup(<CalculationFormulaManagementPage />);

    expect(html).toContain('data-screen-id="SCR-CALC-FORMULA-MGMT"');
    expect(html).toContain('data-testid="calculation-formula-page"');
    expect(html).toContain("평가 기준 관리 / 평가 기준정보 관리 / 계산식 관리");
    expect(html).toContain("계산식 관리");
    expect(html).toContain("산식 ID");
    expect(html).toContain("계산 유형");
    expect(html).toContain("변수 정의");
    expect(html).toContain("반올림 기준");
    expect(html).toContain("상한");
    expect(html).toContain("하한");
    expect(html).toContain("적용연도");
    expect(html).toContain("작성중 규정버전에서만 저장");
    const componentSource = CalculationFormulaManagementPage.toString();
    expect(componentSource).toContain("저장되었습니다");
    expect(componentSource).toContain("조회된 계산식이 없습니다");
    expect(componentSource).toContain("계산식 관리 권한이 없습니다");
  });

  it("uses relative calculation formula API and exposes default page size options", async () => {
    const { calculationFormulaApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<CalculationFormulaManagementPage />);

    expect(
      calculationFormulaApi.listCalculationFormulas,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<CalculationFormulaManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

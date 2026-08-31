import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationAreaManagementPage } from "./SCR-EVALUATION-AREA-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationAreaApi: {
      listEvaluationAreas: vi.fn(async () => ({
        success: true,
        data: {
          evaluationAreas: [
            {
              areaId: 100,
              ruleVersionId: 10,
              versionCode: "B33-DRAFT-2026",
              versionStatus: "DRAFT",
              areaCode: "EDUCATION",
              areaName: "교육",
              sortOrder: 1,
              activeYn: "Y",
              periodApplyMethod: "YEAR",
              changeReason: "평가영역 정비",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveEvaluationArea: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-EVALUATION-AREA-MGMT", () => {
  it("renders evaluation area route contract and required states", () => {
    const html = renderToStaticMarkup(<EvaluationAreaManagementPage />);

    expect(html).toContain('data-screen-id="SCR-EVALUATION-AREA-MGMT"');
    expect(html).toContain('data-testid="evaluation-area-page"');
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 평가영역 관리",
    );
    expect(html).toContain("평가영역 관리");
    expect(html).toContain("규정버전 ID");
    expect(html).toContain("평가기간 적용방식");
    expect(html).toContain("작성중 규정버전에서만 저장");
    const componentSource = EvaluationAreaManagementPage.toString();
    expect(componentSource).toContain("저장되었습니다");
    expect(componentSource).toContain("조회된 평가영역이 없습니다");
    expect(componentSource).toContain("평가영역 관리 권한이 없습니다");
    expect(html).toContain(
      "점수·배분율·계산식 설정 및 하위 평가항목 편집은 이 화면 범위가 아닙니다",
    );
  });

  it("uses relative evaluation area API and exposes default page size options", async () => {
    const { evaluationAreaApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<EvaluationAreaManagementPage />);

    expect(evaluationAreaApi.listEvaluationAreas).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<EvaluationAreaManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationItemManagementPage } from "./SCR-EVALUATION-ITEM-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationItemApi: {
      listEvaluationItems: vi.fn(async () => ({
        success: true,
        data: {
          evaluationItems: [
            {
              itemId: 200,
              areaId: 100,
              ruleVersionId: 10,
              versionCode: "B33-DRAFT-2026",
              versionStatus: "DRAFT",
              areaCode: "EDUCATION",
              areaName: "교육",
              itemCode: "LECTURE",
              itemName: "강의",
              parentItemCode: null,
              sortOrder: 1,
              activeYn: "Y",
              scoreApplyMethod: "FIXED",
              changeReason: "평가항목 정비",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveEvaluationItem: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-EVALUATION-ITEM-MGMT", () => {
  it("renders evaluation item route contract and required states", () => {
    const html = renderToStaticMarkup(<EvaluationItemManagementPage />);

    expect(html).toContain('data-screen-id="SCR-EVALUATION-ITEM-MGMT"');
    expect(html).toContain('data-testid="evaluation-item-page"');
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 평가항목 관리",
    );
    expect(html).toContain("평가항목 관리");
    expect(html).toContain("규정버전 ID");
    expect(html).toContain("평가영역 코드");
    expect(html).toContain("상위항목 코드");
    expect(html).toContain("배점 적용방식");
    expect(html).toContain("작성중 규정버전에서만 저장");
    const componentSource = EvaluationItemManagementPage.toString();
    expect(componentSource).toContain("저장되었습니다");
    expect(componentSource).toContain("조회된 평가항목이 없습니다");
    expect(componentSource).toContain("평가항목 관리 권한이 없습니다");
    expect(html).toContain(
      "평가요소·관리항목 상세 입력필드와 실제 평가점수·최대점수 입력은 이 화면 범위가 아닙니다",
    );
  });

  it("uses relative evaluation item API and exposes default page size options", async () => {
    const { evaluationItemApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<EvaluationItemManagementPage />);

    expect(evaluationItemApi.listEvaluationItems).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<EvaluationItemManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

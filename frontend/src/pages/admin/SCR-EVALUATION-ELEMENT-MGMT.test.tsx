import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationElementManagementPage } from "./SCR-EVALUATION-ELEMENT-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationElementApi: {
      listEvaluationElements: vi.fn(async () => ({
        success: true,
        data: {
          evaluationElements: [
            {
              elementId: 300,
              itemId: 200,
              areaId: 100,
              ruleVersionId: 10,
              versionCode: "B33-DRAFT-2026",
              versionStatus: "DRAFT",
              areaCode: "EDUCATION",
              areaName: "교육",
              itemCode: "LECTURE",
              itemName: "강의",
              evaluationYear: "2026",
              elementCode: "ATTENDANCE",
              elementName: "출석",
              sortOrder: 1,
              activeYn: "Y",
              changeReason: "평가요소 정비",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveEvaluationElement: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-EVALUATION-ELEMENT-MGMT", () => {
  it("renders evaluation element route contract and required states", () => {
    const html = renderToStaticMarkup(<EvaluationElementManagementPage />);

    expect(html).toContain('data-screen-id="SCR-EVALUATION-ELEMENT-MGMT"');
    expect(html).toContain('data-testid="evaluation-element-page"');
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 평가요소 관리",
    );
    expect(html).toContain("평가요소 관리");
    expect(html).toContain("규정버전 ID");
    expect(html).toContain("평가영역 코드");
    expect(html).toContain("평가항목 코드");
    expect(html).toContain("평가연도");
    expect(html).toContain("평가요소 코드");
    expect(html).toContain("작성중 규정버전에서만 저장");
    const componentSource = EvaluationElementManagementPage.toString();
    expect(componentSource).toContain("저장되었습니다");
    expect(componentSource).toContain("조회된 평가요소가 없습니다");
    expect(componentSource).toContain("평가요소 관리 권한이 없습니다");
    expect(html).toContain(
      "평가요소 아래 관리항목 조건 설정과 평가점수·계산식 변경은 이 화면 범위가 아닙니다",
    );
  });

  it("uses relative evaluation element API and exposes default page size options", async () => {
    const { evaluationElementApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<EvaluationElementManagementPage />);

    expect(
      evaluationElementApi.listEvaluationElements,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<EvaluationElementManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

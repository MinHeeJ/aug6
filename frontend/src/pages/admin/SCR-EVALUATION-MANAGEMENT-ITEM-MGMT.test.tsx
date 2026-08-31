import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationManagementItemManagementPage } from "./SCR-EVALUATION-MANAGEMENT-ITEM-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationManagementItemApi: {
      listEvaluationManagementItems: vi.fn(async () => ({
        success: true,
        data: {
          evaluationManagementItems: [
            {
              managementItemId: 400,
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
              managementItemCode: "EVIDENCE",
              managementItemName: "증빙파일",
              sortOrder: 1,
              activeYn: "Y",
              teacherEditableYn: "Y",
              requiredYn: "Y",
              dataType: "FILE",
              changeReason: "관리항목 정비",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveEvaluationManagementItem: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-EVALUATION-MANAGEMENT-ITEM-MGMT", () => {
  it("renders management item route contract and required input-condition states", () => {
    const html = renderToStaticMarkup(
      <EvaluationManagementItemManagementPage />,
    );

    expect(html).toContain(
      'data-screen-id="SCR-EVALUATION-MANAGEMENT-ITEM-MGMT"',
    );
    expect(html).toContain('data-testid="evaluation-management-item-page"');
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 관리항목 관리",
    );
    expect(html).toContain("관리항목 관리");
    expect(html).toContain("평가요소 코드");
    expect(html).toContain("관리항목 코드");
    expect(html).toContain("교원 입력가능");
    expect(html).toContain("필수여부");
    expect(html).toContain("데이터형식");
    expect(html).toContain("작성중 규정버전에서만 저장");
    const componentSource = EvaluationManagementItemManagementPage.toString();
    expect(componentSource).toContain("저장되었습니다");
    expect(componentSource).toContain("조회된 관리항목이 없습니다");
    expect(componentSource).toContain("관리항목 관리 권한이 없습니다");
    expect(html).toContain(
      "관리항목별 평가점수·계산식 설정과 실제 교원 업적자료 변경은 이 화면 범위가 아닙니다",
    );
  });

  it("uses relative management item API and exposes default page size options", async () => {
    const { evaluationManagementItemApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<EvaluationManagementItemManagementPage />);

    expect(
      evaluationManagementItemApi.listEvaluationManagementItems,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(
      <EvaluationManagementItemManagementPage />,
    );
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

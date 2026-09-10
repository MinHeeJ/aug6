import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { canAccessAdminRoute } from "../LoginPage";
import { EvaluationElementManagementItemsPage } from "./SCR-EVALUATION-ELEMENT-MGMT-ITEMS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationElementManagementItemApi: {
      listEvaluationElementManagementItems: vi.fn(async () => ({
        success: true,
        data: {
          evaluationElementManagementItems: [
            {
              settingId: 5900101,
              ruleVersionId: 1,
              versionCode: "B33-DRAFT-2026",
              versionStatus: "DRAFT",
              evaluationYear: "2026",
              areaCode: "EDUCATION",
              elementCode: "LECTURE_EVALUATION",
              managementItemCode: "LECTURE_EVAL_SCORE",
              managementItemName: "강의평가 점수",
              teacherEditablePart: "점수 확인 및 의견 입력",
              sortOrder: 1,
              activeYn: "Y",
              changeReason: "B59-SEED-001 정상 강의평가 관리항목",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveEvaluationElementManagementItem: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-EVALUATION-ELEMENT-MGMT-ITEMS", () => {
  it("renders FR-018 route contract, filters, list columns, state copy, and save controls", () => {
    const html = renderToStaticMarkup(<EvaluationElementManagementItemsPage />);

    expect(html).toContain(
      'data-screen-id="SCR-EVALUATION-ELEMENT-MGMT-ITEMS"',
    );
    expect(html).toContain(
      'data-testid="evaluation-element-management-items-page"',
    );
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 평가요소별 관리항목 관리",
    );
    expect(html).toContain("평가연도");
    expect(html).toContain("평가영역 코드");
    expect(html).toContain("평가요소 코드");
    expect(html).toContain("교수입력 가능부분");
    expect(html).toContain("정렬순서");
    expect(html).toContain("사용여부");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain("엑셀 내려받기");
    expect(html).toContain(
      'data-testid="evaluation-element-management-items-excel-button"',
    );
    expect(html).toContain("확정 규정버전은 수정할 수 없습니다");
  });

  it("hides FR-018 menu for R01/R08 and exposes it for R04/R09 role contracts", () => {
    const route = "/admin/evaluation-element-management-items";
    const menus = [
      {
        menuId: 565,
        menuName: "평가요소별 관리항목 관리",
        displayOrder: 1,
        children: [],
        url: route,
      },
    ];

    expect(
      canAccessAdminRoute(
        {
          userId: 1,
          loginId: "professor1",
          name: "교원",
          roles: ["R01"],
          menus: [],
        },
        route,
      ),
    ).toBe(false);
    expect(
      canAccessAdminRoute(
        {
          userId: 8,
          loginId: "auditor",
          name: "감사",
          roles: ["R08"],
          menus: [],
        },
        route,
      ),
    ).toBe(false);
    expect(
      canAccessAdminRoute(
        {
          userId: 4,
          loginId: "business-admin",
          name: "담당자",
          roles: ["R04"],
          menus,
        },
        route,
      ),
    ).toBe(true);
    expect(
      canAccessAdminRoute(
        { userId: 9, loginId: "admin", name: "관리자", roles: ["R09"], menus },
        route,
      ),
    ).toBe(true);
  });
});

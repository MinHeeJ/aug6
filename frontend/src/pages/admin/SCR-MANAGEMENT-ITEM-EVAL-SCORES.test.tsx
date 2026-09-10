import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { canAccessAdminRoute } from "../LoginPage";
import { ManagementItemEvaluationScoresPage } from "./SCR-MANAGEMENT-ITEM-EVAL-SCORES";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    managementItemEvaluationScoreApi: {
      listManagementItemEvaluationScores: vi.fn(async () => ({
        success: true,
        data: {
          managementItemEvaluationScores: [
            {
              settingId: 5900301,
              ruleVersionId: 1,
              versionCode: "B33-DRAFT-2026",
              versionStatus: "DRAFT",
              evaluationYear: "2026",
              achievementAreaCode: "EDUCATION",
              achievementCategoryCode: "LECTURE",
              managementItemCode: "LECTURE_EVAL_SCORE",
              collegeCode: "KNUE-COL-EDU",
              evaluationScore: 30,
              sortOrder: 1,
              activeYn: "Y",
              changeReason: "B59-SEED-003 교육영역 A대학 점수",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveManagementItemEvaluationScore: vi.fn(async () => ({
        success: true,
        data: {
          settingId: 5900301,
          ruleVersionId: 1,
          versionCode: "B33-DRAFT-2026",
          versionStatus: "DRAFT",
          evaluationYear: "2026",
          achievementAreaCode: "EDUCATION",
          achievementCategoryCode: "LECTURE",
          managementItemCode: "LECTURE_EVAL_SCORE",
          collegeCode: "KNUE-COL-EDU",
          evaluationScore: 33.75,
          sortOrder: 1,
          activeYn: "Y",
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-MANAGEMENT-ITEM-EVAL-SCORES", () => {
  it("renders FR-020 route contract, filters, score columns, active state, and save controls", () => {
    const html = renderToStaticMarkup(<ManagementItemEvaluationScoresPage />);

    expect(html).toContain('data-screen-id="SCR-MANAGEMENT-ITEM-EVAL-SCORES"');
    expect(html).toContain(
      'data-testid="management-item-evaluation-scores-page"',
    );
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 관리항목별 평가점수 관리",
    );
    expect(html).toContain("업적영역 코드");
    expect(html).toContain("업적분류 코드");
    expect(html).toContain("소속대학 코드");
    expect(html).toContain("관리항목");
    expect(html).toContain("평가점수");
    expect(html).toContain("정렬순서");
    expect(html).toContain("사용여부");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain("엑셀 내려받기");
    expect(html).toContain(
      'data-testid="management-item-evaluation-scores-excel-button"',
    );
    expect(html).toContain("확정 규정버전은 수정할 수 없습니다");
    expect(html).toContain("저장");
  });

  it("hides FR-020 menu for R01/R08 and exposes it for R04/R09 role contracts", () => {
    const route = "/admin/management-item-evaluation-scores";
    const menus = [
      {
        menuId: 567,
        menuName: "관리항목별 평가점수 관리",
        displayOrder: 3,
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

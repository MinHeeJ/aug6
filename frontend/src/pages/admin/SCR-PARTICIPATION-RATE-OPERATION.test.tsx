import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { canAccessAdminRoute } from "../LoginPage";
import { ParticipationRateOperationSettingsPage } from "./SCR-PARTICIPATION-RATE-OPERATION";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    participationRateOperationSettingApi: {
      listParticipationRateOperationSettings: vi.fn(async () => ({
        success: true,
        data: {
          participationRateOperationSettings: [
            {
              settingId: 5900201,
              ruleVersionId: 1,
              versionCode: "B33-DRAFT-2026",
              versionStatus: "DRAFT",
              evaluationYear: "2026",
              achievementAreaCode: "RESEARCH",
              achievementCategoryCode: "PAPER",
              managementItemCode: "JOURNAL_ARTICLE",
              researcherCountBand: "TWO",
              participationTypeCode: "CORRESPONDING_AUTHOR",
              distributionRate: 70,
              activeYn: "Y",
              changeReason: "B59-SEED-002 논문 공동저자 2인 배분율",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveParticipationRateOperationSetting: vi.fn(async () => ({
        success: true,
        data: { participationRateOperationSettings: [] },
        meta: {},
      })),
    },
  };
});

describe("SCR-PARTICIPATION-RATE-OPERATION", () => {
  it("renders FR-019 route contract, filters, matrix columns, lock copy, and bulk save controls", () => {
    const html = renderToStaticMarkup(
      <ParticipationRateOperationSettingsPage />,
    );

    expect(html).toContain('data-screen-id="SCR-PARTICIPATION-RATE-OPERATION"');
    expect(html).toContain(
      'data-testid="participation-rate-operation-settings-page"',
    );
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 참여구분별 배분율 관리",
    );
    expect(html).toContain("업적영역 코드");
    expect(html).toContain("업적분류 코드");
    expect(html).toContain("관리항목 x 연구자수");
    expect(html).toContain("참여구분");
    expect(html).toContain("배분율");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain("엑셀 내려받기");
    expect(html).toContain(
      'data-testid="participation-rate-operation-settings-excel-button"',
    );
    expect(html).toContain(
      "확정 규정버전/확정 평가결과 소급 변경은 차단됩니다",
    );
    expect(html).toContain("일괄 저장");
  });

  it("hides FR-019 menu for R01/R08 and exposes it for R04/R09 role contracts", () => {
    const route = "/admin/participation-rate-operation-settings";
    const menus = [
      {
        menuId: 566,
        menuName: "참여구분별 배분율 관리",
        displayOrder: 2,
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

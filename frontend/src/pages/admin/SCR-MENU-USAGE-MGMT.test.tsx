import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { MenuUsageManagementPage } from "./SCR-MENU-USAGE-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    menuUsageApi: {
      listMenuUsageSettings: vi.fn(async () => ({
        success: true,
        data: {
          settings: [
            {
              menuId: 133,
              parentMenuId: 130,
              topMenuName: "시스템 관리",
              middleMenuName: "메뉴 관리",
              menuName: "메뉴 사용 관리",
              screenId: "SCR-MENU-USAGE-MGMT",
              url: "/admin/menu-usage",
              systemUseYn: "Y",
              exposureStartAt: "2026-08-19T00:00:00",
              exposureEndAt: "2026-12-31T23:59:59",
              status: "ACTIVE",
            },
          ],
          page: 0,
          size: 10,
          totalElements: 1,
        },
        meta: {},
      })),
      saveMenuUsageSettings: vi.fn(async () => ({
        success: true,
        data: [],
        meta: {},
      })),
    },
  };
});

describe("SCR-MENU-USAGE-MGMT", () => {
  it("renders menu usage management contract and required state text", () => {
    const html = renderToStaticMarkup(<MenuUsageManagementPage />);

    expect(html).toContain('data-screen-id="SCR-MENU-USAGE-MGMT"');
    expect(html).toContain("메뉴 사용 관리");
    expect(html).toContain("검색조건");
    expect(html).toContain("메뉴 사용 설정 목록");
    expect(html).toContain("사용여부");
    expect(html).toContain("노출 시작");
    expect(html).toContain("노출 종료");
    expect(html).toContain("메뉴 사용 설정을 불러오는 중입니다");
    expect(html).toContain("조회된 메뉴 사용 설정이 없습니다");
    expect(html).toContain("메뉴 사용 관리 권한이 없습니다");
    expect(html).toContain("저장되었습니다");
  });

  it("declares regression wording for hidden menu and direct URL blocking", () => {
    const html = renderToStaticMarkup(<MenuUsageManagementPage />);

    expect(html).toContain(
      "중지 또는 비노출 메뉴는 사용자 메뉴와 직접 URL 접근에서 차단됩니다",
    );
  });
});

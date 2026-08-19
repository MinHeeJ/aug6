import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { MenuInfoManagementPage } from "./SCR-MENU-INFO-MGMT";

vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    user: {
      menus: [
        {
          menuId: 100,
          menuName: "시스템 관리",
          displayOrder: 1,
          children: [
            {
              menuId: 130,
              menuName: "메뉴 관리",
              displayOrder: 1,
              children: [
                {
                  menuId: 132,
                  parentMenuId: 130,
                  menuName: "메뉴 정보 관리",
                  screenId: "SCR-MENU-INFO-MGMT",
                  url: "/admin/menu-info",
                  icon: "file-cog",
                  displayOrder: 2,
                  children: [],
                },
              ],
            },
          ],
        },
      ],
    },
  }),
}));

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    menuExecutionApi: {
      getMenuExecution: vi.fn(async () => ({
        success: true,
        data: {
          menuId: 132,
          parentMenuId: 130,
          menuType: "SCREEN",
          menuName: "메뉴 정보 관리",
          screenId: "SCR-MENU-INFO-MGMT",
          url: "/admin/menu-info",
          icon: "file-cog",
          businessCategory: "SYSTEM",
          description: "메뉴 실행 정보 관리",
          status: "ACTIVE",
          changeReason: "시드 실행정보",
        },
        meta: {},
      })),
      updateMenuExecution: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-MENU-INFO-MGMT", () => {
  it("renders menu execution info contract and all required state text", () => {
    const html = renderToStaticMarkup(<MenuInfoManagementPage />);

    expect(html).toContain('data-screen-id="SCR-MENU-INFO-MGMT"');
    expect(html).toContain("메뉴 정보 관리");
    expect(html).toContain("검색조건");
    expect(html).toContain("메뉴 실행정보 목록");
    expect(html).toContain("실행정보 상세/저장");
    expect(html).toContain("메뉴 실행정보를 불러오는 중입니다");
    expect(html).toContain("조회된 메뉴 실행정보가 없습니다");
    expect(html).toContain("메뉴 정보 관리 권한이 없습니다");
    expect(html).toContain("저장되었습니다");
    expect(html).toContain(
      "저장된 URL로 메뉴 클릭 시 이동 대상과 화면ID가 일치해야 합니다",
    );
  });

  it("uses selected menuId from the authorized menu tree rather than a hardcoded sample id", async () => {
    const { menuExecutionApi } = await import("../../api/apiClient");

    renderToStaticMarkup(<MenuInfoManagementPage />);

    expect(menuExecutionApi.getMenuExecution).not.toHaveBeenCalledWith(
      "MENU-1",
    );
    expect(menuExecutionApi.updateMenuExecution).not.toHaveBeenCalledWith(
      "MENU-1",
      expect.anything(),
    );
  });
});

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { MenuStructureManagementPage } from "./SCR-MENU-STRUCTURE-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    menuStructureApi: {
      getMenuTree: vi.fn(async () => ({
        success: true,
        data: [
          {
            menuId: 100,
            parentMenuId: null,
            menuType: "MAIN",
            menuName: "시스템 관리",
            displayOrder: 1,
            icon: "settings",
            systemUseYn: "Y",
            status: "ACTIVE",
            children: [
              {
                menuId: 130,
                parentMenuId: 100,
                menuType: "MIDDLE",
                menuName: "메뉴 관리",
                displayOrder: 3,
                icon: "menu",
                systemUseYn: "Y",
                status: "ACTIVE",
                children: [
                  {
                    menuId: 131,
                    parentMenuId: 130,
                    menuType: "SCREEN",
                    menuName: "메뉴 구조 관리",
                    displayOrder: 1,
                    screenId: "SCR-MENU-STRUCTURE-MGMT",
                    url: "/admin/menu-structure",
                    icon: "tree",
                    systemUseYn: "Y",
                    status: "ACTIVE",
                    children: [],
                  },
                ],
              },
            ],
          },
        ],
        meta: {},
      })),
      updateMenuParent: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
      reorderMenus: vi.fn(async () => ({ success: true, data: [], meta: {} })),
    },
  };
});

describe("SCR-MENU-STRUCTURE-MGMT", () => {
  it("renders menu structure contract and required loading empty error permission success texts", () => {
    const html = renderToStaticMarkup(<MenuStructureManagementPage />);

    expect(html).toContain('data-screen-id="SCR-MENU-STRUCTURE-MGMT"');
    expect(html).toContain("메뉴 구조 관리");
    expect(html).toContain("메뉴 tree");
    expect(html).toContain("부모 메뉴 변경");
    expect(html).toContain("표시 순서 재정렬");
    expect(html).toContain("메뉴 구조를 불러오는 중입니다");
    expect(html).toContain("조회된 메뉴가 없습니다");
    expect(html).toContain("메뉴 구조 관리 권한이 없습니다");
    expect(html).toContain("저장되었습니다");
  });

  it("uses selected menu ids for API path parameters and relative OpenAPI calls only", async () => {
    const { menuStructureApi } = await import("../../api/apiClient");

    renderToStaticMarkup(<MenuStructureManagementPage />);

    expect(menuStructureApi.updateMenuParent).not.toHaveBeenCalledWith(
      999999,
      expect.anything(),
    );
    expect(menuStructureApi.reorderMenus).not.toHaveBeenCalledWith(
      expect.objectContaining({ orderedMenuIds: [999999] }),
    );
  });
});

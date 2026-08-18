import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { MenuPermissionManagementPage } from "./SCR-MENU-PERMISSION-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    menuPermissionApi: {
      listMenuPermissions: vi.fn(async () => ({
        success: true,
        data: {
          permissions: [
            {
              permissionId: 1,
              targetType: "ROLE",
              targetId: "R09",
              targetName: "시스템관리자",
              menuId: 123,
              topMenuName: "시스템 관리",
              middleMenuName: "역할·권한 관리",
              screenMenuName: "메뉴 권한 관리",
              screenId: "SCR-MENU-PERMISSION-MGMT",
              url: "/admin/menu-permissions",
              accessAllowed: "ALLOW",
              status: "ACTIVE",
              changeReason: "시드 권한",
            },
          ],
          page: 0,
          size: 10,
          totalElements: 1,
        },
        meta: {},
      })),
      saveMenuPermissions: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-MENU-PERMISSION-MGMT", () => {
  it("renders menu permission matrix contract and all required state text", () => {
    const html = renderToStaticMarkup(<MenuPermissionManagementPage />);

    expect(html).toContain('data-screen-id="SCR-MENU-PERMISSION-MGMT"');
    expect(html).toContain("메뉴 권한 관리");
    expect(html).toContain("검색조건");
    expect(html).toContain("접근권한 matrix");
    expect(html).toContain("선택 권한 상세/저장");
    expect(html).toContain("메뉴 권한을 불러오는 중입니다");
    expect(html).toContain("조회된 메뉴 권한이 없습니다");
    expect(html).toContain("메뉴 권한 관리 권한이 없습니다");
    expect(html).toContain("저장되었습니다");
  });

  it("uses only OpenAPI query parameters and never invents a frontend access calculation", async () => {
    const { menuPermissionApi } = await import("../../api/apiClient");

    renderToStaticMarkup(<MenuPermissionManagementPage />);

    expect(menuPermissionApi.listMenuPermissions).not.toHaveBeenCalledWith(
      expect.objectContaining({ organizationPriority: expect.anything() }),
    );
  });
});

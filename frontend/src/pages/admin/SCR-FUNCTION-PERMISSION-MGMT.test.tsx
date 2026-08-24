import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { FunctionPermissionManagementPage } from "./SCR-FUNCTION-PERMISSION-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    functionPermissionApi: {
      listFunctionPermissions: vi.fn(async () => ({
        success: true,
        data: {
          permissions: [
            {
              functionPermissionId: 1,
              screenId: "SCR-FUNCTION-PERMISSION-MGMT",
              screenName: "기능 권한 관리",
              roleCode: "R09",
              roleName: "시스템관리자",
              functionType: "READ",
              permissionAllowed: "ALLOW",
              changeReason: "시드 권한",
            },
          ],
          page: 0,
          size: 10,
          totalElements: 1,
        },
        meta: {},
      })),
      saveFunctionPermissions: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
      evaluateFunctionPermission: vi.fn(async () => ({
        success: true,
        data: {
          allowed: true,
          screenId: "SCR-FUNCTION-PERMISSION-MGMT",
          roleCode: "R09",
          functionType: "READ",
          reason: "ALLOW",
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-FUNCTION-PERMISSION-MGMT", () => {
  it("renders function permission route contract and required five state text", () => {
    const html = renderToStaticMarkup(<FunctionPermissionManagementPage />);

    expect(html).toContain('data-screen-id="SCR-FUNCTION-PERMISSION-MGMT"');
    expect(html).toContain('data-testid="function-permission-page"');
    expect(html).toContain("기능 권한 관리");
    expect(html).toContain("화면 ID");
    expect(html).toContain("역할 코드");
    expect(html).toContain("기능구분");
    expect(html).toContain("허용 여부");
    expect(html).toContain("기능 권한 관리 권한이 없습니다");
    expect(html).toContain("조회된 기능 권한이 없습니다");
    expect(html).toContain("저장되었습니다");
  });

  it("uses OpenAPI function permission APIs with relative route helpers only", async () => {
    const { functionPermissionApi } = await import("../../api/apiClient");

    renderToStaticMarkup(<FunctionPermissionManagementPage />);

    expect(
      functionPermissionApi.listFunctionPermissions,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
  });
});

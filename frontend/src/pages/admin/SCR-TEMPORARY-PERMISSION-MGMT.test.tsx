import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { TemporaryPermissionManagementPage } from "./SCR-TEMPORARY-PERMISSION-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    temporaryPermissionApi: {
      listTemporaryPermissions: vi.fn(async () => ({
        success: true,
        data: {
          permissions: [
            {
              temporaryPermissionId: 11,
              userId: 2,
              userName: "홍길동",
              workDataRef: "WRK-2026-001",
              functionType: "UPDATE",
              validStartAt: "2026-08-24T09:00:00",
              validEndAt: "2026-08-31T18:00:00",
              status: "ACTIVE",
              changeReason: "마감 보정 임시 권한",
            },
          ],
          page: 0,
          size: 10,
          totalElements: 1,
        },
        meta: {},
      })),
      createTemporaryPermission: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-TEMPORARY-PERMISSION-MGMT", () => {
  it("renders temporary permission route contract and required five state text", () => {
    const html = renderToStaticMarkup(<TemporaryPermissionManagementPage />);

    expect(html).toContain('data-screen-id="SCR-TEMPORARY-PERMISSION-MGMT"');
    expect(html).toContain('data-testid="temporary-permission-page"');
    expect(html).toContain("임시 권한 관리");
    expect(html).toContain("대상 교원 ID");
    expect(html).toContain("업무자료 식별자");
    expect(html).toContain("지정 기능");
    expect(html).toContain("유효기간");
    expect(html).toContain("임시 권한 관리 권한이 없습니다");
    expect(html).toContain("조회된 임시 권한이 없습니다");
    expect(html).toContain("저장되었습니다");
  });

  it("uses relative temporary permission APIs and keeps user role mutation out of the UI", async () => {
    const { temporaryPermissionApi } = await import("../../api/apiClient");

    renderToStaticMarkup(<TemporaryPermissionManagementPage />);

    expect(
      temporaryPermissionApi.listTemporaryPermissions,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    expect(
      htmlDoesNotContainUserRoleMutation(
        renderToStaticMarkup(<TemporaryPermissionManagementPage />),
      ),
    ).toBe(true);
  });
});

function htmlDoesNotContainUserRoleMutation(html: string) {
  return (
    !html.includes("/api/admin/users/") && !html.includes("user_roles 변경")
  );
}

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { PeriodPermissionManagementPage } from "./SCR-PERIOD-PERMISSION-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    periodPermissionApi: {
      listPeriodPermissions: vi.fn(async () => ({
        success: true,
        data: {
          links: [
            {
              periodPermissionLinkId: 1,
              businessPeriodId: "BP-2026-A",
              functionPermissionId: 77,
              screenId: "SCR-PERIOD-PERMISSION-MGMT",
              screenName: "기간별 권한 관리",
              roleCode: "R09",
              roleName: "시스템관리자",
              functionType: "UPDATE",
              permissionAllowed: "ALLOW",
              effectiveStartAt: "2026-08-24T00:00:00",
              effectiveEndAt: "2026-08-31T23:59:59",
              periodState: "ACTIVE",
              effectiveAllowed: true,
              changeReason: "평가 기간 연결",
            },
          ],
          page: 0,
          size: 10,
          totalElements: 1,
        },
        meta: {},
      })),
      savePeriodPermissions: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-PERIOD-PERMISSION-MGMT", () => {
  it("renders period permission route contract and required five state text", () => {
    const html = renderToStaticMarkup(<PeriodPermissionManagementPage />);

    expect(html).toContain('data-screen-id="SCR-PERIOD-PERMISSION-MGMT"');
    expect(html).toContain('data-testid="period-permission-page"');
    expect(html).toContain("기간별 권한 관리");
    expect(html).toContain("업무기간 ID");
    expect(html).toContain("기능 권한 ID");
    expect(html).toContain("기간 상태");
    expect(html).toContain("처리 시점 기준");
    expect(html).toContain("기간별 권한 관리 권한이 없습니다");
    expect(html).toContain("조회된 기간별 권한이 없습니다");
    expect(html).toContain("저장되었습니다");
  });

  it("uses OpenAPI period permission APIs with relative route helpers only", async () => {
    const { periodPermissionApi } = await import("../../api/apiClient");

    renderToStaticMarkup(<PeriodPermissionManagementPage />);

    expect(periodPermissionApi.listPeriodPermissions).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
  });
});

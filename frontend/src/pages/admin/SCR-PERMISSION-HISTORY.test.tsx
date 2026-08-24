import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { PermissionHistoryPage } from "./SCR-PERMISSION-HISTORY";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    permissionHistoryApi: {
      listPermissionChangeHistory: vi.fn(async () => ({
        success: true,
        data: {
          history: [
            {
              permissionHistoryId: 91,
              targetType: "FUNCTION",
              targetId: "SCR-FUNCTION-PERMISSION-MGMT:R09:UPDATE",
              beforeValue: '{"permissionAllowed":"ALLOW"}',
              afterValue: '{"permissionAllowed":"DENY"}',
              changedBy: 1,
              reason: "수정 기능 차단",
              changedAt: "2026-08-24T09:00:00",
            },
          ],
          page: 0,
          size: 10,
          total: 1,
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-PERMISSION-HISTORY", () => {
  it("renders read-only permission history route contract and five state text", () => {
    const html = renderToStaticMarkup(<PermissionHistoryPage />);

    expect(html).toContain('data-screen-id="SCR-PERMISSION-HISTORY"');
    expect(html).toContain('data-testid="permission-history-page"');
    expect(html).toContain("권한 변경 이력 조회");
    expect(html).toContain("유형");
    expect(html).toContain("대상 ID");
    expect(html).toContain("변경 전 값");
    expect(html).toContain("변경 후 값");
    expect(html).toContain("처리자");
    expect(html).toContain("사유");
    expect(html).toContain("권한 변경 이력 조회 권한이 없습니다");
    expect(html).toContain("조회된 권한 변경 이력이 없습니다");
    expect(html).toContain("검색이 완료되었습니다");
    expect(html).toContain(
      "이 화면에는 권한 변경, 이력 수정, 이력 삭제 버튼이 없습니다",
    );
    expect(html).not.toContain('data-testid="permission-history-save-button"');
    expect(html).not.toContain(
      'data-testid="permission-history-delete-button"',
    );
  });

  it("uses relative listPermissionChangeHistory helper without sample localhost URLs", async () => {
    const { permissionHistoryApi } = await import("../../api/apiClient");

    renderToStaticMarkup(<PermissionHistoryPage />);

    expect(
      permissionHistoryApi.listPermissionChangeHistory,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
  });
});

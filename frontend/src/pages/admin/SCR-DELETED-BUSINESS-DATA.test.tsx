import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { DeletedBusinessDataPage } from "./SCR-DELETED-BUSINESS-DATA";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    deletedBusinessDataApi: {
      listDeletedBusinessData: vi.fn(async () => ({
        success: true,
        data: {
          deletedData: [
            {
              deletedDataId: 200,
              businessType: "FACULTY_ACHIEVEMENT",
              originalKey: "ACH-2026-0001",
              deletedBy: 1,
              deletedByLoginId: "admin",
              deletedByName: "시스템관리자",
              deletedAt: "2026-08-31T10:00:00",
              deleteReason: "중복 입력 정리",
              recoverableYn: "N",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-DELETED-BUSINESS-DATA", () => {
  it("renders deleted business data route contract and deletion info columns", () => {
    const html = renderToStaticMarkup(<DeletedBusinessDataPage />);

    expect(html).toContain('data-screen-id="SCR-DELETED-BUSINESS-DATA"');
    expect(html).toContain('data-testid="deleted-business-data-page"');
    expect(html).toContain("삭제자료 관리");
    expect(html).toContain("삭제된 업무자료");
    expect(html).toContain("원본키");
    expect(html).toContain("삭제자");
    expect(html).toContain("삭제일시");
    expect(html).toContain("삭제사유");
    expect(html).toContain("복구가능여부");
    expect(html).toContain("복구/물리삭제 기능은 제공하지 않습니다");
    expect(html).not.toContain(
      'data-testid="deleted-business-data-restore-button"',
    );
    expect(html).not.toContain(
      'data-testid="deleted-business-data-delete-button"',
    );
  });

  it("exposes allowed page sizes and avoids absolute API URLs", async () => {
    const { deletedBusinessDataApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<DeletedBusinessDataPage />);

    expect(
      deletedBusinessDataApi.listDeletedBusinessData,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<DeletedBusinessDataPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

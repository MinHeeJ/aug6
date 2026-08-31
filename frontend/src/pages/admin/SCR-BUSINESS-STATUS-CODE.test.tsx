import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { BusinessStatusCodePage } from "./SCR-BUSINESS-STATUS-CODE";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    businessStatusCodeApi: {
      listBusinessStatusCodes: vi.fn(async () => ({
        success: true,
        data: {
          statusCodes: [
            {
              statusCodeId: 10,
              definitionVersion: "DRAFT",
              businessType: "FACULTY_ACHIEVEMENT",
              statusCode: "SUBMITTED",
              displayName: "제출",
              systemUseYn: "Y",
              changeReason: "상태 표시명 정비",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveBusinessStatusCode: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-BUSINESS-STATUS-CODE", () => {
  it("renders business status code route contract and required states", () => {
    const html = renderToStaticMarkup(<BusinessStatusCodePage />);

    expect(html).toContain('data-screen-id="SCR-BUSINESS-STATUS-CODE"');
    expect(html).toContain('data-testid="business-status-code-page"');
    expect(html).toContain("상태코드 관리");
    expect(html).toContain("업무유형");
    expect(html).toContain("상태정의 버전");
    expect(html).toContain("상태 표시명");
    expect(html).toContain("확정된 기술 상태코드");
    expect(html).toContain("저장되었습니다");
    expect(html).toContain("조회된 상태코드가 없습니다");
    expect(html).toContain("상태코드 관리 권한이 없습니다");
  });

  it("uses relative status code API and exposes default page size options", async () => {
    const { businessStatusCodeApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<BusinessStatusCodePage />);

    expect(
      businessStatusCodeApi.listBusinessStatusCodes,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<BusinessStatusCodePage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

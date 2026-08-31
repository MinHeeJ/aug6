import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { RejectionReasonPage } from "./SCR-REJECTION-REASON";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    rejectionReasonApi: {
      listRejectionReasons: vi.fn(async () => ({
        success: true,
        data: {
          reasons: [
            {
              rejectionReasonId: 10,
              businessType: "FACULTY_ACHIEVEMENT",
              reasonCode: "DEPT_REVIEW_REQUIRED",
              standardMessage: "학과장 검토 의견이 필요합니다.",
              additionalOpinionAllowedYn: "Y",
              changeReason: "반려사유 정비",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveRejectionReason: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-REJECTION-REASON", () => {
  it("renders rejection reason route contract and required states", () => {
    const html = renderToStaticMarkup(<RejectionReasonPage />);

    expect(html).toContain('data-screen-id="SCR-REJECTION-REASON"');
    expect(html).toContain('data-testid="rejection-reason-page"');
    expect(html).toContain("반려사유 관리");
    expect(html).toContain("업무유형");
    expect(html).toContain("반려사유 코드");
    expect(html).toContain("표준 문구");
    expect(html).toContain("추가 의견 허용");
    expect(html).toContain("저장되었습니다");
    expect(html).toContain("조회된 반려사유가 없습니다");
    expect(html).toContain("반려사유 관리 권한이 없습니다");
  });

  it("exposes allowed page sizes and avoids absolute API URLs", async () => {
    const { rejectionReasonApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<RejectionReasonPage />);

    expect(rejectionReasonApi.listRejectionReasons).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<RejectionReasonPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { BusinessStatusTransitionPage } from "./SCR-BUSINESS-STATUS-TRANSITION";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    businessStatusTransitionApi: {
      listBusinessStatusTransitions: vi.fn(async () => ({
        success: true,
        data: {
          transitions: [
            {
              transitionId: 10,
              definitionVersion: "DRAFT",
              businessType: "FACULTY_ACHIEVEMENT",
              fromStatusCode: "SUBMITTED",
              toStatusCode: "DEPARTMENT_CONFIRMED",
              executorRoleCode: "R02",
              opinionRequiredYn: "N",
              attachmentRequiredYn: "N",
              cancellableYn: "Y",
              changeReason: "전이규칙 정비",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveBusinessStatusTransition: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-BUSINESS-STATUS-TRANSITION", () => {
  it("renders business status transition route contract and required states", () => {
    const html = renderToStaticMarkup(<BusinessStatusTransitionPage />);

    expect(html).toContain('data-screen-id="SCR-BUSINESS-STATUS-TRANSITION"');
    expect(html).toContain('data-testid="business-status-transition-page"');
    expect(html).toContain("상태 전이 관리");
    expect(html).toContain("업무유형");
    expect(html).toContain("현재 상태");
    expect(html).toContain("다음 상태");
    expect(html).toContain("실행 역할");
    expect(html).toContain("필수의견");
    expect(html).toContain("필수첨부");
    expect(html).toContain("취소가능");
    expect(html).toContain("저장되었습니다");
    expect(html).toContain("조회된 상태 전이규칙이 없습니다");
    expect(html).toContain("상태 전이 관리 권한이 없습니다");
  });

  it("exposes allowed page sizes and avoids absolute API URLs", async () => {
    const { businessStatusTransitionApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<BusinessStatusTransitionPage />);

    expect(
      businessStatusTransitionApi.listBusinessStatusTransitions,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<BusinessStatusTransitionPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

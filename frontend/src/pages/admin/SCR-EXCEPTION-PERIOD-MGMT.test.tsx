import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { ExceptionPeriodManagementPage } from "./SCR-EXCEPTION-PERIOD-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    exceptionPeriodApi: {
      listExceptionPeriods: vi.fn(async () => ({
        success: true,
        data: {
          exceptionPeriods: [
            {
              settingId: 701,
              evaluationYear: "2026",
              teacherUserId: 2,
              teacherName: "홍길동",
              areaCode: "EDUCATION",
              targetFunctionCode: "MODIFY_ACHIEVEMENT",
              exceptionStartAt: "2026-08-01T09:00:00",
              exceptionEndAt: "2026-08-05T18:00:00",
              approvalReason: "학회 출장으로 승인된 예외",
              activeYn: "Y",
              changeReason: "BASIC-40 예외기간 저장",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveExceptionPeriod: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-EXCEPTION-PERIOD-MGMT", () => {
  it("renders route contract, target fields, approval reason, and priority rule guidance", () => {
    const html = renderToStaticMarkup(<ExceptionPeriodManagementPage />);

    expect(html).toContain('data-screen-id="SCR-EXCEPTION-PERIOD-MGMT"');
    expect(html).toContain('data-testid="exception-period-page"');
    expect(html).toContain("평가 기준 관리 / 업무기간 관리 / 예외기간 관리");
    expect(html).toContain("예외기간 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("대상 교원 ID");
    expect(html).toContain("평가영역 코드");
    expect(html).toContain("대상 기능");
    expect(html).toContain("예외 시작일시");
    expect(html).toContain("예외 종료일시");
    expect(html).toContain("승인사유");
    expect(html).toContain("일반기간은 변경하지 않습니다");
    const source = ExceptionPeriodManagementPage.toString();
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("저장되었습니다");
    expect(source).toContain("조회된 예외기간이 없습니다");
    expect(source).toContain("예외기간 관리 권한이 없습니다");
    expect(source).toContain("평가확정 수정 금지는 예외기간보다 우선");
  });

  it("exposes pagination sizes and uses relative exception period API paths", async () => {
    await import("../../api/apiClient");
    const source = ExceptionPeriodManagementPage.toString();
    const html = renderToStaticMarkup(<ExceptionPeriodManagementPage />);

    expect(source).toContain("listExceptionPeriods");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

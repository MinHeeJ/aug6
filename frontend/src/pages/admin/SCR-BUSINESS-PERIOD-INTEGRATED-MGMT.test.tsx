import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { BusinessPeriodManagementPage } from "./SCR-BUSINESS-PERIOD-INTEGRATED-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    businessPeriodApi: {
      listBusinessPeriods: vi.fn(async () => ({
        success: true,
        data: {
          businessPeriods: [
            {
              settingId: 201,
              evaluationYear: "2026",
              areaCode: "EDUCATION",
              organizationCode: "KNUE-COL-EDU",
              userTypeCode: "FACULTY",
              startAt: "2026-05-16T09:00:00",
              endAt: "2026-05-31T18:00:00",
              baseDate: "2026-05-16",
              activeYn: "Y",
              changeReason: "BASIC-35 평가·업적입력 기간 저장",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveBusinessPeriod: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-BUSINESS-PERIOD-INTEGRATED-MGMT", () => {
  it("renders route contract, state copy, required fields, and save confirmation behavior", () => {
    const html = renderToStaticMarkup(<BusinessPeriodManagementPage />);

    expect(html).toContain(
      'data-screen-id="SCR-BUSINESS-PERIOD-INTEGRATED-MGMT"',
    );
    expect(html).toContain('data-testid="business-period-page"');
    expect(html).toContain(
      "평가 기준 관리 / 업무기간 관리 / 평가·업적입력 기간 관리",
    );
    expect(html).toContain("평가·업적입력 기간 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("소속/학과 코드");
    expect(html).toContain("평가영역 코드");
    expect(html).toContain("시작일시");
    expect(html).toContain("종료일시");
    expect(html).toContain("변경 사유");
    const source = BusinessPeriodManagementPage.toString();
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("저장되었습니다");
    expect(source).toContain("조회된 평가·업적입력 기간이 없습니다");
    expect(source).toContain("평가·업적입력 기간 관리 권한이 없습니다");
  });

  it("exposes pagination sizes and uses relative business period API client", async () => {
    const { businessPeriodApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<BusinessPeriodManagementPage />);

    expect(businessPeriodApi.listBusinessPeriods).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<BusinessPeriodManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

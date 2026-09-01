import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { ResultViewPeriodManagementPage } from "./SCR-RESULT-VIEW-PERIOD-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    resultViewPeriodApi: {
      listResultViewPeriods: vi.fn(async () => ({
        success: true,
        data: {
          resultViewPeriods: [
            {
              settingId: 501,
              evaluationYear: "2026",
              collegeOrganizationCode: "KNUE-COL-EDU",
              departmentOrganizationCode: "KNUE-DEPT-COMP",
              viewStartAt: "2026-07-01T09:00:00",
              viewEndAt: "2026-07-10T18:00:00",
              visibilityScope: "SELF",
              activeYn: "Y",
              changeReason: "BASIC-40 결과조회기간 저장",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveResultViewPeriod: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-RESULT-VIEW-PERIOD-MGMT", () => {
  it("renders route contract, state copy, visibility scope field, and save confirmation behavior", () => {
    const html = renderToStaticMarkup(<ResultViewPeriodManagementPage />);

    expect(html).toContain('data-screen-id="SCR-RESULT-VIEW-PERIOD-MGMT"');
    expect(html).toContain('data-testid="result-view-period-page"');
    expect(html).toContain(
      "평가 기준 관리 / 업무기간 관리 / 결과조회기간 관리",
    );
    expect(html).toContain("결과조회기간 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("소속대학 코드");
    expect(html).toContain("학과 코드");
    expect(html).toContain("공개 범위");
    expect(html).toContain("공개 시작일시");
    expect(html).toContain("공개 종료일시");
    expect(html).toContain("변경 사유");
    const source = ResultViewPeriodManagementPage.toString();
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("저장되었습니다");
    expect(source).toContain("조회된 결과조회기간이 없습니다");
    expect(source).toContain("결과조회기간 관리 권한이 없습니다");
    expect(source).toContain(
      "결과 생성·수정·확정취소는 이 화면에서 수행하지 않습니다",
    );
  });

  it("exposes pagination sizes and uses relative result view period API client", async () => {
    const { resultViewPeriodApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<ResultViewPeriodManagementPage />);

    expect(resultViewPeriodApi.listResultViewPeriods).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<ResultViewPeriodManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

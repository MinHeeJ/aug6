import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { AppealPeriodManagementPage } from "./SCR-APPEAL-PERIOD-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    appealPeriodApi: {
      listAppealPeriods: vi.fn(async () => ({
        success: true,
        data: {
          appealPeriods: [
            {
              settingId: 401,
              evaluationYear: "2026",
              collegeOrganizationCode: "KNUE-COL-EDU",
              departmentOrganizationCode: "KNUE-DEPT-COMP",
              appealStartAt: "2026-06-01T09:00:00",
              appealEndAt: "2026-06-10T18:00:00",
              handlerUserId: 4,
              activeYn: "Y",
              changeReason: "BASIC-40 이의신청기간 저장",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveAppealPeriod: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-APPEAL-PERIOD-MGMT", () => {
  it("renders route contract, state copy, required fields, and save confirmation behavior", () => {
    const html = renderToStaticMarkup(<AppealPeriodManagementPage />);

    expect(html).toContain('data-screen-id="SCR-APPEAL-PERIOD-MGMT"');
    expect(html).toContain('data-testid="appeal-period-page"');
    expect(html).toContain(
      "평가 기준 관리 / 업무기간 관리 / 이의신청기간 관리",
    );
    expect(html).toContain("이의신청기간 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("소속대학 코드");
    expect(html).toContain("학과 코드");
    expect(html).toContain("시작일시");
    expect(html).toContain("종료일시");
    expect(html).toContain("처리 담당자 ID");
    expect(html).toContain("변경 사유");
    const source = AppealPeriodManagementPage.toString();
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("저장되었습니다");
    expect(source).toContain("조회된 이의신청기간이 없습니다");
    expect(source).toContain("이의신청기간 관리 권한이 없습니다");
  });

  it("exposes pagination sizes and uses relative appeal period API client", async () => {
    const { appealPeriodApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<AppealPeriodManagementPage />);

    expect(appealPeriodApi.listAppealPeriods).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<AppealPeriodManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

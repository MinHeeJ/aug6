import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { ModificationPeriodManagementPage } from "./SCR-MODIFICATION-PERIOD-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    modificationPeriodApi: {
      listModificationPeriods: vi.fn(async () => ({
        success: true,
        data: {
          modificationPeriods: [
            {
              settingId: 201,
              evaluationYear: "2026",
              areaCode: "EDUCATION",
              organizationCode: "KNUE-COL-EDU",
              userTypeCode: "FACULTY",
              startAt: "2026-04-01T09:00:00",
              endAt: "2026-04-30T18:00:00",
              baseDate: "2026-04-01",
              activeYn: "Y",
              changeReason: "BASIC-35 수정기간 저장",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveModificationPeriod: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-MODIFICATION-PERIOD-MGMT", () => {
  it("renders route contract, state copy, required fields, and save confirmation behavior", () => {
    const html = renderToStaticMarkup(<ModificationPeriodManagementPage />);

    expect(html).toContain('data-screen-id="SCR-MODIFICATION-PERIOD-MGMT"');
    expect(html).toContain('data-testid="modification-period-page"');
    expect(html).toContain("평가 기준 관리 / 업무기간 관리 / 수정기간 관리");
    expect(html).toContain("수정기간 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("소속/학과 코드");
    expect(html).toContain("평가영역 코드");
    expect(html).toContain("시작일시");
    expect(html).toContain("종료일시");
    expect(html).toContain("변경 사유");
    const source = ModificationPeriodManagementPage.toString();
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("저장되었습니다");
    expect(source).toContain("조회된 수정기간이 없습니다");
    expect(source).toContain("수정기간 관리 권한이 없습니다");
  });

  it("exposes pagination sizes and uses relative modification period API client", async () => {
    const { modificationPeriodApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<ModificationPeriodManagementPage />);

    expect(
      modificationPeriodApi.listModificationPeriods,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<ModificationPeriodManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

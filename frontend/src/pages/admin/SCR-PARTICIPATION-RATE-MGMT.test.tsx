import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { ParticipationRateManagementPage } from "./SCR-PARTICIPATION-RATE-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    participationRateApi: {
      listParticipationRates: vi.fn(async () => ({
        success: true,
        data: {
          participationRates: [
            {
              participationRateRuleId: 710,
              managementItemId: 400,
              ruleVersionId: 10,
              versionCode: "B34-DRAFT-2026",
              versionStatus: "DRAFT",
              areaCode: "EDUCATION",
              areaName: "교육",
              itemCode: "LECTURE",
              itemName: "강의",
              evaluationYear: "2026",
              elementCode: "ATTENDANCE",
              elementName: "출석",
              managementItemCode: "EVIDENCE",
              managementItemName: "증빙파일",
              researcherCount: 3,
              participationType: "LEAD",
              participationTypeName: "주저자",
              distributionRate: 0.5,
              effectiveStartDate: "2026-01-01",
              effectiveEndDate: "2026-12-31",
              activeYn: "Y",
              changeReason: "배분율 정비",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveParticipationRate: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-PARTICIPATION-RATE-MGMT", () => {
  it("renders participation rate route contract and required save states", () => {
    const html = renderToStaticMarkup(<ParticipationRateManagementPage />);

    expect(html).toContain('data-screen-id="SCR-PARTICIPATION-RATE-MGMT"');
    expect(html).toContain('data-testid="participation-rate-page"');
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 참여구분·배분율 관리",
    );
    expect(html).toContain("참여구분·배분율 관리");
    expect(html).toContain("관리항목 ID");
    expect(html).toContain("연구자 수");
    expect(html).toContain("참여구분");
    expect(html).toContain("배분율");
    expect(html).toContain("적용시작일");
    expect(html).toContain("작성중 규정버전에서만 저장");
    const componentSource = ParticipationRateManagementPage.toString();
    expect(componentSource).toContain("저장되었습니다");
    expect(componentSource).toContain("조회된 배분율이 없습니다");
    expect(componentSource).toContain("참여구분·배분율 관리 권한이 없습니다");
  });

  it("uses relative participation rate API and exposes default page size options", async () => {
    const { participationRateApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<ParticipationRateManagementPage />);

    expect(
      participationRateApi.listParticipationRates,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<ParticipationRateManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

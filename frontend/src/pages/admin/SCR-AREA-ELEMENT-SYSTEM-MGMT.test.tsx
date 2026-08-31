import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { AreaElementSystemManagementPage } from "./SCR-AREA-ELEMENT-SYSTEM-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    areaElementSystemApi: {
      listAreaElementSystems: vi.fn(async () => ({
        success: true,
        data: {
          areaElementSystems: [
            {
              systemSettingId: 500,
              elementId: 300,
              itemId: 200,
              areaId: 100,
              ruleVersionId: 10,
              versionCode: "B33-DRAFT-2026",
              versionStatus: "DRAFT",
              areaCode: "EDUCATION",
              areaName: "교육",
              itemCode: "LECTURE",
              itemName: "강의",
              evaluationYear: "2026",
              elementCode: "ATTENDANCE",
              elementName: "출석",
              targetScope: "DEPARTMENT",
              activeYn: "Y",
              changeReason: "영역별 체계 정비",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveAreaElementSystem: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-AREA-ELEMENT-SYSTEM-MGMT", () => {
  it("renders area element system route contract and required target scope states", () => {
    const html = renderToStaticMarkup(<AreaElementSystemManagementPage />);

    expect(html).toContain('data-screen-id="SCR-AREA-ELEMENT-SYSTEM-MGMT"');
    expect(html).toContain('data-testid="area-element-system-page"');
    expect(html).toContain(
      "평가 기준 관리 / 평가 기준정보 관리 / 영역별 평가요소 체계 관리",
    );
    expect(html).toContain("영역별 평가요소 체계 관리");
    expect(html).toContain("평가영역 코드");
    expect(html).toContain("평가항목 코드");
    expect(html).toContain("평가요소 코드");
    expect(html).toContain("적용 대상");
    expect(html).toContain("사용상태");
    expect(html).toContain("작성중 규정버전에서만 저장");
    const componentSource = AreaElementSystemManagementPage.toString();
    expect(componentSource).toContain("저장되었습니다");
    expect(componentSource).toContain("조회된 영역별 평가요소 체계가 없습니다");
    expect(componentSource).toContain(
      "영역별 평가요소 체계 관리 권한이 없습니다",
    );
    expect(html).toContain(
      "평가점수·배분율·계산식 설정과 관리항목 세부 입력필드 변경은 이 화면 범위가 아닙니다",
    );
  });

  it("uses relative area element system API and exposes default page size options", async () => {
    const { areaElementSystemApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<AreaElementSystemManagementPage />);

    expect(
      areaElementSystemApi.listAreaElementSystems,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<AreaElementSystemManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

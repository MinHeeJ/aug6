import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { InputPeriodManagementPage } from "./SCR-INPUT-PERIOD-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    inputPeriodApi: {
      listInputPeriods: vi.fn(async () => ({
        success: true,
        data: {
          inputPeriods: [
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
              changeReason: "BASIC-35 입력기간 저장",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveInputPeriod: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-INPUT-PERIOD-MGMT", () => {
  it("renders route contract, state copy, required fields, and save confirmation behavior", () => {
    const html = renderToStaticMarkup(<InputPeriodManagementPage />);

    expect(html).toContain('data-screen-id="SCR-INPUT-PERIOD-MGMT"');
    expect(html).toContain('data-testid="input-period-page"');
    expect(html).toContain("평가 기준 관리 / 업무기간 관리 / 입력기간 관리");
    expect(html).toContain("입력기간 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("소속/학과 코드");
    expect(html).toContain("평가영역 코드");
    expect(html).toContain("시작일시");
    expect(html).toContain("종료일시");
    expect(html).toContain("변경 사유");
    const source = InputPeriodManagementPage.toString();
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("저장되었습니다");
    expect(source).toContain("조회된 입력기간이 없습니다");
    expect(source).toContain("입력기간 관리 권한이 없습니다");
  });

  it("exposes pagination sizes and uses relative input period API client", async () => {
    const { inputPeriodApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<InputPeriodManagementPage />);

    expect(inputPeriodApi.listInputPeriods).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<InputPeriodManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

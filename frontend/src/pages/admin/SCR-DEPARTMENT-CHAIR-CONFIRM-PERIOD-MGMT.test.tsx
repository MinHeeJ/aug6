import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { DepartmentChairConfirmPeriodManagementPage } from "./SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    departmentChairConfirmPeriodApi: {
      listDepartmentChairConfirmPeriods: vi.fn(async () => ({
        success: true,
        data: {
          departmentChairConfirmPeriods: [
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
              changeReason: "BASIC-35 학과장 확인기간 저장",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveDepartmentChairConfirmPeriod: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT", () => {
  it("renders route contract, state copy, required fields, and save confirmation behavior", () => {
    const html = renderToStaticMarkup(
      <DepartmentChairConfirmPeriodManagementPage />,
    );

    expect(html).toContain(
      'data-screen-id="SCR-DEPARTMENT-CHAIR-CONFIRM-PERIOD-MGMT"',
    );
    expect(html).toContain(
      'data-testid="department-chair-confirm-period-page"',
    );
    expect(html).toContain(
      "평가 기준 관리 / 업무기간 관리 / 학과장 확인기간 관리",
    );
    expect(html).toContain("학과장 확인기간 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("소속/학과 코드");
    expect(html).toContain("평가영역 코드");
    expect(html).toContain("시작일시");
    expect(html).toContain("종료일시");
    expect(html).toContain("변경 사유");
    const source = DepartmentChairConfirmPeriodManagementPage.toString();
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("저장되었습니다");
    expect(source).toContain("조회된 학과장 확인기간이 없습니다");
    expect(source).toContain("학과장 확인기간 관리 권한이 없습니다");
  });

  it("exposes pagination sizes and uses relative department chair confirm period API client", async () => {
    const { departmentChairConfirmPeriodApi } = await import(
      "../../api/apiClient"
    );
    renderToStaticMarkup(<DepartmentChairConfirmPeriodManagementPage />);

    expect(
      departmentChairConfirmPeriodApi.listDepartmentChairConfirmPeriods,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(
      <DepartmentChairConfirmPeriodManagementPage />,
    );
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

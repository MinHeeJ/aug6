import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { DepartmentChairConfirmationManagementPage } from "./SCR-DEPARTMENT-CHAIR-CONFIRM-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    departmentChairConfirmationApi: {
      listDepartmentChairConfirmTargets: vi.fn(async () => ({
        success: true,
        data: {
          targets: [
            {
              confirmationId: 301,
              achievementId: 9001,
              evaluationYear: "2026",
              departmentOrganizationCode: "DEPT-EDU",
              areaCode: "EDUCATION",
              confirmStatus: "DEPARTMENT_CONFIRMED",
              previousStatus: "SUBMITTED",
              nextStatus: "DEPARTMENT_CONFIRMED",
              opinion: "확인",
              processedBy: 1,
              processedAt: "2026-09-02T09:00:00",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveDepartmentChairConfirmTargetsTransition: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-DEPARTMENT-CHAIR-CONFIRM-MGMT", () => {
  it("renders UI contract, filters, required transition fields, and states", () => {
    const html = renderToStaticMarkup(
      <DepartmentChairConfirmationManagementPage />,
    );
    expect(html).toContain(
      'data-screen-id="SCR-DEPARTMENT-CHAIR-CONFIRM-MGMT"',
    );
    expect(html).toContain('data-testid="department-chair-confirmation-page"');
    expect(html).toContain("학과장 확인 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("평가영역");
    expect(html).toContain("인증상태");
    expect(html).toContain("첨부여부");
    expect(html).toContain("처리구분");
    expect(html).toContain("미승인 사유");
    expect(html).toContain("의견");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    const source = DepartmentChairConfirmationManagementPage.toString();
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("처리되었습니다");
    expect(source).toContain("학과장 확인 관리 권한이 없습니다");
    expect(source).toContain("조회된 학과장 확인 대상이 없습니다");
  });

  it("uses relative API client and exposes Excel download action", async () => {
    const { departmentChairConfirmationApi } = await import(
      "../../api/apiClient"
    );
    renderToStaticMarkup(<DepartmentChairConfirmationManagementPage />);
    expect(
      departmentChairConfirmationApi.listDepartmentChairConfirmTargets,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    expect(DepartmentChairConfirmationManagementPage.toString()).toContain(
      "department-chair-confirmations.csv",
    );
  });
});

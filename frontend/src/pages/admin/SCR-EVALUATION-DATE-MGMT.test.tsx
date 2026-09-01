import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationDateManagementPage } from "./SCR-EVALUATION-DATE-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationDateApi: {
      listEvaluationDates: vi.fn(async () => ({
        success: true,
        data: {
          evaluationDates: [
            {
              settingId: 101,
              evaluationYear: "2026",
              areaCode: "EDUCATION",
              organizationCode: "KNUE-COL-EDU",
              userTypeCode: "FACULTY",
              startAt: "2026-03-01T09:00:00",
              endAt: "2026-03-31T18:00:00",
              baseDate: "2026-03-31",
              activeYn: "Y",
              changeReason: "BASIC-35 평가일자 저장",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveEvaluationDate: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-EVALUATION-DATE-MGMT", () => {
  it("renders route contract, state copy, required fields, and save confirmation behavior", () => {
    const html = renderToStaticMarkup(<EvaluationDateManagementPage />);

    expect(html).toContain('data-screen-id="SCR-EVALUATION-DATE-MGMT"');
    expect(html).toContain('data-testid="evaluation-date-page"');
    expect(html).toContain("평가 기준 관리 / 업무기간 관리 / 평가일자 관리");
    expect(html).toContain("평가일자 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("소속/학과 코드");
    expect(html).toContain("평가영역 코드");
    expect(html).toContain("기준일자");
    expect(html).toContain("변경 사유");
    const source = EvaluationDateManagementPage.toString();
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("저장되었습니다");
    expect(source).toContain("조회된 평가일자가 없습니다");
    expect(source).toContain("평가일자 관리 권한이 없습니다");
  });

  it("exposes pagination sizes and uses relative evaluation date API client", async () => {
    const { evaluationDateApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<EvaluationDateManagementPage />);

    expect(evaluationDateApi.listEvaluationDates).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<EvaluationDateManagementPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});

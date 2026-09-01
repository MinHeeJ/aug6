import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { FullTimeFacultyStatusPage } from "./SCR-FULL-TIME-FACULTY-STATUS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    fullTimeFacultyStatusApi: {
      listStatuses: vi.fn(async () => ({
        success: true,
        data: {
          statuses: [
            {
              employeeNo: "E1001",
              name: "홍길동",
              collegeCode: "KNUE-COL-EDU",
              collegeName: "교육과학대학",
              departmentCode: "KNUE-DEPT-COMP",
              departmentName: "컴퓨터교육과",
              rankName: "교수",
              retirementDate: null,
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
          baseYear: 2026,
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-FULL-TIME-FACULTY-STATUS", () => {
  it("renders the faculty status route contract and required query fields", () => {
    const html = renderToStaticMarkup(<FullTimeFacultyStatusPage />);

    expect(html).toContain('data-screen-id="SCR-FULL-TIME-FACULTY-STATUS"');
    expect(html).toContain('data-testid="full-time-faculty-status-page"');
    expect(html).toContain("교수업적평가 / 기준정보 조회 / 전임교원 현황");
    expect(html).toContain("기준연도");
    expect(html).toContain("소속");
    expect(html).toContain("교번");
    expect(html).toContain("성명");
    expect(html).toContain("대학");
    expect(html).toContain("학과");
    expect(html).toContain("직급");
    expect(html).toContain("퇴직일자");
  });

  it("exposes read-only list states, pagination sizes, and no mutation CTA", () => {
    const html = renderToStaticMarkup(<FullTimeFacultyStatusPage />);

    expect(html).toContain("조회");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain("조회 전용");
    expect(html).not.toContain("저장");
    expect(html).not.toContain("삭제");
  });
});

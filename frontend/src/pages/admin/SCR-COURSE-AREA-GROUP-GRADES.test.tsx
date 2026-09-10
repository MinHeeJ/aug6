import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { canAccessAdminRoute } from "../LoginPage";
import { CourseAreaGroupGradesPage } from "./SCR-COURSE-AREA-GROUP-GRADES";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    courseAreaGroupGradeApi: {
      listCourseAreaGroupGrades: vi.fn(async () => ({
        success: true,
        data: {
          courseAreaGroupGrades: [
            {
              gradeId: 5900401,
              evaluationYear: "2026",
              teacherUserId: 2,
              teacherName: "김교수",
              collegeCode: "KNUE-COL-EDU",
              departmentCode: "KNUE-DEPT-COMP",
              completionTypeCode: "LIBERAL_ARTS",
              semesterCode: "SEMESTER_1",
              courseAreaCode: "CORE_LITERACY",
              courseAreaName: "교양 핵심소양",
              groupGrade: "A_PLUS",
              totalScore: 95.5,
              publishedYn: "Y",
              finalizationStatus: "CERTIFIED",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      getCourseAreaGroupGradeDetail: vi.fn(async () => ({
        success: true,
        data: {
          gradeId: 5900401,
          evaluationYear: "2026",
          teacherUserId: 2,
          teacherName: "김교수",
          collegeCode: "KNUE-COL-EDU",
          departmentCode: "KNUE-DEPT-COMP",
          completionTypeCode: "LIBERAL_ARTS",
          semesterCode: "SEMESTER_1",
          courseAreaCode: "CORE_LITERACY",
          courseAreaName: "교양 핵심소양",
          groupGrade: "A_PLUS",
          totalScore: 95.5,
          publishedYn: "Y",
          finalizationStatus: "CERTIFIED",
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-COURSE-AREA-GROUP-GRADES", () => {
  it("renders FR-024 search conditions, grade columns, detail region, and 403 guidance", () => {
    const html = renderToStaticMarkup(<CourseAreaGroupGradesPage />);

    expect(html).toContain('data-screen-id="SCR-COURSE-AREA-GROUP-GRADES"');
    expect(html).toContain('data-testid="course-area-group-grades-page"');
    expect(html).toContain("교과영역 그룹평가 성적 조회");
    expect(html).toContain("교원 식별자");
    expect(html).toContain("이수구분");
    expect(html).toContain("학기");
    expect(html).toContain("교과영역");
    expect(html).toContain("성적");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain("엑셀 내려받기");
    expect(html).toContain(
      'data-testid="course-area-group-grades-excel-button"',
    );
    expect(html).toContain("상세");
    expect(html).toContain("R01 교원은 본인 성적만 조회할 수 있습니다");
  });

  it("allows R01 and R04 route access but hides FR-024 from unrelated roles without menu permission", () => {
    const route = "/evaluation/course-area-group-grades";
    const menus = [
      {
        menuId: 568,
        menuName: "교과영역 그룹평가 성적 조회",
        displayOrder: 4,
        children: [],
        url: route,
      },
    ];

    expect(
      canAccessAdminRoute(
        {
          userId: 1,
          loginId: "professor1",
          name: "교원",
          roles: ["R01"],
          menus,
        },
        route,
      ),
    ).toBe(true);
    expect(
      canAccessAdminRoute(
        {
          userId: 4,
          loginId: "business-admin",
          name: "담당자",
          roles: ["R04"],
          menus,
        },
        route,
      ),
    ).toBe(true);
    expect(
      canAccessAdminRoute(
        { userId: 9, loginId: "admin", name: "관리자", roles: ["R09"], menus },
        route,
      ),
    ).toBe(false);
    expect(
      canAccessAdminRoute(
        {
          userId: 8,
          loginId: "auditor",
          name: "감사",
          roles: ["R08"],
          menus: [],
        },
        route,
      ),
    ).toBe(false);
  });
});

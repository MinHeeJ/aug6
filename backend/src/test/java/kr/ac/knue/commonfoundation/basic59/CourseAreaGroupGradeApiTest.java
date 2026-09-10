package kr.ac.knue.commonfoundation.basic59;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourseAreaGroupGradeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseAreaGroupGradeApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean CourseAreaGroupGradeService service;

    private final CurrentUser r01 = new CurrentUser(1L, "professor1", "E0001", "교원", List.of("R01"), List.of());
    private final CurrentUser r04 = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser r08 = new CurrentUser(8L, "auditor", "E0008", "감사담당자", List.of("R08"), List.of());

    @Test
    void listCourseAreaGroupGradesSupportsTeacherCompletionSemesterCourseAreaFiltersAndPagination() throws Exception {
        when(service.list(any(CourseAreaGroupGradeSearchCriteria.class), eq(r04), eq("REQ-B59-FR024-LIST"))).thenReturn(response());

        mockMvc.perform(get("/api/business/course-area-group-grades")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B59-FR024-LIST")
                        .param("page", "0")
                        .param("pageSize", "20")
                        .param("teacherUserId", "2")
                        .param("completionTypeCode", "LIBERAL_ARTS")
                        .param("semesterCode", "SEMESTER_1")
                        .param("courseAreaCode", "CORE_LITERACY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseAreaGroupGrades[0].gradeId").value(5900401))
                .andExpect(jsonPath("$.data.courseAreaGroupGrades[0].teacherUserId").value(2))
                .andExpect(jsonPath("$.data.courseAreaGroupGrades[0].completionTypeCode").value("LIBERAL_ARTS"))
                .andExpect(jsonPath("$.data.courseAreaGroupGrades[0].semesterCode").value("SEMESTER_1"))
                .andExpect(jsonPath("$.data.courseAreaGroupGrades[0].courseAreaCode").value("CORE_LITERACY"))
                .andExpect(jsonPath("$.data.courseAreaGroupGrades[0].groupGrade").value("A_PLUS"))
                .andExpect(jsonPath("$.data.courseAreaGroupGrades[0].totalScore").value(95.50))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B59-FR024-LIST"));
    }

    @Test
    void getCourseAreaGroupGradeDetailReturnsSelectedGradeDetail() throws Exception {
        when(service.getDetail(5900401L, r04, "REQ-B59-FR024-DETAIL")).thenReturn(row());

        mockMvc.perform(get("/api/business/course-area-group-grades/5900401")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B59-FR024-DETAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gradeId").value(5900401))
                .andExpect(jsonPath("$.data.courseAreaName").value("교양 핵심소양"))
                .andExpect(jsonPath("$.data.finalizationStatus").value("CERTIFIED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B59-FR024-DETAIL"));
    }

    @Test
    void r01CannotReadOtherTeachersDetailAndUnauthorizedRolesAreForbidden() throws Exception {
        when(service.getDetail(5900401L, r01, "REQ-B59-FR024-R01"))
                .thenThrow(new ForbiddenException());

        mockMvc.perform(get("/api/business/course-area-group-grades/5900401")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B59-FR024-R01"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/business/course-area-group-grades").requestAttr("currentUser", r08).cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any(), eq(r08), any());
    }

    @Test
    void serviceAppliesR01SelfScopeAndRecordsDataAccessHistory() {
        CourseAreaGroupGradeMapper mapper = org.mockito.Mockito.mock(CourseAreaGroupGradeMapper.class);
        CourseAreaGroupGradeService gradeService = new CourseAreaGroupGradeService(mapper);
        CourseAreaGroupGradeSearchCriteria r01Criteria = new CourseAreaGroupGradeSearchCriteria(0, 20, 2L, "LIBERAL_ARTS", "SEMESTER_1", "CORE_LITERACY");
        when(mapper.listCourseAreaGroupGrades(any())).thenReturn(List.of(rowForTeacher(1L)));
        when(mapper.countCourseAreaGroupGrades(any())).thenReturn(1L);

        CourseAreaGroupGradeSearchResponse listed = gradeService.list(r01Criteria, r01, "REQ-B59-FR024-AUDIT");

        org.assertj.core.api.Assertions.assertThat(listed.courseAreaGroupGrades()).hasSize(1);
        verify(mapper).listCourseAreaGroupGrades(org.mockito.ArgumentMatchers.argThat(criteria -> criteria.effectiveTeacherUserId().equals(1L)));
        verify(mapper).insertDataAccessHistory(eq("COURSE_AREA_GROUP_GRADE"), eq(1L), eq("teacherUserId=1;completionTypeCode=LIBERAL_ARTS;semesterCode=SEMESTER_1;courseAreaCode=CORE_LITERACY"), eq("교과영역 그룹평가 성적 목록 조회"), eq("REQ-B59-FR024-AUDIT"));

        when(mapper.findCourseAreaGroupGradeById(5900401L)).thenReturn(rowForTeacher(2L));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> gradeService.getDetail(5900401L, r01, "REQ-B59-FR024-DENY"))
                .isInstanceOf(ForbiddenException.class);
        verify(mapper, never()).insertDataAccessHistory(eq("COURSE_AREA_GROUP_GRADE_DETAIL"), eq(1L), eq("gradeId=5900401"), any(), any());
    }

    private CourseAreaGroupGradeSearchResponse response() {
        return new CourseAreaGroupGradeSearchResponse(List.of(row()), 0, 20, 1);
    }

    private CourseAreaGroupGradeRow row() {
        return rowForTeacher(2L);
    }

    private CourseAreaGroupGradeRow rowForTeacher(Long teacherUserId) {
        return new CourseAreaGroupGradeRow(5900401L, "2026", teacherUserId, teacherUserId == 1L ? "교원" : "김교수", "KNUE-COL-EDU", "KNUE-DEPT-COMP", "LIBERAL_ARTS", "SEMESTER_1", "CORE_LITERACY", "교양 핵심소양", "A_PLUS", new BigDecimal("95.50"), "Y", "CERTIFIED", LocalDateTime.parse("2026-09-01T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

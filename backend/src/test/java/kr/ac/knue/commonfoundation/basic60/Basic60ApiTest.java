package kr.ac.knue.commonfoundation.basic60;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(Basic60Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic60ApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean Basic60Service service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listEvaluationElementManagementItemSettingsReturnsContractEnvelope() throws Exception {
        when(service.listElementSettings(any())).thenReturn(new OperationalSettingResponses.EvaluationElementManagementItemSettingSearchResponse(List.of(elementRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/evaluation-element-management-item-settings")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B60-ELEMENT-LIST")
                        .param("evaluationYear", "2026")
                        .param("areaCode", "EDUCATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].managementItemCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.evaluationElementManagementItemSettings[0].teacherEditableYn").value("Y"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B60-ELEMENT-LIST"));
    }

    @Test
    void saveEvaluationElementManagementItemSettingRejectsR01AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementPayload(10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).saveElementSetting(any(), any(), any());
    }

    @Test
    void saveEvaluationElementManagementItemSettingReturnsConflictForConfirmedRuleLock() throws Exception {
        when(service.saveElementSetting(any(), eq(1L), eq("REQ-B60-ELEMENT-CONFLICT")))
                .thenThrow(new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/evaluation-element-management-item-settings/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B60-ELEMENT-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementPayload(11L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void listParticipationAllocationRateSettingsReturnsAllocationMatrixRows() throws Exception {
        when(service.listParticipationSettings(any())).thenReturn(new OperationalSettingResponses.ParticipationAllocationRateSettingSearchResponse(List.of(participationRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/participation-allocation-rate-settings")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .param("managementItemCode", "PAPER_SCORE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].researcherCount").value(3))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].participationType").value("LEAD"))
                .andExpect(jsonPath("$.data.participationAllocationRateSettings[0].allocationRate").value(0.7));
    }

    @Test
    void saveParticipationAllocationRateSettingRequiresFields() throws Exception {
        mockMvc.perform(post("/api/admin/participation-allocation-rate-settings/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetScope":"COLLEGE_EDU","areaCode":"RESEARCH","itemCode":"PAPER","evaluationYear":"2026","elementCode":"AUTHORSHIP","managementItemCode":"PAPER_SCORE","researcherCount":3,"participationType":"LEAD","allocationRate":0.7,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listManagementItemEvaluationScoreSettingsReturnsCollegeScoreRows() throws Exception {
        when(service.listScoreSettings(any())).thenReturn(new OperationalSettingResponses.ManagementItemEvaluationScoreSettingSearchResponse(List.of(scoreRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/management-item-evaluation-score-settings")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .param("organizationCode", "COL-EDU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].organizationCode").value("COL-EDU"))
                .andExpect(jsonPath("$.data.managementItemEvaluationScoreSettings[0].evaluationScore").value(10.0));
    }

    @Test
    void saveManagementItemEvaluationScoreSettingReturnsSavedScore() throws Exception {
        when(service.saveScoreSetting(any(), eq(4L), eq("REQ-B60-SCORE-SAVE"))).thenReturn(scoreRow());

        mockMvc.perform(post("/api/admin/management-item-evaluation-score-settings/save")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B60-SCORE-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","organizationCode":"COL-EDU","organizationName":"사범대학","evaluationScore":10.0,"maxScore":20.0,"sortOrder":1,"activeYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"점수 설정"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.evaluationScore").value(10.0));
    }

    @Test
    void listCourseAreaGroupGradesLimitsR01ToOwnGradesAndReturnsReadOnlyRows() throws Exception {
        when(service.listCourseAreaGroupGrades(any(), eq(teacher), eq("REQ-B60-GRADE-LIST")))
                .thenReturn(new CourseAreaGroupGradeSearchResponse(List.of(gradeRow()), 0, 20, 1));

        mockMvc.perform(get("/api/faculty/course-area-group-grades")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B60-GRADE-LIST")
                        .param("completionType", "MAJOR")
                        .param("semester", "2026-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseAreaGroupGrades[0].facultyUserId").value(2))
                .andExpect(jsonPath("$.data.courseAreaGroupGrades[0].completionType").value("MAJOR"))
                .andExpect(jsonPath("$.data.courseAreaGroupGrades[0].courseArea").value("LECTURE"))
                .andExpect(jsonPath("$.data.courseAreaGroupGrades[0].groupGrade").value(95.5));
    }

    @Test
    void serviceBlocksR01FromQueryingOtherFacultyGrades() {
        Basic60Mapper mapper = org.mockito.Mockito.mock(Basic60Mapper.class);
        Basic60Service basic60Service = new Basic60Service(mapper);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> basic60Service.listCourseAreaGroupGrades(
                        new CourseAreaGroupGradeSearchCriteria(0, 20, "MAJOR", "2026-1", "LECTURE", 4L, null), teacher, "REQ-B60-GRADE-FORBIDDEN"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ForbiddenException.class);
        verify(mapper, never()).listCourseAreaGroupGrades(any());
    }

    private String elementPayload(Long ruleVersionId) {
        return """
                {"ruleVersionId":%d,"targetScope":"COLLEGE_EDU","areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"COURSE_GROUP","managementItemCode":"ATTENDANCE","managementItemName":"출석관리","sortOrder":1,"activeYn":"Y","teacherEditableYn":"Y","effectiveStartDate":"2026-01-01","effectiveEndDate":"2026-12-31","changeReason":"항목 설정"}
                """.formatted(ruleVersionId);
    }

    private OperationalSettingRow elementRow() {
        return new OperationalSettingRow(1001L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026", "COURSE_GROUP", null, "ATTENDANCE", "출석관리", null, null, null, null, null, null, null, 1, "Y", "Y", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "항목 설정", 1L, LocalDateTime.parse("2026-09-10T09:00:00"));
    }

    private OperationalSettingRow participationRow() {
        return new OperationalSettingRow(2001L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "RESEARCH", "PAPER", "2026", "AUTHORSHIP", null, "PAPER_SCORE", null, null, null, 3, "LEAD", BigDecimal.valueOf(0.7), null, null, null, "Y", null, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "배분율 설정", 1L, LocalDateTime.parse("2026-09-10T09:00:00"));
    }

    private OperationalSettingRow scoreRow() {
        return new OperationalSettingRow(3001L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026", "COURSE_GROUP", null, "ATTENDANCE", null, "COL-EDU", "사범대학", null, null, null, BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0), 1, "Y", null, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "점수 설정", 4L, LocalDateTime.parse("2026-09-10T09:00:00"));
    }

    private CourseAreaGroupGradeRow gradeRow() {
        return new CourseAreaGroupGradeRow(4001L, 2L, "E0002", "교원사용자", "MAJOR", "2026-1", "LECTURE", BigDecimal.valueOf(95.5), "전공 강의 그룹평가 상위 등급", "Y", LocalDateTime.parse("2026-09-10T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

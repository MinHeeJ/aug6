package kr.ac.knue.commonfoundation.basic59;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        EvaluationElementManagementItemController.class,
        ParticipationRateOperationSettingController.class,
        ManagementItemEvaluationScoreController.class,
        CourseAreaGroupGradeController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic59CommonVerificationApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationElementManagementItemService elementService;
    @MockBean ParticipationRateOperationSettingService participationService;
    @MockBean ManagementItemEvaluationScoreService scoreService;
    @MockBean CourseAreaGroupGradeService gradeService;

    private final CurrentUser r04 = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());

    @Test
    void everyBasic59ListApiDefaultsToTwentyAcceptsOnlyCommonPageSizesAndPropagatesRequestIdsForReq1753Req1759() throws Exception {
        when(elementService.list(any())).thenReturn(new EvaluationElementManagementItemSearchResponse(List.of(elementRow()), 0, 20, 1));
        when(participationService.list(any())).thenReturn(new ParticipationRateOperationSettingSearchResponse(List.of(participationRow()), 0, 20, 1));
        when(scoreService.list(any())).thenReturn(new ManagementItemEvaluationScoreSearchResponse(List.of(scoreRow()), 0, 20, 1));
        when(gradeService.list(any(), eq(r04), any())).thenReturn(new CourseAreaGroupGradeSearchResponse(List.of(gradeRow()), 0, 20, 1));

        assertListDefaultsAndPageSizes("/api/business/evaluation-element-management-items", "$.data.pageSize");
        assertListDefaultsAndPageSizes("/api/business/participation-rate-operation-settings", "$.data.pageSize");
        assertListDefaultsAndPageSizes("/api/business/management-item-evaluation-scores", "$.data.pageSize");
        assertListDefaultsAndPageSizes("/api/business/course-area-group-grades", "$.data.pageSize");
    }

    @Test
    void everyBasic59SaveApiReturnsApiErrorFieldsForRequiredServerValidationForReq1758() throws Exception {
        mockMvc.perform(post("/api/business/evaluation-element-management-items/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("ruleVersionId")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("evaluationYear")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("areaCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("elementCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("managementItemCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("managementItemName")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("teacherEditablePart")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("sortOrder")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("activeYn")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("changeReason")));

        mockMvc.perform(post("/api/business/participation-rate-operation-settings/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("ruleVersionId")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("achievementAreaCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("achievementCategoryCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("rates")));

        mockMvc.perform(post("/api/business/management-item-evaluation-scores/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("ruleVersionId")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("achievementAreaCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("achievementCategoryCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("managementItemCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("collegeCode")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("evaluationScore")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("sortOrder")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("activeYn")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("changeReason")));

        verify(elementService, never()).save(any(), any(), any());
        verify(participationService, never()).save(any(), any(), any());
        verify(scoreService, never()).save(any(), any(), any());
    }

    private void assertListDefaultsAndPageSizes(String path, String pageSizeJsonPath) throws Exception {
        mockMvc.perform(get(path)
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B59-COMMON-TRACE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(pageSizeJsonPath).value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B59-COMMON-TRACE"))
                .andExpect(jsonPath("$.meta.traceId", not(blankOrNullString())));

        for (String pageSize : List.of("20", "50", "100")) {
            mockMvc.perform(get(path)
                            .requestAttr("currentUser", r04)
                            .cookie(sessionCookie())
                            .param("pageSize", pageSize))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get(path)
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .param("pageSize", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("pageSize")));
    }

    private EvaluationElementManagementItemRow elementRow() {
        return new EvaluationElementManagementItemRow(5900101L, 1L, "B33-DRAFT-2026", "DRAFT", "2026", "EDUCATION", "LECTURE_EVALUATION", "LECTURE_EVAL_SCORE", "강의평가 점수", "점수 확인 및 의견 입력", 1, "Y", "B59-SEED-001", 9L, LocalDateTime.parse("2026-09-01T09:00:00"));
    }

    private ParticipationRateOperationSettingRow participationRow() {
        return new ParticipationRateOperationSettingRow(5900201L, 1L, "B33-DRAFT-2026", "DRAFT", "2026", "RESEARCH", "PAPER", "JOURNAL_ARTICLE", "TWO", "CORRESPONDING_AUTHOR", new BigDecimal("70.00"), "Y", "B59-SEED-002", 9L, LocalDateTime.parse("2026-09-01T09:00:00"));
    }

    private ManagementItemEvaluationScoreRow scoreRow() {
        return new ManagementItemEvaluationScoreRow(5900301L, 1L, "B33-DRAFT-2026", "DRAFT", "2026", "EDUCATION", "LECTURE", "LECTURE_EVAL_SCORE", "KNUE-COL-EDU", new BigDecimal("30.00"), 1, "Y", "B59-SEED-003", 9L, LocalDateTime.parse("2026-09-01T09:00:00"));
    }

    private CourseAreaGroupGradeRow gradeRow() {
        return new CourseAreaGroupGradeRow(5900401L, "2026", 2L, "김교수", "KNUE-COL-EDU", "KNUE-DEPT-COMP", "LIBERAL_ARTS", "SEMESTER_1", "CORE_LITERACY", "교양 핵심소양", "A_PLUS", new BigDecimal("95.50"), "Y", "CERTIFIED", LocalDateTime.parse("2026-09-01T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

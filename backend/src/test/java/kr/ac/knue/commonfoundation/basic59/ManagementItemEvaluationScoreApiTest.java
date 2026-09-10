package kr.ac.knue.commonfoundation.basic59;

import static org.hamcrest.Matchers.hasItem;
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

@WebMvcTest(ManagementItemEvaluationScoreController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ManagementItemEvaluationScoreApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ManagementItemEvaluationScoreService service;

    private final CurrentUser r04 = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser r09 = new CurrentUser(9L, "admin", "E0009", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser r01 = new CurrentUser(1L, "professor1", "E0001", "교원", List.of("R01"), List.of());
    private final CurrentUser r08 = new CurrentUser(8L, "auditor", "E0008", "감사담당자", List.of("R08"), List.of());

    @Test
    void listManagementItemEvaluationScoresSupportsAreaCategoryCollegeFiltersAndPagination() throws Exception {
        when(service.list(any(ManagementItemEvaluationScoreSearchCriteria.class))).thenReturn(response());

        mockMvc.perform(get("/api/business/management-item-evaluation-scores")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B59-FR020-LIST")
                        .param("page", "0")
                        .param("pageSize", "20")
                        .param("achievementAreaCode", "EDUCATION")
                        .param("achievementCategoryCode", "LECTURE")
                        .param("collegeCode", "KNUE-COL-EDU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.managementItemEvaluationScores[0].settingId").value(5900301))
                .andExpect(jsonPath("$.data.managementItemEvaluationScores[0].achievementAreaCode").value("EDUCATION"))
                .andExpect(jsonPath("$.data.managementItemEvaluationScores[0].achievementCategoryCode").value("LECTURE"))
                .andExpect(jsonPath("$.data.managementItemEvaluationScores[0].collegeCode").value("KNUE-COL-EDU"))
                .andExpect(jsonPath("$.data.managementItemEvaluationScores[0].evaluationScore").value(30.00))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B59-FR020-LIST"));

        mockMvc.perform(get("/api/business/management-item-evaluation-scores")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void saveManagementItemEvaluationScorePersistsScoreAndReturnsUpdatedRow() throws Exception {
        when(service.save(any(SaveManagementItemEvaluationScoreRequest.class), eq(r04), eq("REQ-B59-FR020-SAVE")))
                .thenReturn(savedRow());

        mockMvc.perform(post("/api/business/management-item-evaluation-scores/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B59-FR020-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("33.75", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationScore").value(33.75))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B59-FR020-SAVE"));
    }

    @Test
    void saveManagementItemEvaluationScoreReturnsFieldErrorForMissingEvaluationScore() throws Exception {
        mockMvc.perform(post("/api/business/management-item-evaluation-scores/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleVersionId\":1,\"achievementAreaCode\":\"EDUCATION\",\"achievementCategoryCode\":\"LECTURE\",\"managementItemCode\":\"LECTURE_EVAL_SCORE\",\"collegeCode\":\"KNUE-COL-EDU\",\"sortOrder\":1,\"activeYn\":\"Y\",\"changeReason\":\"검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("evaluationScore")));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void saveManagementItemEvaluationScoreRejectsConfirmedRuleVersionAsConflictNoChange() throws Exception {
        when(service.save(any(SaveManagementItemEvaluationScoreRequest.class), eq(r04), any()))
                .thenThrow(new ConflictException("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/business/management-item-evaluation-scores/save")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("41.00", 2L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("CONFIRMED_RULE_LOCKED: 확정 규정버전은 수정할 수 없습니다."));
    }

    @Test
    void r01AndR08CannotAccessManagementItemEvaluationScores() throws Exception {
        mockMvc.perform(get("/api/business/management-item-evaluation-scores").requestAttr("currentUser", r01).cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(post("/api/business/management-item-evaluation-scores/save")
                        .requestAttr("currentUser", r08)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("33.75", 1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionBeforePersistenceAndRecordsChangeHistoryOnSave() {
        ManagementItemEvaluationScoreMapper mapper = org.mockito.Mockito.mock(ManagementItemEvaluationScoreMapper.class);
        ManagementItemEvaluationScoreService scoreService = new ManagementItemEvaluationScoreService(mapper);
        when(mapper.findRuleVersionStatus(2L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> scoreService.save(validRequest(2L), r04, "REQ-B59-LOCK"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_RULE_LOCKED");
        verify(mapper, never()).upsertManagementItemEvaluationScore(any(), any(), any());

        when(mapper.findRuleVersionStatus(1L)).thenReturn("DRAFT");
        when(mapper.findRuleVersionEvaluationYear(1L)).thenReturn("2026");
        when(mapper.managementItemMatchesCategory(1L, "EDUCATION", "LECTURE", "LECTURE_EVAL_SCORE")).thenReturn(true);
        when(mapper.findByBusinessKey(1L, "2026", "EDUCATION", "LECTURE_EVAL_SCORE", "KNUE-COL-EDU")).thenReturn(row());
        when(mapper.upsertManagementItemEvaluationScore(any(), eq(4L), eq("REQ-B59-AUDIT"))).thenReturn(savedRow());

        scoreService.save(validRequest(1L), r04, "REQ-B59-AUDIT");

        verify(mapper).insertChangeHistory(eq("management_item_score_settings"),
                eq("1:2026:EDUCATION:LECTURE_EVAL_SCORE:KNUE-COL-EDU"), eq("UPDATE"),
                eq("evaluationScore"), any(), any(), eq(4L), eq("FR-020 저장"), eq("REQ-B59-AUDIT"));
    }

    private ManagementItemEvaluationScoreSearchResponse response() {
        return new ManagementItemEvaluationScoreSearchResponse(List.of(row()), 0, 20, 1);
    }

    private ManagementItemEvaluationScoreRow row() {
        return new ManagementItemEvaluationScoreRow(5900301L, 1L, "B33-DRAFT-2026", "DRAFT", "2026", "EDUCATION", "LECTURE", "LECTURE_EVAL_SCORE", "KNUE-COL-EDU", new BigDecimal("30.00"), 1, "Y", "B59-SEED-003 교육영역 A대학 점수", 9L, LocalDateTime.parse("2026-09-01T09:00:00"));
    }

    private ManagementItemEvaluationScoreRow savedRow() {
        return new ManagementItemEvaluationScoreRow(5900301L, 1L, "B33-DRAFT-2026", "DRAFT", "2026", "EDUCATION", "LECTURE", "LECTURE_EVAL_SCORE", "KNUE-COL-EDU", new BigDecimal("33.75"), 1, "Y", "FR-020 저장", 4L, LocalDateTime.parse("2026-09-10T09:00:00"));
    }

    private SaveManagementItemEvaluationScoreRequest validRequest(Long ruleVersionId) {
        return new SaveManagementItemEvaluationScoreRequest(ruleVersionId, "EDUCATION", "LECTURE", "LECTURE_EVAL_SCORE", "KNUE-COL-EDU", new BigDecimal("33.75"), 1, "Y", "FR-020 저장");
    }

    private String validJson(String score, Long ruleVersionId) {
        return "{\"ruleVersionId\":" + ruleVersionId + ",\"achievementAreaCode\":\"EDUCATION\",\"achievementCategoryCode\":\"LECTURE\",\"managementItemCode\":\"LECTURE_EVAL_SCORE\",\"collegeCode\":\"KNUE-COL-EDU\",\"evaluationScore\":" + score + ",\"sortOrder\":1,\"activeYn\":\"Y\",\"changeReason\":\"FR-020 저장\"}";
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

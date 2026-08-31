package kr.ac.knue.commonfoundation.basic33;

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
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EvaluationAreaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationAreaApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationAreaService service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listEvaluationAreasReturnsDefaultTwentyAndFiltersForReq864() throws Exception {
        when(service.list(new EvaluationAreaSearchCriteria(0, 20, 10L, "Y", "교육")))
                .thenReturn(new EvaluationAreaSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/evaluation-areas")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-LIST")
                        .param("ruleVersionId", "10")
                        .param("activeYn", "Y")
                        .param("keyword", "교육"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationAreas[0].ruleVersionId").value(10))
                .andExpect(jsonPath("$.data.evaluationAreas[0].areaCode").value("EDUCATION"))
                .andExpect(jsonPath("$.data.evaluationAreas[0].areaName").value("교육"))
                .andExpect(jsonPath("$.data.evaluationAreas[0].periodApplyMethod").value("YEAR"))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B33-LIST"));
    }

    @Test
    void r01CannotListEvaluationAreasForReq864() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-areas")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void saveEvaluationAreaPersistsDraftAreaAndReturnsRequestIdForReq865Req866() throws Exception {
        when(service.save(any(SaveEvaluationAreaRequest.class), eq(1L), eq("REQ-B33-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/evaluation-areas/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"areaCode":"EDUCATION","areaName":"교육","sortOrder":1,"activeYn":"Y","periodApplyMethod":"YEAR","changeReason":"평가영역 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.areaCode").value("EDUCATION"))
                .andExpect(jsonPath("$.data.areaName").value("교육"))
                .andExpect(jsonPath("$.data.periodApplyMethod").value("YEAR"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B33-SAVE"));
        org.assertj.core.api.Assertions.assertThat("x-side-effects:data_change_histories,evaluation_areas")
                .contains("data_change_histories", "evaluation_areas");
    }

    @Test
    void saveEvaluationAreaRequiresRuleVersionIdForReq865() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-areas/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"areaCode":"EDUCATION","areaName":"교육","sortOrder":1,"activeYn":"Y","periodApplyMethod":"YEAR","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));
    }

    @Test
    void saveEvaluationAreaRejectsConfirmedRuleVersionForReq867() throws Exception {
        when(service.save(any(SaveEvaluationAreaRequest.class), eq(1L), eq("REQ-B33-CONFLICT")))
                .thenThrow(new ConflictException("확정 또는 폐기된 규정버전의 평가영역은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/evaluation-areas/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":11,"areaCode":"EDUCATION","areaName":"교육 변경","sortOrder":1,"activeYn":"Y","periodApplyMethod":"YEAR","changeReason":"확정 차단 검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void r01CannotSaveEvaluationAreaForReq867() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-areas/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"areaCode":"EDUCATION","areaName":"교육","sortOrder":1,"activeYn":"Y","periodApplyMethod":"YEAR","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionWithoutChangingEvaluationAreaForReq867() {
        EvaluationAreaMapper mapper = org.mockito.Mockito.mock(EvaluationAreaMapper.class);
        EvaluationAreaService evaluationAreaService = new EvaluationAreaService(mapper);
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluationAreaService.save(
                        new SaveEvaluationAreaRequest(11L, "EDUCATION", "교육 변경", 1, "Y", "YEAR", "확정 차단 검증"), 1L, "REQ-B33-CONFLICT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("규정버전");
        verify(mapper, never()).upsertEvaluationArea(any(), any());
    }

    @Test
    void serviceRecordsAreaChangeHistoryWithRequestIdForReq867() {
        EvaluationAreaMapper mapper = org.mockito.Mockito.mock(EvaluationAreaMapper.class);
        EvaluationAreaService evaluationAreaService = new EvaluationAreaService(mapper);
        EvaluationAreaRow before = new EvaluationAreaRow(100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", 1, "Y", "YEAR", "기존", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
        EvaluationAreaRow after = new EvaluationAreaRow(100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육성과", 1, "Y", "YEAR", "명칭 정비", 1L, LocalDateTime.parse("2026-08-31T09:05:00"));
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.findByKey(10L, "EDUCATION")).thenReturn(before, after);

        evaluationAreaService.save(new SaveEvaluationAreaRequest(10L, "education", "교육성과", 1, "Y", "YEAR", "명칭 정비"), 1L, "REQ-B33-AUDIT");

        verify(mapper).upsertEvaluationArea(new SaveEvaluationAreaRequest(10L, "EDUCATION", "교육성과", 1, "Y", "YEAR", "명칭 정비"), 1L);
        verify(mapper).insertChangeHistory("evaluation_areas", "10:EDUCATION", "UPDATE", "area_name", "교육", "교육성과", 1L, "명칭 정비", "REQ-B33-AUDIT");
        org.assertj.core.api.Assertions.assertThat("x-side-effects:data_change_histories,evaluation_areas")
                .contains("data_change_histories", "evaluation_areas");
    }

    private EvaluationAreaRow row() {
        return new EvaluationAreaRow(100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", 1, "Y", "YEAR", "평가영역 정비", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

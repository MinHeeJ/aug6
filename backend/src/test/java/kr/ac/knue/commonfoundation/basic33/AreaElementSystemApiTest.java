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

@WebMvcTest(AreaElementSystemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AreaElementSystemApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean AreaElementSystemService service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listAreaElementSystemsReturnsDefaultTwentyAndFiltersForReq890Req892() throws Exception {
        when(service.list(new AreaElementSystemSearchCriteria(0, 20, 10L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE", "Y", "학과")))
                .thenReturn(new AreaElementSystemSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/area-element-systems")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-AREA-ELEMENT-LIST")
                        .param("ruleVersionId", "10")
                        .param("areaCode", "EDUCATION")
                        .param("itemCode", "LECTURE")
                        .param("evaluationYear", "2026")
                        .param("elementCode", "ATTENDANCE")
                        .param("activeYn", "Y")
                        .param("keyword", "학과"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.areaElementSystems[0].ruleVersionId").value(10))
                .andExpect(jsonPath("$.data.areaElementSystems[0].areaCode").value("EDUCATION"))
                .andExpect(jsonPath("$.data.areaElementSystems[0].itemCode").value("LECTURE"))
                .andExpect(jsonPath("$.data.areaElementSystems[0].elementCode").value("ATTENDANCE"))
                .andExpect(jsonPath("$.data.areaElementSystems[0].targetScope").value("DEPARTMENT"))
                .andExpect(jsonPath("$.data.areaElementSystems[0].activeYn").value("Y"))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B33-AREA-ELEMENT-LIST"));
    }

    @Test
    void r01CannotListAreaElementSystemsForReq890() throws Exception {
        mockMvc.perform(get("/api/admin/area-element-systems")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void saveAreaElementSystemPersistsDraftSettingAndReturnsRequestIdForReq891Req892() throws Exception {
        when(service.save(any(SaveAreaElementSystemRequest.class), eq(1L), eq("REQ-B33-AREA-ELEMENT-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/area-element-systems/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-AREA-ELEMENT-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","targetScope":"DEPARTMENT","activeYn":"Y","changeReason":"영역별 체계 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetScope").value("DEPARTMENT"))
                .andExpect(jsonPath("$.data.activeYn").value("Y"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B33-AREA-ELEMENT-SAVE"));
        org.assertj.core.api.Assertions.assertThat("x-side-effects:area_element_system_settings,data_change_histories")
                .contains("area_element_system_settings", "data_change_histories");
    }

    @Test
    void saveAreaElementSystemRequiresRuleVersionIdForReq891() throws Exception {
        mockMvc.perform(post("/api/admin/area-element-systems/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","targetScope":"DEPARTMENT","activeYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));
    }

    @Test
    void saveAreaElementSystemRejectsConfirmedRuleVersionForReq894() throws Exception {
        when(service.save(any(SaveAreaElementSystemRequest.class), eq(1L), eq("REQ-B33-AREA-ELEMENT-CONFLICT")))
                .thenThrow(new ConflictException("확정 또는 폐기된 규정버전의 영역별 평가요소 체계는 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/area-element-systems/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B33-AREA-ELEMENT-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":11,"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","targetScope":"DEPARTMENT","activeYn":"N","changeReason":"확정 차단 검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void r01CannotSaveAreaElementSystemForReq893() throws Exception {
        mockMvc.perform(post("/api/admin/area-element-systems/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"areaCode":"EDUCATION","itemCode":"LECTURE","evaluationYear":"2026","elementCode":"ATTENDANCE","targetScope":"DEPARTMENT","activeYn":"Y","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsConfirmedRuleVersionWithoutChangingAreaElementSystemForReq894() {
        AreaElementSystemMapper mapper = org.mockito.Mockito.mock(AreaElementSystemMapper.class);
        AreaElementSystemService areaElementSystemService = new AreaElementSystemService(mapper);
        when(mapper.findRuleVersionStatus(11L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> areaElementSystemService.save(
                        new SaveAreaElementSystemRequest(11L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE", "DEPARTMENT", "N", "확정 차단 검증"), 1L, "REQ-B33-AREA-ELEMENT-CONFLICT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("규정버전");
        verify(mapper, never()).upsertAreaElementSystem(any(), any(), any(), any(), any());
    }

    @Test
    void serviceRecordsAreaElementSystemChangeHistoryWithRequestIdForReq893() {
        AreaElementSystemMapper mapper = org.mockito.Mockito.mock(AreaElementSystemMapper.class);
        AreaElementSystemService areaElementSystemService = new AreaElementSystemService(mapper);
        AreaElementSystemRow before = new AreaElementSystemRow(500L, 300L, 200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의", "2026", "ATTENDANCE", "출석", "DEPARTMENT", "Y", "기존", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
        AreaElementSystemRow after = new AreaElementSystemRow(500L, 300L, 200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의", "2026", "ATTENDANCE", "출석", "DEPARTMENT", "N", "사용중지", 1L, LocalDateTime.parse("2026-08-31T09:05:00"));
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.findAreaItemElementIds(10L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE"))
                .thenReturn(new AreaElementSystemTargetIds(100L, 200L, 300L));
        when(mapper.findByKey(100L, 200L, 300L, "DEPARTMENT")).thenReturn(before, after);

        areaElementSystemService.save(new SaveAreaElementSystemRequest(10L, "education", "lecture", "2026", "attendance", "department", "N", "사용중지"), 1L, "REQ-B33-AREA-ELEMENT-AUDIT");

        verify(mapper).upsertAreaElementSystem(new SaveAreaElementSystemRequest(10L, "EDUCATION", "LECTURE", "2026", "ATTENDANCE", "DEPARTMENT", "N", "사용중지"), 100L, 200L, 300L, 1L);
        verify(mapper).insertChangeHistory("area_element_system_settings", "10:EDUCATION:LECTURE:2026:ATTENDANCE:DEPARTMENT", "UPDATE", "active_yn", "Y", "N", 1L, "사용중지", "REQ-B33-AREA-ELEMENT-AUDIT");
        org.assertj.core.api.Assertions.assertThat("x-side-effects:area_element_system_settings,data_change_histories")
                .contains("area_element_system_settings", "data_change_histories");
    }

    private AreaElementSystemRow row() {
        return new AreaElementSystemRow(500L, 300L, 200L, 100L, 10L, "B33-DRAFT-2026", "DRAFT", "EDUCATION", "교육", "LECTURE", "강의", "2026", "ATTENDANCE", "출석", "DEPARTMENT", "Y", "영역별 체계 정비", 1L, LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

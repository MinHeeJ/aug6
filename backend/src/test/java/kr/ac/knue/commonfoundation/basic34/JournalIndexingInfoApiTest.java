package kr.ac.knue.commonfoundation.basic34;

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

@WebMvcTest(JournalIndexingInfoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class JournalIndexingInfoApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean JournalIndexingInfoService service;

    private final CurrentUser businessAdmin = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser systemAdmin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listJournalIndexingInfosReturnsPaginationAndFiltersForReq1025Req1026() throws Exception {
        when(service.list(new JournalIndexingInfoSearchCriteria(0, 20, 10L, "1225-6463", "한국교육학술지", "KCI", "KR", "Y", "교육")))
                .thenReturn(new JournalIndexingInfoSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/journal-indexing-infos")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-JOURNAL-LIST")
                        .param("ruleVersionId", "10")
                        .param("issn", "1225-6463")
                        .param("journalName", "한국교육학술지")
                        .param("indexingType", "KCI")
                        .param("publicationCountry", "KR")
                        .param("activeYn", "Y")
                        .param("keyword", "교육"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.journalIndexingInfos[0].journalIndexingInfoId").value(920))
                .andExpect(jsonPath("$.data.journalIndexingInfos[0].ruleVersionId").value(10))
                .andExpect(jsonPath("$.data.journalIndexingInfos[0].issn").value("1225-6463"))
                .andExpect(jsonPath("$.data.journalIndexingInfos[0].journalName").value("한국교육학술지"))
                .andExpect(jsonPath("$.data.journalIndexingInfos[0].indexingType").value("KCI"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B34-JOURNAL-LIST"));
    }

    @Test
    void r01CannotListJournalIndexingInfosForReq1025() throws Exception {
        mockMvc.perform(get("/api/admin/journal-indexing-infos")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any());
    }

    @Test
    void listJournalIndexingInfosRejectsInvalidPageSizeForReq1025() throws Exception {
        mockMvc.perform(get("/api/admin/journal-indexing-infos")
                        .requestAttr("currentUser", businessAdmin)
                        .cookie(sessionCookie())
                        .param("pageSize", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("pageSize"));
        verify(service, never()).list(any());
    }

    @Test
    void saveJournalIndexingInfoPersistsDraftInfoAndReturnsRequestIdForReq1026Req1027() throws Exception {
        when(service.save(any(SaveJournalIndexingInfoRequest.class), eq(1L), eq("REQ-B34-JOURNAL-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/journal-indexing-infos/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-JOURNAL-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"issn":"1225-6463","journalName":"한국교육학술지","indexingType":"KCI","publicationCountry":"KR","validStartDate":"2026-01-01","validEndDate":"2026-12-31","sourceName":"파일럿 시드","sourceUpdatedAt":"2026-08-31T09:00:00","activeYn":"Y","changeReason":"학술지 등재정보 정비"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.journalIndexingInfoId").value(920))
                .andExpect(jsonPath("$.data.issn").value("1225-6463"))
                .andExpect(jsonPath("$.data.journalName").value("한국교육학술지"))
                .andExpect(jsonPath("$.data.activeYn").value("Y"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B34-JOURNAL-SAVE"));
    }

    @Test
    void saveJournalIndexingInfoRequiresRuleVersionIdForReq1026() throws Exception {
        mockMvc.perform(post("/api/admin/journal-indexing-infos/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"issn":"1225-6463","journalName":"한국교육학술지","indexingType":"KCI","publicationCountry":"KR","validStartDate":"2026-01-01","validEndDate":"2026-12-31","sourceName":"파일럿 시드","sourceUpdatedAt":"2026-08-31T09:00:00","activeYn":"Y","changeReason":"검증"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("ruleVersionId"));
    }

    @Test
    void saveJournalIndexingInfoRejectsConfirmedRuleVersionForReq1028() throws Exception {
        when(service.save(any(SaveJournalIndexingInfoRequest.class), eq(1L), eq("REQ-B34-JOURNAL-CONFLICT")))
                .thenThrow(new ConflictException("확정 또는 폐기된 규정버전의 학술지 등재정보는 수정할 수 없습니다."));

        mockMvc.perform(post("/api/admin/journal-indexing-infos/save")
                        .requestAttr("currentUser", systemAdmin)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B34-JOURNAL-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":11,"issn":"1225-6463","journalName":"한국교육학술지","indexingType":"KCI","publicationCountry":"KR","validStartDate":"2026-01-01","validEndDate":"2026-12-31","sourceName":"파일럿 시드","sourceUpdatedAt":"2026-08-31T09:00:00","activeYn":"Y","changeReason":"확정 차단 검증"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void saveJournalIndexingInfoRejectsOverlappingIssnPeriodForReq1029Req1034() {
        JournalIndexingInfoMapper mapper = org.mockito.Mockito.mock(JournalIndexingInfoMapper.class);
        JournalIndexingInfoService journalService = new JournalIndexingInfoService(mapper);
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.countOverlappingIssnPeriods(any(SaveJournalIndexingInfoRequest.class))).thenReturn(1L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> journalService.save(
                        request(10L), 1L, "REQ-B34-JOURNAL-OVERLAP"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ISSN");
        verify(mapper, never()).upsertJournalIndexingInfo(any(), any());
    }

    @Test
    void r01CannotSaveJournalIndexingInfoForReq1032() throws Exception {
        mockMvc.perform(post("/api/admin/journal-indexing-infos/save")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruleVersionId":10,"issn":"1225-6463","journalName":"한국교육학술지","indexingType":"KCI","publicationCountry":"KR","validStartDate":"2026-01-01","validEndDate":"2026-12-31","sourceName":"파일럿 시드","sourceUpdatedAt":"2026-08-31T09:00:00","activeYn":"Y","changeReason":"권한 검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRecordsJournalIndexingInfoChangeHistoryWithRequestIdForReq1028() {
        JournalIndexingInfoMapper mapper = org.mockito.Mockito.mock(JournalIndexingInfoMapper.class);
        JournalIndexingInfoService journalService = new JournalIndexingInfoService(mapper);
        JournalIndexingInfoRow before = row("N");
        JournalIndexingInfoRow after = row("Y");
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.findByKey(any(SaveJournalIndexingInfoRequest.class))).thenReturn(before, after);

        journalService.save(request(10L), 1L, "REQ-B34-JOURNAL-AUDIT");

        verify(mapper).upsertJournalIndexingInfo(any(SaveJournalIndexingInfoRequest.class), eq(1L));
        verify(mapper).insertChangeHistory(eq("journal_indexing_infos"), eq("10:1225-6463:2026-01-01:2026-12-31"), eq("UPDATE"), eq("active_yn"), eq("N"), eq("Y"), eq(1L), eq("학술지 등재정보 정비"), eq("REQ-B34-JOURNAL-AUDIT"));
    }

    private SaveJournalIndexingInfoRequest request(Long ruleVersionId) {
        return new SaveJournalIndexingInfoRequest(ruleVersionId, "1225-6463", "한국교육학술지", "KCI", "KR",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "파일럿 시드",
                LocalDateTime.parse("2026-08-31T09:00:00"), "Y", "학술지 등재정보 정비");
    }

    private JournalIndexingInfoRow row() {
        return row("Y");
    }

    private JournalIndexingInfoRow row(String activeYn) {
        return new JournalIndexingInfoRow(920L, 10L, "B34-DRAFT-2026", "DRAFT", "1225-6463", "한국교육학술지",
                "KCI", "등재지", "KR", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "파일럿 시드",
                LocalDateTime.parse("2026-08-31T09:00:00"), activeYn, "학술지 등재정보 정비", 1L,
                LocalDateTime.parse("2026-08-31T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

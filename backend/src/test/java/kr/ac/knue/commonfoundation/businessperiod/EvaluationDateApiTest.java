package kr.ac.knue.commonfoundation.businessperiod;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasItem;
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

@WebMvcTest(EvaluationDateController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationDateApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationDateService service;

    private final CurrentUser r03 = new CurrentUser(3L, "dept-admin", "E0003", "학과관리자", List.of("R03"), List.of());
    private final CurrentUser r04 = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser r09 = new CurrentUser(9L, "admin", "E0009", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser r01 = new CurrentUser(1L, "teacher", "E0001", "교원", List.of("R01"), List.of());

    @Test
    void listEvaluationDatesReturnsPaginationFiltersAndRequestIdForR03R04R09() throws Exception {
        when(service.list(eq(r03), any(BusinessPeriodSearchCriteria.class))).thenReturn(response());

        mockMvc.perform(get("/api/admin/evaluation-dates")
                        .requestAttr("currentUser", r03)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B35-LIST")
                        .param("page", "0")
                        .param("size", "20")
                        .param("evaluationYear", "2026")
                        .param("areaCode", "EDUCATION")
                        .param("organizationCode", "KNUE-COL-EDU")
                        .param("userTypeCode", "FACULTY")
                        .param("activeYn", "Y")
                        .param("keyword", "EDU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evaluationDates[0].settingId").value(101))
                .andExpect(jsonPath("$.data.evaluationDates[0].evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.evaluationDates[0].organizationCode").value("KNUE-COL-EDU"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B35-LIST"));

        mockMvc.perform(get("/api/admin/evaluation-dates")
                        .requestAttr("currentUser", r04)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/evaluation-dates")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void r01CannotListEvaluationDates() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-dates")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any(), any());
    }

    @Test
    void listEvaluationDatesRejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/admin/evaluation-dates")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .param("size", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("size")));
        verify(service, never()).list(any(), any());
    }

    @Test
    void saveEvaluationDatePersistsAndReturnsRequestIdForR09() throws Exception {
        when(service.save(any(SaveEvaluationDateRequest.class), eq(r09), eq("REQ-B35-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/evaluation-dates/save")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B35-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evaluationYear":"2027","areaCode":"EDUCATION","organizationCode":"KNUE-COL-EDU","userTypeCode":"FACULTY","startAt":"2027-03-01T09:00:00","endAt":"2027-03-31T18:00:00","baseDate":"2027-03-31","activeYn":"Y","changeReason":"2027 평가일자 등록"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingId").value(101))
                .andExpect(jsonPath("$.data.baseDate").value("2026-03-31"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B35-SAVE"));
    }

    @Test
    void saveEvaluationDateReturnsFieldErrorsForRequiredValues() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-dates/save")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("activeYn")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("evaluationYear")))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("organizationCode")));
    }

    @Test
    void r01CannotSaveEvaluationDate() throws Exception {
        mockMvc.perform(post("/api/admin/evaluation-dates/save")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void serviceRejectsReversePeriodWithoutPersistence() {
        BusinessPeriodMapper mapper = org.mockito.Mockito.mock(BusinessPeriodMapper.class);
        EvaluationDateService evaluationDateService = new EvaluationDateService(mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluationDateService.save(new SaveEvaluationDateRequest(null,
                        "2027", "EDUCATION", "KNUE-COL-EDU", "FACULTY",
                        LocalDateTime.parse("2027-03-31T18:00:00"), LocalDateTime.parse("2027-03-01T09:00:00"),
                        LocalDate.parse("2027-03-31"), "Y", "역전 검증"), r09, "REQ-B35-REVERSE"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.BusinessValidationException.class)
                .hasMessageContaining("평가일자");
        verify(mapper, never()).insertEvaluationDate(any(), any());
        verify(mapper, never()).updateEvaluationDate(any(), any());
    }

    @Test
    void serviceRejectsActiveOverlapWithoutPersistence() {
        BusinessPeriodMapper mapper = org.mockito.Mockito.mock(BusinessPeriodMapper.class);
        EvaluationDateService evaluationDateService = new EvaluationDateService(mapper);
        when(mapper.countOverlappingEvaluationDates(null, "2027", "EDUCATION", "KNUE-COL-EDU", "FACULTY",
                LocalDateTime.parse("2027-03-01T09:00:00"), LocalDateTime.parse("2027-03-31T18:00:00"))).thenReturn(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluationDateService.save(validRequest(), r09, "REQ-B35-OVERLAP"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("중복");
        verify(mapper, never()).insertEvaluationDate(any(), any());
        verify(mapper, never()).updateEvaluationDate(any(), any());
    }

    @Test
    void serviceBlocksR03R04OutsideMappedOrganizationScope() {
        BusinessPeriodMapper mapper = org.mockito.Mockito.mock(BusinessPeriodMapper.class);
        EvaluationDateService evaluationDateService = new EvaluationDateService(mapper);
        when(mapper.existsAuthorizedEvaluationOrganization(4L, "KNUE-COL-SCI")).thenReturn(0);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluationDateService.save(new SaveEvaluationDateRequest(null,
                        "2027", "EDUCATION", "KNUE-COL-SCI", "FACULTY",
                        LocalDateTime.parse("2027-03-01T09:00:00"), LocalDateTime.parse("2027-03-31T18:00:00"),
                        LocalDate.parse("2027-03-31"), "Y", "범위 검증"), r04, "REQ-B35-SCOPE"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ForbiddenException.class);
        verify(mapper, never()).insertEvaluationDate(any(), any());
        verify(mapper, never()).updateEvaluationDate(any(), any());
    }

    @Test
    void serviceRecordsCreateSideEffectWithRequestId() {
        BusinessPeriodMapper mapper = org.mockito.Mockito.mock(BusinessPeriodMapper.class);
        EvaluationDateService evaluationDateService = new EvaluationDateService(mapper);
        when(mapper.countOverlappingEvaluationDates(any(), any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(mapper.insertEvaluationDate(any(), eq(9L))).thenReturn(row());

        evaluationDateService.save(validRequest(), r09, "REQ-B35-AUDIT");

        verify(mapper).insertChangeHistory(eq("evaluation_date_settings"), eq("101"), eq("CREATE"), eq("setting"), eq(null), eq("2026:EDUCATION:KNUE-COL-EDU:FACULTY"), eq(9L), eq("BASIC-35 평가일자 저장"), eq("REQ-B35-AUDIT"));
    }

    private EvaluationDateSearchResponse response() {
        return new EvaluationDateSearchResponse(List.of(row()), 0, 20, 1);
    }

    private BusinessPeriodSettingRow row() {
        return new BusinessPeriodSettingRow(101L, "2026", "EDUCATION", "KNUE-COL-EDU", "FACULTY",
                LocalDateTime.parse("2026-03-01T09:00:00"), LocalDateTime.parse("2026-03-31T18:00:00"),
                LocalDate.parse("2026-03-31"), "Y", 1L, 9L,
                LocalDateTime.parse("2026-08-31T09:00:00"), LocalDateTime.parse("2026-09-01T09:00:00"),
                "BASIC-35 평가일자 저장");
    }

    private SaveEvaluationDateRequest validRequest() {
        return new SaveEvaluationDateRequest(null, "2027", "EDUCATION", "KNUE-COL-EDU", "FACULTY",
                LocalDateTime.parse("2027-03-01T09:00:00"), LocalDateTime.parse("2027-03-31T18:00:00"),
                LocalDate.parse("2027-03-31"), "Y", "BASIC-35 평가일자 저장");
    }

    private String validJson() {
        return "{\"evaluationYear\":\"2027\",\"areaCode\":\"EDUCATION\",\"organizationCode\":\"KNUE-COL-EDU\",\"userTypeCode\":\"FACULTY\",\"startAt\":\"2027-03-01T09:00:00\",\"endAt\":\"2027-03-31T18:00:00\",\"baseDate\":\"2027-03-31\",\"activeYn\":\"Y\",\"changeReason\":\"2027 평가일자 등록\"}";
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

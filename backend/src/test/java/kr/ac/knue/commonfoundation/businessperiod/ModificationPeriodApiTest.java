package kr.ac.knue.commonfoundation.businessperiod;

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

@WebMvcTest(ModificationPeriodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ModificationPeriodApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ModificationPeriodService service;

    private final CurrentUser r03 = new CurrentUser(3L, "dept-admin", "E0003", "학과관리자", List.of("R03"), List.of());
    private final CurrentUser r04 = new CurrentUser(4L, "business-admin", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser r09 = new CurrentUser(9L, "admin", "E0009", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser r01 = new CurrentUser(1L, "teacher", "E0001", "교원", List.of("R01"), List.of());

    @Test
    void listModificationPeriodsReturnsPaginationFiltersAndRequestIdForR03R04R09() throws Exception {
        when(service.list(eq(r03), any(BusinessPeriodSearchCriteria.class))).thenReturn(response());
        when(service.list(eq(r04), any(BusinessPeriodSearchCriteria.class))).thenReturn(response());
        when(service.list(eq(r09), any(BusinessPeriodSearchCriteria.class))).thenReturn(response());

        mockMvc.perform(get("/api/admin/modification-periods")
                        .requestAttr("currentUser", r03)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B35-MODIFICATION-LIST")
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
                .andExpect(jsonPath("$.data.modificationPeriods[0].settingId").value(201))
                .andExpect(jsonPath("$.data.modificationPeriods[0].evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.modificationPeriods[0].organizationCode").value("KNUE-COL-EDU"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B35-MODIFICATION-LIST"));

        mockMvc.perform(get("/api/admin/modification-periods").requestAttr("currentUser", r04).cookie(sessionCookie()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/modification-periods").requestAttr("currentUser", r09).cookie(sessionCookie()))
                .andExpect(status().isOk());
    }

    @Test
    void r01CannotListModificationPeriods() throws Exception {
        mockMvc.perform(get("/api/admin/modification-periods").requestAttr("currentUser", r01).cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any(), any());
    }

    @Test
    void listModificationPeriodsRejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/admin/modification-periods")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .param("size", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[*].field").value(hasItem("size")));
        verify(service, never()).list(any(), any());
    }

    @Test
    void saveModificationPeriodPersistsAndReturnsRequestIdForR09() throws Exception {
        when(service.save(any(SaveModificationPeriodRequest.class), eq(r09), eq("REQ-B35-MODIFICATION-SAVE"))).thenReturn(row());

        mockMvc.perform(post("/api/admin/modification-periods/save")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B35-MODIFICATION-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.settingId").value(201))
                .andExpect(jsonPath("$.data.startAt").value("2026-04-01T09:00:00"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B35-MODIFICATION-SAVE"));
    }

    @Test
    void saveModificationPeriodReturnsFieldErrorsForRequiredValues() throws Exception {
        mockMvc.perform(post("/api/admin/modification-periods/save")
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
    void r01CannotSaveModificationPeriod() throws Exception {
        mockMvc.perform(post("/api/admin/modification-periods/save")
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
        ModificationPeriodService modificationPeriodService = new ModificationPeriodService(mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> modificationPeriodService.save(new SaveModificationPeriodRequest(null,
                        "2027", "EDUCATION", "KNUE-COL-EDU", "FACULTY",
                        LocalDateTime.parse("2027-04-30T18:00:00"), LocalDateTime.parse("2027-04-01T09:00:00"),
                        LocalDate.parse("2027-04-01"), "Y", "역전 검증"), r09, "REQ-B35-MODIFICATION-REVERSE"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.BusinessValidationException.class)
                .hasMessageContaining("수정기간");
        verify(mapper, never()).insertModificationPeriod(any(), any());
        verify(mapper, never()).updateModificationPeriod(any(), any());
    }

    @Test
    void serviceRejectsActiveOverlapWithoutPersistence() {
        BusinessPeriodMapper mapper = org.mockito.Mockito.mock(BusinessPeriodMapper.class);
        ModificationPeriodService modificationPeriodService = new ModificationPeriodService(mapper);
        when(mapper.countOverlappingModificationPeriods(null, "2027", "EDUCATION", "KNUE-COL-EDU", "FACULTY",
                LocalDateTime.parse("2027-04-01T09:00:00"), LocalDateTime.parse("2027-04-30T18:00:00"))).thenReturn(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> modificationPeriodService.save(validRequest(), r09, "REQ-B35-MODIFICATION-OVERLAP"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("중복");
        verify(mapper, never()).insertModificationPeriod(any(), any());
        verify(mapper, never()).updateModificationPeriod(any(), any());
    }

    @Test
    void serviceBlocksR03R04OutsideMappedOrganizationScope() {
        BusinessPeriodMapper mapper = org.mockito.Mockito.mock(BusinessPeriodMapper.class);
        ModificationPeriodService modificationPeriodService = new ModificationPeriodService(mapper);
        when(mapper.existsAuthorizedEvaluationOrganization(4L, "KNUE-COL-SCI")).thenReturn(0);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> modificationPeriodService.save(new SaveModificationPeriodRequest(null,
                        "2027", "EDUCATION", "KNUE-COL-SCI", "FACULTY",
                        LocalDateTime.parse("2027-04-01T09:00:00"), LocalDateTime.parse("2027-04-30T18:00:00"),
                        LocalDate.parse("2027-04-01"), "Y", "범위 검증"), r04, "REQ-B35-MODIFICATION-SCOPE"))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ForbiddenException.class);
        verify(mapper, never()).insertModificationPeriod(any(), any());
        verify(mapper, never()).updateModificationPeriod(any(), any());
    }

    @Test
    void serviceRecordsCreateSideEffectWithRequestId() {
        BusinessPeriodMapper mapper = org.mockito.Mockito.mock(BusinessPeriodMapper.class);
        ModificationPeriodService modificationPeriodService = new ModificationPeriodService(mapper);
        when(mapper.countOverlappingModificationPeriods(any(), any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(mapper.insertModificationPeriod(any(), eq(9L))).thenReturn(row());

        modificationPeriodService.save(validRequest(), r09, "REQ-B35-MODIFICATION-AUDIT");

        verify(mapper).insertChangeHistory(eq("modification_period_settings"), eq("201"), eq("CREATE"), eq("setting"), eq(null), eq("2026:EDUCATION:KNUE-COL-EDU:FACULTY"), eq(9L), eq("BASIC-35 수정기간 저장"), eq("REQ-B35-MODIFICATION-AUDIT"));
    }

    private ModificationPeriodSearchResponse response() {
        return new ModificationPeriodSearchResponse(List.of(row()), 0, 20, 1);
    }

    private BusinessPeriodSettingRow row() {
        return new BusinessPeriodSettingRow(201L, "2026", "EDUCATION", "KNUE-COL-EDU", "FACULTY",
                LocalDateTime.parse("2026-04-01T09:00:00"), LocalDateTime.parse("2026-04-30T18:00:00"),
                LocalDate.parse("2026-04-01"), "Y", 1L, 9L,
                LocalDateTime.parse("2026-08-31T09:00:00"), LocalDateTime.parse("2026-09-01T09:00:00"),
                "BASIC-35 수정기간 저장");
    }

    private SaveModificationPeriodRequest validRequest() {
        return new SaveModificationPeriodRequest(null, "2027", "EDUCATION", "KNUE-COL-EDU", "FACULTY",
                LocalDateTime.parse("2027-04-01T09:00:00"), LocalDateTime.parse("2027-04-30T18:00:00"),
                LocalDate.parse("2027-04-01"), "Y", "BASIC-35 수정기간 저장");
    }

    private String validJson() {
        return "{\"evaluationYear\":\"2027\",\"areaCode\":\"EDUCATION\",\"organizationCode\":\"KNUE-COL-EDU\",\"userTypeCode\":\"FACULTY\",\"startAt\":\"2027-04-01T09:00:00\",\"endAt\":\"2027-04-30T18:00:00\",\"baseDate\":\"2027-04-01\",\"activeYn\":\"Y\",\"changeReason\":\"2027 수정기간 등록\"}";
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

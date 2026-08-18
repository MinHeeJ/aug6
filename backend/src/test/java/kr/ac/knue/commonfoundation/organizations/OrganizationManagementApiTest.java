package kr.ac.knue.commonfoundation.organizations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
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

@WebMvcTest(OrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrganizationManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean OrganizationService organizationService;

    @Test
    void searchOrganizationsReturnsOrganizationFieldsFromContract() throws Exception {
        when(organizationService.searchOrganizations("KNUE", 0, 10)).thenReturn(List.of(
                new OrganizationRow("KNUE-COL-EDU", "교육과학대학", "COLLEGE", "Y", "ACTIVE", "KNUE", LocalDate.parse("2026-01-01"), null, LocalDateTime.parse("2026-01-02T03:04:05"))));

        mockMvc.perform(get("/api/admin/organizations").param("organizationCodeFilter", "KNUE").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].organizationCode").value("KNUE-COL-EDU"))
                .andExpect(jsonPath("$.data[0].organizationType").value("COLLEGE"))
                .andExpect(jsonPath("$.data[0].parentOrganizationCode").value("KNUE"));
    }

    @Test
    void getOrganizationTreeReturnsParentChildHierarchy() throws Exception {
        when(organizationService.getOrganizationTree()).thenReturn(List.of(
                new OrganizationTreeNode("KNUE", "한국교원대학교", "UNIVERSITY", "Y", "ACTIVE", null, List.of(
                        new OrganizationTreeNode("KNUE-COL-EDU", "교육과학대학", "COLLEGE", "Y", "ACTIVE", "KNUE", List.of())))));

        mockMvc.perform(get("/api/admin/organizations/tree").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].children[0].organizationCode").value("KNUE-COL-EDU"));
    }

    @Test
    void saveOrganizationParentRelationPersistsAndReturnsUpdatedRelation() throws Exception {
        when(organizationService.saveParentRelation(eq("KNUE-DEPT-COMP"), any(OrganizationParentRelationRequest.class), eq(1L)))
                .thenReturn(new OrganizationRow("KNUE-DEPT-COMP", "컴퓨터교육과", "DEPARTMENT", "Y", "ACTIVE", "KNUE-COL-EDU", LocalDate.parse("2026-03-01"), null, LocalDateTime.parse("2026-03-01T09:00:00")));

        mockMvc.perform(put("/api/admin/organizations/KNUE-DEPT-COMP/parent-relations")
                        .requestAttr("currentUser", new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of()))
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentOrganizationCode\":\"KNUE-COL-EDU\",\"effectiveStartDate\":\"2026-03-01\",\"changeReason\":\"조직 개편 반영\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentOrganizationCode").value("KNUE-COL-EDU"))
                .andExpect(jsonPath("$.data.effectiveStartDate").value("2026-03-01"));
    }

    @Test
    void saveOrganizationParentRelationReturnsFieldErrorWhenDateOrderIsInvalid() throws Exception {
        when(organizationService.saveParentRelation(eq("KNUE-DEPT-COMP"), any(OrganizationParentRelationRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("적용 종료일은 시작일보다 빠를 수 없습니다.", List.of(new ValidationError("effectiveEndDate", "적용 종료일은 시작일 이후여야 합니다."))));

        mockMvc.perform(put("/api/admin/organizations/KNUE-DEPT-COMP/parent-relations")
                        .requestAttr("currentUser", new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of()))
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentOrganizationCode\":\"KNUE\",\"effectiveStartDate\":\"2026-03-10\",\"effectiveEndDate\":\"2026-03-01\",\"changeReason\":\"날짜 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields[0].field").value("effectiveEndDate"));
    }

    @Test
    void saveOrganizationParentRelationRequiresMandatoryFields() throws Exception {
        mockMvc.perform(put("/api/admin/organizations/KNUE-DEPT-COMP/parent-relations")
                        .requestAttr("currentUser", new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of()))
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listOrganizationParentRelationHistoryReturnsContractRowsForLiteralOperation() throws Exception {
        when(organizationService.listHistory("KNUE-DEPT-COMP", 0, 10)).thenReturn(List.of(
                new OrganizationRelationHistoryRow(77L, 10L, "KNUE-DEPT-COMP", "KNUE-COL-EDU", "KNUE",
                        LocalDate.parse("2026-01-01"), null, LocalDate.parse("2026-03-01"), null,
                        LocalDateTime.parse("2026-03-01T09:00:00"), 1L, "조직 개편")));

        mockMvc.perform(get("/api/admin/organizations/KNUE-DEPT-COMP/parent-relations/history")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].organizationCode").value("KNUE-DEPT-COMP"))
                .andExpect(jsonPath("$.data[0].beforeParentOrganizationCode").value("KNUE-COL-EDU"))
                .andExpect(jsonPath("$.data[0].afterParentOrganizationCode").value("KNUE"));
    }

    @Test
    void saveOrganizationParentRelationRequiresAuthBeforeTableSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/organizations/KNUE-DEPT-COMP/parent-relations")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentOrganizationCode\":\"KNUE\",\"effectiveStartDate\":\"2026-03-01\",\"changeReason\":\"권한 검증\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(organizationService, never()).saveParentRelation(any(), any(), any());
    }

    @Test
    void serviceEndsPreviousRelationInsertsNewRelationAndStoresHistorySideEffect() {
        OrganizationMapper mapper = mock(OrganizationMapper.class);
        OrganizationService service = new OrganizationService(mapper);
        OrganizationRow child = new OrganizationRow("KNUE-DEPT-COMP", "컴퓨터교육과", "DEPARTMENT", "Y", "ACTIVE", "KNUE-COL-EDU", LocalDate.parse("2026-01-01"), null, LocalDateTime.parse("2026-01-01T00:00:00"));
        OrganizationRow parent = new OrganizationRow("KNUE", "한국교원대학교", "UNIVERSITY", "Y", "ACTIVE", null, null, null, LocalDateTime.parse("2026-01-01T00:00:00"));
        OrganizationRelationRow current = new OrganizationRelationRow(10L, "KNUE-DEPT-COMP", "KNUE-COL-EDU", LocalDate.parse("2026-01-01"), null, "ACTIVE", "초기 관계");
        OrganizationRelationRow latest = new OrganizationRelationRow(11L, "KNUE-DEPT-COMP", "KNUE", LocalDate.parse("2026-03-01"), null, "ACTIVE", "조직 개편");
        when(mapper.findOrganization("KNUE-DEPT-COMP")).thenReturn(child);
        when(mapper.findOrganization("KNUE")).thenReturn(parent);
        when(mapper.findCurrentRelation("KNUE-DEPT-COMP")).thenReturn(current);
        when(mapper.findLatestRelation("KNUE-DEPT-COMP")).thenReturn(latest);

        service.saveParentRelation("KNUE-DEPT-COMP", new OrganizationParentRelationRequest("KNUE", LocalDate.parse("2026-03-01"), null, "조직 개편"), 1L);

        verify(mapper).endRelation(10L, LocalDate.parse("2026-02-28"), 1L, "조직 개편");
        verify(mapper).insertRelation("KNUE-DEPT-COMP", "KNUE", LocalDate.parse("2026-03-01"), null, 1L, "조직 개편");
        verify(mapper).insertHistory(eq(current), eq(latest), eq("KNUE-DEPT-COMP"), eq(1L), eq("조직 개편"));
    }

    @Test
    void serviceRejectsOverlappingOrganizationRelationWithoutSideEffects() {
        OrganizationMapper mapper = mock(OrganizationMapper.class);
        OrganizationService service = new OrganizationService(mapper);
        OrganizationRow child = new OrganizationRow("KNUE-DEPT-COMP", "컴퓨터교육과", "DEPARTMENT", "Y", "ACTIVE", "KNUE-COL-EDU", LocalDate.parse("2026-01-01"), null, LocalDateTime.parse("2026-01-01T00:00:00"));
        OrganizationRow parent = new OrganizationRow("KNUE", "한국교원대학교", "UNIVERSITY", "Y", "ACTIVE", null, null, null, LocalDateTime.parse("2026-01-01T00:00:00"));
        OrganizationRelationRow overlap = new OrganizationRelationRow(99L, "KNUE-DEPT-COMP", "KNUE-COL-OLD", LocalDate.parse("2026-02-01"), LocalDate.parse("2026-12-31"), "ACTIVE", "기존 관계");
        when(mapper.findOrganization("KNUE-DEPT-COMP")).thenReturn(child);
        when(mapper.findOrganization("KNUE")).thenReturn(parent);
        when(mapper.findOverlappingRelation(eq("KNUE-DEPT-COMP"), any(LocalDate.class), any(), any())).thenReturn(overlap);

        assertThatThrownBy(() -> service.saveParentRelation("KNUE-DEPT-COMP", new OrganizationParentRelationRequest("KNUE", LocalDate.parse("2026-03-01"), null, "조직 개편"), 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("기간");
        verify(mapper, never()).insertRelation(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).insertHistory(any(), any(), any(), any(), any());
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

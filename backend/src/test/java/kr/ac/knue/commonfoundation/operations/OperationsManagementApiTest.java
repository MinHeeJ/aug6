package kr.ac.knue.commonfoundation.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OperationsManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OperationsManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean OperationsManagementService service;

    @Test
    void searchPositionAssignmentsUsesDefaultTwentyAndReturnsEffectiveRows() throws Exception {
        when(service.searchPositionAssignments(new AssignmentSearchCriteria(0, 20, LocalDate.parse("2026-03-01"), null)))
                .thenReturn(new PositionAssignmentSearchResponse(List.of(positionRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/position-assignments").param("referenceDate", "2026-03-01").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.assignments[0].positionCode").value("DEPT_HEAD"))
                .andExpect(jsonPath("$.data.assignments[0].status").value("ACTIVE"));
    }

    @Test
    void savePositionAssignmentPersistsRequiredFieldsForR09() throws Exception {
        when(service.savePositionAssignment(any(PositionAssignmentRequest.class), eq(1L))).thenReturn(positionRow());

        mockMvc.perform(post("/api/admin/position-assignments")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"2\",\"organizationCode\":\"KNUE-DEPT-COMP\",\"effectiveStartDate\":\"2026-03-01\",\"changeReason\":\"보직 지정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positionCode").value("DEPT_HEAD"))
                .andExpect(jsonPath("$.data.organizationCode").value("KNUE-DEPT-COMP"));
    }

    @Test
    void updatePositionAssignmentRequiresEffectiveStartDateFieldError() throws Exception {
        mockMvc.perform(put("/api/admin/position-assignments/10")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"2\",\"organizationCode\":\"KNUE-DEPT-COMP\",\"changeReason\":\"보직 지정\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field=='effectiveStartDate')]").exists());
    }

    @Test
    void savePositionAssignmentRejectsNonR09WithoutSideEffect() throws Exception {
        mockMvc.perform(post("/api/admin/position-assignments")
                        .requestAttr("currentUser", teacherUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"2\",\"organizationCode\":\"KNUE-DEPT-COMP\",\"effectiveStartDate\":\"2026-03-01\",\"changeReason\":\"보직 지정\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).savePositionAssignment(any(), any());
    }

    @Test
    void searchDutyAssignmentsReturnsLatestConfirmedEffectiveAssignment() throws Exception {
        when(service.searchDutyAssignments(new AssignmentSearchCriteria(0, 50, LocalDate.parse("2026-04-01"), "교수업적")))
                .thenReturn(new DutyAssignmentSearchResponse(List.of(dutyRow()), 0, 50, 1));

        mockMvc.perform(get("/api/admin/duty-assignments")
                        .param("size", "50")
                        .param("referenceDate", "2026-04-01")
                        .param("filter", "교수업적")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignments[0].dutyArea").value("교수업적평가"))
                .andExpect(jsonPath("$.data.assignments[0].confirmedAt").value("2026-03-02T09:00:00"));
    }

    @Test
    void saveDutyAssignmentRequiresValidStartDateFieldError() throws Exception {
        mockMvc.perform(post("/api/admin/duty-assignments")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dutyOrganization\":\"단과대학\",\"userId\":\"2\",\"dutyArea\":\"교수업적평가\",\"dataScopeType\":\"COLLEGE\",\"processingPermission\":\"READ_WRITE\",\"changeReason\":\"담당 지정\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field=='validStartDate')]").exists());
    }

    @Test
    void updateDutyAssignmentPersistsConfirmedAtSideEffect() throws Exception {
        when(service.updateDutyAssignment(eq(30L), any(DutyAssignmentRequest.class), eq(1L))).thenReturn(dutyRow());

        mockMvc.perform(put("/api/admin/duty-assignments/30")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dutyOrganization\":\"단과대학\",\"userId\":\"2\",\"dutyArea\":\"교수업적평가\",\"validStartDate\":\"2026-03-01\",\"dataScopeType\":\"COLLEGE\",\"processingPermission\":\"READ_WRITE\",\"changeReason\":\"담당 변경\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmedAt").value("2026-03-02T09:00:00"));
    }

    @Test
    void searchDataScopeRulesReturnsRoleScopeRules() throws Exception {
        when(service.searchDataScopeRules(new AssignmentSearchCriteria(0, 100, null, "R09")))
                .thenReturn(new DataScopeRulesSearchResponse(List.of(scopeRow()), 0, 100, 1));

        mockMvc.perform(get("/api/admin/data-scope-rules").param("size", "100").param("filter", "R09").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rules[0].roleCode").value("R09"))
                .andExpect(jsonPath("$.data.rules[0].dataScopeType").value("ALL"));
    }

    @Test
    void saveDataScopeRulesRequiresRoleCodeFieldError() throws Exception {
        mockMvc.perform(put("/api/admin/data-scope-rules")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataScopeType\":\"ALL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field=='roleCode')]").exists());
    }

    @Test
    void saveDataScopeRulesPersistsUnionScopeRuleForR09() throws Exception {
        when(service.saveDataScopeRules(any(DataScopeRulesSaveRequest.class), eq(1L))).thenReturn(scopeRow());

        mockMvc.perform(put("/api/admin/data-scope-rules")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCode\":\"R09\",\"dataScopeType\":\"ALL\",\"changeReason\":\"전체 범위 적용\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("R09"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-03-02T09:00:00"));
    }

    @Test
    void postDutyAssignmentsRequiresR09AuthenticationBeforeTableSideEffect() throws Exception {
        mockMvc.perform(post("/api/admin/duty-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dutyOrganization\":\"단과대학\",\"userId\":\"2\",\"dutyArea\":\"교수업적평가\",\"validStartDate\":\"2026-03-01\",\"dataScopeType\":\"COLLEGE\",\"processingPermission\":\"READ_WRITE\",\"changeReason\":\"권한 검증\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveDutyAssignment(any(), any());
    }

    @Test
    void postDutyAssignmentsPersistsConfirmedTableSideEffectAndReturnsHappyBody() throws Exception {
        when(service.saveDutyAssignment(any(DutyAssignmentRequest.class), eq(1L))).thenReturn(dutyRow());

        mockMvc.perform(post("/api/admin/duty-assignments")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dutyOrganization\":\"단과대학\",\"userId\":\"2\",\"dutyArea\":\"교수업적평가\",\"validStartDate\":\"2026-03-01\",\"dataScopeType\":\"COLLEGE\",\"processingPermission\":\"READ_WRITE\",\"changeReason\":\"담당 지정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentId").value(30))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.confirmedAt").value("2026-03-02T09:00:00"));
        verify(service).saveDutyAssignment(any(DutyAssignmentRequest.class), eq(1L));
    }

    @Test
    void postDutyAssignmentsBusinessRulePreventsTableSideEffect() throws Exception {
        when(service.saveDutyAssignment(any(DutyAssignmentRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("담당 업무 규칙 위반", List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("changeReason", "담당 업무 규칙 위반"))));

        mockMvc.perform(post("/api/admin/duty-assignments")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dutyOrganization\":\"단과대학\",\"userId\":\"2\",\"dutyArea\":\"교수업적평가\",\"validStartDate\":\"2026-03-01\",\"dataScopeType\":\"COLLEGE\",\"processingPermission\":\"READ_WRITE\",\"changeReason\":\"업무 규칙 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void putDutyAssignmentsRequiresR09AuthenticationBeforeTableSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/duty-assignments/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dutyOrganization\":\"단과대학\",\"userId\":\"2\",\"dutyArea\":\"교수업적평가\",\"validStartDate\":\"2026-03-01\",\"dataScopeType\":\"COLLEGE\",\"processingPermission\":\"READ_WRITE\",\"changeReason\":\"권한 검증\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).updateDutyAssignment(any(), any(), any());
    }

    @Test
    void putDutyAssignmentsBusinessRulePreventsTableSideEffect() throws Exception {
        when(service.updateDutyAssignment(eq(30L), any(DutyAssignmentRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("담당 업무 규칙 위반", List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("changeReason", "담당 업무 규칙 위반"))));

        mockMvc.perform(put("/api/admin/duty-assignments/30")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dutyOrganization\":\"단과대학\",\"userId\":\"2\",\"dutyArea\":\"교수업적평가\",\"validStartDate\":\"2026-03-01\",\"dataScopeType\":\"COLLEGE\",\"processingPermission\":\"READ_WRITE\",\"changeReason\":\"업무 규칙 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void putDataScopeRulesRequiresR09AuthenticationBeforeTableSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/data-scope-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCode\":\"R09\",\"dataScopeType\":\"ALL\",\"changeReason\":\"권한 검증\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).saveDataScopeRules(any(), any());
    }

    @Test
    void putDataScopeRulesBusinessRulePreventsTableSideEffect() throws Exception {
        when(service.saveDataScopeRules(any(DataScopeRulesSaveRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("자료 범위 업무 규칙 위반", List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("roleCode", "자료 범위 업무 규칙 위반"))));

        mockMvc.perform(put("/api/admin/data-scope-rules")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCode\":\"R09\",\"dataScopeType\":\"ALL\",\"changeReason\":\"업무 규칙 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void putPositionAssignmentsRequiresAllVendorObligationsInOneLiteralOperation() throws Exception {
        when(service.updatePositionAssignment(eq(10L), any(PositionAssignmentRequest.class), eq(1L))).thenReturn(positionRow());

        mockMvc.perform(put("/api/admin/position-assignments/10")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"2\",\"organizationCode\":\"KNUE-DEPT-COMP\",\"effectiveStartDate\":\"2026-03-01\",\"changeReason\":\"보직 변경\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentId").value(10))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-03-02T09:00:00"));
        verify(service).updatePositionAssignment(eq(10L), any(PositionAssignmentRequest.class), eq(1L));
    }

    @Test
    void postPositionAssignmentsBusinessRulePreventsTableSideEffect() throws Exception {
        when(service.savePositionAssignment(any(PositionAssignmentRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("보직 업무 규칙 위반", List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("positionCode", "보직 업무 규칙 위반"))));

        mockMvc.perform(post("/api/admin/position-assignments")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positionCode\":\"DEPT_HEAD\",\"userId\":\"2\",\"organizationCode\":\"KNUE-DEPT-COMP\",\"effectiveStartDate\":\"2026-03-01\",\"changeReason\":\"업무 규칙 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void serviceRejectsExpiredDateRangeWithoutPersistingPositionAssignment() {
        OperationsManagementMapper mapper = mock(OperationsManagementMapper.class);
        OperationsManagementService realService = new OperationsManagementService(mapper);
        PositionAssignmentRequest request = new PositionAssignmentRequest();
        request.setPositionCode("DEPT_HEAD");
        request.setUserId("2");
        request.setOrganizationCode("KNUE-DEPT-COMP");
        request.setEffectiveStartDate(LocalDate.parse("2026-03-10"));
        request.setEffectiveEndDate(LocalDate.parse("2026-03-01"));
        request.setChangeReason("기간 검증");

        assertThatThrownBy(() -> realService.savePositionAssignment(request, 1L))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insertPositionAssignment(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void serviceResolvesMultipleRoleDataScopesAsUnion() {
        OperationsManagementMapper mapper = mock(OperationsManagementMapper.class);
        OperationsManagementService realService = new OperationsManagementService(mapper);
        when(mapper.findRulesByRoles(List.of("R01", "R02", "R09"))).thenReturn(List.of(
                new DataScopeRuleRow(1L, "R01", "교원", "SELF", null, null, null, null, null),
                new DataScopeRuleRow(2L, "R02", "학과장", "DEPARTMENT", null, null, null, null, null),
                new DataScopeRuleRow(3L, "R09", "시스템관리자", "ALL", null, null, null, null, null)));

        Set<String> scopes = realService.resolveUnionDataScopes(List.of("R01", "R02", "R09"));

        assertThat(scopes).containsExactly("SELF", "DEPARTMENT", "ALL");
    }

    private PositionAssignmentRow positionRow() {
        return new PositionAssignmentRow(10L, "DEPT_HEAD", 2L, "홍길동", "KNUE-DEPT-COMP", "컴퓨터교육과", LocalDate.parse("2026-03-01"), null, "ACTIVE", LocalDateTime.parse("2026-03-02T09:00:00"), "보직 지정", LocalDateTime.parse("2026-03-02T09:00:00"));
    }

    private DutyAssignmentRow dutyRow() {
        return new DutyAssignmentRow(30L, "단과대학", 2L, "홍길동", "교수업적평가", LocalDate.parse("2026-03-01"), null, "COLLEGE", "READ_WRITE", "ACTIVE", LocalDateTime.parse("2026-03-02T09:00:00"), "담당 지정", LocalDateTime.parse("2026-03-02T09:00:00"));
    }

    private DataScopeRuleRow scopeRow() {
        return new DataScopeRuleRow(40L, "R09", "시스템관리자", "ALL", null, null, null, "전체 범위 적용", LocalDateTime.parse("2026-03-02T09:00:00"));
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }

    private CurrentUser teacherUser() {
        return new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

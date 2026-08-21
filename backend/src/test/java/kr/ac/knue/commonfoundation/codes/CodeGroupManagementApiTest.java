package kr.ac.knue.commonfoundation.codes;

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
import java.time.LocalDateTime;
import java.util.List;
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

@WebMvcTest(CodeGroupManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CodeGroupManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean CodeGroupManagementService codeGroupManagementService;

    @Test
    void listCodeGroupsReturnsFilteredGroupsWithDetailCodeConnectionCount() throws Exception {
        when(codeGroupManagementService.listCodeGroups(0, 20, "EVAL", null)).thenReturn(List.of(
                new CodeGroupRow("EVAL_AREA", "평가영역", "평가영역 코드 묶음", "교수지원과", "Y", "ACTIVE", 3, LocalDateTime.parse("2026-01-02T03:04:05"), LocalDateTime.parse("2026-01-03T03:04:05"))));

        mockMvc.perform(get("/api/admin/code-groups").param("groupIdFilter", "EVAL").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].groupId").value("EVAL_AREA"))
                .andExpect(jsonPath("$.data[0].groupName").value("평가영역"))
                .andExpect(jsonPath("$.data[0].detailCodeCount").value(3));
    }

    @Test
    void createCodeGroupPersistsEditableMetadataAndReturnsCreatedGroup() throws Exception {
        when(codeGroupManagementService.createCodeGroup(any(CodeGroupRequest.class), eq(1L)))
                .thenReturn(new CodeGroupRow("PROC_STATUS", "처리상태", "처리상태 코드 묶음", "교수지원과", "Y", "ACTIVE", 0, LocalDateTime.parse("2026-03-01T09:00:00"), LocalDateTime.parse("2026-03-01T09:00:00")));

        mockMvc.perform(post("/api/admin/code-groups")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId":"PROC_STATUS","groupName":"처리상태","description":"처리상태 코드 묶음","managingDepartment":"교수지원과","systemUseYn":"Y","changeReason":"신규 등록"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value("PROC_STATUS"))
                .andExpect(jsonPath("$.data.groupName").value("처리상태"));
    }

    @Test
    void updateCodeGroupKeepsPathGroupIdAsIdentityAndReturnsUpdatedMetadata() throws Exception {
        when(codeGroupManagementService.updateCodeGroup(eq("PROC_STATUS"), any(CodeGroupRequest.class), eq(1L)))
                .thenReturn(new CodeGroupRow("PROC_STATUS", "처리 상태", "업무 처리 상태 코드", "공통기능팀", "N", "INACTIVE", 2, LocalDateTime.parse("2026-03-01T09:00:00"), LocalDateTime.parse("2026-03-02T10:00:00")));

        mockMvc.perform(put("/api/admin/code-groups/PROC_STATUS")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId":"PROC_STATUS","groupName":"처리 상태","description":"업무 처리 상태 코드","managingDepartment":"공통기능팀","systemUseYn":"N","changeReason":"관리부서 변경"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value("PROC_STATUS"))
                .andExpect(jsonPath("$.data.managingDepartment").value("공통기능팀"))
                .andExpect(jsonPath("$.data.systemUseYn").value("N"));
    }

    @Test
    void mutatingCodeGroupsRequiresAuthenticatedAdminSession() throws Exception {
        mockMvc.perform(post("/api/admin/code-groups")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId":"AUTH_TYPE","groupName":"인증구분","managingDepartment":"교수지원과","changeReason":"신규 등록"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void createCodeGroupReturnsFieldErrorsForMissingRequiredMetadata() throws Exception {
        mockMvc.perform(post("/api/admin/code-groups")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateCodeGroupRequiresAuthBeforeTableSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/code-groups/PROC_STATUS")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"PROC_STATUS\",\"groupName\":\"처리상태\",\"managingDepartment\":\"교수지원과\",\"changeReason\":\"권한 검증\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(codeGroupManagementService, never()).updateCodeGroup(any(), any(), any());
    }

    @Test
    void updateCodeGroupValidationRejectsMissingFieldsBeforeTableSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/code-groups/PROC_STATUS")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").exists());
        verify(codeGroupManagementService, never()).updateCodeGroup(any(), any(), any());
    }

    @Test
    void updateCodeGroupBusinessRulePreventsTableSideEffectAndReturnsFieldError() throws Exception {
        when(codeGroupManagementService.updateCodeGroup(eq("PROC_STATUS"), any(CodeGroupRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("코드그룹 업무 규칙 위반",
                        List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("groupId", "코드그룹 식별자는 변경할 수 없습니다."))));

        mockMvc.perform(put("/api/admin/code-groups/PROC_STATUS")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"PROC_STATUS_NEW\",\"groupName\":\"처리상태\",\"managingDepartment\":\"교수지원과\",\"changeReason\":\"업무 규칙 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("groupId"));
    }

    @Test
    void createCodeGroupBusinessRulePreventsTableSideEffectAndReturnsFieldError() throws Exception {
        when(codeGroupManagementService.createCodeGroup(any(CodeGroupRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("코드그룹 업무 규칙 위반",
                        List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("groupId", "이미 등록된 코드그룹입니다."))));

        mockMvc.perform(post("/api/admin/code-groups")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"PROC_STATUS\",\"groupName\":\"처리상태\",\"managingDepartment\":\"교수지원과\",\"changeReason\":\"업무 규칙 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("groupId"));
    }

    @Test
    void updateCodeGroupRejectsMissingFieldsBeforeCodeGroupsSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/code-groups/PROC_STATUS")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(codeGroupManagementService, never()).updateCodeGroup(any(), any(), any());
    }

    @Test
    void updateCodeGroupBusinessRulePreventsCodeGroupsSideEffectAndReturnsFieldError() throws Exception {
        when(codeGroupManagementService.updateCodeGroup(eq("PROC_STATUS"), any(CodeGroupRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("코드그룹 업무 규칙 위반",
                        List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("groupId", "경로 코드그룹과 요청 코드그룹이 다릅니다."))));

        mockMvc.perform(put("/api/admin/code-groups/PROC_STATUS")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"PROC_STATUS_NEW\",\"groupName\":\"처리상태\",\"managingDepartment\":\"교수지원과\",\"changeReason\":\"업무 규칙 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("groupId"));
    }

    @Test
    void serviceRejectsDuplicateGroupCreationWithoutChangingRows() {
        CodeGroupManagementMapper mapper = mock(CodeGroupManagementMapper.class);
        CodeGroupManagementService service = new CodeGroupManagementService(mapper);
        CodeGroupRequest request = validRequest("AUTH_TYPE");
        when(mapper.findCodeGroupById("AUTH_TYPE")).thenReturn(new CodeGroupRow("AUTH_TYPE", "인증구분", "", "교수지원과", "Y", "ACTIVE", 0, null, null));

        assertThatThrownBy(() -> service.createCodeGroup(request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("이미 등록");
        verify(mapper, never()).insertCodeGroup(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void serviceRejectsGroupIdMutationUntilReq056IsResolvedWithoutSideEffects() {
        CodeGroupManagementMapper mapper = mock(CodeGroupManagementMapper.class);
        CodeGroupManagementService service = new CodeGroupManagementService(mapper);
        CodeGroupRequest request = validRequest("AUTH_TYPE_NEW");
        when(mapper.findCodeGroupById("AUTH_TYPE")).thenReturn(new CodeGroupRow("AUTH_TYPE", "인증구분", "", "교수지원과", "Y", "ACTIVE", 0, null, null));

        assertThatThrownBy(() -> service.updateCodeGroup("AUTH_TYPE", request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("코드그룹 요청");
        verify(mapper, never()).updateCodeGroup(any(), any(), any(), any(), any(), any(), any());
    }

    private CodeGroupRequest validRequest(String groupId) {
        CodeGroupRequest request = new CodeGroupRequest();
        request.setGroupId(groupId);
        request.setGroupName("인증구분");
        request.setDescription("인증구분 코드 묶음");
        request.setManagingDepartment("교수지원과");
        request.setSystemUseYn("Y");
        request.setChangeReason("코드그룹 관리");
        return request;
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

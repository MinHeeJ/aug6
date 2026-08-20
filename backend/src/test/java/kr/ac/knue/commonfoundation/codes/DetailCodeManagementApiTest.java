package kr.ac.knue.commonfoundation.codes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

@WebMvcTest(DetailCodeManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DetailCodeManagementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean DetailCodeManagementService detailCodeManagementService;

    @Test
    void listDetailCodesReturnsGroupScopedHierarchyAndMappingFields() throws Exception {
        when(detailCodeManagementService.listDetailCodes("COMMON_STATUS", 0, 20)).thenReturn(List.of(
                activeRow("COMMON_STATUS", "ACTIVE", "활성", null, 1, "{\"korusCode\":\"01\"}"),
                activeRow("COMMON_STATUS", "ACTIVE_CHILD", "활성 하위", "ACTIVE", 2, null)));

        mockMvc.perform(get("/api/admin/code-groups/COMMON_STATUS/codes").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].groupId").value("COMMON_STATUS"))
                .andExpect(jsonPath("$.data[0].codeValue").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].additionalAttributes").value("{\"korusCode\":\"01\"}"))
                .andExpect(jsonPath("$.data[1].parentCodeValue").value("ACTIVE"));
    }

    @Test
    void createDetailCodePersistsEditableCodeFieldsAndReturnsCreatedRow() throws Exception {
        when(detailCodeManagementService.createDetailCode(eq("COMMON_STATUS"), any(DetailCodeRequest.class), eq(1L)))
                .thenReturn(activeRow("COMMON_STATUS", "PENDING", "대기", "ACTIVE", 3, "{\"externalCode\":\"P\"}"));

        mockMvc.perform(post("/api/admin/code-groups/COMMON_STATUS/codes")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeValue\":\"PENDING\",\"codeName\":\"대기\",\"parentCodeValue\":\"ACTIVE\",\"sortOrder\":3,\"additionalAttributes\":{\"externalCode\":\"P\"},\"systemUseYn\":\"Y\",\"validStartDate\":\"2026-01-01\",\"changeReason\":\"상세코드 등록\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value("COMMON_STATUS"))
                .andExpect(jsonPath("$.data.codeValue").value("PENDING"))
                .andExpect(jsonPath("$.data.parentCodeValue").value("ACTIVE"));
    }

    @Test
    void updateDetailCodeKeepsPathIdentityAndReturnsUpdatedMetadata() throws Exception {
        when(detailCodeManagementService.updateDetailCode(eq("COMMON_STATUS"), eq("PENDING"), any(DetailCodeRequest.class), eq(1L)))
                .thenReturn(new DetailCodeRow("COMMON_STATUS", "PENDING", "처리 대기", null, 4, null, "N", LocalDate.parse("2026-01-01"), null, "INACTIVE", LocalDateTime.parse("2026-03-01T09:00:00"), LocalDateTime.parse("2026-03-02T10:00:00")));

        mockMvc.perform(put("/api/admin/code-groups/COMMON_STATUS/codes/PENDING")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeValue\":\"PENDING\",\"codeName\":\"처리 대기\",\"sortOrder\":4,\"systemUseYn\":\"N\",\"validStartDate\":\"2026-01-01\",\"changeReason\":\"명칭 수정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codeValue").value("PENDING"))
                .andExpect(jsonPath("$.data.codeName").value("처리 대기"))
                .andExpect(jsonPath("$.data.systemUseYn").value("N"));
    }

    @Test
    void mutatingDetailCodesRequiresAuthenticatedAdminSession() throws Exception {
        mockMvc.perform(post("/api/admin/code-groups/COMMON_STATUS/codes")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeValue\":\"PENDING\",\"codeName\":\"대기\",\"sortOrder\":3,\"changeReason\":\"상세코드 등록\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void createDetailCodeReturnsFieldErrorsForMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/admin/code-groups/COMMON_STATUS/codes")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateDetailCodeRequiresAuthBeforeTableSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/code-groups/COMMON_STATUS/codes/PENDING")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeValue\":\"PENDING\",\"codeName\":\"대기\",\"sortOrder\":3,\"systemUseYn\":\"Y\",\"validStartDate\":\"2026-01-01\",\"changeReason\":\"권한 검증\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(detailCodeManagementService, never()).updateDetailCode(any(), any(), any(), any());
    }

    @Test
    void createDetailCodeBusinessRulePreventsTableSideEffectAndReturnsFieldError() throws Exception {
        when(detailCodeManagementService.createDetailCode(eq("COMMON_STATUS"), any(DetailCodeRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("상세코드 업무 규칙 위반",
                        List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("codeValue", "이미 등록된 상세코드입니다."))));

        mockMvc.perform(post("/api/admin/code-groups/COMMON_STATUS/codes")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeValue\":\"PENDING\",\"codeName\":\"대기\",\"sortOrder\":3,\"systemUseYn\":\"Y\",\"validStartDate\":\"2026-01-01\",\"changeReason\":\"업무 규칙 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("codeValue"));
    }

    @Test
    void updateDetailCodeRejectsMissingFieldsBeforeDetailCodesSideEffect() throws Exception {
        mockMvc.perform(put("/api/admin/code-groups/COMMON_STATUS/codes/PENDING")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(detailCodeManagementService, never()).updateDetailCode(any(), any(), any(), any());
    }

    @Test
    void updateDetailCodeBusinessRulePreventsDetailCodesSideEffectAndReturnsFieldError() throws Exception {
        when(detailCodeManagementService.updateDetailCode(eq("COMMON_STATUS"), eq("PENDING"), any(DetailCodeRequest.class), eq(1L)))
                .thenThrow(new BusinessValidationException("상세코드 업무 규칙 위반",
                        List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("codeValue", "경로 상세코드와 요청 상세코드가 다릅니다."))));

        mockMvc.perform(put("/api/admin/code-groups/COMMON_STATUS/codes/PENDING")
                        .requestAttr("currentUser", adminUser())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeValue\":\"PENDING_NEW\",\"codeName\":\"대기\",\"sortOrder\":3,\"systemUseYn\":\"Y\",\"validStartDate\":\"2026-01-01\",\"changeReason\":\"업무 규칙 검증\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("codeValue"));
    }

    @Test
    void serviceRejectsDuplicateDetailCodeWithoutChangingRows() {
        DetailCodeManagementMapper mapper = mock(DetailCodeManagementMapper.class);
        DetailCodeManagementService service = new DetailCodeManagementService(mapper);
        DetailCodeRequest request = validRequest("ACTIVE");
        when(mapper.findCodeGroupById("COMMON_STATUS")).thenReturn(new CodeGroupRow("COMMON_STATUS", "공통 상태", "", "시스템관리", "Y", "ACTIVE", 2, null, null));
        when(mapper.findDetailCode("COMMON_STATUS", "ACTIVE")).thenReturn(activeRow("COMMON_STATUS", "ACTIVE", "활성", null, 1, null));

        assertThatThrownBy(() -> service.createDetailCode("COMMON_STATUS", request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("이미 등록");
        verify(mapper, never()).insertDetailCode(any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void serviceRejectsUnresolvedAdditionalAttributeMutationAndCodeValueMutationWithoutSideEffects() {
        DetailCodeManagementMapper mapper = mock(DetailCodeManagementMapper.class);
        DetailCodeManagementService service = new DetailCodeManagementService(mapper);
        DetailCodeRequest request = validRequest("INACTIVE");
        request.putAdditionalAttribute("unconfirmed", "value");
        when(mapper.findCodeGroupById("COMMON_STATUS")).thenReturn(new CodeGroupRow("COMMON_STATUS", "공통 상태", "", "시스템관리", "Y", "ACTIVE", 2, null, null));
        when(mapper.findDetailCode("COMMON_STATUS", "ACTIVE")).thenReturn(activeRow("COMMON_STATUS", "ACTIVE", "활성", null, 1, null));

        assertThatThrownBy(() -> service.updateDetailCode("COMMON_STATUS", "ACTIVE", request, 1L))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("상세코드 요청");
        verify(mapper, never()).updateDetailCode(any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any());
    }

    private DetailCodeRequest validRequest(String codeValue) {
        DetailCodeRequest request = new DetailCodeRequest();
        request.setCodeValue(codeValue);
        request.setCodeName("활성");
        request.setSortOrder(1);
        request.setSystemUseYn("Y");
        request.setValidStartDate(LocalDate.parse("2026-01-01"));
        request.setChangeReason("상세코드 관리");
        return request;
    }

    private DetailCodeRow activeRow(String groupId, String codeValue, String codeName, String parentCodeValue, int sortOrder, String additionalAttributes) {
        return new DetailCodeRow(groupId, codeValue, codeName, parentCodeValue, sortOrder, additionalAttributes, "Y", LocalDate.parse("2026-01-01"), null, "ACTIVE", LocalDateTime.parse("2026-03-01T09:00:00"), LocalDateTime.parse("2026-03-01T09:00:00"));
    }

    private CurrentUser adminUser() {
        return new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

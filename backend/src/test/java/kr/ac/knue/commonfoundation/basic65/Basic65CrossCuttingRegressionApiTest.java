package kr.ac.knue.commonfoundation.basic65;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.CodedConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.PayloadTooLargeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase 6 API regression boundary for BASIC-65 common guards.
 *
 * <p>Service tests own persistence-level no-change assertions. These MockMvc tests verify the stable HTTP
 * envelope presented to browser clients when a shared guard rejects a request.</p>
 */
@WebMvcTest({TeachingAchievementController.class, StudentGuidanceAchievementController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic65CrossCuttingRegressionApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeachingAchievementService teachingService;

    @MockBean
    private StudentGuidanceAchievementService studentGuidanceService;

    @Test
    void unauthenticatedMutationReturns401WithoutCallingBusinessService() throws Exception {
        mockMvc.perform(post("/api/business/teaching-achievements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teachingRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(teachingService);
    }

    @Test
    void unauthorizedRoleReturns403WithoutCallingBusinessService() throws Exception {
        mockMvc.perform(post("/api/business/teaching-achievements")
                        .requestAttr("currentUser", user("R02"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teachingRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verifyNoInteractions(teachingService);
    }

    @Test
    void inactiveInputPeriodIsReturnedAs409WithPeriodCodeAndNoSensitiveDetails() throws Exception {
        when(teachingService.save(any(), any(), any()))
                .thenThrow(new CodedConflictException("PERIOD_NOT_ACTIVE", "입력기간이 아닙니다."));

        mockMvc.perform(post("/api/business/teaching-achievements")
                        .requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teachingRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"))
                .andExpect(jsonPath("$.error.message").value("입력기간이 아닙니다."))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL"))));
    }

    @Test
    void confirmedAchievementIsLockedWith409AndStableErrorCode() throws Exception {
        when(teachingService.save(any(), any(), any()))
                .thenThrow(new CodedConflictException("CONFIRMED_DATA_LOCKED", "평가확정된 강의실적은 수정할 수 없습니다."));

        mockMvc.perform(post("/api/business/teaching-achievements")
                        .requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teachingRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
    }

    @Test
    void oversizedStudentGuidanceUploadIsReturnedAs413WithoutSensitiveDetails() throws Exception {
        when(studentGuidanceService.upload(any(), any(), any(), any()))
                .thenThrow(new PayloadTooLargeException("첨부파일 용량이 허용 한도를 초과했습니다."));
        MockMultipartFile file = new MockMultipartFile(
                "file", "student-guidance.csv", "text/csv", "evaluationYear,studentNo\n2026,S1".getBytes());

        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads")
                        .file(file)
                        .requestAttr("currentUser", user("R07")))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FILE_TOO_LARGE"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Exception"))));
    }

    private CurrentUser user(String role) {
        return new CurrentUser(1L, "faculty", "E0001", "교원", List.of(role), List.of());
    }

    private String teachingRequest() {
        return """
                {"evaluationYear":"2026","academicYear":"2026","semester":"1",
                "courseCode":"B65-REGRESSION","courseName":"공통 회귀 검증","courseType":"MAJOR",
                "creditHours":3.0,"dynamicFields":{},"attachmentRefs":[],"changeReason":"Phase 6 regression"}
                """;
    }
}

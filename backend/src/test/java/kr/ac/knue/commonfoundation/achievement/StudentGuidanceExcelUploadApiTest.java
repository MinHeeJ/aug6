package kr.ac.knue.commonfoundation.achievement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Defines the R07 student-guidance Excel validation contract before the
 * STUDENT_GUIDANCE wrapper is connected to the existing Excel operation service.
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentGuidanceExcelUploadApiTest {
    @Autowired MockMvc mockMvc;

    private final CurrentUser excelOperator = new CurrentUser(
            7L, "excel-operator", "E7007", "일괄등록 담당자", List.of("R07"), List.of());

    @Test
    void r07CanValidateStudentGuidanceTemplateAndReceivesSeparatedNormalRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "student-guidance.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "templateVersion,managementItemCode,guidanceStartDate,guidanceEndDate,studentCount\n"
                        .concat("STUDENT_GUIDANCE-v1,EDU-STUDENT-GUIDANCE,2026-03-01,2026-06-30,2\n")
                        .getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads")
                        .file(file)
                        .requestAttr("currentUser", excelOperator)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"))
                        .header("X-Request-Id", "REQ-B77-SG-EXCEL-VALIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadId").isNotEmpty())
                .andExpect(jsonPath("$.data.businessType").value("STUDENT_GUIDANCE"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.errorCount").value(0))
                .andExpect(jsonPath("$.data.errors").isEmpty())
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-SG-EXCEL-VALIDATE"));
    }
}

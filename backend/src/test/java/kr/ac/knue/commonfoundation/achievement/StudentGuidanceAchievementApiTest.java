package kr.ac.knue.commonfoundation.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.io.InputStream;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import kr.ac.knue.commonfoundation.excel.ExcelUploadCommitResult;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentGuidanceAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentGuidanceAchievementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudentGuidanceAchievementService service;
    @MockBean ExcelOperationsService excelService;

    @Test
    void r07ValidatesStudentGuidanceUploadAndOnlyR07MayUseExcelFlow() throws Exception {
        try (InputStream contract = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            org.assertj.core.api.Assertions.assertThat(new String(contract.readAllBytes()))
                    .contains("operationId: createStudentGuidanceAchievementExcelUpload")
                    .contains("businessType은 STUDENT_GUIDANCE 고정");
        }
        when(excelService.createExcelUpload(eq("STUDENT_GUIDANCE"), eq(null), any(), eq(7001L)))
                .thenReturn(new ExcelUploadResult("UP-SG-1", "STUDENT_GUIDANCE", "guidance.csv", "VALIDATED", 1, 1, 0, 0, 0, List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "guidance.csv", "text/csv", "managementItemCode,guidanceStartDate\nEDU-GUIDANCE-A,2026-03-01\n".getBytes());
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).requestAttr("currentUser", r07()).cookie(cookie()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.businessType").value("STUDENT_GUIDANCE"))
                .andExpect(jsonPath("$.data.successCount").value(1));
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).requestAttr("currentUser", r01()).cookie(cookie()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(excelService, never()).createExcelUpload(eq("STUDENT_GUIDANCE"), eq(null), any(), eq(1001L));
    }

    @Test
    void commitReturnsSavedCountAndErrorRowsAreNotCommittedByController() throws Exception {
        when(excelService.commitExcelUpload("UP-SG-1", 7001L)).thenReturn(new ExcelUploadCommitResult("UP-SG-1", 1));
        mockMvc.perform(post("/api/business/student-guidance-achievements/excel-uploads/UP-SG-1/commit").requestAttr("currentUser", r07()).cookie(cookie()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(1));
    }

    @Test
    void individualSaveRejectsR07BeforeServiceMutation() throws Exception {
        mockMvc.perform(post("/api/business/student-guidance-achievements")
                        .requestAttr("currentUser", r07())
                        .cookie(cookie())
                        .contentType("application/json")
                        .content("{\"managementItemCode\":\"EDU-GUIDANCE-A\",\"guidanceStartDate\":\"2026-03-01\",\"guidanceEndDate\":\"2026-03-02\",\"students\":[{\"studentName\":\"학생\"}]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), eq(r07()));
    }

    private CurrentUser r07() { return new CurrentUser(7001L, "operator", "E7001", "운영자", List.of("R07"), List.of()); }
    private CurrentUser r01() { return new CurrentUser(1001L, "professor1", "E1001", "교원", List.of("R01"), List.of()); }
    private Cookie cookie() { return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"); }
}

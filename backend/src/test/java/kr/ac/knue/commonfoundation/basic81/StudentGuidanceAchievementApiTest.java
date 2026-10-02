package kr.ac.knue.commonfoundation.basic81;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

/** MockMvc contract tests for the controller owning student-guidance and R07 Excel routes. */
@WebMvcTest(StudentGuidanceAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentGuidanceAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private StudentGuidanceAchievementService service;

    private final CurrentUser r01 = new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(107L, "excel-operator", "E0107", "엑셀담당자", List.of("R07"), List.of());

    @Test
    void approvedOpenApiFixtureDeclaresStudentGuidanceOperations() throws Exception {
        String openApi = StreamUtils.copyToString(new ClassPathResource("contracts/openapi.yaml").getInputStream(), StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/student-guidance-achievements:")
                .contains("operationId: listStudentGuidanceAchievements")
                .contains("operationId: createStudentGuidanceAchievementExcelUpload");
    }

    @Test
    void savesStudentGuidanceWithStudentsInApprovedEnvelope() throws Exception {
        when(service.save(any(), eq(r01), eq("REQ-B81-SG-SAVE"))).thenReturn(seedRow());
        mockMvc.perform(post("/api/business/student-guidance-achievements")
                        .requestAttr("currentUser", r01).cookie(cookie()).header("X-Request-Id", "REQ-B81-SG-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"STUDENT_GUIDANCE\",\"guidanceStartDate\":\"2026-03-01\",\"guidanceEndDate\":\"2026-03-31\",\"students\":[{\"studentName\":\"홍길동\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("STUDENT_GUIDANCE"))
                .andExpect(jsonPath("$.data.studentCount").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B81-SG-SAVE"));
        verify(service).save(any(), eq(r01), eq("REQ-B81-SG-SAVE"));
    }

    @Test
    void rejectsMissingStudentsBeforeServiceMutation() throws Exception {
        mockMvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser", r01).cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"STUDENT_GUIDANCE\",\"guidanceStartDate\":\"2026-03-01\",\"guidanceEndDate\":\"2026-03-31\",\"students\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'students')]").isNotEmpty());
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void r07CanValidateStudentGuidanceExcelAndR01Cannot() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "student-guidance.csv", "text/csv",
                "templateVersion,managementItemCode,guidanceStartDate,guidanceEndDate,studentName\nv1.0,STUDENT_GUIDANCE,2026-03-01,2026-03-31,홍길동".getBytes(StandardCharsets.UTF_8));
        when(service.validateExcelUpload(any(), eq(r07))).thenReturn(new StudentGuidanceExcelUploadResult(
                "SG-UP-001", "student-guidance.csv", 1, 1, 0, List.of()));
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file)
                        .requestAttr("currentUser", r07).cookie(cookie()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.successCount").value(1));
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file)
                        .requestAttr("currentUser", r01).cookie(cookie()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void errorsPreventCommitAndCanBeListedForR07() throws Exception {
        when(service.commitExcelUpload("SG-UP-ERR", r07, "REQ-B81-SG-COMMIT"))
                .thenThrow(new kr.ac.knue.commonfoundation.common.api.ConflictException("오류 행이 있어 전체 반영을 차단했습니다."));
        mockMvc.perform(post("/api/business/student-guidance-achievements/excel-uploads/SG-UP-ERR/commit")
                        .requestAttr("currentUser", r07).cookie(cookie()).header("X-Request-Id", "REQ-B81-SG-COMMIT"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFLICT"));
        when(service.listExcelErrors("SG-UP-ERR", r07)).thenReturn(List.of(new StudentGuidanceExcelErrorRow(
                2, "studentName", "", "REQUIRED", "학생명은 필수입니다.", "현행 양식과 입력값을 확인하세요.")));
        mockMvc.perform(get("/api/business/student-guidance-achievements/excel-uploads/SG-UP-ERR/errors")
                        .requestAttr("currentUser", r07).cookie(cookie()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].rowNumber").value(2));
    }

    private StudentGuidanceAchievementRow seedRow() {
        return new StudentGuidanceAchievementRow(91L, "B77-SG-001", 101L, "faculty", "2026", "STUDENT_GUIDANCE",
                LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-31"), 1, "DRAFT", null,
                "[{\"studentName\":\"홍길동\"}]", LocalDateTime.parse("2026-03-01T09:00:00"), LocalDateTime.parse("2026-03-01T09:00:00"));
    }
    private Cookie cookie() { return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"); }
}

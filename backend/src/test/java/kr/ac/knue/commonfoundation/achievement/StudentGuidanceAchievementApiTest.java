package kr.ac.knue.commonfoundation.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentGuidanceAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentGuidanceAchievementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudentGuidanceAchievementService service;
    private final CurrentUser r07 = new CurrentUser(7L, "excel", "E0007", "Excel 담당", List.of("R07"), List.of());
    private final CurrentUser r01 = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void r07ValidatesStudentGuidanceUploadAndReadsOwnHistory() throws Exception {
        when(service.validateUpload(any(), eq(r07))).thenReturn(new StudentGuidanceDtos.UploadResult("SG-UP-1", "학생지도.csv", 1, 1, 0, List.of()));
        when(service.histories(eq(r07))).thenReturn(List.of(new StudentGuidanceDtos.UploadHistory("SG-UP-1", "학생지도.csv", 7L, 1, 1, 0, 0, LocalDateTime.parse("2026-03-01T10:00:00"))));
        MockMultipartFile file = new MockMultipartFile("file", "학생지도.csv", "text/csv", "employeeNo,evaluationYear,organizationCode,managementItemCode,guidanceStartDate,guidanceEndDate,studentName\nE0002,2026,ORG-1,B77-SG-001,2026-03-01,2026-06-30,학생A\n".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).requestAttr("currentUser", r07))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.successCount").value(1)).andExpect(jsonPath("$.data.errorCount").value(0));
        mockMvc.perform(get("/api/business/student-guidance-achievements/excel-uploads/histories").requestAttr("currentUser", r07))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].uploaderUserId").value(7));
    }

    @Test
    void r09AdministratorCanReadStudentGuidanceUploadHistoryAndTemplate() throws Exception {
        CurrentUser administrator = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
        when(service.histories(eq(administrator))).thenReturn(List.of());
        when(service.template(eq(administrator))).thenReturn("employeeNo\n".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/business/student-guidance-achievements/excel-uploads/histories").requestAttr("currentUser", administrator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(get("/api/business/student-guidance-achievements/excel-uploads/template").requestAttr("currentUser", administrator))
                .andExpect(status().isOk());
    }

    @Test
    void individualSaveRequiresGuidanceStudentDetails() throws Exception {
        mockMvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser", r01)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"organizationCode\":\"ORG-1\",\"managementItemCode\":\"B77-SG-001\",\"guidanceStartDate\":\"2026-03-01\",\"guidanceEndDate\":\"2026-06-30\",\"changeReason\":\"등록\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void r01SavesIndividualGuidanceWithStudentDetails() throws Exception {
        when(service.save(any(), eq(r01))).thenReturn(new StudentGuidanceDtos.Row(31L, "2026", 2L, "ORG-1",
                "B77-SG-001", java.time.LocalDate.parse("2026-03-01"), java.time.LocalDate.parse("2026-06-30"),
                1, List.of(new StudentGuidanceDtos.Student("학생A", java.util.Map.of())), "DRAFTING",
                LocalDateTime.parse("2026-03-01T10:00:00")));

        mockMvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser", r01)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2026\",\"organizationCode\":\"ORG-1\",\"managementItemCode\":\"B77-SG-001\",\"guidanceStartDate\":\"2026-03-01\",\"guidanceEndDate\":\"2026-06-30\",\"students\":[{\"studentName\":\"학생A\",\"detail\":{}}],\"achievementDetail\":{},\"changeReason\":\"등록\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(31))
                .andExpect(jsonPath("$.data.students[0].studentName").value("학생A"));
    }

    @Test
    void nonR07CannotUploadAndErrorUploadCannotCommit() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "학생지도.csv", "text/csv", "x".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).requestAttr("currentUser", r01))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        org.mockito.Mockito.doThrow(new kr.ac.knue.commonfoundation.common.api.ConflictException("오류 행이 있어 전체 반영을 차단했습니다."))
                .when(service).commitUpload("SG-UP-ERR", r07);
        mockMvc.perform(post("/api/business/student-guidance-achievements/excel-uploads/SG-UP-ERR/commit").requestAttr("currentUser", r07))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }
}

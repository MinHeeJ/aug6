package kr.ac.knue.commonfoundation.basic65;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentGuidanceAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentGuidanceAchievementApiTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private StudentGuidanceAchievementService service;

    @Test
    void r01ListsOwnStudentGuidanceAchievementsWithFilterContract() throws Exception {
        when(service.list(any(), any())).thenReturn(new StudentGuidanceAchievementSearchResponse(List.of(row(71L)), 0, 20, 1));
        mockMvc.perform(get("/api/business/student-guidance-achievements").requestAttr("currentUser", user("R01")).param("studentKeyword", "김학생"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentGuidanceAchievements[0].studentName").value("김학생"))
                .andExpect(jsonPath("$.data.size").value(20));
        verify(service).list(any(), any());
    }

    @Test
    void r01SavesStudentGuidanceWithRequestIdAndRequiredFields() throws Exception {
        when(service.save(any(), any(), any())).thenReturn(row(72L));
        mockMvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser", user("R01"))
                        .header("X-Request-Id", "student-guidance-save").contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.guidanceType").value("ADVISORY"))
                .andExpect(jsonPath("$.data.guidanceDate").value("2026-03-03"))
                .andExpect(jsonPath("$.meta.requestId").value("student-guidance-save"));
        verify(service).save(any(), any(), any());
    }

    @Test
    void r07UploadsStudentGuidanceTemplateAndReceivesValidationResult() throws Exception {
        when(service.upload(any(), any(), any(), any())).thenReturn(new StudentGuidanceExcelUploadResult("UP-guidance", "VALIDATED", 1, 1, 0, 1, List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "student-guidance.csv", "text/csv", "evaluationYear,academicYear,semester,studentNo,studentName,guidanceType,guidanceDate,guidanceContent\n2026,2026,1,S1,김학생,ADVISORY,2026-03-03,상담".getBytes());
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).param("templateId", "STUDENT-GUIDANCE-V1").requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.validationStatus").value("VALIDATED"))
                .andExpect(jsonPath("$.data.savedCount").value(1));
        verify(service).upload(any(), any(), any(), any());
    }

    @Test
    void uploadErrorsAreReturnedWithoutSensitiveInternals() throws Exception {
        when(service.upload(any(), any(), any(), any())).thenThrow(new kr.ac.knue.commonfoundation.common.api.BusinessValidationException("학생지도 Excel 파일을 선택하세요.", List.of(new kr.ac.knue.commonfoundation.common.api.ValidationError("file", "파일은 필수입니다."))));
        MockMultipartFile file = new MockMultipartFile("file", "student-guidance.csv", "text/csv", new byte[0]);
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).requestAttr("currentUser", user("R07")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields").isArray())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL"))));
    }

    @Test
    void r02CannotUploadStudentGuidanceRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "student-guidance.csv", "text/csv", "header\nrow".getBytes());
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).requestAttr("currentUser", user("R02")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private CurrentUser user(String role) { return new CurrentUser(1L, "faculty", "E0001", "교원", List.of(role), List.of()); }
    private String validRequest() { return "{\"evaluationYear\":\"2026\",\"academicYear\":\"2026\",\"semester\":\"1\",\"studentNo\":\"S1\",\"studentName\":\"김학생\",\"guidanceType\":\"ADVISORY\",\"guidanceDate\":\"2026-03-03\",\"guidanceContent\":\"상담\",\"dynamicFields\":{},\"attachmentRefs\":[],\"changeReason\":\"학생지도 입력\"}"; }
    private StudentGuidanceAchievementRow row(Long id) { return new StudentGuidanceAchievementRow(id, 1L, "2026", "2026", "1", "S1", "김학생", "ADVISORY", LocalDate.parse("2026-03-03"), "상담", "DRAFTING", null, "{}", "[]", LocalDateTime.parse("2026-09-15T09:00:00"), LocalDateTime.parse("2026-09-15T09:00:00")); }
}

package kr.ac.knue.commonfoundation.basic71;

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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import kr.ac.knue.commonfoundation.auth.AuthController;
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

/** Defines the approved education-achievement HTTP contract for faculty workflows. */
@WebMvcTest(EducationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EducationAchievementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EducationAchievementService service;

    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());
    private final CurrentUser chair = new CurrentUser(3L, "chair", "E0003", "학과장", List.of("R02"), List.of());
    private final CurrentUser excelManager = new CurrentUser(7L, "excel-manager", "E0007", "엑셀담당", List.of("R07"), List.of());

    @Test
    void teacherCanCreateLectureEvaluationAndReceivesTheSavedAchievement() throws Exception {
        when(service.create(any(), eq(2L), eq("REQ-B71-CREATE"))).thenReturn(row(101L, "DRAFT"));

        mockMvc.perform(post("/api/faculty/education-achievements")
                        .requestAttr("currentUser", teacher).cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B71-CREATE").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementType\":\"LECTURE_EVALUATION\",\"managementItemCode\":\"LECTURE_EVAL\",\"achievementOccurredOn\":\"2026-03-01\",\"details\":{\"courseName\":\"교육학\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementId").value(101))
                .andExpect(jsonPath("$.data.achievementType").value("LECTURE_EVALUATION"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B71-CREATE"));
        verify(service).create(any(), eq(2L), eq("REQ-B71-CREATE"));
    }

    @Test
    void nonTeacherCannotCreateEducationAchievement() throws Exception {
        mockMvc.perform(post("/api/faculty/education-achievements")
                        .requestAttr("currentUser", chair).cookie(sessionCookie()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementType\":\"LECTURE_PERFORMANCE\",\"managementItemCode\":\"LECTURE\",\"achievementOccurredOn\":\"2026-03-01\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void missingManagementItemCodeReturnsFieldValidationError() throws Exception {
        mockMvc.perform(post("/api/faculty/education-achievements")
                        .requestAttr("currentUser", teacher).cookie(sessionCookie()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementType\":\"LECTURE_EVALUATION\",\"achievementOccurredOn\":\"2026-03-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty());
    }

    @Test
    void chairCanConfirmSubmittedAchievementAndTransitionResponseContainsNewStatus() throws Exception {
        when(service.transition(eq(101L), any(), eq(chair), eq("REQ-B71-TRANSITION"))).thenReturn(row(101L, "DEPARTMENT_CONFIRMED"));

        mockMvc.perform(post("/api/faculty/education-achievements/101/transition")
                        .requestAttr("currentUser", chair).cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B71-TRANSITION").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"DEPARTMENT_CONFIRM\",\"evidenceRef\":\"검토완료\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificationStatus").value("DEPARTMENT_CONFIRMED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B71-TRANSITION"));
    }

    @Test
    void departmentRejectionRequiresOpinion() throws Exception {
        mockMvc.perform(post("/api/faculty/education-achievements/101/transition")
                        .requestAttr("currentUser", chair).cookie(sessionCookie()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"DEPARTMENT_REJECT\",\"evidenceRef\":\"검토\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'opinion')]").isNotEmpty());
    }

    @Test
    void excelManagerCanValidateStudentGuidanceUpload() throws Exception {
        when(service.validateStudentGuidanceUpload(any(), eq(7L), eq("REQ-B71-UPLOAD"))).thenReturn(new StudentGuidanceUploadResult("UP-1", 1, 1, 0, List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "학생지도.csv", "text/csv", "managementItemCode\nGUIDANCE".getBytes());

        mockMvc.perform(multipart("/api/faculty/student-guidance-achievements/excel-uploads").file(file)
                        .requestAttr("currentUser", excelManager).cookie(sessionCookie()).header("X-Request-Id", "REQ-B71-UPLOAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadId").value("UP-1"))
                .andExpect(jsonPath("$.data.normalCount").value(1));
    }

    @Test
    void teacherCannotUploadStudentGuidanceExcel() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "학생지도.csv", "text/csv", "managementItemCode\nGUIDANCE".getBytes());
        mockMvc.perform(multipart("/api/faculty/student-guidance-achievements/excel-uploads").file(file)
                        .requestAttr("currentUser", teacher).cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).validateStudentGuidanceUpload(any(), any(), any());
    }

    @Test
    void mapperProjectionCanConstructAnUnhydratedAchievementRow() {
        assertThat(Arrays.stream(EducationAchievementRow.class.getDeclaredConstructors())
                .mapToInt(constructor -> constructor.getParameterCount()))
                .contains(9);
    }

    private EducationAchievementRow row(Long id, String status) {
        return new EducationAchievementRow(id, "LECTURE_EVALUATION", 2L, "2026", "LECTURE_EVAL", LocalDate.parse("2026-03-01"), status, false, null, List.of(), List.of());
    }

    private Cookie sessionCookie() { return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"); }
}

package kr.ac.knue.commonfoundation.educationachievements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

/** HTTP contract coverage for the BASIC-72 education-achievement endpoints. */
@WebMvcTest(EducationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EducationAchievementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EducationAchievementService service;

    @Test
    void r01CanSaveAndThenListLectureEvaluationAchievement() throws Exception {
        CurrentUser professor = user("R01");
        EducationAchievement saved = achievement(10L, "강의평가");
        when(service.saveLectureEvaluation(any(), any())).thenReturn(saved);
        when(service.listLectureEvaluations(anyInt(), anyInt(), any())).thenReturn(new EducationAchievementPage(List.of(saved), 0, 20, 1));

        mockMvc.perform(post("/api/business/lecture-evaluations")
                        .requestAttr("currentUser", professor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"LECTURE_EVALUATION\",\"occurredOn\":\"2026-03-01\",\"detailContent\":\"강의평가 실적\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.managementItemCode").value("LECTURE_EVALUATION"));
        mockMvc.perform(get("/api/business/lecture-evaluations").requestAttr("currentUser", professor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].detailContent").value("강의평가 실적"));
    }

    @Test
    void nonProfessorCannotSaveLectureEvaluationAndMissingRequiredFieldIsReported() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluations")
                        .requestAttr("currentUser", user("R02"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"LECTURE_EVALUATION\",\"occurredOn\":\"2026-03-01\",\"detailContent\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(post("/api/business/lecture-evaluations")
                        .requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"occurredOn\":\"2026-03-01\",\"detailContent\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode"));
    }

    @Test
    void r07ExcelUploadReturnsValidationCountsAndOtherRolesAreForbidden() throws Exception {
        when(service.validateStudentGuidanceUpload(any(), any())).thenReturn(new StudentGuidanceUploadResult(7L, 1, 0, 1, null));
        MockMultipartFile file = new MockMultipartFile("file", "student-guidance.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "sheet".getBytes());
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel/uploads").file(file).requestAttr("currentUser", user("R07")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failureCount").value(0));
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel/uploads").file(file).requestAttr("currentUser", user("R01")))
                .andExpect(status().isForbidden());
    }

    @Test
    void graduateListIsReadableByAuthorizedFacultyRole() throws Exception {
        when(service.listGraduateAchievements(anyInt(), anyInt(), any())).thenReturn(new EducationAchievementPage(List.of(achievement(22L, "석사 배출")), 0, 20, 1));
        mockMvc.perform(get("/api/business/graduate-achievements").requestAttr("currentUser", user("R02")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].detailContent").value("석사 배출"));
    }

    @Test
    void allFourEducationAchievementCreateOperationsAreAvailableToAnAuthorizedUser() throws Exception {
        EducationAchievement saved = achievement(30L, "저장된 실적");
        when(service.saveLectureAchievement(any(), any())).thenReturn(saved);
        when(service.saveStudentGuidanceAchievement(any(), any())).thenReturn(saved);
        when(service.saveGraduateAchievement(any(), any())).thenReturn(saved);
        String payload = "{\"managementItemCode\":\"EDU-01\",\"occurredOn\":\"2026-03-01\",\"detailContent\":\"저장된 실적\"}";
        mockMvc.perform(post("/api/business/lecture-achievements").requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk());
        mockMvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk());
        mockMvc.perform(post("/api/business/graduate-achievements").requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk());
    }

    @Test
    void authorizedUserCanUpdateLectureEvaluation() throws Exception {
        when(service.updateAchievement(any(), any(), any())).thenReturn(achievement(10L, "수정된 강의평가"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/business/lecture-evaluations/10")
                        .requestAttr("currentUser", user("R01")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"LECTURE_EVALUATION\",\"occurredOn\":\"2026-03-02\",\"detailContent\":\"수정된 강의평가\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detailContent").value("수정된 강의평가"));
    }

    private CurrentUser user(String role) { return new CurrentUser(1L, "professor1", "E0001", "교원", List.of(role), List.of()); }
    private EducationAchievement achievement(Long id, String detail) { return new EducationAchievement(id, "LECTURE_EVALUATION", LocalDate.of(2026, 3, 1), detail, "DRAFT", false); }
}

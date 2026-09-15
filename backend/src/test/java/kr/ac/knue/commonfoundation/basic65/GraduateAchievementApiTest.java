package kr.ac.knue.commonfoundation.basic65;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GraduateAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GraduateAchievementApiTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private GraduateAchievementService service;

    @Test
    void r01SavesGraduateStudentDetailAndListsPersistedDetail() throws Exception {
        GraduateAchievementRow saved = row(81L);
        when(service.save(any(), any(), any())).thenReturn(saved);
        when(service.list(any(), any())).thenReturn(new GraduateAchievementSearchResponse(List.of(saved), 0, 20, 1));

        mockMvc.perform(post("/api/business/graduate-achievements")
                        .requestAttr("currentUser", user("R01"))
                        .header("X-Request-Id", "graduate-save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.degreeType").value("MASTER"))
                .andExpect(jsonPath("$.data.studentName").value("김석사"))
                .andExpect(jsonPath("$.data.thesisTitle").value("교육성과 연구"))
                .andExpect(jsonPath("$.data.awardDate").value("2026-02-20"))
                .andExpect(jsonPath("$.meta.requestId").value("graduate-save"));
        mockMvc.perform(get("/api/business/graduate-achievements")
                        .requestAttr("currentUser", user("R01"))
                        .param("degreeType", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.graduateAchievements[0].degreeType").value("MASTER"))
                .andExpect(jsonPath("$.data.graduateAchievements[0].studentName").value("김석사"))
                .andExpect(jsonPath("$.data.graduateAchievements[0].thesisTitle").value("교육성과 연구"))
                .andExpect(jsonPath("$.data.graduateAchievements[0].awardDate").value("2026-02-20"));
        verify(service).save(any(), any(), any());
        verify(service).list(any(), any());
    }

    @Test
    void r02CannotSaveGraduateAchievement() throws Exception {
        mockMvc.perform(post("/api/business/graduate-achievements")
                        .requestAttr("currentUser", user("R02"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void missingDegreeTypeReturnsFieldValidationWithoutSensitiveDetail() throws Exception {
        mockMvc.perform(post("/api/business/graduate-achievements")
                        .requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("\"degreeType\":\"MASTER\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields[0].field").value("degreeType"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL"))));
    }

    private CurrentUser user(String role) {
        return new CurrentUser(1L, "faculty", "E0001", "교원", List.of(role), List.of());
    }

    private String validRequest() {
        return "{\"evaluationYear\":\"2026\",\"academicYear\":\"2026\",\"semester\":\"1\",\"studentNo\":\"G202601\",\"studentName\":\"김석사\",\"degreeType\":\"MASTER\",\"thesisTitle\":\"교육성과 연구\",\"awardDate\":\"2026-02-20\",\"changeReason\":\"석사 배출 실적 입력\"}";
    }

    private GraduateAchievementRow row(Long id) {
        return new GraduateAchievementRow(id, 1L, "2026", "2026", "1", "G202601", "김석사", "MASTER", "교육성과 연구", LocalDate.parse("2026-02-20"), "DRAFTING", null, "{}", "[]", LocalDateTime.parse("2026-09-15T09:00:00"), LocalDateTime.parse("2026-09-15T09:00:00"));
    }
}

package kr.ac.knue.commonfoundation.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

@WebMvcTest(DegreeCompletionAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DegreeCompletionAchievementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean DegreeCompletionAchievementService service;
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listDegreeCompletionAchievementsReturnsSeedContract() throws Exception {
        when(service.list(any(), eq(teacher))).thenReturn(new DegreeCompletionDtos.SearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/degree-completion-achievements").requestAttr("currentUser", teacher)
                        .header("X-Request-Id", "B77-DC-001").param("evaluationYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode").value("B77-DC-001"))
                .andExpect(jsonPath("$.data.achievements[0].students[0].degreeType").value("MASTER"))
                .andExpect(jsonPath("$.meta.requestId").value("B77-DC-001"));
    }

    @Test
    void saveDegreeCompletionAchievementRejectsMissingStudentAwardDate() throws Exception {
        mockMvc.perform(post("/api/business/degree-completion-achievements").requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"evaluationYear":"2026","organizationCode":"ORG-1","managementItemCode":"B77-DC-001","occurredDate":"2026-02-20","students":[{"degreeType":"MASTER","studentName":"학생","thesisTitle":"논문"}],"changeReason":"등록"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listDegreeCompletionAchievementsRejectsUnauthorizedRole() throws Exception {
        CurrentUser uploader = new CurrentUser(7L, "excel", "E0007", "담당", List.of("R07"), List.of());
        mockMvc.perform(get("/api/business/degree-completion-achievements").requestAttr("currentUser", uploader))
                .andExpect(status().isForbidden());
    }

    @Test
    void r09AdministratorCanReadDegreeCompletionAchievements() throws Exception {
        CurrentUser administrator = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
        when(service.list(any(), eq(administrator))).thenReturn(new DegreeCompletionDtos.SearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/degree-completion-achievements").requestAttr("currentUser", administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private DegreeCompletionDtos.Row row() {
        return new DegreeCompletionDtos.Row(63L, "2026", 2L, "ORG-1", "B77-DC-001", LocalDate.parse("2026-02-20"), Map.of("fixtureId", "B77-DC-001"), "DRAFTING", null,
                List.of(new DegreeCompletionDtos.Student("MASTER", "석사 지도학생", "교육성과 분석", LocalDate.parse("2026-02-20"))), LocalDateTime.parse("2026-02-20T09:00:00"));
    }
}

package kr.ac.knue.commonfoundation.basic83;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** MockMvc contract tests for the controller responsible for teaching-improvement routes. */
@WebMvcTest(LectureImprovementAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureImprovementAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureImprovementAchievementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of()
    );
    private final CurrentUser r07 = new CurrentUser(
            107L,
            "operator",
            "E0107",
            "담당자",
            List.of("R07"),
            List.of()
    );

    @Test
    void listReturnsApprovedEnvelope() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(
                new LectureImprovementSearchResponse(List.of(row()), 0, 20, 1)
        );

        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-LIA-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievements[0].semester").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-LIA-LIST"));
    }

    @Test
    void createReturnsPersistedAcademicYearAndSemester() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-LIA-CREATE"))).thenReturn(
                new LectureImprovementSaveResult(row(), false, null)
        );

        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-LIA-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementContent").value("수업 개선안"))
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(1));

        verify(service).create(any(), eq(r01), eq("REQ-LIA-CREATE"));
    }

    @Test
    void missingAcademicYearReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"LECTURE","achievementDate":"2026-04-10",
                                "achievementContent":"수업 개선안","semester":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void r07CannotCreate() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void updateUsesPathId() throws Exception {
        when(service.update(eq(83L), any(), eq(r01), eq("REQ-LIA-UPDATE"))).thenReturn(
                new LectureImprovementSaveResult(row(), false, null)
        );

        mockMvc.perform(put("/api/business/lecture-improvements/83")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-LIA-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementId").value(83));

        verify(service).update(eq(83L), any(), eq(r01), eq("REQ-LIA-UPDATE"));
    }

    @Test
    void packagedOpenApiFixtureIsAvailable() {
        ClassPathResource fixture = new ClassPathResource("contracts/openapi.yaml");
        org.assertj.core.api.Assertions.assertThat(fixture.exists()).isTrue();
    }

    private String body() {
        return """
                {"managementItemCode":"LECTURE","achievementDate":"2026-04-10",
                "achievementContent":"수업 개선안","academicYear":2025,"semester":1}
                """;
    }

    private LectureImprovementAchievementRow row() {
        return new LectureImprovementAchievementRow(
                83L,
                "B83-LIA-001",
                101L,
                "faculty",
                "2026",
                "LECTURE",
                LocalDate.parse("2026-04-10"),
                "수업 개선안",
                2025,
                1,
                null,
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00")
        );
    }
}

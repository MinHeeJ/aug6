package kr.ac.knue.commonfoundation.lectureimprovements;

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

import java.nio.charset.StandardCharsets;
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
import org.springframework.util.StreamUtils;

/** MockMvc contract tests for the controller that owns all lecture-improvement URLs. */
@WebMvcTest(LectureImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureImprovementControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureImprovementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L,
            "operator",
            "E0107",
            "담당자",
            List.of("R07"),
            List.of());

    @Test
    void approvedOpenApiFixtureDeclaresAllLectureImprovementOperations() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/lecture-improvements:")
                .contains("operationId: listLectureImprovements")
                .contains("operationId: createLectureImprovement")
                .contains("operationId: getLectureImprovement")
                .contains("operationId: updateLectureImprovement");
    }

    @Test
    void listLectureImprovementsReturnsTheApprovedEnvelope() throws Exception {
        when(service.list(0, 20, r01)).thenReturn(
                new LectureImprovementSearchResponse(List.of(achievement()), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-B83-LI-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievements[0].semester").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-LI-LIST"));
    }

    @Test
    void getLectureImprovementReturnsTheSelectedDetail() throws Exception {
        when(service.get(91L, r01)).thenReturn(achievement());

        mockMvc.perform(get("/api/business/lecture-improvements/{achievementId}", 91L)
                        .requestAttr("currentUser", r01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(91))
                .andExpect(jsonPath("$.data.achievementContent").value("수업 개선 보고서 작성"));
    }

    @Test
    void createLectureImprovementAcceptsTheRequiredAcademicFields() throws Exception {
        when(service.create(any(), eq(r01))).thenReturn(achievement());

        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("LECTURE_IMPROVEMENT"))
                .andExpect(jsonPath("$.data.academicYear").value(2025));
        verify(service).create(any(), eq(r01));
    }

    @Test
    void createLectureImprovementRejectsMissingSemesterBeforeServiceMutation() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"LECTURE_IMPROVEMENT",
                                  "achievementDate":"2025-05-01",
                                  "achievementContent":"개선"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'semester')]").isNotEmpty());
        verify(service, never()).create(any(), any());
    }

    @Test
    void r07CannotCreateOrUpdateLectureImprovement() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/business/lecture-improvements/{achievementId}", 91L)
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any(), any());
        verify(service, never()).update(any(), any(), any());
    }

    private LectureImprovementAchievement achievement() {
        return new LectureImprovementAchievement(
                91L,
                101L,
                "교원",
                "LI-2025-001",
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-05-01"),
                "수업 개선 보고서 작성",
                2025,
                2,
                "DRAFT",
                "[\"attachment-1\"]",
                LocalDateTime.parse("2025-05-01T09:00:00"),
                LocalDateTime.parse("2025-05-01T09:00:00"));
    }

    private String validRequest() {
        return """
                {
                  "managementItemCode":"LECTURE_IMPROVEMENT",
                  "achievementDate":"2025-05-01",
                  "achievementContent":"수업 개선 보고서 작성",
                  "academicYear":2025,
                  "semester":2,
                  "attachmentIds":["attachment-1"]
                }
                """;
    }
}

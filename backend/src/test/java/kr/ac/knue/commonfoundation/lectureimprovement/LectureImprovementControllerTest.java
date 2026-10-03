package kr.ac.knue.commonfoundation.lectureimprovement;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract tests for the controller that owns lecture-improvement API routes. */
@WebMvcTest(LectureImprovementController.class)
class LectureImprovementControllerTest {
    private static final CurrentUser FACULTY_USER = new CurrentUser(
            101L,
            "faculty-user",
            "E101",
            "교원",
            List.of("R01"),
            List.of());
    private static final CurrentUser READER_USER = new CurrentUser(
            102L,
            "reviewer",
            "E102",
            "검토자",
            List.of("R02"),
            List.of());

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureImprovementService service;

    @BeforeEach
    void loadOpenApiFixtureFromClasspath() throws IOException {
        String contract = new ClassPathResource("contracts/openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertTrue(contract.contains("/api/business/lecture-improvements"));
    }

    @Test
    void listUsesTheApprovedPageSizeAndReturnsThePagedEnvelope() throws Exception {
        when(service.list(any(LectureImprovementSearchCriteria.class), eq(FACULTY_USER)))
                .thenReturn(new LectureImprovementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-improvements")
                        .param("page", "0")
                        .param("pageSize", "20")
                        .requestAttr("currentUser", FACULTY_USER)
                        .header("X-Request-Id", "lecture-list-request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.achievements[0].academicYear", is(2025)))
                .andExpect(jsonPath("$.meta.requestId", is("lecture-list-request")));

        verify(service).list(new LectureImprovementSearchCriteria(0, 20), FACULTY_USER);
    }

    @Test
    void detailAllowsAnAuthorizedReviewer() throws Exception {
        when(service.get(55L, READER_USER)).thenReturn(row());

        mockMvc.perform(get("/api/business/lecture-improvements/{achievementId}", 55L)
                        .requestAttr("currentUser", READER_USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId", is(55)))
                .andExpect(jsonPath("$.data.semester", is(2)));

        verify(service).get(55L, READER_USER);
    }

    @Test
    void createPersistsTheOpenApiPayloadForR01() throws Exception {
        when(service.create(any(LectureImprovementRequest.class), eq(FACULTY_USER), eq("lecture-create-request")))
                .thenReturn(row());

        mockMvc.perform(post("/api/business/lecture-improvements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload())
                        .requestAttr("currentUser", FACULTY_USER)
                        .header("X-Request-Id", "lecture-create-request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.achievementContent", is("수업 피드백을 반영했습니다.")))
                .andExpect(jsonPath("$.data.attachmentIds[0]", is("file-101")));

        verify(service).create(any(LectureImprovementRequest.class), eq(FACULTY_USER), eq("lecture-create-request"));
    }

    @Test
    void updateRejectsMissingAcademicYearBeforeCallingTheService() throws Exception {
        String invalidPayload = """
                {
                  "managementItemCode": "TEACHING_IMPROVEMENT",
                  "achievementDate": "2025-09-01",
                  "achievementContent": "수업 피드백을 반영했습니다.",
                  "semester": 2
                }
                """;

        mockMvc.perform(put("/api/business/lecture-improvements/{achievementId}", 55L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload)
                        .requestAttr("currentUser", FACULTY_USER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void createRejectsAReadOnlyRoleWithForbiddenEnvelope() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload())
                        .requestAttr("currentUser", READER_USER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    private LectureImprovementRow row() {
        return new LectureImprovementRow(
                55L,
                "LI-55",
                FACULTY_USER.userId(),
                "교원",
                "TEACHING_IMPROVEMENT",
                LocalDate.of(2025, 9, 1),
                "수업 피드백을 반영했습니다.",
                2025,
                2,
                "DRAFT",
                List.of("file-101"),
                LocalDateTime.of(2025, 9, 1, 9, 0),
                LocalDateTime.of(2025, 9, 1, 9, 0));
    }

    private String validPayload() {
        return """
                {
                  "managementItemCode": "TEACHING_IMPROVEMENT",
                  "achievementDate": "2025-09-01",
                  "achievementContent": "수업 피드백을 반영했습니다.",
                  "academicYear": 2025,
                  "semester": 2,
                  "attachmentIds": ["file-101"]
                }
                """;
    }
}

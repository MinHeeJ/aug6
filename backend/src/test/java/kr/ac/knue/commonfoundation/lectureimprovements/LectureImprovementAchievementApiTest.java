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
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.Row;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.SaveResult;
import kr.ac.knue.commonfoundation.lectureimprovements.LectureImprovementModels.SearchResponse;
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

/** MockMvc contract coverage for the controller that owns lecture-improvement routes. */
@WebMvcTest(LectureImprovementAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureImprovementAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureImprovementAchievementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L, "excel-operator", "E0107", "엑셀담당자", List.of("R07"), List.of());

    @Test
    void approvedOpenApiFixtureDeclaresLectureImprovementOperations() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/lecture-improvements:")
                .contains("operationId: listLectureImprovements")
                .contains("operationId: createLectureImprovement")
                .contains("operationId: updateLectureImprovement");
    }

    @Test
    void listReturnsThe2025SemesterFixtureInTheApprovedEnvelope() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(new SearchResponse(List.of(row("DRAFT")), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-B83-LI-LIST")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievements[0].semester").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-LI-LIST"));
    }

    @Test
    void listRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/business/lecture-improvements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).list(any(), any());
    }

    @Test
    void listRejectsRoleOutsideTheApprovedReadScopes() throws Exception {
        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r07))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any(), any());
    }

    @Test
    void listRejectsNegativePageAndUnsupportedPageSize() throws Exception {
        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("page"));
        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("pageSize"));
        verify(service, never()).list(any(), any());
    }

    @Test
    void createReturnsThePersistedAcademicYearAndSemester() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-B83-LI-CREATE")))
                .thenReturn(new SaveResult(row("DRAFT"), false, null));

        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-B83-LI-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementContent").value("토론형 수업 개선"))
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(2));
        verify(service).create(any(), eq(r01), eq("REQ-B83-LI-CREATE"));
    }

    @Test
    void createRejectsAnOutOfRangeSemesterBeforeTheService() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"LECTURE_IMPROVEMENT","achievementDate":"2025-09-01",
                                "achievementContent":"토론형 수업 개선","academicYear":2025,"semester":3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'semester')]").isNotEmpty());
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void r07CannotCreateLectureImprovement() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void updatePreservesTheConflictEnvelopeForConfirmedData() throws Exception {
        when(service.update(eq(83L), any(), eq(r01), eq("REQ-B83-LI-CONFIRMED")))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다."));

        mockMvc.perform(put("/api/business/lecture-improvements/{achievementId}", 83L)
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-B83-LI-CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString(
                        "CONFIRMED_DATA_LOCKED")));
    }

    private String validRequest() {
        return """
                {"managementItemCode":"LECTURE_IMPROVEMENT","achievementDate":"2025-09-01",
                "achievementContent":"토론형 수업 개선","academicYear":2025,"semester":2,
                "attachmentIds":["attachment-opaque-ref"]}
                """;
    }

    private Row row(String achievementStatus) {
        return new Row(
                83L,
                101L,
                "faculty",
                "KNUE-DEPT-COMP",
                "2025",
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-09-01"),
                "2025학년도 2학기 강의개선",
                achievementStatus,
                "[\"attachment-opaque-ref\"]",
                "토론형 수업 개선",
                2025,
                2,
                LocalDateTime.parse("2025-09-01T09:00:00"),
                LocalDateTime.parse("2025-09-01T09:00:00"));
    }
}

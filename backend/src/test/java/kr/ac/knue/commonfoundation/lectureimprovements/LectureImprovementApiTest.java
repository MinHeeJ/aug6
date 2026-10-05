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

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
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

/** MockMvc contract tests for the controller that owns lecture-improvement routes. */
@WebMvcTest(LectureImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureImprovementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureImprovementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L, "professor1", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L, "excel-operator", "E0107", "엑셀담당자", List.of("R07"), List.of());

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
    void listLectureImprovementsReturnsThe2025SemesterFixtureInTheApprovedEnvelope() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(
                new LectureImprovementSearchResponse(List.of(seedRow()), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-LI-LIST")
                        .param("academicYear", "2025")
                        .param("semester", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievements[0].semester").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-LI-LIST"));
    }

    @Test
    void createLectureImprovementReturnsPersistedAcademicYearAndSemester() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-B83-LI-CREATE"))).thenReturn(
                new LectureImprovementSaveResult(seedRow(), false, null));

        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-LI-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementContent").value("강의 개선 실적"))
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(2));
        verify(service).create(any(), eq(r01), eq("REQ-B83-LI-CREATE"));
    }

    @Test
    void getLectureImprovementReturnsTheScopedDetail() throws Exception {
        when(service.get(301L, r01)).thenReturn(seedRow());

        mockMvc.perform(get("/api/business/lecture-improvements/{achievementId}", 301L)
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementNo").value("B83-LI-002"))
                .andExpect(jsonPath("$.data.attachmentIds[0]").value("opaque-file-ref"));
    }

    @Test
    void updateLectureImprovementReturnsConflictForAnEvaluationConfirmedRow() throws Exception {
        when(service.update(eq(301L), any(), eq(r01), eq("REQ-B83-LI-LOCK")))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다."));

        mockMvc.perform(put("/api/business/lecture-improvements/{achievementId}", 301L)
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-LI-LOCK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void createLectureImprovementRejectsAnUnauthorizedRole() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void createLectureImprovementRejectsAnInvalidSemester() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"LECTURE_IMPROVEMENT",
                                  "achievementDate":"2025-12-31",
                                  "achievementContent":"강의 개선 실적",
                                  "academicYear":2025,
                                  "semester":3
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(service, never()).create(any(), any(), any());
    }

    private LectureImprovementRow seedRow() {
        return new LectureImprovementRow(
                301L,
                "B83-LI-002",
                101L,
                "professor1",
                "KNUE-DEPT-COMP",
                "2025",
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-12-31"),
                "DRAFT",
                "강의 개선 실적",
                2025,
                2,
                List.of("opaque-file-ref"),
                LocalDateTime.parse("2025-12-31T09:00:00"),
                LocalDateTime.parse("2025-12-31T09:00:00"));
    }

    private String requestJson() {
        return """
                {
                  "managementItemCode":"LECTURE_IMPROVEMENT",
                  "achievementDate":"2025-12-31",
                  "achievementContent":"강의 개선 실적",
                  "academicYear":2025,
                  "semester":2,
                  "attachmentIds":["opaque-file-ref"]
                }
                """;
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

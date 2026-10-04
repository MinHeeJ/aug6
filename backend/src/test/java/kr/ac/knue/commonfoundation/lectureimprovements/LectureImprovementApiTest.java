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

/** MockMvc contract coverage for the controller that owns the BASIC-83 lecture-improvement routes. */
@WebMvcTest(LectureImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureImprovementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureImprovementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L, "operator", "E0107", "담당자", List.of("R07"), List.of());

    @Test
    void approvedOpenApiFixtureDeclaresAllLectureImprovementOperations() throws Exception {
        String contract = new ClassPathResource("contracts/openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(contract)
                .contains("/api/business/lecture-improvements:")
                .contains("operationId: listLectureImprovements")
                .contains("operationId: createLectureImprovement")
                .contains("operationId: getLectureImprovement")
                .contains("operationId: updateLectureImprovement");
    }

    @Test
    void listReturnsTheScoped2025SemesterFixtureInTheApprovedEnvelope() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(
                new LectureImprovementSearchResponse(List.of(row("개선 전 강의 운영")), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-LI-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievements[0].semester").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-LI-LIST"));
    }

    @Test
    void listRejectsAnUnsupportedPageSizeBeforeCallingTheService() throws Exception {
        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .param("pageSize", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'pageSize')]").isNotEmpty());
        verify(service, never()).list(any(), any());
    }

    @Test
    void createStoresAcademicYearAndSemesterThroughTheOwningService() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-B83-LI-CREATE"))).thenReturn(
                new LectureImprovementSaveResponse(row("수업 피드백 개선"), false, null));

        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-LI-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode": "LECTURE_IMPROVEMENT",
                                  "achievementDate": "2025-09-01",
                                  "achievementContent": "수업 피드백 개선",
                                  "academicYear": 2025,
                                  "semester": 2,
                                  "attachmentIds": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(2));
        verify(service).create(any(), eq(r01), eq("REQ-B83-LI-CREATE"));
    }

    @Test
    void updateUsesPathIdentifierAndReturnsTheReadBackValue() throws Exception {
        when(service.update(eq(83L), any(), eq(r01), eq("REQ-B83-LI-UPDATE"))).thenReturn(
                new LectureImprovementSaveResponse(row("수정된 개선 내용"), true, "평가대상 기간 밖입니다."));

        mockMvc.perform(put("/api/business/lecture-improvements/{achievementId}", 83L)
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-LI-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode": "LECTURE_IMPROVEMENT",
                                  "achievementDate": "2025-09-02",
                                  "achievementContent": "수정된 개선 내용",
                                  "academicYear": 2025,
                                  "semester": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementId").value(83))
                .andExpect(jsonPath("$.data.achievementDateWarning").value(true));
        verify(service).update(eq(83L), any(), eq(r01), eq("REQ-B83-LI-UPDATE"));
    }

    @Test
    void updateReturnsConflictWhenTheServiceDetectsAnEvaluationFinalizationLock() throws Exception {
        when(service.update(eq(83L), any(), eq(r01), eq("REQ-B83-LI-CONFLICT")))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED"));

        mockMvc.perform(put("/api/business/lecture-improvements/{achievementId}", 83L)
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-LI-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode": "LECTURE_IMPROVEMENT",
                                  "achievementDate": "2025-09-02",
                                  "achievementContent": "수정",
                                  "academicYear": 2025,
                                  "semester": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void createRejectsMissingAcademicYearBeforeCallingTheService() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode": "LECTURE_IMPROVEMENT",
                                  "achievementDate": "2025-09-01",
                                  "achievementContent": "개선"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'academicYear')]").isNotEmpty());
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void r07CannotCreateLectureImprovement() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode": "LECTURE_IMPROVEMENT",
                                  "achievementDate": "2025-09-01",
                                  "achievementContent": "개선",
                                  "academicYear": 2025,
                                  "semester": 1
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any(), any());
    }

    private LectureImprovementRow row(String content) {
        return new LectureImprovementRow(
                83L,
                "B83-LI-001",
                101L,
                "faculty",
                "2025",
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-09-01"),
                content,
                2025,
                2,
                List.of(),
                "DRAFT",
                LocalDateTime.parse("2025-09-01T09:00:00"),
                LocalDateTime.parse("2025-09-01T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

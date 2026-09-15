package kr.ac.knue.commonfoundation.basic65;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TeachingEvaluationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TeachingEvaluationAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeachingEvaluationAchievementService service;

    private final CurrentUser faculty = new CurrentUser(1L, "faculty", "E0001", "교원", List.of("R01"), List.of());

    @Test
    void facultyListsOwnTeachingEvaluationAchievementsWithPageContract() throws Exception {
        TeachingEvaluationAchievementRow row = row(41L, "교육성과 분석", "DRAFTING");
        when(service.list(any(), any())).thenReturn(new TeachingEvaluationAchievementSearchResponse(List.of(row), 0, 20, 1));

        mockMvc.perform(get("/api/business/teaching-evaluation-achievements")
                        .requestAttr("currentUser", faculty)
                        .cookie(sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("semester", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teachingEvaluationAchievements[0].achievementId").value(41))
                .andExpect(jsonPath("$.data.teachingEvaluationAchievements[0].courseName").value("교육성과 분석"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));

        verify(service).list(any(), any());
    }

    @Test
    void facultySavesTeachingEvaluationAndReturnsPersistedDynamicFieldsAttachmentAndStatus() throws Exception {
        TeachingEvaluationAchievementRow saved = row(42L, "교육과정 설계", "DRAFTING");
        when(service.save(any(), any(), any())).thenReturn(saved);

        mockMvc.perform(post("/api/business/teaching-evaluation-achievements")
                        .requestAttr("currentUser", faculty)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "teaching-evaluation-save-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "evaluationYear":"2026",
                                  "academicYear":"2026",
                                  "semester":"1",
                                  "courseCode":"EDU-101",
                                  "courseName":"교육과정 설계",
                                  "evaluationScore":91.5,
                                  "managementItemSettingId":11,
                                  "dynamicFields":{"lectureMethod":"토론"},
                                  "attachmentRefs":["attachment-token-1"],
                                  "changeReason":"강의평가 입력"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseName").value("교육과정 설계"))
                .andExpect(jsonPath("$.data.dynamicFields.lectureMethod").value("토론"))
                .andExpect(jsonPath("$.data.attachmentRefs[0]").value("attachment-token-1"))
                .andExpect(jsonPath("$.data.evaluationStatus").value("DRAFTING"))
                .andExpect(jsonPath("$.meta.requestId").value("teaching-evaluation-save-test"));

        verify(service).save(any(), any(), any());
    }

    @Test
    void unauthenticatedTeachingEvaluationListReturnsSafeApiError() throws Exception {
        mockMvc.perform(get("/api/business/teaching-evaluation-achievements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.message").value("인증이 필요합니다."));
    }

    @Test
    void nonFacultyCannotSaveTeachingEvaluationAchievement() throws Exception {
        CurrentUser departmentChair = new CurrentUser(2L, "chair", "E0002", "학과장", List.of("R02"), List.of());

        mockMvc.perform(post("/api/business/teaching-evaluation-achievements")
                        .requestAttr("currentUser", departmentChair)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSaveRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void rejectsMissingRequiredTeachingEvaluationFields() throws Exception {
        mockMvc.perform(post("/api/business/teaching-evaluation-achievements")
                        .requestAttr("currentUser", faculty)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields").isArray());
    }

    @Test
    void duplicateTeachingEvaluationReturnsConflictWithoutSensitiveDetails() throws Exception {
        when(service.save(any(), any(), any())).thenThrow(new ConflictException("동일 강좌의 강의평가 실적이 이미 존재합니다."));

        mockMvc.perform(post("/api/business/teaching-evaluation-achievements")
                        .requestAttr("currentUser", faculty)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSaveRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("동일 강좌의 강의평가 실적이 이미 존재합니다."))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL"))));
    }

    private String validSaveRequest() {
        return """
                {
                  "evaluationYear":"2026",
                  "academicYear":"2026",
                  "semester":"1",
                  "courseCode":"EDU-101",
                  "courseName":"교육과정 설계",
                  "evaluationScore":91.5,
                  "managementItemSettingId":11,
                  "dynamicFields":{"lectureMethod":"토론"},
                  "attachmentRefs":["attachment-token-1"],
                  "changeReason":"강의평가 입력"
                }
                """;
    }

    private TeachingEvaluationAchievementRow row(Long achievementId, String courseName, String status) {
        return new TeachingEvaluationAchievementRow(
                achievementId,
                1L,
                "2026",
                "2026",
                "1",
                "EDU-101",
                courseName,
                new BigDecimal("91.50"),
                status,
                11L,
                "{\"lectureMethod\":\"토론\"}",
                "[\"attachment-token-1\"]",
                LocalDateTime.parse("2026-09-15T09:00:00"),
                1L,
                LocalDateTime.parse("2026-09-15T09:00:00"),
                1L);
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "BASIC65-TEST-SESSION");
    }
}

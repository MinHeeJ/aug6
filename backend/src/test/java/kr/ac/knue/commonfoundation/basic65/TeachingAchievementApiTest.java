package kr.ac.knue.commonfoundation.basic65;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

@WebMvcTest(TeachingAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TeachingAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeachingAchievementService service;

    @Test
    void r01ListsOwnTeachingAchievementsWithSearchAndPageContract() throws Exception {
        when(service.list(any(), any())).thenReturn(new TeachingAchievementSearchResponse(List.of(row(41L, "교육성과 분석")), 0, 20, 1));

        mockMvc.perform(get("/api/business/teaching-achievements")
                        .requestAttr("currentUser", user(1L, "R01"))
                        .param("evaluationYear", "2026")
                        .param("courseKeyword", "성과")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teachingAchievements[0].courseName").value("교육성과 분석"))
                .andExpect(jsonPath("$.data.teachingAchievements[0].creditHours").value(3.0))
                .andExpect(jsonPath("$.data.size").value(20));

        verify(service).list(any(), any());
    }

    @Test
    void r02AndR04CanReadTeachingAchievementsWithinTheirGrantedScope() throws Exception {
        when(service.list(any(), any())).thenReturn(new TeachingAchievementSearchResponse(List.of(row(42L, "교육과정 설계")), 0, 20, 1));

        for (String role : List.of("R02", "R04")) {
            mockMvc.perform(get("/api/business/teaching-achievements").requestAttr("currentUser", user(2L, role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.teachingAchievements[0].achievementId").value(42));
        }
    }

    @Test
    void r01SavesAndUpdatesTeachingAchievementWithPersistedDetailContract() throws Exception {
        when(service.save(any(), any(), any())).thenReturn(row(43L, "교육과정 설계"));

        mockMvc.perform(post("/api/business/teaching-achievements")
                        .requestAttr("currentUser", user(1L, "R01"))
                        .header("X-Request-Id", "teaching-achievement-save-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSaveRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseType").value("MAJOR"))
                .andExpect(jsonPath("$.data.creditHours").value(3.0))
                .andExpect(jsonPath("$.data.dynamicFields.lectureMethod").value("토론"))
                .andExpect(jsonPath("$.data.attachmentRefs[0]").value("attachment-token-1"))
                .andExpect(jsonPath("$.meta.requestId").value("teaching-achievement-save-test"));

        verify(service).save(any(), any(), any());
    }

    @Test
    void rejectsMissingRequiredTeachingAchievementFields() throws Exception {
        mockMvc.perform(post("/api/business/teaching-achievements")
                        .requestAttr("currentUser", user(1L, "R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.fields").isArray());
    }

    @Test
    void r02CannotSaveTeachingAchievement() throws Exception {
        mockMvc.perform(post("/api/business/teaching-achievements")
                        .requestAttr("currentUser", user(2L, "R02"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSaveRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void duplicateTeachingAchievementReturnsConflictWithoutSensitiveDetails() throws Exception {
        when(service.save(any(), any(), any())).thenThrow(new ConflictException("동일 강좌의 강의실적이 이미 존재합니다."));

        mockMvc.perform(post("/api/business/teaching-achievements")
                        .requestAttr("currentUser", user(1L, "R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSaveRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("동일 강좌의 강의실적이 이미 존재합니다."))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL"))));
    }

    private CurrentUser user(Long userId, String role) {
        return new CurrentUser(userId, "faculty", "E0001", "교원", List.of(role), List.of());
    }

    private String validSaveRequest() {
        return """
                {
                  "evaluationYear":"2026",
                  "academicYear":"2026",
                  "semester":"1",
                  "courseCode":"EDU-101",
                  "courseName":"교육과정 설계",
                  "courseType":"MAJOR",
                  "creditHours":3.0,
                  "managementItemSettingId":11,
                  "dynamicFields":{"lectureMethod":"토론"},
                  "attachmentRefs":["attachment-token-1"],
                  "changeReason":"강의실적 입력"
                }
                """;
    }

    private TeachingAchievementRow row(Long achievementId, String courseName) {
        return new TeachingAchievementRow(achievementId, 1L, "2026", "2026", "1", "EDU-101", courseName,
                "MAJOR", new BigDecimal("3.0"), "DRAFTING", 11L, "{\"lectureMethod\":\"토론\"}",
                "[\"attachment-token-1\"]", LocalDateTime.parse("2026-09-15T09:00:00"), 1L,
                LocalDateTime.parse("2026-09-15T09:00:00"), 1L);
    }
}

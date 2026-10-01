package kr.ac.knue.commonfoundation.faculty.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

/**
 * MockMvc integration coverage of the real lecture-evaluation service validation, warning,
 * source persistence, status history, and operational audit path.
 */
@WebMvcTest(EducationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, EducationAchievementService.class})
class LectureEvaluationAchievementIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EducationAchievementMapper mapper;

    private final CurrentUser teacher = new CurrentUser(
            2L,
            "teacher",
            "E0002",
            "교원",
            List.of("R01"),
            List.of()
    );

    @Test
    void saveRejectsMissingRequiredManagementItemWithoutPersisting() throws Exception {
        when(mapper.hasActiveInputPeriod("2026")).thenReturn(1);
        when(mapper.isEvaluationConfirmed(2L, "2026")).thenReturn(0);
        when(mapper.findEvaluationPeriod("2026")).thenReturn(
                new EducationAchievementEvaluationPeriod(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31)
                )
        );

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"occurredDate\":\"2026-03-02\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").exists());

        verify(mapper, never()).insertLectureEvaluationAchievement(
                anyString(),
                anyLong(),
                anyString(),
                any(),
                anyString(),
                anyLong()
        );
    }

    @Test
    void savePersistsOutOfPeriodDateWithWarningAndAuditHistories() throws Exception {
        when(mapper.hasActiveInputPeriod("2027")).thenReturn(1);
        when(mapper.isEvaluationConfirmed(2L, "2027")).thenReturn(0);
        when(mapper.findEvaluationPeriod("2027")).thenReturn(
                new EducationAchievementEvaluationPeriod(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31)
                )
        );
        when(mapper.existsActiveManagementItem("EDU-LECTURE-EVALUATION", "2027")).thenReturn(1);
        when(mapper.insertLectureEvaluationAchievement(
                anyString(),
                anyLong(),
                anyString(),
                any(),
                anyString(),
                anyLong()
        )).thenReturn(savedRow());
        when(mapper.listLectureEvaluationAchievements(any(), eq(2L), eq(true), eq(false)))
                .thenReturn(List.of(savedRow()));
        when(mapper.countLectureEvaluationAchievements(any(), eq(2L), eq(true), eq(false)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .header("X-Request-Id", "REQ-B77-SAVE-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU-LECTURE-EVALUATION\",\"occurredDate\":\"2027-01-01\",\"achievementDetail\":{\"score\":4.5}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievement.managementItemCode").value("EDU-LECTURE-EVALUATION"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-SAVE-001"));

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .param("managementItemCode", "EDU-LECTURE-EVALUATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode")
                        .value("EDU-LECTURE-EVALUATION"))
                .andExpect(jsonPath("$.data.achievements[0].occurredDate").value("2027-01-01"));

        verify(mapper).insertEducationAchievementStatusHistory(any(), anyLong());
        verify(mapper).insertChangeHistory(
                "lecture_evaluation_achievements",
                "810004",
                "CREATE",
                "achievement",
                null,
                "{\"score\":4.5}",
                2L,
                "강의평가 실적 등록",
                "REQ-B77-SAVE-001"
        );
    }

    private LectureEvaluationAchievementRow savedRow() {
        return new LectureEvaluationAchievementRow(
                810004L,
                "810004",
                2L,
                "교원",
                "EDU-LECTURE-EVALUATION",
                LocalDate.of(2027, 1, 1),
                "{\"score\":4.5}",
                "DRAFTING",
                false,
                LocalDateTime.of(2027, 1, 1, 9, 0),
                LocalDateTime.of(2027, 1, 1, 9, 0)
        );
    }
}

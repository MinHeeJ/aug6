package kr.ac.knue.commonfoundation.basic73;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementController;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementMapper;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementRow;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementService;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementStatusHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Defines the HTTP contract for the Phase 2 lecture-evaluation vertical slice before its controller exists.
 */
class EducationAchievementApiContractTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EducationAchievementMapper mapper = org.mockito.Mockito.mock(EducationAchievementMapper.class);
        EducationAchievementRow drafting = new EducationAchievementRow(1L, "LECTURE_EVALUATION", "DRAFTING", "2026", 101L,
                "LECTURE_EVALUATION_SCORE", "4.8", "강의평가", LocalDate.of(2026, 3, 15), "N", LocalDateTime.of(2026, 3, 15, 9, 0), List.of(), List.of());
        org.mockito.Mockito.when(mapper.functionPermissionAllowed(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString())).thenReturn(1);
        org.mockito.Mockito.when(mapper.list(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(drafting));
        org.mockito.Mockito.when(mapper.count(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(1L);
        org.mockito.Mockito.when(mapper.findByIdForUpdate(1L)).thenReturn(drafting);
        org.mockito.Mockito.when(mapper.transitionExists(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList())).thenReturn(1);
        org.mockito.Mockito.when(mapper.findLatestStatusHistory(1L)).thenReturn(new EducationAchievementStatusHistory(1L, "DRAFTING", "SUBMITTED", 101L,
                LocalDateTime.of(2026, 3, 15, 9, 0), "강의평가 제출"));
        mockMvc = MockMvcBuilders.standaloneSetup(new EducationAchievementController(new EducationAchievementService(mapper)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listLectureEvaluationAchievementsReturnsOnlyTheRequestedTypeWithDefaultTwentyRowsForReq1909() throws Exception {
        mockMvc.perform(get("/api/business/education-achievements")
                        .requestAttr("currentUser", faculty())
                        .param("achievementType", "LECTURE_EVALUATION")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].achievementType").value("LECTURE_EVALUATION"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void saveLectureEvaluationRequiresManagementItemCodeForReq1910() throws Exception {
        mockMvc.perform(post("/api/business/education-achievements")
                        .requestAttr("currentUser", faculty())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"achievementType":"LECTURE_EVALUATION","occurrenceDate":"2026-03-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").exists());
    }

    @Test
    void transitionLectureEvaluationFromDraftingToSubmittedReturnsProcessorTimestampAndStatusHistoryForReq1869() throws Exception {
        mockMvc.perform(post("/api/business/education-achievements/1/transition")
                        .requestAttr("currentUser", faculty())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actionType":"SUBMIT","opinion":"강의평가 제출"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.statusHistory.previousStatus").value("DRAFTING"))
                .andExpect(jsonPath("$.data.statusHistory.nextStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.statusHistory.processedBy").isNumber())
                .andExpect(jsonPath("$.data.statusHistory.processedAt").isString())
                .andExpect(jsonPath("$.data.statusHistory.reason").value("강의평가 제출"));
    }

    private CurrentUser faculty() {
        return new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01"), List.of());
    }
}

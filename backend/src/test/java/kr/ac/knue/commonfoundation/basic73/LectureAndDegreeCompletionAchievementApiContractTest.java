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
import kr.ac.knue.commonfoundation.educationachievement.DegreeCompletionStudentDetail;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementController;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementMapper;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementRow;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementSearchCriteria;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Defines the Phase 3 HTTP contract before lecture-achievement and degree-completion behavior is implemented.
 */
class LectureAndDegreeCompletionAchievementApiContractTest {
    private MockMvc mockMvc;
    private EducationAchievementMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = org.mockito.Mockito.mock(EducationAchievementMapper.class);
        org.mockito.Mockito.when(mapper.functionPermissionAllowed(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString())).thenReturn(1);
        org.mockito.Mockito.when(mapper.activeInputPeriodExists(org.mockito.ArgumentMatchers.anyString())).thenReturn(1);
        org.mockito.Mockito.when(mapper.insertAchievement(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(901L);
        org.mockito.Mockito.when(mapper.list(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            EducationAchievementSearchCriteria criteria = invocation.getArgument(0);
            return List.of(row(criteria.normalizedAchievementType(), "N"));
        });
        org.mockito.Mockito.when(mapper.count(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(1L);
        org.mockito.Mockito.when(mapper.findById(901L)).thenReturn(row("LECTURE_ACHIEVEMENT", "N"));
        org.mockito.Mockito.when(mapper.findByIdForUpdate(701L)).thenReturn(row("LECTURE_ACHIEVEMENT", "Y"));
        mockMvc = MockMvcBuilders.standaloneSetup(new EducationAchievementController(new EducationAchievementService(mapper)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listLectureAchievementsReturnsOnlyLectureAchievementRowsForReq1914() throws Exception {
        mockMvc.perform(get("/api/business/education-achievements")
                        .requestAttr("currentUser", faculty())
                        .param("achievementType", "LECTURE_ACHIEVEMENT")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].achievementType").value("LECTURE_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void saveLectureAchievementReturnsTheSavedLectureAchievementForReq1915() throws Exception {
        mockMvc.perform(post("/api/business/education-achievements")
                        .requestAttr("currentUser", faculty())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"achievementType":"LECTURE_ACHIEVEMENT","managementItemCode":"LECTURE-HOURS","occurrenceDate":"2026-03-15"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementType").value("LECTURE_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.managementItemCode").value("LECTURE-HOURS"))
                .andExpect(jsonPath("$.data.occurrenceDate").value("2026-03-15"));
    }

    @Test
    void saveLectureAchievementRejectsAnInactiveInputPeriodWithoutCreatingDataForReq1917() throws Exception {
        org.mockito.Mockito.when(mapper.activeInputPeriodExists("2025")).thenReturn(0);
        mockMvc.perform(post("/api/business/education-achievements")
                        .requestAttr("currentUser", faculty())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"achievementType":"LECTURE_ACHIEVEMENT","managementItemCode":"LECTURE-HOURS","occurrenceDate":"2025-12-31"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
    }

    @Test
    void saveConfirmedLectureAchievementReturnsConfirmedDataLockedWithoutChangingTheRowForReq1917() throws Exception {
        mockMvc.perform(post("/api/business/education-achievements")
                        .requestAttr("currentUser", faculty())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"achievementId":701,"achievementType":"LECTURE_ACHIEVEMENT","managementItemCode":"LECTURE-HOURS","occurrenceDate":"2026-03-15"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
    }

    @Test
    void saveDegreeCompletionPersistsAndReturnsEachStudentDetailForReq1930() throws Exception {
        org.mockito.Mockito.when(mapper.findById(901L)).thenReturn(row("DEGREE_COMPLETION", "N"));
        mockMvc.perform(post("/api/business/education-achievements")
                        .requestAttr("currentUser", faculty())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "achievementType":"DEGREE_COMPLETION",
                                  "managementItemCode":"DEGREE-COMPLETION",
                                  "occurrenceDate":"2026-08-31",
                                  "degreeCompletionStudentDetails":[
                                    {"degreeType":"MASTER","studentName":"김석사","thesisTitle":"교육성과 분석","degreeAwardedOn":"2026-08-31"},
                                    {"degreeType":"DOCTOR","studentName":"이박사","thesisTitle":"교육평가 모형","degreeAwardedOn":"2026-08-31"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementType").value("DEGREE_COMPLETION"))
                .andExpect(jsonPath("$.data.degreeCompletionStudentDetails[0].degreeType").value("MASTER"))
                .andExpect(jsonPath("$.data.degreeCompletionStudentDetails[0].studentName").value("김석사"))
                .andExpect(jsonPath("$.data.degreeCompletionStudentDetails[0].thesisTitle").value("교육성과 분석"))
                .andExpect(jsonPath("$.data.degreeCompletionStudentDetails[0].degreeAwardedOn").value("2026-08-31"))
                .andExpect(jsonPath("$.data.degreeCompletionStudentDetails[1].degreeType").value("DOCTOR"));
    }

    @Test
    void listDegreeCompletionsReturnsThePersistedStudentDetailsForReq1930() throws Exception {
        mockMvc.perform(get("/api/business/education-achievements")
                        .requestAttr("currentUser", faculty())
                        .param("achievementType", "DEGREE_COMPLETION")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].achievementType").value("DEGREE_COMPLETION"))
                .andExpect(jsonPath("$.data.items[0].degreeCompletionStudentDetails[0].degreeType").value("MASTER"))
                .andExpect(jsonPath("$.data.items[0].degreeCompletionStudentDetails[0].studentName").value("김석사"))
                .andExpect(jsonPath("$.data.items[0].degreeCompletionStudentDetails[0].thesisTitle").value("교육성과 분석"))
                .andExpect(jsonPath("$.data.items[0].degreeCompletionStudentDetails[0].degreeAwardedOn").value("2026-08-31"));
    }

    private CurrentUser faculty() {
        return new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01"), List.of());
    }

    private EducationAchievementRow row(String achievementType, String confirmed) {
        List<DegreeCompletionStudentDetail> details = "DEGREE_COMPLETION".equals(achievementType)
                ? List.of(new DegreeCompletionStudentDetail(1L, "MASTER", "김석사", "교육성과 분석", LocalDate.of(2026, 8, 31)),
                        new DegreeCompletionStudentDetail(2L, "DOCTOR", "이박사", "교육평가 모형", LocalDate.of(2026, 8, 31)))
                : List.of();
        return new EducationAchievementRow(confirmed.equals("Y") ? 701L : 901L, achievementType, "DRAFTING", "2026", 101L,
                "DEGREE_COMPLETION".equals(achievementType) ? "DEGREE-COMPLETION" : "LECTURE-HOURS", "", "실적",
                LocalDate.of(2026, "DEGREE_COMPLETION".equals(achievementType) ? 8 : 3, "DEGREE_COMPLETION".equals(achievementType) ? 31 : 15),
                confirmed, LocalDateTime.of(2026, 3, 15, 9, 0), details, List.of());
    }
}

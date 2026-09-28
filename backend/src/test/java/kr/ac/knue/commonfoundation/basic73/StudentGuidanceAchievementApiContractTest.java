package kr.ac.knue.commonfoundation.basic73;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import kr.ac.knue.commonfoundation.educationachievement.StudentGuidanceDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Defines the T011 retrieval contract for a persisted student-guidance achievement before its API support exists.
 */
class StudentGuidanceAchievementApiContractTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EducationAchievementMapper mapper = org.mockito.Mockito.mock(EducationAchievementMapper.class);
        EducationAchievementRow guidance = new EducationAchievementRow(701L, "STUDENT_GUIDANCE", "DRAFTING", "2026", 101L,
                "GUIDANCE", "학생지도", "학생지도", LocalDate.of(2026, 3, 1), "N", LocalDateTime.of(2026, 3, 1, 9, 0), List.of(),
                List.of(new StudentGuidanceDetail(1L, "김학생", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), 1)));
        org.mockito.Mockito.when(mapper.functionPermissionAllowed(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString())).thenReturn(1);
        org.mockito.Mockito.when(mapper.list(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(guidance));
        org.mockito.Mockito.when(mapper.count(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(1L);
        EducationAchievementService service = new EducationAchievementService(mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(new EducationAchievementController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listStudentGuidanceReturnsStudentNameGuidancePeriodAndStudentCountForReq1918() throws Exception {
        CurrentUser faculty = new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01"), List.of());

        mockMvc.perform(get("/api/business/education-achievements")
                        .requestAttr("currentUser", faculty)
                        .param("achievementType", "STUDENT_GUIDANCE")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].achievementType").value("STUDENT_GUIDANCE"))
                .andExpect(jsonPath("$.data.items[0].studentGuidanceDetails[0].studentName").value("김학생"))
                .andExpect(jsonPath("$.data.items[0].studentGuidanceDetails[0].guidanceStartDate").value("2026-03-01"))
                .andExpect(jsonPath("$.data.items[0].studentGuidanceDetails[0].guidanceEndDate").value("2026-06-30"))
                .andExpect(jsonPath("$.data.items[0].studentGuidanceDetails[0].studentCount").value(1));
    }
}

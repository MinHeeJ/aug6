package kr.ac.knue.commonfoundation.basic73;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementController;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementMapper;
import kr.ac.knue.commonfoundation.educationachievement.EducationAchievementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Defines the HTTP contract for an auditable, request-correlated logical deletion before the delete operation exists.
 */
class EducationAchievementDeletionAuditApiContractTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EducationAchievementMapper mapper = org.mockito.Mockito.mock(EducationAchievementMapper.class);
        EducationAchievementService service = new EducationAchievementService(mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(new EducationAchievementController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void logicalDeleteRecordsTheActorTimestampAndRequestIdWithoutPhysicallyRemovingTheAchievementForReq1870() throws Exception {
        CurrentUser faculty = new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01"), List.of());
        String requestId = "basic73-delete-audit-request";

        mockMvc.perform(delete("/api/business/education-achievements/{achievementId}", 701L)
                        .requestAttr("currentUser", faculty)
                        .header("X-Request-Id", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementId").value(701))
                .andExpect(jsonPath("$.data.deletedYn").value("Y"))
                .andExpect(jsonPath("$.data.deletedBy").value(101))
                .andExpect(jsonPath("$.data.deletedAt").isString())
                .andExpect(jsonPath("$.data.changeHistory.changeType").value("DELETE"))
                .andExpect(jsonPath("$.data.changeHistory.beforeValue").isNotEmpty())
                .andExpect(jsonPath("$.data.changeHistory.changedBy").value(101))
                .andExpect(jsonPath("$.data.changeHistory.requestId").value(requestId))
                .andExpect(jsonPath("$.meta.requestId").value(requestId));
    }
}

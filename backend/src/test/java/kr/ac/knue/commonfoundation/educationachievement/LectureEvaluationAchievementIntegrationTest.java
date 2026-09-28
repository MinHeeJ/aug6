package kr.ac.knue.commonfoundation.educationachievement;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP contract for the first FR-025 journey: an authorized faculty member can retrieve a paged
 * lecture-evaluation achievement list before entering its detail or save flow.
 *
 * <p>The missing collection operation currently makes this test fail with 404. The production
 * slice must add only the API behavior necessary to return the established response envelope and
 * the requested pagination metadata.</p>
 */
@WebMvcTest(EducationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureEvaluationAchievementIntegrationTest {
    private static final String COLLECTION_PATH = "/api/business/lecture-evaluation-achievements";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EducationAchievementService educationAchievementService;

    /**
     * Removing the FR-025 collection endpoint or ignoring its allowed page size would leave an
     * authorized faculty user unable to begin the required list-to-detail achievement workflow.
     */
    @Test
    void authorizedFacultyCanRetrieveFirstPageOfLectureEvaluationAchievements() throws Exception {
        CurrentUser facultyMember = new CurrentUser(
                101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
        when(educationAchievementService.countActiveByAchievementType(eq("LECTURE_EVALUATION"))).thenReturn(2);
        when(educationAchievementService.requestId(eq("REQ-B74-LECTURE-EVALUATION-LIST")))
                .thenReturn("REQ-B74-LECTURE-EVALUATION-LIST");

        mockMvc.perform(get(COLLECTION_PATH)
                        .requestAttr("currentUser", facultyMember)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"))
                        .header("X-Request-Id", "REQ-B74-LECTURE-EVALUATION-LIST")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B74-LECTURE-EVALUATION-LIST"));
    }
}

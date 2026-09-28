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
 * HTTP contract for the first FR-028 journey: an authorized faculty member can retrieve a paged
 * graduate-degree achievement list before viewing student details or entering a save flow.
 *
 * <p>The current controller has no graduate-degree collection operation, so this test must fail
 * with 404 until the T016 production slice adds the endpoint and established response envelope.</p>
 */
@WebMvcTest(EducationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GraduateDegreeAchievementIntegrationTest {
    private static final String COLLECTION_PATH = "/api/business/graduate-degree-achievements";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EducationAchievementService educationAchievementService;

    /**
     * Removing the FR-028 collection endpoint or returning a mismatched page envelope would block
     * an authorized faculty member from reaching the required graduate-degree list and details.
     */
    @Test
    void authorizedFacultyCanRetrieveFirstPageOfGraduateDegreeAchievements() throws Exception {
        CurrentUser facultyMember = new CurrentUser(
                101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
        when(educationAchievementService.countActiveByAchievementType(eq("GRADUATE_DEGREE"))).thenReturn(1);
        when(educationAchievementService.requestId(eq("REQ-B74-GRADUATE-DEGREE-LIST")))
                .thenReturn("REQ-B74-GRADUATE-DEGREE-LIST");

        mockMvc.perform(get(COLLECTION_PATH)
                        .requestAttr("currentUser", facultyMember)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"))
                        .header("X-Request-Id", "REQ-B74-GRADUATE-DEGREE-LIST")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B74-GRADUATE-DEGREE-LIST"));
    }
}

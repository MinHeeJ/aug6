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
 * HTTP contract for the first FR-027 journey: an authorized faculty member can retrieve a paged
 * student-guidance achievement list before opening its detail, individual save, or Excel flow.
 *
 * <p>The current controller has no student-guidance collection operation, so this test must fail
 * with 404 until the T012 production slice adds the endpoint and established response envelope.</p>
 */
@WebMvcTest(EducationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentGuidanceAchievementIntegrationTest {
    private static final String COLLECTION_PATH = "/api/business/student-guidance-achievements";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EducationAchievementService educationAchievementService;

    /**
     * Removing the FR-027 collection endpoint or returning a mismatched page envelope would block
     * the authorized list-to-detail student-guidance workflow before Excel bulk registration.
     */
    @Test
    void authorizedFacultyCanRetrieveFirstPageOfStudentGuidanceAchievements() throws Exception {
        CurrentUser facultyMember = new CurrentUser(
                101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
        when(educationAchievementService.countActiveByAchievementType(eq("STUDENT_GUIDANCE"))).thenReturn(1);
        when(educationAchievementService.requestId(eq("REQ-B74-STUDENT-GUIDANCE-LIST")))
                .thenReturn("REQ-B74-STUDENT-GUIDANCE-LIST");

        mockMvc.perform(get(COLLECTION_PATH)
                        .requestAttr("currentUser", facultyMember)
                        .cookie(new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"))
                        .header("X-Request-Id", "REQ-B74-STUDENT-GUIDANCE-LIST")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B74-STUDENT-GUIDANCE-LIST"));
    }
}

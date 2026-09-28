package kr.ac.knue.commonfoundation.basic73;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Specifies the Phase 4 student-guidance save and read-back HTTP contract.
 *
 * <p>The contract protects against accepting a generic achievement while dropping its guidance detail fields.
 * It must remain red until student_guidance_details is persisted and rehydrated through the real MVC boundary.</p>
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentGuidanceAchievementApiContractTest {
    @Autowired MockMvc mockMvc;

    private final CurrentUser facultyMember = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void saveAndListStudentGuidancePreservesStudentPeriodAndCountDetails() throws Exception {
        mockMvc.perform(post("/api/business/education-achievements")
                        .requestAttr("currentUser", facultyMember)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-1918-STUDENT-GUIDANCE-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "achievementType":"STUDENT_GUIDANCE",
                                  "managementItemCode":"STUDENT-GUIDANCE-COUNSELING",
                                  "occurrenceDate":"2026-03-30",
                                  "studentGuidanceDetails":[
                                    {
                                      "guidanceStudentName":"이학생",
                                      "guidanceStartDate":"2026-03-01",
                                      "guidanceEndDate":"2026-03-30",
                                      "studentCount":3
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementType").value("STUDENT_GUIDANCE"))
                .andExpect(jsonPath("$.data.studentGuidanceDetails.length()").value(1))
                .andExpect(jsonPath("$.data.studentGuidanceDetails[0].guidanceStudentName").value("이학생"))
                .andExpect(jsonPath("$.data.studentGuidanceDetails[0].guidanceStartDate").value("2026-03-01"))
                .andExpect(jsonPath("$.data.studentGuidanceDetails[0].guidanceEndDate").value("2026-03-30"))
                .andExpect(jsonPath("$.data.studentGuidanceDetails[0].studentCount").value(3))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-1918-STUDENT-GUIDANCE-SAVE"));

        mockMvc.perform(get("/api/business/education-achievements")
                        .requestAttr("currentUser", facultyMember)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-1918-STUDENT-GUIDANCE-LIST")
                        .param("achievementType", "STUDENT_GUIDANCE")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].achievementType").value("STUDENT_GUIDANCE"))
                .andExpect(jsonPath("$.data.items[0].studentGuidanceDetails[0].guidanceStudentName").value("이학생"))
                .andExpect(jsonPath("$.data.items[0].studentGuidanceDetails[0].guidanceStartDate").value("2026-03-01"))
                .andExpect(jsonPath("$.data.items[0].studentGuidanceDetails[0].guidanceEndDate").value("2026-03-30"))
                .andExpect(jsonPath("$.data.items[0].studentGuidanceDetails[0].studentCount").value(3))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-1918-STUDENT-GUIDANCE-LIST"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

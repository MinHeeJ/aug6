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
 * Defines the Phase 3 HTTP contracts for lecture achievements and degree-completion details.
 *
 * <p>These tests intentionally exercise the real MVC boundary. They must fail until the Phase 3
 * production implementation stores and rehydrates the requested business data.</p>
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureAndDegreeAchievementApiContractTest {
    @Autowired MockMvc mockMvc;

    private final CurrentUser facultyMember = new CurrentUser(
            2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listLectureAchievementsReturnsOnlyLectureAchievementRowsInThePagedEnvelope() throws Exception {
        mockMvc.perform(get("/api/business/education-achievements")
                        .requestAttr("currentUser", facultyMember)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-1914-LECTURE-ACHIEVEMENT-LIST")
                        .param("achievementType", "LECTURE_ACHIEVEMENT")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].achievementType").value("LECTURE_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.items[0].managementItemCode").value("EDU-LECTURE-ACHIEVEMENT-01"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-1914-LECTURE-ACHIEVEMENT-LIST"));
    }

    @Test
    void saveLectureAchievementReturnsThePersistedMasterForAnActiveFacultyInputPeriod() throws Exception {
        mockMvc.perform(post("/api/business/education-achievements")
                        .requestAttr("currentUser", facultyMember)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-1915-LECTURE-ACHIEVEMENT-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"achievementType":"LECTURE_ACHIEVEMENT","managementItemCode":"LECTURE-LOAD-HOURS","occurrenceDate":"2026-03-30"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementType").value("LECTURE_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.managementItemCode").value("LECTURE-LOAD-HOURS"))
                .andExpect(jsonPath("$.data.occurrenceDate").value("2026-03-30"))
                .andExpect(jsonPath("$.data.certificationStatus").value("DRAFTING"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-1915-LECTURE-ACHIEVEMENT-SAVE"));
    }

    @Test
    void savingAConfirmedLectureAchievementIsRejectedWithoutReportingItAsSaved() throws Exception {
        mockMvc.perform(post("/api/business/education-achievements")
                        .requestAttr("currentUser", facultyMember)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"achievementId":4100,"achievementType":"LECTURE_ACHIEVEMENT","managementItemCode":"LECTURE-LOAD-HOURS","occurrenceDate":"2026-03-31"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
    }

    @Test
    void saveDegreeCompletionStoresAndReturnsTheStudentDegreeDetailWithItsMaster() throws Exception {
        mockMvc.perform(post("/api/business/education-achievements")
                        .requestAttr("currentUser", facultyMember)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-1930-DEGREE-COMPLETION-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "achievementType":"DEGREE_COMPLETION",
                                  "managementItemCode":"DEGREE-COMPLETION-THESIS",
                                  "occurrenceDate":"2026-02-28",
                                  "degreeCompletionDetails":[
                                    {
                                      "degreeType":"DOCTOR",
                                      "studentName":"김연구",
                                      "thesisTitle":"교육성과 분석 연구",
                                      "degreeAwardedDate":"2026-02-28"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementType").value("DEGREE_COMPLETION"))
                .andExpect(jsonPath("$.data.degreeCompletionDetails.length()").value(1))
                .andExpect(jsonPath("$.data.degreeCompletionDetails[0].degreeType").value("DOCTOR"))
                .andExpect(jsonPath("$.data.degreeCompletionDetails[0].studentName").value("김연구"))
                .andExpect(jsonPath("$.data.degreeCompletionDetails[0].thesisTitle").value("교육성과 분석 연구"))
                .andExpect(jsonPath("$.data.degreeCompletionDetails[0].degreeAwardedDate").value("2026-02-28"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-1930-DEGREE-COMPLETION-SAVE"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

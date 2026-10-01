package kr.ac.knue.commonfoundation.faculty.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

/** OpenAPI HTTP contract coverage for listLectureEvaluationAchievements and its role boundary. */
@WebMvcTest(EducationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureEvaluationAchievementContractTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EducationAchievementService service;

    private final CurrentUser teacher = new CurrentUser(
            2L,
            "teacher",
            "E0002",
            "교원",
            List.of("R01"),
            List.of()
    );
    private final CurrentUser uploader = new CurrentUser(
            7L,
            "uploader",
            "E0007",
            "업로더",
            List.of("R07"),
            List.of()
    );
    private final CurrentUser administrator = new CurrentUser(
            1L,
            "admin",
            "E0001",
            "시스템 관리자",
            List.of("R09"),
            List.of()
    );

    @Test
    void listLectureEvaluationAchievementsReturnsB77ListFieldsAndPaging() throws Exception {
        when(service.listLectureEvaluationAchievements(any(), eq(teacher))).thenReturn(
                new LectureEvaluationAchievementSearchResponse(List.of(row()), 0, 20, 1)
        );

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .param("managementNo", "810001")
                        .param("managementItemCode", "EDU-LECTURE-EVALUATION")
                        .param("occurredDateFrom", "2026-03-01")
                        .param("occurredDateTo", "2026-03-31")
                        .param("certificationStatus", "DRAFTING")
                        .header("X-Request-Id", "REQ-B77-LIST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("810001"))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode").value("EDU-LECTURE-EVALUATION"))
                .andExpect(jsonPath("$.data.achievements[0].certificationStatus").value("DRAFTING"))
                .andExpect(jsonPath("$.data.achievements[0].hasAttachment").value(false))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-LIST-001"));

        verify(service).listLectureEvaluationAchievements(any(), eq(teacher));
    }

    @Test
    void listLectureEvaluationAchievementsAllowsSystemAdministrator() throws Exception {
        when(service.listLectureEvaluationAchievements(any(), eq(administrator))).thenReturn(
                new LectureEvaluationAchievementSearchResponse(List.of(), 0, 20, 0)
        );

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(service).listLectureEvaluationAchievements(any(), eq(administrator));
    }

    @Test
    void saveLectureEvaluationAchievementRejectsR07BeforeServiceInvocation() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", uploader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU-LECTURE-EVALUATION\",\"occurredDate\":\"2026-03-02\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void listLectureEvaluationAchievementsRejectsUnauthenticatedRequestsBeforeServiceInvocation() throws Exception {
        mockMvc.perform(get("/api/business/lecture-evaluation-achievements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(service);
    }

    @Test
    void saveLectureEvaluationAchievementRejectsUnauthenticatedRequestsBeforeServiceInvocation() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU-LECTURE-EVALUATION\",\"occurredDate\":\"2026-03-02\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(service);
    }

    private LectureEvaluationAchievementRow row() {
        return new LectureEvaluationAchievementRow(
                810001L,
                "810001",
                2L,
                "교원",
                "EDU-LECTURE-EVALUATION",
                LocalDate.of(2026, 3, 2),
                "{\"fixtureId\":\"B77-LE-001\"}",
                "DRAFTING",
                false,
                LocalDateTime.of(2026, 3, 2, 9, 0),
                LocalDateTime.of(2026, 3, 2, 9, 0)
        );
    }
}

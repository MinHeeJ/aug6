package kr.ac.knue.commonfoundation.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LectureEvaluationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureEvaluationAchievementApiTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    LectureEvaluationAchievementService service;

    private final CurrentUser r01 = new CurrentUser(
            1001L,
            "professor1",
            "E1001",
            "교원",
            List.of("R01"),
            List.of()
    );
    private final CurrentUser r07 = new CurrentUser(
            7001L,
            "operator",
            "E7001",
            "운영자",
            List.of("R07"),
            List.of()
    );

    @Test
    void listLectureEvaluationAchievementsReturnsOpenApiContractFieldsForB77Le001() throws Exception {
        try (InputStream contract = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThatContractContainsLectureEvaluationOperation(contract);
        }
        when(service.list(any(), eq(r01))).thenReturn(new LectureEvaluationAchievementModels.SearchResponse(
                List.of(row()),
                0,
                20,
                1
        ));

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .param("managementItemCode", "EDU-LECTURE-EVALUATION-A")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("LE-790001"))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode")
                        .value("EDU-LECTURE-EVALUATION-A"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void saveRequiresManagementItemCodeAndRejectsR07BeforeServiceMutation() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"occurredDate\":\"2026-02-28\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").exists());

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), eq(r07));
    }

    @Test
    void saveReturnsStoredAchievementAndOccurrenceWarning() throws Exception {
        when(service.save(any(), eq(r01))).thenReturn(rowWithWarning());

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(790010))
                .andExpect(jsonPath("$.data.occurredDateOutOfRangeWarning").value(true))
                .andExpect(jsonPath("$.data.managementItemCode")
                        .value("EDU-LECTURE-EVALUATION-A"));
    }

    private void assertThatContractContainsLectureEvaluationOperation(InputStream contract) throws Exception {
        String content = new String(contract.readAllBytes());
        org.assertj.core.api.Assertions.assertThat(content)
                .contains("operationId: listLectureEvaluationAchievements")
                .contains("operationId: saveLectureEvaluationAchievement");
    }

    private String validRequestJson() {
        return "{\"managementItemCode\":\"EDU-LECTURE-EVALUATION-A\","
                + "\"occurredDate\":\"2026-02-28\","
                + "\"achievementDetail\":{\"score\":4.8}}";
    }

    private LectureEvaluationAchievementModels.Row row() {
        return new LectureEvaluationAchievementModels.Row(
                790001L,
                "LE-790001",
                1001L,
                "교원",
                "KNUE-COL-EDU",
                "2026",
                "EDU-LECTURE-EVALUATION-A",
                LocalDate.of(2026, 2, 28),
                "{\"fixtureId\":\"B77-LE-001\"}",
                "DRAFT",
                null,
                false,
                LocalDateTime.parse("2026-09-29T09:00:00")
        );
    }

    private LectureEvaluationAchievementModels.Row rowWithWarning() {
        LectureEvaluationAchievementModels.Row row = row();
        return new LectureEvaluationAchievementModels.Row(
                790010L,
                row.managementNo(),
                row.teacherUserId(),
                row.teacherName(),
                row.organizationCode(),
                row.evaluationYear(),
                row.managementItemCode(),
                row.occurredDate(),
                row.achievementDetail(),
                row.certificationStatus(),
                row.attachmentRef(),
                true,
                row.updatedAt()
        );
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

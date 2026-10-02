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

@WebMvcTest(LectureAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureAchievementApiTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    LectureAchievementService service;

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
    private final CurrentUser r09 = new CurrentUser(
            1L,
            "admin",
            "E0001",
            "시스템 관리자",
            List.of("R09"),
            List.of()
    );

    @Test
    void listLectureAchievementsReturnsOpenApiContractFieldsForB77La001() throws Exception {
        try (InputStream contract = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            assertThatContractContainsLectureAchievementOperation(contract);
        }
        when(service.list(any(), eq(r01))).thenReturn(new LectureAchievementModels.SearchResponse(
                List.of(row()),
                0,
                20,
                1
        ));

        mockMvc.perform(get("/api/business/lecture-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .param("managementItemCode", "EDU-LECTURE-A")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("LA-790101"))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode")
                        .value("EDU-LECTURE-A"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listLectureAchievementsAllowsSystemAdministrator() throws Exception {
        when(service.list(any(), eq(r09))).thenReturn(new LectureAchievementModels.SearchResponse(
                List.of(row()),
                0,
                20,
                1
        ));

        mockMvc.perform(get("/api/business/lecture-achievements")
                        .requestAttr("currentUser", r09)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("LA-790101"));
    }

    @Test
    void saveRequiresManagementItemCodeAndRejectsR07BeforeServiceMutation() throws Exception {
        mockMvc.perform(post("/api/business/lecture-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"occurredDate\":\"2026-02-28\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").exists());

        mockMvc.perform(post("/api/business/lecture-achievements")
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

        mockMvc.perform(post("/api/business/lecture-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(790010))
                .andExpect(jsonPath("$.data.occurredDateOutOfRangeWarning").value(true))
                .andExpect(jsonPath("$.data.managementItemCode")
                        .value("EDU-LECTURE-A"));
    }

    private void assertThatContractContainsLectureAchievementOperation(InputStream contract) throws Exception {
        String content = new String(contract.readAllBytes());
        org.assertj.core.api.Assertions.assertThat(content)
                .contains("operationId: listLectureAchievements")
                .contains("operationId: saveLectureAchievement");
    }

    private String validRequestJson() {
        return "{\"managementItemCode\":\"EDU-LECTURE-A\","
                + "\"occurredDate\":\"2026-02-28\","
                + "\"achievementDetail\":{\"score\":4.8}}";
    }

    private LectureAchievementModels.Row row() {
        return new LectureAchievementModels.Row(
                790101L,
                "LA-790101",
                1001L,
                "교원",
                "KNUE-COL-EDU",
                "2026",
                "EDU-LECTURE-A",
                LocalDate.of(2026, 2, 28),
                "{\"fixtureId\":\"B77-LA-001\"}",
                "DRAFT",
                null,
                false,
                LocalDateTime.parse("2026-09-29T09:00:00")
        );
    }

    private LectureAchievementModels.Row rowWithWarning() {
        LectureAchievementModels.Row row = row();
        return new LectureAchievementModels.Row(
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

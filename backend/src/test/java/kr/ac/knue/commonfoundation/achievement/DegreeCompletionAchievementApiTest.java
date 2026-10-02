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

@WebMvcTest(DegreeCompletionAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DegreeCompletionAchievementApiTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    DegreeCompletionAchievementService service;

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
    void listReturnsContractFixtureAndRecipientSubTable() throws Exception {
        try (InputStream contract = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            org.assertj.core.api.Assertions.assertThat(new String(contract.readAllBytes()))
                    .contains("operationId: listDegreeCompletionAchievements")
                    .contains("operationId: saveDegreeCompletionAchievement");
        }
        when(service.list(any(), eq(r01))).thenReturn(
                new DegreeCompletionAchievementModels.SearchResponse(List.of(row()), 0, 20, 1)
        );

        mockMvc.perform(get("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("DC-790401"))
                .andExpect(jsonPath("$.data.achievements[0].students[0].degreeType").value("MASTER"))
                .andExpect(jsonPath("$.data.achievements[0].students[0].studentName").value("석사 지도학생"));
    }

    @Test
    void saveRejectsMissingDegreeTypeAndR07BeforeServiceMutation() throws Exception {
        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU-DEGREE-A\",\"students\":[{\"studentName\":\"학생\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'degreeType')]").exists());

        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", r07)
                        .cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), eq(r07));
    }

    @Test
    void saveReturnsRequeriedDegreeRecipientDetails() throws Exception {
        when(service.save(any(), eq(r01))).thenReturn(row());

        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(790401))
                .andExpect(jsonPath("$.data.students[0].thesisTitle").value("교육 리더십 연구"))
                .andExpect(jsonPath("$.data.students[0].degreeAwardedDate").value("2026-02-20"));
    }

    private DegreeCompletionAchievementModels.Row row() {
        return new DegreeCompletionAchievementModels.Row(
                790401L,
                "DC-790401",
                1001L,
                "교원",
                "KNUE-COL-EDU",
                "2026",
                "EDU-DEGREE-A",
                LocalDate.of(2026, 2, 20),
                "{}",
                "DRAFT",
                null,
                List.of(new DegreeCompletionAchievementModels.Student(
                        1L,
                        "MASTER",
                        "석사 지도학생",
                        "교육 리더십 연구",
                        LocalDate.of(2026, 2, 20)
                )),
                false,
                LocalDateTime.parse("2026-09-29T09:00:00")
        );
    }

    private String validRequestJson() {
        return "{\"managementItemCode\":\"EDU-DEGREE-A\",\"students\":[{"
                + "\"degreeType\":\"MASTER\",\"studentName\":\"석사 지도학생\","
                + "\"thesisTitle\":\"교육 리더십 연구\",\"degreeAwardedDate\":\"2026-02-20\"}]}";
    }

    private Cookie cookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

package kr.ac.knue.commonfoundation.basic81;

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
import java.nio.charset.StandardCharsets;
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
import org.springframework.util.StreamUtils;

/** MockMvc contract coverage for the controller that owns degree-completion routes. */
@WebMvcTest(DegreeCompletionAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DegreeCompletionAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DegreeCompletionAchievementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L,
            "excel-operator",
            "E0107",
            "엑셀담당자",
            List.of("R07"),
            List.of());

    @Test
    void approvedOpenApiFixtureDeclaresDegreeCompletionOperations() throws Exception {
        String openApi = StreamUtils.copyToString(
                new ClassPathResource("contracts/openapi.yaml").getInputStream(),
                StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/degree-completion-achievements:")
                .contains("operationId: listDegreeCompletionAchievements")
                .contains("operationId: saveDegreeCompletionAchievement");
    }

    @Test
    void listDegreeCompletionAchievementsReturnsB77FixtureAndStudentDetails() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(
                new DegreeCompletionAchievementSearchResponse(List.of(seedRow()), 0, 20, 1));

        mockMvc.perform(get("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B81-DC-LIST")
                        .param("managementNo", "B77-DC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B77-DC-001"))
                .andExpect(jsonPath("$.data.achievements[0].students[0].degreeType").value("MASTER"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B81-DC-LIST"));
    }

    @Test
    void saveDegreeCompletionAchievementReturnsPersistedStudentDetails() throws Exception {
        when(service.save(any(), eq(r01), eq("REQ-B81-DC-SAVE"))).thenReturn(seedRow());

        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B81-DC-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"DEGREE_COMPLETION","students":[{"degreeType":"MASTER","studentName":"홍길동","thesisTitle":"교육과정 연구","degreeAwardedDate":"2026-02-20"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("DEGREE_COMPLETION"))
                .andExpect(jsonPath("$.data.students[0].thesisTitle").value("교육과정 연구"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B81-DC-SAVE"));
        verify(service).save(any(), eq(r01), eq("REQ-B81-DC-SAVE"));
    }

    @Test
    void missingDegreeTypeIsReportedBeforeServiceMutation() throws Exception {
        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"DEGREE_COMPLETION","students":[{"studentName":"홍길동","thesisTitle":"교육과정 연구","degreeAwardedDate":"2026-02-20"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'degreeType')]").isNotEmpty());
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void r07CannotSaveDegreeCompletionAchievement() throws Exception {
        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"DEGREE_COMPLETION","students":[{"degreeType":"MASTER","studentName":"홍길동","thesisTitle":"교육과정 연구","degreeAwardedDate":"2026-02-20"}]}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    private DegreeCompletionAchievementRow seedRow() {
        return new DegreeCompletionAchievementRow(
                101L,
                "B77-DC-001",
                101L,
                "교원",
                "2026",
                "DEGREE_COMPLETION",
                LocalDate.parse("2026-02-20"),
                "학생 수: 1",
                "DRAFT",
                null,
                List.of(new DegreeCompletionStudent(
                        1001L,
                        101L,
                        "MASTER",
                        "홍길동",
                        "교육과정 연구",
                        LocalDate.parse("2026-02-20"))),
                LocalDateTime.parse("2026-02-20T09:00:00"),
                LocalDateTime.parse("2026-02-20T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

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

/** MockMvc contract coverage for the controller that owns the lecture achievement routes. */
@WebMvcTest(LectureAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureAchievementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L, "excel-operator", "E0107", "엑셀담당자", List.of("R07"), List.of());

    @Test
    void approvedOpenApiFixtureDeclaresTheLectureOperations() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/lecture-achievements:")
                .contains("operationId: listLectureAchievements")
                .contains("operationId: saveLectureAchievement");
    }

    @Test
    void listLectureAchievementsReturnsTheB77FixtureInTheApprovedEnvelope() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(
                new LectureAchievementSearchResponse(List.of(seedRow()), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B81-LA-LIST")
                        .param("managementNo", "B77-LA-001")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B77-LA-001"))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode").value("LECTURE"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B81-LA-LIST"));
    }

    @Test
    void saveLectureAchievementReturnsThePersistedValueAndWarning() throws Exception {
        when(service.save(any(), eq(r01), eq("REQ-B81-LA-SAVE"))).thenReturn(
                new LectureAchievementSaveResult(seedRow(), true, "업적발생일이 평가대상 기간 밖입니다."));

        mockMvc.perform(post("/api/business/lecture-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B81-LA-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"LECTURE","occurredDate":"2025-12-31","achievementDetail":{"hours":3}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.managementItemCode").value("LECTURE"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B81-LA-SAVE"));
        verify(service).save(any(), eq(r01), eq("REQ-B81-LA-SAVE"));
    }

    @Test
    void saveLectureAchievementRejectsMissingOccurredDate() throws Exception {
        mockMvc.perform(post("/api/business/lecture-achievements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"managementItemCode\":\"LECTURE\"" + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'occurredDate')]").isNotEmpty());
        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void r07CannotSaveLectureAchievement() throws Exception {
        mockMvc.perform(post("/api/business/lecture-achievements")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"LECTURE","occurredDate":"2026-04-10"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), any(), any());
    }

    private LectureAchievementRow seedRow() {
        return new LectureAchievementRow(
                82L,
                "B77-LA-001",
                101L,
                "faculty",
                "2026",
                "LECTURE",
                LocalDate.parse("2026-04-10"),
                "{\"hours\":3}",
                "DRAFT",
                null,
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

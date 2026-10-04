package kr.ac.knue.commonfoundation.lectureimprovements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

/** MockMvc contract coverage for the controller that owns lecture-improvement routes. */
@WebMvcTest(LectureImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureImprovementControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureImprovementService service;

    private final CurrentUser r01 = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());
    private final CurrentUser r02 = new CurrentUser(
            102L,
            "department-head",
            "E0102",
            "학과장",
            List.of("R02"),
            List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L,
            "excel-operator",
            "E0107",
            "엑셀담당자",
            List.of("R07"),
            List.of());

    @Test
    void approvedOpenApiFixtureDeclaresLectureImprovementOperations() throws Exception {
        String openApi = StreamUtils.copyToString(
                new ClassPathResource("contracts/openapi.yaml").getInputStream(),
                StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/lecture-improvements:")
                .contains("operationId: listLectureImprovements")
                .contains("operationId: createLectureImprovement")
                .contains("operationId: updateLectureImprovement");
    }

    @Test
    void r02CanListButCannotCreateLectureImprovements() throws Exception {
        when(service.list(any(), eq(r02))).thenReturn(null);

        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r02)
                        .cookie(cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(service).list(any(), eq(r02));

        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r02)
                        .cookie(cookie())
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), eq(r02), any());
    }

    @Test
    void r01CreatesAndUpdatesWithRequestIdMetadata() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .header("X-Request-Id", "REQ-B83-LI-CREATE")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-LI-CREATE"));
        verify(service).create(any(), eq(r01), eq("REQ-B83-LI-CREATE"));

        mockMvc.perform(put("/api/business/lecture-improvements/{achievementId}", 11L)
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .header("X-Request-Id", "REQ-B83-LI-UPDATE")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-LI-UPDATE"));
        verify(service).update(eq(11L), any(), eq(r01), eq("REQ-B83-LI-UPDATE"));
    }

    @Test
    void rejectsInvalidSemesterBeforeServiceAndBlocksUnauthorizedDetail() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(cookie())
                        .contentType("application/json")
                        .content("{\"managementItemCode\":\"LECTURE_IMPROVEMENT\","
                                + "\"achievementDate\":\"2026-03-03\",\"achievementContent\":\"개선\","
                                + "\"academicYear\":2026,\"semester\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'semester')]").isNotEmpty());
        verify(service, never()).create(any(), any(), any());

        mockMvc.perform(get("/api/business/lecture-improvements/{achievementId}", 11L)
                        .requestAttr("currentUser", r07)
                        .cookie(cookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).get(any(), eq(r07));
    }

    private String validRequest() {
        return "{\"managementItemCode\":\"LECTURE_IMPROVEMENT\","
                + "\"achievementDate\":\"2026-03-03\",\"achievementContent\":\"강의 개선 실적\","
                + "\"academicYear\":2026,\"semester\":1,"
                + "\"attachmentIds\":[\"opaque-file-ref\"]}";
    }

    private Cookie cookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

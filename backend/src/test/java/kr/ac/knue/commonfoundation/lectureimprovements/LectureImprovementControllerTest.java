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
import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
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

/** MockMvc contract tests for the controller owning every lecture-improvement route. */
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
            "professor1",
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
    void approvedOpenApiFixtureDeclaresAllLectureImprovementOperations() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/lecture-improvements:")
                .contains("operationId: listLectureImprovements")
                .contains("operationId: createLectureImprovement")
                .contains("operationId: getLectureImprovement")
                .contains("operationId: updateLectureImprovement");
    }

    @Test
    void listReturnsCallerScopedRowsInTheApprovedEnvelope() throws Exception {
        when(service.list(0, 20, r01)).thenReturn(
                new LectureImprovementListResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-1900-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievements[0].semester").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-1900-LIST"));
    }

    @Test
    void detailReturnsSavedAcademicYearAndSemester() throws Exception {
        when(service.get(65L, r01)).thenReturn(row());

        mockMvc.perform(get("/api/business/lecture-improvements/65")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementContent").value("강의 품질 개선 활동"))
                .andExpect(jsonPath("$.data.academicYear").value(2025))
                .andExpect(jsonPath("$.data.semester").value(2));
    }

    @Test
    void createPersistsValidAcademicYearAndSemester() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-1901-CREATE"))).thenReturn(row());

        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-1901-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("LECTURE_IMPROVEMENT"))
                .andExpect(jsonPath("$.data.academicYear").value(2025))
                .andExpect(jsonPath("$.data.semester").value(2));
        verify(service).create(any(), eq(r01), eq("REQ-1901-CREATE"));
    }

    @Test
    void updateReturnsConflictWithoutLeakingPersistenceDetailsForConfirmedRecords() throws Exception {
        when(service.update(eq(65L), any(), eq(r01), eq("REQ-1903-CONFLICT")))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다."));

        mockMvc.perform(put("/api/business/lecture-improvements/65")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-1903-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void invalidSemesterIsRejectedBeforeTheServiceIsCalled() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"LECTURE_IMPROVEMENT","achievementDate":"2025-12-31",
                                "achievementContent":"내용","academicYear":2025,"semester":3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'semester')]").isNotEmpty());
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void r07CannotCreateLectureImprovement() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any(), any());
    }

    private LectureImprovementRow row() {
        return new LectureImprovementRow(
                65L,
                "B83-LI-002",
                101L,
                "professor1",
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-12-31"),
                "DRAFT",
                "강의 품질 개선 활동",
                2025,
                2,
                "[]");
    }

    private String requestBody() {
        return """
                {"managementItemCode":"LECTURE_IMPROVEMENT","achievementDate":"2025-12-31",
                "achievementContent":"강의 품질 개선 활동","academicYear":2025,"semester":2,
                "attachmentIds":[]}
                """;
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

package kr.ac.knue.commonfoundation.courseoperations;

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
import java.time.LocalDateTime;
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

/** MockMvc contract tests for the controller that owns the Course Operations routes. */
@WebMvcTest(CourseOperationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseOperationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseOperationService service;

    private final CurrentUser r01 = new CurrentUser(
            101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L, "excel-operator", "E0107", "엑셀담당자", List.of("R07"), List.of());

    @Test
    void approvedOpenApiFixtureDeclaresCourseOperationOperations() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/course-operations:")
                .contains("operationId: listCourseOperations")
                .contains("operationId: createCourseOperation")
                .contains("operationId: getCourseOperation")
                .contains("operationId: updateCourseOperation");
    }

    @Test
    void listCourseOperationsReturnsApprovedEnvelope() throws Exception {
        when(service.list(0, 20, r01)).thenReturn(
                new CourseOperationSearchResponse(List.of(seedResponse()), 0, 20, 1));

        mockMvc.perform(get("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-COURSE-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode").value("COURSE_OPERATION"))
                .andExpect(jsonPath("$.data.achievements[0].performanceDetails").value("현장실습 강좌 운영"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-COURSE-LIST"));
    }

    @Test
    void listCourseOperationsRejectsNegativePageInsteadOfSilentlyClampingIt() throws Exception {
        mockMvc.perform(get("/api/business/course-operations")
                        .param("page", "-1")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'page')]").isNotEmpty());
        verify(service, never()).list(-1, 20, r01);
    }

    @Test
    void getCourseOperationReturnsSelectedDetail() throws Exception {
        when(service.get(83L, r01)).thenReturn(seedResponse());

        mockMvc.perform(get("/api/business/course-operations/{achievementId}", 83L)
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(83))
                .andExpect(jsonPath("$.data.attachmentIds[0]").value("ATTACHMENT-83"));
    }

    @Test
    void createCourseOperationReturnsSavedValue() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-B83-COURSE-CREATE"))).thenReturn(
                new CourseOperationSaveResponse(seedResponse(), false, null));

        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-COURSE-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"COURSE_OPERATION",
                                  "achievementDate":"2026-04-11",
                                  "performanceDetails":"현장실습 강좌 운영",
                                  "attachmentIds":["ATTACHMENT-83"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.performanceDetails").value("현장실습 강좌 운영"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-COURSE-CREATE"));
        verify(service).create(any(), eq(r01), eq("REQ-B83-COURSE-CREATE"));
    }

    @Test
    void createCourseOperationRejectsMissingPerformanceDetails() throws Exception {
        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{"
                                        + "\"managementItemCode\":\"COURSE_OPERATION\","
                                        + "\"achievementDate\":\"2026-04-11\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'performanceDetails')]").isNotEmpty());
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void r07CannotCreateCourseOperation() throws Exception {
        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r07)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"COURSE_OPERATION",
                                  "achievementDate":"2026-04-11",
                                  "performanceDetails":"권한 없는 저장"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void updateCourseOperationPreservesConfirmedRowWhenServiceReportsConflict() throws Exception {
        when(service.update(eq(83L), any(), eq(r01), eq("REQ-B83-COURSE-UPDATE")))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다."));

        mockMvc.perform(put("/api/business/course-operations/{achievementId}", 83L)
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-COURSE-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"COURSE_OPERATION",
                                  "achievementDate":"2026-05-02",
                                  "performanceDetails":"변경 시도"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    private CourseOperationResponse seedResponse() {
        return new CourseOperationResponse(
                83L,
                "faculty",
                "KNUE-DEPT-COMP",
                "2026",
                "COURSE_OPERATION",
                LocalDate.parse("2026-04-11"),
                "현장실습 강좌 운영",
                "현장실습 강좌 운영",
                List.of("ATTACHMENT-83"),
                "DRAFT",
                LocalDateTime.parse("2026-04-11T09:00:00"),
                LocalDateTime.parse("2026-04-11T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

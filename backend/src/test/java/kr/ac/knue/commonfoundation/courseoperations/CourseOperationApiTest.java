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

/** MockMvc contract coverage for the controller owning course-operation routes. */
@WebMvcTest(CourseOperationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseOperationApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseOperationService service;

    private final CurrentUser r01 = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());
    private final CurrentUser r07 = new CurrentUser(
            107L,
            "operator",
            "E0107",
            "운영담당자",
            List.of("R07"),
            List.of());

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
                new CourseOperationSearchResponse(List.of(seedRow()), 0, 20, 1));

        mockMvc.perform(get("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-CO-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].performanceDetails").value("신규 강좌 개설"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-CO-LIST"));
    }

    @Test
    void createCourseOperationReturnsPersistedRow() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-B83-CO-CREATE"))).thenReturn(seedRow());

        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-CO-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"COURSE_OPERATION","achievementDate":"2026-04-10",
                                "performanceDetails":"신규 강좌 개설","attachmentIds":["file-ref-1"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(83))
                .andExpect(jsonPath("$.data.performanceDetails").value("신규 강좌 개설"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-CO-CREATE"));
        verify(service).create(any(), eq(r01), eq("REQ-B83-CO-CREATE"));
    }

    @Test
    void getCourseOperationReturnsSelectedDetail() throws Exception {
        when(service.get(83L, r01)).thenReturn(seedRow());

        mockMvc.perform(get("/api/business/course-operations/{achievementId}", 83L)
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("COURSE_OPERATION"))
                .andExpect(jsonPath("$.data.performanceDetails").value("신규 강좌 개설"));
    }

    @Test
    void updateCourseOperationReturnsUpdatedDetail() throws Exception {
        CourseOperationRow updated = new CourseOperationRow(
                83L,
                101L,
                "faculty",
                "COURSE_OPERATION",
                LocalDate.parse("2026-04-11"),
                "수정된 강좌 운영",
                "DRAFT",
                new String[0],
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-11T09:00:00"));
        when(service.update(eq(83L), any(), eq(r01), eq("REQ-B83-CO-UPDATE"))).thenReturn(updated);

        mockMvc.perform(put("/api/business/course-operations/{achievementId}", 83L)
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B83-CO-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"COURSE_OPERATION","achievementDate":"2026-04-11",
                                "performanceDetails":"수정된 강좌 운영"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceDetails").value("수정된 강좌 운영"));
        verify(service).update(eq(83L), any(), eq(r01), eq("REQ-B83-CO-UPDATE"));
    }

    @Test
    void createCourseOperationRejectsMissingPerformanceDetails() throws Exception {
        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"managementItemCode\":\"COURSE_OPERATION\"," +
                                "\"achievementDate\":\"2026-04-10\"}"))
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
                                {"managementItemCode":"COURSE_OPERATION","achievementDate":"2026-04-10",
                                "performanceDetails":"권한 검증"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any(), any());
    }

    private CourseOperationRow seedRow() {
        return new CourseOperationRow(
                83L,
                101L,
                "faculty",
                "COURSE_OPERATION",
                LocalDate.parse("2026-04-10"),
                "신규 강좌 개설",
                "DRAFT",
                new String[]{"file-ref-1"},
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

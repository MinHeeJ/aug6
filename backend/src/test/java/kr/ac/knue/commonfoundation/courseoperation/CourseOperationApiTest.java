package kr.ac.knue.commonfoundation.courseoperation;

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

import java.nio.charset.StandardCharsets;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

/** MockMvc contract tests for the controller that owns all course-operation endpoints. */
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
            "excel-operator",
            "E0107",
            "엑셀담당자",
            List.of("R07"),
            List.of());

    @Test
    void approvedOpenApiFixtureDeclaresAllCourseOperationMethods() throws Exception {
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
    void listCourseOperationsReturnsTheApprovedEnvelope() throws Exception {
        when(service.list(0, 20, r01)).thenReturn(
                new CourseOperationSearchResponse(List.of(seedResponse()), 0, 20, 1));

        mockMvc.perform(get("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-B83-CO-LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseOperations[0].managementNo").value("B83-CO-001"))
                .andExpect(jsonPath("$.data.courseOperations[0].performanceDetails").value("신규 강좌 운영"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-CO-LIST"));
    }

    @Test
    void getCourseOperationReturnsTheSelectedAchievement() throws Exception {
        when(service.get(83L, r01)).thenReturn(seedResponse());

        mockMvc.perform(get("/api/business/course-operations/{achievementId}", 83L)
                        .requestAttr("currentUser", r01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(83))
                .andExpect(jsonPath("$.data.attachmentIds[0]").value("file-83"));
    }

    @Test
    void createCourseOperationReturnsThePersistedValue() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-B83-CO-CREATE"))).thenReturn(seedResponse());

        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-B83-CO-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"COURSE_OPERATION",
                                  "achievementDate":"2026-04-10",
                                  "performanceDetails":"신규 강좌 운영",
                                  "attachmentIds":["file-83"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementItemCode").value("COURSE_OPERATION"))
                .andExpect(jsonPath("$.data.performanceDetails").value("신규 강좌 운영"));
        verify(service).create(any(), eq(r01), eq("REQ-B83-CO-CREATE"));
    }

    @Test
    void updateCourseOperationReturnsTheChangedValue() throws Exception {
        CourseOperationResponse updated = new CourseOperationResponse(
                83L,
                "B83-CO-001",
                "교원",
                "2026",
                "COURSE_OPERATION",
                LocalDate.parse("2026-04-11"),
                "변경된 강좌 운영",
                List.of(),
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-11T09:00:00"));
        when(service.update(eq(83L), any(), eq(r01), eq("REQ-B83-CO-UPDATE"))).thenReturn(updated);

        mockMvc.perform(put("/api/business/course-operations/{achievementId}", 83L)
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-B83-CO-UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"COURSE_OPERATION",
                                  "achievementDate":"2026-04-11",
                                  "performanceDetails":"변경된 강좌 운영"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.performanceDetails").value("변경된 강좌 운영"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-CO-UPDATE"));
        verify(service).update(eq(83L), any(), eq(r01), eq("REQ-B83-CO-UPDATE"));
    }

    @Test
    void createCourseOperationRejectsMissingPerformanceDetails() throws Exception {
        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"COURSE_OPERATION","achievementDate":"2026-04-10"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'performanceDetails')]").isNotEmpty());
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void r07CannotCreateCourseOperation() throws Exception {
        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"COURSE_OPERATION",
                                  "achievementDate":"2026-04-10",
                                  "performanceDetails":"권한 없는 요청"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any(), any());
    }

    private CourseOperationResponse seedResponse() {
        return new CourseOperationResponse(
                83L,
                "B83-CO-001",
                "교원",
                "2026",
                "COURSE_OPERATION",
                LocalDate.parse("2026-04-10"),
                "신규 강좌 운영",
                List.of("file-83"),
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}

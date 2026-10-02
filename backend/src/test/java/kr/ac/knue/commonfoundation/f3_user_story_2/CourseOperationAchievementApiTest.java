package kr.ac.knue.commonfoundation.f3_user_story_2;

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
import java.util.List;
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

/** MockMvc contract coverage for the controller that owns the course-operation routes. */
@WebMvcTest(CourseOperationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseOperationAchievementApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseOperationAchievementService service;

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
            "담당자",
            List.of("R07"),
            List.of());

    @Test
    void approvedOpenApiFixtureDeclaresCourseOperationRoutes() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        String openApi = StreamUtils.copyToString(contract.getInputStream(), StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(openApi)
                .contains("/api/business/course-operations:")
                .contains("operationId: listCourseOperations")
                .contains("operationId: createCourseOperation")
                .contains("operationId: updateCourseOperation");
    }

    @Test
    void createCourseOperationReturnsThePersistedPerformanceDetails() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-B83-COO-CREATE")))
                .thenReturn(seedResponse());

        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-B83-COO-CREATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"COURSE_OPERATION",
                                  "achievementDate":"2026-03-10",
                                  "performanceDetails":"신규 강좌를 개설했습니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.performanceDetails").value("신규 강좌를 개설했습니다."))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-COO-CREATE"));

        verify(service).create(any(), eq(r01), eq("REQ-B83-COO-CREATE"));
    }

    @Test
    void createCourseOperationRejectsUsersWithoutR01() throws Exception {
        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"COURSE_OPERATION",
                                  "achievementDate":"2026-03-10",
                                  "performanceDetails":"권한 없는 저장"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void updateCourseOperationReturnsConflictWhenTheRecordIsEvaluationConfirmed() throws Exception {
        when(service.update(eq(83L), any(), eq(r01), eq("REQ-B83-COO-CONFLICT")))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다."));

        mockMvc.perform(put("/api/business/course-operations/{achievementId}", 83L)
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-B83-COO-CONFLICT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"COURSE_OPERATION",
                                  "achievementDate":"2026-03-10",
                                  "performanceDetails":"변경 시도"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));

        verify(service).update(eq(83L), any(), eq(r01), eq("REQ-B83-COO-CONFLICT"));
    }

    @Test
    void createCourseOperationRejectsMissingPerformanceDetails() throws Exception {
        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "managementItemCode":"COURSE_OPERATION",
                                  "achievementDate":"2026-03-10"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'performanceDetails')]").isNotEmpty());

        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void listCourseOperationsUsesTheApprovedPageSize() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(new CourseOperationSearchResponse(
                List.of(seedResponse()),
                0,
                20,
                1));

        mockMvc.perform(get("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B83-COO-001"));
    }

    private CourseOperationResponse seedResponse() {
        return new CourseOperationResponse(
                83L,
                "B83-COO-001",
                "faculty",
                "2026",
                "COURSE_OPERATION",
                LocalDate.parse("2026-03-10"),
                "신규 강좌를 개설했습니다.",
                "DRAFT",
                List.of());
    }
}

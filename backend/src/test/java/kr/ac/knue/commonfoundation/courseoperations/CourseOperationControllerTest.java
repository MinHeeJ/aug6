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

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract tests for the Course Operations endpoints owned by this slice. */
@WebMvcTest(CourseOperationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseOperationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseOperationService service;

    @Test
    void listReturnsCallerScopedRowsAtRequestedPageSize() throws Exception {
        when(service.list(eq(0), eq(20), any(CurrentUser.class)))
                .thenReturn(new CourseOperationSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/course-operations")
                        .requestAttr("currentUser", r01User())
                        .header("X-Request-Id", "course-list-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].performanceDetails").value("현장 연계 강좌 운영"))
                .andExpect(jsonPath("$.meta.requestId").value("course-list-1"));
    }

    @Test
    void getReturnsTheSelectedCourseOperationDetail() throws Exception {
        when(service.get(eq(71L), any(CurrentUser.class))).thenReturn(row());

        mockMvc.perform(get("/api/business/course-operations/71")
                        .requestAttr("currentUser", r01User()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(71))
                .andExpect(jsonPath("$.data.managementItemCode").value("COURSE_OPERATION"));
    }

    @Test
    void createReturnsSavedDetailForR01() throws Exception {
        when(service.create(any(CourseOperationRequest.class), eq(r01User())))
                .thenReturn(new CourseOperationSaveResponse(row(), false, null));

        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r01User())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.performanceDetails").value("현장 연계 강좌 운영"));
    }

    @Test
    void updateReturnsConflictWhenFinalizationGuardRejectsTheMutation() throws Exception {
        when(service.update(eq(71L), any(CourseOperationRequest.class), eq(r01User())))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED"));

        mockMvc.perform(put("/api/business/course-operations/71")
                        .requestAttr("currentUser", r01User())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void createRejectsR02BeforeCallingTheWriteService() throws Exception {
        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r02User())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).create(any(), any());
    }

    @Test
    void createReportsMissingPerformanceDetailsAsAFieldError() throws Exception {
        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r01User())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"COURSE_OPERATION\",\"achievementDate\":\"2026-04-11\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field=='performanceDetails')]").exists());

        verify(service, never()).create(any(), any());
    }

    private CurrentUser r01User() {
        return new CurrentUser(101L, "professor1", "E0101", "교원", List.of("R01"), List.of());
    }

    private CurrentUser r02User() {
        return new CurrentUser(102L, "chair", "E0102", "학과장", List.of("R02"), List.of());
    }

    private CourseOperationRow row() {
        return new CourseOperationRow(
                71L,
                "B83-CO-001",
                101L,
                "professor1",
                "2026",
                "COURSE_OPERATION",
                LocalDate.parse("2026-04-11"),
                "현장 연계 강좌 운영",
                "현장 연계 강좌 운영",
                "DRAFT",
                "[]",
                LocalDateTime.parse("2026-04-11T09:00:00"),
                LocalDateTime.parse("2026-04-11T09:00:00"));
    }

    private String validPayload() {
        return "{\"managementItemCode\":\"COURSE_OPERATION\","
                + "\"achievementDate\":\"2026-04-11\","
                + "\"performanceDetails\":\"현장 연계 강좌 운영\","
                + "\"attachmentIds\":[]}";
    }
}

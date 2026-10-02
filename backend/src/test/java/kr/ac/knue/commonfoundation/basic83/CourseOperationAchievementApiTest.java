package kr.ac.knue.commonfoundation.basic83;

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
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** MockMvc contract tests for the controller responsible for course-operation routes. */
@WebMvcTest(CourseOperationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseOperationAchievementApiTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private CourseOperationAchievementService service;
    private final CurrentUser r01 = new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(107L, "operator", "E0107", "담당자", List.of("R07"), List.of());

    @Test void listReturnsApprovedEnvelope() throws Exception {
        when(service.list(any(), eq(r01))).thenReturn(new CourseOperationSearchResponse(List.of(row()), 0, 20, 1));
        mockMvc.perform(get("/api/business/course-operations").requestAttr("currentUser", r01).header("X-Request-Id", "REQ-COA-LIST"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].performanceDetails").value("운영 내역"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-COA-LIST"));
    }
    @Test void createReturnsPersistedDetail() throws Exception {
        when(service.create(any(), eq(r01), eq("REQ-COA-CREATE"))).thenReturn(new CourseOperationSaveResult(row(), false, null));
        mockMvc.perform(post("/api/business/course-operations").requestAttr("currentUser", r01).header("X-Request-Id", "REQ-COA-CREATE").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.performanceDetails").value("운영 내역"));
        verify(service).create(any(), eq(r01), eq("REQ-COA-CREATE"));
    }
    @Test void missingPerformanceDetailsReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/business/course-operations")
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"COURSE\",\"achievementDate\":\"2026-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(service, never()).create(any(), any(), any());
    }
    @Test void r07CannotCreate() throws Exception {
        mockMvc.perform(post("/api/business/course-operations").requestAttr("currentUser", r07).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }
    @Test void updateUsesPathId() throws Exception {
        when(service.update(eq(82L), any(), eq(r01), eq("REQ-COA-UPDATE"))).thenReturn(new CourseOperationSaveResult(row(), false, null));
        mockMvc.perform(put("/api/business/course-operations/82").requestAttr("currentUser", r01).header("X-Request-Id", "REQ-COA-UPDATE").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.achievementId").value(82));
        verify(service).update(eq(82L), any(), eq(r01), eq("REQ-COA-UPDATE"));
    }
    private String body() {
        return "{\"managementItemCode\":\"COURSE\",\"achievementDate\":\"2026-04-10\","
                + "\"performanceDetails\":\"운영 내역\"}";
    }
    private CourseOperationAchievementRow row() { return new CourseOperationAchievementRow(82L, "B83-COA-001", 101L, "faculty", "2026", "COURSE", LocalDate.parse("2026-04-10"), "운영 내역", null, "DRAFT", LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00")); }
}

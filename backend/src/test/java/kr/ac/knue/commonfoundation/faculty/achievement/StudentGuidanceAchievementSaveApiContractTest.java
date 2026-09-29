package kr.ac.knue.commonfoundation.faculty.achievement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Defines the REQ-1917 individual student-guidance save boundary before its controller, service,
 * mapper, and migration-backed fixture are introduced.
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentGuidanceAchievementSaveApiContractTest {
    @Autowired MockMvc mockMvc;

    private final CurrentUser instructor = new CurrentUser(
            2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());

    @Test
    void savingStudentGuidanceKeepsGuidancePeriodAndStudentsVisibleInTheMatchingListRow() throws Exception {
        String requestBody = new ClassPathResource("fixtures/basic77/student-guidance-achievement.json")
                .getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/business/student-guidance-achievements")
                        .requestAttr("currentUser", instructor)
                        .header("X-Request-Id", "REQ-B77-SG-SAVE-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.managementNo").value("B77-SG-001"))
                .andExpect(jsonPath("$.data.guidanceStartDate").value("2026-03-01"))
                .andExpect(jsonPath("$.data.guidanceEndDate").value("2026-06-30"))
                .andExpect(jsonPath("$.data.studentCount").value(2))
                .andExpect(jsonPath("$.data.students[0].studentNo").value("S2026001"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-SG-SAVE-001"));

        mockMvc.perform(get("/api/business/student-guidance-achievements")
                        .requestAttr("currentUser", instructor)
                        .param("managementNo", "B77-SG-001")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].managementNo").value("B77-SG-001"))
                .andExpect(jsonPath("$.data.items[0].guidanceStartDate").value("2026-03-01"))
                .andExpect(jsonPath("$.data.items[0].guidanceEndDate").value("2026-06-30"))
                .andExpect(jsonPath("$.data.items[0].studentCount").value(2))
                .andExpect(jsonPath("$.data.items[0].certificationStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}

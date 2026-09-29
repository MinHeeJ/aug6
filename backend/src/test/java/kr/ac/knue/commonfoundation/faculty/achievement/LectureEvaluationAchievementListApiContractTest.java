package kr.ac.knue.commonfoundation.faculty.achievement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract for the Phase 2 lecture-evaluation list boundary. It fails until the business controller is
 * registered; after the controller exists, this slice must be retargeted to that controller without
 * changing the HTTP assertions.
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureEvaluationAchievementListApiContractTest {
    @Autowired MockMvc mockMvc;

    private final CurrentUser instructor = new CurrentUser(
            2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());

    @Test
    void listLectureEvaluationAchievementsReturnsFilteredB77FixtureWithPagingContract() throws Exception {
        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", instructor)
                        .header("X-Request-Id", "REQ-B77-LE-LIST-001")
                        .param("managementNo", "B77-LE-001")
                        .param("managementItemCode", "EDU_LECTURE_EVALUATION")
                        .param("occurredDateFrom", "2026-03-01")
                        .param("occurredDateTo", "2026-03-31")
                        .param("certificationStatus", "DRAFT")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].managementNo").value("B77-LE-001"))
                .andExpect(jsonPath("$.data.items[0].managementItemCode").value("EDU_LECTURE_EVALUATION"))
                .andExpect(jsonPath("$.data.items[0].occurredDate").value("2026-03-15"))
                .andExpect(jsonPath("$.data.items[0].certificationStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.items[0].attachmentAvailable").value(false))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-LE-LIST-001"));
    }
}

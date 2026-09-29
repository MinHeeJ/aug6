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
 * Defines the REQ-1913 list boundary before the lecture-achievement controller and persistence query exist.
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureAchievementListApiContractTest {
    @Autowired MockMvc mockMvc;

    private final CurrentUser instructor = new CurrentUser(
            2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());

    @Test
    void listLectureAchievementsReturnsTheMatchingB77FixtureWithPagingAndRequestIdentifier() throws Exception {
        mockMvc.perform(get("/api/business/lecture-achievements")
                        .requestAttr("currentUser", instructor)
                        .header("X-Request-Id", "REQ-B77-LA-LIST-001")
                        .param("managementNo", "B77-LA-001")
                        .param("managementItemCode", "EDU_LECTURE")
                        .param("certificationStatus", "DRAFT")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].managementNo").value("B77-LA-001"))
                .andExpect(jsonPath("$.data.items[0].managementItemCode").value("EDU_LECTURE"))
                .andExpect(jsonPath("$.data.items[0].certificationStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-LA-LIST-001"));
    }
}

package kr.ac.knue.commonfoundation.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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

/** HTTP contract tests for the controller that owns the lecture-achievement route. */
@WebMvcTest(LectureAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureAchievementApiTest {
    @Autowired
    MockMvc mvc;

    @MockBean
    LectureAchievementService service;

    private final CurrentUser permitted = new CurrentUser(1L, "professor1", "E1", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(7L, "operator", "E7", "운영자", List.of("R07"), List.of());

    @Test
    void listUsesLectureContractFiltersPaginationAndAuthorizedUser() throws Exception {
        when(service.list(any(), any())).thenReturn(new LectureAchievementResponse.Search(List.of(row()), 0, 20, 1));

        mvc.perform(get("/api/business/lecture-achievements")
                        .requestAttr("currentUser", permitted)
                        .param("managementItemCode", "LECTURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].managementNo").value("B77-LA-001"))
                .andExpect(jsonPath("$.data.items[0].managementItemCode").value("LECTURE"))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    void listAllowsSystemAdministratorForRuntimeReadVerification() throws Exception {
        when(service.list(any(), any())).thenReturn(new LectureAchievementResponse.Search(List.of(row()), 0, 20, 1));

        mvc.perform(get("/api/business/lecture-achievements")
                        .requestAttr("currentUser", new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of())))
                .andExpect(status().isOk());
    }

    @Test
    void saveRejectsUnauthorizedAndInvalidRequestsWithoutServiceSideEffect() throws Exception {
        mvc.perform(post("/api/business/lecture-achievements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"LECTURE\",\"occurredDate\":\"2026-04-10\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mvc.perform(post("/api/business/lecture-achievements")
                        .requestAttr("currentUser", permitted)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"occurredDate\":\"2026-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode"));

        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void saveReturnsPersistedStatusAndAttachmentContract() throws Exception {
        when(service.save(any(), any(), any())).thenReturn(row());

        mvc.perform(post("/api/business/lecture-achievements")
                        .requestAttr("currentUser", permitted)
                        .header("X-Request-Id", "req-81-lecture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"LECTURE\",\"occurredDate\":\"2026-04-10\",\"achievementDetail\":{\"courseName\":\"교육평가론\"},\"attachmentCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(91))
                .andExpect(jsonPath("$.data.certificationStatus").value("DRAFTING"))
                .andExpect(jsonPath("$.data.attachmentCount").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("req-81-lecture"));
    }

    private LectureAchievementResponse.Row row() {
        return new LectureAchievementResponse.Row(91L, "B77-LA-001", "2026", "KNUE-DEPT-COMP", 1L,
                "교원", "LECTURE", LocalDate.parse("2026-04-10"), EducationAchievementStatus.DRAFTING,
                new ObjectMapper().createObjectNode().put("courseName", "교육평가론"), 1, false);
    }
}

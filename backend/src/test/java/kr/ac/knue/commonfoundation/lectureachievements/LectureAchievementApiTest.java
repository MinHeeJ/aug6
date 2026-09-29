package kr.ac.knue.commonfoundation.lectureachievements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LectureAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureAchievementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean LectureAchievementService service;

    @Test
    void listLectureAchievementsReturnsB77LectureFixtureContract() throws Exception {
        when(service.list(any(), eq(0), eq(20), any(CurrentUser.class)))
                .thenReturn(new LectureAchievementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-achievements").requestAttr("currentUser", user()).cookie(cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("LA-78"))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode").value("EDU_LECTURE"))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    void saveRejectsMissingOccurredDateWithFieldError() throws Exception {
        mockMvc.perform(post("/api/business/lecture-achievements").requestAttr("currentUser", user()).cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU_LECTURE\",\"organizationCode\":\"KNUE-COL-EDU\",\"achievementDetail\":\"강의실적\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("occurredDate"));
    }

    @Test
    void saveReturnsPersistedLectureAchievementWithAttachmentReference() throws Exception {
        when(service.save(any(LectureAchievementSaveRequest.class), any(CurrentUser.class))).thenReturn(row());

        mockMvc.perform(post("/api/business/lecture-achievements").requestAttr("currentUser", user()).cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU_LECTURE\",\"occurredDate\":\"2026-03-10\",\"organizationCode\":\"KNUE-COL-EDU\",\"achievementDetail\":\"신규 강의실적\",\"attachmentRef\":\"attachment-77\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(78))
                .andExpect(jsonPath("$.data.attachmentRef").value("attachment-77"));
    }

    private LectureAchievementRow row() {
        return new LectureAchievementRow(78L, "LA-78", "교원", "EDU_LECTURE", "KNUE-COL-EDU",
                LocalDate.parse("2026-03-10"), "B77-LA-001 정상 강의실적", "DRAFTING", "attachment-77",
                LocalDateTime.parse("2026-03-10T09:00:00"), 1L);
    }

    private CurrentUser user() { return new CurrentUser(1L, "faculty", "E0001", "교원", List.of("R01"), List.of()); }
    private Cookie cookie() { return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"); }
}

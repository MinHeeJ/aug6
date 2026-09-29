package kr.ac.knue.commonfoundation.degreecompletions;

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

@WebMvcTest(DegreeCompletionAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, DegreeCompletionAchievementService.class})
class DegreeCompletionAchievementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean DegreeCompletionAchievementMapper mapper;

    @Test
    void listDegreeCompletionAchievementsReturnsB77DegreeFixtureContract() throws Exception {
        when(mapper.list(any(), eq(20), eq(0))).thenReturn(List.of(row()));
        when(mapper.count(any())).thenReturn(1L);
        when(mapper.findStudents(91L)).thenReturn(List.of(student()));

        mockMvc.perform(get("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", user()).cookie(cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("DC-91"))
                .andExpect(jsonPath("$.data.achievements[0].students[0].degreeType").value("DOCTORAL"))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    void saveRejectsStudentMissingDegreeTypeWithFieldError() throws Exception {
        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", user()).cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU_DEGREE_COMPLETION\",\"organizationCode\":\"KNUE-COL-EDU\",\"occurredDate\":\"2026-08-20\",\"students\":[{\"studentName\":\"김교원\",\"thesisTitle\":\"교수학습 연구\",\"degreeAwardedDate\":\"2026-08-20\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("students[0].degreeType"));
    }

    @Test
    void saveReturnsRereadDegreeStudentDetails() throws Exception {
        when(mapper.insert(any(DegreeCompletionAchievementSaveRequest.class), eq("2026"), eq(1L))).thenAnswer(invocation -> {
            invocation.getArgument(0, DegreeCompletionAchievementSaveRequest.class).setAchievementId(91L);
            return 1;
        });
        when(mapper.find(91L)).thenReturn(row());
        when(mapper.findStudents(91L)).thenReturn(List.of(student()));

        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", user()).cookie(cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU_DEGREE_COMPLETION\",\"organizationCode\":\"KNUE-COL-EDU\",\"occurredDate\":\"2026-08-20\",\"achievementDetail\":\"석·박사 배출\",\"students\":[{\"degreeType\":\"DOCTORAL\",\"studentName\":\"김교원\",\"thesisTitle\":\"교수학습 연구\",\"degreeAwardedDate\":\"2026-08-20\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(91))
                .andExpect(jsonPath("$.data.students[0].studentName").value("김교원"))
                .andExpect(jsonPath("$.data.students[0].degreeAwardedDate").value("2026-08-20"));
    }

    private DegreeCompletionStudentRequest student() {
        DegreeCompletionStudentRequest student = new DegreeCompletionStudentRequest();
        student.setDegreeType("DOCTORAL");
        student.setStudentName("김교원");
        student.setThesisTitle("교수학습 연구");
        student.setDegreeAwardedDate(LocalDate.parse("2026-08-20"));
        return student;
    }

    private DegreeCompletionAchievementRow row() {
        return new DegreeCompletionAchievementRow(91L, "DC-91", "교원", "EDU_DEGREE_COMPLETION", "KNUE-COL-EDU",
                LocalDate.parse("2026-08-20"), "B77-DC-001 정상 박사 배출", "DRAFTING", null,
                LocalDateTime.parse("2026-08-20T09:00:00"), 1L);
    }

    private CurrentUser user() { return new CurrentUser(1L, "faculty", "E0001", "교원", List.of("R01"), List.of()); }
    private Cookie cookie() { return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"); }
}

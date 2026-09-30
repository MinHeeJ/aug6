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

/** HTTP contract tests for the controller that owns the degree-completion API route. */
@WebMvcTest(DegreeCompletionAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DegreeCompletionAchievementApiTest {
    @Autowired
    MockMvc mvc;

    @MockBean
    DegreeCompletionAchievementService service;

    private final CurrentUser permitted = new CurrentUser(1L, "professor1", "E1", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(7L, "operator", "E7", "운영자", List.of("R07"), List.of());

    @Test
    void listReturnsSeedShapedHeaderAndGuidedStudentDetailsForFacultyRole() throws Exception {
        when(service.list(any(), any())).thenReturn(new DegreeCompletionAchievementResponse.Search(
                List.of(row()), 0, 20, 1));

        mvc.perform(get("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", permitted)
                        .param("managementNo", "B77-DC-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].managementNo").value("B77-DC-001"))
                .andExpect(jsonPath("$.data.items[0].students[0].degreeType").value("MASTER"))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    void listAllowsSystemAdministratorForRuntimeReadVerification() throws Exception {
        when(service.list(any(), any())).thenReturn(new DegreeCompletionAchievementResponse.Search(List.of(row()), 0, 20, 1));

        mvc.perform(get("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of())))
                .andExpect(status().isOk());
    }

    @Test
    void saveRejectsR07AndMissingGuidedStudentDegreeTypeWithoutServiceSideEffect() throws Exception {
        mvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", permitted)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"DEGREE_COMPLETION\",\"students\":[{\"studentName\":\"김석사\",\"thesisTitle\":\"교육평가 개선 연구\",\"degreeAwardedDate\":\"2026-02-20\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("students[0].degreeType"));

        verify(service, never()).save(any(), any(), any());
    }

    @Test
    void saveReturnsPersistedStudentDetailsAndRequestId() throws Exception {
        when(service.save(any(), any(), any())).thenReturn(row());

        mvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", permitted)
                        .header("X-Request-Id", "req-81-degree-save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(63))
                .andExpect(jsonPath("$.data.students[0].studentName").value("김석사"))
                .andExpect(jsonPath("$.data.students[0].thesisTitle").value("교육평가 개선 연구"))
                .andExpect(jsonPath("$.data.students[0].degreeAwardedDate").value("2026-02-20"))
                .andExpect(jsonPath("$.meta.requestId").value("req-81-degree-save"));
    }

    private String validPayload() {
        return "{\"managementItemCode\":\"DEGREE_COMPLETION\",\"occurredDate\":\"2026-02-20\",\"achievementDetail\":{\"graduationSemester\":\"2026-1\"},\"students\":[{\"degreeType\":\"MASTER\",\"studentName\":\"김석사\",\"thesisTitle\":\"교육평가 개선 연구\",\"degreeAwardedDate\":\"2026-02-20\"}]}";
    }

    private DegreeCompletionAchievementResponse.Row row() {
        return new DegreeCompletionAchievementResponse.Row(63L, "B77-DC-001", "2026", "KNUE-DEPT-COMP", 1L,
                "교원", "DEGREE_COMPLETION", LocalDate.parse("2026-02-20"), EducationAchievementStatus.DRAFTING,
                new ObjectMapper().createObjectNode().put("graduationSemester", "2026-1"), 0, false,
                List.of(new DegreeCompletionStudentRow(11L, "MASTER", "김석사", "교육평가 개선 연구",
                        LocalDate.parse("2026-02-20"))));
    }
}

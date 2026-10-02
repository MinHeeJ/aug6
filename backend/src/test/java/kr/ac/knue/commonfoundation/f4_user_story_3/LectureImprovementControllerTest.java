package kr.ac.knue.commonfoundation.f4_user_story_3;

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
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.Request;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.Row;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.SaveResponse;
import kr.ac.knue.commonfoundation.f4_user_story_3.LectureImprovementModels.SearchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** MockMvc contract coverage for the controller that owns the lecture-improvement URL. */
@WebMvcTest(LectureImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureImprovementControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureImprovementService service;

    private final CurrentUser r01 = new CurrentUser(101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(107L, "operator", "E0107", "담당자", List.of("R07"), List.of());

    @Test
    void savesThe2025SemesterFixture() throws Exception {
        when(service.create(any(Request.class), eq(r01), eq("REQ-B83-LI-SAVE")))
                .thenReturn(new SaveResponse(row("DRAFT"), false, null));

        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r01)
                        .header("X-Request-Id", "REQ-B83-LI-SAVE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"LECTURE_IMPROVEMENT","achievementDate":"2025-03-10",
                                "achievementContent":"수업 피드백 반영","academicYear":2025,"semester":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B83-LI-SAVE"));
        verify(service).create(any(Request.class), eq(r01), eq("REQ-B83-LI-SAVE"));
    }

    @Test
    void rejectsWriteForNonR01User() throws Exception {
        mockMvc.perform(post("/api/business/lecture-improvements")
                        .requestAttr("currentUser", r07)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"LECTURE_IMPROVEMENT","achievementDate":"2025-03-10",
                                "achievementContent":"수업 피드백 반영","academicYear":2025,"semester":1}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void returnsConflictForEvaluationConfirmedUpdate() throws Exception {
        when(service.update(eq(83L), any(Request.class), eq(r01), any()))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다."));

        mockMvc.perform(put("/api/business/lecture-improvements/{achievementId}", 83L)
                        .requestAttr("currentUser", r01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"LECTURE_IMPROVEMENT","achievementDate":"2025-03-10",
                                "achievementContent":"변경","academicYear":2025,"semester":2}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void getDelegatesToScopedServiceForR02() throws Exception {
        CurrentUser r02 = new CurrentUser(102L, "chair", "E0102", "학과장", List.of("R02"), List.of());
        when(service.get(83L, r02)).thenReturn(row("DRAFT"));

        mockMvc.perform(get("/api/business/lecture-improvements/{achievementId}", 83L)
                        .requestAttr("currentUser", r02))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievementId").value(83));
        verify(service).get(83L, r02);
    }

    private Row row(String status) {
        return new Row(
                83L,
                "B83-TIA-001",
                101L,
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-03-10"),
                "수업 피드백 반영",
                2025,
                1,
                status,
                "[]",
                LocalDateTime.parse("2025-03-10T09:00:00"),
                LocalDateTime.parse("2025-03-10T09:00:00"));
    }
}

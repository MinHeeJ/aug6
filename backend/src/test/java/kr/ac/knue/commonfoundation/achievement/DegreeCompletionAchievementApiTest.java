package kr.ac.knue.commonfoundation.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@WebMvcTest(DegreeCompletionAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DegreeCompletionAchievementApiTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    DegreeCompletionAchievementService service;

    private final CurrentUser teacher = new CurrentUser(
            101L,
            "professor1",
            "E1001",
            "홍길동",
            List.of("R01"),
            List.of()
    );
    private final CurrentUser uploadOperator = new CurrentUser(
            701L,
            "operator7",
            "E7001",
            "실적부서",
            List.of("R07"),
            List.of()
    );

    @Test
    void listDegreeCompletionAchievementsReturnsPersistedStudentDetails() throws Exception {
        when(service.list(any(DegreeCompletionAchievementSearchCriteria.class), eq(teacher)))
                .thenReturn(new DegreeCompletionAchievementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", teacher)
                        .param("managementNo", "B77-DC-001")
                        .param("teacherName", "홍길동")
                        .param("certificationStatus", "DRAFT")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B77-DC-001-01"))
                .andExpect(jsonPath("$.data.achievements[0].students[0].degreeType").value("MASTER"))
                .andExpect(jsonPath("$.data.achievements[0].students[0].studentName").value("학생가"))
                .andExpect(jsonPath("$.data.achievements[0].students[0].thesisTitle").value("교원 역량 개발 연구"))
                .andExpect(jsonPath("$.data.achievements[0].students[0].degreeAwardedDate").value("2026-08-20"));
    }

    @Test
    void saveDegreeCompletionAchievementRejectsMissingDegreeTypeAndR07() throws Exception {
        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EDU-DEGREE-COMPLETION","occurredDate":"2026-08-20","students":[{"studentName":"학생가","degreeAwardedDate":"2026-08-20"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'students[0].degreeType')]").exists());

        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", uploadOperator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EDU-DEGREE-COMPLETION","occurredDate":"2026-08-20","students":[{"degreeType":"MASTER","studentName":"학생가","degreeAwardedDate":"2026-08-20"}]}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), eq(uploadOperator));
    }

    @Test
    void saveDegreeCompletionAchievementReturnsHeaderAndStudentDetails() throws Exception {
        when(service.save(any(SaveDegreeCompletionAchievementRequest.class), eq(teacher)))
                .thenReturn(row());

        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"managementItemCode":"EDU-DEGREE-COMPLETION","occurredDate":"2026-08-20","students":[{"degreeType":"MASTER","studentName":"학생가","thesisTitle":"교원 역량 개발 연구","degreeAwardedDate":"2026-08-20"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificationStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.students[0].degreeType").value("MASTER"));
    }

    private DegreeCompletionAchievementRow row() {
        return new DegreeCompletionAchievementRow(
                792001L,
                "B77-DC-001-01",
                101L,
                "홍길동",
                "2026",
                "KNUE-DEPT-COMP",
                "EDU-DEGREE-COMPLETION",
                LocalDate.parse("2026-08-20"),
                "{\"program\":\"교육학\"}",
                "DRAFT",
                true,
                List.of(new DegreeCompletionStudentRow(
                        1L,
                        "MASTER",
                        "학생가",
                        "교원 역량 개발 연구",
                        LocalDate.parse("2026-08-20")
                )),
                LocalDateTime.parse("2026-08-20T09:00:00"),
                LocalDateTime.parse("2026-08-20T09:00:00")
        );
    }
}

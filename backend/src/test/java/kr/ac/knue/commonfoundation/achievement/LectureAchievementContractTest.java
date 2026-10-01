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

@WebMvcTest(LectureAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureAchievementContractTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    LectureAchievementService service;

    private final CurrentUser teacher = new CurrentUser(
            101L, "professor1", "E1001", "홍길동", List.of("R01"), List.of()
    );
    private final CurrentUser uploadOperator = new CurrentUser(
            701L, "operator7", "E7001", "실적부서", List.of("R07"), List.of()
    );
    private final CurrentUser administrator = new CurrentUser(
            1L, "admin", "ADMIN", "관리자", List.of("R09"), List.of()
    );

    @Test
    void listLectureAchievementsReturnsContractFieldsAndAppliesFilters() throws Exception {
        when(service.list(any(LectureAchievementSearchCriteria.class), eq(teacher)))
                .thenReturn(new LectureAchievementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-achievements")
                        .requestAttr("currentUser", teacher)
                        .param("managementNo", "B77-LA-001")
                        .param("teacherName", "홍길동")
                        .param("managementItemCode", "EDU-LECTURE")
                        .param("certificationStatus", "DRAFT")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B77-LA-001-01"))
                .andExpect(jsonPath("$.data.achievements[0].teacherName").value("홍길동"))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode").value("EDU-LECTURE"))
                .andExpect(jsonPath("$.data.achievements[0].certificationStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listLectureAchievementsAllowsTheAuthenticatedAdministrator() throws Exception {
        when(service.list(any(LectureAchievementSearchCriteria.class), eq(administrator)))
                .thenReturn(new LectureAchievementSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/business/lecture-achievements")
                        .requestAttr("currentUser", administrator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B77-LA-001-01"));
    }

    @Test
    void saveLectureAchievementValidatesRequiredOccurredDateAndRejectsR07() throws Exception {
        mockMvc.perform(post("/api/business/lecture-achievements")
                        .requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU-LECTURE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'occurredDate')]").exists());

        mockMvc.perform(post("/api/business/lecture-achievements")
                        .requestAttr("currentUser", uploadOperator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU-LECTURE\",\"occurredDate\":\"2026-04-10\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), eq(uploadOperator));
    }

    @Test
    void saveAndTransitionUseLectureAchievementControllerContracts() throws Exception {
        when(service.save(any(SaveLectureAchievementRequest.class), eq(teacher))).thenReturn(row());
        when(service.transition(eq(791001L), any(LectureAchievementTransitionRequest.class), eq(teacher), eq("REQ-B77-LA-10")))
                .thenReturn(new LectureAchievementRow(
                        791001L, "B77-LA-001-01", 101L, "홍길동", "2026", "KNUE-DEPT-COMP",
                        "EDU-LECTURE", LocalDate.parse("2026-04-10"), "{}", "SUBMITTED", true,
                        LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00")
                ));

        mockMvc.perform(post("/api/business/lecture-achievements")
                        .requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU-LECTURE\",\"occurredDate\":\"2026-04-10\",\"attachmentRef\":\"opaque-reference\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementNo").value("B77-LA-001-01"));

        mockMvc.perform(post("/api/business/lecture-achievements/791001/transitions")
                        .requestAttr("currentUser", teacher)
                        .header("X-Request-Id", "REQ-B77-LA-10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"SUBMIT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificationStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-LA-10"));
    }

    private LectureAchievementRow row() {
        return new LectureAchievementRow(
                791001L, "B77-LA-001-01", 101L, "홍길동", "2026", "KNUE-DEPT-COMP",
                "EDU-LECTURE", LocalDate.parse("2026-04-10"), "{\"courseName\":\"교육방법론\"}",
                "DRAFT", true, LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00")
        );
    }
}

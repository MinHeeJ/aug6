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

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LectureEvaluationAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureEvaluationAchievementApiTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    LectureEvaluationAchievementService service;

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
    void listLectureEvaluationAchievementsReturnsContractFieldsAndBoundedPagination() throws Exception {
        when(service.list(any(LectureEvaluationAchievementSearchCriteria.class), eq(teacher)))
                .thenReturn(new LectureEvaluationAchievementSearchResponse(
                        List.of(row(790001L, "B77-LE-001-01", "DRAFT")),
                        0,
                        20,
                        1
                ));

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .param("managementNo", "B77-LE-001")
                        .param("managementItemCode", "EDU-LECTURE-EVALUATION")
                        .param("occurredDateFrom", "2026-04-01")
                        .param("occurredDateTo", "2026-04-30")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievements[0].managementNo").value("B77-LE-001-01"))
                .andExpect(jsonPath("$.data.achievements[0].teacherName").value("홍길동"))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode").value("EDU-LECTURE-EVALUATION"))
                .andExpect(jsonPath("$.data.achievements[0].occurredDate").value("2026-04-10"))
                .andExpect(jsonPath("$.data.achievements[0].certificationStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.achievements[0].attachmentPresent").value(true))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void saveLectureEvaluationAchievementRejectsMissingRequiredFieldsAndR07() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").exists())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'occurredDate')]").exists());

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", uploadOperator)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU-LECTURE-EVALUATION\",\"occurredDate\":\"2026-04-10\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).save(any(), eq(uploadOperator));
    }

    @Test
    void saveLectureEvaluationAchievementReturnsStoredWarningDateRecordAndLockedConflict() throws Exception {
        when(service.save(any(SaveLectureEvaluationAchievementRequest.class), eq(teacher)))
                .thenReturn(row(790010L, "LE-2026-NEW", "DRAFT"));

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU-LECTURE-EVALUATION\",\"occurredDate\":\"2026-02-28\",\"achievementDetail\":{\"lectureName\":\"교육과정\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managementNo").value("LE-2026-NEW"))
                .andExpect(jsonPath("$.data.occurredDate").value("2026-04-10"));

        when(service.save(any(SaveLectureEvaluationAchievementRequest.class), eq(teacher)))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정된 실적은 변경할 수 없습니다."));
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"achievementId\":790003,\"managementItemCode\":\"EDU-LECTURE-EVALUATION\",\"occurredDate\":\"2026-04-20\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void transitionUsesLectureEvaluationControllerAndReturnsUpdatedStatus() throws Exception {
        when(service.transition(eq(790001L), any(), eq(teacher), eq("REQ-B77-LE-10")))
                .thenReturn(row(790001L, "B77-LE-001-01", "SUBMITTED"));

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements/790001/transitions")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-B77-LE-10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"SUBMIT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificationStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-LE-10"));
    }

    private LectureEvaluationAchievementRow row(Long id, String managementNo, String status) {
        return new LectureEvaluationAchievementRow(
                id,
                managementNo,
                101L,
                "홍길동",
                "2026",
                "KNUE-DEPT-COMP",
                "EDU-LECTURE-EVALUATION",
                LocalDate.parse("2026-04-10"),
                "{\"lectureName\":\"교육방법론\"}",
                status,
                true,
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00")
        );
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

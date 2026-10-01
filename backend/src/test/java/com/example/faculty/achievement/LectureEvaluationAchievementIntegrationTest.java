package com.example.faculty.achievement;

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
import kr.ac.knue.commonfoundation.CommonFoundationApplication;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the actual BASIC-79 lecture-evaluation controller route and its API envelope contract.
 */
@WebMvcTest(LectureEvaluationAchievementController.class)
@ContextConfiguration(classes = CommonFoundationApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LectureEvaluationAchievementIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureEvaluationAchievementService service;

    private final CurrentUser teacher = new CurrentUser(
            2L,
            "professor1",
            "E0002",
            "교원",
            List.of("R01"),
            List.of()
    );
    private final CurrentUser excelOperator = new CurrentUser(
            7L,
            "excel-operator",
            "E0007",
            "엑셀담당",
            List.of("R07"),
            List.of()
    );

    @Test
    void listLectureEvaluationAchievementsReturnsB77FixtureAndAllowedPagination() throws Exception {
        LectureEvaluationAchievementRow row = row("B77-LE-001", "DRAFTING");
        when(service.list(any(), eq(teacher))).thenReturn(
                new LectureEvaluationAchievementSearchResponse(List.of(row), 0, 20, 1)
        );

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .param("managementNo", "B77-LE-001")
                        .param("managementItemCode", "LECTURE_EVALUATION_STANDARD")
                        .param("page", "0")
                        .param("pageSize", "20")
                        .header("X-Request-Id", "REQ-B77-LIST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rows[0].managementNo").value("B77-LE-001"))
                .andExpect(jsonPath("$.data.rows[0].managementItemCode").value("LECTURE_EVALUATION_STANDARD"))
                .andExpect(jsonPath("$.data.rows[0].certificationStatus").value("DRAFTING"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-LIST-001"));
    }

    @Test
    void lectureEvaluationAchievementOperationsRequireAnAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/business/lecture-evaluation-achievements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verify(service, never()).list(any(), any());
        verify(service, never()).save(any(), any());
    }

    @Test
    void saveValidatesRequiredFieldsRejectsR07AndReturnsOutOfPeriodWarningAfterSave() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").isNotEmpty())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'occurredDate')]").isNotEmpty());
        verify(service, never()).save(any(), any());

        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", excelOperator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        when(service.save(any(), eq(teacher))).thenReturn(
                new LectureEvaluationAchievementSaveResult(
                        row("LE-NEW-001", "DRAFTING"),
                        true,
                        "OCCURRED_DATE_OUTSIDE_EVALUATION_PERIOD"
                )
        );
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", teacher)
                        .header("X-Request-Id", "REQ-B77-SAVE-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.managementItemCode").value("LECTURE_EVALUATION_STANDARD"))
                .andExpect(jsonPath("$.data.warning").value(true))
                .andExpect(jsonPath("$.data.warningCode").value("OCCURRED_DATE_OUTSIDE_EVALUATION_PERIOD"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-SAVE-001"));
        verify(service).save(any(), eq(teacher));
    }

    private LectureEvaluationAchievementRow row(String managementNo, String certificationStatus) {
        return new LectureEvaluationAchievementRow(
                101L,
                managementNo,
                2L,
                "교원",
                "2026",
                "KNUE-COL-EDU",
                "LECTURE_EVALUATION_STANDARD",
                LocalDate.of(2025, 12, 31),
                "{\"semester\":\"2025-2\"}",
                certificationStatus,
                null,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 1, 9, 0)
        );
    }

    private String validPayload() {
        return """
                {"managementItemCode":"LECTURE_EVALUATION_STANDARD","occurredDate":"2025-12-31","achievementDetail":{"semester":"2025-2"}}
                """;
    }
}

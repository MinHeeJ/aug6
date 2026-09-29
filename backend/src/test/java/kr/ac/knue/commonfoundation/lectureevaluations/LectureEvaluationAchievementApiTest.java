package kr.ac.knue.commonfoundation.lectureevaluations;

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
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
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
    @Autowired MockMvc mockMvc;
    @MockBean LectureEvaluationAchievementService service;

    @Test
    void listLectureEvaluationAchievementsReturnsB77LectureEvaluationFixtureContract() throws Exception {
        when(service.list(any(), eq(0), eq(20), any(CurrentUser.class))).thenReturn(new LectureEvaluationAchievementSearchResponse(List.of(row()), 0, 20, 1));
        mockMvc.perform(get("/api/business/lecture-evaluation-achievements").requestAttr("currentUser", user()).requestAttr("requestId", "REQ-B77-TRACE-001").cookie(cookie()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievements[0].managementNo").value("LE-77"))
                .andExpect(jsonPath("$.data.achievements[0].managementItemCode").value("EDU_LECTURE_EVALUATION"))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-TRACE-001"));
    }

    @Test
    void directBusinessApiCallFromExcelOnlyRoleIsForbidden() throws Exception {
        when(service.list(any(), eq(0), eq(20), any(CurrentUser.class))).thenThrow(new ForbiddenException());
        CurrentUser excelOnlyUser = new CurrentUser(7L, "excel-user", "E0007", "엑셀 담당자", List.of("R07"), List.of());

        mockMvc.perform(get("/api/business/lecture-evaluation-achievements")
                        .requestAttr("currentUser", excelOnlyUser).cookie(cookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void saveRejectsMissingManagementItemWithFieldError() throws Exception {
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements").requestAttr("currentUser", user()).cookie(cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"occurredDate\":\"2026-03-15\",\"organizationCode\":\"KNUE-COL-EDU\",\"achievementDetail\":\"강의평가\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode"));
    }

    @Test
    void saveReturnsPersistedAchievementForOutsidePeriodWarningEligibleDate() throws Exception {
        when(service.save(any(LectureEvaluationAchievementSaveRequest.class), any(CurrentUser.class))).thenReturn(row());
        mockMvc.perform(post("/api/business/lecture-evaluation-achievements").requestAttr("currentUser", user()).cookie(cookie()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"managementItemCode\":\"EDU_LECTURE_EVALUATION\",\"occurredDate\":\"2026-01-01\",\"organizationCode\":\"KNUE-COL-EDU\",\"achievementDetail\":\"기간 외 강의평가\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementId").value(77));
    }

    private LectureEvaluationAchievementRow row() { return new LectureEvaluationAchievementRow(77L, "LE-77", "교원", "EDU_LECTURE_EVALUATION", "KNUE-COL-EDU", LocalDate.parse("2026-03-15"), "B77-LE-001 정상 강의평가 실적", "DRAFTING", null, LocalDateTime.parse("2026-03-15T09:00:00"), 1L); }
    private CurrentUser user() { return new CurrentUser(1L, "faculty", "E0001", "교원", List.of("R01"), List.of()); }
    private Cookie cookie() { return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION"); }
}

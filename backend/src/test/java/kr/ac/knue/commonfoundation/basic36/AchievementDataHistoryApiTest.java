package kr.ac.knue.commonfoundation.basic36;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AchievementDataHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AchievementDataHistoryApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean AchievementDataHistoryService service;

    private final CurrentUser businessOwner = new CurrentUser(4L, "business-owner", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser auditUser = new CurrentUser(8L, "auditor", "E0008", "감사담당자", List.of("R08"), List.of());
    private final CurrentUser unauthorized = new CurrentUser(5L, "viewer", "E0005", "조회자", List.of("R03"), List.of());

    @Test
    void listAchievementDataHistoriesReturnsBeforeAfterAndActorForReq1282Req1283() throws Exception {
        when(service.listHistories(any(), eq(businessOwner))).thenReturn(new AchievementDataHistorySearchResponse(List.of(history()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/achievement-data-histories")
                        .requestAttr("currentUser", businessOwner)
                        .cookie(sessionCookie())
                        .param("achievementType", "BASIC36_RESEARCHER_PROFILE")
                        .param("achievementKey", "E1001")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.histories[0].achievementType").value("BASIC36_RESEARCHER_PROFILE"))
                .andExpect(jsonPath("$.data.histories[0].beforeValue").value("[]"))
                .andExpect(jsonPath("$.data.histories[0].afterValue").value("[{degreeType=DOCTOR}]"))
                .andExpect(jsonPath("$.data.histories[0].changedByName").value("업무담당자"))
                .andExpect(jsonPath("$.data.histories[0].changedAt").exists());
    }

    @Test
    void listAchievementDataHistoriesRejectsUnauthorizedRoleForReq1282() throws Exception {
        mockMvc.perform(get("/api/admin/achievement-data-histories")
                        .requestAttr("currentUser", unauthorized)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).listHistories(any(), any());
    }

    @Test
    void listAchievementDataHistoriesReturnsValidationErrorWithoutMutationForReq1285() throws Exception {
        when(service.listHistories(any(), eq(auditUser))).thenThrow(new BusinessValidationException("업적데이터 변경이력 검색조건이 올바르지 않습니다.", List.of(new ValidationError("changeType", "CREATE, UPDATE, DELETE 중 하나를 선택하세요."))));

        mockMvc.perform(get("/api/admin/achievement-data-histories")
                        .requestAttr("currentUser", auditUser)
                        .cookie(sessionCookie())
                        .param("changeType", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("changeType"));
    }

    @Test
    void listAchievementDataAsOfRequiresBaseTimestampAndReturnsSnapshotForReq1284Req1287Req1288() throws Exception {
        when(service.listAsOf(any(), eq(auditUser))).thenReturn(new AchievementDataAsOfSearchResponse(List.of(snapshot()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/achievement-data-as-of")
                        .requestAttr("currentUser", auditUser)
                        .cookie(sessionCookie())
                        .param("asOfAt", "2026-09-01T02:00:00")
                        .param("employeeNo", "E1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshots[0].achievementKey").value("E1001"))
                .andExpect(jsonPath("$.data.snapshots[0].achievementTitle").value("연구자 프로필 직접관리 정보"))
                .andExpect(jsonPath("$.data.snapshots[0].snapshotValue").value("DOCTOR:한국교원대학교"))
                .andExpect(jsonPath("$.data.snapshots[0].baseAt").exists());
    }

    @Test
    void listAchievementDataAsOfRejectsMissingBaseTimestampForReq1287() throws Exception {
        when(service.listAsOf(any(), eq(auditUser))).thenThrow(new BusinessValidationException("업적데이터 기준시점 검색조건이 올바르지 않습니다.", List.of(new ValidationError("asOfAt", "기준시점은 필수입니다."))));

        mockMvc.perform(get("/api/admin/achievement-data-as-of")
                        .requestAttr("currentUser", auditUser)
                        .cookie(sessionCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("asOfAt"));
    }

    @Test
    void achievementHistoryEndpointsDoNotExposeCreateUpdateDeleteForReq1292Req1293() throws Exception {
        mockMvc.perform(delete("/api/admin/achievement-data-histories")
                        .requestAttr("currentUser", businessOwner)
                        .cookie(sessionCookie()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/admin/achievement-data-as-of")
                        .requestAttr("currentUser", businessOwner)
                        .cookie(sessionCookie()))
                .andExpect(status().isMethodNotAllowed());
    }

    private AchievementDataHistoryRow history() {
        return new AchievementDataHistoryRow(1L, "BASIC36_RESEARCHER_PROFILE", "E1001", "UPDATE", "degrees", "[]", "[{degreeType=DOCTOR}]", 4L, "business-owner", "업무담당자", LocalDateTime.parse("2026-09-01T02:00:00"), "연구자 프로필 탭 저장");
    }

    private AchievementDataAsOfRow snapshot() {
        return new AchievementDataAsOfRow(1L, "BASIC36_RESEARCHER_PROFILE", "E1001", "E1001", "연구자 프로필 직접관리 정보", "CERTIFIED", "DOCTOR:한국교원대학교", LocalDateTime.parse("2026-09-01T02:00:00"), LocalDateTime.parse("2026-09-01T02:01:00"));
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

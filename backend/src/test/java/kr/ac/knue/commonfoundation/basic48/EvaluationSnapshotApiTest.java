package kr.ac.knue.commonfoundation.basic48;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EvaluationSnapshotController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EvaluationSnapshotApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationSnapshotService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "professor1", "E0002", "교원", List.of("R01"), List.of());
    private final CurrentUser auditor = new CurrentUser(8L, "score-auditor", "E0008", "점수산출감사자", List.of("R08"), List.of());

    @Test
    void listEvaluationSnapshotsReturnsB48SeedRowsAndAppliesR04R08R09AuthorizationForReq1533Req1534() throws Exception {
        when(service.list(new EvaluationSnapshotSearchCriteria(0, 20, "2026", "2026-FINAL-01", EvaluationSnapshotDataScope.ALL, null)))
                .thenReturn(new EvaluationSnapshotSearchResponse(List.of(snapshotRow()), 0, 20, 1));

        mockMvc.perform(get("/api/business/evaluation-snapshots")
                        .requestAttr("currentUser", admin)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("finalizationPoint", "2026-FINAL-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].snapshotId").value("B48-SNAPSHOT-001"))
                .andExpect(jsonPath("$.data.results[0].evaluationYear").value("2026"))
                .andExpect(jsonPath("$.data.results[0].finalizationPoint").value("2026-FINAL-01"))
                .andExpect(jsonPath("$.data.results[0].ruleSnapshotRef").value("B48-RULE-SNAPSHOT-001"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/business/evaluation-snapshots")
                        .requestAttr("currentUser", teacher)
                        .cookie(Basic48ApiTestSupport.sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(new EvaluationSnapshotSearchCriteria(0, 20, null, null, EvaluationSnapshotDataScope.ALL, null));
    }

    @Test
    void getEvaluationSnapshotDetailReturnsRuleMaterialAndPreservedResultSnapshotsForReq1534Req1535Req1536() throws Exception {
        when(service.getDetail("B48-SNAPSHOT-001", EvaluationSnapshotDataScope.ORGANIZATION, "KNUE-COLLEGE-EDU", auditor.userId()))
                .thenReturn(snapshotDetail());

        mockMvc.perform(get("/api/business/evaluation-snapshots/B48-SNAPSHOT-001")
                        .requestAttr("currentUser", auditor)
                        .cookie(Basic48ApiTestSupport.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshotId").value("B48-SNAPSHOT-001"))
                .andExpect(jsonPath("$.data.ruleSnapshotJson").value("{\"ruleSet\":\"B33-CONFIRMED-2026\"}"))
                .andExpect(jsonPath("$.data.materialSnapshotJson").value("{\"materialCount\":3}"))
                .andExpect(jsonPath("$.data.preservedResultJson").value("{\"finalScore\":27.50}"))
                .andExpect(jsonPath("$.data.readOnlyNotice").value("현재 기준정보·평가자료 변경 및 신규 확정 실행은 제공하지 않는 조회 전용 snapshot입니다."));
    }

    @Test
    void downloadEvaluationSnapshotsExcelReturnsFilteredDownloadMetadataAndRequestIdForReq1520Req1536() throws Exception {
        when(service.download(new EvaluationSnapshotSearchCriteria(0, 100, "2026", "2026-FINAL-01", EvaluationSnapshotDataScope.ALL, null), admin.userId()))
                .thenReturn(new EvaluationSnapshotExcelDownload("evaluation-snapshots-2026.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 1,
                        "권한과 검색조건이 적용된 시점 데이터 조회 결과", "REQ-B48-SNAPSHOT-DOWNLOAD-001"));

        mockMvc.perform(get("/api/business/evaluation-snapshots/download")
                        .requestAttr("currentUser", admin)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("evaluationYear", "2026")
                        .param("finalizationPoint", "2026-FINAL-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("evaluation-snapshots-2026.xlsx"))
                .andExpect(jsonPath("$.data.rowCount").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B48-SNAPSHOT-DOWNLOAD-001"));
    }

    private EvaluationSnapshotRow snapshotRow() {
        return new EvaluationSnapshotRow("B48-SNAPSHOT-001", "2026", "2026-FINAL-01", "KNUE-DEPT-COMP", 2L,
                "B48-RULE-SNAPSHOT-001", "B48-MATERIAL-SNAPSHOT-001", "B48-RESULT-SNAPSHOT-001", "PRESERVED",
                LocalDateTime.parse("2026-09-03T09:00:00"), "REQ-B48-SEED-SNAPSHOT-001");
    }

    private EvaluationSnapshotDetail snapshotDetail() {
        return new EvaluationSnapshotDetail("B48-SNAPSHOT-001", "2026", "2026-FINAL-01", "KNUE-DEPT-COMP", 2L,
                "B48-RULE-SNAPSHOT-001", "B48-MATERIAL-SNAPSHOT-001", "B48-RESULT-SNAPSHOT-001", "PRESERVED",
                "{\"ruleSet\":\"B33-CONFIRMED-2026\"}", "{\"materialCount\":3}", "{\"finalScore\":27.50}",
                LocalDateTime.parse("2026-09-03T09:00:00"), "REQ-B48-SEED-SNAPSHOT-001",
                "현재 기준정보·평가자료 변경 및 신규 확정 실행은 제공하지 않는 조회 전용 snapshot입니다.");
    }
}

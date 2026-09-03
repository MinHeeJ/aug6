package kr.ac.knue.commonfoundation.basic48;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
        EvaluationSnapshotController.class,
        ScoreCalculationHistoryController.class,
        ScoreAdjustmentHistoryController.class,
        ScoreRecalculationHistoryController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic48CrossCuttingVerificationTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationSnapshotService snapshotService;
    @MockBean ScoreCalculationHistoryService calculationService;
    @MockBean ScoreAdjustmentHistoryService adjustmentService;
    @MockBean ScoreRecalculationHistoryService recalculationService;

    private static final String BAD_YEAR = "20X6";
    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());

    @Test
    void allFourReadonlyApiGroupsRejectMissingSessionWith401ForReq1514Req1517() throws Exception {
        for (String path : readonlyListPaths()) {
            mockMvc.perform(get(path).cookie(Basic48ApiTestSupport.sessionCookie()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        }
    }

    @Test
    void serverSidePermissionRejectsClientBypassForR01OnSnapshotAdjustmentRecalculationAndOtherTeacherCalcForReq1514Req1515Req1517() throws Exception {
        mockMvc.perform(get("/api/business/evaluation-snapshots")
                        .requestAttr("currentUser", teacher)
                        .cookie(Basic48ApiTestSupport.sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/business/score-adjustment-histories")
                        .requestAttr("currentUser", teacher)
                        .cookie(Basic48ApiTestSupport.sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/business/score-recalculation-histories")
                        .requestAttr("currentUser", teacher)
                        .cookie(Basic48ApiTestSupport.sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/business/score-calculation-histories")
                        .requestAttr("currentUser", teacher)
                        .cookie(Basic48ApiTestSupport.sessionCookie())
                        .param("targetUserId", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void invalidSearchConditionsReturn400FieldErrorsWithoutSensitiveLeakageForReq1515Req1530Req1531() throws Exception {
        when(snapshotService.list(new EvaluationSnapshotSearchCriteria(0, 20, BAD_YEAR, null,
                EvaluationSnapshotDataScope.ALL, null))).thenThrow(validation("evaluationYear"));
        when(calculationService.list(new ScoreCalculationHistorySearchCriteria(0, 20, BAD_YEAR, null, null,
                ScoreCalculationHistoryDataScope.ALL, null, null), admin.userId())).thenThrow(validation("evaluationYear"));
        when(adjustmentService.list(new ScoreAdjustmentHistorySearchCriteria(0, 20, BAD_YEAR, null, null,
                null, ScoreAdjustmentHistoryDataScope.ALL, null), admin.userId())).thenThrow(validation("evaluationYear"));
        when(recalculationService.list(new ScoreRecalculationHistorySearchCriteria(0, 20, BAD_YEAR, null,
                null, null, ScoreRecalculationHistoryDataScope.ALL, null), admin.userId())).thenThrow(validation("evaluationYear"));

        for (String path : readonlyListPaths()) {
            mockMvc.perform(get(path)
                            .requestAttr("currentUser", admin)
                            .cookie(Basic48ApiTestSupport.sessionCookie())
                            .param("evaluationYear", BAD_YEAR))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.fields[0].field").value("evaluationYear"))
                    .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Exception"))))
                    .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))));
        }
    }

    @Test
    void missingDetailsReturn404ApiErrorAcrossFourReadonlyApiGroupsForReq1515() throws Exception {
        when(snapshotService.getDetail("B48-MISSING", EvaluationSnapshotDataScope.ALL, null, admin.userId()))
                .thenThrow(new NotFoundException("시점 데이터 snapshot을 찾을 수 없습니다."));
        when(calculationService.getDetail("B48-MISSING", ScoreCalculationHistoryDataScope.ALL, null, null, admin.userId()))
                .thenThrow(new NotFoundException("점수 산출 이력을 찾을 수 없습니다."));
        when(adjustmentService.getDetail("B48-MISSING", ScoreAdjustmentHistoryDataScope.ALL, null, admin.userId()))
                .thenThrow(new NotFoundException("점수 조정 이력을 찾을 수 없습니다."));
        when(recalculationService.getDetail("B48-MISSING", ScoreRecalculationHistoryDataScope.ALL, null, admin.userId()))
                .thenThrow(new NotFoundException("재계산 이력을 찾을 수 없습니다."));

        for (String path : readonlyDetailPaths("B48-MISSING")) {
            mockMvc.perform(get(path)
                            .requestAttr("currentUser", admin)
                            .cookie(Basic48ApiTestSupport.sessionCookie()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }

    @Test
    void downloadOperationsExposeRequestIdTraceMetadataAcrossAllFourApiGroupsForReq1518Req1522() throws Exception {
        when(snapshotService.download(new EvaluationSnapshotSearchCriteria(0, 100, "2026", null,
                EvaluationSnapshotDataScope.ALL, null), admin.userId()))
                .thenReturn(new EvaluationSnapshotExcelDownload("evaluation-snapshots-2026.xlsx", excelContentType(), 3,
                        "권한과 검색조건이 적용된 시점 데이터 조회 결과", "REQ-B48-SNAPSHOT-DOWNLOAD-CROSS-001"));
        when(calculationService.download(new ScoreCalculationHistorySearchCriteria(0, 100, "2026", null, null,
                ScoreCalculationHistoryDataScope.ALL, null, null), admin.userId()))
                .thenReturn(new ScoreCalculationHistoryExcelDownload("score-calculation-histories-2026.xlsx", excelContentType(), 3,
                        "권한과 검색조건이 적용된 점수 산출 이력 조회 결과", "REQ-B48-CALC-DOWNLOAD-CROSS-001"));
        when(adjustmentService.download(new ScoreAdjustmentHistorySearchCriteria(0, 100, "2026", null, null,
                null, ScoreAdjustmentHistoryDataScope.ALL, null), admin.userId()))
                .thenReturn(new ScoreAdjustmentHistoryExcelDownload("score-adjustment-histories-2026.xlsx", excelContentType(), 3,
                        "권한과 검색조건이 적용된 점수 조정 이력 조회 결과", "REQ-B48-ADJ-DOWNLOAD-CROSS-001"));
        when(recalculationService.download(new ScoreRecalculationHistorySearchCriteria(0, 100, "2026", null,
                null, null, ScoreRecalculationHistoryDataScope.ALL, null), admin.userId()))
                .thenReturn(new ScoreRecalculationHistoryExcelDownload("score-recalculation-histories-2026.xlsx", excelContentType(), 3,
                        "권한과 검색조건이 적용된 재계산 이력 조회 결과", "REQ-B48-RECALC-DOWNLOAD-CROSS-001"));

        for (String path : readonlyDownloadPaths()) {
            mockMvc.perform(get(path)
                            .requestAttr("currentUser", admin)
                            .cookie(Basic48ApiTestSupport.sessionCookie())
                            .param("evaluationYear", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.rowCount").value(3))
                    .andExpect(jsonPath("$.meta.requestId").value(org.hamcrest.Matchers.startsWith("REQ-B48-")));
        }
    }

    @Test
    void mutatingHttpMethodsAreNotExposedForReadonlyHistoryAndSnapshotResourcesForReq1498Req1499Req1549Req1550() throws Exception {
        for (String path : readonlyListPaths()) {
            mockMvc.perform(post(path)
                            .requestAttr("currentUser", admin)
                            .cookie(Basic48ApiTestSupport.sessionCookie()))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
        }
    }

    private BusinessValidationException validation(String field) {
        return new BusinessValidationException("BASIC-48 조회 조건이 올바르지 않습니다.",
                List.of(new ValidationError(field, "평가연도는 4자리 연도여야 합니다.")));
    }

    private String excelContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    private List<String> readonlyListPaths() {
        return List.of(
                "/api/business/evaluation-snapshots",
                "/api/business/score-calculation-histories",
                "/api/business/score-adjustment-histories",
                "/api/business/score-recalculation-histories");
    }

    private List<String> readonlyDownloadPaths() {
        return readonlyListPaths().stream().map(path -> path + "/download").toList();
    }

    private List<String> readonlyDetailPaths(String id) {
        return List.of(
                "/api/business/evaluation-snapshots/" + id,
                "/api/business/score-calculation-histories/" + id,
                "/api/business/score-adjustment-histories/" + id,
                "/api/business/score-recalculation-histories/" + id);
    }
}

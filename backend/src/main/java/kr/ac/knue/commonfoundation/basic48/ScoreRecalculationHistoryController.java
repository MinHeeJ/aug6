package kr.ac.knue.commonfoundation.basic48;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScoreRecalculationHistoryController {
    private final ScoreRecalculationHistoryService service;

    public ScoreRecalculationHistoryController(ScoreRecalculationHistoryService service) {
        this.service = service;
    }

    @GetMapping("/api/business/score-recalculation-histories")
    public ApiResponse<ScoreRecalculationHistorySearchResponse> listScoreRecalculationHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(required = false) String executedFrom,
            @RequestParam(required = false) String executedTo,
            HttpServletRequest servletRequest) {
        ScoreRecalculationActor actor = requireScoreRecalculationViewer(servletRequest);
        return ApiResponse.ok(service.list(new ScoreRecalculationHistorySearchCriteria(page, size, evaluationYear, targetUserId,
                executedFrom, executedTo, actor.dataScope(), actor.organizationCode()), actor.currentUser().userId()));
    }

    @GetMapping("/api/business/score-recalculation-histories/{recalcHistId}")
    public ApiResponse<ScoreRecalculationHistoryDetail> getScoreRecalculationHistoryDetail(
            @PathVariable String recalcHistId,
            HttpServletRequest servletRequest) {
        ScoreRecalculationActor actor = requireScoreRecalculationViewer(servletRequest);
        return ApiResponse.ok(service.getDetail(recalcHistId, actor.dataScope(), actor.organizationCode(), actor.currentUser().userId()));
    }

    @GetMapping("/api/business/score-recalculation-histories/download")
    public ApiResponse<ScoreRecalculationHistoryExcelDownload> downloadScoreRecalculationHistoriesExcel(
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(required = false) String executedFrom,
            @RequestParam(required = false) String executedTo,
            HttpServletRequest servletRequest) {
        ScoreRecalculationActor actor = requireScoreRecalculationViewer(servletRequest);
        ScoreRecalculationHistoryExcelDownload download = service.download(new ScoreRecalculationHistorySearchCriteria(0, 100,
                evaluationYear, targetUserId, executedFrom, executedTo, actor.dataScope(), actor.organizationCode()), actor.currentUser().userId());
        return ApiResponse.ok(download, download.requestId());
    }

    private ScoreRecalculationActor requireScoreRecalculationViewer(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R09")) {
                return new ScoreRecalculationActor(currentUser, ScoreRecalculationHistoryDataScope.ALL, null);
            }
            if (currentUser.roles().contains("R08") || currentUser.roles().contains("R04")) {
                return new ScoreRecalculationActor(currentUser, ScoreRecalculationHistoryDataScope.ORGANIZATION, "KNUE-DEPT-COMP");
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private record ScoreRecalculationActor(CurrentUser currentUser, ScoreRecalculationHistoryDataScope dataScope, String organizationCode) {
    }
}

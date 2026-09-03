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
public class ScoreAdjustmentHistoryController {
    private final ScoreAdjustmentHistoryService service;

    public ScoreAdjustmentHistoryController(ScoreAdjustmentHistoryService service) {
        this.service = service;
    }

    @GetMapping("/api/business/score-adjustment-histories")
    public ApiResponse<ScoreAdjustmentHistorySearchResponse> listScoreAdjustmentHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(required = false) String adjustmentTarget,
            HttpServletRequest servletRequest) {
        ScoreAdjustmentActor actor = requireScoreAdjustmentViewer(servletRequest);
        return ApiResponse.ok(service.list(new ScoreAdjustmentHistorySearchCriteria(page, size, evaluationYear, areaCode,
                targetUserId, adjustmentTarget, actor.dataScope(), actor.organizationCode()), actor.currentUser().userId()));
    }

    @GetMapping("/api/business/score-adjustment-histories/{adjustmentHistId}")
    public ApiResponse<ScoreAdjustmentHistoryDetail> getScoreAdjustmentHistoryDetail(
            @PathVariable String adjustmentHistId,
            HttpServletRequest servletRequest) {
        ScoreAdjustmentActor actor = requireScoreAdjustmentViewer(servletRequest);
        return ApiResponse.ok(service.getDetail(adjustmentHistId, actor.dataScope(), actor.organizationCode(), actor.currentUser().userId()));
    }

    @GetMapping("/api/business/score-adjustment-histories/download")
    public ApiResponse<ScoreAdjustmentHistoryExcelDownload> downloadScoreAdjustmentHistoriesExcel(
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(required = false) String adjustmentTarget,
            HttpServletRequest servletRequest) {
        ScoreAdjustmentActor actor = requireScoreAdjustmentViewer(servletRequest);
        ScoreAdjustmentHistoryExcelDownload download = service.download(new ScoreAdjustmentHistorySearchCriteria(0, 100, evaluationYear,
                areaCode, targetUserId, adjustmentTarget, actor.dataScope(), actor.organizationCode()), actor.currentUser().userId());
        return ApiResponse.ok(download, download.requestId());
    }

    private ScoreAdjustmentActor requireScoreAdjustmentViewer(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R09")) {
                return new ScoreAdjustmentActor(currentUser, ScoreAdjustmentHistoryDataScope.ALL, null);
            }
            if (currentUser.roles().contains("R08") || currentUser.roles().contains("R04")) {
                return new ScoreAdjustmentActor(currentUser, ScoreAdjustmentHistoryDataScope.ORGANIZATION, "KNUE-DEPT-COMP");
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private record ScoreAdjustmentActor(CurrentUser currentUser, ScoreAdjustmentHistoryDataScope dataScope, String organizationCode) {
    }
}

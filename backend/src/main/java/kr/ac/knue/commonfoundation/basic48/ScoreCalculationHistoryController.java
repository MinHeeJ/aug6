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
public class ScoreCalculationHistoryController {
    private final ScoreCalculationHistoryService service;

    public ScoreCalculationHistoryController(ScoreCalculationHistoryService service) {
        this.service = service;
    }

    @GetMapping("/api/business/score-calculation-histories")
    public ApiResponse<ScoreCalculationHistorySearchResponse> listScoreCalculationHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) Long targetUserId,
            HttpServletRequest servletRequest) {
        ScoreCalculationActor actor = requireScoreCalculationViewer(servletRequest, targetUserId);
        return ApiResponse.ok(service.list(new ScoreCalculationHistorySearchCriteria(page, size, evaluationYear, areaCode,
                actor.targetUserId(targetUserId), actor.dataScope(), actor.organizationCode(), actor.selfUserId()), actor.currentUser().userId()));
    }

    @GetMapping("/api/business/score-calculation-histories/{calcHistId}")
    public ApiResponse<ScoreCalculationHistoryDetail> getScoreCalculationHistoryDetail(
            @PathVariable String calcHistId,
            HttpServletRequest servletRequest) {
        ScoreCalculationActor actor = requireScoreCalculationViewer(servletRequest, null);
        return ApiResponse.ok(service.getDetail(calcHistId, actor.dataScope(), actor.organizationCode(), actor.selfUserId(),
                actor.currentUser().userId()));
    }

    @GetMapping("/api/business/score-calculation-histories/download")
    public ApiResponse<ScoreCalculationHistoryExcelDownload> downloadScoreCalculationHistoriesExcel(
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) Long targetUserId,
            HttpServletRequest servletRequest) {
        ScoreCalculationActor actor = requireScoreCalculationViewer(servletRequest, targetUserId);
        ScoreCalculationHistoryExcelDownload download = service.download(new ScoreCalculationHistorySearchCriteria(0, 100, evaluationYear,
                areaCode, actor.targetUserId(targetUserId), actor.dataScope(), actor.organizationCode(), actor.selfUserId()), actor.currentUser().userId());
        return ApiResponse.ok(download, download.requestId());
    }

    private ScoreCalculationActor requireScoreCalculationViewer(HttpServletRequest request, Long requestedTargetUserId) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R09")) {
                return new ScoreCalculationActor(currentUser, ScoreCalculationHistoryDataScope.ALL, null, null);
            }
            if (currentUser.roles().contains("R08") || currentUser.roles().contains("R04")) {
                return new ScoreCalculationActor(currentUser, ScoreCalculationHistoryDataScope.ORGANIZATION, "KNUE-DEPT-COMP", null);
            }
            if (currentUser.roles().contains("R01")) {
                if (requestedTargetUserId != null && !requestedTargetUserId.equals(currentUser.userId())) {
                    throw new ForbiddenException();
                }
                return new ScoreCalculationActor(currentUser, ScoreCalculationHistoryDataScope.SELF, null, currentUser.userId());
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private record ScoreCalculationActor(CurrentUser currentUser,
                                         ScoreCalculationHistoryDataScope dataScope,
                                         String organizationCode,
                                         Long selfUserId) {
        Long targetUserId(Long requestedTargetUserId) {
            return selfUserId == null ? requestedTargetUserId : selfUserId;
        }
    }
}

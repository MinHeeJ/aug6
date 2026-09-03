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
public class EvaluationSnapshotController {
    private final EvaluationSnapshotService service;

    public EvaluationSnapshotController(EvaluationSnapshotService service) {
        this.service = service;
    }

    @GetMapping("/api/business/evaluation-snapshots")
    public ApiResponse<EvaluationSnapshotSearchResponse> listEvaluationSnapshots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String finalizationPoint,
            HttpServletRequest servletRequest) {
        SnapshotActor actor = requireSnapshotViewer(servletRequest);
        return ApiResponse.ok(service.list(new EvaluationSnapshotSearchCriteria(page, size, evaluationYear, finalizationPoint,
                actor.dataScope(), actor.organizationCode())));
    }

    @GetMapping("/api/business/evaluation-snapshots/{snapshotId}")
    public ApiResponse<EvaluationSnapshotDetail> getEvaluationSnapshotDetail(
            @PathVariable String snapshotId,
            HttpServletRequest servletRequest) {
        SnapshotActor actor = requireSnapshotViewer(servletRequest);
        return ApiResponse.ok(service.getDetail(snapshotId, actor.dataScope(), actor.organizationCode(), actor.currentUser().userId()));
    }

    @GetMapping("/api/business/evaluation-snapshots/download")
    public ApiResponse<EvaluationSnapshotExcelDownload> downloadEvaluationSnapshotsExcel(
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String finalizationPoint,
            HttpServletRequest servletRequest) {
        SnapshotActor actor = requireSnapshotViewer(servletRequest);
        EvaluationSnapshotExcelDownload download = service.download(new EvaluationSnapshotSearchCriteria(0, 100, evaluationYear,
                finalizationPoint, actor.dataScope(), actor.organizationCode()), actor.currentUser().userId());
        return ApiResponse.ok(download, download.requestId());
    }

    private SnapshotActor requireSnapshotViewer(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            if (currentUser.roles().contains("R09")) {
                return new SnapshotActor(currentUser, EvaluationSnapshotDataScope.ALL, null);
            }
            if (currentUser.roles().contains("R08") || currentUser.roles().contains("R04")) {
                String organizationCode = currentUser.roles().contains("R08") ? "KNUE-COLLEGE-EDU" : "KNUE-DEPT-COMP";
                return new SnapshotActor(currentUser, EvaluationSnapshotDataScope.ORGANIZATION, organizationCode);
            }
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private record SnapshotActor(CurrentUser currentUser, EvaluationSnapshotDataScope dataScope, String organizationCode) {
    }
}

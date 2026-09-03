package kr.ac.knue.commonfoundation.basic45;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;

public final class Basic45EvaluationBatchFoundationContract {
    public static final List<String> JOB_TYPES = List.of("GENERATION", "DELETION", "RECALCULATION", "CONFIRMATION");
    public static final Map<String, String> API_ROUTE_BY_PREFIX = orderedRoutes();
    private static final DateTimeFormatter BATCH_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private Basic45EvaluationBatchFoundationContract() {
    }

    public static String uiRouteForApiPath(String apiPath) {
        if (apiPath == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : API_ROUTE_BY_PREFIX.entrySet()) {
            if (apiPath.equals(entry.getKey()) || apiPath.startsWith(entry.getKey() + "/")) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static String batchId(String jobType, Clock clock, long sequence) {
        String normalizedJobType = normalizeJobType(jobType);
        if (sequence < 1L || sequence > 999999L) {
            throw new IllegalArgumentException("batchId sequence는 1 이상 999999 이하만 허용됩니다.");
        }
        LocalDate batchDate = LocalDate.now(clock == null ? Clock.systemDefaultZone() : clock);
        return "B45-" + normalizedJobType + "-" + BATCH_DATE_FORMAT.format(batchDate) + "-" + String.format("%06d", sequence);
    }

    public static <T> ApiResponse<T> okWithRequestId(T data, String requestId) {
        return ApiResponse.ok(data, requestId);
    }

    public static PreconditionReport commonPreconditionReport() {
        return new PreconditionReport(
                "READY",
                List.of(
                        "기존 SessionCookie COMMON_FOUNDATION_SESSION과 CurrentUser Principal 재사용",
                        "기존 R01~R09 role code와 EffectivePermissionService 메뉴 권한 guard 재사용",
                        "기존 batch_definitions/batch_executions/batch_execution_results 공통 배치 서비스 재사용 가능",
                        "기존 business_process_audit_logs와 request_id 기반 감사 추적 컬럼 재사용 가능",
                        "기존 React AuthProvider, AdminShell, route guard, /api 상대경로 apiClient 패턴 재사용"),
                List.of(
                        "BASIC-45 평가자료·일괄처리·재계산세대·확정 foundation schema 추가",
                        "BASIC-45 업무 API prefix를 기존 React shell route guard에 연결",
                        "신규 업무 API가 공통 ApiResponse/ApiError와 requestId, batchId 명명 규칙을 사용하도록 foundation contract 제공"),
                List.of());
    }

    private static String normalizeJobType(String jobType) {
        if (jobType == null || jobType.trim().isBlank()) {
            throw new IllegalArgumentException("jobType은 필수입니다.");
        }
        String normalized = jobType.trim().toUpperCase();
        if (!JOB_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("지원하지 않는 BASIC-45 jobType입니다.");
        }
        return normalized;
    }

    private static Map<String, String> orderedRoutes() {
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("/api/business/evaluation-material-generations", "/admin/evaluation-material-generations");
        routes.put("/api/business/evaluation-material-deletions", "/admin/evaluation-material-deletions");
        routes.put("/api/business/score-recalculations", "/admin/score-recalculations");
        routes.put("/api/business/final-evaluation-confirmations", "/admin/final-evaluation-confirmations");
        routes.put("/api/business/evaluation-batch-results", "/admin/evaluation-batch-results");
        return Map.copyOf(routes);
    }

    public record BatchSubmission(String batchId, String requestId) {
    }

    public record PreconditionReport(
            String status,
            List<String> preservedBehaviors,
            List<String> requestedChanges,
            List<String> missingContracts) {
        public PreconditionReport {
            preservedBehaviors = List.copyOf(preservedBehaviors);
            requestedChanges = List.copyOf(requestedChanges);
            missingContracts = List.copyOf(missingContracts);
        }
    }
}

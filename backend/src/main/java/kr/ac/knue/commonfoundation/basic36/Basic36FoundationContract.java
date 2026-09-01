package kr.ac.knue.commonfoundation.basic36;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Basic36FoundationContract {
    public static final Map<String, String> API_ROUTE_BY_PREFIX = orderedRoutes();

    private Basic36FoundationContract() {
    }

    public static String uiRouteForApiPath(String apiPath) {
        if (apiPath == null) {
            return null;
        }
        if (apiPath.matches("/api/researcher-profiles/[^/]+/(research-fields|careers|degrees|certifications)")) {
            return "/researcher-profiles/{employeeNo}";
        }
        if (apiPath.matches("/api/researcher-profiles/[^/]+")) {
            return "/researcher-profiles/{employeeNo}";
        }
        for (Map.Entry<String, String> entry : API_ROUTE_BY_PREFIX.entrySet()) {
            if (apiPath.equals(entry.getKey()) || apiPath.startsWith(entry.getKey() + "/")) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static PreconditionReport commonPreconditionReport() {
        return new PreconditionReport(
                "READY",
                List.of(
                        "기존 backend/frontend/infra/docker-compose.yml 단일 저장소 구조 재사용",
                        "기존 세션 쿠키 인증 Principal 재사용",
                        "기존 메뉴 URL 기반 화면 권한 guard 재사용",
                        "기존 코드·감사·배치 공통 모듈 재사용"),
                List.of(
                        "BASIC-36 증분 Flyway migration skeleton 추가",
                        "신규 업무 route placeholder를 React shell registry에 추가"),
                List.of());
    }

    private static Map<String, String> orderedRoutes() {
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("/api/admin/korus-faculty-sync-results", "/admin/korus-faculty-sync");
        routes.put("/api/admin/korus-faculty-sync-runs", "/admin/korus-faculty-sync");
        routes.put("/api/admin/korus-faculty-sync-retries", "/admin/korus-faculty-sync");
        routes.put("/api/admin/full-time-faculty-statuses", "/admin/full-time-faculty-statuses");
        routes.put("/api/researcher-profiles", "/researcher-profiles");
        routes.put("/api/admin/researcher-profiles/degree-prerequisite-missing", "/admin/researcher-profiles/degree-prerequisite-missing");
        routes.put("/api/admin/achievement-data-histories", "/admin/achievement-data-histories");
        routes.put("/api/admin/achievement-data-as-of", "/admin/achievement-data-as-of");
        return Map.copyOf(routes);
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

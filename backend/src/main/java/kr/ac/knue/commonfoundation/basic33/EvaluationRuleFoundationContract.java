package kr.ac.knue.commonfoundation.basic33;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EvaluationRuleFoundationContract {
    public static final List<String> ALLOWED_ROLE_CODES = List.of(
            "R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");

    public static final Map<String, String> API_ROUTE_BY_PREFIX = orderedRoutes();

    private EvaluationRuleFoundationContract() {
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

    private static Map<String, String> orderedRoutes() {
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("/api/admin/evaluation-areas", "/admin/evaluation-areas");
        routes.put("/api/admin/evaluation-items", "/admin/evaluation-items");
        routes.put("/api/admin/evaluation-elements", "/admin/evaluation-elements");
        routes.put("/api/admin/evaluation-management-items", "/admin/evaluation-management-items");
        routes.put("/api/admin/area-element-systems", "/admin/area-element-systems");
        return Map.copyOf(routes);
    }
}

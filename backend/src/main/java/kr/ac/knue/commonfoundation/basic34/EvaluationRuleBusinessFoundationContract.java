package kr.ac.knue.commonfoundation.basic34;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EvaluationRuleBusinessFoundationContract {
    public static final List<String> ALLOWED_ROLE_CODES = List.of(
            "R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");

    public static final List<String> WRITE_ROLE_CODES = List.of("R04", "R08", "R09");

    public static final Map<String, String> API_ROUTE_BY_PREFIX = orderedRoutes();

    private EvaluationRuleBusinessFoundationContract() {
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
        routes.put("/api/admin/evaluation-scores", "/admin/evaluation-scores");
        routes.put("/api/admin/participation-rates", "/admin/participation-rates");
        routes.put("/api/admin/calculation-formulas", "/admin/calculation-formulas");
        routes.put("/api/admin/evaluation-rule-sets", "/admin/evaluation-rule-sets");
        routes.put("/api/admin/journal-indexing-infos", "/admin/journal-indexing-infos");
        return Map.copyOf(routes);
    }
}

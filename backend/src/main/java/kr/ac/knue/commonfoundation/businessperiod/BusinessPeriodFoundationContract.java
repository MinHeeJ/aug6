package kr.ac.knue.commonfoundation.businessperiod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BusinessPeriodFoundationContract {
    public static final List<String> TABLES = List.of(
            "evaluation_date_settings",
            "input_period_settings",
            "modification_period_settings",
            "department_chair_confirm_period_settings",
            "business_period_integrated_settings");

    public static final List<String> ALLOWED_ROLE_CODES = List.of("R03", "R04", "R09");

    public static final Map<String, String> API_ROUTE_BY_PREFIX = orderedRoutes();

    private BusinessPeriodFoundationContract() {
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
        routes.put("/api/admin/evaluation-dates", "/admin/evaluation-dates");
        routes.put("/api/admin/input-periods", "/admin/input-periods");
        routes.put("/api/admin/modification-periods", "/admin/modification-periods");
        routes.put("/api/admin/department-chair-confirm-periods", "/admin/department-chair-confirm-periods");
        routes.put("/api/admin/business-periods", "/admin/business-periods");
        return Map.copyOf(routes);
    }
}

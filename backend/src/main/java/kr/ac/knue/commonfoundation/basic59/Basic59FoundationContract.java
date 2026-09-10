package kr.ac.knue.commonfoundation.basic59;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Basic59FoundationContract {
    public static final String FOUNDATION_MIGRATION = "V56__basic59_phase1_foundation_reservations.sql";
    public static final String MENU_ID_BASIS = "maxExistingMenuId=564; nextReservedMenuIds=565-568";

    public static final List<String> EXISTING_ROLE_CODES = List.of(
            "R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
    public static final List<String> AUTH_REUSE_POINTS = List.of(
            "SessionCookie: COMMON_FOUNDATION_SESSION",
            "CurrentUser Principal",
            "EffectivePermissionService menu permission resolver",
            "existing R01-R09 role resolver",
            "existing data scope resolver contracts");
    public static final List<String> REVIEW_CHECKLIST = List.of(
            "no-new-auth",
            "no-new-user-table",
            "no-new-org-table",
            "no-new-role-table",
            "no-second-compose");

    public static final Map<String, Long> RESERVED_MENU_IDS = Map.of(
            "SCR-EVALUATION-ELEMENT-MGMT-ITEMS", 565L,
            "SCR-PARTICIPATION-RATE-OPERATION", 566L,
            "SCR-MANAGEMENT-ITEM-EVAL-SCORES", 567L,
            "SCR-COURSE-AREA-GROUP-GRADES", 568L);

    public static final Map<String, String> API_ROUTE_BY_PREFIX = orderedRoutes();

    private Basic59FoundationContract() {
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
        routes.put("/api/business/evaluation-element-management-items", "/admin/evaluation-element-management-items");
        routes.put("/api/business/participation-rate-operation-settings", "/admin/participation-rate-operation-settings");
        routes.put("/api/business/management-item-evaluation-scores", "/admin/management-item-evaluation-scores");
        routes.put("/api/business/course-area-group-grades", "/evaluation/course-area-group-grades");
        return Map.copyOf(routes);
    }
}

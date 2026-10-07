package kr.ac.knue.commonfoundation.common.api;

import java.util.Map;

/** Maps only the new education resources to their single canonical permission menu. */
public final class EducationAchievementRoutes {
    private static final Map<String, String> ROUTES = Map.of(
            "/api/business/employment-rate-improvements", "/faculty/employment-rate-improvement-achievements",
            "/api/business/course-operations", "/faculty/course-offering-operation-achievements",
            "/api/business/lecture-improvements", "/faculty/teaching-improvement-achievements",
            "/api/business/employment-rate-achievements", "/faculty/employment-rate-achievements");

    private EducationAchievementRoutes() {
    }

    /** Includes subresources without accepting unrelated resource names that share a prefix. */
    public static String uiRouteForApiPath(String path) {
        if (path == null) {
            return null;
        }
        for (var route : ROUTES.entrySet()) {
            if (path.equals(route.getKey()) || path.startsWith(route.getKey() + "/")) {
                return route.getValue();
            }
        }
        return null;
    }

    public static boolean supports(String path) {
        return uiRouteForApiPath(path) != null;
    }
}

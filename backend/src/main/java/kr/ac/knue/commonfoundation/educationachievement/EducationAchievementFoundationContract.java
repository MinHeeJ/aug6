package kr.ac.knue.commonfoundation.educationachievement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps education-achievement business APIs to their existing menu-authorized UI routes.
 *
 * <p>The authentication filter uses this mapping so a valid SessionCookie still has to pass the
 * menu permission boundary before later achievement operations can access shared data.</p>
 */
public final class EducationAchievementFoundationContract {
    private static final Map<String, String> API_ROUTE_BY_PREFIX = routes();

    private EducationAchievementFoundationContract() {
    }

    /**
     * Resolves the protected UI route for an education-achievement API path.
     *
     * @param apiPath request API path
     * @return the menu route, or {@code null} when the path does not belong to this module
     */
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

    private static Map<String, String> routes() {
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("/api/business/lecture-evaluation-achievements", "/achievement/lecture-evaluations");
        routes.put("/api/business/lecture-performance-achievements", "/achievement/lecture-performances");
        routes.put("/api/business/student-guidance-achievements", "/achievement/student-guidance");
        routes.put("/api/business/graduate-degree-achievements", "/achievement/graduate-degrees");
        return Map.copyOf(routes);
    }
}

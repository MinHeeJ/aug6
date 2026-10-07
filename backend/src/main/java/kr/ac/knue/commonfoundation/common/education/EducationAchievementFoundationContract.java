package kr.ac.knue.commonfoundation.common.education;

import java.util.Map;

/** Shared route and Excel identifiers; mapping a route never grants endpoint or data-scope permission. */
public final class EducationAchievementFoundationContract {
    public static final String EMPLOYMENT_TEMPLATE_ID = "EMPLOYMENT-RATE-TEMPLATE-001";
    private static final Map<String, String> ROUTES = Map.of(
            "/api/business/employment-rate-improvements", "/faculty/employment-rate-improvement-achievements",
            "/api/business/course-operations", "/faculty/course-offering-operation-achievements",
            "/api/business/lecture-improvements", "/faculty/teaching-improvement-achievements",
            "/api/business/employment-rate-achievements", "/faculty/employment-rate-achievements");

    private EducationAchievementFoundationContract() {
    }

    /** Resolves resource and child paths, without accidentally accepting a similarly prefixed resource. */
    public static String uiRouteForApiPath(String apiPath) {
        if (apiPath == null) {
            return null;
        }
        return ROUTES.entrySet().stream()
                .filter(entry -> apiPath.equals(entry.getKey()) || apiPath.startsWith(entry.getKey() + "/"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}

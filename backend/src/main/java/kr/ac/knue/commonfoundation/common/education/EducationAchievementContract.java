package kr.ac.knue.commonfoundation.common.education;

import java.util.List;

/**
 * Defines the approved education achievement surfaces shared by menu and API wiring.
 * This metadata never supplies menu visibility, business data or authorization decisions;
 * those remain owned by the existing database-backed permission and domain services.
 */
public final class EducationAchievementContract {
    public static final String EMPLOYMENT_EXCEL_TEMPLATE_ID = "EMPLOYMENT-RATE-V1";
    public static final String EMPLOYMENT_EXCEL_BUSINESS_TYPE = "EMPLOYMENT_RATE";

    public static final List<Surface> SURFACES = List.of(
            new Surface(
                    "/api/business/employment-rate-improvements",
                    "/faculty/employment-rate-improvement-achievements",
                    "SCR-EMPLOYMENT-RATE-IMPROVEMENT-ACHIEVEMENT",
                    "FR-029"),
            new Surface(
                    "/api/business/course-operations",
                    "/faculty/course-offering-operation-achievements",
                    "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT",
                    "FR-030"),
            new Surface(
                    "/api/business/lecture-improvements",
                    "/faculty/teaching-improvement-achievements",
                    "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT",
                    "FR-031"),
            new Surface(
                    "/api/business/employment-rate-achievements",
                    "/faculty/employment-rate-achievements",
                    "SCR-EMPLOYMENT-RATE-ACHIEVEMENT",
                    "FR-032"));

    private EducationAchievementContract() {
    }

    /**
     * Maps both collections and slash-delimited descendants to the same menu permission.
     * An unknown path returns null so unrelated mappings can continue unchanged.
     */
    public static String uiRouteForApiPath(String apiPath) {
        if (apiPath == null) {
            return null;
        }
        for (Surface surface : SURFACES) {
            if (apiPath.equals(surface.apiPath()) || apiPath.startsWith(surface.apiPath() + "/")) {
                return surface.uiRoute();
            }
        }
        return null;
    }

    /** Approved canonical names; SQL seed and final router/filter registrations must agree. */
    public record Surface(String apiPath, String uiRoute, String screenId, String achievementType) {
    }
}

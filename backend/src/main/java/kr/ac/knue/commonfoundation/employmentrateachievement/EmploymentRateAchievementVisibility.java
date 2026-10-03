package kr.ac.knue.commonfoundation.employmentrateachievement;

/**
 * Encodes the server-side data-scope predicate selected from the authenticated
 * role. It prevents a requester identifier from becoming a universal row filter.
 */
public record EmploymentRateAchievementVisibility(Long requesterUserId, String scope) {
    public static final String OWNER = "OWNER";
    public static final String SHARED_ORGANIZATION = "SHARED_ORGANIZATION";
    public static final String CERTIFICATION_SCOPE = "CERTIFICATION_SCOPE";
}

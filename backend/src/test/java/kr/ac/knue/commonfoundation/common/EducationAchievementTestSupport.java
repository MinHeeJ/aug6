package kr.ac.knue.commonfoundation.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;

/** DB-free principal/date fixtures, not a substitute for real session-cookie integration tests. */
public final class EducationAchievementTestSupport {
    public static final LocalDate ACHIEVEMENT_DATE = LocalDate.parse("2026-04-10");
    public static final LocalDate BOUNDARY_DATE = LocalDate.parse("2025-12-31");
    public static final LocalDateTime PROCESSED_AT = LocalDateTime.parse("2026-04-11T09:30:00");

    private EducationAchievementTestSupport() {
    }

    /** Caller supplies identity and isolated/union roles; no R09 role is silently attached. */
    public static CurrentUser principal(Long userId, String... roles) {
        return new CurrentUser(userId, "test-user-" + userId, "test-employee-" + userId,
                "검증 사용자", List.of(roles), List.of());
    }
}

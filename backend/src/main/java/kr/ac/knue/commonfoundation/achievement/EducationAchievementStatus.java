package kr.ac.knue.commonfoundation.achievement;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Canonical technical states for education-achievement certification and the only permitted transitions.
 */
public enum EducationAchievementStatus {
    DRAFTING,
    SUBMITTED,
    DEPARTMENT_CONFIRMED,
    DEPARTMENT_REJECTED,
    CERTIFIED,
    CERTIFICATION_RETURNED,
    EVALUATION_CONFIRMED,
    DELETED;

    private static final Map<EducationAchievementStatus, Set<EducationAchievementStatus>> ALLOWED_TRANSITIONS = Map.of(
            DRAFTING, EnumSet.of(SUBMITTED),
            SUBMITTED, EnumSet.of(DEPARTMENT_CONFIRMED, DEPARTMENT_REJECTED),
            DEPARTMENT_REJECTED, EnumSet.of(SUBMITTED),
            DEPARTMENT_CONFIRMED, EnumSet.of(CERTIFIED, CERTIFICATION_RETURNED),
            CERTIFICATION_RETURNED, EnumSet.of(SUBMITTED),
            CERTIFIED, EnumSet.of(EVALUATION_CONFIRMED),
            EVALUATION_CONFIRMED, EnumSet.of(CERTIFIED),
            DELETED, EnumSet.noneOf(EducationAchievementStatus.class));

    /** Returns whether the requested state change is part of CMN-202's closed transition graph. */
    public boolean canTransitionTo(EducationAchievementStatus nextStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(nextStatus);
    }

    /** Returns whether CMN-203 requires a rejection reason or opinion for this destination state. */
    public boolean requiresReason() {
        return this == DEPARTMENT_REJECTED || this == CERTIFICATION_RETURNED;
    }
}

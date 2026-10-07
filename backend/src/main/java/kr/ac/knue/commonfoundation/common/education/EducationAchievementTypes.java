package kr.ac.knue.commonfoundation.common.education;

import java.util.Set;

/** Canonical source type identifiers shared by new headers and polymorphic lifecycle histories. */
public final class EducationAchievementTypes {
    public static final String EMPLOYMENT_RATE_IMPROVEMENT = "EMPLOYMENT_RATE_IMPROVEMENT";
    public static final String COURSE_OPERATION = "COURSE_OPERATION";
    public static final String LECTURE_IMPROVEMENT = "LECTURE_IMPROVEMENT";
    public static final String EMPLOYMENT_RATE_ACHIEVEMENT = "EMPLOYMENT_RATE_ACHIEVEMENT";
    public static final Set<String> EXTENSION_TYPES = Set.of(
            EMPLOYMENT_RATE_IMPROVEMENT, COURSE_OPERATION, LECTURE_IMPROVEMENT, EMPLOYMENT_RATE_ACHIEVEMENT);
    public static final Set<String> HISTORY_TYPES = Set.of(
            "LECTURE_EVALUATION", "LECTURE", "STUDENT_GUIDANCE", "DEGREE_COMPLETION",
            EMPLOYMENT_RATE_IMPROVEMENT, COURSE_OPERATION, LECTURE_IMPROVEMENT, EMPLOYMENT_RATE_ACHIEVEMENT);

    private EducationAchievementTypes() {
    }
}

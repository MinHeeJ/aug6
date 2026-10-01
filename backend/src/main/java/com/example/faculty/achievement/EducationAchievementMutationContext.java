package com.example.faculty.achievement;

import java.time.LocalDate;
import java.util.Set;

/**
 * Supplies the verified request facts required by the common education-achievement mutation guard.
 */
public record EducationAchievementMutationContext(
        Set<String> roleCodes,
        boolean dataScopeAllowed,
        boolean inputPeriodActive,
        String certificationStatus,
        LocalDate occurredDate,
        LocalDate evaluationPeriodStart,
        LocalDate evaluationPeriodEnd
) {
}

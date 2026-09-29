package com.example.faculty.achievement;

import java.util.Set;

/**
 * 저장 또는 상태 전이 전에 공통 정책이 판정할 권한·범위·기간·상태 정보를 전달한다.
 */
public record AchievementValidationRequest(
        boolean functionAllowed,
        Set<String> requesterRoles,
        Set<String> permittedOrganizationCodes,
        String organizationCode,
        boolean inputPeriodOpen,
        AchievementCertificationStatus certificationStatus,
        boolean occurredWithinEvaluationPeriod) {
}

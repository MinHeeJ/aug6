package com.example.faculty.achievement;

import java.time.Instant;

/**
 * 허용된 인증 상태 전이의 불변 감사 레코드다. 저장 계층은 이 값을 append-only 상태 이력으로 기록한다.
 */
public record EducationAchievementStatusHistory(
        AchievementCertificationStatus previousStatus,
        AchievementCertificationStatus nextStatus,
        String actionType,
        String reasonCode,
        String opinion,
        long processedBy,
        Instant processedAt) {
}

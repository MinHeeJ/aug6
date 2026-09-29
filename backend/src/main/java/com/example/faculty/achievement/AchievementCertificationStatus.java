package com.example.faculty.achievement;

/**
 * 인증상태 전이의 단일 코드 집합이다. 교육영역 실적 header와 상태 이력의 값이 일치하도록 유지한다.
 */
public enum AchievementCertificationStatus {
    DRAFTING,
    SUBMITTED,
    DEPARTMENT_CONFIRMED,
    DEPARTMENT_REJECTED,
    CERTIFIED,
    CERTIFICATION_RETURNED,
    EVALUATION_CONFIRMED,
    DELETED
}

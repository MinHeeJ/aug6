package com.example.faculty.achievement;

/**
 * 인증 상태 전이 요청의 원천 상태, 대상 상태, 처리 역할과 반려 근거를 전달한다.
 */
public record EducationAchievementTransitionRequest(
        AchievementCertificationStatus previousStatus,
        AchievementCertificationStatus nextStatus,
        String requesterRole,
        String reasonCode,
        String opinion) {
}

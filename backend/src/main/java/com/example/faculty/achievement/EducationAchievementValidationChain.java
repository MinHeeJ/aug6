package com.example.faculty.achievement;

import java.util.List;

/**
 * 교육영역 실적 mutation의 공통 접근 조건을 고정 순서로 집행한다.
 * 기능 권한과 데이터 범위가 모두 충족된 경우에만 기간 및 상태 검증으로 진행한다.
 */
public final class EducationAchievementValidationChain {
    private static final String OUTSIDE_EVALUATION_PERIOD_WARNING = "업적발생일이 평가대상 기간 밖입니다. 저장은 허용되며 평가 시 확인이 필요합니다.";

    /**
     * 권한, 데이터 범위, 입력기간, 평가확정 잠금, 발생일 경고를 순서대로 판정한다.
     * 발생일 기간 밖은 CMN-1102에 따라 차단 대신 경고로 반환한다.
     */
    public AchievementValidationResult validate(AchievementValidationRequest request) {
        if (request == null) {
            throw new AchievementValidationException("실적 검증 정보가 필요합니다.");
        }
        if (!request.functionAllowed() || request.requesterRoles() == null || request.requesterRoles().isEmpty()) {
            throw new AchievementValidationException("실적 처리 기능 권한이 없습니다.");
        }
        if (request.organizationCode() == null || request.organizationCode().isBlank()
                || request.permittedOrganizationCodes() == null
                || !request.permittedOrganizationCodes().contains(request.organizationCode())) {
            throw new AchievementValidationException("대상 실적의 데이터 범위 권한이 없습니다.");
        }
        if (!request.inputPeriodOpen()) {
            throw new AchievementValidationException("현재는 실적 입력기간이 아닙니다.");
        }
        if (request.certificationStatus() == AchievementCertificationStatus.EVALUATION_CONFIRMED) {
            throw new AchievementValidationException("평가확정 실적은 수정하거나 삭제할 수 없습니다.");
        }
        if (!request.occurredWithinEvaluationPeriod()) {
            return new AchievementValidationResult(List.of(OUTSIDE_EVALUATION_PERIOD_WARNING));
        }
        return new AchievementValidationResult(List.of());
    }
}

package com.example.faculty.achievement;

/**
 * 교육영역 실적 공통 검증 체인이 요청을 차단할 때 사용하는 업무 예외다.
 */
public class AchievementValidationException extends RuntimeException {
    public AchievementValidationException(String message) {
        super(message);
    }
}

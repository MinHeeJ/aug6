package com.example.faculty.achievement;

import java.util.List;

/**
 * 차단하지 않는 업무 경고를 호출 계층이 API와 화면에 동일하게 전달할 수 있게 한다.
 */
public record AchievementValidationResult(List<String> warnings) {
}

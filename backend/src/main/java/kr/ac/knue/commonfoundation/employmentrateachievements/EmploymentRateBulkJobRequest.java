package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.Map;

/** Policy-gated bulk command: validation does not imply that execution is enabled. */
public record EmploymentRateBulkJobRequest(
        String evaluationYear,
        String actionType,
        Map<String, Object> targetCondition) {
}

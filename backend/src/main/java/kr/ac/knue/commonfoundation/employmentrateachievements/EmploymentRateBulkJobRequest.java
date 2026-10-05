package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.Map;

/** Request for an R07 employment-rate bulk operation awaiting approved policy. */
public record EmploymentRateBulkJobRequest(
        String evaluationYear,
        String actionType,
        Map<String, Object> targetCondition) {
}

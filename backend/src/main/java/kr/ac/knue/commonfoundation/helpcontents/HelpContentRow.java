package kr.ac.knue.commonfoundation.helpcontents;

import java.time.LocalDateTime;

public record HelpContentRow(
        String screenId,
        String businessDescription,
        String inputCriteria,
        String faq,
        String contact,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {
}

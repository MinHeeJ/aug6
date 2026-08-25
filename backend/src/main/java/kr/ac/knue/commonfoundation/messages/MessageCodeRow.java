package kr.ac.knue.commonfoundation.messages;

import java.time.LocalDateTime;

public record MessageCodeRow(
        String messageCode,
        String messageType,
        String userMessage,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {
}

package kr.ac.knue.commonfoundation.messages;

import java.util.List;

public record MessageSearchResponse(List<MessageCodeRow> messages, int page, int size, long totalElements) {
}

package kr.ac.knue.commonfoundation.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageManagementService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private static final Set<String> ALLOWED_MESSAGE_TYPES = Set.of("SAVE", "DELETE", "APPROVAL", "REJECT", "ERROR", "SESSION_EXPIRED");
    private final MessageManagementMapper mapper;

    public MessageManagementService(MessageManagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public MessageSearchResponse listMessages(int page, int size, String messageType, String messageCode) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        String normalizedType = normalizeTypeOrNull(messageType);
        String normalizedCode = blankToNull(messageCode);
        List<MessageCodeRow> messages = mapper.listMessages(normalizedType, normalizedCode, safeSize, safePage * safeSize);
        long total = mapper.countMessages(normalizedType, normalizedCode);
        return new MessageSearchResponse(messages, safePage, safeSize, total);
    }

    @Transactional
    public MessageCodeRow createMessage(MessageSaveRequest request, Long currentUserId) {
        String normalizedCode = normalizeCode(request.getMessageCode());
        return persistMessage(normalizedCode, request, currentUserId);
    }

    @Transactional
    public MessageCodeRow saveMessage(String messageCode, MessageSaveRequest request, Long currentUserId) {
        String normalizedCode = normalizeCode(messageCode);
        return persistMessage(normalizedCode, request, currentUserId);
    }

    private MessageCodeRow persistMessage(String normalizedCode, MessageSaveRequest request, Long currentUserId) {
        validateSaveRequest(normalizedCode, request);
        String type = request.getMessageType().trim().toUpperCase();
        mapper.upsertMessage(normalizedCode, type, request.getUserMessage().trim(), currentUserId, request.getChangeReason().trim());
        return mapper.findMessage(normalizedCode);
    }

    @Transactional(readOnly = true)
    public MessageTextResponse getMessageText(String messageCode) {
        MessageTextResponse text = mapper.findMessageText(normalizeCode(messageCode));
        if (text == null) {
            throw new NotFoundException("등록된 메시지코드가 없습니다.");
        }
        return text;
    }

    private void validateSaveRequest(String messageCode, MessageSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (messageCode.isBlank()) {
            fields.add(new ValidationError("messageCode", "메시지코드를 입력하세요."));
        }
        request.getUnexpectedFields().forEach(field -> fields.add(new ValidationError(field, "메시지 관리에서 허용하지 않는 필드입니다.")));
        String type = request.getMessageType() == null ? null : request.getMessageType().trim().toUpperCase();
        if (type != null && !type.isBlank() && !ALLOWED_MESSAGE_TYPES.contains(type)) {
            fields.add(new ValidationError("messageType", "허용된 메시지 유형이 아닙니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("메시지 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizeTypeOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}

package kr.ac.knue.commonfoundation.basic32;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataChangeHistoryService {
    private static final Set<String> CHANGE_TYPES = Set.of("CREATE", "UPDATE", "DELETE");
    private final DataChangeHistoryMapper mapper;

    public DataChangeHistoryService(DataChangeHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DataChangeHistorySearchResponse list(DataChangeHistorySearchCriteria criteria, CurrentUser currentUser) {
        if (currentUser == null || !currentUser.roles().contains("R09")) {
            throw new ForbiddenException();
        }
        List<ValidationError> fields = validate(criteria);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("데이터 변경 이력 검색조건이 올바르지 않습니다.", fields);
        }
        return new DataChangeHistorySearchResponse(
                mapper.listHistories(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countHistories(criteria));
    }

    private List<ValidationError> validate(DataChangeHistorySearchCriteria criteria) {
        List<ValidationError> fields = new ArrayList<>();
        if (hasText(criteria.changeType()) && !CHANGE_TYPES.contains(criteria.changeType())) {
            fields.add(new ValidationError("changeType", "CREATE, UPDATE, DELETE 중 하나를 선택하세요."));
        }
        validateDateTime(criteria.changedAtFrom(), "changedAtFrom", fields);
        validateDateTime(criteria.changedAtTo(), "changedAtTo", fields);
        if (hasText(criteria.changedAtFrom()) && hasText(criteria.changedAtTo()) && fields.isEmpty()) {
            LocalDateTime from = LocalDateTime.parse(criteria.changedAtFrom());
            LocalDateTime to = LocalDateTime.parse(criteria.changedAtTo());
            if (from.isAfter(to)) {
                fields.add(new ValidationError("changedAtTo", "종료 변경일시는 시작 변경일시보다 빠를 수 없습니다."));
            }
        }
        return fields;
    }

    private void validateDateTime(String value, String field, List<ValidationError> fields) {
        if (!hasText(value)) {
            return;
        }
        try {
            LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            fields.add(new ValidationError(field, "yyyy-MM-ddTHH:mm:ss 형식으로 입력하세요."));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

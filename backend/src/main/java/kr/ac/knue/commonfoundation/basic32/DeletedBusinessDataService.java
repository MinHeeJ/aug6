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
public class DeletedBusinessDataService {
    private static final Set<String> BUSINESS_TYPES = Set.of("FACULTY_ACHIEVEMENT", "ACADEMIC_GRANT", "OBJECTION");
    private final DeletedBusinessDataMapper mapper;

    public DeletedBusinessDataService(DeletedBusinessDataMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DeletedBusinessDataSearchResponse list(DeletedBusinessDataSearchCriteria criteria, CurrentUser currentUser) {
        if (currentUser == null || !currentUser.roles().contains("R09")) {
            throw new ForbiddenException();
        }
        List<ValidationError> fields = validate(criteria);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("삭제자료 검색조건이 올바르지 않습니다.", fields);
        }
        return new DeletedBusinessDataSearchResponse(
                mapper.listDeletedData(criteria),
                Math.max(criteria.page(), 0),
                criteria.safeSize(),
                mapper.countDeletedData(criteria));
    }

    private List<ValidationError> validate(DeletedBusinessDataSearchCriteria criteria) {
        List<ValidationError> fields = new ArrayList<>();
        if (hasText(criteria.businessType()) && !BUSINESS_TYPES.contains(criteria.businessType())) {
            fields.add(new ValidationError("businessType", "FACULTY_ACHIEVEMENT, ACADEMIC_GRANT, OBJECTION 중 하나를 선택하세요."));
        }
        validateDateTime(criteria.deletedAtFrom(), "deletedAtFrom", fields);
        validateDateTime(criteria.deletedAtTo(), "deletedAtTo", fields);
        if (hasText(criteria.deletedAtFrom()) && hasText(criteria.deletedAtTo()) && fields.isEmpty()) {
            LocalDateTime from = LocalDateTime.parse(criteria.deletedAtFrom());
            LocalDateTime to = LocalDateTime.parse(criteria.deletedAtTo());
            if (from.isAfter(to)) {
                fields.add(new ValidationError("deletedAtTo", "종료 삭제일시는 시작 삭제일시보다 빠를 수 없습니다."));
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

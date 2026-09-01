package kr.ac.knue.commonfoundation.basic36;

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
public class AchievementDataHistoryService {
    private static final Set<String> CHANGE_TYPES = Set.of("CREATE", "UPDATE", "DELETE");
    private final AchievementDataHistoryMapper mapper;

    public AchievementDataHistoryService(AchievementDataHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AchievementDataHistorySearchResponse listHistories(AchievementDataHistorySearchCriteria criteria, CurrentUser currentUser) {
        ensureHistoryReader(currentUser);
        List<ValidationError> fields = validateHistoryCriteria(criteria);
        if (!fields.isEmpty()) throw new BusinessValidationException("업적데이터 변경이력 검색조건이 올바르지 않습니다.", fields);
        return new AchievementDataHistorySearchResponse(mapper.listHistories(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countHistories(criteria));
    }

    @Transactional(readOnly = true)
    public AchievementDataAsOfSearchResponse listAsOf(AchievementDataAsOfSearchCriteria criteria, CurrentUser currentUser) {
        ensureHistoryReader(currentUser);
        List<ValidationError> fields = validateAsOfCriteria(criteria);
        if (!fields.isEmpty()) throw new BusinessValidationException("업적데이터 기준시점 검색조건이 올바르지 않습니다.", fields);
        return new AchievementDataAsOfSearchResponse(mapper.listAsOfSnapshots(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countAsOfSnapshots(criteria));
    }

    private void ensureHistoryReader(CurrentUser currentUser) {
        if (currentUser == null || !(currentUser.roles().contains("R04") || currentUser.roles().contains("R08") || currentUser.roles().contains("R09"))) {
            throw new ForbiddenException();
        }
    }

    private List<ValidationError> validateHistoryCriteria(AchievementDataHistorySearchCriteria criteria) {
        List<ValidationError> fields = new ArrayList<>();
        validateSize(criteria.size(), fields);
        if (hasText(criteria.changeType()) && !CHANGE_TYPES.contains(criteria.changeType())) fields.add(new ValidationError("changeType", "CREATE, UPDATE, DELETE 중 하나를 선택하세요."));
        validateDateTime(criteria.changedAtFrom(), "changedAtFrom", fields);
        validateDateTime(criteria.changedAtTo(), "changedAtTo", fields);
        if (hasText(criteria.changedAtFrom()) && hasText(criteria.changedAtTo()) && fields.isEmpty() && LocalDateTime.parse(criteria.changedAtFrom()).isAfter(LocalDateTime.parse(criteria.changedAtTo()))) {
            fields.add(new ValidationError("changedAtTo", "종료 변경일시는 시작 변경일시보다 빠를 수 없습니다."));
        }
        return fields;
    }

    private List<ValidationError> validateAsOfCriteria(AchievementDataAsOfSearchCriteria criteria) {
        List<ValidationError> fields = new ArrayList<>();
        validateSize(criteria.size(), fields);
        if (!hasText(criteria.asOfAt())) fields.add(new ValidationError("asOfAt", "기준시점은 필수입니다."));
        validateDateTime(criteria.asOfAt(), "asOfAt", fields);
        return fields;
    }

    private void validateSize(int size, List<ValidationError> fields) {
        if (size != 20 && size != 50 && size != 100) fields.add(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요."));
    }

    private void validateDateTime(String value, String field, List<ValidationError> fields) {
        if (!hasText(value)) return;
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

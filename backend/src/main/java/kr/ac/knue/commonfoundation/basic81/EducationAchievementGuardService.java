package kr.ac.knue.commonfoundation.basic81;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enforces the ordered server-side guards shared by all education-achievement
 * writes: role and data scope, active input period, finalization lock, then
 * the non-blocking occurred-date warning.
 */
@Service
public class EducationAchievementGuardService {
    private static final Set<String> EDUCATION_ACHIEVEMENT_ROLES = Set.of("R01", "R02", "R04");
    private static final Pattern EVALUATION_YEAR = Pattern.compile("^[0-9]{4}$");
    private final EducationAchievementGuardMapper mapper;

    public EducationAchievementGuardService(EducationAchievementGuardMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Verifies a command before a mapper can mutate an education-achievement
     * row. A date outside the evaluation period is deliberately returned as a
     * warning because the business contract permits the save.
     */
    @Transactional(readOnly = true)
    public OccurredDateValidation validateMutation(
            CurrentUser requester,
            EducationAchievementMutationContext context) {
        validateContext(context);
        requireDataScope(requester, context.targetUserId());
        requireActiveInputPeriod(context.evaluationYear(), context.targetUserId());
        requireNotEvaluationConfirmed(context.evaluationYear(), context.targetUserId());
        return validateOccurredDate(context.evaluationYear(), context.targetUserId(), context.occurredDate());
    }

    private void requireDataScope(CurrentUser requester, Long targetUserId) {
        if (requester == null) {
            throw new UnauthenticatedException();
        }
        if (requester.roles() == null
                || requester.roles().stream().noneMatch(EDUCATION_ACHIEVEMENT_ROLES::contains)) {
            throw new ForbiddenException();
        }
        if (requester.roles().contains("R01") && targetUserId.equals(requester.userId())) {
            return;
        }
        if (requester.roles().contains("R02")
                && mapper.countSharedActiveOrganization(requester.userId(), targetUserId) > 0) {
            return;
        }
        if (requester.roles().contains("R04")
                && mapper.countCertificationScope(requester.userId(), targetUserId) > 0) {
            return;
        }
        throw new ForbiddenException();
    }

    private void requireActiveInputPeriod(String evaluationYear, Long targetUserId) {
        if (mapper.countActiveInputPeriods(evaluationYear, targetUserId) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아니므로 저장하거나 수정할 수 없습니다.");
        }
    }

    private void requireNotEvaluationConfirmed(String evaluationYear, Long targetUserId) {
        if (mapper.countEvaluationConfirmations(targetUserId, evaluationYear) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정하거나 삭제할 수 없습니다.");
        }
    }

    private OccurredDateValidation validateOccurredDate(
            String evaluationYear,
            Long targetUserId,
            java.time.LocalDate occurredDate) {
        if (mapper.countEvaluationDatePeriods(evaluationYear, targetUserId, occurredDate) == 0) {
            return OccurredDateValidation.outsideEvaluationPeriod();
        }
        return OccurredDateValidation.accepted();
    }

    private void validateContext(EducationAchievementMutationContext context) {
        List<ValidationError> fields = new ArrayList<>();
        if (context == null) {
            fields.add(new ValidationError("body", "교육영역 실적 저장 정보가 필요합니다."));
        } else {
            if (context.targetUserId() == null || context.targetUserId() <= 0) {
                fields.add(new ValidationError("targetUserId", "실적 대상자를 입력하세요."));
            }
            if (context.evaluationYear() == null
                    || !EVALUATION_YEAR.matcher(context.evaluationYear().trim()).matches()) {
                fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
            }
            if (context.occurredDate() == null) {
                fields.add(new ValidationError("occurredDate", "업적발생일을 입력하세요."));
            }
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("교육영역 실적 검증 요청이 올바르지 않습니다.", fields);
        }
    }
}

package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;

/**
 * Applies the shared education-achievement write guards in their required order before a feature service mutates data.
 */
public class EducationAchievementValidationChain {
    private static final String STUDENT_GUIDANCE = "STUDENT_GUIDANCE";
    private static final String EVALUATION_CONFIRMED = "EVALUATION_CONFIRMED";
    private static final Set<String> MUTATING_FUNCTIONS = Set.of("CREATE", "UPDATE", "DELETE", "EXECUTE");
    private final EducationAchievementAccessPort accessPort;

    public EducationAchievementValidationChain(EducationAchievementAccessPort accessPort) {
        this.accessPort = accessPort;
    }

    /**
     * Checks authorization, data scope, active input period, confirmed-data locking, and occurrence-date warning eligibility.
     * Request payload validation is deliberately supplied by the owning feature service after the common guards pass.
     */
    public EducationAchievementValidationResult validate(
            CurrentUser user,
            EducationAchievementValidationRequest request,
            Instant processedAt) {
        requireAuthenticated(user);
        requireRoleAndDataScope(user, request);
        requireActiveInputPeriod(user, request, processedAt);
        requireNotEvaluationConfirmed(request);
        return new EducationAchievementValidationResult(
                !accessPort.isWithinEvaluationPeriod(request.evaluationYear(), request.occurredDate()),
                !accessPort.isWithinEvaluationPeriod(request.evaluationYear(), request.occurredDate())
                        ? "업적발생일이 평가대상 기간 밖입니다. 저장은 가능하지만 평가대상 기간을 확인하세요."
                        : null);
    }

    private void requireAuthenticated(CurrentUser user) {
        if (user == null) {
            throw new UnauthenticatedException();
        }
    }

    private void requireRoleAndDataScope(CurrentUser user, EducationAchievementValidationRequest request) {
        if (STUDENT_GUIDANCE.equals(request.achievementType()) && "EXCEL_UPLOAD".equals(request.functionType())) {
            if (user.roles().contains("R07") && accessPort.hasDataScope(user.userId(), request.organizationCode(), request.employeeNo())) {
                return;
            }
        } else if (user.roles().stream().anyMatch(role -> Set.of("R01", "R02", "R04", "R09").contains(role))
                && accessPort.hasDataScope(user.userId(), request.organizationCode(), request.employeeNo())) {
            return;
        }
        throw new ForbiddenException();
    }

    private void requireActiveInputPeriod(CurrentUser user, EducationAchievementValidationRequest request, Instant processedAt) {
        if (MUTATING_FUNCTIONS.contains(request.functionType())
                && !accessPort.hasActiveInputPeriod(user.userId(), request.evaluationYear(), request.organizationCode(), processedAt)) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
    }

    private void requireNotEvaluationConfirmed(EducationAchievementValidationRequest request) {
        if (MUTATING_FUNCTIONS.contains(request.functionType()) && EVALUATION_CONFIRMED.equals(request.targetStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정하거나 삭제할 수 없습니다.");
        }
    }

    /** Shared persistence lookups required to apply the business guard without duplicating common authorization rules. */
    public interface EducationAchievementAccessPort {
        boolean hasDataScope(Long userId, String organizationCode, String employeeNo);

        boolean hasActiveInputPeriod(Long userId, String evaluationYear, String organizationCode, Instant processedAt);

        boolean isWithinEvaluationPeriod(String evaluationYear, LocalDate occurredDate);
    }

    /** Immutable input to the common guard; the feature service owns detailed payload validation. */
    public record EducationAchievementValidationRequest(
            String achievementType,
            String functionType,
            String evaluationYear,
            String organizationCode,
            String employeeNo,
            String targetStatus,
            LocalDate occurredDate) {
    }

    /** Warning-only occurrence-date decision returned after all blocking guards have passed. */
    public record EducationAchievementValidationResult(boolean occurredDateWarning, String warningMessage) {
    }
}

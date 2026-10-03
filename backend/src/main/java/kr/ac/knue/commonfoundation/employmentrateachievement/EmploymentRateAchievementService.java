package kr.ac.knue.commonfoundation.employmentrateachievement;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies the approved owner, department, and certification read scopes before
 * employment-rate data is handed to an API caller.
 */
@Service
public class EmploymentRateAchievementService {
    private final EmploymentRateAchievementMapper mapper;

    public EmploymentRateAchievementService(EmploymentRateAchievementMapper mapper) {
        this.mapper = mapper;
    }

    /** Returns only rows reachable through the caller's approved data scope. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementSearchResponse list(
            EmploymentRateAchievementSearchCriteria criteria,
            CurrentUser requester) {
        EmploymentRateAchievementVisibility visibility = visibilityFor(requester);
        return new EmploymentRateAchievementSearchResponse(
                mapper.list(criteria, visibility),
                criteria.page(),
                criteria.pageSize(),
                mapper.count(criteria, visibility));
    }

    /** Reads one row with the same data-scope predicate as the list operation. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementRow get(Long achievementId, CurrentUser requester) {
        EmploymentRateAchievementRow row = mapper.findAccessible(achievementId, visibilityFor(requester));
        if (row == null) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private EmploymentRateAchievementVisibility visibilityFor(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null) {
            throw new ForbiddenException();
        }
        List<String> roles = requester.roles();
        if (roles.contains("R01")) {
            return new EmploymentRateAchievementVisibility(
                    requester.userId(),
                    EmploymentRateAchievementVisibility.OWNER);
        }
        if (roles.contains("R02")) {
            return new EmploymentRateAchievementVisibility(
                    requester.userId(),
                    EmploymentRateAchievementVisibility.SHARED_ORGANIZATION);
        }
        if (roles.contains("R04")) {
            return new EmploymentRateAchievementVisibility(
                    requester.userId(),
                    EmploymentRateAchievementVisibility.CERTIFICATION_SCOPE);
        }
        throw new ForbiddenException();
    }
}

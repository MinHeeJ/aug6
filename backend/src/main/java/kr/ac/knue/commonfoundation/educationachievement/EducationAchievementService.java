package kr.ac.knue.commonfoundation.educationachievement;

import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Owns shared education-achievement application concerns before story-specific CRUD is added.
 *
 * <p>It centralizes request identifier normalization and the common mapper boundary so subsequent
 * flows can persist traceable state and change history without bypassing existing infrastructure.</p>
 */
@Service
public class EducationAchievementService {
    private final EducationAchievementMapper mapper;

    public EducationAchievementService(EducationAchievementMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Returns a stable request identifier supplied by the caller or creates one for traceability.
     *
     * @param suppliedRequestId optional {@code X-Request-Id} header value
     * @return a nonblank identifier suitable for response metadata and audit records
     */
    public String requestId(String suppliedRequestId) {
        if (suppliedRequestId != null && !suppliedRequestId.trim().isBlank()) {
            return suppliedRequestId.trim();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Reads active source rows through the shared persistence adapter for later typed flows.
     *
     * @param achievementType selected type from the request path
     * @return active record count for the selected type
     */
    public int countActiveByAchievementType(String achievementType) {
        return mapper.countActiveByAchievementType(achievementType);
    }
}

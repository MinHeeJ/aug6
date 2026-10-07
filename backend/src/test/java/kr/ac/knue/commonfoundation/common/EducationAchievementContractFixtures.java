package kr.ac.knue.commonfoundation.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;

/**
 * Reusable database-free role, date-boundary and JSON fixtures for education API slices.
 * Principals are test values only: authenticated database/filter tests must provision real users and grants.
 */
public final class EducationAchievementContractFixtures {
    public static final String REQUEST_ID = "education-contract-request";
    public static final LocalDate IN_PERIOD_DATE = LocalDate.of(2026, 4, 10);
    public static final LocalDate OUTSIDE_PERIOD_DATE = LocalDate.of(2025, 12, 31);

    private EducationAchievementContractFixtures() {
    }

    /** Returns a single-role principal so an administrator bypass cannot conceal a missing role guard. */
    public static CurrentUser principal(String roleCode) {
        return new CurrentUser(101L, "education-test", "E0101", "교육 실적 검증 교원", List.of(roleCode), List.of());
    }

    /** Creates the common approved request fields without an editable owner, status or identity. */
    public static ObjectNode request(ObjectMapper objectMapper, String managementItemCode) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("managementItemCode", managementItemCode);
        body.put("achievementDate", IN_PERIOD_DATE.toString());
        return body;
    }
}

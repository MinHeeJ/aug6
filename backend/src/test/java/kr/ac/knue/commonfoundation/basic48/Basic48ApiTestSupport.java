package kr.ac.knue.commonfoundation.basic48;

import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;

final class Basic48ApiTestSupport {
    private static final List<String> ROLE_CODES = List.of("R01", "R02", "R03", "R04", "R05", "R06", "R07", "R08", "R09");
    private static final List<String> READ_ONLY_FUNCTIONS = List.of("READ", "DOWNLOAD");

    private Basic48ApiTestSupport() {
    }

    static Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "B48-TEST-SESSION");
    }

    static Actor teacherR01() {
        return new Actor(new CurrentUser(2L, "professor1", "E0002", "교원", List.of("R01"), List.of()),
                DataScope.SELF,
                "KNUE-DEPT-COMP");
    }

    static Actor departmentManagerR04() {
        return new Actor(new CurrentUser(4L, "dept-manager", "E0004", "학과담당자", List.of("R04"), List.of()),
                DataScope.ORGANIZATION,
                "KNUE-DEPT-COMP");
    }

    static Actor scoreAuditR08() {
        return new Actor(new CurrentUser(8L, "score-auditor", "E0008", "점수산출감사자", List.of("R08"), List.of()),
                DataScope.ORGANIZATION,
                "KNUE-COLLEGE-EDU");
    }

    static Actor adminR09() {
        return new Actor(new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of()),
                DataScope.ALL,
                null);
    }

    static List<String> allRoleCodes() {
        return ROLE_CODES;
    }

    static List<String> readOnlyFunctionTypes() {
        return READ_ONLY_FUNCTIONS;
    }

    enum DataScope {
        SELF,
        ORGANIZATION,
        ALL
    }

    record Actor(CurrentUser currentUser, DataScope dataScope, String organizationCode) {
    }
}

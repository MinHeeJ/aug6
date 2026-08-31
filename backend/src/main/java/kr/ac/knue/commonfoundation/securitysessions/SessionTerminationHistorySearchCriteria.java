package kr.ac.knue.commonfoundation.securitysessions;

import java.time.LocalDate;

public record SessionTerminationHistorySearchCriteria(String filter, String terminationType,
        LocalDate fromDate, LocalDate toDate) {
}

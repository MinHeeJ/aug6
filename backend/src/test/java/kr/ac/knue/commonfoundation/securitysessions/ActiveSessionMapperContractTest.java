package kr.ac.knue.commonfoundation.securitysessions;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ActiveSessionMapperContractTest {
    private final String mapperXml;

    ActiveSessionMapperContractTest() throws Exception {
        mapperXml = new ClassPathResource("mapper/securitysessions/ActiveSessionMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void activeSessionListReadsOnlyActiveUnexpiredRowsAndReadonlyFields() {
        assertThat(mapperXml)
                .contains("s.status = 'ACTIVE'")
                .contains("s.expires_at &gt; CURRENT_TIMESTAMP")
                .contains("coalesce(s.login_at, s.created_at) as \"loginAt\"")
                .contains("s.last_accessed_at as \"lastAccessedAt\"")
                .contains("s.ip_address as \"ipAddress\"")
                .doesNotContain("#{criteria.status} IS NULL", "COALESCE", "? IS NULL");
    }

    @Test
    void terminateSessionMutatesOnlyTerminationColumnsAndWritesImmutableAuditRows() {
        assertThat(mapperXml)
                .contains("update sessions")
                .contains("set status = 'TERMINATED'")
                .contains("terminated_by = #{operatorUserId}")
                .contains("termination_reason = #{reason}")
                .contains("where session_id = #{sessionId}")
                .contains("and status = 'ACTIVE'")
                .contains("insert into session_termination_history")
                .contains("ADMIN_TERMINATED")
                .contains("insert into business_process_audit_logs")
                .contains("SESSION_TERMINATE")
                .contains("#{requestId}");
    }

    @Test
    void sessionTerminationHistoryListReadsImmutableHistoryWithDynamicFiltersOnly() {
        assertThat(mapperXml)
                .contains("select h.history_id as \"historyId\"")
                .contains("from session_termination_history h")
                .contains("h.termination_type as \"terminationType\"")
                .contains("h.termination_reason as \"terminationReason\"")
                .contains("h.terminated_at as \"terminatedAt\"")
                .contains("<if test=\"criteria.filter != null and criteria.filter != ''\">")
                .contains("<if test=\"criteria.terminationType != null and criteria.terminationType != ''\">")
                .contains("<if test=\"criteria.fromDate != null\">")
                .contains("<if test=\"criteria.toDate != null\">")
                .doesNotContain("update session_termination_history", "delete from session_termination_history")
                .doesNotContain("#{criteria.terminationType} IS NULL", "COALESCE", "? IS NULL");
    }
}

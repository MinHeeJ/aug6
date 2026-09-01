package kr.ac.knue.commonfoundation.basic36;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic36FoundationContractTest {
    @Test
    void phase1AddsOnlyIncrementalBasic36FlywaySkeletonForFutureBusinessTables() throws Exception {
        String sql = new ClassPathResource("db/migration/V35__basic36_business_pilot_foundation.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        String lowerSql = sql.toLowerCase();

        assertThat(lowerSql)
                .contains("basic-36")
                .contains("create table if not exists basic36_foundation_readiness")
                .contains("comment on table basic36_foundation_readiness")
                .contains("insert into basic36_foundation_readiness")
                .doesNotContain("drop table")
                .doesNotContain("alter table users")
                .doesNotContain("alter table roles");
    }

    @Test
    void phase1DocumentsCommonPreconditionDiscoveryWithoutCreatingFallbackCommonContracts() {
        Basic36FoundationContract.PreconditionReport report = Basic36FoundationContract.commonPreconditionReport();

        assertThat(report.status()).isEqualTo("READY");
        assertThat(report.preservedBehaviors()).contains(
                "기존 backend/frontend/infra/docker-compose.yml 단일 저장소 구조 재사용",
                "기존 세션 쿠키 인증 Principal 재사용",
                "기존 메뉴 URL 기반 화면 권한 guard 재사용",
                "기존 코드·감사·배치 공통 모듈 재사용");
        assertThat(report.requestedChanges()).contains(
                "BASIC-36 증분 Flyway migration skeleton 추가",
                "신규 업무 route placeholder를 React shell registry에 추가");
        assertThat(report.missingContracts()).isEmpty();
    }

    @Test
    void phase1RegistersBusinessApiPrefixesToExistingMenuGuardRoutes() {
        assertThat(Basic36FoundationContract.API_ROUTE_BY_PREFIX)
                .containsEntry("/api/admin/korus-faculty-sync-results", "/admin/korus-faculty-sync")
                .containsEntry("/api/admin/korus-faculty-sync-runs", "/admin/korus-faculty-sync")
                .containsEntry("/api/admin/full-time-faculty-statuses", "/admin/full-time-faculty-statuses")
                .containsEntry("/api/researcher-profiles", "/researcher-profiles")
                .containsEntry("/api/admin/researcher-profiles/degree-prerequisite-missing", "/admin/researcher-profiles/degree-prerequisite-missing")
                .containsEntry("/api/admin/achievement-data-histories", "/admin/achievement-data-histories")
                .containsEntry("/api/admin/achievement-data-as-of", "/admin/achievement-data-as-of");
        assertThat(Basic36FoundationContract.uiRouteForApiPath("/api/researcher-profiles/E10001/degrees"))
                .isEqualTo("/researcher-profiles/{employeeNo}");
    }
}

package kr.ac.knue.commonfoundation.basic69;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.basic60.Basic60Mapper;
import kr.ac.knue.commonfoundation.basic60.Basic60Service;
import kr.ac.knue.commonfoundation.basic60.OperationalSettingRow;
import kr.ac.knue.commonfoundation.basic60.SaveParticipationAllocationRateSettingRequest;
import org.junit.jupiter.api.Test;

/** Verifies the required lock and audit-history behavior for participation-rate saves. */
class ParticipationAllocationRateSettingsServiceTest {
    @Test
    void successfulMatrixCellSaveWritesParticipationHistoryWithTheRequestIdentifier() {
        Basic60Mapper mapper = mock(Basic60Mapper.class);
        Basic60Service service = new Basic60Service(mapper);
        SaveParticipationAllocationRateSettingRequest request = request();
        OperationalSettingRow saved = savedRow();
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedParticipationSettingImpact(request)).thenReturn(false);
        when(mapper.findParticipationSettingByKey(request)).thenReturn(null, saved);

        service.saveParticipationSetting(request, 4L, "REQ-B69-RATE-HISTORY");

        verify(mapper).upsertParticipationSetting(request, 4L);
        verify(mapper).insertChangeHistory(
                eq("participation_allocation_rate_settings"),
                eq("10:COLLEGE_EDU:B60_SEED_RATE_MATRIX:2:LEAD"),
                eq("CREATE"),
                eq("allocation_rate"),
                eq(null),
                eq("0.7000"),
                eq(4L),
                eq("배분율 매트릭스 설정"),
                eq("REQ-B69-RATE-HISTORY"));
    }

    @Test
    void confirmedRuleVersionRejectsTheSaveBeforeTheMatrixOrHistoryCanChange() {
        Basic60Mapper mapper = mock(Basic60Mapper.class);
        Basic60Service service = new Basic60Service(mapper);
        SaveParticipationAllocationRateSettingRequest request = request();
        when(mapper.findRuleVersionStatus(10L)).thenReturn("CONFIRMED");

        assertThatThrownBy(() -> service.saveParticipationSetting(request, 4L, "REQ-B69-RATE-RULE-LOCK"))
                .hasMessageContaining("CONFIRMED_RULE_LOCKED");

        verify(mapper, never()).upsertParticipationSetting(any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmedEvaluationImpactRejectsTheSaveBeforeTheMatrixOrHistoryCanChange() {
        Basic60Mapper mapper = mock(Basic60Mapper.class);
        Basic60Service service = new Basic60Service(mapper);
        SaveParticipationAllocationRateSettingRequest request = request();
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedParticipationSettingImpact(request)).thenReturn(true);

        assertThatThrownBy(() -> service.saveParticipationSetting(request, 4L, "REQ-B69-RATE-DATA-LOCK"))
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).upsertParticipationSetting(any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private SaveParticipationAllocationRateSettingRequest request() {
        return new SaveParticipationAllocationRateSettingRequest(
                10L,
                "COLLEGE_EDU",
                "RESEARCH",
                "PAPER",
                "2026",
                "AUTHORSHIP",
                "B60_SEED_RATE_MATRIX",
                2,
                "LEAD",
                new BigDecimal("0.7000"),
                "Y",
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                "배분율 매트릭스 설정");
    }

    private OperationalSettingRow savedRow() {
        return new OperationalSettingRow(2002L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "RESEARCH", "PAPER", "2026", "AUTHORSHIP", null, "B60_SEED_RATE_MATRIX", null, null, null, 2, "LEAD", new BigDecimal("0.7000"), null, null, null, "Y", null, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "배분율 매트릭스 설정", 4L, LocalDateTime.parse("2026-09-21T19:40:00"));
    }
}

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
import kr.ac.knue.commonfoundation.basic60.SaveManagementItemEvaluationScoreSettingRequest;
import org.junit.jupiter.api.Test;

/** Verifies audit and lock behavior required when a management-item score is saved. */
class ManagementItemEvaluationScoreSettingsServiceTest {
    @Test
    void successfulScoreSaveWritesTheChangeHistoryWithTheRequestIdentifier() {
        Basic60Mapper mapper = mock(Basic60Mapper.class);
        Basic60Service service = new Basic60Service(mapper);
        SaveManagementItemEvaluationScoreSettingRequest request = request();
        OperationalSettingRow saved = savedRow();
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedScoreSettingImpact(request)).thenReturn(false);
        when(mapper.findScoreSettingByKey(request)).thenReturn(null, saved);

        service.saveScoreSetting(request, 4L, "REQ-B69-SCORE-HISTORY");

        verify(mapper).upsertScoreSetting(request, 4L);
        verify(mapper).insertChangeHistory(
                eq("management_item_evaluation_score_settings"),
                eq("10:COLLEGE_EDU:B60_SEED_SCORE_LECTURE:COLLEGE-A"),
                eq("CREATE"),
                eq("evaluation_score"),
                eq(null),
                eq("92.5000"),
                eq(4L),
                eq("소속대학별 평가점수 설정"),
                eq("REQ-B69-SCORE-HISTORY"));
    }

    @Test
    void confirmedRuleVersionRejectsTheScoreSaveBeforeTheSettingOrHistoryCanChange() {
        Basic60Mapper mapper = mock(Basic60Mapper.class);
        Basic60Service service = new Basic60Service(mapper);
        SaveManagementItemEvaluationScoreSettingRequest request = request();
        when(mapper.findRuleVersionStatus(10L)).thenReturn("CONFIRMED");

        assertThatThrownBy(() -> service.saveScoreSetting(request, 4L, "REQ-B69-SCORE-RULE-LOCK"))
                .hasMessageContaining("CONFIRMED_RULE_LOCKED");

        verify(mapper, never()).upsertScoreSetting(any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmedEvaluationImpactRejectsTheScoreSaveBeforeTheSettingOrHistoryCanChange() {
        Basic60Mapper mapper = mock(Basic60Mapper.class);
        Basic60Service service = new Basic60Service(mapper);
        SaveManagementItemEvaluationScoreSettingRequest request = request();
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedScoreSettingImpact(request)).thenReturn(true);

        assertThatThrownBy(() -> service.saveScoreSetting(request, 4L, "REQ-B69-SCORE-DATA-LOCK"))
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).upsertScoreSetting(any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private SaveManagementItemEvaluationScoreSettingRequest request() {
        return new SaveManagementItemEvaluationScoreSettingRequest(
                10L,
                "COLLEGE_EDU",
                "EDUCATION",
                "LECTURE",
                "2026",
                "COURSE_GROUP",
                "B60_SEED_SCORE_LECTURE",
                "COLLEGE-A",
                "A대학",
                new BigDecimal("92.5000"),
                new BigDecimal("100.0000"),
                1,
                "Y",
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                "소속대학별 평가점수 설정");
    }

    private OperationalSettingRow savedRow() {
        return new OperationalSettingRow(3001L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026", "COURSE_GROUP", null, "B60_SEED_SCORE_LECTURE", "강의평가", "COLLEGE-A", "A대학", null, null, null, new BigDecimal("92.5000"), new BigDecimal("100.0000"), 1, "Y", null, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "소속대학별 평가점수 설정", 4L, LocalDateTime.parse("2026-09-21T19:40:00"));
    }
}

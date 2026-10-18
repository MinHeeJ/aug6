package kr.ac.knue.commonfoundation.basic69;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.basic60.Basic60Mapper;
import kr.ac.knue.commonfoundation.basic60.Basic60Service;
import kr.ac.knue.commonfoundation.basic60.OperationalSettingRow;
import kr.ac.knue.commonfoundation.basic60.SaveEvaluationElementManagementItemSettingRequest;
import org.junit.jupiter.api.Test;

/** Verifies the required audit side effect of a successful element-setting save. */
class EvaluationElementManagementItemSettingsServiceTest {
    @Test
    void successfulSaveWritesTheSettingAndItsDataChangeHistoryInTheSameOperation() {
        Basic60Mapper mapper = mock(Basic60Mapper.class);
        Basic60Service service = new Basic60Service(mapper);
        SaveEvaluationElementManagementItemSettingRequest request = request();
        OperationalSettingRow saved = savedRow();
        when(mapper.findRuleVersionStatus(10L)).thenReturn("DRAFT");
        when(mapper.hasConfirmedElementSettingImpact(request)).thenReturn(false);
        when(mapper.findElementSettingByKey(request)).thenReturn(null, saved);

        service.saveElementSetting(request, 4L, "REQ-B69-ELEMENT-HISTORY");

        verify(mapper).upsertElementSetting(request, 4L);
        verify(mapper).insertChangeHistory(
                eq("evaluation_element_management_item_settings"),
                eq("10:COLLEGE_EDU:EDUCATION:LECTURE:2026:COURSE_GROUP:ATTENDANCE"),
                eq("CREATE"),
                eq("management_item_name"),
                eq(null),
                eq("출석관리"),
                eq(4L),
                eq("항목 설정"),
                eq("REQ-B69-ELEMENT-HISTORY"));
    }

    @Test
    void confirmedRuleVersionIsRejectedBeforeTheSettingOrHistoryCanChange() {
        Basic60Mapper mapper = mock(Basic60Mapper.class);
        Basic60Service service = new Basic60Service(mapper);
        SaveEvaluationElementManagementItemSettingRequest request = request();
        when(mapper.findRuleVersionStatus(10L)).thenReturn("CONFIRMED");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.saveElementSetting(request, 4L, "REQ-B69-ELEMENT-LOCK"))
                .hasMessageContaining("CONFIRMED_RULE_LOCKED");

        verify(mapper, never()).upsertElementSetting(any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private SaveEvaluationElementManagementItemSettingRequest request() {
        return new SaveEvaluationElementManagementItemSettingRequest(
                10L, "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026", "COURSE_GROUP", "ATTENDANCE", "출석관리", 1,
                "Y", "Y", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "항목 설정");
    }

    private OperationalSettingRow savedRow() {
        return new OperationalSettingRow(1001L, 10L, "B60-DRAFT-2026", "DRAFT", "COLLEGE_EDU", "EDUCATION", "LECTURE", "2026", "COURSE_GROUP", null, "ATTENDANCE", "출석관리", null, null, null, null, null, null, null, 1, "Y", "Y", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), "N", "항목 설정", 4L, LocalDateTime.parse("2026-09-21T19:40:00"));
    }
}

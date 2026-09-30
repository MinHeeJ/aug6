package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Regression coverage for REQ-1896: an evaluation-confirmed row must remain unchanged when a mutation is attempted.
 */
class LectureEvaluationAchievementConfirmedLockTest {
    @Test
    void confirmedAchievementUpdateStopsBeforeRowOrAttachmentMutation() {
        LectureEvaluationAchievementMapper mapper = Mockito.mock(LectureEvaluationAchievementMapper.class);
        EducationAchievementFoundationService foundation = Mockito.mock(EducationAchievementFoundationService.class);
        LectureEvaluationAchievementService service = new LectureEvaluationAchievementService(mapper, foundation);
        CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());
        LectureEvaluationAchievementDtos.SaveRequest request = new LectureEvaluationAchievementDtos.SaveRequest(
                77L, "2026", "ORG-1", "B77-LE-001", LocalDate.parse("2026-03-15"), Map.of("score", 95),
                "opaque-reference", "평가확정 행 수정 시도");
        when(mapper.findById(77L)).thenReturn(new LectureEvaluationAchievementDtos.Row(
                77L, "2026", 2L, "ORG-1", "B77-LE-001", LocalDate.parse("2026-03-15"), Map.of("score", 95),
                "DRAFTING", "opaque-reference", null));

        when(foundation.validateMutation(any())).thenThrow(new ConflictException(
                "CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다."));

        assertThatThrownBy(() -> service.save(request, teacher))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        assertThatThrownBy(() -> service.saveAttachmentReference(77L,
                new LectureEvaluationAchievementDtos.AttachmentReferenceRequest(
                        "replacement-opaque-reference", "평가확정 첨부 참조 수정 시도"), teacher))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).update(eq(77L), any(), eq(2L));
        verify(mapper, never()).updateAttachmentReference(eq(77L), any(), eq(2L));
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any());
    }
}

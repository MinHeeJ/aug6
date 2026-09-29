package kr.ac.knue.commonfoundation.lectureevaluations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Regression coverage for the final-evaluation lock before any mapper mutation can occur. */
@ExtendWith(MockitoExtension.class)
class LectureEvaluationConfirmationLockTest {
    @Mock
    private LectureEvaluationAchievementMapper mapper;

    @Test
    void confirmedAchievementRejectsUpdateAndLeavesSourceAndAttachmentReferenceUnchanged() {
        LectureEvaluationAchievementService service = new LectureEvaluationAchievementService(mapper);
        LectureEvaluationAchievementSaveRequest request = confirmedUpdateRequest();
        when(mapper.find(77L)).thenReturn(confirmedRow());

        assertThatThrownBy(() -> service.save(request, facultyUser()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("평가확정");

        verify(mapper, never()).update(request, 1L);
        verify(mapper, never()).insertStatusHistory(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong());
        verify(mapper, never()).insertChangeHistory(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void excelOnlyRoleCannotBypassTheLectureEvaluationServerBoundary() {
        LectureEvaluationAchievementService service = new LectureEvaluationAchievementService(mapper);
        CurrentUser excelOnlyUser = new CurrentUser(7L, "excel-user", "E0007", "엑셀 담당자", List.of("R07"), List.of());

        assertThatThrownBy(() -> service.save(confirmedUpdateRequest(), excelOnlyUser))
                .isInstanceOf(ForbiddenException.class);

        verify(mapper, never()).find(org.mockito.ArgumentMatchers.anyLong());
    }

    private LectureEvaluationAchievementSaveRequest confirmedUpdateRequest() {
        LectureEvaluationAchievementSaveRequest request = new LectureEvaluationAchievementSaveRequest();
        request.setAchievementId(77L);
        request.setManagementItemCode("EDU_LECTURE_EVALUATION");
        request.setOrganizationCode("KNUE-COL-EDU");
        request.setOccurredDate(LocalDate.parse("2026-03-15"));
        request.setAchievementDetail("변경을 시도한 강의평가 실적");
        request.setAttachmentRef("replacement-attachment-ref");
        return request;
    }

    private LectureEvaluationAchievementRow confirmedRow() {
        return new LectureEvaluationAchievementRow(77L, "LE-77", "교원", "EDU_LECTURE_EVALUATION", "KNUE-COL-EDU",
                LocalDate.parse("2026-03-15"), "B77-LE-001 평가확정 강의평가 실적", "EVALUATION_CONFIRMED",
                "retained-attachment-ref", LocalDateTime.parse("2026-03-15T09:00:00"), 1L);
    }

    private CurrentUser facultyUser() {
        return new CurrentUser(1L, "faculty", "E0001", "교원", List.of("R01"), List.of());
    }
}

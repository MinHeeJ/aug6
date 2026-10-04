package kr.ac.knue.commonfoundation.lectureimprovements;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

/** Verifies the service lock that prevents mapper mutations of finalized lecture improvements. */
class LectureImprovementServiceTest {
    @Test
    void finalizedLectureImprovementCannotBeUpdatedAndLeavesRowsUnchanged() {
        LectureImprovementMapper mapper = org.mockito.Mockito.mock(LectureImprovementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        LectureImprovementService service = new LectureImprovementService(
                mapper,
                guardService,
                new ObjectMapper());
        CurrentUser r01 = new CurrentUser(
                101L,
                "faculty",
                "E0101",
                "교원",
                List.of("R01"),
                List.of());
        LectureImprovementRow finalized = new LectureImprovementRow(
                31L,
                "B83-LI-003",
                101L,
                "faculty",
                "KNUE-DEPT-COMP",
                "2026",
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2026-06-01"),
                "확정 강의개선 실적",
                "EVALUATION_CONFIRMED",
                null,
                "확정 강의개선 실적내용",
                2026,
                2,
                LocalDateTime.parse("2026-06-01T09:00:00"),
                LocalDateTime.parse("2026-06-01T09:00:00"));
        when(mapper.findVisible(eq(31L), eq(101L), eq(List.of("R01")))).thenReturn(finalized);
        LectureImprovementRequest request = new LectureImprovementRequest(
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2026-06-02"),
                "변경 시도",
                2026,
                2,
                List.of());

        assertThatThrownBy(() -> service.update(31L, request, r01, "REQ-B83-LI-LOCK"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).updateHeader(anyLong(), any(), any(), any(), any());
        verify(mapper, never()).updateDetail(anyLong(), any(), any(), any(), any());
        verify(mapper, never()).insertChangeHistory(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
        verify(guardService, never()).validateMutation(any(), any());
    }
}

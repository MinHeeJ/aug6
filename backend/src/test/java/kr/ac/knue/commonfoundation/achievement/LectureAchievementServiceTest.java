package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

class LectureAchievementServiceTest {
    @Test
    void savesOutsidePeriodWarningAndRequeriesPersistedRowWithHistory() {
        LectureAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureAchievementMapper.class
        );
        EducationAchievementGuard guard = org.mockito.Mockito.mock(EducationAchievementGuard.class);
        LectureAchievementService service = new LectureAchievementService(
                mapper,
                guard,
                new ObjectMapper()
        );
        LectureSaveRequest request = request();
        doAnswer(invocation -> {
            invocation.<LectureSaveRequest>getArgument(0).setAchievementId(790010L);
            return null;
        }).when(mapper).insert(any(), any(), any(), any());
        when(guard.validateMutation(any(), any()))
                .thenReturn(new EducationAchievementGuardResult(true));
        when(mapper.findById(790010L)).thenReturn(row(790010L, "DRAFT", "{\"score\":4.8}"));

        LectureAchievementModels.Row saved = service.save(request, r01());

        assertThat(saved.achievementId()).isEqualTo(790010L);
        assertThat(saved.occurredDateOutOfRangeWarning()).isTrue();
        verify(mapper).insertStatusHistory(
                790010L,
                null,
                "DRAFT",
                "CREATE",
                null,
                null,
                1001L
        );
        verify(mapper).insertChangeHistory(
                790010L,
                "CREATE",
                null,
                "{\"score\":4.8}",
                1001L,
                "강의실적 등록"
        );
    }

    @Test
    void submitsExistingAchievementAndRecordsStatusAndDataChangeHistories() {
        LectureAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureAchievementMapper.class
        );
        EducationAchievementGuard guard = org.mockito.Mockito.mock(EducationAchievementGuard.class);
        LectureAchievementService service = new LectureAchievementService(
                mapper,
                guard,
                new ObjectMapper()
        );
        LectureSaveRequest request = request();
        request.setAchievementId(790001L);
        request.setActionType("SUBMIT");
        when(mapper.findById(790001L))
                .thenReturn(
                        row(790001L, "DRAFT", "{}"),
                        row(790001L, "SUBMITTED", "{\"score\":4.8}")
                );
        when(guard.validateMutation(any(), any()))
                .thenReturn(new EducationAchievementGuardResult(false));
        when(guard.validateTransition("DRAFT", "SUBMIT", null, null))
                .thenReturn(new EducationAchievementTransition("DRAFT", "SUBMITTED", "SUBMIT", null, null));

        LectureAchievementModels.Row saved = service.save(request, r01());

        assertThat(saved.certificationStatus()).isEqualTo("SUBMITTED");
        verify(mapper).update(
                790001L,
                "EDU-LECTURE-A",
                LocalDate.of(2026, 2, 28),
                "{\"score\":4.8}",
                null,
                "SUBMITTED",
                1001L
        );
        verify(mapper).insertStatusHistory(
                790001L,
                "DRAFT",
                "SUBMITTED",
                "SUBMIT",
                null,
                null,
                1001L
        );
        verify(mapper).insertChangeHistory(
                eq(790001L),
                eq("UPDATE"),
                eq("{}"),
                eq("{\"score\":4.8}"),
                eq(1001L),
                eq("강의실적 수정")
        );
    }

    @Test
    void refusesFinalizedAchievementBeforeAnyMutation() {
        LectureAchievementMapper mapper = org.mockito.Mockito.mock(
                LectureAchievementMapper.class
        );
        EducationAchievementGuard guard = org.mockito.Mockito.mock(EducationAchievementGuard.class);
        LectureAchievementService service = new LectureAchievementService(
                mapper,
                guard,
                new ObjectMapper()
        );
        LectureSaveRequest request = request();
        request.setAchievementId(790003L);
        when(mapper.findById(790003L)).thenReturn(row(790003L, "EVALUATION_CONFIRMED", "{}"));
        when(guard.validateMutation(any(), any())).thenThrow(new ConflictException("평가확정 잠금"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.save(request, r01()))
                .isInstanceOf(ConflictException.class);

        verify(mapper, org.mockito.Mockito.never()).update(
                any(), any(), any(), any(), any(), any(), any()
        );
        verify(mapper, org.mockito.Mockito.never()).insertChangeHistory(
                any(), any(), any(), any(), any(), any()
        );
    }

    private LectureSaveRequest request() {
        LectureSaveRequest request = new LectureSaveRequest();
        request.setTeacherUserId(1001L);
        request.setOrganizationCode("KNUE-COL-EDU");
        request.setEvaluationYear("2026");
        request.setManagementItemCode("EDU-LECTURE-A");
        request.setOccurredDate(LocalDate.of(2026, 2, 28));
        request.setAchievementDetail(new ObjectMapper().createObjectNode().put("score", 4.8));
        return request;
    }

    private LectureAchievementModels.Row row(Long achievementId, String status, String detail) {
        return new LectureAchievementModels.Row(
                achievementId,
                "LA-" + achievementId,
                1001L,
                "교원",
                "KNUE-COL-EDU",
                "2026",
                "EDU-LECTURE-A",
                LocalDate.of(2026, 2, 28),
                detail,
                status,
                null,
                false,
                LocalDateTime.parse("2026-09-29T09:00:00")
        );
    }

    private CurrentUser r01() {
        return new CurrentUser(1001L, "professor1", "E1001", "교원", List.of("R01"), List.of());
    }
}

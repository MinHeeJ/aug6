package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Service tests for the transactional lecture-achievement mutation, status, attachment and audit path. */
class LectureAchievementServiceTest {
    private final CurrentUser permitted = new CurrentUser(1L, "professor1", "E1", "교원", List.of("R01"), List.of());

    @Test
    void saveCreatesLectureWithAttachmentStatusHistoryAndFieldAudit() {
        LectureAchievementMapper mapper = org.mockito.Mockito.mock(LectureAchievementMapper.class);
        LectureAchievementService service = new LectureAchievementService(mapper, new ObjectMapper());
        stubOpenMutationGate(mapper);
        doAnswer(invocation -> {
            ((LectureAchievementData) invocation.getArgument(0)).setAchievementId(91L);
            return null;
        }).when(mapper).insert(any(LectureAchievementData.class));
        when(mapper.findById(91L)).thenReturn(persisted(EducationAchievementStatus.SUBMITTED, 2));

        LectureAchievementResponse.Row saved = service.save(
                new LectureAchievementRequest(null, "LECTURE", LocalDate.parse("2026-04-10"),
                        new ObjectMapper().createObjectNode().put("courseName", "교육평가론"),
                        EducationAchievementStatus.SUBMITTED, "제출", 2, "강의실적 등록"),
                permitted, "req-81-lecture-save");

        ArgumentCaptor<EducationAchievementStatusTransition> transition = ArgumentCaptor.forClass(EducationAchievementStatusTransition.class);
        verify(mapper).record(transition.capture());
        verify(mapper).insertChangeHistory(eq(91L), eq("CREATE"), eq(null), any(), eq(1L),
                eq("강의실적 등록"), eq("req-81-lecture-save"));
        assertThat(transition.getValue().achievementType()).isEqualTo("LECTURE");
        assertThat(transition.getValue().nextStatus()).isEqualTo(EducationAchievementStatus.SUBMITTED);
        assertThat(saved.attachmentCount()).isEqualTo(2);
        assertThat(saved.certificationStatus()).isEqualTo(EducationAchievementStatus.SUBMITTED);
    }

    @Test
    void confirmedLectureCannotUpdateOrWriteHistory() {
        LectureAchievementMapper mapper = org.mockito.Mockito.mock(LectureAchievementMapper.class);
        LectureAchievementService service = new LectureAchievementService(mapper, new ObjectMapper());
        when(mapper.findById(91L)).thenReturn(persisted(EducationAchievementStatus.EVALUATION_CONFIRMED, 1));

        assertThatThrownBy(() -> service.save(
                new LectureAchievementRequest(91L, "LECTURE", LocalDate.parse("2026-04-10"),
                        new ObjectMapper().createObjectNode(), null, null, 0, "수정"),
                permitted, "req-81-confirmed"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).update(any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any());
        verify(mapper, never()).record(any());
    }

    private void stubOpenMutationGate(LectureAchievementMapper mapper) {
        when(mapper.findOrganizationCodeForUser(1L)).thenReturn("KNUE-DEPT-COMP");
        when(mapper.hasDataScope(1L, 1L, "KNUE-DEPT-COMP", null)).thenReturn(true);
        when(mapper.hasActiveInputPeriod(eq("2026"), eq("KNUE-DEPT-COMP"), eq(null), any(LocalDateTime.class))).thenReturn(true);
        when(mapper.hasEvaluationResultLock("2026", "KNUE-DEPT-COMP", null)).thenReturn(false);
        when(mapper.isWithinEvaluationDate("2026", "KNUE-DEPT-COMP", LocalDate.parse("2026-04-10"))).thenReturn(true);
    }

    private LectureAchievementData persisted(EducationAchievementStatus status, int attachmentCount) {
        LectureAchievementData row = new LectureAchievementData();
        row.setAchievementId(91L);
        row.setManagementNo("B77-LA-001");
        row.setEvaluationYear("2026");
        row.setOrganizationCode("KNUE-DEPT-COMP");
        row.setTeacherUserId(1L);
        row.setTeacherName("교원");
        row.setManagementItemCode("LECTURE");
        row.setOccurredDate(LocalDate.parse("2026-04-10"));
        row.setCertificationStatus(status);
        row.setAchievementDetail("{\"courseName\":\"교육평가론\"}");
        row.setAttachmentCount(attachmentCount);
        row.setOccurrenceDateWarning(false);
        return row;
    }
}

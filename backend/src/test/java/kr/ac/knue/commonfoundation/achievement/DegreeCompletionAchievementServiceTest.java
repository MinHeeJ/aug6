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

/** Service tests for the atomic degree-completion header, student detail and immutable audit path. */
class DegreeCompletionAchievementServiceTest {
    private final CurrentUser permitted = new CurrentUser(1L, "professor1", "E1", "교원", List.of("R01"), List.of());

    @Test
    void savePersistsAndRequeriesEveryRequiredGuidedStudentField() {
        DegreeCompletionAchievementMapper mapper = org.mockito.Mockito.mock(DegreeCompletionAchievementMapper.class);
        DegreeCompletionAchievementService service = new DegreeCompletionAchievementService(mapper, new ObjectMapper());
        stubOpenMutationGate(mapper);
        doAnswer(invocation -> {
            ((DegreeCompletionAchievementData) invocation.getArgument(0)).setAchievementId(63L);
            return null;
        }).when(mapper).insert(any(DegreeCompletionAchievementData.class));
        when(mapper.findById(63L)).thenReturn(persisted(EducationAchievementStatus.DRAFTING));

        DegreeCompletionAchievementResponse.Row saved = service.save(request(null), permitted, "req-81-degree-save");

        verify(mapper).insertStudent(eq(63L), any(DegreeCompletionStudentRequest.class), eq(1L));
        verify(mapper).insertChangeHistory(eq(63L), eq("CREATE"), eq(null), any(), eq(1L),
                eq("석·박사 배출 실적 저장"), eq("req-81-degree-save"));
        assertThat(saved.students()).singleElement().satisfies(student -> {
            assertThat(student.degreeType()).isEqualTo("MASTER");
            assertThat(student.studentName()).isEqualTo("김석사");
            assertThat(student.thesisTitle()).isEqualTo("교육평가 개선 연구");
            assertThat(student.degreeAwardedDate()).isEqualTo(LocalDate.parse("2026-02-20"));
        });
    }

    @Test
    void confirmedHeaderCannotReplaceStudentDetailsOrWriteAuditHistory() {
        DegreeCompletionAchievementMapper mapper = org.mockito.Mockito.mock(DegreeCompletionAchievementMapper.class);
        DegreeCompletionAchievementService service = new DegreeCompletionAchievementService(mapper, new ObjectMapper());
        when(mapper.findById(63L)).thenReturn(persisted(EducationAchievementStatus.EVALUATION_CONFIRMED));

        assertThatThrownBy(() -> service.save(request(63L), permitted, "req-81-degree-confirmed"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).deleteStudents(any());
        verify(mapper, never()).insertStudent(any(), any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any());
    }

    private DegreeCompletionAchievementRequest request(Long achievementId) {
        return new DegreeCompletionAchievementRequest(achievementId, "DEGREE_COMPLETION", LocalDate.parse("2026-02-20"),
                new ObjectMapper().createObjectNode().put("graduationSemester", "2026-1"), null, null, 0, null,
                List.of(new DegreeCompletionStudentRequest("MASTER", "김석사", "교육평가 개선 연구",
                        LocalDate.parse("2026-02-20"))));
    }

    private void stubOpenMutationGate(DegreeCompletionAchievementMapper mapper) {
        when(mapper.findOrganizationCodeForUser(1L)).thenReturn("KNUE-DEPT-COMP");
        when(mapper.hasDataScope(1L, 1L, "KNUE-DEPT-COMP", null)).thenReturn(true);
        when(mapper.hasActiveInputPeriod(eq("2026"), eq("KNUE-DEPT-COMP"), eq(null), any(LocalDateTime.class))).thenReturn(true);
        when(mapper.hasEvaluationResultLock("2026", "KNUE-DEPT-COMP", null)).thenReturn(false);
        when(mapper.isWithinEvaluationDate("2026", "KNUE-DEPT-COMP", LocalDate.parse("2026-02-20"))).thenReturn(true);
    }

    private DegreeCompletionAchievementData persisted(EducationAchievementStatus status) {
        DegreeCompletionAchievementData row = new DegreeCompletionAchievementData();
        row.setAchievementId(63L);
        row.setManagementNo("B77-DC-001");
        row.setEvaluationYear("2026");
        row.setOrganizationCode("KNUE-DEPT-COMP");
        row.setTeacherUserId(1L);
        row.setTeacherName("교원");
        row.setManagementItemCode("DEGREE_COMPLETION");
        row.setOccurredDate(LocalDate.parse("2026-02-20"));
        row.setCertificationStatus(status);
        row.setAchievementDetail("{\"graduationSemester\":\"2026-1\"}");
        row.setAttachmentCount(0);
        row.setOccurrenceDateWarning(false);
        row.setStudents(List.of(new DegreeCompletionStudentRow(11L, "MASTER", "김석사", "교육평가 개선 연구",
                LocalDate.parse("2026-02-20"))));
        return row;
    }
}

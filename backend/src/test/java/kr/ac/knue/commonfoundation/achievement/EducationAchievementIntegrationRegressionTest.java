package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for the final-evaluation lock at the mutation boundary. These tests prove that
 * a confirmed row is rejected before its primary row, child rows, or immutable history can be changed.
 */
class EducationAchievementIntegrationRegressionTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CurrentUser faculty = new CurrentUser(1L, "professor1", "E1", "교원", List.of("R01"), List.of());

    @Test
    void confirmedLectureEvaluationSaveReturnsConflictBeforeAnyPersistentMutation() {
        LectureEvaluationAchievementMapper mapper = org.mockito.Mockito.mock(LectureEvaluationAchievementMapper.class);
        LectureEvaluationAchievementData confirmed = lectureEvaluationData(81L);
        when(mapper.findById(81L)).thenReturn(confirmed);
        LectureEvaluationAchievementService service = new LectureEvaluationAchievementService(mapper, objectMapper);
        LectureEvaluationAchievementRequest request = new LectureEvaluationAchievementRequest(81L, "LECTURE_EVALUATION",
                LocalDate.parse("2026-04-10"), objectMapper.createObjectNode().put("score", 4.9), null, null, 0,
                "확정 자료 변경 시도");

        assertThatThrownBy(() -> service.save(request, faculty, "req-confirmed-lecture-evaluation"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).insert(any());
        verify(mapper, never()).update(any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any());
        verify(mapper, never()).record(any());
    }

    @Test
    void confirmedDegreeCompletionSaveReturnsConflictBeforeReplacingStudentDetails() {
        DegreeCompletionAchievementMapper mapper = org.mockito.Mockito.mock(DegreeCompletionAchievementMapper.class);
        DegreeCompletionAchievementData confirmed = degreeCompletionData(63L);
        when(mapper.findById(63L)).thenReturn(confirmed);
        DegreeCompletionAchievementService service = new DegreeCompletionAchievementService(mapper, objectMapper);
        DegreeCompletionAchievementRequest request = new DegreeCompletionAchievementRequest(63L, "DEGREE_COMPLETION",
                LocalDate.parse("2026-02-20"), objectMapper.createObjectNode(), null, null, 0, "확정 자료 변경 시도",
                List.of(new DegreeCompletionStudentRequest("MASTER", "김석사", "교육평가 개선 연구",
                        LocalDate.parse("2026-02-20"))));

        assertThatThrownBy(() -> service.save(request, faculty, "req-confirmed-degree"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).insert(any());
        verify(mapper, never()).update(any());
        verify(mapper, never()).deleteStudents(any());
        verify(mapper, never()).insertStudent(any(), any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any());
        verify(mapper, never()).record(any());
    }

    private LectureEvaluationAchievementData lectureEvaluationData(Long achievementId) {
        LectureEvaluationAchievementData row = new LectureEvaluationAchievementData();
        row.setAchievementId(achievementId);
        row.setCertificationStatus(EducationAchievementStatus.EVALUATION_CONFIRMED);
        row.setAchievementDetail("{\"score\":4.8}");
        return row;
    }

    private DegreeCompletionAchievementData degreeCompletionData(Long achievementId) {
        DegreeCompletionAchievementData row = new DegreeCompletionAchievementData();
        row.setAchievementId(achievementId);
        row.setCertificationStatus(EducationAchievementStatus.EVALUATION_CONFIRMED);
        row.setAchievementDetail("{}");
        return row;
    }
}

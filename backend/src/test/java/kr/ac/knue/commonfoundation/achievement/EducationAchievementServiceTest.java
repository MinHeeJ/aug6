package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Verifies the save path retains occurred-date warnings while persisting and recording audit history. */
class EducationAchievementServiceTest {
    @Test
    void savePersistsOutsideEvaluationPeriodWarningAndRecordsChangeHistory() throws Exception {
        EducationAchievementMapper mapper = Mockito.mock(EducationAchievementMapper.class);
        EducationAchievementAccessValidator validator = Mockito.mock(EducationAchievementAccessValidator.class);
        EducationAchievementService service = new EducationAchievementService(mapper, validator);
        CurrentUser teacher = new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
        LectureEvaluationAchievementRequest request = new LectureEvaluationAchievementRequest();
        request.setManagementItemCode("B77-LE-001");
        request.setOccurredDate(LocalDate.parse("2026-02-28"));
        request.setAchievementDetail(new ObjectMapper().readTree("{\"fixtureId\":\"B77-LE-001\"}"));
        when(mapper.findActiveOrganizationCodeForUser(2L)).thenReturn("KNUE-DEPT-COMP");
        doAnswer(invocation -> {
            invocation.getArgument(0, LectureEvaluationAchievementRequest.class).setAchievementId(790010L);
            return null;
        }).when(mapper).insertLectureEvaluationAchievement(any(), any(), any(), any());
        when(validator.validateWrite(eq(teacher), any(AchievementWriteContext.class)))
                .thenReturn(new EducationAchievementAccessDecision(List.of("OCCURRED_DATE_OUTSIDE_EVALUATION_PERIOD")));
        when(mapper.findLectureEvaluationAchievement(790010L)).thenReturn(savedRow());

        LectureEvaluationAchievementRow saved = service.save(request, teacher, "REQ-B77-WARNING");

        assertThat(saved.managementItemCode()).isEqualTo("B77-LE-001");
        verify(mapper).insertLectureEvaluationAchievement(eq(request), eq("2026"), eq("KNUE-DEPT-COMP"), eq(2L));
        verify(mapper).insertChangeHistory(
                eq("lecture_evaluation_achievements"),
                eq("790010"),
                eq("CREATE"),
                eq("achievement"),
                eq(null),
                eq("{\"fixtureId\":\"B77-LE-001\"}"),
                eq(2L),
                eq("REQ-B77-WARNING")
        );
    }

    @Test
    void listConvertsJdbcDateFromMapperIntoApiLocalDate() {
        EducationAchievementMapper mapper = Mockito.mock(EducationAchievementMapper.class);
        EducationAchievementAccessValidator validator = Mockito.mock(EducationAchievementAccessValidator.class);
        EducationAchievementService service = new EducationAchievementService(mapper, validator);
        CurrentUser administrator = new CurrentUser(1L, "admin", "E0001", "시스템 관리자", List.of("R09"), List.of());
        LectureEvaluationAchievementSearchCriteria criteria = new LectureEvaluationAchievementSearchCriteria(
                0, 20, null, null, null, null, null, null
        );
        Map<String, Object> row = new java.util.HashMap<>(savedRow());
        row.put("occurredDate", Date.valueOf("2026-03-15"));
        when(mapper.listLectureEvaluationAchievements(criteria)).thenReturn(List.of(row));
        when(mapper.countLectureEvaluationAchievements(criteria)).thenReturn(1L);

        LectureEvaluationAchievementSearchResponse response = service.list(criteria, administrator);

        assertThat(response.achievements().get(0).occurredDate()).isEqualTo(LocalDate.parse("2026-03-15"));
    }

    private Map<String, Object> savedRow() {
        return Map.of(
                "achievementId", 790010L,
                "managementItemCode", "B77-LE-001",
                "occurredDate", LocalDate.parse("2026-02-28"),
                "certificationStatus", "DRAFTING",
                "achievementDetail", "{\"fixtureId\":\"B77-LE-001\"}",
                "evaluationYear", "2026",
                "targetUserId", 2L,
                "organizationCode", "KNUE-DEPT-COMP"
        );
    }
}

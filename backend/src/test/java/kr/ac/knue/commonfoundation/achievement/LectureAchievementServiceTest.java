package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.junit.jupiter.api.Test;

class LectureAchievementServiceTest {
    @Test
    void saveCreatesLectureAchievementAndItsChangeHistoryInTheSameServicePath() {
        LectureAchievementMapper mapper = org.mockito.Mockito.mock(LectureAchievementMapper.class);
        EducationAchievementAccessValidator validator = org.mockito.Mockito.mock(EducationAchievementAccessValidator.class);
        LectureAchievementService service = new LectureAchievementService(
                mapper,
                validator,
                new EducationAchievementStatusTransitionPolicy(),
                new ObjectMapper()
        );
        CurrentUser teacher = new CurrentUser(101L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
        LectureAchievementRow stored = new LectureAchievementRow(
                791010L, "LA-2026-created", 101L, "홍길동", "2026", "KNUE-DEPT-COMP",
                "EDU-LECTURE", LocalDate.parse("2026-04-10"), "{}", "DRAFT", false,
                LocalDateTime.parse("2026-04-10T09:00:00"), LocalDateTime.parse("2026-04-10T09:00:00")
        );
        when(mapper.findActiveOrganizationCode(101L)).thenReturn("KNUE-DEPT-COMP");
        when(mapper.findByManagementNo(any())).thenReturn(stored);

        LectureAchievementRow saved = service.save(new SaveLectureAchievementRequest(
                null, null, null, null, "EDU-LECTURE", LocalDate.parse("2026-04-10"),
                new ObjectMapper().createObjectNode().put("courseName", "교육방법론"),
                "opaque-reference", "강의실적 등록"
        ), teacher);

        assertThat(saved.managementNo()).isEqualTo("LA-2026-created");
        verify(validator).validateMutation(eq(teacher), any(EducationAchievementMutationContext.class));
        verify(mapper).insertAchievement(
                any(), eq(101L), eq("2026"), eq("KNUE-DEPT-COMP"), eq("EDU-LECTURE"),
                eq(LocalDate.parse("2026-04-10")), any(), eq("opaque-reference"), eq(101L),
                eq("강의실적 등록")
        );
        verify(mapper).insertChangeHistory(
                eq(791010L), eq("CREATE"), eq("achievement"), eq(null), any(), eq(101L),
                eq("강의실적 등록")
        );
    }
}

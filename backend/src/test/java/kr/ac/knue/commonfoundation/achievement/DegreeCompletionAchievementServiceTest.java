package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.junit.jupiter.api.Test;

class DegreeCompletionAchievementServiceTest {
    @Test
    void savePersistsDegreeCompletionHeaderStudentsAndChangeHistoryTogether() {
        DegreeCompletionAchievementMapper mapper = org.mockito.Mockito.mock(
                DegreeCompletionAchievementMapper.class
        );
        EducationAchievementAccessValidator validator = org.mockito.Mockito.mock(
                EducationAchievementAccessValidator.class
        );
        DegreeCompletionAchievementService service = new DegreeCompletionAchievementService(
                mapper,
                validator
        );
        CurrentUser teacher = new CurrentUser(
                101L,
                "professor1",
                "E1001",
                "홍길동",
                List.of("R01"),
                List.of()
        );
        DegreeCompletionAchievementHeaderRow stored = new DegreeCompletionAchievementHeaderRow(
                792010L,
                "DC-2026-created",
                101L,
                "홍길동",
                "2026",
                "KNUE-DEPT-COMP",
                "EDU-DEGREE-COMPLETION",
                LocalDate.parse("2026-08-20"),
                "{}",
                "DRAFT",
                false,
                LocalDateTime.parse("2026-08-20T09:00:00"),
                LocalDateTime.parse("2026-08-20T09:00:00")
        );
        when(mapper.findActiveOrganizationCode(101L)).thenReturn("KNUE-DEPT-COMP");
        when(mapper.findByManagementNo(any())).thenReturn(stored);
        when(mapper.findById(792010L)).thenReturn(stored);
        when(mapper.findStudents(792010L)).thenReturn(List.of(new DegreeCompletionStudentRow(
                4L,
                "DOCTORAL",
                "학생나",
                "교육정책 분석 연구",
                LocalDate.parse("2026-08-20")
        )));

        DegreeCompletionAchievementRow saved = service.save(
                new SaveDegreeCompletionAchievementRequest(
                        null,
                        null,
                        null,
                        null,
                        "EDU-DEGREE-COMPLETION",
                        LocalDate.parse("2026-08-20"),
                        null,
                        "opaque-reference",
                        List.of(new DegreeCompletionStudentRequest(
                                "doctoral",
                                "학생나",
                                "교육정책 분석 연구",
                                LocalDate.parse("2026-08-20")
                        )),
                        "석·박사 배출 등록"
                ),
                teacher
        );

        assertThat(saved.students()).extracting(DegreeCompletionStudentRow::degreeType)
                .containsExactly("DOCTORAL");
        verify(validator).validateMutation(eq(teacher), any(EducationAchievementMutationContext.class));
        verify(mapper).insertAchievement(
                any(),
                eq(101L),
                eq("2026"),
                eq("KNUE-DEPT-COMP"),
                eq("EDU-DEGREE-COMPLETION"),
                eq(LocalDate.parse("2026-08-20")),
                eq("{}"),
                eq("opaque-reference"),
                eq(101L),
                eq("석·박사 배출 등록")
        );
        verify(mapper).insertStudent(
                eq(792010L),
                eq(new DegreeCompletionStudentRequest(
                        "DOCTORAL",
                        "학생나",
                        "교육정책 분석 연구",
                        LocalDate.parse("2026-08-20")
                )),
                eq(101L)
        );
        verify(mapper).insertChangeHistory(
                eq(792010L),
                eq("CREATE"),
                eq("achievement"),
                eq(null),
                eq(null),
                eq(101L),
                eq("석·박사 배출 등록")
        );
    }
}

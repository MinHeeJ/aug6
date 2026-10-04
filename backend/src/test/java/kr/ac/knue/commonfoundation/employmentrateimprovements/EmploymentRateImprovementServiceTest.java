package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.Test;

/** Verifies R01 detail reads are restricted to the achievement owner. */
class EmploymentRateImprovementServiceTest {
    @Test
    void getReturnsAchievementWhenR01RequesterOwnsIt() {
        EmploymentRateImprovementMapper mapper = org.mockito.Mockito.mock(
                EmploymentRateImprovementMapper.class);
        EmploymentRateImprovementService service = new EmploymentRateImprovementService(
                mapper,
                org.mockito.Mockito.mock(EducationAchievementGuardService.class));
        EmploymentRateImprovementRow achievement = row(81L, 101L);
        CurrentUser requester = r01(101L);
        when(mapper.findById(81L)).thenReturn(achievement);

        EmploymentRateImprovementRow result = service.get(81L, requester);

        assertThat(result).isSameAs(achievement);
        verify(mapper).findById(81L);
    }

    @Test
    void getRejectsR01RequesterWhenAnotherFacultyOwnsAchievement() {
        EmploymentRateImprovementMapper mapper = org.mockito.Mockito.mock(
                EmploymentRateImprovementMapper.class);
        EmploymentRateImprovementService service = new EmploymentRateImprovementService(
                mapper,
                org.mockito.Mockito.mock(EducationAchievementGuardService.class));
        when(mapper.findById(81L)).thenReturn(row(81L, 202L));

        assertThatThrownBy(() -> service.get(81L, r01(101L)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한이 없습니다.");
        verify(mapper).findById(81L);
    }

    private CurrentUser r01(Long userId) {
        return new CurrentUser(
                userId,
                "faculty-" + userId,
                "E" + userId,
                "교원",
                List.of("R01"),
                List.of());
    }

    private EmploymentRateImprovementRow row(Long achievementId, Long teacherUserId) {
        return new EmploymentRateImprovementRow(
                achievementId,
                "ERI-TEST-" + achievementId,
                teacherUserId,
                "faculty-" + teacherUserId,
                "ORG-01",
                "2026",
                "EMPLOYMENT_RATE_IMPROVEMENT",
                LocalDate.parse("2026-03-01"),
                "DRAFT",
                null,
                null,
                null,
                null,
                LocalDateTime.parse("2026-03-01T09:00:00"),
                LocalDateTime.parse("2026-03-01T09:00:00"));
    }
}

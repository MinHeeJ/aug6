package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

/** Verifies the 취업률 제고 service preserves its atomic detail and audit write contract. */
class EmploymentRateImprovementAchievementServiceTest {
    @Test
    void createWritesMasterDetailStatusAndAuditAfterTheSharedGuard() {
        EmploymentRateImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                EmploymentRateImprovementAchievementMapper.class);
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        EmploymentRateImprovementAchievementService service = new EmploymentRateImprovementAchievementService(
                mapper,
                guard,
                objectMapper());
        CurrentUser requester = r01();
        EmploymentRateImprovementRequest request = new EmploymentRateImprovementRequest(
                "EMPLOYMENT_RATE_IMPROVEMENT",
                LocalDate.parse("2026-04-10"),
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-10"),
                "2026-03-15~2026-03-20",
                List.of("B83-ATTACHMENT-001"));
        EmploymentRateImprovementAchievementEntity saved = entity(501L, "DRAFT");
        when(guard.validateMutation(eq(requester), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.findActiveOrganizationCode(101L)).thenReturn("KNUE-DEPT-COMP");
        when(mapper.insertAchievement(
                eq(101L),
                eq("KNUE-DEPT-COMP"),
                eq("2026"),
                eq("EMPLOYMENT_RATE_IMPROVEMENT"),
                eq(LocalDate.parse("2026-04-10")),
                eq("[\"B83-ATTACHMENT-001\"]"),
                eq(101L))).thenReturn(501L);
        when(mapper.findScoped(501L, 101L, List.of("R01"))).thenReturn(saved);

        EmploymentRateImprovementSaveResponse result = service.create(
                request,
                requester,
                "REQ-B83-ERI-CREATE");

        assertThat(result.achievement().achievementId()).isEqualTo(501L);
        assertThat(result.achievement().attachmentIds()).containsExactly("B83-ATTACHMENT-001");
        verify(mapper).insertDetail(
                501L,
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-10"),
                "2026-03-15~2026-03-20");
        verify(mapper).insertCreateStatusHistory(501L, "취업률 제고 실적 최초 입력", 101L);
        verify(mapper).insertChangeHistory(
                eq("501"),
                eq("CREATE"),
                eq(null),
                org.mockito.ArgumentMatchers.contains("EMPLOYMENT_RATE_IMPROVEMENT"),
                eq(101L),
                eq("취업률 제고 실적 저장"),
                eq("REQ-B83-ERI-CREATE"));
    }

    @Test
    void updateConfirmedAchievementDoesNotMutateTheMasterOrDetailRows() {
        EmploymentRateImprovementAchievementMapper mapper = org.mockito.Mockito.mock(
                EmploymentRateImprovementAchievementMapper.class);
        EducationAchievementGuardService guard = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        EmploymentRateImprovementAchievementService service = new EmploymentRateImprovementAchievementService(
                mapper,
                guard,
                objectMapper());
        CurrentUser requester = r01();
        when(mapper.findScoped(501L, 101L, List.of("R01"))).thenReturn(
                entity(501L, "EVALUATION_CONFIRMED"));

        assertThatThrownBy(() -> service.update(
                501L,
                new EmploymentRateImprovementRequest(
                        "EMPLOYMENT_RATE_IMPROVEMENT",
                        LocalDate.parse("2026-04-11"),
                        null,
                        null,
                        null,
                        List.of()),
                requester,
                "REQ-B83-ERI-CONFIRMED"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).updateAchievement(any(), any(), any(), any(), any());
        verify(mapper, never()).updateDetail(any(), any(), any(), any());
        verify(mapper, never()).insertChangeHistory(any(), any(), any(), any(), any(), any(), any());
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private CurrentUser r01() {
        return new CurrentUser(
                101L,
                "professor1",
                "E0101",
                "교원",
                List.of("R01"),
                List.of());
    }

    private EmploymentRateImprovementAchievementEntity entity(Long achievementId, String status) {
        return new EmploymentRateImprovementAchievementEntity(
                achievementId,
                101L,
                "professor1",
                "KNUE-DEPT-COMP",
                "2026",
                "EMPLOYMENT_RATE_IMPROVEMENT",
                LocalDate.parse("2026-04-10"),
                null,
                "[\"B83-ATTACHMENT-001\"]",
                status,
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-10"),
                "2026-03-15~2026-03-20",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}

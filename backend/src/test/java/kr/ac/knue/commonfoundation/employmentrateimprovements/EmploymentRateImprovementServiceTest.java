package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies guarded mapper-backed mutations for the 취업률 제고 service. */
@ExtendWith(MockitoExtension.class)
class EmploymentRateImprovementServiceTest {
    @Mock
    private EmploymentRateImprovementMapper mapper;

    @Mock
    private EducationAchievementGuardService guardService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private EmploymentRateImprovementService service;

    private final CurrentUser owner = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());

    @Test
    void updateRejectsAchievementDateOutsideExistingEvaluationYearBeforeMapperWrite() {
        when(mapper.findById(81L)).thenReturn(row());
        EmploymentRateImprovementRequest request = request(LocalDate.of(2027, 1, 2), List.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.update(81L, request, owner, "REQ-CROSS-YEAR"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("EVALUATION_YEAR_MISMATCH");

        verify(mapper, never()).update(anyLong(), any(), anyString(), anyLong());
        verify(guardService, never()).validateMutation(any(), any());
    }

    @Test
    void createRejectsDuplicateAttachmentReferencesBeforeMapperWrite() {
        EmploymentRateImprovementRequest request = request(
                LocalDate.of(2026, 4, 10),
                List.of("file-token-1", "file-token-1"));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.create(request, owner, "REQ-DUPLICATE-ATTACHMENT"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("첨부 참조값");

        verify(mapper, never()).insert(
                anyString(),
                anyLong(),
                anyString(),
                any(),
                anyString(),
                anyLong());
        verify(guardService, never()).validateMutation(any(), any());
    }

    @Test
    void createWritesRequestIdToChangeHistoryAfterMapperPersistsRecord() {
        when(guardService.validateMutation(eq(owner), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.findByManagementNo(anyString())).thenReturn(row());
        EmploymentRateImprovementRequest request = request(LocalDate.of(2026, 4, 10), List.of());

        service.create(request, owner, "REQ-CORRELATED-CREATE");

        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        verify(mapper).insertChangeHistory(
                eq("employment_rate_improvement_achievements"),
                eq("81"),
                eq("CREATE"),
                any(),
                any(),
                eq(owner.userId()),
                anyString(),
                requestId.capture());
        org.assertj.core.api.Assertions.assertThat(requestId.getValue()).isEqualTo("REQ-CORRELATED-CREATE");
    }

    private EmploymentRateImprovementRequest request(LocalDate achievementDate, List<String> attachmentIds) {
        return new EmploymentRateImprovementRequest(
                "EMPLOYMENT_IMPROVEMENT",
                achievementDate,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10),
                "2026-1 모의평가 출제",
                attachmentIds);
    }

    private EmploymentRateImprovementRow row() {
        return new EmploymentRateImprovementRow(
                81L,
                "B83-ERI-001",
                owner.userId(),
                owner.loginId(),
                "2026",
                "EMPLOYMENT_IMPROVEMENT",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10),
                "2026-1 모의평가 출제",
                "DRAFT",
                false,
                LocalDateTime.of(2026, 4, 10, 9, 0),
                LocalDateTime.of(2026, 4, 10, 9, 0));
    }
}

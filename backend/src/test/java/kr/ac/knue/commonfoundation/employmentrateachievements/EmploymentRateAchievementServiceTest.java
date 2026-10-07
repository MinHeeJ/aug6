package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.*;
import kr.ac.knue.commonfoundation.common.api.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Regression checks for generated identity, frozen evaluation year, scope, audit and policy refusal. */
class EmploymentRateAchievementServiceTest {
    private final EmploymentRateAchievementMapper mapper = mock(EmploymentRateAchievementMapper.class);
    private final EducationAchievementGuardService guard = mock(EducationAchievementGuardService.class);
    private final EducationAchievementGuardMapper guards = mock(EducationAchievementGuardMapper.class);
    private final EmploymentRateExcelRepository excel = mock(EmploymentRateExcelRepository.class);
    private final FileStoragePort storage = mock(FileStoragePort.class);
    private final EmploymentRateAchievementService service = new EmploymentRateAchievementService(
            mapper, guard, guards, excel, storage, new ObjectMapper());
    private final CurrentUser faculty = new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01"), List.of());
    private final EmploymentRateAchievementRequest request = new EmploymentRateAchievementRequest(
            "EMPLOYMENT_RATE", LocalDate.parse("2025-12-31"), "새 이름", "새 상세", null);

    private Map<String, Object> row(String state, String name) {
        return new LinkedHashMap<>(Map.of("achievementId", 10L, "teacherUserId", 101L,
                "evaluationYear", "2026", "achievementStatus", state, "achievementName", name,
                "managementItemCode", "EMPLOYMENT_RATE", "achievementDate", "2026-04-12"));
    }

    @Test
    void updatePreservesEvaluationYearAndAuditsEntireBeforeAndAfter() {
        Map<String, Object> original = row("DRAFT", "기존 이름");
        when(mapper.find(10L, true)).thenReturn(original);
        when(mapper.find(10L, false)).thenReturn(row("DRAFT", "새 이름"));
        when(guard.validateMutation(eq(faculty), any())).thenReturn(OccurredDateValidation.outsideEvaluationPeriod());
        when(excel.countItem(anyString(), eq("2026"), any())).thenReturn(1);
        when(mapper.update(anyMap())).thenReturn(1);
        var result = service.save(faculty, request, 10L, "request-update");
        assertThat(result.get("achievementDateWarning")).isEqualTo(true);
        ArgumentCaptor<EducationAchievementMutationContext> context = ArgumentCaptor.forClass(EducationAchievementMutationContext.class);
        verify(guard).validateMutation(eq(faculty), context.capture());
        assertThat(context.getValue().evaluationYear()).isEqualTo("2026");
        verify(mapper).update(argThat(r -> "2026".equals(r.get("evaluationYear"))));
        verify(mapper).history(eq(10L), contains("기존 이름"), contains("새 이름"), eq(101L),
                eq("request-update"), eq("UPDATE"));
        assertThat(original.get("achievementName")).isEqualTo("기존 이름");
    }

    @Test
    void createUsesGeneratedKeyBeforeReadingAndRecordsInitialState() {
        when(guard.validateMutation(eq(faculty), any())).thenReturn(OccurredDateValidation.accepted());
        when(excel.countItem(anyString(), anyString(), any())).thenReturn(1);
        when(mapper.organization(101L)).thenReturn("ORG-A");
        doAnswer(call -> { ((Map<String, Object>) call.getArgument(0)).put("achievementId", 10L); return null; })
                .when(mapper).insert(anyMap());
        when(mapper.find(10L, false)).thenReturn(row("DRAFT", "새 이름"));
        service.save(faculty, request, null, "create-trace");
        var order = inOrder(mapper);
        order.verify(mapper).organization(101L);
        order.verify(mapper).insert(anyMap());
        order.verify(mapper).statusHistory(10L, 101L, "create-trace");
        order.verify(mapper).find(10L, false);
        verify(mapper).history(eq(10L), isNull(), contains("새 이름"), eq(101L), eq("create-trace"), eq("CREATE"));
    }

    @Test
    void confirmedAndSubmittedCannotChangeOriginal() {
        for (String state : List.of("EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED")) {
            var original = row(state, "기존 이름");
            when(mapper.find(10L, true)).thenReturn(original);
            assertThatThrownBy(() -> service.save(faculty, request, 10L, "locked"))
                    .isInstanceOf(ConflictException.class).hasMessageContaining("CONFIRMED_DATA_LOCKED");
            assertThat(original.get("achievementName")).isEqualTo("기존 이름");
        }
        verify(mapper, never()).update(anyMap());
        verify(mapper, never()).history(anyLong(), any(), any(), anyLong(), any(), any());
    }

    @Test
    void otherOwnerCannotWriteAndReadScopeIsUnion() {
        var other = row("DRAFT", "타인");
        other.put("teacherUserId", 202L);
        when(mapper.find(10L, true)).thenReturn(other);
        assertThatThrownBy(() -> service.save(faculty, request, 10L, "other")).isInstanceOf(ForbiddenException.class);
        var multi = new CurrentUser(101L, "faculty", "E101", "교원", List.of("R01", "R02"), List.of());
        when(mapper.find(10L, false)).thenReturn(other);
        when(guards.countSharedActiveOrganization(101L, 202L)).thenReturn(1);
        assertThat(service.detail(multi, 10L).get("teacherUserId")).isEqualTo(202L);
        when(mapper.list(anyMap())).thenReturn(List.of(other));
        when(mapper.count(anyMap())).thenReturn(1L);
        var listed = service.list(multi, Map.of("managementItemCode", "EMPLOYMENT_RATE"), 0, 20);
        assertThat(listed.get("totalElements")).isEqualTo(1L);
        verify(mapper).list(argThat(q -> q.get("roles").equals(List.of("R01", "R02"))
                && "EMPLOYMENT_RATE".equals(q.get("managementItemCode"))));
        verify(mapper).count(argThat(q -> q.get("roles").equals(List.of("R01", "R02"))
                && "EMPLOYMENT_RATE".equals(q.get("managementItemCode"))));
    }

    @Test
    void bulkMissingYearAndUnapprovedPolicyNeverWrite() {
        var operator = new CurrentUser(107L, "operator", "E107", "담당", List.of("R07"), List.of());
        assertThatThrownBy(() -> service.createBulk(operator,
                new EmploymentRateAchievementService.BulkRequest(null, Map.of(), "GENERATE", true)))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> service.createBulk(operator,
                new EmploymentRateAchievementService.BulkRequest("2026", Map.of(), "GENERATE", true)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("OQ-83-01");
        verifyNoInteractions(mapper);
    }

    @Test
    void downloadDoesNotLimitToPageAndRejectsImplicitAdministrator() {
        when(mapper.list(anyMap())).thenReturn(List.of(row("DRAFT", "실적")));
        byte[] file = service.download(faculty, Map.of("page", "3", "pageSize", "20"));
        assertThat(new EmploymentRateWorkbookCodec().read(file)).hasSize(2);
        verify(mapper).list(argThat(q -> !q.containsKey("pageSize") && !q.containsKey("pageOffset")));
        var admin = new CurrentUser(1L, "admin", null, "관리자", List.of("R09"), List.of());
        assertThatThrownBy(() -> service.list(admin, Map.of(), 0, 20)).isInstanceOf(ForbiddenException.class);
    }
}

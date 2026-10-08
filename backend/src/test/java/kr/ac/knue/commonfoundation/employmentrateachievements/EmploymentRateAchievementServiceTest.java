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
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class EmploymentRateAchievementServiceTest {
    private EmploymentRateAchievementMapper mapper;
    private EducationAchievementGuardMapper guards;
    private EmploymentRateAchievementService service;
    private final CurrentUser faculty = user("R01");

    @BeforeEach
    void setup() {
        mapper = mock(EmploymentRateAchievementMapper.class);
        guards = mock(EducationAchievementGuardMapper.class);
        service = new EmploymentRateAchievementService(mapper, guards, new ObjectMapper().findAndRegisterModules()
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
        when(mapper.organization(101L)).thenReturn("KNUE-DEPT-COMP");
        when(mapper.setting(any())).thenReturn(Map.of("teacherEditablePart", "SELF_REPORT"));
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
    }

    private static CurrentUser user(String... roles) {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of(roles), List.of());
    }

    private Map<String, Object> row(String status) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("achievementId", 42L);
        p.put("teacherUserId", 101L);
        p.put("evaluationYear", "2026");
        p.put("achievementStatus", status);
        p.put("achievementName", "old");
        p.put("managementItemCode", "EMPLOYMENT_RATE_ACHIEVEMENT");
        p.put("achievementDate", LocalDate.of(2026, 4, 10));
        p.put("achievementDetail", "{}");
        return p;
    }

    private EmploymentRateAchievementRequest request(LocalDate date) {
        return new EmploymentRateAchievementRequest("EMPLOYMENT_RATE_ACHIEVEMENT", date, "new", null, null);
    }

    @Test
    void createUsesGeneratedKeyAndWritesLifecycleAndCompleteAudit() {
        doAnswer(call -> {
            Map<String, Object> p = call.getArgument(0);
            p.put("achievementId", 42L);
            return null;
        }).when(mapper).insert(any());
        when(mapper.find(Map.of("achievementId", 42L))).thenReturn(row("DRAFT"));
        service.save(null, request(LocalDate.of(2026, 4, 10)), faculty, "trace");
        var order = inOrder(mapper);
        order.verify(mapper).organization(101L);
        order.verify(mapper).lockFinalizations(any());
        order.verify(mapper).setting(any());
        order.verify(mapper).duplicates(any());
        order.verify(mapper).insert(any());
        order.verify(mapper).statusHistory(argThat(p -> Long.valueOf(42).equals(p.get("achievementId"))));
        order.verify(mapper).audit(argThat(p -> "42".equals(p.get("targetKey"))
                && p.get("afterValue").toString().contains("achievementDate")
                && "trace".equals(p.get("requestId"))));
        order.verify(mapper).find(Map.of("achievementId", 42L));
    }

    @Test
    void updatePreservesYearEvenWhenDateChangesAndAuditsAllChangedValues() {
        Map<String, Object> before = row("DRAFT");
        when(mapper.lock(any())).thenReturn(before);
        when(mapper.update(any())).thenReturn(1);
        when(mapper.find(any())).thenReturn(row("DRAFT"));
        Map<String, Object> result = service.save(42L, request(LocalDate.of(2025, 12, 31)), faculty, "trace");
        verify(guards).countActiveInputPeriods("2026", 101L);
        verify(mapper).update(argThat(p -> "2026".equals(p.get("evaluationYear"))));
        ArgumentCaptor<Map<String, Object>> audit = ArgumentCaptor.forClass(Map.class);
        verify(mapper).audit(audit.capture());
        assertThat(audit.getValue().get("beforeValue").toString()).contains("old", "2026-04-10");
        assertThat(audit.getValue().get("afterValue").toString()).contains("new", "2025-12-31");
        assertThat(result.get("occurredDateWarning")).isEqualTo(true);
        assertThat(before.get("achievementName")).isEqualTo("old");
    }

    @ParameterizedTest
    @ValueSource(strings = {"EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED", "DEPARTMENT_CONFIRMED"})
    void nonEditableStatusNeverMutates(String status) {
        Map<String, Object> original = row(status);
        when(mapper.lock(any())).thenReturn(original);
        assertThatThrownBy(() -> service.save(42L, request(LocalDate.of(2026, 4, 10)), faculty, "trace"))
                .isInstanceOf(ConflictException.class);
        assertThat(original.get("achievementName")).isEqualTo("old");
        verify(mapper, never()).update(any());
        verify(mapper, never()).audit(any());
    }

    @Test
    void otherTeacherUpdateForbiddenBeforeMutation() {
        Map<String, Object> original = row("DRAFT");
        original.put("teacherUserId", 102L);
        when(mapper.lock(any())).thenReturn(original);
        assertThatThrownBy(() -> service.save(42L, request(LocalDate.of(2026, 4, 10)), faculty, "trace"))
                .isInstanceOf(ForbiddenException.class);
        verify(mapper, never()).update(any());
    }

    @Test
    void closedPeriodRejectsCreateWithoutAudit() {
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        assertThatThrownBy(() -> service.save(null, request(LocalDate.of(2026, 4, 10)), faculty, "trace"))
                .hasMessageContaining("PERIOD_NOT_ACTIVE");
        verify(mapper, never()).insert(any());
        verify(mapper, never()).audit(any());
    }

    @Test
    void evaluationFinalizationRejectsCreate() {
        when(guards.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        assertThatThrownBy(() -> service.save(null, request(LocalDate.of(2026, 4, 10)), faculty, "trace"))
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");
        verify(mapper, never()).insert(any());
    }

    @Test
    void missingSettingAndForbiddenFieldsRejected() {
        when(mapper.setting(any())).thenReturn(null);
        assertThatThrownBy(() -> service.save(null, request(LocalDate.of(2026, 4, 10)), faculty, "trace"))
                .isInstanceOf(BusinessValidationException.class);
        when(mapper.setting(any())).thenReturn(Map.of("teacherEditablePart", "SELF_REPORT"));
        var request = new EmploymentRateAchievementRequest("EMPLOYMENT_RATE_ACHIEVEMENT", LocalDate.of(2026, 4, 10),
                "name", new ObjectMapper().createObjectNode().put("confirmed", true), null);
        assertThatThrownBy(() -> service.save(null, request, faculty, "trace"))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insert(any());
    }

    @Test
    void multiRoleDetailUsesUnionRatherThanFirstRole() {
        Map<String, Object> row = row("DRAFT");
        row.put("teacherUserId", 102L);
        when(mapper.find(any())).thenReturn(row);
        when(guards.countSharedActiveOrganization(101L, 102L)).thenReturn(1);
        assertThat(service.get(42L, user("R01", "R02"))).isEqualTo(row);
    }

    @Test
    void outsideScopeDetailForbidden() {
        Map<String, Object> row = row("DRAFT");
        row.put("teacherUserId", 102L);
        when(mapper.find(any())).thenReturn(row);
        assertThatThrownBy(() -> service.get(42L, faculty)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listAndCountReceiveSameFiltersAndUnionRoleSet() {
        Map<String, Object> filters = Map.of("page", 0, "pageSize", 20, "pageOffset", 0,
                "managementNo", "selected");
        when(mapper.list(any())).thenReturn(List.of(row("DRAFT")));
        when(mapper.count(any())).thenReturn(1L);
        Map<String, Object> result = service.list(filters, user("R01", "R02"), false);
        ArgumentCaptor<Map<String, Object>> a = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> b = ArgumentCaptor.forClass(Map.class);
        verify(mapper).list(a.capture());
        verify(mapper).count(b.capture());
        assertThat(a.getValue()).isEqualTo(b.getValue());
        assertThat(a.getValue().get("roles")).isEqualTo(List.of("R01", "R02"));
        assertThat(result.get("totalElements")).isEqualTo(1L);
    }

    @Test
    void duplicateNeverUpdatesExistingRow() {
        when(mapper.duplicates(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.save(null, request(LocalDate.of(2026, 4, 10)), faculty, "trace"))
                .hasMessageContaining("DUPLICATE_DATA");
        verify(mapper, never()).insert(any());
    }

    @Test
    void bulkPolicyIsBlockedBeforeAnyPersistence() {
        assertThatThrownBy(() -> service.createBulk(new EmploymentRateBulkJobRequest("2026", "GENERATE", null, true),
                user("R07"))).hasMessageContaining("BULK_POLICY_NOT_APPROVED");
        verifyNoInteractions(mapper);
    }

    @Test
    void retainedJobOwnershipAndNotFoundAreEnforced() {
        when(mapper.job("selected")).thenReturn(Map.of("jobId", "selected", "requesterUserId", 102L));
        when(mapper.job("missing")).thenReturn(null);
        assertThatThrownBy(() -> service.job("selected", user("R07"))).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.job("missing", user("R07"))).isInstanceOf(NotFoundException.class);
    }
}

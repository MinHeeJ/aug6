package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.functionpermissions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmploymentRateAchievementServiceTest {
    private final EmploymentRateAchievementMapper mapper = mock(EmploymentRateAchievementMapper.class);
    private final EducationAchievementGuardMapper guards = mock(EducationAchievementGuardMapper.class);
    private final FunctionPermissionMapper functions = mock(FunctionPermissionMapper.class);
    private EmploymentRateAchievementService service;
    private final CurrentUser faculty = user("R01");

    @BeforeEach
    void setup() {
        service = new EmploymentRateAchievementService(mapper, guards, functions, new ObjectMapper());
        when(functions.findByKey(anyString(), anyString(), anyString())).thenAnswer(call ->
                new FunctionPermissionRow(1L, call.getArgument(0), "취업률", call.getArgument(1),
                        "역할", call.getArgument(2), "ALLOW", null, null));
        when(mapper.countActiveItems("ITEM")).thenReturn(1);
        when(mapper.organizations(101L, LocalDate.parse("2026-04-10"))).thenReturn(List.of("ORG"));
        when(guards.countActiveInputPeriods("2026", 101L)).thenReturn(1);
        when(guards.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.find(1L, true)).thenReturn(row("DRAFT"));
        when(mapper.find(1L, false)).thenReturn(row("DRAFT"));
        when(mapper.update(anyMap())).thenReturn(1);
    }

    @Test
    void generatedHeaderKeyAndEveryCreatedFieldAreAudited() {
        doAnswer(call -> { ((Map<String, Object>) call.getArgument(0)).put("achievementId", 1L); return 1; })
                .when(mapper).insert(anyMap());
        Map<String, Object> result = service.create(input("new"), faculty, "REQ");
        assertThat(((Map<?, ?>) result.get("achievement")).containsKey("attachmentIds")).isTrue();
        ArgumentCaptor<Map<String, Object>> history = ArgumentCaptor.forClass(Map.class);
        verify(mapper, atLeast(10)).history(history.capture());
        assertThat(history.getAllValues()).allSatisfy(h -> {
            assertThat(h.get("targetKey")).isEqualTo("1");
            assertThat(h.get("requestId")).isEqualTo("REQ");
            assertThat(h.get("changeType")).isEqualTo("CREATE");
        });
    }

    @Test
    void yearAndTeacherStayImmutableEvenWhenOccurredDateChangesYear() {
        service.update(1L, new EmploymentRateAchievementRequest("ITEM", LocalDate.parse("2025-12-31"),
                "changed", List.of()), faculty, "REQ");
        ArgumentCaptor<Map<String, Object>> update = ArgumentCaptor.forClass(Map.class);
        verify(mapper).update(update.capture());
        assertThat(update.getValue()).containsEntry("evaluationYear", "2026")
                .containsEntry("teacherUserId", 101L);
        verify(guards).countActiveInputPeriods("2026", 101L);
    }

    @Test
    void confirmedAndInactivePeriodAreTypedConflictsBeforeAnyWrite() {
        when(mapper.find(1L, true)).thenReturn(row("EVALUATION_CONFIRMED"));
        assertThatThrownBy(() -> service.update(1L, input("new"), faculty, "REQ"))
                .isInstanceOf(EducationAchievementConflictException.class)
                .extracting("code").isEqualTo("CONFIRMED_DATA_LOCKED");
        when(mapper.find(1L, true)).thenReturn(row("DRAFT"));
        when(guards.countActiveInputPeriods("2026", 101L)).thenReturn(0);
        assertThatThrownBy(() -> service.update(1L, input("new"), faculty, "REQ"))
                .isInstanceOf(EducationAchievementConflictException.class)
                .extracting("code").isEqualTo("PERIOD_NOT_ACTIVE");
        verify(mapper, never()).update(anyMap());
        verify(mapper, never()).history(anyMap());
    }

    @Test
    void onlyDraftAndRejectedAreEditableAndFinalizationAlsoLocksDraft() {
        for (String status : List.of("SUBMITTED", "DEPARTMENT_CONFIRMED", "CERTIFIED")) {
            when(mapper.find(1L, true)).thenReturn(row(status));
            assertThatThrownBy(() -> service.update(1L, input("new"), faculty, "REQ"))
                    .isInstanceOf(ConflictException.class);
        }
        when(mapper.find(1L, true)).thenReturn(row("DRAFT"));
        when(guards.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        assertThatThrownBy(() -> service.update(1L, input("new"), faculty, "REQ"))
                .isInstanceOf(EducationAchievementConflictException.class);
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void multipleRolesUseUnionAndR07CannotReadDetail() {
        Map<String, Object> other = row("DRAFT");
        other.put("teacherUserId", 202L);
        when(mapper.find(1L, false)).thenReturn(other);
        when(guards.countCertificationScope(101L, 202L)).thenReturn(1);
        CurrentUser multi = new CurrentUser(101L, "u", "E", "name", List.of("R01", "R04"), List.of());
        assertThat(service.detail(1L, multi)).containsEntry("teacherUserId", 202L);
        assertThatThrownBy(() -> service.detail(1L, user("R07"))).isInstanceOf(ForbiddenException.class);
        assertThat(service.detail(1L, user("R09"))).containsEntry("teacherUserId", 202L);
    }

    @Test
    void r07DownloadUsesCertificationMappingNotSharedOrganization() {
        when(mapper.countMappedOrganizations(101L)).thenReturn(0);
        assertThatThrownBy(() -> service.downloadRows(0, 20, user("R07")))
                .isInstanceOf(ForbiddenException.class);
        when(mapper.countMappedOrganizations(101L)).thenReturn(1);
        when(mapper.list(anyMap())).thenReturn(List.of());
        service.downloadRows(0, 20, user("R07"));
        ArgumentCaptor<Map<String, Object>> scope = ArgumentCaptor.forClass(Map.class);
        verify(mapper).list(scope.capture());
        assertThat(scope.getValue()).containsEntry("certification", true)
                .containsEntry("self", false).containsEntry("department", false);
    }

    @Test
    void bulkIsRoleThenValidationThenConflictWithoutPersistence() {
        EmploymentRateBulkJobRequest valid = new EmploymentRateBulkJobRequest("2026", "GENERATE", Map.of());
        assertThatThrownBy(() -> service.createBulk(valid, faculty, "REQ")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.createBulk(new EmploymentRateBulkJobRequest("bad", "X", Map.of()),
                user("R07"), "REQ")).isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> service.createBulk(valid, user("R07"), "REQ")).isInstanceOf(ConflictException.class);
        verifyNoInteractions(mapper, guards);
    }

    @Test
    void jobResultRequiresOwnerAndChecksEachTargetScope() {
        when(mapper.job("JOB")).thenReturn(new LinkedHashMap<>(Map.of("requestedBy", 202L)));
        assertThatThrownBy(() -> service.job("JOB", user("R07"))).isInstanceOf(ForbiddenException.class);
        when(mapper.job("JOB")).thenReturn(new LinkedHashMap<>(Map.of("requestedBy", 101L)));
        when(mapper.jobItems("JOB")).thenReturn(List.of(Map.of("targetUserId", 202L)));
        assertThatThrownBy(() -> service.job("JOB", user("R07"))).isInstanceOf(ForbiddenException.class);
        when(guards.countCertificationScope(101L, 202L)).thenReturn(1);
        assertThat(service.job("JOB", user("R07"))).containsKey("items");
    }

    @Test
    void ambiguousOrganizationAndUnverifiedAttachmentsAreRejected() {
        when(mapper.organizations(anyLong(), any())).thenReturn(List.of("A", "B"));
        assertThatThrownBy(() -> service.create(input("new"), faculty, "REQ"))
                .isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> service.create(new EmploymentRateAchievementRequest("ITEM",
                LocalDate.parse("2026-04-10"), "new", List.of("token")), faculty, "REQ"))
                .isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).insert(anyMap());
    }

    @Test
    void updateAuditsEveryChangedFieldInSameCommandAndRejectedIsEditable() {
        when(mapper.find(1L, true)).thenReturn(row("CERTIFICATION_REJECTED"));
        service.update(1L, input("changed"), faculty, "REQ-HISTORY");
        ArgumentCaptor<Map<String, Object>> history = ArgumentCaptor.forClass(Map.class);
        verify(mapper).history(history.capture());
        assertThat(history.getValue()).containsEntry("fieldName", "achievementName")
                .containsEntry("beforeValue", "old").containsEntry("afterValue", "changed")
                .containsEntry("requestId", "REQ-HISTORY").containsEntry("targetKey", "1");
    }

    @Test
    void deniedFunctionCannotWriteButFinalization409IsNotHiddenByDeny() {
        when(functions.findByKey(anyString(), anyString(), eq("UPDATE"))).thenReturn(
                new FunctionPermissionRow(1L, "SCR-EMPLOYMENT-RATE-ACHIEVEMENT", "screen", "R01", "role",
                        "UPDATE", "DENY", null, null));
        assertThatThrownBy(() -> service.update(1L, input("new"), faculty, "REQ"))
                .isInstanceOf(ForbiddenException.class);
        when(guards.countEvaluationConfirmations(101L, "2026")).thenReturn(1);
        assertThatThrownBy(() -> service.update(1L, input("new"), faculty, "REQ"))
                .isInstanceOf(EducationAchievementConflictException.class);
        verify(mapper, never()).update(anyMap());
    }

    @Test
    void missingAndOutOfScopeRowsAreNotExposedAndPaginationIsValidated() {
        when(mapper.find(99L, false)).thenReturn(null);
        assertThatThrownBy(() -> service.detail(99L, faculty)).isInstanceOf(NotFoundException.class);
        Map<String, Object> other = row("DRAFT");
        other.put("teacherUserId", 202L);
        when(mapper.find(1L, false)).thenReturn(other);
        assertThatThrownBy(() -> service.detail(1L, faculty)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.list(0, 10, faculty)).isInstanceOf(BusinessValidationException.class);
        verify(mapper, never()).list(anyMap());
    }

    @Test
    void listAndTotalUseIdenticalUnionScopeAndRequestedPage() {
        CurrentUser multiple = new CurrentUser(101L, "u", "E", "교원", List.of("R01", "R02", "R04"), List.of());
        when(mapper.list(anyMap())).thenReturn(List.of(row("DRAFT")));
        when(mapper.count(anyMap())).thenReturn(51L);
        Map<String, Object> result = service.list(1, 50, multiple);
        ArgumentCaptor<Map<String, Object>> listed = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> counted = ArgumentCaptor.forClass(Map.class);
        verify(mapper).list(listed.capture());
        verify(mapper).count(counted.capture());
        assertThat(counted.getValue()).isEqualTo(listed.getValue())
                .containsEntry("self", true).containsEntry("department", true)
                .containsEntry("certification", true).containsEntry("offset", 50L)
                .containsEntry("pageSize", 50);
        assertThat(result).containsEntry("totalElements", 51L).containsEntry("page", 1);
    }

    private static EmploymentRateAchievementRequest input(String name) {
        return new EmploymentRateAchievementRequest("ITEM", LocalDate.parse("2026-04-10"), name, List.of());
    }

    static CurrentUser user(String role) {
        return new CurrentUser(101L, "u", "E101", "교원", List.of(role), List.of());
    }

    private static Map<String, Object> row(String status) {
        return new LinkedHashMap<>(Map.of("achievementId", 1L, "teacherUserId", 101L,
                "evaluationYear", "2026", "organizationCode", "ORG", "managementItemCode", "ITEM",
                "achievementDate", "2026-04-10", "achievementName", "old", "attachmentRef", "[]",
                "achievementStatus", status));
    }
}

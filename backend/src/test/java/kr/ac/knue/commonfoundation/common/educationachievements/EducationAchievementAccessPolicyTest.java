package kr.ac.knue.commonfoundation.common.educationachievements;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Exercises real shared admission rules; only existing persistence/permission ports are doubled. */
class EducationAchievementAccessPolicyTest {
    private final EducationAchievementGuardMapper mapper = org.mockito.Mockito.mock(
            EducationAchievementGuardMapper.class);
    private final EffectivePermissionService menus = org.mockito.Mockito.mock(EffectivePermissionService.class);
    private final FunctionPermissionService functions = org.mockito.Mockito.mock(FunctionPermissionService.class);
    private final EducationAchievementAccessPolicy policy =
            new EducationAchievementAccessPolicy(mapper, menus, functions);

    @Test
    void anonymousReaderIsUnauthenticated() {
        assertThatThrownBy(() -> policy.requireRead(null)).isInstanceOf(UnauthenticatedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"R01", "R02", "R04", "R09"})
    void admittedRolesCanRead(String role) {
        assertThatCode(() -> policy.requireRead(user(role))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"R03", "R05", "R06", "R07", "R08"})
    void otherRolesCannotRead(String role) {
        assertThatThrownBy(() -> policy.requireRead(user(role))).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void teacherCanMutateOwnAchievement() {
        assertThatCode(() -> policy.requireMutation(user("R01"), 101L)).doesNotThrowAnyException();
    }

    @Test
    void teacherCannotMutateAnotherOwner() {
        assertThatThrownBy(() -> policy.requireMutation(user("R01"), 202L)).isInstanceOf(ForbiddenException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"R02", "R04", "R07"})
    void readAndOperatorRolesCannotPerformIndividualMutation(String role) {
        assertThatThrownBy(() -> policy.requireMutation(user(role), 101L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void r07CanDownloadButNotReadDetail() {
        assertThatCode(() -> policy.requireDownload(user("R07"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireRead(user("R07"))).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void r07CanUseExcel() {
        assertThatCode(() -> policy.requireExcel(user("R07"))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"R01", "R02", "R04"})
    void businessReadersCannotUseExcel(String role) {
        assertThatThrownBy(() -> policy.requireExcel(user(role))).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void uploadOwnerCannotReadAnotherOperatorsDiagnostics() {
        assertThatThrownBy(() -> policy.requireUploadOwner(user("R07"), 202L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void uploadOwnerCanReadOwnDiagnostics() {
        assertThatCode(() -> policy.requireUploadOwner(user("R07"), 101L)).doesNotThrowAnyException();
    }

    @Test
    void selfScopeNeedsNoOrganizationQuery() {
        policy.requireReadScope(user("R01"), 101L);
        verify(mapper, never()).countSharedActiveOrganization(any(), any());
        verify(mapper, never()).countCertificationScope(any(), any());
    }

    @Test
    void departmentScopeAllowsMember() {
        when(mapper.countSharedActiveOrganization(101L, 202L)).thenReturn(1);
        assertThatCode(() -> policy.requireReadScope(user("R02"), 202L)).doesNotThrowAnyException();
    }

    @Test
    void certificationScopeAllowsAssignedTarget() {
        when(mapper.countCertificationScope(101L, 202L)).thenReturn(1);
        assertThatCode(() -> policy.requireReadScope(user("R04"), 202L)).doesNotThrowAnyException();
    }

    @Test
    void multiRoleReadUnionsSelfAndAssignedScopes() {
        when(mapper.countCertificationScope(101L, 202L)).thenReturn(1);
        assertThatCode(() -> policy.requireReadScope(user("R01", "R02", "R04"), 202L))
                .doesNotThrowAnyException();
        verify(mapper).countCertificationScope(101L, 202L);
    }

    @Test
    void outOfScopeReadIsForbidden() {
        assertThatThrownBy(() -> policy.requireReadScope(user("R02", "R04"), 202L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void operatorDownloadRequiresDatabaseScope() {
        assertThatThrownBy(() -> policy.requireDownloadScope(user("R07"), 202L))
                .isInstanceOf(ForbiddenException.class);
        when(mapper.countCertificationScope(101L, 202L)).thenReturn(1);
        assertThatCode(() -> policy.requireDownloadScope(user("R07"), 202L)).doesNotThrowAnyException();
    }

    @Test
    void menuDenialPreventsFunctionEvaluation() {
        assertThatThrownBy(() -> requireReadFunction(user("R01"))).isInstanceOf(ForbiddenException.class);
        verify(functions, never()).evaluate(any());
    }

    @Test
    void functionDenialIsNotBypassedByMenuGrant() {
        CurrentUser user = user("R01");
        when(menus.canAccess(101L, user.roles(), route())).thenReturn(true);
        when(functions.evaluate(any())).thenThrow(new ForbiddenException());
        assertThatThrownBy(() -> requireReadFunction(user)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void allowedFunctionUsesExactScreenRoleAndStatus() {
        CurrentUser user = user("R01");
        when(menus.canAccess(101L, user.roles(), route())).thenReturn(true);
        requireReadFunction(user);
        verify(functions).evaluate(new FunctionPermissionEvaluateRequest(
                "SCR-COURSE-OPERATIONS", "R01", "READ", "DRAFT", null));
    }

    @Test
    void functionRoleUnionKeepsSecondRolesGrant() {
        CurrentUser user = user("R01", "R04");
        when(menus.canAccess(101L, user.roles(), route())).thenReturn(true);
        when(functions.evaluate(new FunctionPermissionEvaluateRequest(
                "SCR-COURSE-OPERATIONS", "R01", "READ", "DRAFT", null))).thenThrow(new ForbiddenException());
        assertThatCode(() -> requireReadFunction(user)).doesNotThrowAnyException();
        verify(functions).evaluate(new FunctionPermissionEvaluateRequest(
                "SCR-COURSE-OPERATIONS", "R04", "READ", "DRAFT", null));
    }

    @Test
    void finalizationConflictPrecedesFunctionDenial() {
        assertThatThrownBy(() -> policy.requireFunction(
                user("R01"), "SCR-COURSE-OPERATIONS", route(), "UPDATE", "EVALUATION_CONFIRMED", Set.of("R01")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("CONFIRMED_DATA_LOCKED");
        verify(functions, never()).evaluate(any());
    }

    @Test
    void administratorOverrideAdmitsEveryOperationAndScope() {
        CurrentUser administrator = user("R09");
        assertThatCode(() -> {
            policy.requireReadScope(administrator, 202L);
            policy.requireDownloadScope(administrator, 202L);
            policy.requireMutation(administrator, 202L);
            policy.requireUploadOwner(administrator, 202L);
            policy.requireFunction(administrator, "SCR-COURSE-OPERATIONS", route(), "UPDATE", "DRAFT", Set.of("R01"));
        }).doesNotThrowAnyException();
        verify(mapper, never()).countCertificationScope(any(), any());
        verify(functions, never()).evaluate(any());
    }

    @Test
    void administratorCannotBypassConfirmedLock() {
        assertThatThrownBy(() -> policy.requireFunction(
                user("R09"), "SCR-COURSE-OPERATIONS", route(), "UPDATE", "EVALUATION_CONFIRMED", Set.of("R01")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("CONFIRMED_DATA_LOCKED");
    }

    private void requireReadFunction(CurrentUser user) {
        policy.requireFunction(user, "SCR-COURSE-OPERATIONS", route(), "READ", "DRAFT", Set.of("R01", "R02", "R04"));
    }

    private String route() {
        return "/faculty/education/course-operations";
    }

    private CurrentUser user(String... roles) {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of(roles), List.of());
    }
}

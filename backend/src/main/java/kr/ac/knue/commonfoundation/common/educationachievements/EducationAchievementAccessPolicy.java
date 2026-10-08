package kr.ac.knue.commonfoundation.common.educationachievements;

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
import org.springframework.stereotype.Service;

/**
 * Narrows admission for the four new education resources and reuses existing menu,
 * function, and database scope checks. The explicit administrator override affects
 * authorization only; input periods and finalization locks remain mandatory.
 */
@Service
public class EducationAchievementAccessPolicy {
    private static final Set<String> READ_ROLES = Set.of("R01", "R02", "R04");
    private static final Set<String> DOWNLOAD_ROLES = Set.of("R01", "R02", "R04", "R07");
    private final EducationAchievementGuardMapper scopeMapper;
    private final EffectivePermissionService menuPermissions;
    private final FunctionPermissionService functionPermissions;

    public EducationAchievementAccessPolicy(
            EducationAchievementGuardMapper scopeMapper,
            EffectivePermissionService menuPermissions,
            FunctionPermissionService functionPermissions) {
        this.scopeMapper = scopeMapper;
        this.menuPermissions = menuPermissions;
        this.functionPermissions = functionPermissions;
    }

    /** Admits list/detail only; callers must also scope list SQL or call requireReadScope. */
    public void requireRead(CurrentUser user) {
        requireRole(user, READ_ROLES);
    }

    /** Admits individual writes only for the owning teacher, except the requested R09 override. */
    public void requireMutation(CurrentUser user, Long teacherUserId) {
        requireRole(user, Set.of("R01"));
        if (!isAdministrator(user) && !user.userId().equals(teacherUserId)) {
            throw new ForbiddenException();
        }
    }

    /** Excel validation, commit, diagnostics and bulk operations have separate admission. */
    public void requireExcel(CurrentUser user) {
        requireRole(user, Set.of("R07"));
    }

    /** Download admission intentionally does not grant R07 list or detail access. */
    public void requireDownload(CurrentUser user) {
        requireRole(user, DOWNLOAD_ROLES);
    }

    /** Unions self, department and certification scopes for a multi-role reader. */
    public void requireReadScope(CurrentUser user, Long teacherUserId) {
        requireRead(user);
        requireScope(user, teacherUserId, false);
    }

    /** Download and importer target checks include the R07 assigned business scope. */
    public void requireDownloadScope(CurrentUser user, Long teacherUserId) {
        requireDownload(user);
        requireScope(user, teacherUserId, true);
    }

    /** Retained upload/job ownership must be checked independently of target data scope. */
    public void requireUploadOwner(CurrentUser user, Long uploaderUserId) {
        requireExcel(user);
        if (!isAdministrator(user) && !user.userId().equals(uploaderUserId)) {
            throw new ForbiddenException();
        }
    }

    /**
     * ANDs menu and function grants with operation admission performed by the caller.
     * A typed finalization conflict precedes the legacy evaluator's generic 403.
     */
    public void requireFunction(
            CurrentUser user,
            String screenId,
            String uiRoute,
            String functionType,
            String targetStatus,
            Set<String> operationRoles) {
        requireRole(user, operationRoles);
        if (Set.of("CREATE", "UPDATE", "DELETE", "EXECUTE").contains(functionType)) {
            requireMutableStatus(targetStatus);
        }
        if (isAdministrator(user)) {
            return;
        }
        if (!menuPermissions.canAccess(user.userId(), user.roles(), uiRoute)) {
            throw new ForbiddenException();
        }
        for (String role : user.roles()) {
            if (!operationRoles.contains(role)) {
                continue;
            }
            try {
                functionPermissions.evaluate(new FunctionPermissionEvaluateRequest(
                        screenId, role, functionType, targetStatus == null ? "DRAFT" : targetStatus, null));
                return;
            } catch (ForbiddenException deniedRole) {
                // Another admitted role may have a function grant; scopes are also a union.
            }
        }
        throw new ForbiddenException();
    }

    /** Must be called on the row locked by the writing service, including for administrators. */
    public void requireMutableStatus(String achievementStatus) {
        if ("EVALUATION_CONFIRMED".equals(achievementStatus)) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 변경할 수 없습니다.");
        }
    }

    private void requireScope(CurrentUser user, Long teacherUserId, boolean includeDepartmentOperator) {
        if (isAdministrator(user)) {
            return;
        }
        List<String> roles = user.roles();
        if (roles.contains("R01") && user.userId().equals(teacherUserId)) {
            return;
        }
        if (roles.contains("R02")
                && scopeMapper.countSharedActiveOrganization(user.userId(), teacherUserId) > 0) {
            return;
        }
        if ((roles.contains("R04") || (includeDepartmentOperator && roles.contains("R07")))
                && scopeMapper.countCertificationScope(user.userId(), teacherUserId) > 0) {
            return;
        }
        throw new ForbiddenException();
    }

    private void requireRole(CurrentUser user, Set<String> allowedRoles) {
        if (user == null || user.userId() == null) {
            throw new UnauthenticatedException();
        }
        if (!isAdministrator(user)
                && (user.roles() == null || user.roles().stream().noneMatch(allowedRoles::contains))) {
            throw new ForbiddenException();
        }
    }

    private boolean isAdministrator(CurrentUser user) {
        return user.roles() != null && user.roles().contains("R09");
    }
}

package kr.ac.knue.commonfoundation.userroles;

import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleManagementMapper {
    java.util.List<UserRoleAssignmentSummary> listAssignments(@Param("criteria") UserRoleAssignmentSearchCriteria criteria);
    int countAssignments(@Param("criteria") UserRoleAssignmentSearchCriteria criteria);
    java.util.List<UserRoleAssignmentSummary> listCurrentUserRoles(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);
    int countCurrentUserRoles(@Param("userId") Long userId);
    UserRoleAssignmentSummary findAssignmentById(@Param("assignmentId") Long assignmentId);
    int existsUser(@Param("userId") Long userId);
    int existsActiveRole(@Param("roleCode") String roleCode);
    int countOverlappingActiveAssignment(@Param("assignmentId") Long assignmentId, @Param("userId") Long userId, @Param("roleCode") String roleCode, @Param("validStartDate") LocalDate validStartDate, @Param("validEndDate") LocalDate validEndDate);
    int insertAssignment(@Param("userId") Long userId, @Param("roleCode") String roleCode, @Param("assignmentType") String assignmentType, @Param("validStartDate") LocalDate validStartDate, @Param("validEndDate") LocalDate validEndDate, @Param("approverUserId") Long approverUserId, @Param("changeReason") String changeReason);
    int updateAssignment(@Param("assignmentId") Long assignmentId, @Param("userId") Long userId, @Param("roleCode") String roleCode, @Param("assignmentType") String assignmentType, @Param("validStartDate") LocalDate validStartDate, @Param("validEndDate") LocalDate validEndDate, @Param("approverUserId") Long approverUserId, @Param("changeReason") String changeReason);
    int revokeAssignment(@Param("assignmentId") Long assignmentId, @Param("revokedBy") Long revokedBy, @Param("changeReason") String changeReason);
}

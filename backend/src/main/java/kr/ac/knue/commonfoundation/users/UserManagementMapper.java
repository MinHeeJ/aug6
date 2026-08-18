package kr.ac.knue.commonfoundation.users;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserManagementMapper {
    List<UserSummary> searchUsers(@Param("criteria") UserSearchCriteria criteria);
    UserSummary findUserById(@Param("userId") Long userId);
    int countUsers(@Param("criteria") UserSearchCriteria criteria);
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
    List<UserRoleSummary> findCurrentRolesByUserId(@Param("userId") Long userId);
    List<AvailableRole> findAvailableRoles();
    int existsUser(@Param("userId") Long userId);
    int countActiveRoleCodes(@Param("roleCodes") List<String> roleCodes);
    int updateUserAccount(@Param("userId") Long userId, @Param("systemUseYn") String systemUseYn, @Param("changeReason") String changeReason, @Param("updatedBy") Long updatedBy);
    int endManualRoles(@Param("userId") Long userId, @Param("changeReason") String changeReason);
    int insertManualRole(@Param("userId") Long userId, @Param("roleCode") String roleCode, @Param("validStartDate") LocalDate validStartDate, @Param("validEndDate") LocalDate validEndDate, @Param("approverUserId") Long approverUserId, @Param("changeReason") String changeReason);
}

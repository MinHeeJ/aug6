package kr.ac.knue.commonfoundation.roles;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleManagementMapper {
    List<RoleRow> listRoles(@Param("criteria") RoleSearchCriteria criteria);
    RoleRow findRoleByCode(@Param("roleCode") String roleCode);
    int updateRole(
            @Param("roleCode") String roleCode,
            @Param("roleName") String roleName,
            @Param("purpose") String purpose,
            @Param("assignmentCriteria") String assignmentCriteria,
            @Param("defaultDataScope") String defaultDataScope,
            @Param("updatedBy") Long updatedBy,
            @Param("changeReason") String changeReason);
}

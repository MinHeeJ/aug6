package kr.ac.knue.commonfoundation.codes;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CodeGroupManagementMapper {
    List<CodeGroupRow> listCodeGroups(@Param("criteria") CodeGroupSearchCriteria criteria);
    CodeGroupRow findCodeGroupById(@Param("groupId") String groupId);
    int insertCodeGroup(
            @Param("groupId") String groupId,
            @Param("groupName") String groupName,
            @Param("description") String description,
            @Param("managingDepartment") String managingDepartment,
            @Param("systemUseYn") String systemUseYn,
            @Param("createdBy") Long createdBy,
            @Param("changeReason") String changeReason);
    int updateCodeGroup(
            @Param("groupId") String groupId,
            @Param("groupName") String groupName,
            @Param("description") String description,
            @Param("managingDepartment") String managingDepartment,
            @Param("systemUseYn") String systemUseYn,
            @Param("updatedBy") Long updatedBy,
            @Param("changeReason") String changeReason);
}

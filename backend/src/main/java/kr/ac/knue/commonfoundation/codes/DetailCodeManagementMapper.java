package kr.ac.knue.commonfoundation.codes;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DetailCodeManagementMapper {
    List<DetailCodeRow> listDetailCodes(@Param("criteria") DetailCodeSearchCriteria criteria);
    CodeGroupRow findCodeGroupById(@Param("groupId") String groupId);
    DetailCodeRow findDetailCode(@Param("groupId") String groupId, @Param("codeValue") String codeValue);
    int countChildDetailCodes(@Param("groupId") String groupId, @Param("codeValue") String codeValue);
    int insertDetailCode(
            @Param("groupId") String groupId,
            @Param("codeValue") String codeValue,
            @Param("codeName") String codeName,
            @Param("parentCodeValue") String parentCodeValue,
            @Param("sortOrder") int sortOrder,
            @Param("additionalAttributes") String additionalAttributes,
            @Param("systemUseYn") String systemUseYn,
            @Param("validStartDate") LocalDate validStartDate,
            @Param("validEndDate") LocalDate validEndDate,
            @Param("createdBy") Long createdBy,
            @Param("changeReason") String changeReason);
    int updateDetailCode(
            @Param("groupId") String groupId,
            @Param("codeValue") String codeValue,
            @Param("codeName") String codeName,
            @Param("parentCodeValue") String parentCodeValue,
            @Param("sortOrder") int sortOrder,
            @Param("systemUseYn") String systemUseYn,
            @Param("validStartDate") LocalDate validStartDate,
            @Param("validEndDate") LocalDate validEndDate,
            @Param("updatedBy") Long updatedBy,
            @Param("changeReason") String changeReason);
}

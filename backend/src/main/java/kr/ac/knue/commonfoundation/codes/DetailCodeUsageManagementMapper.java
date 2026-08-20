package kr.ac.knue.commonfoundation.codes;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DetailCodeUsageManagementMapper {
    CodeGroupRow findCodeGroupById(@Param("groupId") String groupId);
    List<DetailCodeUsageRow> listDetailCodeUsageSettings(@Param("groupId") String groupId,
                                                          @Param("limit") int limit,
                                                          @Param("offset") int offset);
    long countDetailCodeUsageSettings(@Param("groupId") String groupId);
    List<DetailCodeUsageRow> listSelectableDetailCodesForNewInput(@Param("groupId") String groupId);
    DetailCodeUsageRow findDetailCodeUsageSetting(@Param("groupId") String groupId,
                                                   @Param("codeValue") String codeValue);
    int updateDetailCodeUsageSetting(@Param("groupId") String groupId,
                                     @Param("codeValue") String codeValue,
                                     @Param("systemUseYn") String systemUseYn,
                                     @Param("validStartDate") LocalDate validStartDate,
                                     @Param("validEndDate") LocalDate validEndDate,
                                     @Param("updatedBy") Long updatedBy,
                                     @Param("changeReason") String changeReason);
}

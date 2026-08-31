package kr.ac.knue.commonfoundation.basic34;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface JournalIndexingInfoMapper {
    List<JournalIndexingInfoRow> listJournalIndexingInfos(@Param("criteria") JournalIndexingInfoSearchCriteria criteria);
    long countJournalIndexingInfos(@Param("criteria") JournalIndexingInfoSearchCriteria criteria);
    String findRuleVersionStatus(@Param("ruleVersionId") Long ruleVersionId);
    JournalIndexingInfoRow findByKey(@Param("request") SaveJournalIndexingInfoRequest request);
    long countOverlappingIssnPeriods(@Param("request") SaveJournalIndexingInfoRequest request);
    void upsertJournalIndexingInfo(@Param("request") SaveJournalIndexingInfoRequest request, @Param("updatedBy") Long updatedBy);
    void insertChangeHistory(@Param("targetBusiness") String targetBusiness,
                             @Param("targetKey") String targetKey,
                             @Param("changeType") String changeType,
                             @Param("fieldName") String fieldName,
                             @Param("beforeValue") String beforeValue,
                             @Param("afterValue") String afterValue,
                             @Param("changedBy") Long changedBy,
                             @Param("changeReason") String changeReason,
                             @Param("requestId") String requestId);
}

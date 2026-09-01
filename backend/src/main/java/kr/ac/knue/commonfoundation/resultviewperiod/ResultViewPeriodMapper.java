package kr.ac.knue.commonfoundation.resultviewperiod;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ResultViewPeriodMapper {
    List<ResultViewPeriodRow> listResultViewPeriods(@Param("criteria") ResultViewPeriodSearchCriteria criteria);
    long countResultViewPeriods(@Param("criteria") ResultViewPeriodSearchCriteria criteria);
    ResultViewPeriodRow findResultViewPeriodById(@Param("settingId") Long settingId);
    ResultViewPeriodRow insertResultViewPeriod(@Param("request") SaveResultViewPeriodRequest request, @Param("userId") Long userId);
    ResultViewPeriodRow updateResultViewPeriod(@Param("request") SaveResultViewPeriodRequest request, @Param("userId") Long userId);
    int countOverlappingResultViewPeriods(@Param("settingId") Long settingId,
                                           @Param("evaluationYear") String evaluationYear,
                                           @Param("collegeOrganizationCode") String collegeOrganizationCode,
                                           @Param("departmentOrganizationCode") String departmentOrganizationCode,
                                           @Param("visibilityScope") String visibilityScope,
                                           @Param("viewStartAt") LocalDateTime viewStartAt,
                                           @Param("viewEndAt") LocalDateTime viewEndAt);
    int existsAuthorizedEvaluationOrganization(@Param("userId") Long userId, @Param("organizationCode") String organizationCode);
    ResultViewPeriodRow findActiveResultViewPeriodForAccess(@Param("organizationCode") String organizationCode,
                                                             @Param("visibilityScope") String visibilityScope,
                                                             @Param("requestAt") LocalDateTime requestAt);
    void insertChangeHistory(@Param("targetBusiness") String targetBusiness, @Param("targetKey") String targetKey,
                             @Param("changeType") String changeType, @Param("fieldName") String fieldName,
                             @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
                             @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason,
                             @Param("requestId") String requestId);
}

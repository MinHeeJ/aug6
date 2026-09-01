package kr.ac.knue.commonfoundation.appealperiod;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppealPeriodMapper {
    List<AppealPeriodRow> listAppealPeriods(@Param("criteria") AppealPeriodSearchCriteria criteria);
    long countAppealPeriods(@Param("criteria") AppealPeriodSearchCriteria criteria);
    AppealPeriodRow findAppealPeriodById(@Param("settingId") Long settingId);
    AppealPeriodRow insertAppealPeriod(@Param("request") SaveAppealPeriodRequest request, @Param("userId") Long userId);
    AppealPeriodRow updateAppealPeriod(@Param("request") SaveAppealPeriodRequest request, @Param("userId") Long userId);
    int countOverlappingAppealPeriods(@Param("settingId") Long settingId,
                                      @Param("evaluationYear") String evaluationYear,
                                      @Param("collegeOrganizationCode") String collegeOrganizationCode,
                                      @Param("departmentOrganizationCode") String departmentOrganizationCode,
                                      @Param("appealStartAt") LocalDateTime appealStartAt,
                                      @Param("appealEndAt") LocalDateTime appealEndAt);
    int existsAuthorizedEvaluationOrganization(@Param("userId") Long userId, @Param("organizationCode") String organizationCode);
    int existsHandlerUserForAppealPeriod(@Param("handlerUserId") Long handlerUserId,
                                         @Param("collegeOrganizationCode") String collegeOrganizationCode,
                                         @Param("departmentOrganizationCode") String departmentOrganizationCode);
    AppealPeriodRow findActiveAppealPeriodForSubmission(@Param("applicantOrganizationCode") String applicantOrganizationCode,
                                                        @Param("requestAt") LocalDateTime requestAt);
    int countAppealContentRowsForSetting(@Param("settingId") Long settingId);
    void insertChangeHistory(@Param("targetBusiness") String targetBusiness, @Param("targetKey") String targetKey,
                             @Param("changeType") String changeType, @Param("fieldName") String fieldName,
                             @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
                             @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason,
                             @Param("requestId") String requestId);
}

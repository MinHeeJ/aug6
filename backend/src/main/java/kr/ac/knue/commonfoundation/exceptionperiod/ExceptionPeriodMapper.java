package kr.ac.knue.commonfoundation.exceptionperiod;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExceptionPeriodMapper {
    List<ExceptionPeriodRow> listExceptionPeriods(@Param("criteria") ExceptionPeriodSearchCriteria criteria);
    long countExceptionPeriods(@Param("criteria") ExceptionPeriodSearchCriteria criteria);
    ExceptionPeriodRow findExceptionPeriodById(@Param("settingId") Long settingId);
    ExceptionPeriodRow insertExceptionPeriod(@Param("request") SaveExceptionPeriodRequest request, @Param("userId") Long userId);
    ExceptionPeriodRow updateExceptionPeriod(@Param("request") SaveExceptionPeriodRequest request, @Param("userId") Long userId);
    int countOverlappingExceptionPeriods(@Param("settingId") Long settingId,
                                          @Param("evaluationYear") String evaluationYear,
                                          @Param("teacherUserId") Long teacherUserId,
                                          @Param("areaCode") String areaCode,
                                          @Param("targetFunctionCode") String targetFunctionCode,
                                          @Param("exceptionStartAt") LocalDateTime exceptionStartAt,
                                          @Param("exceptionEndAt") LocalDateTime exceptionEndAt);
    int existsTeacherUser(@Param("teacherUserId") Long teacherUserId);
    int existsEvaluationArea(@Param("areaCode") String areaCode);
    ExceptionPeriodRow findActiveExceptionPeriodForModification(@Param("teacherUserId") Long teacherUserId,
                                                                @Param("areaCode") String areaCode,
                                                                @Param("targetFunctionCode") String targetFunctionCode,
                                                                @Param("requestAt") LocalDateTime requestAt);
    int countActiveModificationPeriods(@Param("evaluationYear") String evaluationYear,
                                       @Param("areaCode") String areaCode,
                                       @Param("organizationCode") String organizationCode,
                                       @Param("requestAt") LocalDateTime requestAt);
    void insertChangeHistory(@Param("targetBusiness") String targetBusiness, @Param("targetKey") String targetKey,
                             @Param("changeType") String changeType, @Param("fieldName") String fieldName,
                             @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
                             @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason,
                             @Param("requestId") String requestId);
}

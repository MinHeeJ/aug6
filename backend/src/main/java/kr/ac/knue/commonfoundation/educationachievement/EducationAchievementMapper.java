package kr.ac.knue.commonfoundation.educationachievement;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for education-achievement rows and their workflow history. */
@Mapper
public interface EducationAchievementMapper {
    List<EducationAchievementRow> list(@Param("criteria") EducationAchievementSearchCriteria criteria,
                                       @Param("ownerUserId") Long ownerUserId);

    long count(@Param("criteria") EducationAchievementSearchCriteria criteria, @Param("ownerUserId") Long ownerUserId);

    int functionPermissionAllowed(@Param("screenId") String screenId, @Param("roles") List<String> roles,
                                  @Param("functionType") String functionType);

    int activeInputPeriodExists(@Param("evaluationYear") String evaluationYear);

    Long insertAchievement(@Param("achievementKey") String achievementKey, @Param("achievementType") String achievementType,
                           @Param("evaluationYear") String evaluationYear, @Param("ownerUserId") Long ownerUserId,
                           @Param("managementItemCode") String managementItemCode, @Param("occurrenceDate") java.time.LocalDate occurrenceDate);

    void insertManagementValue(@Param("achievementId") Long achievementId, @Param("managementItemCode") String managementItemCode,
                               @Param("valueText") String valueText, @Param("userId") Long userId);

    void updateAchievement(@Param("achievementId") Long achievementId, @Param("managementItemCode") String managementItemCode,
                           @Param("occurrenceDate") java.time.LocalDate occurrenceDate, @Param("userId") Long userId);

    void replaceManagementValue(@Param("achievementId") Long achievementId, @Param("managementItemCode") String managementItemCode,
                                @Param("valueText") String valueText, @Param("userId") Long userId);

    void deleteDegreeCompletionStudentDetails(@Param("achievementId") Long achievementId);

    void deleteStudentGuidanceDetails(@Param("achievementId") Long achievementId);

    void insertStudentGuidanceDetail(@Param("achievementId") Long achievementId, @Param("studentName") String studentName,
                                     @Param("guidanceStartDate") java.time.LocalDate guidanceStartDate,
                                     @Param("guidanceEndDate") java.time.LocalDate guidanceEndDate,
                                     @Param("studentCount") int studentCount, @Param("userId") Long userId);

    List<StudentGuidanceDetail> findStudentGuidanceDetails(@Param("achievementId") Long achievementId);

    void insertDegreeCompletionStudentDetail(@Param("achievementId") Long achievementId, @Param("degreeType") String degreeType,
                                             @Param("studentName") String studentName, @Param("thesisTitle") String thesisTitle,
                                             @Param("degreeAwardedOn") java.time.LocalDate degreeAwardedOn, @Param("userId") Long userId);

    List<DegreeCompletionStudentDetail> findDegreeCompletionStudentDetails(@Param("achievementId") Long achievementId);

    EducationAchievementRow findById(@Param("achievementId") Long achievementId);

    EducationAchievementRow findByIdForUpdate(@Param("achievementId") Long achievementId);

    int transitionExists(@Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus,
                         @Param("roles") List<String> roles);

    void updateStatus(@Param("achievementId") Long achievementId, @Param("nextStatus") String nextStatus,
                      @Param("userId") Long userId, @Param("reason") String reason);

    void logicallyDeleteAchievement(@Param("achievementId") Long achievementId, @Param("userId") Long userId,
                                    @Param("changeReason") String changeReason);

    void insertStatusHistory(@Param("achievementId") Long achievementId, @Param("previousStatus") String previousStatus,
                             @Param("nextStatus") String nextStatus, @Param("processedBy") Long processedBy,
                             @Param("reason") String reason, @Param("requestId") String requestId);

    EducationAchievementStatusHistory findLatestStatusHistory(@Param("achievementId") Long achievementId);

    void insertChangeHistory(@Param("targetKey") String targetKey, @Param("changeType") String changeType,
                             @Param("fieldName") String fieldName, @Param("beforeValue") String beforeValue,
                             @Param("afterValue") String afterValue, @Param("changedBy") Long changedBy,
                             @Param("changeReason") String changeReason, @Param("requestId") String requestId);
}

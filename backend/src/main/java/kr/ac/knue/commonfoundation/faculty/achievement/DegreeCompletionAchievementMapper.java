package kr.ac.knue.commonfoundation.faculty.achievement;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Persistence boundary for degree-completion headers, their repeated student details, and the
 * existing common data-change audit trail.
 */
@Mapper
public interface DegreeCompletionAchievementMapper {
    List<DegreeCompletionAchievementRow> list(@Param("criteria") DegreeCompletionSearchCriteria criteria);

    long count(@Param("criteria") DegreeCompletionSearchCriteria criteria);

    DegreeCompletionAchievementRow findByManagementNo(@Param("managementNo") String managementNo);

    void insertAchievement(@Param("request") SaveDegreeCompletionAchievementRequest request,
                           @Param("employeeNo") String employeeNo,
                           @Param("createdBy") Long createdBy);

    void deleteStudents(@Param("achievementId") Long achievementId);

    void insertStudents(@Param("achievementId") Long achievementId,
                        @Param("students") List<DegreeCompletionStudentRequest> students,
                        @Param("createdBy") Long createdBy);

    void insertChangeHistory(@Param("targetKey") String targetKey,
                             @Param("changeType") String changeType,
                             @Param("changedBy") Long changedBy,
                             @Param("requestId") String requestId);
}

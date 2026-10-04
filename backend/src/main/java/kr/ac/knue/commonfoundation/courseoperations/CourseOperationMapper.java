package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for course-operation achievement reads and atomic lifecycle writes. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> list(
            @Param("pageSize") int pageSize,
            @Param("offset") int offset,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long count(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    CourseOperationRow findScoped(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    CourseOperationRow findById(@Param("achievementId") Long achievementId);

    String findPrimaryOrganizationCode(@Param("userId") Long userId);

    Long insertAchievement(
            @Param("teacherUserId") Long teacherUserId,
            @Param("organizationCode") String organizationCode,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("performanceDetails") String performanceDetails,
            @Param("attachmentIdsJson") String attachmentIdsJson,
            @Param("createdBy") Long createdBy);

    void updateAchievement(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("performanceDetails") String performanceDetails,
            @Param("attachmentIdsJson") String attachmentIdsJson,
            @Param("updatedBy") Long updatedBy);

    void updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("performanceDetails") String performanceDetails);

    void insertAchievementDetail(
            @Param("achievementId") Long achievementId,
            @Param("performanceDetails") String performanceDetails);

    void insertStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("previousStatus") String previousStatus,
            @Param("nextStatus") String nextStatus,
            @Param("processedBy") Long processedBy);

    void insertChangeHistory(
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("requestId") String requestId);
}

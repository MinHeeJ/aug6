package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for course-operation headers, details, and audit history. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> listCourseOperations(
            @Param("criteria") CourseOperationSearchCriteria criteria,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    long countCourseOperations(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    CourseOperationRow findCourseOperation(@Param("achievementId") Long achievementId);

    int countReadableScope(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    String findActiveOrganizationCode(@Param("userId") Long userId);

    Long insertCourseOperation(
            @Param("managementNo") String managementNo,
            @Param("teacherUserId") Long teacherUserId,
            @Param("organizationCode") String organizationCode,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentIds") String attachmentIds,
            @Param("createdBy") Long createdBy);

    void insertCourseOperationDetail(
            @Param("achievementId") Long achievementId,
            @Param("performanceDetails") String performanceDetails);

    int updateCourseOperationHeader(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("attachmentIds") String attachmentIds,
            @Param("updatedBy") Long updatedBy);

    int updateCourseOperationDetail(
            @Param("achievementId") Long achievementId,
            @Param("performanceDetails") String performanceDetails);

    void insertStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("previousStatus") String previousStatus,
            @Param("nextStatus") String nextStatus,
            @Param("actionType") String actionType,
            @Param("opinion") String opinion,
            @Param("processedBy") Long processedBy);

    void insertChangeHistory(
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("requestId") String requestId);
}

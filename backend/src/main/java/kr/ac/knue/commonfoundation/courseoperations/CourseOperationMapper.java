package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persistence boundary for course-operation headers, details, and audit history. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> list(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    long count(
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    CourseOperationRow findById(@Param("achievementId") Long achievementId);

    CourseOperationRow findScopedById(
            @Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId,
            @Param("roles") List<String> roles);

    CourseOperationRow findByManagementNo(@Param("managementNo") String managementNo);

    Long insertHeader(
            @Param("managementNo") String managementNo,
            @Param("teacherUserId") Long teacherUserId,
            @Param("evaluationYear") String evaluationYear,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentIds") String attachmentIds,
            @Param("createdBy") Long createdBy);

    void insertDetail(
            @Param("achievementId") Long achievementId,
            @Param("performanceDetails") String performanceDetails,
            @Param("createdBy") Long createdBy);

    void insertInitialStatusHistory(
            @Param("achievementId") Long achievementId,
            @Param("processedBy") Long processedBy);

    void updateHeader(
            @Param("achievementId") Long achievementId,
            @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("achievementName") String achievementName,
            @Param("attachmentIds") String attachmentIds,
            @Param("updatedBy") Long updatedBy);

    void updateDetail(
            @Param("achievementId") Long achievementId,
            @Param("performanceDetails") String performanceDetails,
            @Param("updatedBy") Long updatedBy);

    void insertChangeHistory(
            @Param("targetKey") String targetKey,
            @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy,
            @Param("changeReason") String changeReason);
}

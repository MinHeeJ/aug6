package kr.ac.knue.commonfoundation.employmentrateimprovement;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis persistence boundary for 취업률 제고 실적 reads, writes, and audit side effects. */
@Mapper
public interface EmploymentRateImprovementMapper {
    List<EmploymentRateImprovementRow> list(@Param("pageSize") int pageSize, @Param("offset") int offset,
            @Param("requesterUserId") Long requesterUserId, @Param("roles") List<String> roles);
    long count(@Param("requesterUserId") Long requesterUserId, @Param("roles") List<String> roles);
    EmploymentRateImprovementRow findScoped(@Param("achievementId") Long achievementId,
            @Param("requesterUserId") Long requesterUserId, @Param("roles") List<String> roles);
    EmploymentRateImprovementRow findByManagementNo(@Param("managementNo") String managementNo);
    EmploymentRateImprovementRow findById(@Param("achievementId") Long achievementId);
    void insert(@Param("managementNo") String managementNo, @Param("targetUserId") Long targetUserId,
            @Param("evaluationYear") String evaluationYear, @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod,
            @Param("attachmentIds") String attachmentIds,
            @Param("createdBy") Long createdBy);
    void update(@Param("achievementId") Long achievementId, @Param("managementItemCode") String managementItemCode,
            @Param("achievementDate") LocalDate achievementDate,
            @Param("specialLectureStartDate") LocalDate specialLectureStartDate,
            @Param("specialLectureEndDate") LocalDate specialLectureEndDate,
            @Param("mockExamQuestionPeriod") String mockExamQuestionPeriod,
            @Param("attachmentIds") String attachmentIds,
            @Param("updatedBy") Long updatedBy);
    void insertStatusHistory(@Param("achievementId") Long achievementId, @Param("processedBy") Long processedBy);
    void insertChangeHistory(@Param("targetKey") String targetKey, @Param("changeType") String changeType,
            @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue,
            @Param("changedBy") Long changedBy, @Param("requestId") String requestId);
}

package kr.ac.knue.commonfoundation.educationachievements;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Persistence boundary for BASIC-72 tables; SQL is kept in the mapper XML for safe dynamic predicates. */
@Mapper
public interface EducationAchievementMapper {
    List<EducationAchievement> listByType(@Param("achievementType") String achievementType, @Param("facultyUserId") Long facultyUserId, @Param("offset") int offset, @Param("size") int size);
    long countByType(@Param("achievementType") String achievementType, @Param("facultyUserId") Long facultyUserId);
    void insertAchievement(@Param("achievementType") String achievementType, @Param("facultyUserId") Long facultyUserId, @Param("managementItemCode") String managementItemCode, @Param("occurredOn") LocalDate occurredOn, @Param("detailContent") String detailContent);
    EducationAchievement findLatestByType(@Param("achievementType") String achievementType, @Param("facultyUserId") Long facultyUserId);
    EducationAchievement findById(@Param("achievementId") Long achievementId, @Param("facultyUserId") Long facultyUserId);
    int updateAchievement(@Param("achievementId") Long achievementId, @Param("facultyUserId") Long facultyUserId, @Param("managementItemCode") String managementItemCode, @Param("occurredOn") LocalDate occurredOn, @Param("detailContent") String detailContent);
    void insertChangeHistory(@Param("targetBusiness") String targetBusiness, @Param("targetKey") String targetKey, @Param("changedBy") Long changedBy, @Param("requestId") String requestId);
    void insertUploadHistory(@Param("uploadedBy") Long uploadedBy, @Param("fileName") String fileName, @Param("totalCount") int totalCount, @Param("successCount") int successCount, @Param("failureCount") int failureCount, @Param("errorFileRef") String errorFileRef);
    Long findLatestUploadHistoryId(@Param("uploadedBy") Long uploadedBy);
}

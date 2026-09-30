package kr.ac.knue.commonfoundation.achievement;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis boundary for lecture achievement rows and their immutable change history.
 */
@Mapper
public interface LectureAchievementMapper {
    List<LectureAchievementDtos.Row> list(@Param("criteria") LectureAchievementDtos.SearchCriteria criteria,
                                                    @Param("userId") Long userId);
    long count(@Param("criteria") LectureAchievementDtos.SearchCriteria criteria, @Param("userId") Long userId);
    LectureAchievementDtos.Row findById(@Param("achievementId") Long achievementId);
    Long insert(@Param("request") LectureAchievementDtos.SaveRequest request,
                @Param("ownerUserId") Long ownerUserId, @Param("createdBy") Long createdBy);
    int update(@Param("achievementId") Long achievementId, @Param("request") LectureAchievementDtos.SaveRequest request,
               @Param("updatedBy") Long updatedBy);
    int updateAttachmentReference(@Param("achievementId") Long achievementId, @Param("attachmentReference") String attachmentReference,
                                  @Param("updatedBy") Long updatedBy);
    int insertChangeHistory(@Param("targetKey") String targetKey, @Param("changeType") String changeType,
                            @Param("fieldName") String fieldName, @Param("beforeValue") String beforeValue,
                            @Param("afterValue") String afterValue, @Param("changedBy") Long changedBy,
                            @Param("changeReason") String changeReason);
}

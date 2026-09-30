package kr.ac.knue.commonfoundation.achievement;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis boundary for lecture-evaluation achievement rows and their immutable change history.
 */
@Mapper
public interface LectureEvaluationAchievementMapper {
    List<LectureEvaluationAchievementDtos.Row> list(@Param("criteria") LectureEvaluationAchievementDtos.SearchCriteria criteria,
                                                    @Param("userId") Long userId);
    long count(@Param("criteria") LectureEvaluationAchievementDtos.SearchCriteria criteria, @Param("userId") Long userId);
    LectureEvaluationAchievementDtos.Row findById(@Param("achievementId") Long achievementId);
    Long insert(@Param("request") LectureEvaluationAchievementDtos.SaveRequest request,
                @Param("ownerUserId") Long ownerUserId, @Param("createdBy") Long createdBy);
    int update(@Param("achievementId") Long achievementId, @Param("request") LectureEvaluationAchievementDtos.SaveRequest request,
               @Param("updatedBy") Long updatedBy);
    int updateAttachmentReference(@Param("achievementId") Long achievementId, @Param("attachmentReference") String attachmentReference,
                                  @Param("updatedBy") Long updatedBy);
    int insertChangeHistory(@Param("targetKey") String targetKey, @Param("changeType") String changeType,
                            @Param("fieldName") String fieldName, @Param("beforeValue") String beforeValue,
                            @Param("afterValue") String afterValue, @Param("changedBy") Long changedBy,
                            @Param("changeReason") String changeReason);
}

package kr.ac.knue.commonfoundation.achievement;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis boundary for FR-028 header, detail, and change-history persistence. */
@Mapper
public interface DegreeCompletionMapper {
    List<DegreeCompletionDtos.Header> list(@Param("criteria") DegreeCompletionDtos.SearchCriteria criteria, @Param("userId") Long userId);
    long count(@Param("criteria") DegreeCompletionDtos.SearchCriteria criteria, @Param("userId") Long userId);
    DegreeCompletionDtos.Header findHeader(@Param("achievementId") Long achievementId);
    List<DegreeCompletionDtos.Student> listStudents(@Param("achievementId") Long achievementId);
    int insertAchievement(@Param("request") DegreeCompletionDtos.SaveRequest request, @Param("ownerUserId") Long ownerUserId, @Param("createdBy") Long createdBy);
    Long findCreatedId(@Param("request") DegreeCompletionDtos.SaveRequest request, @Param("ownerUserId") Long ownerUserId);
    int updateAchievement(@Param("request") DegreeCompletionDtos.SaveRequest request, @Param("updatedBy") Long updatedBy);
    int deleteStudents(@Param("achievementId") Long achievementId);
    int insertStudent(@Param("achievementId") Long achievementId, @Param("student") DegreeCompletionDtos.Student student, @Param("userId") Long userId);
    int insertChangeHistory(@Param("targetKey") String targetKey, @Param("changeType") String changeType, @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue, @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason);
}

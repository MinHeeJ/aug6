package kr.ac.knue.commonfoundation.courseareagroupgrades;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis persistence boundary for published course-area group grade projections and their read audit.
 */
@Mapper
public interface CourseAreaGroupGradeMapper {
    List<CourseAreaGroupGradeItem> listPublishedGrades(@Param("criteria") CourseAreaGroupGradeSearchCriteria criteria);

    long countPublishedGrades(@Param("criteria") CourseAreaGroupGradeSearchCriteria criteria);

    void insertQueryAudit(
            @Param("targetKey") String targetKey,
            @Param("changedBy") Long changedBy,
            @Param("requestId") String requestId);
}

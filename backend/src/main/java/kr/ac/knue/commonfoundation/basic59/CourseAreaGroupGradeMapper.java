package kr.ac.knue.commonfoundation.basic59;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseAreaGroupGradeMapper {
    List<CourseAreaGroupGradeRow> listCourseAreaGroupGrades(@Param("criteria") CourseAreaGroupGradeSearchCriteria criteria);

    long countCourseAreaGroupGrades(@Param("criteria") CourseAreaGroupGradeSearchCriteria criteria);

    CourseAreaGroupGradeRow findCourseAreaGroupGradeById(@Param("gradeId") Long gradeId);

    void insertDataAccessHistory(@Param("targetBusiness") String targetBusiness,
                                 @Param("viewerUserId") Long viewerUserId,
                                 @Param("targetScope") String targetScope,
                                 @Param("accessPurpose") String accessPurpose,
                                 @Param("requestId") String requestId);
}

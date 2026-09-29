package kr.ac.knue.commonfoundation.studentguidance;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** MyBatis boundary for student-guidance headers and normalized student details. */
@Mapper
public interface StudentGuidanceAchievementMapper {
    List<StudentGuidanceAchievementRow> list(@Param("limit") int limit, @Param("offset") int offset);
    long count();
    StudentGuidanceAchievementRow find(@Param("achievementId") Long achievementId);
    void insert(@Param("request") StudentGuidanceAchievementSaveRequest request, @Param("year") String year, @Param("userId") Long userId);
    void update(@Param("request") StudentGuidanceAchievementSaveRequest request, @Param("userId") Long userId);
    void deleteStudents(@Param("achievementId") Long achievementId);
    void insertStudent(@Param("achievementId") Long achievementId, @Param("student") StudentGuidanceStudentRequest student);
    List<StudentGuidanceStudentRequest> findStudents(@Param("achievementId") Long achievementId);
    int countExistingStudent(@Param("studentNo") String studentNo, @Param("occurredDate") java.time.LocalDate occurredDate);
}

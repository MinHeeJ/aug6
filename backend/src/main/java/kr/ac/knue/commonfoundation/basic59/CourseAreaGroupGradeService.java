package kr.ac.knue.commonfoundation.basic59;

import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseAreaGroupGradeService {
    private final CourseAreaGroupGradeMapper mapper;

    public CourseAreaGroupGradeService(CourseAreaGroupGradeMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public CourseAreaGroupGradeSearchResponse list(CourseAreaGroupGradeSearchCriteria criteria, CurrentUser user, String requestId) {
        CourseAreaGroupGradeSearchCriteria scopedCriteria = scopedCriteria(criteria, user);
        CourseAreaGroupGradeSearchResponse response = new CourseAreaGroupGradeSearchResponse(
                mapper.listCourseAreaGroupGrades(scopedCriteria),
                scopedCriteria.safePage(),
                scopedCriteria.safeSize(),
                mapper.countCourseAreaGroupGrades(scopedCriteria));
        mapper.insertDataAccessHistory("COURSE_AREA_GROUP_GRADE", user.userId(), targetScope(scopedCriteria), "교과영역 그룹평가 성적 목록 조회", requestId);
        return response;
    }

    @Transactional
    public CourseAreaGroupGradeRow getDetail(Long gradeId, CurrentUser user, String requestId) {
        if (gradeId == null || gradeId <= 0) {
            throw new NotFoundException("교과영역 그룹평가 성적을 찾을 수 없습니다.");
        }
        CourseAreaGroupGradeRow row = mapper.findCourseAreaGroupGradeById(gradeId);
        if (row == null || !"Y".equals(row.publishedYn())) {
            throw new NotFoundException("교과영역 그룹평가 성적을 찾을 수 없습니다.");
        }
        if (isProfessor(user) && !user.userId().equals(row.teacherUserId())) {
            throw new ForbiddenException();
        }
        mapper.insertDataAccessHistory("COURSE_AREA_GROUP_GRADE_DETAIL", user.userId(), "gradeId=" + gradeId, "교과영역 그룹평가 성적 상세 조회", requestId);
        return row;
    }

    private CourseAreaGroupGradeSearchCriteria scopedCriteria(CourseAreaGroupGradeSearchCriteria criteria, CurrentUser user) {
        if (isProfessor(user)) {
            return criteria.withTeacherUserId(user.userId());
        }
        return criteria;
    }

    private boolean isProfessor(CurrentUser user) {
        return user.roles().contains("R01") && !user.roles().contains("R04");
    }

    private String targetScope(CourseAreaGroupGradeSearchCriteria criteria) {
        StringBuilder scope = new StringBuilder("teacherUserId=");
        scope.append(criteria.effectiveTeacherUserId() == null ? "ALL" : criteria.effectiveTeacherUserId());
        if (criteria.normalizedCompletionTypeCode() != null) scope.append(";completionTypeCode=").append(criteria.normalizedCompletionTypeCode());
        if (criteria.normalizedSemesterCode() != null) scope.append(";semesterCode=").append(criteria.normalizedSemesterCode());
        if (criteria.normalizedCourseAreaCode() != null) scope.append(";courseAreaCode=").append(criteria.normalizedCourseAreaCode());
        return scope.toString();
    }
}

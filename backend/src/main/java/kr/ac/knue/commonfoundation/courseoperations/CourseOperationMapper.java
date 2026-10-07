package kr.ac.knue.commonfoundation.courseoperations;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML-backed persistence adapter for header/detail, scope and atomic audit writes. */
@Mapper
public interface CourseOperationMapper {
    List<CourseOperationRow> list(
            @Param("criteria") CourseOperationSearch criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);
    long count(
            @Param("criteria") CourseOperationSearch criteria,
            @Param("userId") Long userId,
            @Param("roles") List<String> roles);
    CourseOperationRow find(@Param("id") Long id);
    CourseOperationRow lock(@Param("id") Long id);
    int inScope(@Param("id") Long id, @Param("userId") Long userId, @Param("roles") List<String> roles);
    List<String> organizations(@Param("userId") Long userId, @Param("date") LocalDate date);
    int managementItems(@Param("code") String code);
    Long insertHeader(
            @Param("managementNo") String managementNo,
            @Param("userId") Long userId,
            @Param("organization") String organization,
            @Param("year") String year,
            @Param("body") CourseOperationRequest body,
            @Param("attachments") String attachments);
    void insertDetail(@Param("id") Long id, @Param("details") String details);
    void updateHeader(
            @Param("id") Long id,
            @Param("body") CourseOperationRequest body,
            @Param("attachments") String attachments,
            @Param("userId") Long userId);
    void updateDetail(@Param("id") Long id, @Param("details") String details);
    void history(
            @Param("id") Long id,
            @Param("action") String action,
            @Param("field") String field,
            @Param("before") String before,
            @Param("after") String after,
            @Param("userId") Long userId,
            @Param("requestId") String requestId);
    void initialStatus(@Param("id") Long id, @Param("userId") Long userId);
}

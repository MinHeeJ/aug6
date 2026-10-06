package kr.ac.knue.commonfoundation.lectureimprovements;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** XML adapter for the approved ledger/detail and atomic audit persistence path. */
@Mapper
public interface LectureImprovementMapper {
    List<LectureImprovementRow> list(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("user") CurrentUser user);
    long count(
            @Param("criteria") LectureImprovementSearchCriteria criteria,
            @Param("user") CurrentUser user);
    LectureImprovementRow find(@Param("id") Long id, @Param("lock") boolean lock);
    int canRead(@Param("id") Long id, @Param("user") CurrentUser user);
    String organization(@Param("userId") Long userId);
    Long lockOwner(@Param("userId") Long userId);
    List<Long> lockFinalizations(@Param("userId") Long userId, @Param("year") String year);
    List<Long> lockInputPeriods(@Param("year") String year);
    List<LectureImprovementOption> codeOptions(@Param("groupId") String groupId);
    List<LectureImprovementManagementItem> managementItems(@Param("year") String year);

    /** INSERT RETURNING yields the generated header ID before any detail join/read. */
    Long insertHeader(
            @Param("request") LectureImprovementRequest request,
            @Param("userId") Long userId,
            @Param("year") String year,
            @Param("organization") String organization,
            @Param("managementNo") String managementNo,
            @Param("detailJson") String detailJson,
            @Param("attachmentsJson") String attachmentsJson);
    void insertDetail(@Param("id") Long id, @Param("request") LectureImprovementRequest request);
    int updateHeader(
            @Param("id") Long id,
            @Param("request") LectureImprovementRequest request,
            @Param("userId") Long userId,
            @Param("detailJson") String detailJson,
            @Param("attachmentsJson") String attachmentsJson);
    void updateDetail(@Param("id") Long id, @Param("request") LectureImprovementRequest request);
    String snapshot(@Param("id") Long id);
    void insertStatusHistory(@Param("id") Long id, @Param("userId") Long userId);
    void insertChangeHistory(
            @Param("id") Long id,
            @Param("before") String before,
            @Param("after") String after,
            @Param("userId") Long userId,
            @Param("requestId") String requestId);
}

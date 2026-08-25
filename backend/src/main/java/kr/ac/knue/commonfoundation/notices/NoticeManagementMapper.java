package kr.ac.knue.commonfoundation.notices;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoticeManagementMapper {
    List<NoticeSummaryRow> listNotices(@Param("criteria") NoticeSearchCriteria criteria, @Param("limit") int limit, @Param("offset") int offset);
    long countNotices(@Param("criteria") NoticeSearchCriteria criteria);
    NoticeRow findNotice(@Param("noticeId") Long noticeId);
    Long nextNoticeId();
    void insertNotice(@Param("noticeId") Long noticeId, @Param("request") NoticeSaveRequest request, @Param("userId") Long userId);
    int updateNotice(@Param("noticeId") Long noticeId, @Param("request") NoticeSaveRequest request, @Param("userId") Long userId);
    void deleteNoticeTargets(@Param("noticeId") Long noticeId);
    void insertNoticeTarget(@Param("noticeId") Long noticeId, @Param("target") NoticeTargetInput target, @Param("userId") Long userId);
    void deleteNoticeAttachments(@Param("noticeId") Long noticeId);
    void insertNoticeAttachment(@Param("noticeId") Long noticeId, @Param("originalFileName") String originalFileName,
                                @Param("storedFileName") String storedFileName, @Param("contentType") String contentType,
                                @Param("fileSize") long fileSize, @Param("fileContent") byte[] fileContent, @Param("userId") Long userId);
    List<NoticeTargetRow> listTargets(@Param("noticeId") Long noticeId);
    List<NoticeAttachmentRow> listAttachments(@Param("noticeId") Long noticeId);
    NoticeAttachmentDownload findAttachmentForDownload(@Param("noticeId") Long noticeId, @Param("attachmentId") Long attachmentId,
                                                       @Param("roleCodes") List<String> roleCodes, @Param("organizationCode") String organizationCode);
}

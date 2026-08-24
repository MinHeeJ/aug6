package kr.ac.knue.commonfoundation.fileoperations;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AttachmentFileMapper {
    List<AttachmentFileRow> listVisibleByBusinessRecordId(@Param("businessRecordId") String businessRecordId,
                                                          @Param("limit") int limit,
                                                          @Param("offset") int offset);

    long countVisibleByBusinessRecordId(@Param("businessRecordId") String businessRecordId);

    AttachmentFileRow findPublicById(@Param("fileId") Long fileId);

    AttachmentFileInternalRow findInternalById(@Param("fileId") Long fileId);

    List<AttachmentFileInternalRow> listActiveInternalFiles();

    int markLogicalDeleted(@Param("fileId") Long fileId,
                           @Param("deleteReason") String deleteReason,
                           @Param("deletedBy") Long deletedBy);

    void insertDeleteHistory(@Param("fileId") Long fileId,
                             @Param("reason") String reason,
                             @Param("deletedBy") Long deletedBy);

    List<AttachmentDeleteHistoryRow> listDeleteHistory(@Param("fileId") Long fileId);
}

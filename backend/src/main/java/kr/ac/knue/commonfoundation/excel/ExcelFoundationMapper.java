package kr.ac.knue.commonfoundation.excel;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExcelFoundationMapper {
    List<Map<String, Object>> listUploadTemplates(@Param("businessType") String businessType,
            @Param("effectiveDate") String effectiveDate, @Param("limit") int limit, @Param("offset") int offset);
    List<Map<String, Object>> listUploadFiles(@Param("businessType") String businessType,
            @Param("uploadId") String uploadId, @Param("limit") int limit, @Param("offset") int offset);
    List<Map<String, Object>> listUploadErrors(@Param("uploadId") String uploadId,
            @Param("limit") int limit, @Param("offset") int offset);
    List<Map<String, Object>> listUploadHistories(@Param("uploadId") String uploadId,
            @Param("originalFileName") String originalFileName, @Param("limit") int limit, @Param("offset") int offset);
    List<Map<String, Object>> listDownloadJobs(@Param("requesterUserId") Long requesterUserId,
            @Param("outputType") String outputType, @Param("limit") int limit, @Param("offset") int offset);
}

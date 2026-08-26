package kr.ac.knue.commonfoundation.excel;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExcelOperationsMapper {
    List<ExcelTemplateRow> listUploadTemplates(@Param("businessType") String businessType, @Param("effectiveDate") String effectiveDate,
            @Param("limit") int limit, @Param("offset") int offset);
    long countUploadTemplates(@Param("businessType") String businessType, @Param("effectiveDate") String effectiveDate);
    List<ExcelTemplateRuleRow> listTemplateRules(@Param("templateId") String templateId);
    void upsertUploadTemplate(@Param("request") UploadTemplateSaveRequest request, @Param("templateId") String templateId, @Param("userId") Long userId);
    void deleteTemplateRules(@Param("templateId") String templateId);
    void insertTemplateRule(@Param("rule") UploadTemplateRuleRequest rule, @Param("ruleId") String ruleId,
            @Param("templateId") String templateId, @Param("userId") Long userId);
    void upsertTemplateFile(@Param("templateId") String templateId, @Param("fileToken") String fileToken,
            @Param("originalFileName") String originalFileName, @Param("userId") Long userId);
    ExcelTemplateRow findUploadTemplate(@Param("templateId") String templateId);
    int countTemplateFile(@Param("templateId") String templateId);

    void insertUploadFile(@Param("uploadId") String uploadId, @Param("businessType") String businessType,
            @Param("templateId") String templateId, @Param("fileToken") String fileToken, @Param("originalFileName") String originalFileName,
            @Param("userId") Long userId, @Param("validationStatus") String validationStatus);
    void insertStagingRow(@Param("stagingRowId") String stagingRowId, @Param("uploadId") String uploadId,
            @Param("rowNumber") int rowNumber, @Param("payload") String payload, @Param("validationStatus") String validationStatus);
    void insertUploadError(@Param("error") ExcelUploadErrorRow error);
    void upsertUploadHistory(@Param("uploadId") String uploadId, @Param("totalCount") int totalCount,
            @Param("successCount") int successCount, @Param("errorCount") int errorCount, @Param("excludedCount") int excludedCount,
            @Param("savedCount") int savedCount, @Param("processingTimeMillis") long processingTimeMillis, @Param("userId") Long userId);
    int countUploadErrorsForCommit(@Param("uploadId") String uploadId);
    int countNormalStagingRows(@Param("uploadId") String uploadId);
    void markUploadCommitted(@Param("uploadId") String uploadId);
    void deleteNormalStagingRows(@Param("uploadId") String uploadId);
    int existsUpload(@Param("uploadId") String uploadId);

    List<ExcelUploadHistoryRow> listExcelUploadHistories(@Param("uploadId") String uploadId, @Param("originalFileName") String originalFileName,
            @Param("limit") int limit, @Param("offset") int offset);
    long countExcelUploadHistories(@Param("uploadId") String uploadId, @Param("originalFileName") String originalFileName);
    List<ExcelUploadErrorRow> listExcelUploadErrors(@Param("uploadId") String uploadId, @Param("limit") int limit, @Param("offset") int offset);
    long countExcelUploadErrors(@Param("uploadId") String uploadId);
    void insertDownloadJob(@Param("downloadId") String downloadId, @Param("requesterUserId") Long requesterUserId,
            @Param("outputType") String outputType, @Param("queryCondition") String queryCondition, @Param("dataScopeRef") String dataScopeRef,
            @Param("fileToken") String fileToken, @Param("originalFileName") String originalFileName);
    ExcelDownloadJobRow findDownloadJob(@Param("downloadId") String downloadId);
}

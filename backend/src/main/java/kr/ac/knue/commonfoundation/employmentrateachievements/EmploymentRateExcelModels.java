package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDateTime;
import java.util.List;

/** Immutable upload contracts and persistence projections for the FR-032 Excel workflow. */
public final class EmploymentRateExcelModels {
    private EmploymentRateExcelModels() { }

    /** Original workbook values with a one-based Excel row number. */
    public record InputRow(int rowNumber, String employeeNo, String managementItemCode,
            String achievementDate, String achievementName, String attachmentRef) { }
    /** Durable row/column diagnostic and correction guidance. */
    public record ErrorRow(int rowNumber, String columnName, String inputValue,
            String errorCode, String errorReason, String correctionGuide) { }
    /** Validation counts, retained errors and nonblocking warnings; savedCount is initially zero. */
    public record UploadResult(String uploadId, String originalFileName, String validationStatus,
            int totalCount, int successCount, int errorCount, int excludedCount, int savedCount,
            List<ErrorRow> errors, List<String> warnings, String errorDownloadUrl) { }
    /** All-or-nothing commit result. */
    public record CommitResult(String uploadId, int savedCount, List<String> warnings) { }
    /** Owner-scoped history row, serialized in the existing list response. */
    public record HistoryRow(String uploadId, String originalFileName, String validationStatus,
            int totalCount, int successCount, int errorCount, int excludedCount, int savedCount,
            LocalDateTime processedAt) { }
    /** Current template identity and ordered persisted columns. */
    public record Template(String templateId, String version, List<String> columns) { }
    /** Upload metadata used for ownership and replay guards. */
    public record Upload(String uploadId, String templateId, String validationStatus,
            String originalFileName) { }
    /** Resolved teacher and organization snapshot, never supplied by the uploader. */
    public record Target(long userId, String organizationCode) { }
    /** Confirmed-version management-item validation settings. */
    public record ItemRule(String requiredYn, String dataType, String editableYn) { }
}

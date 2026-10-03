package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Provides employment-rate exports and delegates upload validation to the shared
 * Excel infrastructure. Bulk execution remains deliberately blocked because its
 * execution policy has not been approved.
 */
@Service
public class EmploymentRateAchievementOperationsService {
    static final String BUSINESS_TYPE = "EMPLOYMENT_RATE_ACHIEVEMENT";
    private final JdbcTemplate jdbcTemplate;
    private final ExcelOperationsService excelOperationsService;

    public EmploymentRateAchievementOperationsService(
            JdbcTemplate jdbcTemplate,
            ExcelOperationsService excelOperationsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.excelOperationsService = excelOperationsService;
    }

    /** Exports the requested page of rows visible to the authenticated operation role. */
    @Transactional(readOnly = true)
    public byte[] download(int page, int pageSize, CurrentUser requester) {
        validatePagination(page, pageSize);
        StringBuilder sql = new StringBuilder("""
                SELECT achievement.achievement_id,
                       achievement.target_user_id,
                       achievement.management_item_code,
                       achievement.achievement_date,
                       achievement.achievement_name,
                       achievement.achievement_status
                FROM employment_rate_achievements achievement
                WHERE achievement.deleted_yn = 'N'
                """);
        List<Object> parameters = new ArrayList<>();
        appendScopePredicate(sql, parameters, requester);
        sql.append("""
                ORDER BY achievement.achievement_date DESC,
                         achievement.achievement_id DESC
                LIMIT ? OFFSET ?
                """);
        parameters.add(pageSize);
        parameters.add(page * pageSize);
        List<ExportRow> rows = jdbcTemplate.query(
                sql.toString(),
                (resultSet, rowNumber) -> new ExportRow(
                        resultSet.getLong("achievement_id"),
                        resultSet.getLong("target_user_id"),
                        resultSet.getString("management_item_code"),
                        resultSet.getObject("achievement_date", LocalDate.class),
                        resultSet.getString("achievement_name"),
                        resultSet.getString("achievement_status")),
                parameters.toArray());
        return createWorkbook(rows);
    }

    /**
     * Resolves the registered template at runtime instead of inventing a template
     * identifier, then records the shared validation result and upload history.
     */
    @Transactional
    public EmploymentRateExcelUploadResult upload(MultipartFile file, CurrentUser requester) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException(
                    "취업률 실적 Excel 파일을 선택하세요.",
                    List.of(new ValidationError("file", "Excel 파일을 선택하세요.")));
        }
        String templateId = jdbcTemplate.query(
                """
                SELECT template.template_id
                FROM excel_upload_templates template
                WHERE template.business_type = ?
                  AND template.system_use_yn = 'Y'
                  AND template.status = 'ACTIVE'
                ORDER BY template.effective_date DESC,
                         template.template_version DESC
                LIMIT 1
                """,
                resultSet -> resultSet.next() ? resultSet.getString("template_id") : null,
                BUSINESS_TYPE);
        if (templateId == null) {
            throw new NotFoundException("등록된 취업률 실적 Excel 양식을 찾을 수 없습니다.");
        }
        ExcelUploadResult result = excelOperationsService.createExcelUpload(
                BUSINESS_TYPE,
                templateId,
                file,
                requester.userId());
        return new EmploymentRateExcelUploadResult(
                result.uploadId(),
                result.originalFileName(),
                result.totalCount(),
                result.successCount(),
                result.errorCount(),
                result.errors());
    }

    /** Validates the request but creates no job until the unresolved bulk policy is approved. */
    @Transactional
    public EmploymentRateBulkJobResult createBulkJob(
            EmploymentRateBulkJobRequest request,
            CurrentUser requester) {
        validateBulkRequest(request);
        throw new ConflictException("OQ-83-01 일괄 생성·삭제 실행정책이 확정되지 않아 작업을 접수할 수 없습니다.");
    }

    /** Looks up a persisted job result after the future approved executor creates it. */
    @Transactional(readOnly = true)
    public EmploymentRateBulkJobResult getBulkJob(String jobId, CurrentUser requester) {
        if (jobId == null || jobId.isBlank()) {
            throw new BusinessValidationException(
                    "일괄 작업 식별자가 올바르지 않습니다.",
                    List.of(new ValidationError("jobId", "작업 식별자를 입력하세요.")));
        }
        List<EmploymentRateBulkJobResult> rows = jdbcTemplate.query(
                """
                SELECT bulk_job.job_id,
                       bulk_job.evaluation_year,
                       bulk_job.action_type,
                       bulk_job.status,
                       COUNT(item.job_item_id) AS total_count,
                       COUNT(item.job_item_id) FILTER (WHERE item.processed_yn = 'Y') AS processed_count,
                       COUNT(item.job_item_id) FILTER (WHERE item.processed_yn = 'N') AS unprocessed_count
                FROM employment_rate_bulk_jobs bulk_job
                LEFT JOIN employment_rate_bulk_job_items item
                    ON item.job_id = bulk_job.job_id
                WHERE bulk_job.job_id = ?
                GROUP BY bulk_job.job_id,
                         bulk_job.evaluation_year,
                         bulk_job.action_type,
                         bulk_job.status
                """,
                (resultSet, rowNumber) -> new EmploymentRateBulkJobResult(
                        resultSet.getString("job_id"),
                        resultSet.getString("evaluation_year"),
                        resultSet.getString("action_type"),
                        resultSet.getString("status"),
                        resultSet.getInt("total_count"),
                        resultSet.getInt("processed_count"),
                        resultSet.getInt("unprocessed_count")),
                jobId.trim());
        if (rows.isEmpty()) {
            throw new NotFoundException("취업률 실적 일괄 작업을 찾을 수 없습니다.");
        }
        return rows.get(0);
    }

    private void appendScopePredicate(
            StringBuilder sql,
            List<Object> parameters,
            CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null) {
            throw new kr.ac.knue.commonfoundation.common.api.ForbiddenException();
        }
        if (requester.roles().contains("R01") || requester.roles().contains("R07")) {
            sql.append("AND achievement.target_user_id = ?\n");
            parameters.add(requester.userId());
            return;
        }
        if (requester.roles().contains("R02")) {
            sql.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM organization_user_mappings requester_mapping
                        JOIN organization_user_mappings target_mapping
                            ON target_mapping.organization_code = requester_mapping.organization_code
                        WHERE requester_mapping.user_id = ?
                          AND requester_mapping.status = 'ACTIVE'
                          AND target_mapping.user_id = achievement.target_user_id
                          AND target_mapping.status = 'ACTIVE'
                    )
                    """);
            parameters.add(requester.userId());
            return;
        }
        if (requester.roles().contains("R04")) {
            sql.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM evaluation_organization_mappings permission_mapping
                        JOIN organization_user_mappings target_mapping
                            ON target_mapping.organization_code = permission_mapping.organization_code
                        WHERE permission_mapping.user_id = ?
                          AND permission_mapping.business_type = 'FACULTY_ACHIEVEMENT'
                          AND permission_mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
                          AND target_mapping.user_id = achievement.target_user_id
                          AND target_mapping.status = 'ACTIVE'
                    )
                    """);
            parameters.add(requester.userId());
            return;
        }
        throw new kr.ac.knue.commonfoundation.common.api.ForbiddenException();
    }

    private void validatePagination(int page, int pageSize) {
        List<ValidationError> errors = new ArrayList<>();
        if (page < 0) {
            errors.add(new ValidationError("page", "0 이상이어야 합니다."));
        }
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            errors.add(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Excel 다운로드 조건이 올바르지 않습니다.", errors);
        }
    }

    private void validateBulkRequest(EmploymentRateBulkJobRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null || request.evaluationYear() == null || request.evaluationYear().isBlank()) {
            errors.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        }
        if (request == null || request.actionType() == null || request.actionType().isBlank()) {
            errors.add(new ValidationError("actionType", "일괄 작업 유형을 입력하세요."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("일괄 작업 요청이 올바르지 않습니다.", errors);
        }
    }

    private byte[] createWorkbook(List<ExportRow> rows) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output)) {
            put(
                    zip,
                    "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            put(
                    zip,
                    "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            put(
                    zip,
                    "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"취업률 실적\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            put(
                    zip,
                    "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            put(zip, "xl/worksheets/sheet1.xml", worksheet(rows));
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("취업률 실적 Excel 파일을 생성하지 못했습니다.", exception);
        }
    }

    private String worksheet(List<ExportRow> rows) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        xml.append("<row r=\"1\">")
                .append(cell("실적ID"))
                .append(cell("대상교원ID"))
                .append(cell("관리항목코드"))
                .append(cell("업적발생일"))
                .append(cell("실적명"))
                .append(cell("상태"))
                .append("</row>");
        for (int index = 0; index < rows.size(); index++) {
            ExportRow row = rows.get(index);
            xml.append("<row r=\"").append(index + 2).append("\">")
                    .append(cell(String.valueOf(row.achievementId())))
                    .append(cell(String.valueOf(row.targetUserId())))
                    .append(cell(row.managementItemCode()))
                    .append(cell(String.valueOf(row.achievementDate())))
                    .append(cell(row.achievementName()))
                    .append(cell(row.achievementStatus()))
                    .append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private String cell(String value) {
        return "<c t=\"inlineStr\"><is><t>" + xml(value) + "</t></is></c>";
    }

    private String xml(String value) {
        return (value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private void put(ZipOutputStream zip, String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private record ExportRow(
            long achievementId,
            long targetUserId,
            String managementItemCode,
            LocalDate achievementDate,
            String achievementName,
            String achievementStatus) {
    }
}

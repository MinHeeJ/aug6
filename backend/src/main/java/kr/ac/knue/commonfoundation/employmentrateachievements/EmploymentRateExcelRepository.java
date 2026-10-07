package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Feature-local JDBC adapter for existing Excel metadata; no schema or shared mapper changes. */
@Repository
public class EmploymentRateExcelRepository {
    public static final String BUSINESS = "EMPLOYMENT_RATE_ACHIEVEMENT";
    private final JdbcTemplate jdbc;

    public EmploymentRateExcelRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Template(String templateId, String templateVersion, List<String> columns) { }
    public record Target(long userId, String organizationCode) { }
    public record Upload(String uploadId, String fileToken, String templateId, String validationStatus,
                         int totalCount, int successCount, int errorCount, int savedCount) { }
    public record Staged(int rowNumber, String payload, String validationStatus) { }

    public Template currentTemplate() {
        List<Template> templates = jdbc.query("""
                SELECT t.template_id, t.template_version
                FROM excel_upload_templates t
                WHERE t.business_type = ? AND t.status = 'ACTIVE' AND t.system_use_yn = 'Y'
                    AND t.effective_date <= CURRENT_DATE
                ORDER BY t.effective_date DESC, t.template_version DESC, t.template_id
                LIMIT 1
                """, (rs, n) -> new Template(rs.getString(1), rs.getString(2), List.of()), BUSINESS);
        if (templates.isEmpty()) throw new ConflictException("INVALID_TEMPLATE: 현행 양식이 없습니다.");
        Template template = templates.get(0);
        List<String> columns = jdbc.query("""
                SELECT r.required_column FROM excel_upload_template_rules r
                WHERE r.template_id = ? ORDER BY r.column_order
                """, (rs, n) -> rs.getString(1), template.templateId());
        if (!columns.equals(EmploymentRateExcelService.COLUMNS)) {
            throw new ConflictException("INVALID_TEMPLATE: 지원하는 공통열과 현행 양식 규칙이 다릅니다.");
        }
        return new Template(template.templateId(), template.templateVersion(), columns);
    }

    /** Resolves one active faculty and one allowed organization; ambiguity is rejected. */
    public Target findTarget(String employeeNo, long requester) {
        List<Target> targets = jdbc.query("""
                SELECT DISTINCT u.user_id, tm.organization_code
                FROM users u
                JOIN organization_user_mappings tm ON tm.user_id = u.user_id
                JOIN evaluation_organization_mappings pm ON pm.organization_code = tm.organization_code
                JOIN organizations o ON o.organization_code = tm.organization_code
                WHERE u.employee_no = ? AND u.status = 'ACTIVE' AND u.system_use_yn = 'Y'
                    AND tm.status = 'ACTIVE' AND tm.effective_start_date <= CURRENT_DATE
                    AND (tm.effective_end_date IS NULL OR tm.effective_end_date >= CURRENT_DATE)
                    AND pm.user_id = ? AND pm.business_type = 'FACULTY_ACHIEVEMENT'
                    AND pm.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
                    AND o.status = 'ACTIVE' AND o.system_use_yn = 'Y'
                    AND EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.user_id
                        AND ur.role_code = 'R01' AND ur.status = 'ACTIVE')
                """, (rs, n) -> new Target(rs.getLong(1), rs.getString(2)), employeeNo, requester);
        return targets.size() == 1 ? targets.get(0) : null;
    }

    public int countItem(String code, String year, LocalDate date) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM evaluation_management_items m
                JOIN evaluation_elements e ON e.element_id = m.element_id
                JOIN evaluation_items i ON i.item_id = e.item_id
                JOIN evaluation_areas a ON a.area_id = i.area_id
                JOIN evaluation_rule_versions v ON v.rule_version_id = a.rule_version_id
                WHERE m.management_item_code = ? AND m.active_yn = 'Y'
                    AND e.element_code = 'FR-032' AND e.evaluation_year = ? AND e.active_yn = 'Y'
                    AND i.active_yn = 'Y' AND a.area_code = 'EDUCATION' AND a.active_yn = 'Y'
                    AND v.version_status = 'CONFIRMED'
                    AND v.effective_start_date <= make_date(CAST(? AS integer), 12, 31)
                    AND v.effective_end_date >= make_date(CAST(? AS integer), 1, 1)
                """, Integer.class, code, year, year, year);
    }

    public int countDuplicate(long userId, String year, String code, LocalDate date) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM education_achievements a
                WHERE a.teacher_user_id = ? AND a.evaluation_year = ? AND a.management_item_code = ?
                    AND a.achievement_date = ? AND a.achievement_type = 'FR-032' AND a.deleted_yn = 'N'
                """, Integer.class, userId, year, code, date);
    }

    /** Coarse PostgreSQL phantom protection for mutable guard/config tables, in a fixed lock order. */
    public void lockGuards() {
        jdbc.execute("""
                LOCK TABLE users, user_roles, organizations, organization_user_mappings,
                    evaluation_organization_mappings, input_period_settings, evaluation_finalizations,
                    evaluation_date_settings, evaluation_rule_versions, evaluation_areas,
                    evaluation_items, evaluation_elements, evaluation_management_items,
                    excel_upload_templates, excel_upload_template_rules IN SHARE MODE
                """);
    }

    public void insertUpload(String id, String templateId, String fileToken, String filename,
                             String validationStatus, long userId) {
        jdbc.update("""
                INSERT INTO excel_upload_files
                    (upload_id, business_type, template_id, file_token, original_file_name,
                     validation_status, uploader_user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, BUSINESS, templateId, fileToken, filename, validationStatus, userId);
    }

    public void insertStaging(String id, int row, String payload, String status) {
        jdbc.update("""
                INSERT INTO excel_upload_staging_rows
                    (staging_row_id, upload_id, row_number, row_payload, validation_status)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?)
                """, "ER-STG-" + UUID.randomUUID(), id, row, payload, status);
    }

    public void insertError(String id, EmploymentRateExcelService.Error error) {
        jdbc.update("""
                INSERT INTO excel_upload_errors
                    (error_id, upload_id, row_number, column_name, input_value,
                     error_code, error_reason, correction_guide)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, "ER-ERR-" + UUID.randomUUID(), id, error.rowNumber(), error.columnName(),
                error.inputValue(), error.errorCode(), error.errorReason(), error.correctionGuide());
    }

    public void insertHistory(String id, int total, int success, int errors, int saved, long userId) {
        jdbc.update("""
                INSERT INTO excel_upload_histories
                    (upload_id, total_count, success_count, error_count, excluded_count,
                     saved_count, processing_time_millis, processor_user_id)
                VALUES (?, ?, ?, ?, 0, ?, 0, ?)
                """, id, total, success, errors, saved, userId);
    }

    public void insertErrorDownload(String id, String token, long userId) {
        jdbc.update("""
                INSERT INTO excel_download_jobs
                    (download_id, requester_user_id, output_type, query_condition,
                     data_scope_ref, file_token, original_file_name, generated_at)
                VALUES (?, ?, 'ERROR', jsonb_build_object('uploadId', CAST(? AS text)), ?, ?, ?, CURRENT_TIMESTAMP)
                """, "ER-DL-" + id, userId, id, BUSINESS, token, "employment-rate-errors.xlsx");
    }

    public Upload findUpload(String id, long userId, boolean lock) {
        List<Upload> rows = jdbc.query("""
                SELECT f.upload_id, f.file_token, f.template_id, f.validation_status,
                    h.total_count, h.success_count, h.error_count, h.saved_count
                FROM excel_upload_files f
                JOIN excel_upload_histories h ON h.upload_id = f.upload_id
                WHERE f.upload_id = ? AND f.uploader_user_id = ? AND f.business_type = ?
                    AND f.status = 'ACTIVE' AND h.status = 'ACTIVE'
                """ + (lock ? " FOR UPDATE OF f, h" : ""), this::upload, id, userId, BUSINESS);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Upload upload(ResultSet rs, int n) throws SQLException {
        return new Upload(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8));
    }

    public List<Staged> staging(String id) {
        return jdbc.query("""
                SELECT s.row_number, s.row_payload::text, s.validation_status
                FROM excel_upload_staging_rows s WHERE s.upload_id = ? ORDER BY s.row_number FOR UPDATE
                """, (rs, n) -> new Staged(rs.getInt(1), rs.getString(2), rs.getString(3)), id);
    }

    public List<EmploymentRateExcelService.Error> errors(String id) {
        return jdbc.query("""
                SELECT e.row_number, e.column_name, e.input_value, e.error_code, e.error_reason, e.correction_guide
                FROM excel_upload_errors e WHERE e.upload_id = ? AND e.status = 'ACTIVE'
                ORDER BY e.row_number, e.column_name
                """, (rs, n) -> new EmploymentRateExcelService.Error(rs.getInt(1), rs.getString(2),
                rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)), id);
    }

    public List<Map<String, Object>> histories(long userId) {
        return jdbc.queryForList("""
                SELECT f.upload_id AS "uploadId", f.original_file_name AS "originalFileName",
                    f.uploader_user_id AS "uploaderUserId", f.uploaded_at AS "uploadedAt",
                    f.validation_status AS "validationStatus", h.total_count AS "totalCount",
                    h.success_count AS "successCount", h.error_count AS "errorCount",
                    h.saved_count AS "savedCount", h.processed_at AS "processedAt"
                FROM excel_upload_files f JOIN excel_upload_histories h ON h.upload_id = f.upload_id
                WHERE f.uploader_user_id = ? AND f.business_type = ?
                    AND f.status = 'ACTIVE' AND h.status = 'ACTIVE'
                ORDER BY f.uploaded_at DESC, f.upload_id
                """, userId, BUSINESS);
    }

    public String errorFileToken(String id, long userId) {
        List<String> rows = jdbc.query("""
                SELECT d.file_token FROM excel_download_jobs d
                WHERE d.download_id = ? AND d.requester_user_id = ? AND d.data_scope_ref = ?
                    AND d.output_type = 'ERROR' AND d.status = 'GENERATED'
                """, (rs, n) -> rs.getString(1), "ER-DL-" + id, userId, BUSINESS);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void finish(String id, long userId, int saved) {
        int changed = jdbc.update("""
                UPDATE excel_upload_files SET validation_status = 'COMMITTED'
                WHERE upload_id = ? AND uploader_user_id = ? AND business_type = ? AND validation_status = 'VALIDATED'
                """, id, userId, BUSINESS);
        if (changed != 1) throw new ConflictException("업로드 상태가 변경되었습니다.");
        jdbc.update("""
                UPDATE excel_upload_histories SET saved_count = ?, processed_at = CURRENT_TIMESTAMP,
                    processor_user_id = ? WHERE upload_id = ?
                """, saved, userId, id);
        jdbc.update("DELETE FROM excel_upload_staging_rows WHERE upload_id = ?", id);
    }
}

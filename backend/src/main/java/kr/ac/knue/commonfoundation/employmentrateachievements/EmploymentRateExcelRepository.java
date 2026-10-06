package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import static kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateExcelModels.*;

/** Independent Excel persistence over V15/V65/V66; does not depend on parent-owned CRUD mappers. */
@Repository
public class EmploymentRateExcelRepository {
    private final JdbcTemplate jdbc;

    public EmploymentRateExcelRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Reads the current active template and its persisted column order. */
    public Template currentTemplate() {
        List<Template> templates = jdbc.query("""
                SELECT t.template_id, t.template_version
                FROM excel_upload_templates t
                WHERE t.business_type IN ('EMPLOYMENT_RATE', 'EMPLOYMENT_RATE_ACHIEVEMENT')
                  AND t.status = 'ACTIVE' AND t.system_use_yn = 'Y'
                  AND t.effective_date <= CURRENT_DATE
                ORDER BY t.effective_date DESC, t.template_id DESC LIMIT 1
                """, (rs, n) -> new Template(rs.getString(1), rs.getString(2), List.of()));
        if (templates.isEmpty()) throw new NotFoundException("현행 취업률 양식이 없습니다.");
        Template template = templates.get(0);
        List<String> columns = jdbc.query("""
                SELECT r.required_column FROM excel_upload_template_rules r
                WHERE r.template_id = ? ORDER BY r.column_order
                """, (rs, n) -> rs.getString(1), template.templateId());
        return new Template(template.templateId(), template.version(), columns);
    }

    /** Resolves active employee identities without choosing among ambiguous organizations. */
    public List<Target> targets(String employeeNo) {
        return jdbc.query("""
                SELECT DISTINCT u.user_id, m.organization_code
                FROM users u
                JOIN organization_user_mappings m ON m.user_id = u.user_id
                WHERE u.employee_no = ? AND u.status = 'ACTIVE' AND u.system_use_yn = 'Y'
                  AND m.status = 'ACTIVE' AND m.mapping_type = 'ORGANIZATION'
                  AND (m.effective_start_date IS NULL OR m.effective_start_date <= CURRENT_DATE)
                  AND (m.effective_end_date IS NULL OR m.effective_end_date >= CURRENT_DATE)
                """, (rs, n) -> new Target(rs.getLong(1), rs.getString(2)), employeeNo);
    }

    /** Checks the requester's persisted organization data scope. */
    public boolean inScope(long requester, Target target) {
        return count("""
                SELECT COUNT(*) FROM evaluation_organization_mappings p
                WHERE p.user_id = ? AND p.business_type = 'FACULTY_ACHIEVEMENT'
                  AND p.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')
                  AND p.organization_code = ?
                """, requester, target.organizationCode()) > 0;
    }

    /** Returns only active FR-032 rules belonging to confirmed versions covering the year. */
    public List<ItemRule> itemRules(String code, String year) {
        return jdbc.query("""
                SELECT m.required_yn, m.data_type, m.teacher_editable_yn
                FROM evaluation_management_items m
                JOIN evaluation_elements e ON e.element_id = m.element_id
                JOIN evaluation_items i ON i.item_id = e.item_id
                JOIN evaluation_areas a ON a.area_id = i.area_id
                JOIN evaluation_rule_versions v ON v.rule_version_id = a.rule_version_id
                WHERE m.management_item_code = ? AND e.evaluation_year = ?
                  AND e.element_code = 'FR-032' AND a.area_code = 'EDUCATION'
                  AND m.active_yn = 'Y' AND e.active_yn = 'Y'
                  AND i.active_yn = 'Y' AND a.active_yn = 'Y'
                  AND v.version_status = 'CONFIRMED'
                  AND CAST(? || '-01-01' AS DATE) <= v.effective_end_date
                  AND CAST(? || '-12-31' AS DATE) >= v.effective_start_date
                """, (rs, n) -> new ItemRule(rs.getString(1), rs.getString(2), rs.getString(3)),
                code, year, year, year);
    }

    /** Matches the V65 live employment-achievement unique index exactly. */
    public boolean duplicate(Target target, InputRow row, LocalDate date) {
        return count("""
                SELECT COUNT(*) FROM education_achievements a
                WHERE a.achievement_type = 'FR-032' AND a.deleted_yn = 'N'
                  AND a.teacher_user_id = ? AND a.management_item_code = ?
                  AND a.achievement_date = ? AND COALESCE(a.achievement_name, '') = ?
                """, target.userId(), row.managementItemCode(), date, row.achievementName()) > 0;
    }

    /** Persists upload metadata independently of achievement materialization. */
    public void insertUpload(String id, Template template, String token, String filename,
            long owner, String status) {
        jdbc.update("""
                INSERT INTO excel_upload_files (upload_id, business_type, template_id,
                    file_token, original_file_name, uploader_user_id, validation_status)
                VALUES (?, 'EMPLOYMENT_RATE', ?, ?, ?, ?, ?)
                """, id, template.templateId(), token, filename, owner, status);
    }

    /** Retains each original row and validation result until successful commit. */
    public void stage(String uploadId, InputRow row, String json, String status) {
        jdbc.update("""
                INSERT INTO excel_upload_staging_rows
                    (staging_row_id, upload_id, row_number, row_payload, validation_status)
                VALUES (?, ?, ?, CAST(? AS JSONB), ?)
                """, UUID.randomUUID().toString(), uploadId, row.rowNumber(), json, status);
    }

    /** Reads validated payloads in original workbook order. */
    public List<String> staged(String uploadId) {
        return jdbc.query("""
                SELECT s.row_payload::text FROM excel_upload_staging_rows s
                WHERE s.upload_id = ? AND s.validation_status = 'NORMAL'
                ORDER BY s.row_number
                """, (rs, n) -> rs.getString(1), uploadId);
    }

    /** Rejects missing or non-normal staging rather than committing a filtered subset. */
    public boolean stagingComplete(String uploadId) {
        return count("""
                SELECT COUNT(*) FROM excel_upload_histories h
                WHERE h.upload_id = ? AND h.total_count > 0
                  AND h.total_count = h.success_count AND h.error_count = 0
                  AND h.excluded_count = 0 AND h.saved_count = 0
                  AND h.total_count = (
                      SELECT COUNT(*) FROM excel_upload_staging_rows s WHERE s.upload_id = h.upload_id
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM excel_upload_staging_rows s
                      WHERE s.upload_id = h.upload_id AND s.validation_status <> 'NORMAL'
                  )
                """, uploadId) == 1;
    }

    /** Stores the first diagnostic for a row/column under the V15 location constraint. */
    public void error(String uploadId, ErrorRow error) {
        jdbc.update("""
                INSERT INTO excel_upload_errors (error_id, upload_id, row_number, column_name,
                    input_value, error_code, error_reason, correction_guide)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), uploadId, error.rowNumber(), error.columnName(),
                error.inputValue(), error.errorCode(), error.errorReason(), error.correctionGuide());
    }

    /** Reads retained diagnostics; commit never deletes or replaces them. */
    public List<ErrorRow> errors(String uploadId) {
        return jdbc.query("""
                SELECT e.row_number, e.column_name, e.input_value, e.error_code,
                    e.error_reason, e.correction_guide
                FROM excel_upload_errors e WHERE e.upload_id = ? AND e.status = 'ACTIVE'
                ORDER BY e.row_number, e.column_name
                """, (rs, n) -> new ErrorRow(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)), uploadId);
    }

    /** Enforces upload ownership and optionally serializes commit/replay on the upload row. */
    public Upload ownedUpload(String id, long owner, boolean lock) {
        List<Upload> rows = jdbc.query("""
                SELECT f.upload_id, f.template_id, f.validation_status, f.original_file_name
                FROM excel_upload_files f WHERE f.upload_id = ? AND f.uploader_user_id = ?
                  AND f.business_type IN ('EMPLOYMENT_RATE', 'EMPLOYMENT_RATE_ACHIEVEMENT')
                  AND f.status = 'ACTIVE'
                """ + (lock ? " FOR UPDATE" : ""),
                (rs, n) -> new Upload(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)), id, owner);
        if (rows.isEmpty()) throw new NotFoundException("업로드 파일을 찾을 수 없습니다.");
        return rows.get(0);
    }

    /** Updates commit counts without overwriting original validation counts or diagnostics. */
    public void history(String id, int total, int success, int errors, int saved, long millis, long owner) {
        jdbc.update("""
                INSERT INTO excel_upload_histories (upload_id, total_count, success_count,
                    error_count, excluded_count, saved_count, processing_time_millis, processor_user_id)
                VALUES (?, ?, ?, ?, 0, ?, ?, ?)
                ON CONFLICT (upload_id) DO UPDATE SET saved_count = EXCLUDED.saved_count,
                    processing_time_millis = EXCLUDED.processing_time_millis,
                    processed_at = CURRENT_TIMESTAMP, processor_user_id = EXCLUDED.processor_user_id
                """, id, total, success, errors, saved, millis, owner);
    }

    /** Returns the owner's upload history as the existing list contract expects. */
    public List<HistoryRow> histories(long owner) {
        return jdbc.query("""
                SELECT f.upload_id, f.original_file_name, f.validation_status, h.total_count,
                    h.success_count, h.error_count, h.excluded_count, h.saved_count, h.processed_at
                FROM excel_upload_files f
                JOIN excel_upload_histories h ON h.upload_id = f.upload_id
                WHERE f.uploader_user_id = ?
                  AND f.business_type IN ('EMPLOYMENT_RATE', 'EMPLOYMENT_RATE_ACHIEVEMENT')
                  AND f.status = 'ACTIVE' AND h.status = 'ACTIVE'
                ORDER BY f.uploaded_at DESC, f.upload_id
                """, (rs, n) -> new HistoryRow(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8),
                        rs.getTimestamp(9).toLocalDateTime()), owner);
    }

    /** Links an owner-bound error workbook to its upload. */
    public void persistErrorFile(String uploadId, String token, long owner) {
        jdbc.update("""
                INSERT INTO excel_download_jobs (download_id, requester_user_id, output_type,
                    query_condition, data_scope_ref, file_token, original_file_name, generated_at)
                VALUES (?, ?, 'ERROR', JSONB_BUILD_OBJECT('uploadId', CAST(? AS TEXT)),
                    'EMPLOYMENT_RATE:OWNER', ?, 'employment-rate-errors.xlsx', CURRENT_TIMESTAMP)
                """, UUID.randomUUID().toString(), owner, uploadId, token);
    }

    /** Finds only error files generated for this owner and upload. */
    public String errorFile(String uploadId, long owner) {
        List<String> rows = jdbc.query("""
                SELECT j.file_token FROM excel_download_jobs j
                WHERE j.requester_user_id = ? AND j.output_type = 'ERROR'
                  AND j.data_scope_ref = 'EMPLOYMENT_RATE:OWNER'
                  AND j.query_condition ->> 'uploadId' = ? AND j.status = 'GENERATED'
                ORDER BY j.requested_at DESC LIMIT 1
                """, (rs, n) -> rs.getString(1), owner, uploadId);
        if (rows.isEmpty()) throw new NotFoundException("오류 결과 파일이 없습니다.");
        return rows.get(0);
    }

    /**
     * Prevents guard changes and phantom finalizations until commit completes.
     * SHARE locks also cover absent rows; row locks alone cannot protect absence.
     * Called only inside the service's write transaction, before revalidation.
     */
    public void lockCommitGuards() {
        jdbc.execute("""
                LOCK TABLE evaluation_finalizations, input_period_settings,
                    evaluation_date_settings, users, organization_user_mappings,
                    evaluation_organization_mappings, evaluation_rule_versions,
                    evaluation_areas, evaluation_items, evaluation_elements,
                    evaluation_management_items, excel_upload_templates,
                    excel_upload_template_rules IN SHARE MODE
                """);
    }

    /** Creates the ledger, initial status history and change audit in the caller's transaction. */
    public void insertAchievement(Target target, InputRow row, LocalDate date, String attachments,
            long actor, String requestId) {
        String managementNo = "ER-" + UUID.randomUUID();
        Long id = jdbc.queryForObject("""
                INSERT INTO education_achievements (management_no, achievement_type, teacher_user_id,
                    organization_code, evaluation_year, management_item_code, achievement_date,
                    achievement_name, attachment_ids, created_by, updated_by)
                VALUES (?, 'FR-032', ?, ?, ?, ?, ?, ?, CAST(? AS JSONB), ?, ?)
                RETURNING achievement_id
                """, Long.class, managementNo, target.userId(), target.organizationCode(),
                Integer.toString(date.getYear()), row.managementItemCode(), date,
                row.achievementName(), attachments, actor, actor);
        jdbc.update("""
                INSERT INTO education_achievement_status_histories (achievement_type, achievement_id,
                    previous_status, next_status, action_type, opinion, processed_by)
                VALUES ('FR-032', ?, NULL, 'DRAFT', 'CREATE', '취업률 Excel 반영', ?)
                """, id, actor);
        jdbc.update("""
                INSERT INTO data_change_histories (target_business, target_key, change_type,
                    field_name, before_value, after_value, changed_by, change_reason, request_id)
                SELECT 'FR-032', CAST(a.achievement_id AS TEXT), 'CREATE', 'snapshot', NULL,
                    TO_JSONB(a)::text, ?, '취업률 Excel 반영', ?
                FROM education_achievements a WHERE a.achievement_id = ?
                """, actor, requestId, id);
    }

    /** Marks successful commit and clears staging, preserving validation errors and history. */
    public void committed(String id) {
        jdbc.update("UPDATE excel_upload_files SET validation_status = 'COMMITTED' WHERE upload_id = ?", id);
        jdbc.update("DELETE FROM excel_upload_staging_rows WHERE upload_id = ?", id);
    }

    private int count(String sql, Object... values) {
        Integer value = jdbc.queryForObject(sql, Integer.class, values);
        return value == null ? 0 : value;
    }
}

package kr.ac.knue.commonfoundation.employmentrateachievements;

import static kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateExcelModels.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Runner-owned PostgreSQL execution required. Uses existing V66 rows, without changing shared fixtures. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_EMPLOYMENT_RATE_DB_TESTS", matches = "true")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = "jdbc:postgresql:.*")
class EmploymentRateExcelPersistenceTest {
    @Autowired EmploymentRateExcelRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void duplicateNaturalKeyRollsBackLedgerStatusAndChangeHistoryTogether() {
        String name = "excel-atomic-" + UUID.randomUUID();
        Target target = jdbc.queryForObject("""
                SELECT a.teacher_user_id, a.organization_code
                FROM education_achievements a
                WHERE a.management_no = 'EDU-FR-032-2025-001'
                """, (rs, n) -> new Target(rs.getLong(1), rs.getString(2)));
        assertThat(target).isNotNull();
        InputRow row = new InputRow(2, "unused", "FR-032", "2025-04-11", name, "");
        String requestId = "REQ-" + UUID.randomUUID();
        var transaction = new TransactionTemplate(transactions);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            repository.insertAchievement(target, row, LocalDate.parse(row.achievementDate()),
                    "[]", target.userId(), requestId);
            repository.insertAchievement(target, row, LocalDate.parse(row.achievementDate()),
                    "[]", target.userId(), requestId);
        })).isInstanceOf(DuplicateKeyException.class);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM education_achievements a WHERE a.achievement_name = ?
                """, Integer.class, name)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM data_change_histories h WHERE h.request_id = ?
                """, Integer.class, requestId)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM education_achievement_status_histories h
                JOIN education_achievements a ON a.achievement_id = h.achievement_id
                    AND a.achievement_type = h.achievement_type
                WHERE a.achievement_name = ?
                """, Integer.class, name)).isZero();
    }

    @Test
    void stagedHistoryAndErrorMaterializationAreDurableAndOwnerScoped() {
        Long owner = jdbc.queryForObject("SELECT user_id FROM users WHERE login_id = 'professor1'", Long.class);
        String id = "ER-TEST-" + UUID.randomUUID();
        var transaction = new TransactionTemplate(transactions);
        try {
            transaction.executeWithoutResult(status -> {
                var template = repository.currentTemplate();
                repository.insertUpload(id, template, "test-only-unread-token", "test.xlsx", owner, "REJECTED");
                InputRow row = new InputRow(2, "bad", "FR-032", "bad", "test", "");
                repository.stage(id, row, "{\"raw\":\"bad\"}", "ERROR");
                repository.error(id, new ErrorRow(2, "教番", "bad", "INVALID_TARGET", "invalid", "correct"));
                repository.history(id, 1, 0, 1, 0, 1, owner);
            });
            assertThat(repository.histories(owner)).anyMatch(h -> h.uploadId().equals(id) && h.errorCount() == 1);
            assertThat(repository.errors(id)).singleElement().extracting(ErrorRow::rowNumber).isEqualTo(2);
            assertThat(repository.ownedUpload(id, owner, false).validationStatus()).isEqualTo("REJECTED");
            assertThatThrownBy(() -> repository.ownedUpload(id, -1L, false))
                    .isInstanceOf(kr.ac.knue.commonfoundation.common.api.NotFoundException.class);
        } finally {
            transaction.executeWithoutResult(status -> {
                jdbc.update("DELETE FROM excel_upload_errors WHERE upload_id = ?", id);
                jdbc.update("DELETE FROM excel_upload_staging_rows WHERE upload_id = ?", id);
                jdbc.update("DELETE FROM excel_upload_histories WHERE upload_id = ?", id);
                jdbc.update("DELETE FROM excel_upload_files WHERE upload_id = ?", id);
            });
        }
    }
}

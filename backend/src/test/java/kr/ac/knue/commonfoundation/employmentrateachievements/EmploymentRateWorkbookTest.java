package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

/** Workbook security and real Spring completion callbacks are exercised without external services. */
class EmploymentRateWorkbookTest {
    @Test
    void textRoundtripKeepsKoreanNullAndFormulaLookingValuesInert() {
        List<List<String>> rows = List.of(List.of("교번", "상세"), List.of("E0101", "=1+1 & <안전>"));
        assertThat(EmploymentRateWorkbook.read(EmploymentRateWorkbook.write(rows))).isEqualTo(rows);
    }

    @Test
    void malformedContentRejected() {
        assertThatThrownBy(() -> EmploymentRateWorkbook.read("not XLSX".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void formulaCellRejected() throws Exception {
        byte[] workbook = replace("xl/worksheets/sheet1.xml", """
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData><row r="1"><c r="A1"><f>1+1</f><v>2</v></c></row></sheetData></worksheet>
                """);
        assertThatThrownBy(() -> EmploymentRateWorkbook.read(workbook)).isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void externalRelationshipRejected() throws Exception {
        byte[] workbook = replace("xl/_rels/workbook.xml.rels", """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" TargetMode="External" Target="https://example.com"/>
                </Relationships>
                """);
        assertThatThrownBy(() -> EmploymentRateWorkbook.read(workbook)).isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void xmlEntitiesRejected() throws Exception {
        byte[] workbook = replace("xl/worksheets/sheet1.xml", """
                <!DOCTYPE worksheet [<!ENTITY x SYSTEM "file:///etc/passwd">]>
                <worksheet><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>&x;</t></is></c>
                </row></sheetData></worksheet>
                """);
        assertThatThrownBy(() -> EmploymentRateWorkbook.read(workbook)).isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void rollbackDeletesOnlyNewFilesAndCommitPreservesFiles() {
        EmploymentRateFileStorage storage = new EmploymentRateFileStorage();
        TransactionTemplate transaction = new TransactionTemplate(new SynchronizingManager());
        String existing = storage.store(new byte[]{1}, "before");
        String[] rolledBack = new String[1];
        transaction.execute(status -> {
            rolledBack[0] = storage.store(new byte[]{2}, "rollback");
            status.setRollbackOnly();
            return null;
        });
        assertThat(storage.read(existing)).containsExactly((byte) 1);
        assertThatThrownBy(() -> storage.read(rolledBack[0]))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.NotFoundException.class);
        String committed = transaction.execute(status -> storage.store(new byte[]{3}, "commit"));
        assertThat(storage.read(committed)).containsExactly((byte) 3);
        storage.remove(existing, "cleanup");
        storage.remove(committed, "cleanup");
    }

    private byte[] replace(String path, String xml) throws IOException {
        byte[] initial = EmploymentRateWorkbook.write(List.of(List.of("header")));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(initial));
                ZipOutputStream zip = new ZipOutputStream(out)) {
            ZipEntry part;
            while ((part = input.getNextEntry()) != null) {
                byte[] data = input.readAllBytes();
                zip.putNextEntry(new ZipEntry(part.getName()));
                zip.write(part.getName().equals(path) ? xml.getBytes(StandardCharsets.UTF_8) : data);
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static class SynchronizingManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }
        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }
        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }
        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}

package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;

/** Exercises actual XLSX bytes, including hostile package and XML input. */
class EmploymentRateWorkbookTest {
    @Test
    void roundTripPreservesEmptyCellsUnicodeAndTextNotFormulas() {
        var rows = List.of(List.of("교번", "실적명", "첨부"), List.of("E101", "<실적 & 내용>", ""));
        assertThat(EmploymentRateWorkbook.read(EmploymentRateWorkbook.write(rows))).isEqualTo(rows);
    }

    @Test
    void renamedCsvIsRejected() {
        assertThatThrownBy(() -> EmploymentRateWorkbook.read("교번,실적명".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(EmploymentRateException.class);
    }

    @Test
    void formulaCellIsRejected() throws Exception {
        byte[] bytes = replaced("xl/worksheets/sheet1.xml", """
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData><row r="1"><c r="A1"><f>1+1</f><v>2</v></c></row></sheetData></worksheet>
                """);
        assertThatThrownBy(() -> EmploymentRateWorkbook.read(bytes)).isInstanceOf(EmploymentRateException.class);
    }

    @Test
    void externalEntityCannotReadHostFiles() throws Exception {
        byte[] bytes = replaced("xl/worksheets/sheet1.xml", """
                <!DOCTYPE worksheet [<!ENTITY external SYSTEM "file:///etc/passwd">]>
                <worksheet><sheetData><row><c t="inlineStr"><is><t>&external;</t></is></c></row></sheetData></worksheet>
                """);
        assertThatThrownBy(() -> EmploymentRateWorkbook.read(bytes)).isInstanceOf(EmploymentRateException.class);
    }

    @Test
    void externalRelationshipsAreRejected() throws Exception {
        byte[] bytes = replaced("xl/_rels/workbook.xml.rels", """
                <Relationships><Relationship TargetMode="External" Target="https://example.invalid"/></Relationships>
                """);
        assertThatThrownBy(() -> EmploymentRateWorkbook.read(bytes)).isInstanceOf(EmploymentRateException.class);
    }

    @Test
    void expandedZipBombIsRejected() throws Exception {
        byte[] bytes = replaced("xl/worksheets/sheet1.xml", "x".repeat(8 * 1024 * 1024 + 1));
        assertThatThrownBy(() -> EmploymentRateWorkbook.read(bytes)).isInstanceOf(EmploymentRateException.class);
    }

    private byte[] replaced(String name, String xml) throws IOException {
        byte[] original = EmploymentRateWorkbook.write(List.of(List.of("title")));
        var result = new ByteArrayOutputStream();
        try (var input = new ZipInputStream(new ByteArrayInputStream(original)); var output = new ZipOutputStream(result)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                output.putNextEntry(new ZipEntry(entry.getName()));
                output.write(entry.getName().equals(name) ? xml.getBytes(StandardCharsets.UTF_8) : input.readAllBytes());
                output.closeEntry();
            }
        }
        return result.toByteArray();
    }
}

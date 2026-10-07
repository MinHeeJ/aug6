package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmploymentRateWorkbookTest {
    @Test
    void roundTripsRealZipWorkbookAndPreservesTextWithoutFormulaExecution() {
        var source = List.of(
                List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조"),
                List.of("00123", "EDU001", "2026-04-10", "=SUM(1,2) & <실적>", ""));
        byte[] bytes = EmploymentRateWorkbook.write(source);
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
        var rows = EmploymentRateWorkbook.read(bytes);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).number()).isEqualTo(2);
        assertThat(rows.get(1).cells()).containsExactlyElementsOf(source.get(1));
    }

    @Test
    void rejectsCsvRenamedToXlsx() {
        assertThatThrownBy(() -> EmploymentRateWorkbook.read(
                "교번,관리항목코드\nE1001,EDU001".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readsSharedStringsSparseCellsAndStyledExcelDates() throws Exception {
        byte[] original = EmploymentRateWorkbook.write(List.of(List.of("placeholder")));
        byte[] fixture = replaceParts(original, java.util.Map.of(
                "xl/sharedStrings.xml", "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                        + "<si><r><t>00</t></r><r><t>123</t></r></si></sst>",
                "xl/styles.xml", "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                        + "<cellXfs count=\"1\"><xf numFmtId=\"14\"/></cellXfs></styleSheet>",
                "xl/worksheets/sheet1.xml", "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                        + "<sheetData><row r=\"7\"><c r=\"A7\" t=\"s\"><v>0</v></c>"
                        + "<c r=\"C7\" s=\"0\"><v>61</v></c></row></sheetData></worksheet>"));
        var rows = EmploymentRateWorkbook.read(fixture);
        assertThat(rows.get(0).number()).isEqualTo(7);
        assertThat(rows.get(0).cells()).containsExactly("00123", "", "1900-03-01");
    }

    @Test
    void rejectsFormulaCellsInsteadOfTrustingCachedResults() throws Exception {
        byte[] original = EmploymentRateWorkbook.write(List.of(List.of("placeholder")));
        byte[] fixture = replaceParts(original, java.util.Map.of(
                "xl/worksheets/sheet1.xml", "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                        + "<sheetData><row r=\"1\"><c r=\"A1\"><f>1+1</f><v>2</v></c>"
                        + "</row></sheetData></worksheet>"));
        assertThatThrownBy(() -> EmploymentRateWorkbook.read(fixture))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("수식");
    }

    @Test
    void rejectsDoctypeWithoutResolvingExternalEntities() throws Exception {
        byte[] original = EmploymentRateWorkbook.write(List.of(List.of("placeholder")));
        byte[] fixture = replaceParts(original, java.util.Map.of("xl/workbook.xml",
                "<!DOCTYPE workbook [<!ENTITY test SYSTEM 'file:///etc/passwd'>]>"
                        + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">&test;</workbook>"));
        assertThatThrownBy(() -> EmploymentRateWorkbook.read(fixture)).isInstanceOf(IllegalArgumentException.class);
    }

    private byte[] replaceParts(byte[] source, java.util.Map<String, String> replacements) throws Exception {
        var parts = new java.util.LinkedHashMap<String, byte[]>();
        try (var input = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(source))) {
            java.util.zip.ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) parts.put(entry.getName(), input.readAllBytes());
        }
        replacements.forEach((name, xml) -> parts.put(name, xml.getBytes(StandardCharsets.UTF_8)));
        var output = new java.io.ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(output)) {
            for (var part : parts.entrySet()) {
                zip.putNextEntry(new java.util.zip.ZipEntry(part.getKey()));
                zip.write(part.getValue());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    @Test
    void rejectsXmlControlCharactersInExport() {
        assertThatThrownBy(() -> EmploymentRateWorkbook.write(List.of(List.of("bad\u0001"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

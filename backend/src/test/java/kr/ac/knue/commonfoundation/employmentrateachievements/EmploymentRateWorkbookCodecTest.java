package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class EmploymentRateWorkbookCodecTest {
    private final EmploymentRateWorkbookCodec codec = new EmploymentRateWorkbookCodec();

    @Test
    void writesRealOoxmlAndPreservesLiteralText() {
        List<List<String>> rows = List.of(
                List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조"),
                List.of("00012", "FR-032", "2026-03-01", "홍길동 & <성과>", "=1+1"),
                List.of("", "끝"));
        byte[] bytes = codec.write(rows);
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
        assertThat(codec.read(bytes)).isEqualTo(rows);
    }

    @Test
    void readsIndependentSharedStringsAndRelationshipSelectedSparseWorksheet() throws Exception {
        byte[] bytes = archive(Map.of(
                "xl/workbook.xml", "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Data\" sheetId=\"1\" r:id=\"rId5\"/></sheets></workbook>",
                "xl/_rels/workbook.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId5\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/custom.xml\"/></Relationships>",
                "xl/sharedStrings.xml", "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><si><r><t>교</t></r><r><t>번</t></r></si><si><t>00042</t></si></sst>",
                "xl/worksheets/custom.xml", "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData><row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c></row><row r=\"3\"><c r=\"C3\" t=\"s\"><v>1</v></c></row></sheetData></worksheet>"));
        assertThat(codec.read(bytes)).isEqualTo(List.of(List.of("교번"), List.of(), List.of("", "", "00042")));
    }

    @Test
    void rejectsFormulaCellsRatherThanTrustingCachedValues() throws Exception {
        byte[] bytes = minimal("<row r=\"1\"><c r=\"A1\"><f>1+1</f><v>2</v></c></row>");
        assertThatThrownBy(() -> codec.read(bytes)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDoctypeAndExternalEntities() throws Exception {
        byte[] bytes = archive(Map.of(
                "xl/workbook.xml", "<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><workbook/>",
                "xl/_rels/workbook.xml.rels", "<Relationships/>"));
        assertThatThrownBy(() -> codec.read(bytes)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCsvDisguisedAsXlsxAndIllegalXmlCharacters() {
        assertThatThrownBy(() -> codec.read("교번,실적명".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.write(List.of(List.of("bad\u0000text"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOutOfBoundsSparseReferencesAndDuplicateCells() throws Exception {
        byte[] tooLarge = minimal("<row r=\"10002\"><c r=\"A10002\" t=\"inlineStr\"><is><t>x</t></is></c></row>");
        assertThatThrownBy(() -> codec.read(tooLarge)).isInstanceOf(IllegalArgumentException.class);
        byte[] duplicate = minimal("<row r=\"1\"><c r=\"A1\"/><c r=\"A1\"/></row>");
        assertThatThrownBy(() -> codec.read(duplicate)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsExternalWorksheetRelationshipEvenWithValidWorkbookAndSheet() throws Exception {
        byte[] bytes = related("https://example.invalid/sheet.xml", "TargetMode=\"External\"");
        assertThatThrownBy(() -> codec.read(bytes)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("외부");
    }

    @Test
    void rejectsTraversalInRelationshipsAndArchiveEntries() throws Exception {
        byte[] related = related("../secret.xml", "");
        assertThatThrownBy(() -> codec.read(related)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("경로");
        for (String path : List.of("../secret.xml", "/absolute.xml", "xl\\secret.xml")) {
            byte[] unsafe = archive(Map.of(path, "secret"));
            assertThatThrownBy(() -> codec.read(unsafe)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ZIP");
        }
    }

    @Test
    void rejectsDuplicateRelationshipIdsAndTooManyArchiveEntries() throws Exception {
        byte[] duplicate = archive(Map.of(
                "xl/workbook.xml", workbook(),
                "xl/_rels/workbook.xml.rels", relationships(
                        "<Relationship Id=\"r1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>".repeat(2)),
                "xl/worksheets/sheet1.xml", "<worksheet/>"));
        assertThatThrownBy(() -> codec.read(duplicate)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("중복");
        Map<String, String> parts = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 129; i++) parts.put("part" + i, "x");
        byte[] many = archive(parts);
        assertThatThrownBy(() -> codec.read(many)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ZIP");
    }

    @Test
    void rejectsErrorCellsBadSharedStringIndicesAndMismatchedCellRows() throws Exception {
        for (String row : List.of(
                "<row r=\"1\"><c r=\"A1\" t=\"e\"><v>#REF!</v></c></row>",
                "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>999</v></c></row>",
                "<row r=\"1\"><c r=\"A2\" t=\"inlineStr\"><is><t>x</t></is></c></row>",
                "<row r=\"1\"><c r=\"AG1\"/></row>",
                "<row r=\"1\"/><row r=\"1\"/>")) {
            byte[] bytes = minimal(row);
            assertThatThrownBy(() -> codec.read(bytes)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void preservesUnicodeWhitespaceLineBreaksAndFormulaLikeTextWithoutFormulaElements() throws Exception {
        var rows = List.of(List.of(" 00001 ", "한글 🎓", "a\r\nb\tc", "=SUM(A1)", "+1", "@cmd"));
        byte[] bytes = codec.write(rows);
        assertThat(codec.read(bytes)).isEqualTo(rows);
        try (var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            boolean found = false;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.getName().equals("xl/worksheets/sheet1.xml")) continue;
                String sheet = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                assertThat(sheet).contains("t=\"inlineStr\"", "=SUM(A1)").doesNotContain("<f>", "<f ");
                found = true;
            }
            assertThat(found).isTrue();
        }
    }

    @Test
    void rejectsOversizedInputOutputColumnsAndCellText() {
        assertThatThrownBy(() -> codec.read(new byte[EmploymentRateWorkbookCodec.MAX_INPUT_BYTES + 1]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.write(List.of(java.util.Collections.nCopies(33, "x"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.write(List.of(List.of("x".repeat(32768)))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String workbook() {
        return "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Data\" sheetId=\"1\" r:id=\"r1\"/></sheets></workbook>";
    }

    private static String relationships(String links) {
        return "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" + links + "</Relationships>";
    }

    private static byte[] related(String target, String attributes) throws Exception {
        return archive(Map.of("xl/workbook.xml", workbook(),
                "xl/_rels/workbook.xml.rels", relationships("<Relationship Id=\"r1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"" + target + "\" " + attributes + "/>"),
                "xl/worksheets/sheet1.xml", "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData/></worksheet>"));
    }

    private static byte[] minimal(String rows) throws Exception {
        return archive(Map.of(
                "xl/workbook.xml", "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Data\" sheetId=\"1\" r:id=\"r1\"/></sheets></workbook>",
                "xl/_rels/workbook.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"r1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>",
                "xl/worksheets/sheet1.xml", "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>" + rows + "</sheetData></worksheet>"));
    }

    private static byte[] archive(Map<String, String> parts) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (var part : parts.entrySet()) {
                zip.putNextEntry(new ZipEntry(part.getKey()));
                zip.write(part.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}

package kr.ac.knue.commonfoundation.basic83;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Writes the contract's small, dependency-free Office Open XML download workbook. */
final class EmploymentRateAchievementWorkbook {
    private EmploymentRateAchievementWorkbook() {
    }

    static byte[] create(List<EmploymentRateAchievementRow> rows) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            write(zip, "[Content_Types].xml", contentTypes());
            write(zip, "_rels/.rels", rootRelationships());
            write(zip, "xl/workbook.xml", workbook());
            write(zip, "xl/_rels/workbook.xml.rels", workbookRelationships());
            write(zip, "xl/styles.xml", styles());
            write(zip, "xl/worksheets/sheet1.xml", sheet(rows));
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("취업률 실적 Excel 파일을 만들 수 없습니다.", exception);
        }
    }

    private static void write(ZipOutputStream zip, String path, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
                """;
    }

    private static String rootRelationships() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;
    }

    private static String workbook() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="취업률 실적" sheetId="1" r:id="rId1"/>
                  </sheets>
                </workbook>
                """;
    }

    private static String workbookRelationships() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """;
    }

    private static String styles() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="1"><font><sz val="11"/><name val="Arial"/></font></fonts>
                  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
                  <borders count="1"><border/></borders>
                  <cellStyleXfs count="1"><xf/></cellStyleXfs>
                  <cellXfs count="1"><xf xfId="0"/></cellXfs>
                </styleSheet>
                """;
    }

    private static String sheet(List<EmploymentRateAchievementRow> rows) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                """);
        appendRow(
                xml,
                1,
                List.of(
                        "관리번호",
                        "교원",
                        "평가연도",
                        "관리항목",
                        "업적발생일",
                        "실적명",
                        "인증상태"));
        int rowNumber = 2;
        for (EmploymentRateAchievementRow row : rows) {
            appendRow(
                    xml,
                    rowNumber++,
                    List.of(
                            value(row.managementNo()),
                            value(row.teacherName()),
                            value(row.evaluationYear()),
                            value(row.managementItemCode()),
                            value(row.achievementDate()),
                            value(row.achievementName()),
                            value(row.certificationStatus())));
        }
        xml.append("""
                  </sheetData>
                </worksheet>
                """);
        return xml.toString();
    }

    private static void appendRow(StringBuilder xml, int rowNumber, List<String> values) {
        xml.append("<row r=\"").append(rowNumber).append("\">");
        for (int column = 0; column < values.size(); column++) {
            xml.append("<c r=\"")
                    .append((char) ('A' + column))
                    .append(rowNumber)
                    .append("\" t=\"inlineStr\"><is><t>")
                    .append(escape(values.get(column)))
                    .append("</t></is></c>");
        }
        xml.append("</row>");
    }

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reads and writes the small, fixed employment-rate XLSX table without adding
 * a second spreadsheet dependency to the established Spring Boot application.
 */
final class XlsxWorkbook {
    private static final String SHEET_PATH = "xl/worksheets/sheet1.xml";

    private XlsxWorkbook() {
    }

    static List<List<String>> read(byte[] content) throws IOException {
        Map<String, byte[]> entries = unzip(content);
        byte[] sheet = entries.get(SHEET_PATH);
        if (sheet == null) {
            throw new IOException("첫 번째 worksheet를 찾을 수 없습니다.");
        }
        List<String> sharedStrings = entries.containsKey("xl/sharedStrings.xml")
                ? strings(entries.get("xl/sharedStrings.xml"))
                : List.of();
        Document document = xml(sheet);
        NodeList rowNodes = document.getElementsByTagNameNS("*", "row");
        List<List<String>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rowNodes.getLength(); rowIndex++) {
            Element row = (Element) rowNodes.item(rowIndex);
            NodeList cells = row.getElementsByTagNameNS("*", "c");
            List<String> values = new ArrayList<>();
            for (int cellIndex = 0; cellIndex < cells.getLength(); cellIndex++) {
                Element cell = (Element) cells.item(cellIndex);
                int column = columnIndex(cell.getAttribute("r"));
                while (values.size() <= column) {
                    values.add("");
                }
                values.set(column, cellValue(cell, sharedStrings));
            }
            rows.add(values);
        }
        return rows;
    }

    static byte[] write(List<List<String>> rows) throws IOException {
        try (
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(bytes)) {
            entry(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels"
                        ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml"
                        ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml"
                        ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """);
            entry(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1"
                        Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
                        Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
            entry(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                      xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets><sheet name="취업률 실적" sheetId="1" r:id="rId1"/></sheets>
                    </workbook>
                    """);
            entry(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1"
                        Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                        Target="worksheets/sheet1.xml"/>
                    </Relationships>
                    """);
            entry(zip, SHEET_PATH, sheet(rows));
            return bytes.toByteArray();
        }
    }

    private static Map<String, byte[]> unzip(byte[] content) throws IOException {
        if (content == null || content.length < 4 || content[0] != 'P' || content[1] != 'K') {
            throw new IOException("XLSX ZIP 형식이 아닙니다.");
        }
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().startsWith("/") || entry.getName().contains("..")) {
                    throw new IOException("허용되지 않은 XLSX 항목입니다.");
                }
                entries.put(entry.getName(), zip.readAllBytes());
            }
        }
        return entries;
    }

    private static List<String> strings(byte[] content) throws IOException {
        Document document = xml(content);
        NodeList nodes = document.getElementsByTagNameNS("*", "si");
        List<String> result = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            result.add(nodes.item(index).getTextContent());
        }
        return result;
    }

    private static String cellValue(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            NodeList strings = cell.getElementsByTagNameNS("*", "t");
            return strings.getLength() == 0 ? "" : strings.item(0).getTextContent();
        }
        NodeList values = cell.getElementsByTagNameNS("*", "v");
        String value = values.getLength() == 0 ? "" : values.item(0).getTextContent();
        if ("s".equals(type) && !value.isBlank()) {
            int index = Integer.parseInt(value);
            if (index < 0 || index >= sharedStrings.size()) {
                throw new IllegalArgumentException("공유 문자열 인덱스가 올바르지 않습니다.");
            }
            return sharedStrings.get(index);
        }
        return value;
    }

    private static Document xml(byte[] content) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(content));
        } catch (Exception exception) {
            throw new IOException("XLSX XML 형식이 올바르지 않습니다.", exception);
        }
    }

    private static int columnIndex(String reference) {
        int result = 0;
        for (int index = 0; index < reference.length(); index++) {
            char value = reference.charAt(index);
            if (value < 'A' || value > 'Z') {
                break;
            }
            result = result * 26 + value - 'A' + 1;
        }
        return Math.max(0, result - 1);
    }

    private static String sheet(List<List<String>> rows) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            xml.append("<row r=\"").append(rowIndex + 1).append("\">");
            List<String> row = rows.get(rowIndex);
            for (int column = 0; column < row.size(); column++) {
                xml.append("<c r=\"").append(columnName(column)).append(rowIndex + 1)
                        .append("\" t=\"inlineStr\"><is><t>").append(escape(row.get(column)))
                        .append("</t></is></c>");
            }
            xml.append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String columnName(int index) {
        StringBuilder name = new StringBuilder();
        int value = index + 1;
        while (value > 0) {
            int remainder = (value - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return name.toString();
    }

    private static String escape(String value) {
        return (value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

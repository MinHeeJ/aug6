package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Bounded OOXML codec: no formulas, DTDs, entities, external relationships or ZIP path extraction. */
public final class EmploymentRateXlsxCodec {
    public static final String MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final int MAX_BYTES = 20 * 1024 * 1024;

    private EmploymentRateXlsxCodec() {
    }

    /** Parses the single workbook worksheet, preserving blank columns and accepting shared or inline strings. */
    public static List<List<String>> read(byte[] bytes) {
        try {
            Map<String, byte[]> entries = new HashMap<>();
            int total = 0;
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entries.size() >= 100 || entry.getName().contains("..") || entry.getName().startsWith("/")) {
                        throw new IllegalArgumentException();
                    }
                    byte[] content = zip.readNBytes(MAX_BYTES - total + 1);
                    total += content.length;
                    if (total > MAX_BYTES || entries.putIfAbsent(entry.getName(), content) != null) {
                        throw new IllegalArgumentException();
                    }
                }
            }
            if (!entries.containsKey("[Content_Types].xml") || !entries.containsKey("xl/workbook.xml")) {
                throw new IllegalArgumentException();
            }
            for (var entry : entries.entrySet()) {
                if (entry.getKey().endsWith(".rels")) {
                    NodeList relationships = xml(entry.getValue()).getElementsByTagNameNS("*", "Relationship");
                    for (int i = 0; i < relationships.getLength(); i++) {
                        Element relationship = (Element) relationships.item(i);
                        if ("External".equalsIgnoreCase(relationship.getAttribute("TargetMode"))) {
                            throw new IllegalArgumentException();
                        }
                    }
                }
                if (entry.getKey().contains("vbaProject") || entry.getKey().startsWith("xl/externalLinks/")) {
                    throw new IllegalArgumentException();
                }
            }
            NodeList sheets = xml(entries.get("xl/workbook.xml")).getElementsByTagNameNS("*", "sheet");
            if (sheets.getLength() != 1) {
                throw new IllegalArgumentException();
            }
            Element sheet = (Element) sheets.item(0);
            String relationshipId = sheet.getAttributeNS(
                    "http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id");
            NodeList relationships = xml(entries.get("xl/_rels/workbook.xml.rels"))
                    .getElementsByTagNameNS("*", "Relationship");
            String target = null;
            for (int i = 0; i < relationships.getLength(); i++) {
                Element relationship = (Element) relationships.item(i);
                if (relationshipId.equals(relationship.getAttribute("Id"))) {
                    target = relationship.getAttribute("Target");
                }
            }
            if (target == null || target.contains("..")) {
                throw new IllegalArgumentException();
            }
            String worksheet = target.startsWith("/xl/") ? target.substring(1) : "xl/" + target;
            Document doc = xml(entries.get(worksheet));
            if (doc.getElementsByTagNameNS("*", "f").getLength() > 0) {
                throw new IllegalArgumentException();
            }
            List<String> strings = new ArrayList<>();
            if (entries.containsKey("xl/sharedStrings.xml")) {
                NodeList shared = xml(entries.get("xl/sharedStrings.xml")).getElementsByTagNameNS("*", "si");
                for (int i = 0; i < shared.getLength(); i++) {
                    strings.add(text((Element) shared.item(i), "t"));
                }
            }
            List<List<String>> rows = new ArrayList<>();
            NodeList nodes = doc.getElementsByTagNameNS("*", "row");
            if (nodes.getLength() > 5001) {
                throw new IllegalArgumentException();
            }
            for (int i = 0; i < nodes.getLength(); i++) {
                Element row = (Element) nodes.item(i);
                NodeList cells = row.getElementsByTagNameNS("*", "c");
                List<String> values = new ArrayList<>();
                for (int j = 0; j < cells.getLength(); j++) {
                    Element cell = (Element) cells.item(j);
                    String ref = cell.getAttribute("r");
                    if (!ref.matches("[A-Z]{1,2}[1-9][0-9]{0,6}")) {
                        throw new IllegalArgumentException();
                    }
                    int column = 0;
                    for (char letter : ref.replaceAll("[0-9]", "").toCharArray()) {
                        column = column * 26 + letter - 'A' + 1;
                    }
                    if (column > 50 || column <= values.size()) {
                        throw new IllegalArgumentException();
                    }
                    while (values.size() < column) {
                        values.add("");
                    }
                    String type = cell.getAttribute("t");
                    String value = "inlineStr".equals(type) ? text(cell, "t") : text(cell, "v");
                    if ("s".equals(type)) {
                        value = strings.get(Integer.parseInt(value));
                    }
                    if (value.length() > 10000 || "e".equals(type)) {
                        throw new IllegalArgumentException();
                    }
                    values.set(column - 1, value);
                }
                if (values.stream().anyMatch(value -> !value.isBlank())) {
                    rows.add(values);
                }
            }
            if (rows.isEmpty()) {
                throw new IllegalArgumentException();
            }
            return rows;
        } catch (Exception exception) {
            throw new IllegalArgumentException("표준 단일 시트 XLSX만 지원합니다. 손상·수식·외부 참조 파일은 허용하지 않습니다.");
        }
    }

    /** Produces genuine XLSX bytes; all exported values are literal strings, never spreadsheet formulas. */
    public static byte[] write(List<List<String>> rows) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                entry(zip, "[Content_Types].xml", """
                        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                          <Default Extension="xml" ContentType="application/xml"/>
                          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                        </Types>
                        """);
                entry(zip, "_rels/.rels", """
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                        </Relationships>
                        """);
                entry(zip, "xl/workbook.xml", """
                        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                          <sheets><sheet name="취업률 실적" sheetId="1" r:id="rId1"/></sheets>
                        </workbook>
                        """);
                entry(zip, "xl/_rels/workbook.xml.rels", """
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                        </Relationships>
                        """);
                StringBuilder sheet = new StringBuilder("<worksheet xmlns=\"" + NS + "\"><sheetData>");
                for (int i = 0; i < rows.size(); i++) {
                    sheet.append("<row r=\"").append(i + 1).append("\">");
                    for (int j = 0; j < rows.get(i).size(); j++) {
                        String column = j < 26 ? String.valueOf((char) ('A' + j))
                                : "A" + (char) ('A' + j - 26);
                        sheet.append("<c r=\"").append(column).append(i + 1)
                                .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                                .append(escape(rows.get(i).get(j))).append("</t></is></c>");
                    }
                    sheet.append("</row>");
                }
                sheet.append("</sheetData></worksheet>");
                entry(zip, "xl/worksheets/sheet1.xml", sheet.toString());
            }
            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("XLSX 파일 생성에 실패했습니다.", exception);
        }
    }

    private static Document xml(byte[] bytes) throws Exception {
        if (bytes == null) {
            throw new IllegalArgumentException();
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    private static String text(Element node, String name) {
        NodeList parts = node.getElementsByTagNameNS("*", name);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.getLength(); i++) {
            result.append(parts.item(i).getTextContent());
        }
        return result.toString();
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}

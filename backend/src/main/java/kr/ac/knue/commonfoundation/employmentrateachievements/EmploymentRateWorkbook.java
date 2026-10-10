package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Bounded XLSX codec: inline/shared strings, no formulas, external entities, relationships or macros. */
public final class EmploymentRateWorkbook {
    public static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int MAX_BYTES = 8 * 1024 * 1024;

    private EmploymentRateWorkbook() {
    }

    public static byte[] write(List<List<String>> rows) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                entry(zip, "[Content_Types].xml", """
                        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                        <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                        <Default Extension="xml" ContentType="application/xml"/>
                        <Override PartName="/xl/workbook.xml"
                        ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                        <Override PartName="/xl/worksheets/sheet1.xml"
                        ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                        </Types>
                        """);
                entry(zip, "_rels/.rels", """
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                        <Relationship Id="rId1"
                        Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
                        Target="xl/workbook.xml"/></Relationships>
                        """);
                entry(zip, "xl/workbook.xml", """
                        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                        xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                        <sheets><sheet name="취업률 실적" sheetId="1" r:id="rId1"/></sheets></workbook>
                        """);
                entry(zip, "xl/_rels/workbook.xml.rels", """
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                        <Relationship Id="rId1"
                        Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                        Target="worksheets/sheet1.xml"/></Relationships>
                        """);
                StringBuilder sheet = new StringBuilder(
                        "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
                int rowNumber = 0;
                for (List<String> row : rows) {
                    sheet.append("<row r=\"").append(++rowNumber).append("\">");
                    for (int column = 0; column < row.size(); column++) {
                        sheet.append("<c r=\"").append(columnName(column)).append(rowNumber)
                                .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                                .append(escape(row.get(column))).append("</t></is></c>");
                    }
                    sheet.append("</row>");
                }
                entry(zip, "xl/worksheets/sheet1.xml", sheet.append("</sheetData></worksheet>").toString());
            }
            return output.toByteArray();
        } catch (IOException failure) {
            throw new IllegalStateException("Workbook serialization failed", failure);
        }
    }

    public static List<List<String>> read(byte[] bytes) {
        if (bytes.length > MAX_BYTES || bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
            throw invalid();
        }
        try {
            Map<String, byte[]> parts = new HashMap<>();
            int total = 0;
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (parts.size() >= 100 || name.contains("..") || name.startsWith("/")
                            || name.contains("vbaProject") || name.contains("externalLinks")) {
                        throw invalid();
                    }
                    byte[] part = zip.readNBytes(MAX_BYTES + 1);
                    total += part.length;
                    if (total > MAX_BYTES || parts.put(name, part) != null) {
                        throw invalid();
                    }
                    if (name.endsWith(".rels")) {
                        NodeList relations = xml(part).getElementsByTagNameNS("*", "Relationship");
                        for (int i = 0; i < relations.getLength(); i++) {
                            Element relation = (Element) relations.item(i);
                            if ("External".equals(relation.getAttribute("TargetMode"))) {
                                throw invalid();
                            }
                        }
                    }
                }
            }
            if (!parts.containsKey("[Content_Types].xml") || !parts.containsKey("xl/workbook.xml")) {
                throw invalid();
            }
            List<String> shared = new ArrayList<>();
            if (parts.containsKey("xl/sharedStrings.xml")) {
                NodeList strings = xml(parts.get("xl/sharedStrings.xml")).getElementsByTagNameNS("*", "si");
                for (int i = 0; i < strings.getLength(); i++) {
                    shared.add(strings.item(i).getTextContent());
                }
            }
            Document sheet = xml(Objects.requireNonNull(parts.get("xl/worksheets/sheet1.xml")));
            if (sheet.getElementsByTagNameNS("*", "f").getLength() > 0) {
                throw invalid();
            }
            NodeList rows = sheet.getElementsByTagNameNS("*", "row");
            if (rows.getLength() > 1001) {
                throw invalid();
            }
            List<List<String>> result = new ArrayList<>();
            for (int i = 0; i < rows.getLength(); i++) {
                NodeList cells = ((Element) rows.item(i)).getElementsByTagNameNS("*", "c");
                List<String> values = new ArrayList<>();
                for (int j = 0; j < cells.getLength(); j++) {
                    Element cell = (Element) cells.item(j);
                    String reference = cell.getAttribute("r").replaceAll("[0-9]", "");
                    int column = 0;
                    for (char c : reference.toCharArray()) {
                        column = column * 26 + c - 'A' + 1;
                    }
                    column = reference.isEmpty() ? values.size() : column - 1;
                    if (column < 0 || column > 50) {
                        throw invalid();
                    }
                    while (values.size() <= column) {
                        values.add("");
                    }
                    NodeList valueNodes = cell.getElementsByTagNameNS("*", "v");
                    String value = "inlineStr".equals(cell.getAttribute("t"))
                            ? cell.getTextContent() : valueNodes.getLength() == 0 ? "" : valueNodes.item(0).getTextContent();
                    if ("s".equals(cell.getAttribute("t"))) {
                        value = shared.get(Integer.parseInt(value));
                    }
                    if (value.length() > 2000 || value.stripLeading().matches("^[=+@].*")) {
                        throw invalid();
                    }
                    values.set(column, value.trim());
                }
                result.add(values);
            }
            return result;
        } catch (EmploymentRateException failure) {
            throw failure;
        } catch (Exception failure) {
            throw invalid();
        }
    }

    private static Document xml(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    private static void entry(ZipOutputStream zip, String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String columnName(int index) {
        StringBuilder result = new StringBuilder();
        for (int n = index + 1; n > 0; n = (n - 1) / 26) {
            result.insert(0, (char) ('A' + (n - 1) % 26));
        }
        return result.toString();
    }

    private static String escape(String text) {
        return Objects.toString(text, "").replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static EmploymentRateException invalid() {
        return new EmploymentRateException("INVALID_WORKBOOK", "안전한 XLSX 파일과 템플릿을 사용하세요.", 400);
    }
}

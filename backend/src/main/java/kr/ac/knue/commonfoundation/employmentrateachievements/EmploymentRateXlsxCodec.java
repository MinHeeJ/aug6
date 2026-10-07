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

/** Bounded OOXML text-table codec; no macros, formulas, external entities or external file access. */
public final class EmploymentRateXlsxCodec {
    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int MAX_BYTES = 20 * 1024 * 1024;

    private EmploymentRateXlsxCodec() {
    }

    /** Reads shared and inline strings, preserving missing cells by their Excel column reference. */
    public static List<List<String>> read(byte[] bytes) {
        try {
            Map<String, byte[]> parts = new HashMap<>();
            int size = 0;
            int entries = 0;
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (++entries > 200) throw new IllegalArgumentException("Excel 파일 항목이 너무 많습니다.");
                    byte[] part = zip.readNBytes(MAX_BYTES + 1);
                    size += part.length;
                    if (size > MAX_BYTES) throw new IllegalArgumentException("Excel 파일 크기를 확인하세요.");
                    parts.put(entry.getName(), part);
                }
            }
            if (!parts.containsKey("[Content_Types].xml") || !parts.containsKey("xl/workbook.xml")) {
                throw new IllegalArgumentException("실제 XLSX 파일을 선택하세요.");
            }
            List<String> strings = new ArrayList<>();
            if (parts.containsKey("xl/sharedStrings.xml")) {
                NodeList nodes = xml(parts.get("xl/sharedStrings.xml")).getElementsByTagName("si");
                for (int i = 0; i < nodes.getLength(); i++) strings.add(nodes.item(i).getTextContent());
            }
            String sheet = "xl/worksheets/sheet1.xml";
            Document workbook = xml(parts.get("xl/workbook.xml"));
            NodeList sheets = workbook.getElementsByTagName("sheet");
            if (sheets.getLength() > 0 && parts.containsKey("xl/_rels/workbook.xml.rels")) {
                String relation = ((Element) sheets.item(0)).getAttribute("r:id");
                NodeList rels = xml(parts.get("xl/_rels/workbook.xml.rels")).getElementsByTagName("Relationship");
                for (int i = 0; i < rels.getLength(); i++) {
                    Element rel = (Element) rels.item(i);
                    if (relation.equals(rel.getAttribute("Id"))) {
                        String target = rel.getAttribute("Target");
                        if (target.contains("..") || "External".equals(rel.getAttribute("TargetMode"))) {
                            throw new IllegalArgumentException("외부 Excel 참조는 지원하지 않습니다.");
                        }
                        sheet = target.startsWith("/") ? target.substring(1) : "xl/" + target;
                    }
                }
            }
            if (!parts.containsKey(sheet)) throw new IllegalArgumentException("Excel 시트가 없습니다.");
            NodeList rows = xml(parts.get(sheet)).getElementsByTagName("row");
            if (rows.getLength() > 10001) throw new IllegalArgumentException("최대 10000행까지 지원합니다.");
            List<List<String>> result = new ArrayList<>();
            for (int i = 0; i < rows.getLength(); i++) {
                Element row = (Element) rows.item(i);
                NodeList cells = row.getElementsByTagName("c");
                List<String> values = new ArrayList<>();
                for (int j = 0; j < cells.getLength(); j++) {
                    Element cell = (Element) cells.item(j);
                    if (cell.getElementsByTagName("f").getLength() > 0) {
                        throw new IllegalArgumentException("수식 대신 입력값을 사용하세요.");
                    }
                    int column = column(cell.getAttribute("r"));
                    if (column > 100) throw new IllegalArgumentException("Excel 열 수를 확인하세요.");
                    while (values.size() <= column) values.add("");
                    NodeList nodes = cell.getElementsByTagName("v");
                    String value = nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
                    if ("s".equals(cell.getAttribute("t"))) value = strings.get(Integer.parseInt(value));
                    if ("inlineStr".equals(cell.getAttribute("t"))) {
                        value = cell.getElementsByTagName("is").item(0).getTextContent();
                    }
                    values.set(column, value.trim());
                }
                result.add(values);
            }
            return result;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Excel 형식이 올바르지 않습니다.");
        }
    }

    /** Produces a real XLSX workbook using text cells, so user content cannot become an Excel formula. */
    public static byte[] write(List<List<String>> rows) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                String spreadsheet = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
                String relationships = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
                part(zip, "[Content_Types].xml", """
                        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                          <Default Extension="xml" ContentType="application/xml"/>
                          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                        </Types>
                        """);
                part(zip, "_rels/.rels", """
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                        </Relationships>
                        """);
                part(zip, "xl/workbook.xml", "<workbook xmlns=\"" + spreadsheet + "\" xmlns:r=\""
                        + relationships + "\"><sheets><sheet name=\"취업률\" sheetId=\"1\" r:id=\"rId1\"/>"
                        + "</sheets></workbook>");
                part(zip, "xl/_rels/workbook.xml.rels", """
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                        </Relationships>
                        """);
                StringBuilder sheet = new StringBuilder("<worksheet xmlns=\"" + spreadsheet + "\"><sheetData>");
                for (int i = 0; i < rows.size(); i++) {
                    sheet.append("<row r=\"").append(i + 1).append("\">");
                    List<String> values = rows.get(i);
                    for (int j = 0; j < values.size(); j++) {
                        sheet.append("<c r=\"").append(columnName(j)).append(i + 1)
                                .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                                .append(escape(values.get(j))).append("</t></is></c>");
                    }
                    sheet.append("</row>");
                }
                part(zip, "xl/worksheets/sheet1.xml", sheet.append("</sheetData></worksheet>").toString());
            }
            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Excel 생성에 실패했습니다.", exception);
        }
    }

    private static Document xml(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    private static int column(String reference) {
        int result = 0;
        for (char letter : reference.toCharArray()) {
            if (letter < 'A' || letter > 'Z') break;
            result = result * 26 + letter - 'A' + 1;
        }
        return Math.max(0, result - 1);
    }

    private static String columnName(int value) {
        StringBuilder name = new StringBuilder();
        for (int n = value + 1; n > 0; n = (n - 1) / 26) name.insert(0, (char) ('A' + (n - 1) % 26));
        return name.toString();
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void part(ZipOutputStream zip, String name, String xml) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}

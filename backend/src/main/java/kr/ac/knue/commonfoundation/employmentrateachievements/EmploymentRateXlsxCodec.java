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
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Bounded, dependency-free OOXML codec; formulas, DTDs and external relationships are forbidden. */
@Component
public class EmploymentRateXlsxCodec {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";

    public List<List<String>> read(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw invalid();
        }
        try {
            Map<String, byte[]> entries = new HashMap<>();
            int total = 0;
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (entries.size() >= 100 || name.contains("..") || name.startsWith("/")
                            || name.contains("\\") || entries.containsKey(name)) {
                        throw invalid();
                    }
                    byte[] data = zip.readNBytes(MAX_BYTES + 1);
                    total += data.length;
                    if (data.length > MAX_BYTES || total > 20 * 1024 * 1024
                            || total > Math.max(1024 * 1024, bytes.length * 100L)) {
                        throw invalid();
                    }
                    entries.put(name, data);
                    if (name.endsWith(".rels")) {
                        NodeList links = parse(data).getElementsByTagNameNS("*", "Relationship");
                        for (int i = 0; i < links.getLength(); i++) {
                            Element link = (Element) links.item(i);
                            if ("External".equalsIgnoreCase(link.getAttribute("TargetMode"))) {
                                throw invalid();
                            }
                        }
                    }
                    if (name.toLowerCase().contains("vbaproject") || name.contains("externalLinks/")) {
                        throw invalid();
                    }
                }
            }
            if (!entries.containsKey("[Content_Types].xml") || !entries.containsKey("xl/workbook.xml")) {
                throw invalid();
            }
            parse(entries.get("[Content_Types].xml"));
            parse(entries.get("xl/workbook.xml"));
            List<String> shared = new ArrayList<>();
            if (entries.containsKey("xl/sharedStrings.xml")) {
                NodeList strings = parse(entries.get("xl/sharedStrings.xml")).getElementsByTagNameNS(NS, "si");
                for (int i = 0; i < strings.getLength(); i++) {
                    shared.add(strings.item(i).getTextContent());
                }
            }
            Document sheet = parse(entries.get("xl/worksheets/sheet1.xml"));
            if (sheet.getElementsByTagNameNS(NS, "f").getLength() > 0) {
                throw invalid();
            }
            NodeList rows = sheet.getElementsByTagNameNS(NS, "row");
            if (rows.getLength() > 10001) {
                throw invalid();
            }
            List<List<String>> result = new ArrayList<>();
            for (int i = 0; i < rows.getLength(); i++) {
                Element row = (Element) rows.item(i);
                NodeList cells = row.getElementsByTagNameNS(NS, "c");
                List<String> values = new ArrayList<>();
                for (int j = 0; j < cells.getLength(); j++) {
                    Element cell = (Element) cells.item(j);
                    int column = column(cell.getAttribute("r"));
                    if (column < 0 || column > 50) {
                        throw invalid();
                    }
                    while (values.size() <= column) {
                        values.add("");
                    }
                    NodeList text = cell.getElementsByTagNameNS(NS, "v");
                    String value = text.getLength() == 0 ? "" : text.item(0).getTextContent();
                    if ("s".equals(cell.getAttribute("t"))) {
                        value = shared.get(Integer.parseInt(value));
                    } else if ("inlineStr".equals(cell.getAttribute("t"))) {
                        value = cell.getTextContent();
                    }
                    if (value.length() > 10000) {
                        throw invalid();
                    }
                    values.set(column, value.trim());
                }
                result.add(values);
            }
            return result;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private int column(String reference) {
        int value = 0;
        for (char c : reference.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                value = value * 26 + c - 'A' + 1;
            } else {
                break;
            }
        }
        return value - 1;
    }

    private Document parse(byte[] xml) throws Exception {
        if (xml == null) {
            throw invalid();
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    /** Writes cells as strings, never formulas, including untrusted values beginning with '='. */
    public byte[] write(List<List<String>> rows) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
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
                        <sheets><sheet name="실적" sheetId="1" r:id="rId1"/></sheets></workbook>
                        """);
                entry(zip, "xl/_rels/workbook.xml.rels", """
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                        <Relationship Id="rId1"
                        Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                        Target="worksheets/sheet1.xml"/></Relationships>
                        """);
                StringBuilder sheet = new StringBuilder("<worksheet xmlns=\"" + NS + "\"><sheetData>");
                int rowNumber = 0;
                for (List<String> row : rows) {
                    sheet.append("<row r=\"").append(++rowNumber).append("\">");
                    int column = 0;
                    for (String cell : row) {
                        sheet.append("<c r=\"").append((char) ('A' + column++)).append(rowNumber)
                                .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                                .append(escape(cell)).append("</t></is></c>");
                    }
                    sheet.append("</row>");
                }
                entry(zip, "xl/worksheets/sheet1.xml", sheet.append("</sheetData></worksheet>").toString());
            }
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Excel 파일을 생성하지 못했습니다.", exception);
        }
    }

    private String escape(String value) {
        return (value == null ? "" : value).replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void entry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private IllegalArgumentException invalid() {
        return new IllegalArgumentException("안전한 .xlsx 표준 양식만 업로드할 수 있습니다.");
    }
}

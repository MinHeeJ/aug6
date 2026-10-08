package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Bounded OOXML reader/writer; rejects formulas, external relationships and XML entities. */
public final class EmploymentRateWorkbook {
    private EmploymentRateWorkbook() {
    }

    public static List<List<String>> read(byte[] content) {
        try {
            Map<String, byte[]> parts = new HashMap<>();
            int total = 0;
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (parts.size() >= 200 || entry.getName().contains("..") || entry.getName().startsWith("/")) {
                        throw new IOException("unsafe zip");
                    }
                    byte[] data = zip.readNBytes(8 * 1024 * 1024 + 1);
                    total += data.length;
                    if (data.length > 8 * 1024 * 1024 || total > 24 * 1024 * 1024) {
                        throw new IOException("zip too large");
                    }
                    if (parts.put(entry.getName(), data) != null) throw new IOException("duplicate entry");
                }
            }
            if (!parts.containsKey("[Content_Types].xml") || !parts.containsKey("xl/workbook.xml")
                    || !parts.containsKey("xl/worksheets/sheet1.xml")) throw new IOException("not workbook");
            for (Map.Entry<String, byte[]> part : parts.entrySet()) {
                if (part.getKey().endsWith(".xml") || part.getKey().endsWith(".rels")) {
                    Document doc = parse(part.getValue());
                    if (doc.getElementsByTagNameNS("*", "f").getLength() > 0) throw new IOException("formula");
                    NodeList relationships = doc.getElementsByTagNameNS("*", "Relationship");
                    for (int i = 0; i < relationships.getLength(); i++) {
                        if ("External".equals(((Element) relationships.item(i)).getAttribute("TargetMode"))) {
                            throw new IOException("external relationship");
                        }
                    }
                }
                if (part.getKey().endsWith(".bin")) throw new IOException("embedded binary");
            }
            List<String> strings = new ArrayList<>();
            if (parts.containsKey("xl/sharedStrings.xml")) {
                NodeList nodes = parse(parts.get("xl/sharedStrings.xml")).getElementsByTagNameNS("*", "si");
                for (int i = 0; i < nodes.getLength(); i++) strings.add(nodes.item(i).getTextContent());
            }
            NodeList rows = parse(parts.get("xl/worksheets/sheet1.xml")).getElementsByTagNameNS("*", "row");
            if (rows.getLength() > 10001) throw new IOException("too many rows");
            List<List<String>> result = new ArrayList<>();
            for (int r = 0; r < rows.getLength(); r++) {
                List<String> values = new ArrayList<>();
                NodeList cells = ((Element) rows.item(r)).getElementsByTagNameNS("*", "c");
                for (int c = 0; c < cells.getLength(); c++) {
                    Element cell = (Element) cells.item(c);
                    String reference = cell.getAttribute("r").replaceAll("[0-9]", "");
                    int index = 0;
                    for (char letter : reference.toCharArray()) index = index * 26 + letter - 'A' + 1;
                    if (index < 1 || index > 100) throw new IOException("invalid column");
                    while (values.size() < index) values.add("");
                    NodeList v = cell.getElementsByTagNameNS("*", "v");
                    String value = v.getLength() == 0 ? "" : v.item(0).getTextContent();
                    if ("s".equals(cell.getAttribute("t"))) value = strings.get(Integer.parseInt(value));
                    if ("inlineStr".equals(cell.getAttribute("t"))) {
                        NodeList inline = cell.getElementsByTagNameNS("*", "is");
                        value = inline.getLength() == 0 ? "" : inline.item(0).getTextContent();
                    }
                    values.set(index - 1, value);
                }
                result.add(values);
            }
            return result;
        } catch (Exception malformed) {
            EmploymentRateAchievementService.invalid("file", "안전한 XLSX 양식이 아닙니다. 수식·외부 참조는 허용되지 않습니다.");
            throw new IllegalStateException(malformed);
        }
    }

    private static Document parse(byte[] data) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(data));
    }

    /** Writes text cells (including formula-looking values) without creating executable formulas. */
    public static byte[] write(List<List<String>> rows) {
        String ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
        StringBuilder sheet = new StringBuilder("<worksheet xmlns=\"" + ns + "\"><sheetData>");
        int rowNumber = 0;
        for (List<String> row : rows) {
            sheet.append("<row r=\"").append(++rowNumber).append("\">");
            for (int i = 0; i < row.size(); i++) {
                sheet.append("<c r=\"").append(column(i + 1)).append(rowNumber)
                        .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                        .append(escape(row.get(i))).append("</t></is></c>");
            }
            sheet.append("</row>");
        }
        sheet.append("</sheetData></worksheet>");
        Map<String, String> parts = new LinkedHashMap<>();
        parts.put("[Content_Types].xml", """
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
                """);
        parts.put("_rels/.rels", """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """);
        parts.put("xl/workbook.xml", """
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="취업률" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
                """);
        parts.put("xl/_rels/workbook.xml.rels", """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
                """);
        parts.put("xl/worksheets/sheet1.xml", sheet.toString());
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                for (Map.Entry<String, String> part : parts.entrySet()) {
                    zip.putNextEntry(new ZipEntry(part.getKey()));
                    zip.write(part.getValue().getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
            return out.toByteArray();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static String column(int value) {
        StringBuilder result = new StringBuilder();
        while (value > 0) {
            result.insert(0, (char) ('A' + (value - 1) % 26));
            value = (value - 1) / 26;
        }
        return result.toString();
    }

    private static String escape(String value) {
        return (value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}

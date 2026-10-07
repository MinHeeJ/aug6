package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Bounded OOXML text codec. Dates must be ISO text, never untyped Excel serial dates. */
public final class EmploymentRateWorkbookCodec {
    public static final String MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final int MAX_INPUT_BYTES = 16 * 1024 * 1024;
    public static final int MAX_ROWS = 10001;
    private static final int MAX_OUTPUT_ROWS = 60002;
    private static final int MAX_COLUMNS = 32;
    private static final int MAX_TEXT = 32767;
    private static final int MAX_INFLATED = 64 * 1024 * 1024;
    private static final String MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PACKAGE_REL = "http://schemas.openxmlformats.org/package/2006/relationships";

    /** Returns positions including blank physical rows; rejects formulas, error cells and unsafe archives. */
    public List<List<String>> read(byte[] bytes) {
        if (bytes == null || bytes.length < 4 || bytes.length > MAX_INPUT_BYTES
                || bytes[0] != 'P' || bytes[1] != 'K') {
            throw invalid("실제 XLSX 파일(16MB 이하)이 필요합니다.");
        }
        try {
            Map<String, byte[]> parts = unzip(bytes);
            Document workbook = xml(required(parts, "xl/workbook.xml"));
            NodeList sheets = workbook.getElementsByTagNameNS(MAIN, "sheet");
            if (sheets.getLength() == 0) throw invalid("워크시트가 없습니다.");
            String id = ((Element) sheets.item(0)).getAttributeNS(REL, "id");
            Document relationships = xml(required(parts, "xl/_rels/workbook.xml.rels"));
            NodeList links = relationships.getElementsByTagNameNS(PACKAGE_REL, "Relationship");
            String sheetPath = null;
            Set<String> relationshipIds = new HashSet<>();
            for (int i = 0; i < links.getLength(); i++) {
                Element link = (Element) links.item(i);
                if (!relationshipIds.add(link.getAttribute("Id"))
                        || "External".equalsIgnoreCase(link.getAttribute("TargetMode"))) {
                    throw invalid("외부 또는 중복 관계는 허용하지 않습니다.");
                }
                if (id.equals(link.getAttribute("Id"))) {
                    if (!link.getAttribute("Type").equals(REL + "/worksheet")) {
                        throw invalid("워크시트 관계가 올바르지 않습니다.");
                    }
                    String target = link.getAttribute("Target");
                    if (target.contains("\\") || target.contains(":") || target.contains("..")) {
                        throw invalid("워크시트 경로가 올바르지 않습니다.");
                    }
                    sheetPath = target.startsWith("/") ? target.substring(1)
                            : Path.of("xl").resolve(target).normalize().toString().replace('\\', '/');
                    if (!sheetPath.startsWith("xl/")) throw invalid("워크시트 경로가 올바르지 않습니다.");
                }
            }
            if (sheetPath == null) throw invalid("워크시트 관계가 없습니다.");
            List<String> shared = new ArrayList<>();
            if (parts.containsKey("xl/sharedStrings.xml")) {
                NodeList strings = xml(parts.get("xl/sharedStrings.xml")).getElementsByTagNameNS(MAIN, "si");
                if (strings.getLength() > MAX_ROWS * MAX_COLUMNS) throw invalid("공유 문자열 제한을 초과했습니다.");
                for (int i = 0; i < strings.getLength(); i++) shared.add(text((Element) strings.item(i), "t"));
            }
            NodeList rows = xml(required(parts, sheetPath)).getElementsByTagNameNS(MAIN, "row");
            List<List<String>> result = new ArrayList<>();
            int previous = 0;
            for (int i = 0; i < rows.getLength(); i++) {
                Element row = (Element) rows.item(i);
                int number = row.hasAttribute("r") ? Integer.parseInt(row.getAttribute("r")) : previous + 1;
                if (number <= previous || number > MAX_ROWS) throw invalid("행 번호/행 제한이 올바르지 않습니다.");
                while (result.size() < number - 1) result.add(List.of());
                List<String> cells = new ArrayList<>();
                int lastColumn = -1;
                for (Node child = row.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (!(child instanceof Element cell) || !"c".equals(cell.getLocalName())) continue;
                    String ref = cell.getAttribute("r");
                    int column = lastColumn + 1;
                    if (!ref.isEmpty()) {
                        if (!ref.matches("[A-Z]+[1-9][0-9]*")) throw invalid("셀 주소가 올바르지 않습니다.");
                        int split = 0;
                        column = 0;
                        while (split < ref.length() && Character.isLetter(ref.charAt(split))) {
                            column = column * 26 + ref.charAt(split++) - 'A' + 1;
                            if (column > MAX_COLUMNS) throw invalid("열 제한을 초과했습니다.");
                        }
                        column--;
                        if (Integer.parseInt(ref.substring(split)) != number) throw invalid("행과 셀 주소가 다릅니다.");
                    }
                    if (column <= lastColumn || column >= MAX_COLUMNS) throw invalid("중복 셀 또는 열 제한 초과입니다.");
                    while (cells.size() < column) cells.add("");
                    if (cell.getElementsByTagNameNS(MAIN, "f").getLength() > 0
                            || "e".equals(cell.getAttribute("t"))) throw invalid("수식/오류 셀은 허용하지 않습니다.");
                    String type = cell.getAttribute("t");
                    String value = "inlineStr".equals(type) ? text(cell, "t") : text(cell, "v");
                    if ("s".equals(type)) value = shared.get(Integer.parseInt(value));
                    else if (!type.isEmpty() && !List.of("inlineStr", "str", "n", "d", "b").contains(type)) {
                        throw invalid("지원하지 않는 셀 형식입니다.");
                    }
                    checkText(value);
                    cells.add(value);
                    lastColumn = column;
                }
                result.add(List.copyOf(cells));
                previous = number;
            }
            return List.copyOf(result);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("XLSX 구조 또는 XML 형식을 확인하세요.", exception);
        }
    }

    /** Produces a real XLSX using inline text cells; leading zeros and formula-like text remain literal. */
    public byte[] write(List<List<String>> rows) {
        if (rows == null || rows.size() > MAX_OUTPUT_ROWS) throw invalid("출력 행 제한을 초과했습니다.");
        StringBuilder sheet = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"" + MAIN + "\"><sheetData>");
        for (int i = 0; i < rows.size(); i++) {
            List<String> cells = rows.get(i);
            if (cells.size() > MAX_COLUMNS) throw invalid("출력 열 제한을 초과했습니다.");
            sheet.append("<row r=\"").append(i + 1).append("\">");
            for (int j = 0; j < cells.size(); j++) {
                String value = cells.get(j) == null ? "" : cells.get(j);
                checkText(value);
                sheet.append("<c r=\"").append(columnName(j)).append(i + 1)
                        .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                        .append(escape(value)).append("</t></is></c>");
            }
            sheet.append("</row>");
            if (sheet.length() > MAX_INFLATED) throw invalid("출력 크기 제한을 초과했습니다.");
        }
        sheet.append("</sheetData></worksheet>");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                part(zip, "[Content_Types].xml", "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
                part(zip, "_rels/.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"" + PACKAGE_REL + "\"><Relationship Id=\"rId1\" Type=\"" + REL + "/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
                part(zip, "xl/workbook.xml", "<?xml version=\"1.0\"?><workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + REL + "\"><sheets><sheet name=\"취업률실적\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
                part(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"" + PACKAGE_REL + "\"><Relationship Id=\"rId1\" Type=\"" + REL + "/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
                part(zip, "xl/worksheets/sheet1.xml", sheet.toString());
            }
            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalArgumentException("XLSX 출력 실패", exception);
        }
    }

    private Map<String, byte[]> unzip(byte[] bytes) throws Exception {
        Map<String, byte[]> parts = new HashMap<>();
        int total = 0;
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (++count > 128 || name.startsWith("/") || name.contains("..") || name.contains("\\")
                        || parts.containsKey(name)) throw invalid("안전하지 않은 ZIP 항목입니다.");
                ByteArrayOutputStream part = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int length;
                while ((length = zip.read(buffer)) != -1) {
                    total += length;
                    if (total > MAX_INFLATED) throw invalid("압축해제 크기 제한을 초과했습니다.");
                    part.write(buffer, 0, length);
                }
                parts.put(name, part.toByteArray());
            }
        }
        return parts;
    }

    private Document xml(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    private byte[] required(Map<String, byte[]> parts, String name) {
        byte[] part = parts.get(name);
        if (part == null) throw invalid("XLSX 필수 항목 누락: " + name);
        return part;
    }

    private String text(Element parent, String tag) {
        StringBuilder text = new StringBuilder();
        NodeList elements = parent.getElementsByTagNameNS(MAIN, tag);
        for (int i = 0; i < elements.getLength(); i++) {
            // Phonetic annotations are not part of the original cell text.
            if (elements.item(i).getParentNode() instanceof Element p && "rPh".equals(p.getLocalName())) continue;
            text.append(elements.item(i).getTextContent());
            if (text.length() > MAX_TEXT) throw invalid("셀 문자열 제한 초과입니다.");
        }
        return text.toString();
    }

    private void checkText(String text) {
        if (text.length() > MAX_TEXT) throw invalid("셀 문자열 제한 초과입니다.");
        for (int i = 0; i < text.length();) {
            int c = text.codePointAt(i);
            if (!(c == 9 || c == 10 || c == 13 || c >= 0x20 && c <= 0xD7FF
                    || c >= 0xE000 && c <= 0xFFFD || c >= 0x10000 && c <= 0x10FFFF)) {
                throw invalid("XML에서 허용하지 않는 문자가 있습니다.");
            }
            i += Character.charCount(c);
        }
    }

    private String columnName(int column) {
        return column < 26 ? String.valueOf((char) ('A' + column)) : "A" + (char) ('A' + column - 26);
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\r", "&#13;");
    }

    private void part(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}

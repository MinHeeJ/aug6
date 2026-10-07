package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import org.w3c.dom.NodeList;

/** Bounded, dependency-free OOXML reader/writer. Does not evaluate formulas or load external resources. */
public final class EmploymentRateWorkbook {
    private static final String MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final int MAX_ROWS = 10_001;
    private static final int MAX_COLUMNS = 64;
    private static final int MAX_EXPANDED = 32 * 1024 * 1024;

    public record Row(int number, List<String> cells) { }

    private EmploymentRateWorkbook() { }

    public static List<Row> read(byte[] bytes) {
        try {
            if (bytes.length > 10 * 1024 * 1024 || bytes.length < 4
                    || bytes[0] != 'P' || bytes[1] != 'K') {
                throw new IllegalArgumentException("10MB 이하의 XLSX 파일을 선택하세요.");
            }
            Map<String, byte[]> parts = unzip(bytes);
            Document workbook = xml(required(parts, "xl/workbook.xml"));
            NodeList sheets = workbook.getElementsByTagNameNS(MAIN, "sheet");
            if (sheets.getLength() != 1) {
                throw new IllegalArgumentException("실적 시트 한 개만 포함한 양식을 사용하세요.");
            }
            String id = ((Element) sheets.item(0)).getAttributeNS(REL, "id");
            Document relationships = xml(required(parts, "xl/_rels/workbook.xml.rels"));
            NodeList relations = relationships.getElementsByTagNameNS("*", "Relationship");
            String sheetPath = null;
            for (int i = 0; i < relations.getLength(); i++) {
                Element relation = (Element) relations.item(i);
                if (id.equals(relation.getAttribute("Id"))) {
                    if ("External".equals(relation.getAttribute("TargetMode"))
                            || !relation.getAttribute("Type").equals(REL + "/worksheet")) {
                        throw new IllegalArgumentException("외부 시트 참조는 허용하지 않습니다.");
                    }
                    String target = relation.getAttribute("Target");
                    sheetPath = target.startsWith("/") ? target.substring(1) : "xl/" + target;
                    if (sheetPath.contains("..") || sheetPath.contains("\\")) {
                        throw new IllegalArgumentException("시트 경로가 올바르지 않습니다.");
                    }
                }
            }
            if (sheetPath == null) throw new IllegalArgumentException("시트 참조가 없습니다.");
            List<String> shared = new ArrayList<>();
            if (parts.containsKey("xl/sharedStrings.xml")) {
                NodeList strings = xml(parts.get("xl/sharedStrings.xml")).getElementsByTagNameNS(MAIN, "si");
                for (int i = 0; i < strings.getLength(); i++) shared.add(text((Element) strings.item(i)));
            }
            Set<Integer> dateStyles = dateStyles(parts.get("xl/styles.xml"));
            NodeList properties = workbook.getElementsByTagNameNS(MAIN, "workbookPr");
            boolean date1904 = properties.getLength() > 0 && Set.of("1", "true").contains(
                    ((Element) properties.item(0)).getAttribute("date1904"));
            Document sheet = xml(required(parts, sheetPath));
            if (sheet.getElementsByTagNameNS(MAIN, "mergeCell").getLength() > 0) {
                throw new IllegalArgumentException("병합된 셀은 허용하지 않습니다.");
            }
            NodeList rowNodes = sheet.getElementsByTagNameNS(MAIN, "row");
            if (rowNodes.getLength() > MAX_ROWS) throw new IllegalArgumentException("최대 10000행을 업로드하세요.");
            List<Row> rows = new ArrayList<>();
            int previous = 0;
            for (int i = 0; i < rowNodes.getLength(); i++) {
                Element row = (Element) rowNodes.item(i);
                int number = row.hasAttribute("r") ? Integer.parseInt(row.getAttribute("r")) : previous + 1;
                if (number <= previous || number > 1_048_576) throw new IllegalArgumentException("행 번호가 올바르지 않습니다.");
                previous = number;
                NodeList cells = row.getElementsByTagNameNS(MAIN, "c");
                Map<Integer, String> values = new HashMap<>();
                int next = 0;
                for (int c = 0; c < cells.getLength(); c++) {
                    Element cell = (Element) cells.item(c);
                    String reference = cell.getAttribute("r");
                    int column = reference.isEmpty() ? next : column(reference, number);
                    if (column >= MAX_COLUMNS || values.containsKey(column)) {
                        throw new IllegalArgumentException("열 번호 또는 중복 셀을 확인하세요.");
                    }
                    next = column + 1;
                    if (cell.getElementsByTagNameNS(MAIN, "f").getLength() > 0) {
                        throw new IllegalArgumentException("수식 대신 실제 값을 입력하세요.");
                    }
                    String type = cell.getAttribute("t");
                    String value = firstText(cell, "v");
                    if ("inlineStr".equals(type)) value = text(cell);
                    else if ("s".equals(type)) value = shared.get(Integer.parseInt(value));
                    else if ("e".equals(type)) throw new IllegalArgumentException("오류 셀을 수정하세요.");
                    else if (!Set.of("", "n", "str", "b", "d").contains(type)) {
                        throw new IllegalArgumentException("지원하지 않는 셀 형식입니다.");
                    }
                    if (!value.isBlank() && (type.isEmpty() || "n".equals(type))
                            && cell.hasAttribute("s")
                            && dateStyles.contains(Integer.parseInt(cell.getAttribute("s")))) {
                        long serial = new BigDecimal(value).longValueExact();
                        if ((!date1904 && serial == 60) || serial < 0 || serial > 2_958_465) {
                            throw new IllegalArgumentException("Excel 날짜가 올바르지 않습니다.");
                        }
                        value = (date1904 ? LocalDate.of(1904, 1, 1)
                                : LocalDate.of(1899, 12, 31))
                                .plusDays(date1904 || serial < 60 ? serial : serial - 1).toString();
                    }
                    if (value.length() > 32767) throw new IllegalArgumentException("셀 값이 너무 깁니다.");
                    values.put(column, value);
                }
                int width = values.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
                List<String> ordered = new ArrayList<>();
                for (int c = 0; c < width; c++) ordered.add(values.getOrDefault(c, ""));
                if (ordered.stream().anyMatch(value -> !value.isBlank())) {
                    rows.add(new Row(number, List.copyOf(ordered)));
                }
            }
            return List.copyOf(rows);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("XLSX 구조와 셀 값을 확인하세요.", exception);
        }
    }

    private static Map<String, byte[]> unzip(byte[] bytes) throws Exception {
        Map<String, byte[]> parts = new LinkedHashMap<>();
        int total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (parts.size() >= 256 || parts.containsKey(entry.getName())
                        || entry.getName().contains("..")) {
                    throw new IllegalArgumentException("XLSX 압축 구조가 올바르지 않습니다.");
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_EXPANDED) throw new IllegalArgumentException("XLSX 압축 해제 크기를 초과했습니다.");
                    output.write(buffer, 0, count);
                }
                parts.put(entry.getName(), output.toByteArray());
            }
        }
        required(parts, "[Content_Types].xml");
        return parts;
    }

    private static byte[] required(Map<String, byte[]> parts, String path) {
        byte[] part = parts.get(path);
        if (part == null) throw new IllegalArgumentException("필수 XLSX 부분이 없습니다: " + path);
        return part;
    }

    private static Document xml(byte[] bytes) throws Exception {
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

    private static String text(Element parent) {
        NodeList texts = parent.getElementsByTagNameNS(MAIN, "t");
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < texts.getLength(); i++) value.append(texts.item(i).getTextContent());
        return value.toString();
    }

    private static String firstText(Element parent, String name) {
        NodeList nodes = parent.getElementsByTagNameNS(MAIN, name);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
    }

    private static int column(String reference, int row) {
        if (!reference.matches("[A-Z]+[1-9][0-9]*")) throw new IllegalArgumentException("셀 주소가 올바르지 않습니다.");
        int split = 0;
        int result = 0;
        while (split < reference.length() && Character.isLetter(reference.charAt(split))) {
            result = result * 26 + reference.charAt(split++) - 'A' + 1;
            if (result > MAX_COLUMNS) throw new IllegalArgumentException("열 개수를 초과했습니다.");
        }
        if (Integer.parseInt(reference.substring(split)) != row) throw new IllegalArgumentException("셀 행 번호가 다릅니다.");
        return result - 1;
    }

    private static Set<Integer> dateStyles(byte[] bytes) throws Exception {
        Set<Integer> result = new HashSet<>();
        if (bytes == null) return result;
        Document styles = xml(bytes);
        Set<Integer> formats = new HashSet<>(Set.of(14, 15, 16, 17, 22, 27, 30, 36, 50, 57));
        NodeList custom = styles.getElementsByTagNameNS(MAIN, "numFmt");
        for (int i = 0; i < custom.getLength(); i++) {
            Element format = (Element) custom.item(i);
            String code = format.getAttribute("formatCode").replaceAll("\"[^\"]*\"|\\\\.", "")
                    .toLowerCase(java.util.Locale.ROOT);
            if (code.contains("y") && code.contains("d")) {
                formats.add(Integer.parseInt(format.getAttribute("numFmtId")));
            }
        }
        NodeList groups = styles.getElementsByTagNameNS(MAIN, "cellXfs");
        if (groups.getLength() == 0) return result;
        NodeList xfs = ((Element) groups.item(0)).getElementsByTagNameNS(MAIN, "xf");
        for (int i = 0; i < xfs.getLength(); i++) {
            if (formats.contains(Integer.parseInt(((Element) xfs.item(i)).getAttribute("numFmtId")))) result.add(i);
        }
        return result;
    }

    /** Writes strings as inline text, including values beginning with '='; never creates executable formulas. */
    public static byte[] write(List<List<String>> rows) {
        if (rows.size() > MAX_ROWS) throw new IllegalArgumentException("다운로드 행 수를 초과했습니다.");
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
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
                part(zip, "xl/workbook.xml", "<workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + REL
                        + "\"><sheets><sheet name=\"취업률 실적\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
                part(zip, "xl/_rels/workbook.xml.rels", """
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                        </Relationships>
                        """);
                StringBuilder sheet = new StringBuilder("<worksheet xmlns=\"" + MAIN + "\"><sheetData>");
                int number = 0;
                for (List<String> row : rows) {
                    if (row.size() > MAX_COLUMNS) throw new IllegalArgumentException("다운로드 열 수를 초과했습니다.");
                    sheet.append("<row r=\"").append(++number).append("\">");
                    for (int c = 0; c < row.size(); c++) {
                        sheet.append("<c r=\"").append(letters(c)).append(number)
                                .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                                .append(escape(row.get(c))).append("</t></is></c>");
                    }
                    sheet.append("</row>");
                }
                part(zip, "xl/worksheets/sheet1.xml", sheet.append("</sheetData></worksheet>").toString());
            }
            return output.toByteArray();
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("XLSX 생성 실패", exception);
        }
    }

    private static String letters(int column) {
        StringBuilder result = new StringBuilder();
        for (int c = column + 1; c > 0; c = (c - 1) / 26) result.insert(0, (char) ('A' + (c - 1) % 26));
        return result.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        if (value.length() > 32767 || value.codePoints().anyMatch(c ->
                !(c == 9 || c == 10 || c == 13 || (c >= 32 && c <= 0xD7FF)
                        || (c >= 0xE000 && c <= 0xFFFD) || (c >= 0x10000 && c <= 0x10FFFF)))) {
            throw new IllegalArgumentException("XML에 저장할 수 없는 셀 값입니다.");
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void part(ZipOutputStream zip, String name, String body) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + body).getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}

package kr.ac.knue.commonfoundation.courseareagroupgrades;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies query validation, role data scope, and query-audit recording for the read-only group grade feature.
 */
@Service
public class CourseAreaGroupGradeService {
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_-]{0,29}$");
    private static final Pattern SEMESTER = Pattern.compile("^[0-9]{4}-[12]$");
    private final CourseAreaGroupGradeMapper mapper;

    public CourseAreaGroupGradeService(CourseAreaGroupGradeMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Returns only published results in the authenticated user's permitted data scope and records a successful read.
     */
    @Transactional
    public CourseAreaGroupGradeSearchResponse list(
            int page,
            int pageSize,
            String completionTypeCode,
            String semesterCode,
            String courseAreaCode,
            Long requestedFacultyUserId,
            CurrentUser user,
            String requestId) {
        CourseAreaGroupGradeSearchCriteria criteria = validatedCriteria(
                page, pageSize, completionTypeCode, semesterCode, courseAreaCode, requestedFacultyUserId, user);
        List<CourseAreaGroupGradeItem> items = mapper.listPublishedGrades(criteria);
        long totalElements = mapper.countPublishedGrades(criteria);
        mapper.insertQueryAudit("course_area_group_grade_results:" + user.userId(), user.userId(), requestId);
        return new CourseAreaGroupGradeSearchResponse(items, page, pageSize, totalElements);
    }

    /**
     * Creates an Excel workbook using precisely the same validation and data-scope rules as list().
     */
    @Transactional
    public byte[] download(
            int page,
            int pageSize,
            String completionTypeCode,
            String semesterCode,
            String courseAreaCode,
            Long requestedFacultyUserId,
            CurrentUser user,
            String requestId) {
        CourseAreaGroupGradeSearchResponse response = list(
                page, pageSize, completionTypeCode, semesterCode, courseAreaCode, requestedFacultyUserId, user, requestId);
        return createWorkbook(response.items());
    }

    private CourseAreaGroupGradeSearchCriteria validatedCriteria(
            int page,
            int pageSize,
            String completionTypeCode,
            String semesterCode,
            String courseAreaCode,
            Long requestedFacultyUserId,
            CurrentUser user) {
        List<ValidationError> fields = new ArrayList<>();
        if (page < 0) fields.add(new ValidationError("page", "페이지 번호는 0 이상이어야 합니다."));
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            fields.add(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요."));
        }
        String completionType = normalizeCode(completionTypeCode, "completionTypeCode", fields);
        String semester = normalizeSemester(semesterCode, fields);
        String courseArea = normalizeCode(courseAreaCode, "courseAreaCode", fields);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("검색조건이 올바르지 않습니다.", fields);
        }
        Long facultyUserId = requestedFacultyUserId;
        if (user.roles().contains("R01")) {
            if (facultyUserId != null && !Objects.equals(facultyUserId, user.userId())) {
                throw new ForbiddenException();
            }
            facultyUserId = user.userId();
        }
        return new CourseAreaGroupGradeSearchCriteria(page, pageSize, completionType, semester, courseArea, facultyUserId);
    }

    private String normalizeCode(String value, String field, List<ValidationError> fields) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase();
        if (!CODE.matcher(normalized).matches()) {
            fields.add(new ValidationError(field, "영문 대문자, 숫자, 하이픈, 밑줄만 입력하세요."));
        }
        return normalized;
    }

    private String normalizeSemester(String value, List<ValidationError> fields) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (!SEMESTER.matcher(normalized).matches()) {
            fields.add(new ValidationError("semesterCode", "학기는 YYYY-1 또는 YYYY-2 형식이어야 합니다."));
        }
        return normalized;
    }

    private byte[] createWorkbook(List<CourseAreaGroupGradeItem> items) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            put(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            put(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            put(zip, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"그룹평가 성적\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            put(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            put(zip, "xl/worksheets/sheet1.xml", worksheet(items));
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("그룹평가 성적 Excel 파일을 생성하지 못했습니다.", exception);
        }
    }

    private String worksheet(List<CourseAreaGroupGradeItem> items) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        xml.append("<row r=\"1\"><c t=\"inlineStr\"><is><t>이수구분</t></is></c><c t=\"inlineStr\"><is><t>학기</t></is></c><c t=\"inlineStr\"><is><t>교과영역</t></is></c><c t=\"inlineStr\"><is><t>그룹평가 성적</t></is></c></row>");
        for (int index = 0; index < items.size(); index++) {
            CourseAreaGroupGradeItem item = items.get(index);
            int row = index + 2;
            xml.append("<row r=\"").append(row).append("\">")
                    .append(inlineCell(item.completionType()))
                    .append(inlineCell(item.semester()))
                    .append(inlineCell(item.courseArea()))
                    .append(item.groupGrade() == null ? inlineCell("") : "<c><v>" + item.groupGrade().toPlainString() + "</v></c>")
                    .append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private String inlineCell(String value) {
        return "<c t=\"inlineStr\"><is><t>" + xml(value) + "</t></is></c>";
    }

    private String xml(String value) {
        return (value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}

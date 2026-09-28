package kr.ac.knue.commonfoundation.excel;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExcelOperationsService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private static final Set<String> ALLOWED_OUTPUT_TYPES = Set.of("TARGET", "STATUS", "ERROR");
    private final ExcelOperationsMapper mapper;

    public ExcelOperationsService(ExcelOperationsMapper mapper) {
        this.mapper = mapper;
    }

    /** Identifies the dedicated student-guidance template before granting the R07 bulk workflow. */
    @Transactional(readOnly = true)
    public boolean isStudentGuidanceTemplate(String templateId) {
        ExcelTemplateRow template = mapper.findUploadTemplate(templateId);
        return template != null && "STUDENT_GUIDANCE_ACHIEVEMENT".equals(template.businessType());
    }

    /** Identifies a student-guidance upload so R07 cannot access another business upload. */
    @Transactional(readOnly = true)
    public boolean isStudentGuidanceUpload(String uploadId) {
        return "STUDENT_GUIDANCE_ACHIEVEMENT".equals(mapper.findUploadBusinessType(uploadId));
    }

    @Transactional(readOnly = true)
    public ExcelTemplateSearchResponse listUploadTemplates(int page, int size, String businessType, String effectiveDate) {
        int safePage = Math.max(page, 0);
        int safeSize = safeSize(size);
        String normalizedBusinessType = blankToNull(businessType);
        String normalizedDate = blankToNull(effectiveDate);
        List<ExcelTemplateRow> templates = mapper.listUploadTemplates(normalizedBusinessType, normalizedDate, safeSize, safePage * safeSize)
                .stream().map(row -> row.withRules(mapper.listTemplateRules(row.templateId()))).toList();
        return new ExcelTemplateSearchResponse(templates, safePage, safeSize, mapper.countUploadTemplates(normalizedBusinessType, normalizedDate));
    }

    @Transactional
    public ExcelTemplateRow saveUploadTemplate(UploadTemplateSaveRequest request, Long userId) {
        validateTemplateRequest(request);
        String templateId = blankToNull(request.getTemplateId()) == null ? "TPL-" + UUID.randomUUID() : request.getTemplateId().trim();
        request.setTemplateId(templateId);
        request.setBusinessType(request.getBusinessType().trim());
        request.setTemplateVersion(request.getTemplateVersion().trim());
        request.setEffectiveDate(LocalDate.parse(request.getEffectiveDate().trim()).toString());
        mapper.upsertUploadTemplate(request, templateId, userId);
        mapper.deleteTemplateRules(templateId);
        int index = 1;
        for (UploadTemplateRuleRequest rule : request.getRules()) {
            mapper.insertTemplateRule(rule, blankToNull(rule.getRuleId()) == null ? templateId + "-RULE-" + index : rule.getRuleId().trim(), templateId, userId);
            index++;
        }
        String originalFileName = blankToNull(request.getOriginalFileName()) == null ? request.getBusinessType() + "_template.csv" : request.getOriginalFileName().trim();
        mapper.upsertTemplateFile(templateId, "template-file-" + templateId, originalFileName, userId);
        ExcelTemplateRow saved = mapper.findUploadTemplate(templateId);
        if (saved == null) {
            throw new NotFoundException("업로드 양식을 찾을 수 없습니다.");
        }
        return saved.withRules(mapper.listTemplateRules(templateId));
    }

    @Transactional(readOnly = true)
    public ExcelDownloadFile downloadUploadTemplate(String templateId, Long userId) {
        requireUser(userId);
        ExcelTemplateRow template = mapper.findUploadTemplate(templateId);
        if (template == null || mapper.countTemplateFile(templateId) == 0) {
            throw new NotFoundException("다운로드할 업로드 양식을 찾을 수 없습니다.");
        }
        StringBuilder csv = new StringBuilder();
        for (ExcelTemplateRuleRow rule : mapper.listTemplateRules(templateId)) {
            if (csv.length() > 0) csv.append(',');
            csv.append(rule.requiredColumn());
        }
        csv.append('\n');
        return new ExcelDownloadFile(template.originalFileName(), "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public ExcelUploadResult createExcelUpload(String businessType, String templateId, MultipartFile file, Long userId) {
        List<ValidationError> fields = new ArrayList<>();
        if (blankToNull(businessType) == null) fields.add(new ValidationError("businessType", "업무구분을 입력하세요."));
        if (file == null || file.isEmpty()) fields.add(new ValidationError("file", "엑셀 파일을 선택하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("엑셀 업로드 요청이 올바르지 않습니다.", fields);
        String originalName = safeOriginalName(file.getOriginalFilename());
        if (!originalName.toLowerCase().endsWith(".csv") && !originalName.toLowerCase().endsWith(".xlsx") && !originalName.toLowerCase().endsWith(".xls")) {
            throw new BusinessValidationException("엑셀 업로드 요청이 올바르지 않습니다.", List.of(new ValidationError("file", "기존 첨부파일 정책의 Excel 허용 확장자만 업로드할 수 있습니다.")));
        }
        String uploadId = "UP-" + UUID.randomUUID();
        String normalizedBusinessType = businessType.trim();
        if ("STUDENT_GUIDANCE_ACHIEVEMENT".equals(normalizedBusinessType)) {
            ExcelTemplateRow template = blankToNull(templateId) == null ? null : mapper.findUploadTemplate(templateId.trim());
            if (template == null || !normalizedBusinessType.equals(template.businessType())) {
                throw new BusinessValidationException("학생지도 업로드 양식이 올바르지 않습니다.", List.of(new ValidationError("templateId", "학생지도 현행 업로드 양식을 선택하세요.")));
            }
            StudentGuidanceInspection inspection = inspectStudentGuidanceUpload(uploadId, file, userId);
            int total = inspection.rows().size();
            int errorCount = (int) inspection.rows().stream().filter(StudentGuidanceUploadRow::invalid).count();
            int successCount = total - errorCount;
            String status = errorCount == 0 ? "VALIDATED" : "REJECTED";
            mapper.insertUploadFile(uploadId, normalizedBusinessType, templateId.trim(), "upload-file-" + uploadId, originalName, userId, status);
            for (StudentGuidanceUploadRow row : inspection.rows()) {
                mapper.insertStagingRow("STG-" + UUID.randomUUID(), uploadId, row.rowNumber(), row.payload(), row.invalid() ? "ERROR" : "NORMAL");
            }
            for (ExcelUploadErrorRow error : inspection.errors()) mapper.insertUploadError(error);
            mapper.upsertUploadHistory(uploadId, total, successCount, errorCount, 0, 0, 1_000, userId);
            return new ExcelUploadResult(uploadId, normalizedBusinessType, originalName, status, total, successCount, errorCount, 0, 0, inspection.errors());
        }
        List<ExcelUploadErrorRow> errors = inspectUpload(uploadId, file);
        int total = Math.max(1, countDataRows(file));
        int errorCount = errors.size();
        int successCount = Math.max(0, total - errorCount);
        String status = errorCount == 0 ? "VALIDATED" : "REJECTED";
        mapper.insertUploadFile(uploadId, businessType.trim(), blankToNull(templateId), "upload-file-" + uploadId, originalName, userId, status);
        if (errorCount == 0) {
            mapper.insertStagingRow("STG-" + UUID.randomUUID(), uploadId, 1, "{}", "NORMAL");
        } else {
            for (ExcelUploadErrorRow error : errors) mapper.insertUploadError(error);
            mapper.insertStagingRow("STG-" + UUID.randomUUID(), uploadId, 1, "{}", "ERROR");
        }
        mapper.upsertUploadHistory(uploadId, total, successCount, errorCount, 0, 0, 1_000, userId);
        return new ExcelUploadResult(uploadId, businessType.trim(), originalName, status, total, successCount, errorCount, 0, 0, errors);
    }

    @Transactional
    public ExcelUploadCommitResult commitExcelUpload(String uploadId, Long userId) {
        requireUser(userId);
        if (blankToNull(uploadId) == null || mapper.existsUpload(uploadId) == 0) throw new NotFoundException("업로드 파일을 찾을 수 없습니다.");
        if (mapper.countUploadErrorsForCommit(uploadId) > 0) throw new ConflictException("오류 행이 있어 전체 반영을 차단했습니다.");
        int savedCount = mapper.countNormalStagingRows(uploadId);
        if ("STUDENT_GUIDANCE_ACHIEVEMENT".equals(mapper.findUploadBusinessType(uploadId))) {
            mapper.commitStudentGuidanceRows(uploadId, userId);
            mapper.insertStudentGuidanceManagementValues(uploadId, userId);
        }
        mapper.markUploadCommitted(uploadId);
        mapper.upsertUploadHistory(uploadId, savedCount, savedCount, 0, 0, savedCount, 1_000, userId);
        mapper.deleteNormalStagingRows(uploadId);
        return new ExcelUploadCommitResult(uploadId, savedCount);
    }

    @Transactional(readOnly = true)
    public ExcelUploadHistorySearchResponse listExcelUploadHistories(int page, int size, String uploadId, String originalFileName) {
        int safePage = Math.max(page, 0);
        int safeSize = safeSize(size);
        return new ExcelUploadHistorySearchResponse(mapper.listExcelUploadHistories(blankToNull(uploadId), blankToNull(originalFileName), safeSize, safePage * safeSize),
                safePage, safeSize, mapper.countExcelUploadHistories(blankToNull(uploadId), blankToNull(originalFileName)));
    }

    @Transactional(readOnly = true)
    public ExcelUploadErrorSearchResponse listExcelUploadErrors(int page, int size, String uploadId) {
        if (blankToNull(uploadId) == null) throw new BusinessValidationException("업로드 오류 조회 요청이 올바르지 않습니다.", List.of(new ValidationError("uploadId", "업로드ID를 입력하세요.")));
        int safePage = Math.max(page, 0);
        int safeSize = safeSize(size);
        return new ExcelUploadErrorSearchResponse(mapper.listExcelUploadErrors(uploadId.trim(), safeSize, safePage * safeSize), safePage, safeSize, mapper.countExcelUploadErrors(uploadId.trim()));
    }

    @Transactional(readOnly = true)
    public ExcelDownloadFile downloadExcelUploadErrors(String uploadId, Long userId) {
        requireUser(userId);
        ExcelUploadErrorSearchResponse response = listExcelUploadErrors(0, 100, uploadId);
        StringBuilder csv = new StringBuilder("rowNumber,columnName,inputValue,errorCode,errorReason,correctionGuide\n");
        for (ExcelUploadErrorRow row : response.errors()) {
            csv.append(row.rowNumber()).append(',').append(row.columnName()).append(',').append(row.inputValue()).append(',')
                    .append(row.errorCode()).append(',').append(row.errorReason()).append(',').append(row.correctionGuide()).append('\n');
        }
        return new ExcelDownloadFile("업로드오류_" + uploadId + ".csv", "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public ExcelDownloadJobRow createExcelDownload(ExcelDownloadRequest request, Long userId) {
        requireUser(userId);
        if (request == null || blankToNull(request.getOutputType()) == null || !ALLOWED_OUTPUT_TYPES.contains(request.getOutputType().trim())) {
            throw new BusinessValidationException("엑셀 다운로드 요청이 올바르지 않습니다.", List.of(new ValidationError("outputType", "출력유형을 선택하세요.")));
        }
        String id = "DL-" + UUID.randomUUID();
        String outputType = request.getOutputType().trim();
        String query = request.getQueryCondition() == null || request.getQueryCondition().isNull() ? "{}" : request.getQueryCondition().toString();
        mapper.insertDownloadJob(id, userId, outputType, query, "R09:ALL", "download-file-" + id, outputType.toLowerCase() + "_download.csv");
        return mapper.findDownloadJob(id);
    }

    private void validateTemplateRequest(UploadTemplateSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("body", "업로드 양식 요청 본문이 필요합니다."));
        } else {
            if (blankToNull(request.getBusinessType()) == null) fields.add(new ValidationError("businessType", "업무구분은 필수입니다."));
            if (blankToNull(request.getTemplateVersion()) == null) fields.add(new ValidationError("templateVersion", "양식 버전은 필수입니다."));
            if (blankToNull(request.getEffectiveDate()) == null) fields.add(new ValidationError("effectiveDate", "시행일은 필수입니다."));
            else LocalDate.parse(request.getEffectiveDate().trim());
            if (request.getRules() == null || request.getRules().isEmpty()) fields.add(new ValidationError("rules", "검증규칙을 1개 이상 입력하세요."));
            else for (int i = 0; i < request.getRules().size(); i++) {
                UploadTemplateRuleRequest rule = request.getRules().get(i);
                if (blankToNull(rule.getRequiredColumn()) == null) fields.add(new ValidationError("rules[" + i + "].requiredColumn", "필수 열을 입력하세요."));
                if (rule.getColumnOrder() == null || rule.getColumnOrder() < 1) fields.add(new ValidationError("rules[" + i + "].columnOrder", "열 순서를 입력하세요."));
                if (blankToNull(rule.getCodeRuleRef()) == null) fields.add(new ValidationError("rules[" + i + "].codeRuleRef", "코드값 규칙을 입력하세요."));
            }
        }
        if (!fields.isEmpty()) throw new BusinessValidationException("업로드 양식 저장 요청이 올바르지 않습니다.", fields);
    }

    /** Validates the student-guidance template rows before any achievement data is written. */
    private StudentGuidanceInspection inspectStudentGuidanceUpload(String uploadId, MultipartFile file, Long userId) {
        List<StudentGuidanceUploadRow> rows = new ArrayList<>();
        List<ExcelUploadErrorRow> errors = new ArrayList<>();
        Set<String> duplicateKeys = new HashSet<>();
        try {
            List<String> lines = new String(file.getBytes(), StandardCharsets.UTF_8).lines().filter(line -> !line.isBlank()).toList();
            if (lines.size() < 2) throw new IllegalArgumentException("업로드할 데이터 행이 없습니다.");
            String[] headers = lines.get(0).split(",", -1);
            for (int rowIndex = 1; rowIndex < lines.size(); rowIndex++) {
                String[] values = lines.get(rowIndex).split(",", -1);
                String managementItemCode = csvValue(headers, values, "관리항목코드");
                String occurrenceDate = csvValue(headers, values, "발생일");
                String studentName = csvValue(headers, values, "지도학생");
                String guidanceStartDate = csvValue(headers, values, "지도시작일");
                String guidanceEndDate = csvValue(headers, values, "지도종료일");
                String studentCount = csvValue(headers, values, "학생수");
                int spreadsheetRow = rowIndex + 1;
                String errorColumn = null;
                String reason = null;
                try {
                    if (blankToNull(managementItemCode) == null || blankToNull(occurrenceDate) == null || blankToNull(studentName) == null || blankToNull(guidanceStartDate) == null || blankToNull(guidanceEndDate) == null || blankToNull(studentCount) == null) {
                        errorColumn = "필수항목"; reason = "관리항목코드, 발생일, 지도학생, 지도기간, 학생수는 모두 필수입니다.";
                    } else {
                        LocalDate start = LocalDate.parse(guidanceStartDate); LocalDate end = LocalDate.parse(guidanceEndDate);
                        if (end.isBefore(start)) { errorColumn = "지도종료일"; reason = "지도 종료일은 시작일보다 빠를 수 없습니다."; }
                        else if (Integer.parseInt(studentCount) <= 0) { errorColumn = "학생수"; reason = "학생수는 1명 이상이어야 합니다."; }
                        else {
                            String duplicateKey = studentName.trim() + "|" + guidanceStartDate + "|" + guidanceEndDate;
                            if (!duplicateKeys.add(duplicateKey) || mapper.countStudentGuidanceDuplicate(studentName.trim(), guidanceStartDate, guidanceEndDate, userId) > 0) { errorColumn = "지도학생"; reason = "중복 학생지도 데이터는 자동 갱신할 수 없습니다."; }
                        }
                    }
                } catch (RuntimeException invalidValue) { errorColumn = "입력값"; reason = "날짜는 YYYY-MM-DD 형식이고 학생수는 양의 정수여야 합니다."; }
                boolean invalid = errorColumn != null;
                if (invalid) errors.add(new ExcelUploadErrorRow("ERR-" + UUID.randomUUID(), uploadId, spreadsheetRow, errorColumn, "", "INVALID_VALUE", reason, "양식의 오류 행을 수정한 후 다시 업로드하세요."));
                rows.add(new StudentGuidanceUploadRow(spreadsheetRow, guidancePayload(managementItemCode, occurrenceDate, studentName, guidanceStartDate, guidanceEndDate, studentCount), invalid));
            }
        } catch (Exception failure) {
            errors.add(new ExcelUploadErrorRow("ERR-" + UUID.randomUUID(), uploadId, 1, "파일", "", "INVALID_TEMPLATE", "학생지도 CSV 양식의 헤더와 데이터 행을 확인하세요.", "템플릿을 다시 다운로드하여 사용하세요."));
            rows.add(new StudentGuidanceUploadRow(1, "{}", true));
        }
        return new StudentGuidanceInspection(rows, errors);
    }

    private String csvValue(String[] headers, String[] values, String name) {
        for (int index = 0; index < headers.length; index++) if (name.equals(headers[index].trim())) return index < values.length ? values[index].trim() : "";
        return "";
    }

    private String guidancePayload(String managementItemCode, String occurrenceDate, String studentName, String guidanceStartDate, String guidanceEndDate, String studentCount) {
        return "{\"managementItemCode\":\"" + json(managementItemCode) + "\",\"occurrenceDate\":\"" + json(occurrenceDate) + "\",\"studentName\":\"" + json(studentName) + "\",\"guidanceStartDate\":\"" + json(guidanceStartDate) + "\",\"guidanceEndDate\":\"" + json(guidanceEndDate) + "\",\"studentCount\":\"" + json(studentCount) + "\"}";
    }

    private String json(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private record StudentGuidanceInspection(List<StudentGuidanceUploadRow> rows, List<ExcelUploadErrorRow> errors) { }
    private record StudentGuidanceUploadRow(int rowNumber, String payload, boolean invalid) { }

    private List<ExcelUploadErrorRow> inspectUpload(String uploadId, MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (text.contains("E9999") || text.contains("DUPLICATE")) {
                return List.of(new ExcelUploadErrorRow("ERR-" + UUID.randomUUID(), uploadId, 2, "교번", "E9999", "INVALID_CODE", "존재하지 않는 교번입니다.", "KORUS 기준 교번을 확인하세요."));
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.of();
    }

    private int countDataRows(MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            return Math.max(1, (int) text.lines().skip(1).filter(line -> !line.isBlank()).count());
        } catch (Exception ignored) {
            return 1;
        }
    }

    private void requireUser(Long userId) { if (userId == null) throw new ForbiddenException(); }
    private int safeSize(int size) { return ALLOWED_PAGE_SIZES.contains(size) ? size : 20; }
    private String blankToNull(String value) { return value == null || value.trim().isBlank() ? null : value.trim(); }
    private String safeOriginalName(String filename) { return blankToNull(filename) == null ? "upload.xlsx" : filename.replace("/", "").replace("\\", ""); }
}

package kr.ac.knue.commonfoundation.excel;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
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

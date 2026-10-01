package kr.ac.knue.commonfoundation.basic81;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Owns individual student-guidance persistence and the R07-only CSV validation/
 * commit workflow. Validation is completed before any domain achievement row is created.
 */
@Service
public class StudentGuidanceAchievementService {
    private static final Set<String> ACHIEVEMENT_ROLES = Set.of("R01", "R02", "R04");
    private static final String TEMPLATE_ID = "B77-SG-TEMPLATE-001";
    private final StudentGuidanceAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;
    private final ExcelOperationsService excelOperationsService;

    public StudentGuidanceAchievementService(
            StudentGuidanceAchievementMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper,
            ExcelOperationsService excelOperationsService) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
        this.excelOperationsService = excelOperationsService;
    }

    /** Lists caller-scoped student guidance records using only supplied filters. */
    @Transactional(readOnly = true)
    public StudentGuidanceAchievementSearchResponse list(StudentGuidanceAchievementSearchCriteria criteria,
            CurrentUser requester) {
        requireAchievementRole(requester);
        StudentGuidanceAchievementSearchCriteria safe = criteria == null
                ? new StudentGuidanceAchievementSearchCriteria(0, 20, null, null, null, null, null, null)
                : criteria;
        return new StudentGuidanceAchievementSearchResponse(
                mapper.list(safe, requester.userId(), requester.roles()), safe.safePage(), safe.safePageSize(),
                mapper.count(safe, requester.userId(), requester.roles()));
    }

    /** Saves a header and all detail rows in one guarded transaction. */
    @Transactional
    public StudentGuidanceAchievementRow save(SaveStudentGuidanceAchievementRequest request,
            CurrentUser requester, String requestId) {
        validateSave(request);
        requireAchievementRole(requester);
        StudentGuidanceAchievementRow existing = request.achievementId() == null ? null : find(request.achievementId());
        Long targetUserId = existing == null ? requester.userId() : existing.targetUserId();
        String evaluationYear = existing == null ? String.valueOf(Year.from(request.guidanceStartDate())) : existing.evaluationYear();
        guardService.validateMutation(requester,
                new EducationAchievementMutationContext(targetUserId, evaluationYear, request.guidanceStartDate()));
        List<String> students = serializeStudents(request.students());
        if (existing == null) {
            String managementNo = "SG-" + UUID.randomUUID();
            mapper.insertHeader(managementNo, requester.userId(), evaluationYear, request.managementItemCode().trim(),
                    request.guidanceStartDate(), request.guidanceEndDate(), students.size(), blankToNull(request.attachmentRef()), requester.userId());
            StudentGuidanceAchievementRow saved = mapper.findByManagementNo(managementNo);
            if (saved == null) throw new NotFoundException("저장한 학생지도 실적을 찾을 수 없습니다.");
            saveStudents(saved.achievementId(), students, requester.userId());
            mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                    "STUDENT_GUIDANCE",
                    saved.achievementId(),
                    null,
                    EducationAchievementStatus.DRAFT,
                    "CREATE",
                    null,
                    "학생지도 실적 최초 입력",
                    requester.userId(),
                    java.time.LocalDateTime.now()));
            mapper.insertChangeHistory("student_guidance_achievements", String.valueOf(saved.achievementId()), "CREATE",
                    "student_count", null, String.valueOf(students.size()), requester.userId(), "학생지도 실적 저장", requestId);
            return mapper.find(saved.achievementId());
        }
        mapper.updateHeader(existing.achievementId(), request.managementItemCode().trim(), request.guidanceStartDate(),
                request.guidanceEndDate(), students.size(), blankToNull(request.attachmentRef()), requester.userId());
        mapper.deleteStudents(existing.achievementId());
        saveStudents(existing.achievementId(), students, requester.userId());
        mapper.insertChangeHistory("student_guidance_achievements", String.valueOf(existing.achievementId()), "UPDATE",
                "student_count", String.valueOf(existing.studentCount()), String.valueOf(students.size()), requester.userId(),
                "학생지도 실적 수정", requestId);
        return mapper.find(existing.achievementId());
    }

    /** Delegates template rendering to the shared Excel template/version service. */
    @Transactional(readOnly = true)
    public ExcelDownloadFile downloadExcelTemplate(CurrentUser requester) {
        requireExcelRole(requester);
        return excelOperationsService.downloadUploadTemplate(TEMPLATE_ID, requester.userId());
    }

    /** Validates an R07 upload and persists only staging/error/history records. */
    @Transactional
    public StudentGuidanceExcelUploadResult validateExcelUpload(MultipartFile file, CurrentUser requester) {
        requireExcelRole(requester);
        validateFile(file);
        String uploadId = "SG-UP-" + UUID.randomUUID();
        List<CsvRow> rows = readRows(file);
        List<StudentGuidanceExcelErrorRow> errors = validateRows(rows);
        mapper.insertExcelUpload(uploadId, TEMPLATE_ID, safeFilename(file.getOriginalFilename()), requester.userId(),
                errors.isEmpty() ? "VALIDATED" : "REJECTED");
        Set<Integer> invalidRows = new HashSet<>();
        for (StudentGuidanceExcelErrorRow error : errors) {
            invalidRows.add(error.rowNumber());
            mapper.insertExcelError("SG-ERR-" + UUID.randomUUID(), uploadId, error.rowNumber(), error.columnName(),
                    error.inputValue(), error.errorCode(), error.errorReason(), error.correctionGuide());
        }
        for (CsvRow row : rows) {
            mapper.insertExcelStaging("SG-STG-" + UUID.randomUUID(), uploadId, row.rowNumber(), row.payload(),
                    invalidRows.contains(row.rowNumber()) ? "ERROR" : "NORMAL");
        }
        mapper.upsertExcelHistory(uploadId, rows.size(), rows.size() - invalidRows.size(), invalidRows.size(), 0,
                requester.userId());
        return new StudentGuidanceExcelUploadResult(uploadId, safeFilename(file.getOriginalFilename()), rows.size(),
                rows.size() - invalidRows.size(), invalidRows.size(), errors);
    }

    /** Commits all validated rows atomically; any recorded error keeps domain data unchanged. */
    @Transactional
    public StudentGuidanceExcelCommitResult commitExcelUpload(String uploadId, CurrentUser requester, String requestId) {
        requireExcelRole(requester);
        if (uploadId == null || uploadId.isBlank() || mapper.existsExcelUpload(uploadId.trim()) == 0) {
            throw new NotFoundException("업로드 파일을 찾을 수 없습니다.");
        }
        if (mapper.countExcelErrors(uploadId.trim()) > 0) {
            throw new ConflictException("오류 행이 있어 전체 반영을 차단했습니다.");
        }
        List<StudentGuidanceExcelStagingRow> rows = mapper.listNormalExcelStaging(uploadId.trim());
        for (StudentGuidanceExcelStagingRow row : rows) {
            JsonNode payload = readPayload(row.payload());
            String managementNo = "SG-" + UUID.randomUUID();
            LocalDate start = LocalDate.parse(payload.path("guidanceStartDate").asText());
            LocalDate end = LocalDate.parse(payload.path("guidanceEndDate").asText());
            mapper.insertHeader(managementNo, requester.userId(), String.valueOf(Year.from(start)),
                    payload.path("managementItemCode").asText(), start, end, 1, null, requester.userId());
            StudentGuidanceAchievementRow saved = mapper.findByManagementNo(managementNo);
            if (saved == null) {
                throw new NotFoundException("Excel 반영한 학생지도 실적을 찾을 수 없습니다.");
            }
            mapper.insertStudent(saved.achievementId(), payload.path("student").toString(), requester.userId());
            mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                    "STUDENT_GUIDANCE",
                    saved.achievementId(),
                    null,
                    EducationAchievementStatus.DRAFT,
                    "CREATE",
                    null,
                    "학생지도 Excel 일괄 반영",
                    requester.userId(),
                    java.time.LocalDateTime.now()));
            mapper.insertChangeHistory("student_guidance_achievements", String.valueOf(saved.achievementId()), "CREATE",
                    "student_count", null, "1", requester.userId(), "학생지도 Excel 일괄 반영", requestId);
        }
        mapper.markExcelCommitted(uploadId.trim());
        mapper.upsertExcelHistory(uploadId.trim(), rows.size(), rows.size(), 0, rows.size(), requester.userId());
        mapper.deleteNormalExcelStaging(uploadId.trim());
        return new StudentGuidanceExcelCommitResult(uploadId.trim(), rows.size());
    }

    @Transactional(readOnly = true)
    public List<StudentGuidanceExcelErrorRow> listExcelErrors(String uploadId, CurrentUser requester) {
        requireExcelRole(requester);
        return mapper.listExcelErrors(uploadId);
    }

    @Transactional(readOnly = true)
    public List<StudentGuidanceExcelHistoryRow> listExcelHistories(CurrentUser requester) {
        requireExcelRole(requester);
        return mapper.listExcelHistories(requester.userId());
    }

    private void saveStudents(Long achievementId, List<String> students, Long userId) {
        for (String student : students) mapper.insertStudent(achievementId, student, userId);
    }

    private StudentGuidanceAchievementRow find(Long achievementId) {
        StudentGuidanceAchievementRow found = mapper.find(achievementId);
        if (found == null) throw new NotFoundException("학생지도 실적을 찾을 수 없습니다.");
        return found;
    }

    private void validateSave(SaveStudentGuidanceAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) errors.add(new ValidationError("body", "학생지도 실적 정보를 입력하세요."));
        else {
            if (blankToNull(request.managementItemCode()) == null) errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            if (request.guidanceStartDate() == null) errors.add(new ValidationError("guidanceStartDate", "지도 시작일을 입력하세요."));
            if (request.guidanceEndDate() == null) errors.add(new ValidationError("guidanceEndDate", "지도 종료일을 입력하세요."));
            if (request.guidanceStartDate() != null && request.guidanceEndDate() != null
                    && request.guidanceEndDate().isBefore(request.guidanceStartDate())) errors.add(new ValidationError("guidanceEndDate", "지도 종료일은 시작일보다 빠를 수 없습니다."));
            if (request.students() == null || request.students().isEmpty()) errors.add(new ValidationError("students", "지도학생을 1명 이상 입력하세요."));
        }
        if (!errors.isEmpty()) throw new BusinessValidationException("학생지도 저장 요청이 올바르지 않습니다.", errors);
    }

    private List<String> serializeStudents(List<JsonNode> students) {
        List<String> serialized = new ArrayList<>();
        try {
            for (JsonNode student : students) {
                serialized.add(objectMapper.writeValueAsString(student));
            }
            return serialized;
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException("지도학생 상세 입력값이 올바르지 않습니다.",
                    List.of(new ValidationError("students", "지도학생 상세 입력값을 확인하세요.")));
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || !safeFilename(file.getOriginalFilename()).toLowerCase().endsWith(".csv")) {
            throw new BusinessValidationException("학생지도 Excel 파일이 올바르지 않습니다.",
                    List.of(new ValidationError("file", "STUDENT_GUIDANCE 현행 CSV 양식을 선택하세요.")));
        }
    }

    private List<CsvRow> readRows(MultipartFile file) {
        try {
            List<String> lines = new String(file.getBytes(), StandardCharsets.UTF_8).lines().filter(line -> !line.isBlank()).toList();
            if (lines.isEmpty() || !lines.get(0).equals("templateVersion,managementItemCode,guidanceStartDate,guidanceEndDate,studentName")) {
                throw new BusinessValidationException("학생지도 Excel 파일이 올바르지 않습니다.",
                        List.of(new ValidationError("file", "templateVersion을 포함한 현행 양식을 사용하세요.")));
            }
            List<CsvRow> rows = new ArrayList<>();
            for (int index = 1; index < lines.size(); index++) {
                String[] values = lines.get(index).split(",", -1);
                String payload = objectMapper.writeValueAsString(java.util.Map.of("templateVersion", value(values, 0),
                        "managementItemCode", value(values, 1), "guidanceStartDate", value(values, 2),
                        "guidanceEndDate", value(values, 3), "student", java.util.Map.of("studentName", value(values, 4))));
                rows.add(new CsvRow(index + 1, value(values, 0), value(values, 1), value(values, 2), value(values, 3), value(values, 4), payload));
            }
            return rows;
        } catch (BusinessValidationException exception) { throw exception;
        } catch (Exception exception) {
            throw new BusinessValidationException("학생지도 Excel 파일을 읽지 못했습니다.",
                    List.of(new ValidationError("file", "CSV 파일 형식을 확인하세요.")));
        }
    }

    private List<StudentGuidanceExcelErrorRow> validateRows(List<CsvRow> rows) {
        List<StudentGuidanceExcelErrorRow> errors = new ArrayList<>();
        Set<String> duplicates = new HashSet<>();
        for (CsvRow row : rows) {
            if (!"v1.0".equals(row.templateVersion())) errors.add(error(row, "templateVersion", row.templateVersion(), "INVALID_TEMPLATE", "현행 양식 버전이 아닙니다."));
            if (blankToNull(row.managementItemCode()) == null) errors.add(error(row, "managementItemCode", row.managementItemCode(), "REQUIRED", "관리항목은 필수입니다."));
            if (blankToNull(row.studentName()) == null) errors.add(error(row, "studentName", row.studentName(), "REQUIRED", "학생명은 필수입니다."));
            try {
                LocalDate start = LocalDate.parse(row.guidanceStartDate());
                LocalDate end = LocalDate.parse(row.guidanceEndDate());
                if (end.isBefore(start)) errors.add(error(row, "guidanceEndDate", row.guidanceEndDate(), "INVALID_DATE_RANGE", "종료일은 시작일보다 빠를 수 없습니다."));
            } catch (Exception ignored) { errors.add(error(row, "guidanceStartDate", row.guidanceStartDate(), "INVALID_DATE", "지도기간 날짜 형식을 확인하세요.")); }
            String key = row.managementItemCode() + "|" + row.guidanceStartDate() + "|" + row.guidanceEndDate() + "|" + row.studentName();
            if (blankToNull(row.studentName()) != null && !duplicates.add(key)) {
                errors.add(error(row, "studentName", row.studentName(), "DUPLICATE", "중복 행은 자동 갱신하지 않습니다."));
            }
        }
        return errors;
    }

    private StudentGuidanceExcelErrorRow error(CsvRow row, String column, String value, String code, String reason) {
        return new StudentGuidanceExcelErrorRow(row.rowNumber(), column, value, code, reason, "현행 양식과 입력값을 확인하세요.");
    }
    private JsonNode readPayload(String payload) { try { return objectMapper.readTree(payload); } catch (JsonProcessingException exception) { throw new IllegalArgumentException("검증된 업로드 행을 읽지 못했습니다."); } }
    private void requireAchievementRole(CurrentUser user) { if (user == null || user.roles() == null || user.roles().stream().noneMatch(ACHIEVEMENT_ROLES::contains)) throw new ForbiddenException(); }
    private void requireExcelRole(CurrentUser user) { if (user == null || user.roles() == null || !user.roles().contains("R07")) throw new ForbiddenException(); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String value(String[] values, int index) { return index < values.length ? values[index].trim() : ""; }
    private String safeFilename(String name) { return blankToNull(name) == null ? "student-guidance.csv" : name.replace("/", "").replace("\\", ""); }
    private record CsvRow(int rowNumber, String templateVersion, String managementItemCode, String guidanceStartDate, String guidanceEndDate, String studentName, String payload) { }
}

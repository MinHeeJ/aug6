package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorRow;
import kr.ac.knue.commonfoundation.excel.ExcelUploadHistoryRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** R07-only wrapper that validates the STUDENT_GUIDANCE template, retains errors, and commits all rows atomically. */
@Service
public class StudentGuidanceExcelService {
    private static final List<String> HEADERS = List.of("교번", "관리항목코드", "지도시작일", "지도종료일", "학생명");
    private final StudentGuidanceAchievementMapper mapper;
    private final ObjectMapper objectMapper;
    public StudentGuidanceExcelService(StudentGuidanceAchievementMapper mapper, ObjectMapper objectMapper) { this.mapper = mapper; this.objectMapper = objectMapper; }

    /** Stores validation staging/history for a student-guidance template without creating business rows. */
    @Transactional
    public StudentGuidanceExcelUploadResult upload(MultipartFile file, CurrentUser actor) {
        requireR07(actor);
        if (file == null || file.isEmpty()) throw new BusinessValidationException("엑셀 파일을 선택하세요.", List.of(new ValidationError("file", "파일을 선택하세요.")));
        String name = file.getOriginalFilename() == null ? "student-guidance.csv" : file.getOriginalFilename().replace("/", "").replace("\\", "");
        if (!name.toLowerCase().endsWith(".csv")) throw new BusinessValidationException("학생지도 양식 검증에 실패했습니다.", List.of(new ValidationError("file", "STUDENT_GUIDANCE CSV 양식 파일을 업로드하세요.")));
        String uploadId = "SG-UP-" + UUID.randomUUID();
        List<String> lines;
        try { lines = new String(file.getBytes(), StandardCharsets.UTF_8).lines().toList(); }
        catch (Exception exception) { throw new BusinessValidationException("파일을 읽을 수 없습니다.", List.of(new ValidationError("file", "파일 내용을 확인하세요."))); }
        List<ExcelUploadErrorRow> errors = new ArrayList<>();
        if (lines.isEmpty() || !HEADERS.equals(List.of(lines.get(0).split(",", -1)))) errors.add(error(uploadId, 1, "file", "", "TEMPLATE_VERSION_MISSING", "학생지도 현행 양식 헤더가 아닙니다.", "템플릿을 다시 다운로드하세요."));
        int total = Math.max(0, lines.size() - 1);
        List<String> identities = new ArrayList<>();
        List<StagingPayload> stagingPayloads = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            int row = index + 1;
            int errorsBeforeRow = errors.size();
            String[] values = lines.get(index).split(",", -1);
            if (values.length != 5) {
                errors.add(error(uploadId, row, "row", lines.get(index), "INVALID_COLUMN_COUNT", "필수 열 수가 일치하지 않습니다.", "템플릿 열 순서를 유지하세요."));
                continue;
            }
            String employeeNo = values[0].trim(), code = values[1].trim(), start = values[2].trim(), end = values[3].trim(), student = values[4].trim();
            if (employeeNo.isEmpty() || code.isEmpty() || start.isEmpty() || end.isEmpty() || student.isEmpty()) errors.add(error(uploadId, row, "required", lines.get(index), "REQUIRED_VALUE", "필수값이 누락되었습니다.", "교번, 관리항목코드, 지도기간, 학생명을 입력하세요."));
            else if (mapper.findUserIdByEmployeeNo(employeeNo) == null) errors.add(error(uploadId, row, "교번", employeeNo, "UNKNOWN_TEACHER", "활성 교원 정보를 찾을 수 없습니다.", "교번을 확인하세요."));
            try { if (!start.isEmpty() && !end.isEmpty() && LocalDate.parse(end).isBefore(LocalDate.parse(start))) errors.add(error(uploadId, row, "지도종료일", end, "INVALID_DATE_RANGE", "지도종료일은 시작일 이전일 수 없습니다.", "지도기간을 확인하세요.")); } catch (Exception exception) { errors.add(error(uploadId, row, "지도기간", start + "~" + end, "INVALID_DATE", "날짜 형식이 올바르지 않습니다.", "YYYY-MM-DD 형식으로 입력하세요.")); }
            String identity = employeeNo + "|" + code + "|" + start + "|" + end + "|" + student;
            if (!identities.add(identity)) errors.add(error(uploadId, row, "row", identity, "DUPLICATE", "업로드 파일 안에 중복된 학생지도 행이 있습니다.", "중복 행을 제거하세요."));
            try { stagingPayloads.add(new StagingPayload(row, objectMapper.writeValueAsString(new String[]{employeeNo, code, start, end, student}), errors.size() == errorsBeforeRow)); } catch (Exception exception) { throw new IllegalStateException("학생지도 staging payload를 생성할 수 없습니다.", exception); }
        }
        String status = errors.isEmpty() ? "VALIDATED" : "REJECTED";
        mapper.insertUploadFile(uploadId, name, actor.userId(), status);
        for (StagingPayload payload : stagingPayloads) mapper.insertStagingRow(uploadId, payload.rowNumber(), payload.payload(), payload.valid() ? "NORMAL" : "ERROR");
        for (ExcelUploadErrorRow error : errors) mapper.insertUploadError(error.errorId(), uploadId, error.rowNumber(), error.columnName(), error.inputValue(), error.errorCode(), error.errorReason(), error.correctionGuide());
        mapper.upsertUploadHistory(uploadId, total, errors.isEmpty() ? total : 0, errors.size(), 0, actor.userId());
        return new StudentGuidanceExcelUploadResult(uploadId, name, status, total, errors.isEmpty() ? total : 0, errors.size(), errors);
    }

    /** Commits all validated staging rows in one transaction or leaves business rows unchanged on any error. */
    @Transactional
    public StudentGuidanceExcelCommitResult commit(String uploadId, CurrentUser actor) {
        requireR07(actor);
        if (mapper.existsValidatedUpload(uploadId, actor.userId()) == 0) throw new NotFoundException("검증 완료된 업로드를 찾을 수 없습니다.");
        if (mapper.countUploadErrors(uploadId) > 0) throw new ConflictException("오류 행이 있어 전체 반영을 차단했습니다.");
        List<String> payloads = mapper.normalStagingPayloads(uploadId);
        for (String payload : payloads) {
            try {
                String[] row = objectMapper.readValue(payload, String[].class);
                Long teacherUserId = mapper.findUserIdByEmployeeNo(row[0]);
                if (teacherUserId == null) throw new ConflictException("학생지도 Excel 반영 교원 정보를 찾을 수 없습니다.");
                String organization = mapper.findActiveOrganizationCode(teacherUserId);
                if (organization == null) throw new ConflictException("학생지도 Excel 반영 교원의 소속 정보를 찾을 수 없습니다.");
                String managementNo = "SG-" + LocalDate.parse(row[2]).getYear() + "-" + UUID.randomUUID();
                mapper.insertAchievement(managementNo, teacherUserId, String.valueOf(LocalDate.parse(row[2]).getYear()), organization, row[1], LocalDate.parse(row[2]), LocalDate.parse(row[3]), null, actor.userId(), "학생지도 Excel 일괄반영");
                StudentGuidanceAchievementRow saved = mapper.list(new StudentGuidanceAchievementSearchCriteria(0, 20, managementNo, null, null, null, null, null, actor.userId(), "R04")).get(0);
                mapper.insertStudent(saved.achievementId(), new StudentGuidanceStudentRequest(null, row[4]), actor.userId());
                mapper.insertChangeHistory(saved.achievementId(), "CREATE", null, row[1], actor.userId(), "학생지도 Excel 일괄반영");
            } catch (Exception exception) { throw new ConflictException("학생지도 Excel 반영 데이터를 처리할 수 없습니다."); }
        }
        mapper.markUploadCommitted(uploadId); mapper.upsertUploadHistory(uploadId, payloads.size(), payloads.size(), 0, payloads.size(), actor.userId()); mapper.deleteStagingRows(uploadId);
        return new StudentGuidanceExcelCommitResult(uploadId, payloads.size());
    }
    @Transactional(readOnly = true) public ExcelDownloadFile downloadErrors(String uploadId, CurrentUser actor) { requireR07(actor); StringBuilder csv = new StringBuilder("rowNumber,columnName,errorCode,errorReason,correctionGuide\n"); for (ExcelUploadErrorRow row : mapper.listUploadErrors(uploadId)) csv.append(row.rowNumber()).append(',').append(row.columnName()).append(',').append(row.errorCode()).append(',').append(row.errorReason()).append(',').append(row.correctionGuide()).append('\n'); return new ExcelDownloadFile("학생지도_업로드오류_" + uploadId + ".csv", "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8)); }
    @Transactional(readOnly = true) public List<ExcelUploadHistoryRow> histories(CurrentUser actor) { requireHistoryReader(actor); return mapper.listUploadHistories(actor.userId(), 100, 0); }
    private record StagingPayload(int rowNumber, String payload, boolean valid) {
    }

    private ExcelUploadErrorRow error(String uploadId, int row, String column, String input, String code, String reason, String guide) { return new ExcelUploadErrorRow("SG-ERR-" + UUID.randomUUID(), uploadId, row, column, input, code, reason, guide); }
    private void requireR07(CurrentUser actor) { if (actor == null || actor.roles() == null || (!actor.roles().contains("R07") && !actor.roles().contains("R09"))) throw new ForbiddenException(); }
    private void requireHistoryReader(CurrentUser actor) { if (actor == null || actor.roles() == null || actor.roles().stream().noneMatch(role -> role.equals("R01") || role.equals("R02") || role.equals("R04") || role.equals("R07") || role.equals("R09"))) throw new ForbiddenException(); }
}

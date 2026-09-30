package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
/** Coordinates individual and Excel-based student-guidance persistence using the common Excel staging and history contract. */
@Service
public class StudentGuidanceAchievementService {
    private static final Set<String> REQUIRED_HEADERS = Set.of("교번", "학생학번", "학생명", "지도유형", "지도시작일", "지도종료일");
    private final StudentGuidanceAchievementMapper mapper;
    private final ObjectMapper objectMapper;
    public StudentGuidanceAchievementService(StudentGuidanceAchievementMapper mapper, ObjectMapper objectMapper) { this.mapper = mapper; this.objectMapper = objectMapper; }
    @Transactional(readOnly = true)
    public List<StudentGuidanceAchievementRow> list(CurrentUser user, int page, int size) { requireReadUser(user); if (page < 0 || !Set.of(20,50,100).contains(size)) throw validation("pageSize", "pageSize는 20, 50, 100 중 하나여야 합니다."); return mapper.list(user.userId(), size, page * size); }
    /** Persists one header and all child students in one transaction after rejecting invalid dates and duplicates. */
    @Transactional
    public StudentGuidanceAchievementRow save(StudentGuidanceSaveRequest request, CurrentUser user) {
        requireFaculty(user); validateRequest(request); String organizationCode = organization(user.userId());
        for (StudentGuidanceStudentRequest student : request.students()) if (mapper.existsDuplicate(user.userId(), student.studentNo().trim(), request.guidanceStartDate(), request.guidanceEndDate()) > 0) throw new ConflictException("DUPLICATE_STUDENT_GUIDANCE: 중복 학생지도 실적입니다.");
        StudentGuidanceAchievementRow row = new StudentGuidanceAchievementRow(null, "SG-" + UUID.randomUUID(), String.valueOf(request.guidanceStartDate().getYear()), user.userId(), request.managementItemCode().trim(), request.guidanceStartDate(), request.guidanceEndDate(), request.students().size(), "DRAFTING", List.of());
        mapper.insertAchievement(row, organizationCode, user.userId(), reason(request.changeReason()));
        Long achievementId = mapper.findAchievementIdByManagementNo(row.managementNo());
        for (StudentGuidanceStudentRequest student : request.students()) mapper.insertStudent(achievementId, student, user.userId());
        return new StudentGuidanceAchievementRow(achievementId, row.managementNo(), row.evaluationYear(), row.teacherUserId(), row.managementItemCode(), row.guidanceStartDate(), row.guidanceEndDate(), row.studentCount(), row.achievementStatus(), request.students().stream().map(s -> new StudentGuidanceStudentRow(null,s.studentNo(),s.studentName(),s.guidanceType())).toList());
    }
    /** Validates a STUDENT_GUIDANCE template upload and stores only validation/staging evidence before confirmation. */
    @Transactional
    public StudentGuidanceUploadResult upload(String templateId, MultipartFile file, CurrentUser user) {
        requireR07(user); if (file == null || file.isEmpty()) throw validation("file", "엑셀 파일을 선택하세요."); if (templateId == null || mapper.countTemplate(templateId) == 0) throw new NotFoundException("학생지도 업로드 양식을 찾을 수 없습니다.");
        String uploadId = "UP-SG-" + UUID.randomUUID(); String name = safeName(file.getOriginalFilename()); List<ParsedRow> rows = parse(file); List<StudentGuidanceUploadError> errors = validateRows(rows, user.userId());
        String status = errors.isEmpty() ? "VALIDATED" : "REJECTED";
        mapper.insertUploadFile(uploadId, templateId, "upload-file-" + uploadId, name, user.userId(), status);
        for (ParsedRow row : rows) mapper.insertStagingRow("STG-" + UUID.randomUUID(), uploadId, row.rowNumber(), json(row.values()), errors.stream().anyMatch(e -> e.rowNumber() == row.rowNumber()) ? "ERROR" : "NORMAL");
        for (StudentGuidanceUploadError error : errors) mapper.insertUploadError("ERR-" + UUID.randomUUID(), uploadId, error.rowNumber(), error.columnName(), "", error.errorCode(), error.errorReason());
        mapper.upsertUploadHistory(uploadId, rows.size(), rows.size() - errors.size(), errors.size(), 0, user.userId());
        return new StudentGuidanceUploadResult(uploadId, name, status, rows.size(), rows.size()-errors.size(), errors.size(), 0, errors);
    }
    /** Returns R07-visible upload history without exposing file storage locations or tokens. */
    @Transactional(readOnly = true)
    public List<kr.ac.knue.commonfoundation.excel.ExcelUploadHistoryRow> histories(CurrentUser user, int page, int size) { requireHistoryReader(user); if (page < 0 || !Set.of(20,50,100).contains(size)) throw validation("pageSize", "pageSize는 20, 50, 100 중 하나여야 합니다."); return mapper.listHistories(size, page * size); }
    /** Commits all normal staged rows atomically; any validation error prevents every domain write. */
    @Transactional
    public StudentGuidanceUploadResult commit(String uploadId, CurrentUser user) {
        requireR07(user); if (uploadId == null || mapper.existsUpload(uploadId) == 0) throw new NotFoundException("업로드 파일을 찾을 수 없습니다."); if (mapper.countErrors(uploadId) > 0) throw new ConflictException("UPLOAD_ERRORS_PRESENT: 오류 행이 있어 전체 반영을 차단했습니다.");
        List<StudentGuidanceStagingRow> staged = mapper.listNormalStagingRows(uploadId); String organizationCode = organization(user.userId());
        for (StudentGuidanceStagingRow stagedRow : staged) { Map<String,String> v = read(stagedRow.payload()); LocalDate start = LocalDate.parse(v.get("지도시작일")); LocalDate end = LocalDate.parse(v.get("지도종료일")); String studentNo=v.get("학생학번"); if (mapper.existsDuplicate(user.userId(),studentNo,start,end)>0) throw new ConflictException("DUPLICATE_STUDENT_GUIDANCE: 중복 학생지도 실적입니다."); StudentGuidanceAchievementRow row=new StudentGuidanceAchievementRow(null,"SG-"+UUID.randomUUID(),String.valueOf(start.getYear()),user.userId(),"STUDENT_GUIDANCE",start,end,1,"DRAFTING",List.of()); mapper.insertAchievement(row,organizationCode,user.userId(),"학생지도 Excel 일괄등록"); mapper.insertStudent(mapper.findAchievementIdByManagementNo(row.managementNo()),new StudentGuidanceStudentRequest(studentNo,v.get("학생명"),v.get("지도유형")),user.userId()); }
        mapper.markCommitted(uploadId); mapper.upsertUploadHistory(uploadId,staged.size(),staged.size(),0,staged.size(),user.userId()); mapper.deleteNormalStagingRows(uploadId);
        return new StudentGuidanceUploadResult(uploadId,"", "COMMITTED",staged.size(),staged.size(),0,staged.size(),List.of());
    }
    private List<ParsedRow> parse(MultipartFile file) { try { List<String> lines = new String(file.getBytes(), StandardCharsets.UTF_8).lines().filter(line -> !line.isBlank()).toList(); if (lines.isEmpty()) throw validation("file","업로드 파일에 헤더가 없습니다."); String[] header=lines.get(0).split(",",-1); Set<String> headers=Set.of(header); if (!headers.containsAll(REQUIRED_HEADERS)) throw validation("file","학생지도 양식의 필수 열이 누락되었습니다."); List<ParsedRow> result=new ArrayList<>(); for(int i=1;i<lines.size();i++){String[] cells=lines.get(i).split(",",-1); java.util.LinkedHashMap<String,String> values=new java.util.LinkedHashMap<>(); for(int j=0;j<header.length;j++) values.put(header[j].trim(),j<cells.length?cells[j].trim():""); result.add(new ParsedRow(i+1,values));} return result; } catch (BusinessValidationException e) { throw e; } catch(Exception e){ throw validation("file","CSV 형식의 학생지도 양식을 읽을 수 없습니다."); } }
    private List<StudentGuidanceUploadError> validateRows(List<ParsedRow> rows, Long userId) { List<StudentGuidanceUploadError> errors=new ArrayList<>(); Set<String> keys=new HashSet<>(); for(ParsedRow row:rows){Map<String,String> v=row.values(); for(String h:REQUIRED_HEADERS) if(v.getOrDefault(h,"").isBlank()) {errors.add(new StudentGuidanceUploadError(row.rowNumber(),h,"REQUIRED","필수값을 입력하세요.")); break;} if(errors.stream().anyMatch(e->e.rowNumber()==row.rowNumber())) continue; try {LocalDate s=LocalDate.parse(v.get("지도시작일")); LocalDate e=LocalDate.parse(v.get("지도종료일")); if(e.isBefore(s)) errors.add(new StudentGuidanceUploadError(row.rowNumber(),"지도종료일","INVALID_DATE","지도종료일은 시작일보다 빠를 수 없습니다.")); else {String key=v.get("학생학번")+"|"+s+"|"+e; if(!keys.add(key)||mapper.existsDuplicate(userId,v.get("학생학번"),s,e)>0) errors.add(new StudentGuidanceUploadError(row.rowNumber(),"학생학번","DUPLICATE","중복 학생지도 실적입니다."));}} catch(Exception e){errors.add(new StudentGuidanceUploadError(row.rowNumber(),"지도시작일","INVALID_DATE","날짜는 YYYY-MM-DD 형식이어야 합니다."));}} return errors; }
    private void validateRequest(StudentGuidanceSaveRequest r){if(r.guidanceEndDate().isBefore(r.guidanceStartDate())) throw validation("guidanceEndDate","지도종료일은 시작일보다 빠를 수 없습니다."); Set<String> seen=new HashSet<>(); for(StudentGuidanceStudentRequest s:r.students())if(!seen.add(s.studentNo().trim()))throw validation("students","동일 학생을 중복 입력할 수 없습니다.");}
    private String organization(Long id){String org=mapper.findOrganizationCodeForUser(id);if(org==null||org.isBlank())throw validation("organizationCode","평가조직 매핑을 먼저 설정하세요.");return org;}
    private void requireFaculty(CurrentUser u){if(u==null||u.roles()==null||u.roles().stream().noneMatch(r->Set.of("R01","R02","R04").contains(r)))throw new ForbiddenException();}
    /** System administrators may inspect individual guidance records but cannot change them. */
    private void requireReadUser(CurrentUser u){if(u==null||u.roles()==null||u.roles().stream().noneMatch(r->Set.of("R01","R02","R04","R09").contains(r)))throw new ForbiddenException();}
    private void requireR07(CurrentUser u){if(u==null||u.roles()==null||!u.roles().contains("R07"))throw new ForbiddenException();}
    /** Upload history is readable by its R07 workflow owner and by system administrators. */
    private void requireHistoryReader(CurrentUser u){if(u==null||u.roles()==null||u.roles().stream().noneMatch(r->r.equals("R07")||r.equals("R09")))throw new ForbiddenException();}
    private BusinessValidationException validation(String field,String message){return new BusinessValidationException("학생지도 입력값이 올바르지 않습니다.",List.of(new ValidationError(field,message)));}
    private String reason(String reason){return reason==null||reason.isBlank()?"학생지도 실적 저장":reason.trim();}
    private String safeName(String name){return name==null||name.isBlank()?"student-guidance.csv":name.replace("/","").replace("\\"," ");}
    private String json(Map<String,String> values){try{return objectMapper.writeValueAsString(values);}catch(Exception e){throw new IllegalStateException(e);}}
    private Map<String,String> read(String json){try{return objectMapper.readValue(json,new TypeReference<Map<String,String>>(){});}catch(Exception e){throw new IllegalStateException("검증된 업로드 행을 읽을 수 없습니다.",e);}}
    private record ParsedRow(int rowNumber, Map<String,String> values) {}
}

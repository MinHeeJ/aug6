package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

/** Persists FR-027 individual entries and adapts the shared Excel staging/history model to atomic student-guidance imports. */
@Service
public class StudentGuidanceAchievementService {
    private static final List<String> HEADERS = List.of("employeeNo", "evaluationYear", "organizationCode", "managementItemCode", "guidanceStartDate", "guidanceEndDate", "studentName");
    private final StudentGuidanceMapper mapper;
    private final ObjectMapper objectMapper;
    public StudentGuidanceAchievementService(StudentGuidanceMapper mapper, ObjectMapper objectMapper) { this.mapper = mapper; this.objectMapper = objectMapper; }

    /** Saves a header and all of its student details in one transaction so its displayed count cannot diverge from child rows. */
    @Transactional
    public StudentGuidanceDtos.Row save(StudentGuidanceDtos.SaveRequest request, CurrentUser user) {
        requireIndividualRole(user); validateSave(request);
        StudentGuidanceDtos.Row before = request.achievementId() == null ? null : find(request.achievementId());
        if (request.achievementId() == null) { mapper.insertAchievement(request, user.userId(), user.userId()); }
        else { if (mapper.updateAchievement(request, user.userId()) != 1) throw new NotFoundException("학생지도 실적을 찾을 수 없습니다."); mapper.deleteStudents(request.achievementId()); }
        Long id = request.achievementId() == null ? mapper.findCreatedId(request, user.userId()) : request.achievementId();
        for (StudentGuidanceDtos.Student student : request.students()) mapper.insertStudent(id, student, user.userId());
        StudentGuidanceDtos.Row after = find(id);
        mapper.insertChangeHistory(String.valueOf(id), before == null ? "CREATE" : "UPDATE", String.valueOf(before), String.valueOf(after), user.userId(), request.changeReason().trim());
        return after;
    }
    @Transactional(readOnly = true) public StudentGuidanceDtos.Row find(Long id) { StudentGuidanceDtos.Header row=mapper.findById(id); if(row==null) throw new NotFoundException("학생지도 실적을 찾을 수 없습니다."); return new StudentGuidanceDtos.Row(row.achievementId(),row.evaluationYear(),row.ownerUserId(),row.organizationCode(),row.managementItemCode(),row.guidanceStartDate(),row.guidanceEndDate(),row.studentCount(),mapper.listStudents(id),row.certificationStatus(),row.updatedAt()); }

    /** Validates all CSV rows into shared staging tables; no business row is written during this phase. */
    @Transactional
    public StudentGuidanceDtos.UploadResult validateUpload(MultipartFile file, CurrentUser user) {
        requireExcelRole(user); if(file==null||file.isEmpty()) throw validation("file","Excel 파일을 선택하세요."); if(mapper.countTemplate()==0) throw new ConflictException("STUDENT_GUIDANCE 양식이 활성화되어 있지 않습니다.");
        String uploadId="SG-UP-"+UUID.randomUUID(); String fileName=safeName(file.getOriginalFilename()); List<Map<String,String>> rows=parse(file); List<StudentGuidanceDtos.ExcelRowError> errors=new ArrayList<>(); Set<String> importKeys=new HashSet<>();
        mapper.insertUploadFile(uploadId,fileName,user.userId(),"UPLOADED");
        for(int index=0; index<rows.size(); index++) { Map<String,String> row=rows.get(index); int rowNumber=index+2; List<StudentGuidanceDtos.ExcelRowError> rowErrors=validateRow(row,rowNumber,importKeys); errors.addAll(rowErrors); mapper.insertStaging("SG-STG-"+UUID.randomUUID(),uploadId,rowNumber,toJson(row),rowErrors.isEmpty()?"NORMAL":"ERROR"); }
        for(StudentGuidanceDtos.ExcelRowError error:errors) mapper.insertError("SG-ERR-"+UUID.randomUUID(),uploadId,error);
        int errorRows=(int) errors.stream().map(StudentGuidanceDtos.ExcelRowError::rowNumber).distinct().count();
        mapper.updateUploadValidationStatus(uploadId, errorRows == 0 ? "VALIDATED" : "REJECTED");
        mapper.upsertHistory(uploadId,rows.size(),rows.size()-errorRows,errorRows,0,user.userId());
        return new StudentGuidanceDtos.UploadResult(uploadId,fileName,rows.size(),rows.size()-errorRows,errorRows,errors);
    }
    /** Commits every validated staging row together, or rejects the whole file before any student-guidance insert. */
    @Transactional
    public StudentGuidanceDtos.CommitResult commitUpload(String uploadId, CurrentUser user) {
        requireExcelRole(user); if(mapper.existsUploadForUser(uploadId, user.userId())==0) throw new NotFoundException("학생지도 업로드를 찾을 수 없습니다."); if(mapper.countErrors(uploadId)>0) throw new ConflictException("오류 행이 있어 전체 반영을 차단했습니다."); if(mapper.markCommitted(uploadId)!=1) throw new ConflictException("검증 완료된 업로드만 한 번 반영할 수 있습니다."); int saved=0;
        for(String payload:mapper.listNormalPayloads(uploadId)) { Map<String,String> row=fromJson(payload); Long owner=mapper.findUserIdByEmployeeNo(row.get("employeeNo")); mapper.insertImportedAchievement(row.get("evaluationYear"),owner,row.get("organizationCode"),row.get("managementItemCode"),LocalDate.parse(row.get("guidanceStartDate")),LocalDate.parse(row.get("guidanceEndDate")),row.get("studentName"),user.userId()); saved++; }
        mapper.upsertHistory(uploadId,saved,saved,0,saved,user.userId()); mapper.deleteStaging(uploadId); return new StudentGuidanceDtos.CommitResult(uploadId,saved);
    }
    @Transactional(readOnly=true) public List<StudentGuidanceDtos.UploadHistory> histories(CurrentUser user) { requireExcelReadRole(user); return mapper.listHistories(user.userId()); }
    @Transactional(readOnly=true) public byte[] errorFile(String uploadId, CurrentUser user) { requireExcelRole(user); if(mapper.existsUploadForUser(uploadId,user.userId())==0) throw new NotFoundException("학생지도 업로드를 찾을 수 없습니다."); StringBuilder csv=new StringBuilder("rowNumber,columnName,errorCode,errorReason\n"); for(var error:mapper.listErrors(uploadId)) csv.append(error.rowNumber()).append(',').append(error.columnName()).append(',').append(error.errorCode()).append(',').append(error.errorReason()).append('\n'); return csv.toString().getBytes(StandardCharsets.UTF_8); }
    @Transactional(readOnly=true) public byte[] template(CurrentUser user) { requireExcelReadRole(user); return (String.join(",",HEADERS)+"\n").getBytes(StandardCharsets.UTF_8); }
    private List<StudentGuidanceDtos.ExcelRowError> validateRow(Map<String,String> row, int number, Set<String> importKeys) {
        List<StudentGuidanceDtos.ExcelRowError> result = new ArrayList<>();
        for (String header : HEADERS) {
            if (blank(row.get(header))) result.add(error(number, header, "REQUIRED", "필수값을 입력하세요."));
        }
        if (!blank(row.get("evaluationYear")) && !row.get("evaluationYear").matches("^[0-9]{4}$")) {
            result.add(error(number, "evaluationYear", "INVALID_YEAR", "평가연도는 YYYY 형식이어야 합니다."));
        }
        boolean validDates = true;
        try {
            if (!blank(row.get("guidanceStartDate"))) LocalDate.parse(row.get("guidanceStartDate"));
            if (!blank(row.get("guidanceEndDate"))) LocalDate.parse(row.get("guidanceEndDate"));
            if (!blank(row.get("guidanceStartDate")) && !blank(row.get("guidanceEndDate"))
                    && LocalDate.parse(row.get("guidanceEndDate")).isBefore(LocalDate.parse(row.get("guidanceStartDate")))) {
                result.add(error(number, "guidanceEndDate", "INVALID_DATE_RANGE", "지도 종료일은 시작일보다 빠를 수 없습니다."));
                validDates = false;
            }
        } catch (Exception exception) {
            result.add(error(number, "guidanceStartDate", "INVALID_DATE", "날짜는 YYYY-MM-DD 형식이어야 합니다."));
            validDates = false;
        }
        if (!blank(row.get("evaluationYear")) && row.get("evaluationYear").matches("^[0-9]{4}$")
                && !blank(row.get("managementItemCode"))
                && mapper.existsActiveManagementItem(row.get("evaluationYear"), row.get("managementItemCode")) == 0) {
            result.add(error(number, "managementItemCode", "INVALID_CODE", "활성 관리항목을 선택하세요."));
        }
        Long owner = blank(row.get("employeeNo")) ? null : mapper.findUserIdByEmployeeNo(row.get("employeeNo"));
        if (!blank(row.get("employeeNo")) && owner == null) result.add(error(number, "employeeNo", "INVALID_CODE", "존재하지 않는 교번입니다."));
        if (owner != null && !blank(row.get("organizationCode"))
                && !row.get("organizationCode").equals(mapper.findOrganizationForUser(owner))) {
            result.add(error(number, "organizationCode", "INVALID_CODE", "교원의 활성 소속과 일치하지 않습니다."));
        }
        if (owner != null && validDates && !blank(row.get("guidanceStartDate")) && !blank(row.get("guidanceEndDate"))
                && !blank(row.get("studentName"))) {
            String importKey = owner + "|" + row.get("guidanceStartDate") + "|" + row.get("guidanceEndDate")
                    + "|" + row.get("studentName").trim();
            if (!importKeys.add(importKey)
                    || mapper.existsDuplicate(owner, LocalDate.parse(row.get("guidanceStartDate")),
                            LocalDate.parse(row.get("guidanceEndDate")), row.get("studentName")) > 0) {
                result.add(error(number, "studentName", "DUPLICATE", "중복 학생지도 실적입니다."));
            }
        }
        return result;
    }
    private List<Map<String,String>> parse(MultipartFile file) { try { List<String> lines=new String(file.getBytes(),StandardCharsets.UTF_8).lines().filter(l->!l.isBlank()).toList(); if(lines.isEmpty()) throw validation("file","양식 헤더가 필요합니다."); String[] header=lines.get(0).split(",",-1); if(!HEADERS.equals(List.of(header))) throw validation("file","STUDENT_GUIDANCE 표준 양식 헤더가 일치하지 않습니다."); List<Map<String,String>> rows=new ArrayList<>(); for(int i=1;i<lines.size();i++){String[] cells=lines.get(i).split(",",-1); Map<String,String> row=new LinkedHashMap<>(); for(int j=0;j<HEADERS.size();j++) row.put(HEADERS.get(j),j<cells.length?cells[j].trim():""); rows.add(row);} return rows; } catch(IOException e){throw validation("file","파일을 읽을 수 없습니다.");} }
    private void validateSave(StudentGuidanceDtos.SaveRequest r){if(r==null) throw validation("body","요청 본문이 필요합니다."); List<ValidationError> f=new ArrayList<>();if(blank(r.evaluationYear())||!r.evaluationYear().matches("^[0-9]{4}$"))f.add(new ValidationError("evaluationYear","평가연도는 YYYY 형식이어야 합니다."));if(blank(r.organizationCode()))f.add(new ValidationError("organizationCode","소속 조직을 입력하세요."));if(blank(r.managementItemCode()))f.add(new ValidationError("managementItemCode","관리항목을 입력하세요."));if(r.guidanceStartDate()==null)f.add(new ValidationError("guidanceStartDate","지도 시작일을 입력하세요."));if(r.guidanceEndDate()==null)f.add(new ValidationError("guidanceEndDate","지도 종료일을 입력하세요."));if(r.guidanceStartDate()!=null&&r.guidanceEndDate()!=null&&r.guidanceEndDate().isBefore(r.guidanceStartDate()))f.add(new ValidationError("guidanceEndDate","지도 종료일은 시작일보다 빠를 수 없습니다."));if(r.students()==null||r.students().isEmpty())f.add(new ValidationError("students","지도학생을 한 명 이상 입력하세요."));if(blank(r.changeReason()))f.add(new ValidationError("changeReason","변경 사유를 입력하세요."));if(!f.isEmpty())throw new BusinessValidationException("학생지도 입력값을 확인하세요.",f);}
    private void requireIndividualRole(CurrentUser u){if(u==null||u.roles()==null||u.roles().stream().noneMatch(r->List.of("R01","R02","R04").contains(r)))throw new ForbiddenException();} private void requireExcelRole(CurrentUser u){if(u==null||u.roles()==null||!u.roles().contains("R07"))throw new ForbiddenException();} private void requireExcelReadRole(CurrentUser u){if(u==null||u.roles()==null||u.roles().stream().noneMatch(r->List.of("R07","R09").contains(r)))throw new ForbiddenException();} private StudentGuidanceDtos.ExcelRowError error(int r,String c,String code,String message){return new StudentGuidanceDtos.ExcelRowError(r,c,code,message);} private BusinessValidationException validation(String field,String message){return new BusinessValidationException("학생지도 Excel 요청을 확인하세요.",List.of(new ValidationError(field,message)));} private boolean blank(String s){return s==null||s.isBlank();} private String safeName(String n){return blank(n)?"student-guidance.csv":n.replace("/","").replace("\\","");} private String toJson(Map<String,String> row){try{return objectMapper.writeValueAsString(row);}catch(Exception e){throw new IllegalStateException(e);}} private Map<String,String> fromJson(String p){try{return objectMapper.readValue(p,new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException(e);}}
}

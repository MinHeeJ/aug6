package kr.ac.knue.commonfoundation.achievement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists FR-028 achievements and their guided-student table in one transaction. */
@Service
public class DegreeCompletionAchievementService {
    private final DegreeCompletionMapper mapper;
    public DegreeCompletionAchievementService(DegreeCompletionMapper mapper) { this.mapper = mapper; }
    /** Lists scoped headers and materializes their student detail rows. */
    @Transactional(readOnly = true)
    public DegreeCompletionDtos.SearchResponse list(DegreeCompletionDtos.SearchCriteria criteria, CurrentUser user) {
        requireReadRole(user); List<DegreeCompletionDtos.Row> rows=mapper.list(criteria,user.userId()).stream().map(this::row).toList(); return new DegreeCompletionDtos.SearchResponse(rows,criteria.safePage(),criteria.safeSize(),mapper.count(criteria,user.userId()));
    }
    /** Saves the header plus every student detail row atomically. */
    @Transactional
    public DegreeCompletionDtos.Row save(DegreeCompletionDtos.SaveRequest request, CurrentUser user) {
        requireRole(user); validate(request); DegreeCompletionDtos.Row before=request.achievementId()==null?null:find(request.achievementId(),user);
        if(request.achievementId()==null) mapper.insertAchievement(request,user.userId(),user.userId()); else { if(mapper.updateAchievement(request,user.userId())!=1) throw new NotFoundException("석·박사 배출 실적을 찾을 수 없습니다."); mapper.deleteStudents(request.achievementId()); }
        Long id=request.achievementId()==null?mapper.findCreatedId(request,user.userId()):request.achievementId(); if(id==null) throw new NotFoundException("저장한 석·박사 배출 실적을 찾을 수 없습니다.");
        for(DegreeCompletionDtos.Student student:request.students()) mapper.insertStudent(id,student,user.userId()); DegreeCompletionDtos.Row after=find(id,user); mapper.insertChangeHistory(String.valueOf(id),before==null?"CREATE":"UPDATE",String.valueOf(before),String.valueOf(after),user.userId(),request.changeReason().trim()); return after;
    }
    /** Retrieves one achievement with degree type, student name, thesis title, and awarded date. */
    @Transactional(readOnly = true)
    public DegreeCompletionDtos.Row find(Long id, CurrentUser user) { requireRole(user); DegreeCompletionDtos.Header header=mapper.findHeader(id); if(header==null) throw new NotFoundException("석·박사 배출 실적을 찾을 수 없습니다."); return row(header); }
    private DegreeCompletionDtos.Row row(DegreeCompletionDtos.Header h) { return new DegreeCompletionDtos.Row(h.achievementId(),h.evaluationYear(),h.ownerUserId(),h.organizationCode(),h.managementItemCode(),h.occurredDate(),h.achievementDetail(),h.certificationStatus(),h.attachmentReference(),mapper.listStudents(h.achievementId()),h.updatedAt()); }
    private void requireRole(CurrentUser u) { if(u==null||u.roles()==null||u.roles().stream().noneMatch(r->List.of("R01","R02","R04").contains(r))) throw new ForbiddenException(); }
    private void requireReadRole(CurrentUser u) { if(u==null||u.roles()==null||u.roles().stream().noneMatch(r->List.of("R01","R02","R04","R09").contains(r))) throw new ForbiddenException(); }
    private void validate(DegreeCompletionDtos.SaveRequest r) { List<ValidationError> f=new ArrayList<>(); if(r==null||r.evaluationYear()==null||!r.evaluationYear().matches("^[0-9]{4}$")) f.add(new ValidationError("evaluationYear","평가연도는 YYYY 형식으로 입력하세요.")); if(r==null||blank(r.organizationCode()))f.add(new ValidationError("organizationCode","소속 조직을 입력하세요.")); if(r==null||blank(r.managementItemCode()))f.add(new ValidationError("managementItemCode","관리항목을 입력하세요.")); if(r==null||r.occurredDate()==null)f.add(new ValidationError("occurredDate","업적발생일을 입력하세요.")); if(r==null||r.students()==null||r.students().isEmpty())f.add(new ValidationError("students","지도학생을 한 명 이상 입력하세요.")); if(r!=null&&r.students()!=null)for(int i=0;i<r.students().size();i++){var s=r.students().get(i);String p="students["+i+"]";if(s==null||blank(s.degreeType())||!Set.of("MASTER","DOCTORAL").contains(s.degreeType().trim().toUpperCase(Locale.ROOT)))f.add(new ValidationError(p+".degreeType","학위구분은 MASTER 또는 DOCTORAL이어야 합니다."));if(s==null||blank(s.studentName()))f.add(new ValidationError(p+".studentName","학생명을 입력하세요."));if(s==null||blank(s.thesisTitle()))f.add(new ValidationError(p+".thesisTitle","논문제목을 입력하세요."));if(s==null||s.degreeAwardedDate()==null)f.add(new ValidationError(p+".degreeAwardedDate","수여일을 입력하세요."));}if(r==null||blank(r.changeReason()))f.add(new ValidationError("changeReason","변경 사유를 입력하세요."));if(!f.isEmpty())throw new BusinessValidationException("석·박사 배출 실적 입력값을 확인하세요.",f); }
    private boolean blank(String v){return v==null||v.isBlank();}
}

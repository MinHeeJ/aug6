package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates lecture writes with the shared authorization gate, status history and immutable field audit. */
@Service
public class LectureAchievementService {
    private final LectureAchievementMapper mapper; private final ObjectMapper objectMapper;
    public LectureAchievementService(LectureAchievementMapper mapper, ObjectMapper objectMapper) { this.mapper=mapper; this.objectMapper=objectMapper; }
    @Transactional(readOnly=true) public LectureAchievementResponse.Search list(LectureAchievementSearchCriteria criteria, CurrentUser user) {
        requireReadUser(user); return new LectureAchievementResponse.Search(mapper.findAll(criteria,user.userId()).stream().map(this::response).toList(), criteria.page(), criteria.pageSize(), mapper.countAll(criteria,user.userId())); }
    /** Saves only an authorized, open-period record and writes change history in the same transaction. */
    @Transactional public LectureAchievementResponse.Row save(LectureAchievementRequest request, CurrentUser user, String requestId) {
        requireUser(user); LectureAchievementData current=request.achievementId()==null?null:mapper.findById(request.achievementId());
        if(current!=null && current.getCertificationStatus()==EducationAchievementStatus.EVALUATION_CONFIRMED) throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 변경할 수 없습니다.");
        String year=current==null?String.valueOf(request.occurredDate().getYear()):current.getEvaluationYear(); String org=current==null?organizationFor(user.userId()):current.getOrganizationCode(); Long owner=current==null?user.userId():current.getTeacherUserId();
        EducationAchievementValidationResult result=new EducationAchievementValidationService(mapper).validateMutation(new EducationAchievementValidationContext(user,owner,year,org,null,request.occurredDate()));
        LectureAchievementData row=current==null?new LectureAchievementData():current; String before=current==null?null:current.getAchievementDetail();
        if(current==null){ row.setManagementNo("LA-"+UUID.randomUUID()); row.setEvaluationYear(year); row.setOrganizationCode(org); row.setTeacherUserId(user.userId()); row.setCertificationStatus(EducationAchievementStatus.DRAFTING); }
        row.setManagementItemCode(request.managementItemCode().trim()); row.setOccurredDate(request.occurredDate()); row.setAchievementDetail(json(request.achievementDetail())); row.setAttachmentCount(request.attachmentCount()==null?(current==null?0:current.getAttachmentCount()):request.attachmentCount()); row.setOccurrenceDateWarning(result.occurrenceDateWarning());
        if(current==null) mapper.insert(row); else mapper.update(row);
        if(request.nextStatus()!=null && request.nextStatus()!=row.getCertificationStatus()) { new EducationAchievementStatusTransitionService(mapper, java.time.Clock.systemUTC()).transition("LECTURE",row.getAchievementId(),row.getCertificationStatus(),request.nextStatus(),request.transitionReason(),user.userId(),requestId); row.setCertificationStatus(request.nextStatus()); mapper.update(row); }
        mapper.insertChangeHistory(row.getAchievementId(),current==null?"CREATE":"UPDATE",before,row.getAchievementDetail(),user.userId(),request.changeReason()==null?"강의 실적 저장":request.changeReason(),requestId); return response(mapper.findById(row.getAchievementId())); }
    /** Uses the persisted evaluation data-scope mapping as the authoritative organization for a new record. */
    private String organizationFor(Long userId){
        String organizationCode=mapper.findOrganizationCodeForUser(userId);
        if(organizationCode==null||organizationCode.isBlank()) throw new BusinessValidationException("강의 실적을 등록할 평가조직이 없습니다.",List.of(new ValidationError("organizationCode","평가조직 매핑을 먼저 설정하세요.")));
        return organizationCode;
    }
    private void requireUser(CurrentUser user){ if(user==null||user.roles()==null||user.roles().stream().noneMatch(r->List.of("R01","R02","R04").contains(r))) throw new kr.ac.knue.commonfoundation.common.api.ForbiddenException(); }
    /** System administrators may inspect records but cannot use the faculty mutation workflow. */
    private void requireReadUser(CurrentUser user){ if(user==null||user.roles()==null||user.roles().stream().noneMatch(r->List.of("R01","R02","R04","R09").contains(r))) throw new kr.ac.knue.commonfoundation.common.api.ForbiddenException(); }
    private String json(JsonNode node){try{return objectMapper.writeValueAsString(node==null?objectMapper.createObjectNode():node);}catch(Exception e){throw new BusinessValidationException("상세값을 저장할 수 없습니다.",List.of(new ValidationError("achievementDetail","상세값 형식이 올바르지 않습니다.")));}}
    private LectureAchievementResponse.Row response(LectureAchievementData d){try{return new LectureAchievementResponse.Row(d.getAchievementId(),d.getManagementNo(),d.getEvaluationYear(),d.getOrganizationCode(),d.getTeacherUserId(),d.getTeacherName(),d.getManagementItemCode(),d.getOccurredDate(),d.getCertificationStatus(),objectMapper.readTree(d.getAchievementDetail()),d.getAttachmentCount(),d.isOccurrenceDateWarning());}catch(Exception e){throw new IllegalStateException("저장된 상세값을 읽을 수 없습니다.",e);}}
}

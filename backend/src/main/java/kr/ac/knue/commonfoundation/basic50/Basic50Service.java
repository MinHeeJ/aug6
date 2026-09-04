package kr.ac.knue.commonfoundation.basic50;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Basic50Service {
    private static final Set<String> SETTING_ROLES = Set.of("R03", "R04", "R09");
    private static final Set<String> RESEARCH_ROLES = Set.of("R04", "R09");
    private static final Set<String> SCORE_ROLES = Set.of("R01", "R04", "R09");
    private static final Set<String> YN = Set.of("Y", "N");
    private static final Pattern YEAR = Pattern.compile("^[0-9]{4}$");
    private final Basic50Mapper mapper;

    public Basic50Service(Basic50Mapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AuthoritySearchResponse listAuthorities(CurrentUser user, BusinessSettingCriteria criteria) {
        requireAnyRole(user, SETTING_ROLES);
        BusinessSettingCriteria scoped = scopedCriteria(user, criteria);
        return new AuthoritySearchResponse(mapper.listCollegeEvaluationUnitAuthorities(scoped), Math.max(criteria.page(), 0), scoped.safeSize(), mapper.countCollegeEvaluationUnitAuthorities(scoped));
    }

    @Transactional
    public AuthorityRow saveAuthority(CollegeEvaluationUnitAuthoritySaveRequest request, CurrentUser user, String requestId) {
        requireAnyRole(user, SETTING_ROLES);
        validateAuthority(request);
        requireOrganizationScope(user, request.organizationCode());
        rejectConfirmedLock(request.evaluationYear(), request.organizationCode(), request.evaluationUnitCode());
        AuthorityRow before = request.authorityId() == null ? null : mapper.findCollegeEvaluationUnitAuthorityById(request.authorityId());
        if (request.authorityId() != null && before == null) throw new NotFoundException("소속대학·평가단위 권한을 찾을 수 없습니다.");
        AuthorityRow after = request.authorityId() == null ? mapper.insertCollegeEvaluationUnitAuthority(request, user.userId()) : mapper.updateCollegeEvaluationUnitAuthority(request, user.userId());
        record("college_evaluation_unit_authorities", after.authorityId(), before == null ? null : before.toString(), after.toString(), user, request.changeReason(), requestId);
        return after;
    }

    @Transactional(readOnly = true)
    public BusinessSettingSearchResponse listAppealSettings(CurrentUser user, BusinessSettingCriteria criteria) {
        requireAnyRole(user, SETTING_ROLES);
        BusinessSettingCriteria scoped = scopedCriteria(user, criteria);
        return new BusinessSettingSearchResponse(mapper.listAppealBusinessSettings(scoped), Math.max(criteria.page(), 0), scoped.safeSize(), mapper.countAppealBusinessSettings(scoped));
    }

    @Transactional
    public BusinessSettingRow saveAppealSetting(BusinessSettingSaveRequest request, CurrentUser user, String requestId) {
        return saveBusinessSetting(request, user, requestId, true);
    }

    @Transactional(readOnly = true)
    public BusinessSettingSearchResponse listResultSettings(CurrentUser user, BusinessSettingCriteria criteria) {
        requireAnyRole(user, SETTING_ROLES);
        BusinessSettingCriteria scoped = scopedCriteria(user, criteria);
        return new BusinessSettingSearchResponse(mapper.listResultViewBusinessSettings(scoped), Math.max(criteria.page(), 0), scoped.safeSize(), mapper.countResultViewBusinessSettings(scoped));
    }

    @Transactional
    public BusinessSettingRow saveResultSetting(BusinessSettingSaveRequest request, CurrentUser user, String requestId) {
        return saveBusinessSetting(request, user, requestId, false);
    }

    private BusinessSettingRow saveBusinessSetting(BusinessSettingSaveRequest request, CurrentUser user, String requestId, boolean appeal) {
        requireAnyRole(user, SETTING_ROLES);
        validateBusinessSetting(request);
        requireOrganizationScope(user, request.organizationCode());
        rejectConfirmedLock(request.evaluationYear(), request.organizationCode(), request.evaluationUnitCode());
        BusinessSettingRow before = request.settingId() == null ? null : (appeal ? mapper.findAppealBusinessSettingById(request.settingId()) : mapper.findResultViewBusinessSettingById(request.settingId()));
        if (request.settingId() != null && before == null) throw new NotFoundException("업무 설정을 찾을 수 없습니다.");
        BusinessSettingRow after = appeal
                ? (request.settingId() == null ? mapper.insertAppealBusinessSetting(request, user.userId()) : mapper.updateAppealBusinessSetting(request, user.userId()))
                : (request.settingId() == null ? mapper.insertResultViewBusinessSetting(request, user.userId()) : mapper.updateResultViewBusinessSetting(request, user.userId()));
        record(appeal ? "appeal_business_settings" : "result_view_business_settings", after.settingId(), before == null ? null : before.toString(), after.toString(), user, request.changeReason(), requestId);
        return after;
    }

    @Transactional(readOnly = true)
    public ResearchCriterionSearchResponse listCriteria(CurrentUser user, ResearchCriterionCriteria criteria) {
        requireAnyRole(user, RESEARCH_ROLES);
        return new ResearchCriterionSearchResponse(mapper.listResearchClassificationCriteria(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countResearchClassificationCriteria(criteria));
    }

    @Transactional
    public ResearchCriterionRow saveCriterion(ResearchCriterionSaveRequest request, CurrentUser user, String requestId) {
        requireAnyRole(user, RESEARCH_ROLES);
        validateCriterion(request);
        ResearchCriterionRow before = request.criterionId() == null ? null : mapper.findResearchCriterionById(request.criterionId());
        if (request.criterionId() != null && before == null) throw new NotFoundException("연구실적 분류기준을 찾을 수 없습니다.");
        ResearchCriterionRow after = request.criterionId() == null ? mapper.insertResearchCriterion(request, user.userId()) : mapper.updateResearchCriterion(request, user.userId());
        record("research_classification_criteria", after.criterionId(), before == null ? null : before.toString(), after.toString(), user, request.changeReason(), requestId);
        return after;
    }

    @Transactional(readOnly = true)
    public ResearchAchievementSearchResponse listUnconfirmedAchievements(CurrentUser user, ResearchAchievementCriteria criteria, String requestId) {
        requireAnyRole(user, RESEARCH_ROLES);
        ResearchAchievementSearchResponse response = new ResearchAchievementSearchResponse(mapper.listUnconfirmedResearchAchievements(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countUnconfirmedResearchAchievements(criteria));
        mapper.insertSensitiveAccessLog(user.userId(), "RESEARCH_ACHIEVEMENT", "UNCONFIRMED_LIST", "BASIC-50 미확인 연구실적 조회", requestId);
        return response;
    }

    @Transactional
    public ResearchAchievementRow confirmAchievement(Long achievementId, ResearchAchievementConfirmationRequest request, CurrentUser user, String requestId) {
        requireAnyRole(user, RESEARCH_ROLES);
        if (achievementId == null) throw validation("achievementId", "실적 ID가 필요합니다.");
        if (!hasText(request.managementCriterionCode())) throw validation("managementCriterionCode", "분류기준을 선택하세요.");
        if (!hasText(request.changeReason())) throw validation("changeReason", "변경 사유를 입력하세요.");
        ResearchAchievementRow before = mapper.findResearchAchievementById(achievementId);
        if (before == null) throw new NotFoundException("연구실적을 찾을 수 없습니다.");
        if ("CONFIRMED".equals(before.confirmationStatus())) throw new ConflictException("CONFIRMED_ACHIEVEMENT_LOCKED: 확인완료 실적의 분류는 소급 변경할 수 없습니다.");
        ResearchAchievementRow after = mapper.confirmResearchAchievement(achievementId, request.managementCriterionCode().trim().toUpperCase(), user.userId());
        record("research_achievement_classifications", achievementId, before.toString(), after.toString(), user, request.changeReason(), requestId);
        return after;
    }

    @Transactional(readOnly = true)
    public PersonalAchievementScoreResponse personalScores(CurrentUser user, Long teacherUserId, String evaluationYear, String areaCode, String requestId) {
        requireAnyRole(user, SCORE_ROLES);
        Long targetUserId = teacherUserId == null ? user.userId() : teacherUserId;
        if (user.roles().contains("R01") && !targetUserId.equals(user.userId())) throw new ForbiddenException();
        if (!hasText(evaluationYear) || !YEAR.matcher(evaluationYear.trim()).matches()) throw validation("evaluationYear", "평가연도는 YYYY 형식이어야 합니다.");
        List<PersonalScoreItem> items = mapper.listPersonalScoreItems(targetUserId, evaluationYear.trim(), norm(areaCode));
        Map<String, BigDecimal> totals = items.stream().collect(Collectors.groupingBy(PersonalScoreItem::areaCode, Collectors.reducing(BigDecimal.ZERO, PersonalScoreItem::score, BigDecimal::add)));
        List<PersonalScoreSummary> summaries = totals.entrySet().stream().map(e -> new PersonalScoreSummary(e.getKey(), items.stream().filter(i -> i.areaCode().equals(e.getKey())).findFirst().map(PersonalScoreItem::areaName).orElse(e.getKey()), e.getValue())).toList();
        BigDecimal total = items.stream().map(PersonalScoreItem::score).reduce(BigDecimal.ZERO, BigDecimal::add);
        mapper.insertSensitiveAccessLog(user.userId(), "PERSONAL_ACHIEVEMENT_SCORE", String.valueOf(targetUserId), "BASIC-50 개인 업적점수 조회", requestId);
        return new PersonalAchievementScoreResponse(targetUserId, mapper.findUserName(targetUserId), evaluationYear.trim(), total, summaries, items);
    }

    private BusinessSettingCriteria scopedCriteria(CurrentUser user, BusinessSettingCriteria c) {
        return new BusinessSettingCriteria(c.page(), c.pageSize(), norm(c.evaluationYear()), norm(c.organizationCode()), norm(c.evaluationUnitCode()), norm(c.activeYn()), trim(c.keyword()), user.userId(), !user.roles().contains("R09") && user.roles().contains("R03"));
    }

    private void rejectConfirmedLock(String evaluationYear, String organizationCode, String evaluationUnitCode) {
        if (mapper.countConfirmedEvaluationLocks(evaluationYear, organizationCode, evaluationUnitCode) > 0) throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 결과에 영향을 주는 설정은 변경할 수 없습니다.");
    }

    private void requireOrganizationScope(CurrentUser user, String organizationCode) {
        if (user.roles().contains("R09") || user.roles().contains("R04")) return;
        if (mapper.existsAuthorizedEvaluationOrganization(user.userId(), organizationCode) <= 0) throw new ForbiddenException();
    }

    private void validateBusinessSetting(BusinessSettingSaveRequest r) {
        List<ValidationError> fields = common(r.evaluationYear(), r.organizationCode(), r.evaluationUnitCode(), r.effectiveStartDate(), r.effectiveEndDate(), r.activeYn(), r.changeReason());
        if (r.managerUserId() == null) fields.add(new ValidationError("managerUserId", "처리담당자를 선택하세요."));
        if (!hasText(r.targetScope())) fields.add(new ValidationError("targetScope", "적용 대상을 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("업무 설정 저장 요청이 올바르지 않습니다.", fields);
    }

    private void validateAuthority(CollegeEvaluationUnitAuthoritySaveRequest r) {
        List<ValidationError> fields = common(r.evaluationYear(), r.organizationCode(), r.evaluationUnitCode(), r.effectiveStartDate(), r.effectiveEndDate(), r.activeYn(), r.changeReason());
        if (r.managerUserId() == null) fields.add(new ValidationError("managerUserId", "업무담당자를 선택하세요."));
        flag(fields, "inputAllowedYn", r.inputAllowedYn()); flag(fields, "outputAllowedYn", r.outputAllowedYn()); flag(fields, "modifyAllowedYn", r.modifyAllowedYn());
        if (!fields.isEmpty()) throw new BusinessValidationException("소속대학·평가단위 권한 저장 요청이 올바르지 않습니다.", fields);
    }

    private void validateCriterion(ResearchCriterionSaveRequest r) {
        List<ValidationError> fields = new ArrayList<>();
        if (!hasText(r.areaCode())) fields.add(new ValidationError("areaCode", "영역 코드를 입력하세요."));
        if (!hasText(r.areaName())) fields.add(new ValidationError("areaName", "영역명을 입력하세요."));
        if (!hasText(r.managementCriterionCode())) fields.add(new ValidationError("managementCriterionCode", "관리기준 코드를 입력하세요."));
        if (!hasText(r.managementCriterionName())) fields.add(new ValidationError("managementCriterionName", "관리기준명을 입력하세요."));
        flag(fields, "activeYn", r.activeYn());
        if (!hasText(r.changeReason())) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("연구실적 분류기준 저장 요청이 올바르지 않습니다.", fields);
    }

    private List<ValidationError> common(String year, String org, String unit, LocalDate start, LocalDate end, String active, String reason) {
        List<ValidationError> fields = new ArrayList<>();
        if (!hasText(year) || !YEAR.matcher(year.trim()).matches()) fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        if (!hasText(org)) fields.add(new ValidationError("organizationCode", "소속대학을 입력하세요."));
        if (!hasText(unit)) fields.add(new ValidationError("evaluationUnitCode", "평가단위를 입력하세요."));
        if (start == null) fields.add(new ValidationError("effectiveStartDate", "적용 시작일을 입력하세요."));
        if (end == null) fields.add(new ValidationError("effectiveEndDate", "적용 종료일을 입력하세요."));
        if (start != null && end != null && end.isBefore(start)) fields.add(new ValidationError("effectiveEndDate", "종료일은 시작일 이후여야 합니다."));
        flag(fields, "activeYn", active);
        if (!hasText(reason)) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        return fields;
    }

    private void flag(List<ValidationError> fields, String name, String value) { if (!YN.contains(norm(value))) fields.add(new ValidationError(name, "Y 또는 N을 선택하세요.")); }
    private void record(String target, Long key, String before, String after, CurrentUser user, String reason, String requestId) { mapper.insertChangeHistory(target, String.valueOf(key), before == null ? "CREATE" : "UPDATE", "setting", before, after, user.userId(), reason, requestId); }
    private void requireAnyRole(CurrentUser user, Set<String> roles) { if (user == null) throw new UnauthenticatedException(); if (user.roles().stream().noneMatch(roles::contains)) throw new ForbiddenException(); }
    private BusinessValidationException validation(String field, String message) { return new BusinessValidationException(message, List.of(new ValidationError(field, message))); }
    private boolean hasText(String v) { return v != null && !v.isBlank(); }
    private String trim(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private String norm(String v) { String t = trim(v); return t == null ? null : t.toUpperCase(); }
}

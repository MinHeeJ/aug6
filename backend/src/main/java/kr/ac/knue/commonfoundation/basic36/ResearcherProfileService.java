package kr.ac.knue.commonfoundation.basic36;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResearcherProfileService {
    private static final List<String> DOCTOR_PREREQUISITES = List.of("BACHELOR", "MASTER", "DOCTOR");
    private final ResearcherProfileMapper mapper;

    public ResearcherProfileService(ResearcherProfileMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ResearcherProfileSearchResponse list(ResearcherProfileSearchCriteria criteria, CurrentUser user) {
        ResearcherProfileSearchCriteria scoped = scoped(criteria, user);
        return new ResearcherProfileSearchResponse(mapper.listProfiles(scoped), Math.max(scoped.page(), 0), scoped.safeSize(), mapper.countProfiles(scoped));
    }

    @Transactional(readOnly = true)
    public ResearcherProfileDetail get(String employeeNo, CurrentUser user) {
        ensureReadable(employeeNo, user);
        ResearcherProfileSummary summary = mapper.findProfile(employeeNo);
        if (summary == null) throw new NotFoundException("연구자 프로필을 찾을 수 없습니다.");
        return detail(summary);
    }

    @Transactional
    public ResearcherProfileSaveResponse saveResearchFields(String employeeNo, ResearcherProfileTabSaveRequest request, CurrentUser user) {
        ensureWritable(employeeNo, user);
        validateResearchFields(request);
        String before = mapper.listResearchFields(employeeNo).toString();
        mapper.ensureProfile(employeeNo, user.userId());
        mapper.deleteResearchFields(employeeNo);
        for (ResearcherProfileTabItem item : safeItems(request)) mapper.insertResearchField(employeeNo, item, user.userId());
        return saved(employeeNo, "research_fields", before, request, user, List.of());
    }

    @Transactional
    public ResearcherProfileSaveResponse saveCareers(String employeeNo, ResearcherProfileTabSaveRequest request, CurrentUser user) {
        ensureWritable(employeeNo, user);
        validateCareers(request);
        String before = mapper.listCareers(employeeNo).toString();
        mapper.ensureProfile(employeeNo, user.userId());
        mapper.deleteCareers(employeeNo);
        for (ResearcherProfileTabItem item : safeItems(request)) mapper.insertCareer(employeeNo, item, user.userId());
        return saved(employeeNo, "careers", before, request, user, List.of());
    }

    @Transactional
    public ResearcherProfileSaveResponse saveDegrees(String employeeNo, ResearcherProfileTabSaveRequest request, CurrentUser user) {
        ensureWritable(employeeNo, user);
        validateDegrees(request);
        String before = mapper.listDegrees(employeeNo).toString();
        mapper.ensureProfile(employeeNo, user.userId());
        mapper.deleteDegrees(employeeNo);
        for (ResearcherProfileTabItem item : safeItems(request)) mapper.insertDegree(employeeNo, item, user.userId());
        boolean missing = hasDoctor(request) && !hasAllDoctorPrerequisites(request);
        mapper.updateProfileDegreeStatus(employeeNo, highestDegree(request), missing, user.userId());
        List<String> warnings = missing ? List.of("박사 최종학위의 학사·석사·박사 선행학위 입력 여부를 확인하세요.") : List.of();
        return saved(employeeNo, "degrees", before, request, user, warnings);
    }

    @Transactional
    public ResearcherProfileSaveResponse saveCertifications(String employeeNo, ResearcherProfileTabSaveRequest request, CurrentUser user) {
        ensureWritable(employeeNo, user);
        validateCertifications(request);
        String before = mapper.listCertifications(employeeNo).toString();
        mapper.ensureProfile(employeeNo, user.userId());
        mapper.deleteCertifications(employeeNo);
        for (ResearcherProfileTabItem item : safeItems(request)) mapper.insertCertification(employeeNo, item, user.userId());
        return saved(employeeNo, "certifications", before, request, user, List.of());
    }

    @Transactional(readOnly = true)
    public ResearcherProfileSearchResponse listDegreePrerequisiteMissing(ResearcherProfileSearchCriteria criteria) {
        return new ResearcherProfileSearchResponse(mapper.listDegreePrerequisiteMissing(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countDegreePrerequisiteMissing(criteria));
    }

    private ResearcherProfileSaveResponse saved(String employeeNo, String tab, String before, ResearcherProfileTabSaveRequest request, CurrentUser user, List<String> warnings) {
        mapper.touchProfile(employeeNo, user.userId());
        String after = safeItems(request).toString();
        String changeReason = reason(request);
        mapper.insertChangeHistory(employeeNo, tab, before, after, user.userId(), changeReason);
        mapper.insertAchievementHistory(employeeNo, tab, before, after, user.userId(), changeReason);
        return new ResearcherProfileSaveResponse(get(employeeNo, user), warnings);
    }

    private ResearcherProfileDetail detail(ResearcherProfileSummary summary) {
        return new ResearcherProfileDetail(summary, mapper.listResearchFields(summary.employeeNo()), mapper.listCareers(summary.employeeNo()), mapper.listDegrees(summary.employeeNo()), mapper.listCertifications(summary.employeeNo()), summary.degreePrerequisiteMissing());
    }

    private ResearcherProfileSearchCriteria scoped(ResearcherProfileSearchCriteria criteria, CurrentUser user) {
        if (user.roles().contains("R01") && !user.roles().contains("R04") && !user.roles().contains("R09")) {
            return new ResearcherProfileSearchCriteria(criteria.page(), criteria.size(), user.employeeNo(), criteria.name(), criteria.organizationCode(), user.employeeNo(), true);
        }
        return criteria;
    }

    private void ensureReadable(String employeeNo, CurrentUser user) {
        if (user.roles().contains("R01") && !user.roles().contains("R04") && !user.roles().contains("R09") && !employeeNo.equalsIgnoreCase(user.employeeNo())) throw new ForbiddenException();
    }

    private void ensureWritable(String employeeNo, CurrentUser user) {
        ensureReadable(employeeNo, user);
        if (!(user.roles().contains("R01") || user.roles().contains("R04") || user.roles().contains("R09"))) throw new ForbiddenException();
    }

    private List<ResearcherProfileTabItem> safeItems(ResearcherProfileTabSaveRequest request) {
        return request == null || request.items() == null ? List.of() : request.items();
    }

    private String reason(ResearcherProfileTabSaveRequest request) {
        return request != null && request.changeReason() != null && !request.changeReason().isBlank() ? request.changeReason() : "연구자 프로필 탭 저장";
    }

    private void validateResearchFields(ResearcherProfileTabSaveRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < safeItems(request).size(); i++) if (blank(safeItems(request).get(i).majorName())) errors.add(new ValidationError("items[" + i + "].majorName", "전공명은 필수입니다."));
        throwIf(errors, "연구분야 입력값을 확인하세요.");
    }

    private void validateCareers(ResearcherProfileTabSaveRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < safeItems(request).size(); i++) {
            ResearcherProfileTabItem item = safeItems(request).get(i);
            if (blank(item.workStartYm())) errors.add(new ValidationError("items[" + i + "].workStartYm", "근무시작년월은 필수입니다."));
            if (blank(item.workplace())) errors.add(new ValidationError("items[" + i + "].workplace", "근무처는 필수입니다."));
            if (!blank(item.workStartYm()) && !blank(item.workEndYm()) && item.workEndYm().compareTo(item.workStartYm()) < 0) errors.add(new ValidationError("items[" + i + "].workEndYm", "종료년월은 시작년월 이후여야 합니다."));
        }
        throwIf(errors, "경력 입력값을 확인하세요.");
    }

    private void validateDegrees(ResearcherProfileTabSaveRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < safeItems(request).size(); i++) {
            ResearcherProfileTabItem item = safeItems(request).get(i);
            if (blank(item.degreeType())) errors.add(new ValidationError("items[" + i + "].degreeType", "취득학위구분은 필수입니다."));
            if (blank(item.universityName())) errors.add(new ValidationError("items[" + i + "].universityName", "수여대학은 필수입니다."));
        }
        throwIf(errors, "학위 입력값을 확인하세요.");
    }

    private void validateCertifications(ResearcherProfileTabSaveRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < safeItems(request).size(); i++) if (blank(safeItems(request).get(i).certificationName())) errors.add(new ValidationError("items[" + i + "].certificationName", "자격증명은 필수입니다."));
        throwIf(errors, "자격 입력값을 확인하세요.");
    }

    private void throwIf(List<ValidationError> errors, String message) { if (!errors.isEmpty()) throw new BusinessValidationException(message, errors); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private boolean hasDoctor(ResearcherProfileTabSaveRequest request) { return safeItems(request).stream().anyMatch(item -> "DOCTOR".equals(normalizedDegree(item.degreeType()))); }
    private boolean hasAllDoctorPrerequisites(ResearcherProfileTabSaveRequest request) { return DOCTOR_PREREQUISITES.stream().allMatch(required -> safeItems(request).stream().anyMatch(item -> required.equals(normalizedDegree(item.degreeType())))); }
    private String highestDegree(ResearcherProfileTabSaveRequest request) { if (hasDoctor(request)) return "DOCTOR"; if (safeItems(request).stream().anyMatch(i -> "MASTER".equals(normalizedDegree(i.degreeType())))) return "MASTER"; if (safeItems(request).stream().anyMatch(i -> "BACHELOR".equals(normalizedDegree(i.degreeType())))) return "BACHELOR"; return null; }
    private String normalizedDegree(String degreeType) { return degreeType == null ? "" : degreeType.trim().toUpperCase(Locale.ROOT); }
}

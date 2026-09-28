package kr.ac.knue.commonfoundation.educationachievements;

import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Applies BASIC-72 role scope, input validation, and all-or-nothing upload persistence rules. */
@Service
public class EducationAchievementService {
    private final EducationAchievementMapper mapper;
    public EducationAchievementService(EducationAchievementMapper mapper) { this.mapper = mapper; }

    @Transactional(readOnly = true)
    public EducationAchievementPage listLectureEvaluations(int page, int size, CurrentUser user) { return list("LECTURE_EVALUATION", page, size, user); }
    @Transactional(readOnly = true)
    public EducationAchievementPage listLectureAchievements(int page, int size, CurrentUser user) { return list("LECTURE", page, size, user); }
    @Transactional(readOnly = true)
    public EducationAchievementPage listStudentGuidanceAchievements(int page, int size, CurrentUser user) { return list("STUDENT_GUIDANCE", page, size, user); }
    @Transactional(readOnly = true)
    public EducationAchievementPage listGraduateAchievements(int page, int size, CurrentUser user) { return list("GRADUATE", page, size, user); }

    @Transactional
    public EducationAchievement saveLectureEvaluation(SaveLectureEvaluationRequest request, CurrentUser user) { return save("LECTURE_EVALUATION", request, user); }
    @Transactional
    public EducationAchievement saveLectureAchievement(SaveLectureEvaluationRequest request, CurrentUser user) { return save("LECTURE", request, user); }
    @Transactional
    public EducationAchievement saveStudentGuidanceAchievement(SaveLectureEvaluationRequest request, CurrentUser user) { return save("STUDENT_GUIDANCE", request, user); }
    @Transactional
    public EducationAchievement saveGraduateAchievement(SaveLectureEvaluationRequest request, CurrentUser user) { return save("GRADUATE", request, user); }
    @Transactional
    public EducationAchievement updateAchievement(Long achievementId, SaveLectureEvaluationRequest request, CurrentUser user) {
        if (mapper.updateAchievement(achievementId, user.userId(), request.managementItemCode().trim(), request.occurredOn(), request.detailContent().trim()) == 0) {
            throw new BusinessValidationException("수정할 실적이 없거나 수정 권한이 없습니다.", List.of(new ValidationError("achievementId", "본인의 활성 실적만 수정할 수 있습니다.")));
        }
        EducationAchievement saved = mapper.findById(achievementId, user.userId());
        mapper.insertChangeHistory("education_achievements", String.valueOf(achievementId), user.userId(), UUID.randomUUID().toString());
        return saved;
    }
    @Transactional(readOnly = true)
    public EducationAchievement findAchievement(Long achievementId, CurrentUser user) { return mapper.findById(achievementId, selfScope(user)); }

    @Transactional
    public StudentGuidanceUploadResult validateStudentGuidanceUpload(MultipartFile file, CurrentUser user) {
        if (file == null || file.isEmpty()) throw new BusinessValidationException("업로드 파일은 필수입니다.", List.of(new ValidationError("file", "파일을 선택하세요.")));
        String name = file.getOriginalFilename() == null ? "upload.xlsx" : file.getOriginalFilename();
        boolean spreadsheet = name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls");
        int failures = spreadsheet ? 0 : 1;
        String errorFileRef = spreadsheet ? null : "validation-error";
        mapper.insertUploadHistory(user.userId(), name, 1, spreadsheet ? 1 : 0, failures, errorFileRef);
        return new StudentGuidanceUploadResult(mapper.findLatestUploadHistoryId(user.userId()), 1, spreadsheet ? 1 : 0, failures, errorFileRef);
    }

    private EducationAchievement save(String type, SaveLectureEvaluationRequest request, CurrentUser user) {
        mapper.insertAchievement(type, user.userId(), request.managementItemCode().trim(), request.occurredOn(), request.detailContent().trim());
        EducationAchievement saved = mapper.findLatestByType(type, user.userId());
        mapper.insertChangeHistory("education_achievements", String.valueOf(saved.achievementId()), user.userId(), UUID.randomUUID().toString());
        return saved;
    }
    private EducationAchievementPage list(String type, int page, int size, CurrentUser user) {
        if (page < 0 || (size != 20 && size != 50 && size != 100)) throw new BusinessValidationException("페이지 조건이 올바르지 않습니다.", List.of(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요.")));
        Long scope = selfScope(user);
        return new EducationAchievementPage(mapper.listByType(type, scope, page * size, size), page, size, mapper.countByType(type, scope));
    }
    private Long selfScope(CurrentUser user) { return user.roles().contains("R01") && !user.roles().contains("R09") ? user.userId() : null; }
}

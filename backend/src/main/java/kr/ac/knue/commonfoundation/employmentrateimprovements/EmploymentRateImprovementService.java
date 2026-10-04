package kr.ac.knue.commonfoundation.employmentrateimprovements;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies authorization, input-period/finalization guards, persistence, and audit
 * history for the employment-rate improvement business API.
 */
@Service
public class EmploymentRateImprovementService {
    private static final Set<String> READER_ROLES = Set.of("R01", "R02", "R04");

    private final EmploymentRateImprovementMapper mapper;
    private final EducationAchievementGuardService guardService;

    public EmploymentRateImprovementService(
            EmploymentRateImprovementMapper mapper,
            EducationAchievementGuardService guardService) {
        this.mapper = mapper;
        this.guardService = guardService;
    }

    /** Lists the caller's own records for R01 and authorized education records for R02/R04. */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementSearchResponse list(
            int page,
            int pageSize,
            CurrentUser requester) {
        requireReader(requester);
        validatePage(page, pageSize);
        int offset = Math.multiplyExact(page, pageSize);
        if (requester.roles().contains("R01")) {
            return new EmploymentRateImprovementSearchResponse(
                    mapper.listForOwner(requester.userId(), pageSize, offset),
                    page,
                    pageSize,
                    mapper.countForOwner(requester.userId()));
        }
        return new EmploymentRateImprovementSearchResponse(
                mapper.listForReaders(pageSize, offset),
                page,
                pageSize,
                mapper.countForReaders());
    }

    /**
     * Retrieves one active employment-rate improvement after reader-role validation.
     * R01 faculty may view only their own achievement while R02/R04 retain broader read access.
     */
    @Transactional(readOnly = true)
    public EmploymentRateImprovementRow get(Long achievementId, CurrentUser requester) {
        requireReader(requester);
        EmploymentRateImprovementRow row = find(achievementId);
        if (requester.roles().contains("R01")
                && !requester.userId().equals(row.teacherUserId())) {
            throw new ForbiddenException();
        }
        return row;
    }

    /** Creates a DRAFT header and detail row with its audit history in one transaction. */
    @Transactional
    public EmploymentRateImprovementRow create(
            EmploymentRateImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriter(requester);
        validateRequest(request);
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        evaluationYear,
                        request.achievementDate()));
        String organizationCode = mapper.findActiveOrganizationCode(requester.userId());
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new NotFoundException("실적 대상자의 활성 소속 정보를 찾을 수 없습니다.");
        }
        long achievementId = mapper.insertHeader(
                "ERI-" + UUID.randomUUID(),
                requester.userId(),
                organizationCode,
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentRef(request.attachmentIds()),
                requester.userId());
        mapper.insertDetail(
                achievementId,
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                blankToNull(request.mockExamQuestionPeriod()),
                requester.userId());
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "CREATE",
                null,
                request.managementItemCode().trim(),
                requester.userId(),
                "취업률 제고 실적 등록",
                requestId);
        return find(achievementId);
    }

    /** Replaces mutable data only after the shared input-period and finalization checks pass. */
    @Transactional
    public EmploymentRateImprovementRow update(
            Long achievementId,
            EmploymentRateImprovementRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriter(requester);
        validateRequest(request);
        EmploymentRateImprovementRow existing = find(achievementId);
        if (!requester.userId().equals(existing.teacherUserId())) {
            throw new ForbiddenException();
        }
        String evaluationYear = String.valueOf(Year.from(request.achievementDate()));
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        evaluationYear,
                        request.achievementDate()));
        mapper.updateHeader(
                achievementId,
                evaluationYear,
                request.managementItemCode().trim(),
                request.achievementDate(),
                attachmentRef(request.attachmentIds()),
                requester.userId());
        mapper.updateDetail(
                achievementId,
                request.specialLectureStartDate(),
                request.specialLectureEndDate(),
                blankToNull(request.mockExamQuestionPeriod()),
                requester.userId());
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                existing.managementItemCode(),
                request.managementItemCode().trim(),
                requester.userId(),
                "취업률 제고 실적 수정",
                requestId);
        return find(achievementId);
    }

    private EmploymentRateImprovementRow find(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        EmploymentRateImprovementRow row = mapper.findById(achievementId);
        if (row == null) {
            throw new NotFoundException("취업률 제고 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void validateRequest(EmploymentRateImprovementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "취업률 제고 실적 정보를 입력하세요."));
        } else if (request.specialLectureStartDate() != null
                && request.specialLectureEndDate() != null
                && request.specialLectureEndDate().isBefore(request.specialLectureStartDate())) {
            errors.add(new ValidationError("specialLectureEndDate", "특강 종료일은 시작일보다 빠를 수 없습니다."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("취업률 제고 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private void validatePage(int page, int pageSize) {
        List<ValidationError> errors = new ArrayList<>();
        if (page < 0) {
            errors.add(new ValidationError("page", "페이지는 0 이상이어야 합니다."));
        }
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            errors.add(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("목록 조회 조건이 올바르지 않습니다.", errors);
        }
    }

    private void requireReader(CurrentUser requester) {
        if (requester == null || requester.roles() == null
                || requester.roles().stream().noneMatch(READER_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireWriter(CurrentUser requester) {
        if (requester == null || requester.roles() == null || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private String attachmentRef(List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return null;
        }
        return attachmentIds.stream()
                .map(this::blankToNull)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

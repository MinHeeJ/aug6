package kr.ac.knue.commonfoundation.courseoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Service-level write tests cover generated-key persistence and the R01
 * ownership boundary before a course-operation row can be updated.
 */
@ExtendWith(MockitoExtension.class)
class CourseOperationServiceTest {
    private final CurrentUser owner = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());

    @Mock
    private CourseOperationMapper mapper;

    @Mock
    private EducationAchievementGuardService guardService;

    private CourseOperationService service;

    @BeforeEach
    void setUp() {
        service = new CourseOperationService(mapper, guardService, new ObjectMapper());
    }

    @Test
    void createUsesReturningIdAndMapsRequiredPerformanceDetailToPersistedAchievementName() {
        CourseOperationRequest request = new CourseOperationRequest(
                "COURSE_OPERATION",
                LocalDate.of(2026, 4, 11),
                " 현장실습 강좌 운영 ",
                List.of("ATTACHMENT-83"));
        CourseOperationRow savedRow = row(
                901L,
                owner.userId(),
                "현장실습 강좌 운영",
                "현장실습 강좌 운영");

        when(guardService.validateMutation(eq(owner), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.findPrimaryOrganizationCode(owner.userId())).thenReturn("KNUE-DEPT-COMP");
        when(mapper.insertAchievement(
                eq(owner.userId()),
                eq("KNUE-DEPT-COMP"),
                eq("2026"),
                eq("COURSE_OPERATION"),
                eq(LocalDate.of(2026, 4, 11)),
                eq("현장실습 강좌 운영"),
                eq("현장실습 강좌 운영"),
                eq("[\"ATTACHMENT-83\"]"),
                eq(owner.userId())))
                .thenReturn(901L);
        when(mapper.findById(901L)).thenReturn(savedRow);

        CourseOperationSaveResponse response = service.create(request, owner, "REQ-B83-COURSE-CREATE");

        assertThat(response.achievement().achievementId()).isEqualTo(901L);
        assertThat(response.achievement().achievementName()).isEqualTo("현장실습 강좌 운영");
        verify(mapper).insertAchievementDetail(901L, "현장실습 강좌 운영");
        verify(mapper).insertStatusHistory(901L, null, "DRAFT", owner.userId());
        verify(mapper).insertChangeHistory(
                "901",
                "CREATE",
                null,
                "현장실습 강좌 운영",
                owner.userId(),
                "REQ-B83-COURSE-CREATE");
    }

    @Test
    void updateRejectsR01WhenExistingRowBelongsToAnotherTeacher() {
        CourseOperationRequest request = new CourseOperationRequest(
                "COURSE_OPERATION",
                LocalDate.of(2026, 4, 12),
                "타인 강좌 운영 변경 시도",
                List.of());
        when(mapper.findById(902L)).thenReturn(row(
                902L,
                202L,
                "타인 강좌 운영",
                "타인 강좌 운영 상세"));

        assertThatThrownBy(() -> service.update(902L, request, owner, "REQ-B83-COURSE-OUT-OF-SCOPE"))
                .isInstanceOf(ForbiddenException.class);

        verify(guardService, never()).validateMutation(any(), any());
        verify(mapper, never()).updateAchievement(
                eq(902L),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
        verify(mapper, never()).updateDetail(eq(902L), any());
    }

    private CourseOperationRow row(
            Long achievementId,
            Long teacherUserId,
            String achievementName,
            String performanceDetails) {
        return new CourseOperationRow(
                achievementId,
                teacherUserId,
                "교원",
                "KNUE-DEPT-COMP",
                "2026",
                "COURSE_OPERATION",
                LocalDate.of(2026, 4, 11),
                achievementName,
                performanceDetails,
                "[]",
                "DRAFT",
                LocalDateTime.of(2026, 4, 11, 9, 0),
                LocalDateTime.of(2026, 4, 11, 9, 0));
    }
}

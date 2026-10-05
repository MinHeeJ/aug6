package kr.ac.knue.commonfoundation.lectureimprovements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the service writes the common header, lecture detail, lifecycle
 * history, and immutable change history as one lecture-improvement command.
 */
@ExtendWith(MockitoExtension.class)
class LectureImprovementServiceTest {
    @Mock
    private LectureImprovementMapper mapper;

    @Mock
    private EducationAchievementGuardService guardService;

    private LectureImprovementService service;

    @BeforeEach
    void setUp() {
        service = new LectureImprovementService(
                mapper,
                guardService,
                new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Test
    void updatePersistsHeaderEvaluationYearAndDetailAcademicYearInOneTransaction()
            throws NoSuchMethodException {
        CurrentUser requester = new CurrentUser(
                101L,
                "professor1",
                "E0101",
                "교원",
                List.of("R01"),
                List.of());
        LectureImprovementRequest request = new LectureImprovementRequest(
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2026-03-15"),
                "2026학년도 강의 품질 개선 활동",
                2026,
                1,
                List.of());
        LectureImprovementRow existing = new LectureImprovementRow(
                65L,
                "B83-LI-002",
                101L,
                "professor1",
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-12-31"),
                "DRAFT",
                "2025학년도 강의 품질 개선 활동",
                2025,
                2,
                "[]");
        LectureImprovementRow updated = new LectureImprovementRow(
                65L,
                "B83-LI-002",
                101L,
                "professor1",
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2026-03-15"),
                "DRAFT",
                "2026학년도 강의 품질 개선 활동",
                2026,
                1,
                "[]");
        when(mapper.findById(65L)).thenReturn(existing, updated);

        LectureImprovementRow result = service.update(65L, request, requester, "REQ-1903-UPDATE");

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(updated);
        org.assertj.core.api.Assertions.assertThat(
                LectureImprovementService.class
                        .getDeclaredMethod(
                                "update",
                                Long.class,
                                LectureImprovementRequest.class,
                                CurrentUser.class,
                                String.class)
                        .isAnnotationPresent(Transactional.class))
                .isTrue();
        InOrder orderedUpdates = inOrder(mapper);
        orderedUpdates.verify(mapper).updateAchievement(
                eq(65L),
                eq("2026"),
                eq("LECTURE_IMPROVEMENT"),
                eq(LocalDate.parse("2026-03-15")),
                eq("[]"),
                eq(101L));
        orderedUpdates.verify(mapper).updateDetail(
                eq(65L),
                eq("2026학년도 강의 품질 개선 활동"),
                eq(2026),
                eq("1"),
                eq(101L));
        verify(guardService).validateMutation(eq(requester), any(EducationAchievementMutationContext.class));
    }

    @Test
    void createWritesHeaderDetailLifecycleAndChangeHistory() {
        CurrentUser requester = new CurrentUser(
                101L,
                "professor1",
                "E0101",
                "교원",
                List.of("R01"),
                List.of());
        LectureImprovementRequest request = new LectureImprovementRequest(
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-12-31"),
                "강의 품질 개선 활동",
                2025,
                2,
                List.of());
        LectureImprovementRow saved = new LectureImprovementRow(
                65L,
                "LI-created",
                101L,
                "professor1",
                "LECTURE_IMPROVEMENT",
                LocalDate.parse("2025-12-31"),
                "DRAFT",
                "강의 품질 개선 활동",
                2025,
                2,
                "[]");
        when(mapper.findIdByManagementNo(any())).thenReturn(65L);
        when(mapper.findById(65L)).thenReturn(saved);

        LectureImprovementRow result = service.create(request, requester, "REQ-1901-CREATE");

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(saved);
        verify(guardService).validateMutation(eq(requester), any(EducationAchievementMutationContext.class));
        verify(mapper).insertAchievement(
                any(),
                eq(101L),
                eq("2025"),
                eq("LECTURE_IMPROVEMENT"),
                eq(LocalDate.parse("2025-12-31")),
                eq("[]"),
                eq(101L));
        verify(mapper).insertDetail(
                eq(65L),
                eq("강의 품질 개선 활동"),
                eq(2025),
                eq("2"),
                eq(101L));
        verify(mapper).insertInitialStatusHistory(65L, 101L);
        verify(mapper).insertChangeHistory(
                eq("education_achievements"),
                eq("65"),
                eq("CREATE"),
                eq("lecture_improvement"),
                isNull(),
                any(),
                eq(101L),
                eq("강의개선 실적 등록"));
    }
}

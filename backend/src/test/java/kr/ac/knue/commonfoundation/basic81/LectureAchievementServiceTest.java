package kr.ac.knue.commonfoundation.basic81;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;

/** Verifies lecture create and update transactions preserve audit and lifecycle behavior. */
class LectureAchievementServiceTest {
    @Test
    void saveCreatesLectureSourceStatusAndDataChangeHistory() {
        LectureAchievementMapper mapper = org.mockito.Mockito.mock(LectureAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        LectureAchievementService service = new LectureAchievementService(
                mapper, guardService, new ObjectMapper());
        CurrentUser requester = new CurrentUser(
                101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
        SaveLectureAchievementRequest request = new SaveLectureAchievementRequest(
                null,
                "LECTURE",
                LocalDate.parse("2025-12-31"),
                new ObjectMapper().createObjectNode().put("hours", 3),
                "attachment-opaque-ref");
        LectureAchievementRow saved = row(82L, "LA-save-test", "{\"hours\":3}");
        when(guardService.validateMutation(eq(requester), any()))
                .thenReturn(OccurredDateValidation.outsideEvaluationPeriod());
        when(mapper.findLectureAchievementByManagementNo(any())).thenReturn(saved);

        LectureAchievementSaveResult result = service.save(request, requester, "REQ-B81-LA-SAVE");

        assertThat(result.achievement().managementItemCode()).isEqualTo("LECTURE");
        assertThat(result.occurredDateWarning()).isTrue();
        verify(mapper).insertLectureAchievement(
                any(),
                eq(101L),
                eq("2025"),
                eq("LECTURE"),
                eq(LocalDate.parse("2025-12-31")),
                eq("{\"hours\":3}"),
                eq("attachment-opaque-ref"),
                eq(101L));
        verify(mapper).insertStatusHistory(any(EducationAchievementStatusHistory.class));
        verify(mapper).insertChangeHistory(
                eq("lecture_achievements"),
                eq("82"),
                eq("CREATE"),
                eq("achievement_detail"),
                eq(null),
                eq("{\"hours\":3}"),
                eq(101L),
                eq("강의실적 저장"),
                eq("REQ-B81-LA-SAVE"));
    }

    @Test
    void saveUpdatesTheSelectedLectureRowAfterGuardChecks() {
        LectureAchievementMapper mapper = org.mockito.Mockito.mock(LectureAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        LectureAchievementService service = new LectureAchievementService(
                mapper, guardService, new ObjectMapper());
        CurrentUser requester = new CurrentUser(
                101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
        LectureAchievementRow existing = row(82L, "B77-LA-001", "{\"hours\":3}");
        LectureAchievementRow updated = row(82L, "B77-LA-001", "{\"hours\":4}");
        SaveLectureAchievementRequest request = new SaveLectureAchievementRequest(
                82L,
                "LECTURE",
                LocalDate.parse("2026-04-11"),
                new ObjectMapper().createObjectNode().put("hours", 4),
                null);
        when(mapper.findLectureAchievement(82L)).thenReturn(existing, updated);
        when(guardService.validateMutation(eq(requester), any()))
                .thenReturn(OccurredDateValidation.accepted());

        LectureAchievementSaveResult result = service.save(request, requester, "REQ-B81-LA-UPDATE");

        assertThat(result.achievement().achievementDetail()).isEqualTo("{\"hours\":4}");
        verify(mapper).updateLectureAchievement(
                eq(82L),
                eq("LECTURE"),
                eq(LocalDate.parse("2026-04-11")),
                eq("{\"hours\":4}"),
                eq(null),
                eq(101L));
        verify(mapper).insertChangeHistory(
                eq("lecture_achievements"),
                eq("82"),
                eq("UPDATE"),
                eq("achievement_detail"),
                eq("{\"hours\":3}"),
                eq("{\"hours\":4}"),
                eq(101L),
                eq("강의실적 수정"),
                eq("REQ-B81-LA-UPDATE"));
    }

    @Test
    void saveRejectsConfirmedLectureBeforeAnyUpdateMutation() {
        LectureAchievementMapper mapper = org.mockito.Mockito.mock(LectureAchievementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(EducationAchievementGuardService.class);
        LectureAchievementService service = new LectureAchievementService(
                mapper, guardService, new ObjectMapper());
        CurrentUser requester = new CurrentUser(
                101L, "faculty", "E0101", "교원", List.of("R01"), List.of());
        SaveLectureAchievementRequest request = new SaveLectureAchievementRequest(
                82L,
                "LECTURE",
                LocalDate.parse("2026-04-11"),
                new ObjectMapper().createObjectNode().put("hours", 4),
                null);
        when(mapper.findLectureAchievement(82L)).thenReturn(row(82L, "B77-LA-001", "{\"hours\":3}"));
        when(guardService.validateMutation(eq(requester), any()))
                .thenThrow(new ConflictException("CONFIRMED_DATA_LOCKED"));

        assertThatThrownBy(() -> service.save(request, requester, "REQ-B81-LA-CONFIRMED"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CONFIRMED_DATA_LOCKED");

        verify(mapper, never()).updateLectureAchievement(any(), any(), any(), any(), any(), any());
        verify(mapper, never()).insertChangeHistory(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private LectureAchievementRow row(Long id, String managementNo, String detail) {
        return new LectureAchievementRow(
                id,
                managementNo,
                101L,
                "faculty",
                "2026",
                "LECTURE",
                LocalDate.parse("2026-04-10"),
                detail,
                "DRAFT",
                null,
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }
}

package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DegreeCompletionAchievementServiceTest {
    @Test
    void savePersistsAndReloadsEveryDegreeCompletionStudentDetail() {
        DegreeCompletionMapper mapper = Mockito.mock(DegreeCompletionMapper.class);
        DegreeCompletionAchievementService service = new DegreeCompletionAchievementService(mapper);
        CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());
        DegreeCompletionDtos.SaveRequest request = new DegreeCompletionDtos.SaveRequest(null, "2026", "ORG-1", "B77-DC-001", LocalDate.parse("2026-02-20"), Map.of(), null,
                List.of(new DegreeCompletionDtos.Student("MASTER", "석사생", "석사논문", LocalDate.parse("2026-02-20")), new DegreeCompletionDtos.Student("DOCTORAL", "박사생", "박사논문", LocalDate.parse("2026-08-31"))), "등록");
        DegreeCompletionDtos.Header header = new DegreeCompletionDtos.Header(63L, "2026", 2L, "ORG-1", "B77-DC-001", LocalDate.parse("2026-02-20"), Map.of(), "DRAFTING", null, LocalDateTime.parse("2026-02-20T09:00:00"));
        when(mapper.findCreatedId(eq(request), eq(2L))).thenReturn(63L);
        when(mapper.findHeader(63L)).thenReturn(header);
        when(mapper.listStudents(63L)).thenReturn(request.students());

        DegreeCompletionDtos.Row saved = service.save(request, teacher);

        ArgumentCaptor<DegreeCompletionDtos.Student> students = ArgumentCaptor.forClass(DegreeCompletionDtos.Student.class);
        verify(mapper, org.mockito.Mockito.times(2)).insertStudent(eq(63L), students.capture(), eq(2L));
        assertThat(students.getAllValues()).extracting(DegreeCompletionDtos.Student::degreeType).containsExactly("MASTER", "DOCTORAL");
        assertThat(saved.students()).containsExactlyElementsOf(request.students());
        verify(mapper).insertChangeHistory(eq("63"), eq("CREATE"), any(), any(), eq(2L), eq("등록"));
    }

    @Test
    void r09AdministratorCanListButCannotSaveDegreeCompletionAchievements() {
        DegreeCompletionMapper mapper = Mockito.mock(DegreeCompletionMapper.class);
        DegreeCompletionAchievementService service = new DegreeCompletionAchievementService(mapper);
        CurrentUser administrator = new CurrentUser(1L, "administrator", "E0001", "관리자", List.of("R09"), List.of());
        DegreeCompletionDtos.SearchCriteria criteria = new DegreeCompletionDtos.SearchCriteria(0, 20, null, null, null, null);
        when(mapper.list(criteria, 1L)).thenReturn(List.of());
        when(mapper.count(criteria, 1L)).thenReturn(0L);

        DegreeCompletionDtos.SearchResponse response = service.list(criteria, administrator);

        assertThat(response.achievements()).isEmpty();
        verify(mapper).list(criteria, 1L);
        assertThatThrownBy(() -> service.save(null, administrator))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ForbiddenException.class);
    }
}

package kr.ac.knue.commonfoundation.degreecompletions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DegreeCompletionAchievementServiceTest {
    @Mock DegreeCompletionAchievementMapper mapper;
    @InjectMocks DegreeCompletionAchievementService service;

    @Test
    void savePersistsAndRereadsAllDegreeStudentFields() {
        DegreeCompletionAchievementSaveRequest request = request();
        DegreeCompletionStudentRequest student = request.getStudents().get(0);
        when(mapper.insert(any(DegreeCompletionAchievementSaveRequest.class), eq("2026"), eq(1L))).thenAnswer(invocation -> {
            invocation.getArgument(0, DegreeCompletionAchievementSaveRequest.class).setAchievementId(91L);
            return 1;
        });
        when(mapper.find(91L)).thenReturn(row());
        when(mapper.findStudents(91L)).thenReturn(List.of(student));

        DegreeCompletionAchievementRow saved = service.save(request, user());

        org.junit.jupiter.api.Assertions.assertEquals("DOCTORAL", saved.students().get(0).getDegreeType());
        org.junit.jupiter.api.Assertions.assertEquals("김교원", saved.students().get(0).getStudentName());
        org.junit.jupiter.api.Assertions.assertEquals("교수학습 연구", saved.students().get(0).getThesisTitle());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.parse("2026-08-20"), saved.students().get(0).getDegreeAwardedDate());
        verify(mapper).deleteStudents(91L);
        verify(mapper).insertStudent(91L, student);
        verify(mapper, times(1)).insertChangeHistory(eq(91L), eq("CREATE"), ArgumentMatchers.isNull(), eq("석·박사 배출 실적"), eq(1L), any());
    }

    private DegreeCompletionAchievementSaveRequest request() {
        DegreeCompletionStudentRequest student = new DegreeCompletionStudentRequest();
        student.setDegreeType("DOCTORAL");
        student.setStudentName("김교원");
        student.setThesisTitle("교수학습 연구");
        student.setDegreeAwardedDate(LocalDate.parse("2026-08-20"));
        DegreeCompletionAchievementSaveRequest request = new DegreeCompletionAchievementSaveRequest();
        request.setManagementItemCode("EDU_DEGREE_COMPLETION");
        request.setOrganizationCode("KNUE-COL-EDU");
        request.setOccurredDate(LocalDate.parse("2026-08-20"));
        request.setAchievementDetail("석·박사 배출 실적");
        request.setStudents(List.of(student));
        return request;
    }

    private DegreeCompletionAchievementRow row() {
        return new DegreeCompletionAchievementRow(91L, "DC-91", "교원", "EDU_DEGREE_COMPLETION", "KNUE-COL-EDU",
                LocalDate.parse("2026-08-20"), "석·박사 배출 실적", "DRAFTING", null,
                LocalDateTime.parse("2026-08-20T09:00:00"), 1L);
    }

    private CurrentUser user() { return new CurrentUser(1L, "faculty", "E0001", "교원", List.of("R01"), List.of()); }
}

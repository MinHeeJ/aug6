package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.junit.jupiter.api.Test;

class DegreeCompletionAchievementServiceTest {
    @Test
    void savesAndRereadsDegreeRecipientDetailsInTheSameCommandFlow() {
        DegreeCompletionAchievementMapper mapper = mock(DegreeCompletionAchievementMapper.class);
        EducationAchievementGuard guard = mock(EducationAchievementGuard.class);
        DegreeCompletionAchievementService service = new DegreeCompletionAchievementService(
                mapper,
                guard,
                new ObjectMapper().findAndRegisterModules()
        );
        DegreeCompletionSaveRequest request = request();
        CurrentUser user = new CurrentUser(
                1001L,
                "professor1",
                "E1001",
                "교원",
                List.of("R01"),
                List.of()
        );
        DegreeCompletionAchievementModels.Header stored = header();
        DegreeCompletionAchievementModels.Student recipient =
                new DegreeCompletionAchievementModels.Student(
                        1L,
                        "MASTER",
                        "석사 지도학생",
                        "교육 리더십 연구",
                        LocalDate.of(2026, 2, 20)
                );
        when(mapper.findOrganizationCodeForUser(1001L)).thenReturn("KNUE-COL-EDU");
        when(guard.validateMutation(eq(user), any())).thenReturn(new EducationAchievementGuardResult(false));
        doAnswer(invocation -> {
            ((DegreeCompletionSaveRequest) invocation.getArgument(0)).setAchievementId(790401L);
            return null;
        }).when(mapper).insert(any(), any(), any(), any(), any());
        when(mapper.findById(790401L)).thenReturn(stored);
        when(mapper.findStudents(790401L)).thenReturn(List.of(recipient));

        DegreeCompletionAchievementModels.Row saved = service.save(request, user);

        assertThat(saved.students()).containsExactly(recipient);
        assertThat(saved.students().get(0).degreeType()).isEqualTo("MASTER");
        assertThat(saved.students().get(0).thesisTitle()).isEqualTo("교육 리더십 연구");
        verify(mapper).insert(
                eq(request),
                eq(LocalDate.of(2026, 2, 20)),
                eq("{\"source\":\"manual\"}"),
                eq("DRAFT"),
                eq(1001L)
        );
        verify(mapper).insertStudent(eq(790401L), eq(request.getStudents().get(0)), eq(1001L));
        verify(mapper).insertChangeHistory(
                eq(790401L),
                eq("CREATE"),
                eq(null),
                any(),
                eq(1001L),
                eq("석·박사 배출 실적 등록")
        );
    }

    private DegreeCompletionSaveRequest request() {
        DegreeCompletionSaveRequest.StudentRequest student = new DegreeCompletionSaveRequest.StudentRequest();
        student.setDegreeType("MASTER");
        student.setStudentName("석사 지도학생");
        student.setThesisTitle("교육 리더십 연구");
        student.setDegreeAwardedDate(LocalDate.of(2026, 2, 20));
        DegreeCompletionSaveRequest request = new DegreeCompletionSaveRequest();
        request.setManagementItemCode("EDU-DEGREE-A");
        request.setAchievementDetail(JsonNodeFactory.instance.objectNode().put("source", "manual"));
        request.setStudents(List.of(student));
        return request;
    }

    private DegreeCompletionAchievementModels.Header header() {
        return new DegreeCompletionAchievementModels.Header(
                790401L,
                "DC-790401",
                1001L,
                "교원",
                "KNUE-COL-EDU",
                "2026",
                "EDU-DEGREE-A",
                LocalDate.of(2026, 2, 20),
                "{}",
                "DRAFT",
                null,
                LocalDateTime.parse("2026-09-29T09:00:00")
        );
    }
}

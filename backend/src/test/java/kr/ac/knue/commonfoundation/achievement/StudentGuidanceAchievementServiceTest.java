package kr.ac.knue.commonfoundation.achievement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StudentGuidanceAchievementServiceTest {
    @Test
    void errorRowsBlockCommitBeforeAnyStudentGuidanceBusinessInsert() {
        StudentGuidanceMapper mapper = Mockito.mock(StudentGuidanceMapper.class);
        StudentGuidanceAchievementService service = new StudentGuidanceAchievementService(mapper, new ObjectMapper());
        CurrentUser r07 = new CurrentUser(7L, "excel", "E0007", "Excel 담당", List.of("R07"), List.of());
        when(mapper.existsUploadForUser("SG-UP-ERR", r07.userId())).thenReturn(1);
        when(mapper.countErrors("SG-UP-ERR")).thenReturn(1);

        assertThatThrownBy(() -> service.commitUpload("SG-UP-ERR", r07)).isInstanceOf(ConflictException.class);
        verify(mapper, never()).listNormalPayloads("SG-UP-ERR");
        verify(mapper, never()).insertImportedAchievement(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void duplicateRowsInOneUploadAreRejectedBeforeCommit() {
        StudentGuidanceMapper mapper = Mockito.mock(StudentGuidanceMapper.class);
        StudentGuidanceAchievementService service = new StudentGuidanceAchievementService(mapper, new ObjectMapper());
        CurrentUser r07 = new CurrentUser(7L, "excel", "E0007", "Excel 담당", List.of("R07"), List.of());
        when(mapper.countTemplate()).thenReturn(1);
        when(mapper.findUserIdByEmployeeNo("E0002")).thenReturn(2L);
        when(mapper.findOrganizationForUser(2L)).thenReturn("ORG-1");
        when(mapper.existsActiveManagementItem("2026", "MI-001")).thenReturn(1);
        when(mapper.existsDuplicate(Mockito.eq(2L), Mockito.any(), Mockito.any(), Mockito.eq("학생A"))).thenReturn(0);
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "학생지도.csv", "text/csv", ("employeeNo,evaluationYear,organizationCode,managementItemCode,guidanceStartDate,guidanceEndDate,studentName\n"
                + "E0002,2026,ORG-1,MI-001,2026-03-01,2026-06-30,학생A\n"
                + "E0002,2026,ORG-1,MI-001,2026-03-01,2026-06-30,학생A\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        StudentGuidanceDtos.UploadResult result = service.validateUpload(file, r07);

        org.assertj.core.api.Assertions.assertThat(result.errorCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(result.errors()).anyMatch(error -> "DUPLICATE".equals(error.errorCode()));
        verify(mapper, never()).insertImportedAchievement(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void r09AdministratorCanReadExcelHistoryAndTemplateWithoutExcelMutationRole() {
        StudentGuidanceMapper mapper = Mockito.mock(StudentGuidanceMapper.class);
        StudentGuidanceAchievementService service = new StudentGuidanceAchievementService(mapper, new ObjectMapper());
        CurrentUser administrator = new CurrentUser(1L, "administrator", "E0001", "관리자", List.of("R09"), List.of());
        when(mapper.listHistories(1L)).thenReturn(List.of());

        org.assertj.core.api.Assertions.assertThat(service.histories(administrator)).isEmpty();
        org.assertj.core.api.Assertions.assertThat(new String(service.template(administrator), java.nio.charset.StandardCharsets.UTF_8))
                .contains("employeeNo,evaluationYear");
        verify(mapper).listHistories(1L);
        assertThatThrownBy(() -> service.commitUpload("SG-UP-1", administrator))
                .isInstanceOf(kr.ac.knue.commonfoundation.common.api.ForbiddenException.class);
    }
}

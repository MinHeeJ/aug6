package kr.ac.knue.commonfoundation.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.excel.ExcelUploadHistoryRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentGuidanceAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentGuidanceAchievementApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudentGuidanceAchievementService service;
    @MockBean StudentGuidanceExcelService excelService;
    private final CurrentUser teacher = new CurrentUser(101L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
    private final CurrentUser r07 = new CurrentUser(701L, "operator7", "E7001", "담당자", List.of("R07"), List.of());

    @Test
    void listAndSaveUseStudentGuidanceControllerContract() throws Exception {
        when(service.list(any(), eq(teacher))).thenReturn(new StudentGuidanceAchievementSearchResponse(List.of(row()), 0, 20, 1));
        when(service.save(any(), eq(teacher))).thenReturn(row());
        mockMvc.perform(get("/api/business/student-guidance-achievements").requestAttr("currentUser", teacher).param("managementItemCode", "EDU-STUDENT-GUIDANCE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievements[0].managementNo").value("B79-SG-001-01"));
        mockMvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content("""
                {"managementItemCode":"EDU-STUDENT-GUIDANCE","guidanceStartDate":"2026-04-01","guidanceEndDate":"2026-04-30","students":[{"studentName":"학생가"}]}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.certificationStatus").value("DRAFT"));
    }

    @Test
    void saveRejectsMissingRequiredFieldsAndR07IndividualAccess() throws Exception {
        mockMvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser", teacher).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[?(@.field == 'managementItemCode')]").exists());
        mockMvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser", r07).contentType(MediaType.APPLICATION_JSON).content("""
                {"managementItemCode":"EDU-STUDENT-GUIDANCE","guidanceStartDate":"2026-04-01","guidanceEndDate":"2026-04-30","students":[{"studentName":"학생가"}]}
                """))
                .andExpect(status().isForbidden());
        verify(service, never()).save(any(), eq(r07));
    }

    @Test
    void authenticatedTeacherCanReadOnlyTheirOwnExcelUploadHistory() throws Exception {
        when(excelService.histories(teacher)).thenReturn(List.of(new ExcelUploadHistoryRow(
                "SG-UP-1",
                "학생지도.csv",
                teacher.userId(),
                1,
                1,
                0,
                0,
                0,
                10L,
                LocalDateTime.parse("2026-04-01T09:00:00")
        )));

        mockMvc.perform(get("/api/business/student-guidance-achievements/excel-upload-histories")
                        .requestAttr("currentUser", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].uploaderUserId").value(teacher.userId()));
    }

    private StudentGuidanceAchievementRow row() {
        return new StudentGuidanceAchievementRow(793001L, "B79-SG-001-01", 101L, "홍길동", "2026", "KNUE-DEPT-COMP", "EDU-STUDENT-GUIDANCE", LocalDate.parse("2026-04-01"), LocalDate.parse("2026-04-30"), "DRAFT", false, "[{\"studentNo\":\"S2026001\",\"studentName\":\"학생가\"}]", LocalDateTime.parse("2026-04-01T09:00:00"), LocalDateTime.parse("2026-04-01T09:00:00"));
    }
}

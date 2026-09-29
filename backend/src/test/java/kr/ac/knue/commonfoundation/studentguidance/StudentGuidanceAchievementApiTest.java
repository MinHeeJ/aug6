package kr.ac.knue.commonfoundation.studentguidance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDate; import java.time.LocalDateTime; import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser; import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; import org.springframework.boot.test.mock.mockito.MockBean; import org.springframework.context.annotation.Import; import org.springframework.http.MediaType; import org.springframework.test.web.servlet.MockMvc;
@WebMvcTest(StudentGuidanceAchievementController.class) @AutoConfigureMockMvc(addFilters=false) @Import(GlobalExceptionHandler.class)
class StudentGuidanceAchievementApiTest { @Autowired MockMvc mockMvc; @MockBean StudentGuidanceAchievementService achievementService; @MockBean StudentGuidanceExcelUploadService excelService;
 @Test void savesIndividualGuidanceWithStudents() throws Exception {
  when(achievementService.save(any(), any())).thenReturn(new StudentGuidanceAchievementRow(9L, "SG-9", "교원", "EDU_GUIDANCE", "KNUE-COL-EDU", LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-01"), LocalDate.parse("2026-06-30"), 1, "지도", "DRAFTING", null, LocalDateTime.now(), 1L, List.of(new StudentGuidanceStudentRequest())));
  mockMvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser", new CurrentUser(1L, "faculty", "E1", "교원", List.of("R01"), List.of())).contentType(MediaType.APPLICATION_JSON).content("{\"managementItemCode\":\"EDU_GUIDANCE\",\"organizationCode\":\"KNUE-COL-EDU\",\"occurredDate\":\"2026-03-01\",\"guidanceStartDate\":\"2026-03-01\",\"guidanceEndDate\":\"2026-06-30\",\"students\":[{\"studentNo\":\"S1\",\"studentName\":\"학생\"}]}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.managementNo").value("SG-9"));
 }
 @Test void rejectsMissingStudents() throws Exception {
  mockMvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser", new CurrentUser(1L, "faculty", "E1", "교원", List.of("R01"), List.of())).contentType(MediaType.APPLICATION_JSON).content("{\"managementItemCode\":\"EDU_GUIDANCE\"}"))
    .andExpect(status().isBadRequest());
 }
}

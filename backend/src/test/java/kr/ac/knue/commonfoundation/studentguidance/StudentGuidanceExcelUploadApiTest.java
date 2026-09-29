package kr.ac.knue.commonfoundation.studentguidance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.nio.charset.StandardCharsets; import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser; import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler; import kr.ac.knue.commonfoundation.excel.ExcelUploadCommitResult; import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorRow; import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; import org.springframework.boot.test.mock.mockito.MockBean; import org.springframework.context.annotation.Import; import org.springframework.mock.web.MockMultipartFile; import org.springframework.test.web.servlet.MockMvc;
@WebMvcTest(StudentGuidanceAchievementController.class) @AutoConfigureMockMvc(addFilters=false) @Import(GlobalExceptionHandler.class)
class StudentGuidanceExcelUploadApiTest { @Autowired MockMvc mockMvc; @MockBean StudentGuidanceAchievementService achievementService; @MockBean StudentGuidanceExcelUploadService excelService;
 @Test void r07SeesNormalAndErrorRowsThenCommitsOnlyTheValidUpload() throws Exception { CurrentUser r07=new CurrentUser(7L,"operator","E7","담당자",List.of("R07"),List.of()); when(excelService.upload(any(),any())).thenReturn(new ExcelUploadResult("UP-SG-1","STUDENT_GUIDANCE","student.csv","VALIDATED",1,1,0,0,0,List.of())); when(excelService.commit("UP-SG-1",r07)).thenReturn(new ExcelUploadCommitResult("UP-SG-1",1)); MockMultipartFile file=new MockMultipartFile("file","student.csv","text/csv","학생번호,학생명\nS1,학생\n".getBytes(StandardCharsets.UTF_8)); mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).requestAttr("currentUser",r07)).andExpect(status().isOk()).andExpect(jsonPath("$.data.successCount").value(1)); mockMvc.perform(post("/api/business/student-guidance-achievements/excel-uploads/UP-SG-1/commit").requestAttr("currentUser",r07)).andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(1)); }
 @Test void r01CannotUseR07ExcelRoute() throws Exception { MockMultipartFile file=new MockMultipartFile("file","student.csv","text/csv",new byte[0]); doThrow(new kr.ac.knue.commonfoundation.common.api.ForbiddenException()).when(excelService).upload(any(),any()); mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).requestAttr("currentUser",new CurrentUser(1L,"faculty","E1","교원",List.of("R01"),List.of()))).andExpect(status().isForbidden()); }
}

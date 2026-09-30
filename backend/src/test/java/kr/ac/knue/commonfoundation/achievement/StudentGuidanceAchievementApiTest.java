package kr.ac.knue.commonfoundation.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** Contract tests for the controller that owns R01/R02/R04 student guidance and R07 Excel routes. */
@WebMvcTest(StudentGuidanceAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentGuidanceAchievementApiTest {
 @Autowired MockMvc mvc;
 @MockBean StudentGuidanceAchievementService service;
 @MockBean ExcelOperationsService excelOperationsService;
 private final CurrentUser faculty = new CurrentUser(1L,"professor1","E1","교원",List.of("R01"),List.of());
 private final CurrentUser operator = new CurrentUser(7L,"operator","E7","운영",List.of("R07"),List.of());
 @Test void individualListAndSaveUseTheStudentGuidanceContract() throws Exception {
  when(service.list(faculty,0,20)).thenReturn(List.of(row())); when(service.save(any(),any())).thenReturn(row());
  mvc.perform(get("/api/business/student-guidance-achievements").requestAttr("currentUser",faculty)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].managementNo").value("B77-SG-001"));
  mvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser",faculty).contentType(MediaType.APPLICATION_JSON).content("{\"managementItemCode\":\"STUDENT_GUIDANCE\",\"guidanceStartDate\":\"2026-03-01\",\"guidanceEndDate\":\"2026-06-30\",\"students\":[{\"studentNo\":\"S1\",\"studentName\":\"학생\",\"guidanceType\":\"ADVISORY\"}]}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.students[0].studentNo").value("S1"));
 }
 @Test void r07CannotCallIndividualAchievementApiButCanUseExcelWorkflow() throws Exception {
  mvc.perform(get("/api/business/student-guidance-achievements").requestAttr("currentUser",operator)).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  mvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser",operator).contentType(MediaType.APPLICATION_JSON).content("{\"managementItemCode\":\"STUDENT_GUIDANCE\",\"guidanceStartDate\":\"2026-03-01\",\"guidanceEndDate\":\"2026-06-30\",\"students\":[{\"studentNo\":\"S1\",\"studentName\":\"학생\",\"guidanceType\":\"ADVISORY\"}]}")).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  verifyNoInteractions(service);
 }
 @Test void systemAdministratorCanReadBothIndividualAndExcelWorkflowData() throws Exception {
  CurrentUser admin = new CurrentUser(1L,"admin","E0001","시스템 관리자",List.of("R09"),List.of());
  when(service.list(admin,0,20)).thenReturn(List.of(row()));
  when(service.histories(admin,0,20)).thenReturn(List.of());
  when(excelOperationsService.downloadUploadTemplate("B77-STUDENT-GUIDANCE-TEMPLATE",admin.userId())).thenReturn(new ExcelDownloadFile("student-guidance.csv","text/csv","교번\n".getBytes()));
  mvc.perform(get("/api/business/student-guidance-achievements").requestAttr("currentUser",admin)).andExpect(status().isOk());
  mvc.perform(get("/api/business/student-guidance-achievements/excel-template").requestAttr("currentUser",admin)).andExpect(status().isOk());
  mvc.perform(get("/api/business/student-guidance-achievements/excel-upload-histories").requestAttr("currentUser",admin)).andExpect(status().isOk());
 }
 @Test void r07UploadSeparatesErrorsAndOnlyAllowsR07Commit() throws Exception {
  MockMultipartFile file=new MockMultipartFile("file","student-guidance.csv","text/csv","교번,학생학번,학생명,지도유형,지도시작일,지도종료일\nE1,S1,학생,ADVISORY,2026-03-01,2026-06-30".getBytes());
  when(service.upload(any(),any(),any())).thenReturn(new StudentGuidanceUploadResult("UP-SG-1","student-guidance.csv","VALIDATED",1,1,0,0,List.of()));
  when(service.commit("UP-SG-1",operator)).thenReturn(new StudentGuidanceUploadResult("UP-SG-1","student-guidance.csv","COMMITTED",1,1,0,1,List.of()));
  when(excelOperationsService.downloadUploadTemplate("B77-STUDENT-GUIDANCE-TEMPLATE",operator.userId())).thenReturn(new ExcelDownloadFile("student-guidance.csv","text/csv","교번\n".getBytes()));
  mvc.perform(get("/api/business/student-guidance-achievements/excel-template").requestAttr("currentUser",operator)).andExpect(status().isOk());
  mvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).param("templateId","B77-STUDENT-GUIDANCE-TEMPLATE").requestAttr("currentUser",operator)).andExpect(status().isOk()).andExpect(jsonPath("$.data.errorCount").value(0));
  mvc.perform(post("/api/business/student-guidance-achievements/excel-uploads/UP-SG-1/commit").requestAttr("currentUser",operator)).andExpect(status().isOk()).andExpect(jsonPath("$.data.savedCount").value(1));
  mvc.perform(post("/api/business/student-guidance-achievements/excel-uploads/UP-SG-1/commit").requestAttr("currentUser",faculty)).andExpect(status().isForbidden());
 }
 @Test void invalidIndividualRequestDoesNotReachService() throws Exception {
  mvc.perform(post("/api/business/student-guidance-achievements").requestAttr("currentUser",faculty).contentType(MediaType.APPLICATION_JSON).content("{\"managementItemCode\":\"\",\"students\":[]}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  verify(service,never()).save(any(),any());
 }
 private StudentGuidanceAchievementRow row(){return new StudentGuidanceAchievementRow(11L,"B77-SG-001","2026",1L,"STUDENT_GUIDANCE",LocalDate.parse("2026-03-01"),LocalDate.parse("2026-06-30"),1,"DRAFTING",List.of(new StudentGuidanceStudentRow(1L,"S1","학생","ADVISORY")));}
}

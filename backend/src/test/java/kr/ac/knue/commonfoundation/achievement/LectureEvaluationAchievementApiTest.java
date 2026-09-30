package kr.ac.knue.commonfoundation.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract tests for the controller that owns the lecture-evaluation route. */
@WebMvcTest(LectureEvaluationAchievementController.class)
@AutoConfigureMockMvc(addFilters=false)
@Import(GlobalExceptionHandler.class)
class LectureEvaluationAchievementApiTest {
 @Autowired MockMvc mvc; @MockBean LectureEvaluationAchievementService service;
 CurrentUser permitted=new CurrentUser(1L,"professor1","E1","교원",List.of("R01"),List.of()); CurrentUser r07=new CurrentUser(7L,"operator","E7","운영자",List.of("R07"),List.of());
 @Test void listUsesContractFiltersPaginationAndAuthorizedUser() throws Exception { when(service.list(any(),any())).thenReturn(new LectureEvaluationAchievementResponse.Search(List.of(row()),0,20,1)); mvc.perform(get("/api/business/lecture-evaluation-achievements").requestAttr("currentUser",permitted).param("managementItemCode","LECTURE_EVALUATION")).andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].managementItemCode").value("LECTURE_EVALUATION")).andExpect(jsonPath("$.data.pageSize").value(20)); }
 @Test void listAcceptsEachFacultyWorkflowRole() throws Exception {
  when(service.list(any(),any())).thenReturn(new LectureEvaluationAchievementResponse.Search(List.of(row()),0,20,1));
  for(String role:List.of("R01","R02","R04")){ CurrentUser user=new CurrentUser(10L,role.toLowerCase(),"E"+role,"검증자",List.of(role),List.of()); mvc.perform(get("/api/business/lecture-evaluation-achievements").requestAttr("currentUser",user)).andExpect(status().isOk()); }
 }
 @Test void listAllowsSystemAdministratorForRuntimeReadVerification() throws Exception {
  when(service.list(any(),any())).thenReturn(new LectureEvaluationAchievementResponse.Search(List.of(row()),0,20,1));
  mvc.perform(get("/api/business/lecture-evaluation-achievements").requestAttr("currentUser",new CurrentUser(1L,"admin","E0001","시스템 관리자",List.of("R09"),List.of()))).andExpect(status().isOk());
 }
 @Test void saveRejectsR07AndMissingManagementItemWithoutServiceSideEffect() throws Exception { mvc.perform(post("/api/business/lecture-evaluation-achievements").requestAttr("currentUser",r07).contentType(MediaType.APPLICATION_JSON).content("{\"managementItemCode\":\"LECTURE_EVALUATION\",\"occurredDate\":\"2026-04-10\"}")).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN")); mvc.perform(post("/api/business/lecture-evaluation-achievements").requestAttr("currentUser",permitted).contentType(MediaType.APPLICATION_JSON).content("{\"occurredDate\":\"2026-04-10\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("managementItemCode")); verify(service,never()).save(any(),any(),any()); }
 @Test void saveReturnsPersistedAchievementContract() throws Exception { when(service.save(any(),any(),any())).thenReturn(row()); mvc.perform(post("/api/business/lecture-evaluation-achievements").requestAttr("currentUser",permitted).header("X-Request-Id","req-81").contentType(MediaType.APPLICATION_JSON).content("{\"managementItemCode\":\"LECTURE_EVALUATION\",\"occurredDate\":\"2026-04-10\",\"achievementDetail\":{\"score\":4.8}}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.achievementId").value(81)).andExpect(jsonPath("$.meta.requestId").value("req-81")); }
 private LectureEvaluationAchievementResponse.Row row(){return new LectureEvaluationAchievementResponse.Row(81L,"B77-LE-001","2026","KNUE-DEPT-COMP",1L,"교원","LECTURE_EVALUATION",LocalDate.parse("2026-04-10"),EducationAchievementStatus.DRAFTING,new ObjectMapper().createObjectNode().put("score",4.8),0,false);}
}

package kr.ac.knue.commonfoundation.basic36;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.AuthController;
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

@WebMvcTest(ResearcherProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ResearcherProfileApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ResearcherProfileService service;

    private final CurrentUser teacher = new CurrentUser(2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
    private final CurrentUser businessOwner = new CurrentUser(4L, "business-owner", "E0004", "업무담당자", List.of("R04"), List.of());
    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "관리자", List.of("R09"), List.of());
    private final CurrentUser unauthorized = new CurrentUser(5L, "viewer", "E0005", "조회자", List.of("R03"), List.of());

    @Test
    void listResearcherProfilesReturnsKorusReadonlySummaryForReq1265Req1266() throws Exception {
        when(service.list(any(), eq(teacher))).thenReturn(new ResearcherProfileSearchResponse(List.of(summary()), 0, 20, 1));

        mockMvc.perform(get("/api/researcher-profiles")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .param("employeeNo", "E1001")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profiles[0].employeeNo").value("E1001"))
                .andExpect(jsonPath("$.data.profiles[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data.profiles[0].organizationCode").value("KNUE-DEPT-COMP"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getResearcherProfileReturnsDetailTabsForReq1267() throws Exception {
        when(service.get("E1001", teacher)).thenReturn(detail());

        mockMvc.perform(get("/api/researcher-profiles/E1001")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeNo").value("E1001"))
                .andExpect(jsonPath("$.data.researchFields[0].majorName").value("컴퓨터교육"))
                .andExpect(jsonPath("$.data.careers[0].workplace").value("한국교원대학교"))
                .andExpect(jsonPath("$.data.degrees[0].degreeType").value("DOCTOR"))
                .andExpect(jsonPath("$.data.certifications[0].certificationName").value("교원자격"));
    }

    @Test
    void saveResearcherProfileDegreesRejectsKorusReadonlyPayloadForReq1275() throws Exception {
        mockMvc.perform(put("/api/researcher-profiles/E1001/degrees")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeNo\":\"E9999\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("employeeNo"));
        verify(service, never()).saveDegrees(any(), any(), any());
    }

    @Test
    void saveResearcherProfileDegreesPersistsChangedMetadataAndDoctorWarningForReq1276Req1277() throws Exception {
        ResearcherProfileSaveResponse response = new ResearcherProfileSaveResponse(detail(),
                List.of("박사 최종학위의 학사·석사·박사 선행학위 입력 여부를 확인하세요."));
        when(service.saveDegrees(eq("E1001"), any(), eq(teacher))).thenReturn(response);

        mockMvc.perform(put("/api/researcher-profiles/E1001/degrees")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"degreeType\":\"DOCTOR\",\"universityName\":\"한국교원대학교\",\"startYm\":\"202001\",\"acquiredYm\":\"202402\",\"countryName\":\"대한민국\",\"collegeName\":\"교육대학원\",\"advisorName\":\"김교수\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warnings[0]").value("박사 최종학위의 학사·석사·박사 선행학위 입력 여부를 확인하세요."));
    }

    @Test
    void listDegreePrerequisiteMissingResearchersAllowsR04AndReturnsRowsForReq1269() throws Exception {
        when(service.listDegreePrerequisiteMissing(any())).thenReturn(new ResearcherProfileSearchResponse(List.of(summary()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/researcher-profiles/degree-prerequisite-missing")
                        .requestAttr("currentUser", businessOwner)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profiles[0].employeeNo").value("E1001"));
    }

    @Test
    void listDegreePrerequisiteMissingResearchersRejectsUnauthorizedRoleForReq1269() throws Exception {
        mockMvc.perform(get("/api/admin/researcher-profiles/degree-prerequisite-missing")
                        .requestAttr("currentUser", unauthorized)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).listDegreePrerequisiteMissing(any());
    }


    @Test
    void saveResearcherProfileResearchFieldsPersistsItemsAndReturnsProfileContract() throws Exception {
        ResearcherProfileSaveResponse response = new ResearcherProfileSaveResponse(detail(), List.of());
        when(service.saveResearchFields(eq("E1001"), any(), eq(teacher))).thenReturn(response);

        mockMvc.perform(put("/api/researcher-profiles/E1001/research-fields")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-RP-FIELDS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeReason":"전공분야 보강","items":[{"majorName":"컴퓨터교육","detailMajorName":"AI교육","disciplineName":"공학"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.employeeNo").value("E1001"))
                .andExpect(jsonPath("$.data.profile.researchFields[0].majorName").value("컴퓨터교육"))
                .andExpect(jsonPath("$.data.warnings").isArray())
                .andExpect(jsonPath("$.meta.requestId").value("REQ-RP-FIELDS"));
    }

    @Test
    void saveResearcherProfileCareersPersistsItemsAndReturnsProfileContract() throws Exception {
        ResearcherProfileSaveResponse response = new ResearcherProfileSaveResponse(detail(), List.of());
        when(service.saveCareers(eq("E1001"), any(), eq(teacher))).thenReturn(response);

        mockMvc.perform(put("/api/researcher-profiles/E1001/careers")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-RP-CAREERS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeReason":"경력 보강","items":[{"startYm":"202001","endYm":"202412","workplace":"한국교원대학교","positionName":"교수","jobDescription":"연구"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.employeeNo").value("E1001"))
                .andExpect(jsonPath("$.data.profile.careers[0].workplace").value("한국교원대학교"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-RP-CAREERS"));
    }

    @Test
    void saveResearcherProfileCertificationsPersistsItemsAndReturnsProfileContract() throws Exception {
        ResearcherProfileSaveResponse response = new ResearcherProfileSaveResponse(detail(), List.of());
        when(service.saveCertifications(eq("E1001"), any(), eq(teacher))).thenReturn(response);

        mockMvc.perform(put("/api/researcher-profiles/E1001/certifications")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .header("X-Request-Id", "REQ-RP-CERTS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changeReason":"자격 보강","items":[{"acquiredYm":"202403","certificationName":"교원자격","issuerName":"교육부"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.employeeNo").value("E1001"))
                .andExpect(jsonPath("$.data.profile.certifications[0].certificationName").value("교원자격"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-RP-CERTS"));
    }

    @Test
    void saveResearcherProfileCareersRejectsReadonlyNestedPayloadBeforeServiceMutation() throws Exception {
        mockMvc.perform(put("/api/researcher-profiles/E1001/careers")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"employeeNo":"E9999","workplace":"외부"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("items[0].employeeNo"));
        verify(service, never()).saveCareers(any(), any(), any());
    }

    private ResearcherProfileSummary summary() {
        return new ResearcherProfileSummary("E1001", "홍길동", "KNUE-DEPT-COMP", "컴퓨터교육과", "교수", "E1001-APPT",
                "010-0000-0000", "RID-1001", "Y", "Y", "DOCTOR", true, null);
    }

    private ResearcherProfileDetail detail() {
        return new ResearcherProfileDetail(summary(),
                List.of(new ResearcherResearchFieldRow(1L, "E1001", "컴퓨터교육", "AI교육", "공학", 2L, null)),
                List.of(new ResearcherCareerRow(1L, "E1001", "202001", "202412", "한국교원대학교", "교수", "연구", 2L, null)),
                List.of(new ResearcherDegreeRow(1L, "E1001", "DOCTOR", "한국교원대학교", "202001", "202402", "대한민국", "교육대학원", "김교수", 2L, null)),
                List.of(new ResearcherCertificationRow(1L, "E1001", "202403", "교원자격", "교육부", 2L, null)),
                true);
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

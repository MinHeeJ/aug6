package kr.ac.knue.commonfoundation.basic36;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResearcherProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ResearcherProfileAdminLookupApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean ResearcherProfileService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "관리자", List.of("R09"), List.of());
    private final CurrentUser businessOwner = new CurrentUser(4L, "business-owner", "E0004", "업무담당자", List.of("R04"), List.of());

    @Test
    void listFacultySearchResultsReturnsApiResponseRowsForReq1149() throws Exception {
        when(service.listFacultySearchResults(any()))
                .thenReturn(new ResearcherLookupPageResponse<>(List.of(facultyRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/researcher-profiles/faculty-search")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("page", "0")
                        .param("size", "20")
                        .param("keyword", "BASIC37"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rows[0].facultyId").value("BASIC37-SEED-FACULTY-001"))
                .andExpect(jsonPath("$.data.rows[0].facultyName").value("BASIC37 검증교원 001"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listResearcherProfilesReturnsApiResponseRowsForReq1150() throws Exception {
        when(service.listResearcherProfiles(any()))
                .thenReturn(new ResearcherLookupPageResponse<>(List.of(profileRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/researcher-profiles")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("page", "0")
                        .param("size", "20")
                        .param("keyword", "RESEARCHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rows[0].researcherProfileId").value("BASIC37-SEED-RESEARCHER-001"))
                .andExpect(jsonPath("$.data.rows[0].profileStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listDegreeDeficiencyTargetsReturnsApiResponseRowsForReq1151() throws Exception {
        when(service.listDegreeDeficiencyTargets(any()))
                .thenReturn(new ResearcherLookupPageResponse<>(List.of(deficiencyRow()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/researcher-profiles/degree-deficiencies")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rows[0].targetId").value("BASIC37-SEED-DEGREE-001"))
                .andExpect(jsonPath("$.data.rows[0].deficiencyReason").value("박사 최종학위의 학사·석사·박사 선행학위 입력 여부를 확인하세요."));
    }

    @Test
    void relatedResearcherLookupApisRejectNonR09WithoutDtoMappingLeakForReq1152() throws Exception {
        mockMvc.perform(get("/api/admin/researcher-profiles/faculty-search")
                        .requestAttr("currentUser", businessOwner)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("접근 권한이 없습니다."));

        mockMvc.perform(get("/api/admin/researcher-profiles")
                        .requestAttr("currentUser", businessOwner)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/admin/researcher-profiles/degree-deficiencies")
                        .requestAttr("currentUser", businessOwner)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(service, never()).listFacultySearchResults(any());
        verify(service, never()).listResearcherProfiles(any());
        verify(service, never()).listDegreeDeficiencyTargets(any());
    }

    private FacultySearchResultRow facultyRow() {
        return new FacultySearchResultRow("BASIC37-SEED-FACULTY-001", "BASIC37 검증교원 001", "KNUE-DEPT-COMP", "컴퓨터교육과", "교수", "ACTIVE", "학과장", "BASIC37-SEED-FACULTY-001-APPOINTMENT");
    }

    private ResearcherProfileListRow profileRow() {
        return new ResearcherProfileListRow("BASIC37-SEED-RESEARCHER-001", "BASIC37-SEED-FACULTY-001", "BASIC37 검증교원 001", "KNUE-DEPT-COMP", "컴퓨터교육과", "교수", "ACTIVE", "DOCTOR", "ACTIVE", "Y", null);
    }

    private DegreeDeficiencyTargetRow deficiencyRow() {
        return new DegreeDeficiencyTargetRow("BASIC37-SEED-DEGREE-001", "BASIC37-SEED-RESEARCHER-001", "BASIC37-SEED-FACULTY-001", "BASIC37 검증교원 001", "KNUE-DEPT-COMP", "컴퓨터교육과", "DOCTOR", "박사 최종학위의 학사·석사·박사 선행학위 입력 여부를 확인하세요.", null);
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

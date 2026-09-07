package kr.ac.knue.commonfoundation.schoolinfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SchoolInfoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SchoolInfoApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean SchoolInfoService schoolInfoService;

    @Test
    void searchSchoolInfoReturnsApiResponseEnvelopeAndDisplayedRowCount() throws Exception {
        when(schoolInfoService.search(any(SchoolInfoQuery.class))).thenReturn(new SchoolInfoSearchResponse(
                1,
                100,
                1,
                List.of(new SchoolInfoRow("서울특별시교육청", "가락고등학교", "고등학교", "서울", "공립", "서울 송파구 송이로 42", "02-0000-0000"))));

        mockMvc.perform(get("/api/admin/school-info")
                        .param("schoolName", "가락")
                        .param("educationOfficeCode", "B10")
                        .param("page", "1")
                        .param("size", "100")
                        .cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(100))
                .andExpect(jsonPath("$.data.displayedCount").value(1))
                .andExpect(jsonPath("$.data.rows[0].schoolName").value("가락고등학교"))
                .andExpect(jsonPath("$.data.rows[0].educationOfficeName").value("서울특별시교육청"));
    }

    @Test
    void searchSchoolInfoTreatsInfo200AsEmptySuccessfulApiResponse() throws Exception {
        when(schoolInfoService.search(any(SchoolInfoQuery.class))).thenReturn(new SchoolInfoSearchResponse(1, 100, 0, List.of()));

        mockMvc.perform(get("/api/admin/school-info").cookie(adminCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayedCount").value(0))
                .andExpect(jsonPath("$.data.rows.length()").value(0));
    }

    @Test
    void searchSchoolInfoConvertsExternalErrorsToCommonApiError() throws Exception {
        when(schoolInfoService.search(any(SchoolInfoQuery.class)))
                .thenThrow(new ExternalIntegrationException("NEIS 오류: 서비스 키가 유효하지 않습니다."));

        mockMvc.perform(get("/api/admin/school-info").cookie(adminCookie()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EXTERNAL_INTEGRATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("NEIS 오류: 서비스 키가 유효하지 않습니다."));
    }

    private Cookie adminCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

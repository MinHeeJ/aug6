package kr.ac.knue.commonfoundation.basic32;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
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

@WebMvcTest(DeletedBusinessDataController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DeletedBusinessDataApiTest {
    @Autowired MockMvc mockMvc;
    @MockBean DeletedBusinessDataService service;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());
    private final CurrentUser teacher = new CurrentUser(2L, "teacher", "E0002", "교원", List.of("R01"), List.of());

    @Test
    void listDeletedBusinessDataReturnsDefaultTwentyAndFilterableDeletionInfoForReq783Req784() throws Exception {
        DeletedBusinessDataSearchCriteria criteria = new DeletedBusinessDataSearchCriteria(
                0,
                20,
                "FACULTY_ACHIEVEMENT",
                "ACH-2026-0001",
                1L,
                "2026-08-31T00:00:00",
                "2026-08-31T23:59:59");
        when(service.list(criteria, admin)).thenReturn(new DeletedBusinessDataSearchResponse(List.of(row()), 0, 20, 1));

        mockMvc.perform(get("/api/admin/deleted-business-data")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie())
                        .param("businessType", "FACULTY_ACHIEVEMENT")
                        .param("originalKey", "ACH-2026-0001")
                        .param("deletedBy", "1")
                        .param("deletedAtFrom", "2026-08-31T00:00:00")
                        .param("deletedAtTo", "2026-08-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedData[0].businessType").value("FACULTY_ACHIEVEMENT"))
                .andExpect(jsonPath("$.data.deletedData[0].originalKey").value("ACH-2026-0001"))
                .andExpect(jsonPath("$.data.deletedData[0].deletedBy").value(1))
                .andExpect(jsonPath("$.data.deletedData[0].deletedByName").value("시스템관리자"))
                .andExpect(jsonPath("$.data.deletedData[0].deleteReason").value("중복 입력 정리"))
                .andExpect(jsonPath("$.data.deletedData[0].recoverableYn").value("N"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void directR01DeletedBusinessDataSearchIsForbiddenForReq783() throws Exception {
        mockMvc.perform(get("/api/admin/deleted-business-data")
                        .requestAttr("currentUser", teacher)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).list(any(), any());
    }

    @Test
    void deletedBusinessDataApiIsReadOnlyAndDoesNotExposeRestoreOrPhysicalDeleteForReq783() throws Exception {
        mockMvc.perform(post("/api/admin/deleted-business-data")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/admin/deleted-business-data/1")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/admin/deleted-business-data/1/restore")
                        .requestAttr("currentUser", admin)
                        .cookie(sessionCookie()))
                .andExpect(status().isNotFound());
    }

    private DeletedBusinessDataRow row() {
        return new DeletedBusinessDataRow(
                200L,
                "FACULTY_ACHIEVEMENT",
                "ACH-2026-0001",
                1L,
                "admin",
                "시스템관리자",
                LocalDateTime.parse("2026-08-31T10:00:00"),
                "중복 입력 정리",
                "N");
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }
}

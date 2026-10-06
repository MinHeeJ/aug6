package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.AuthService;
import kr.ac.knue.commonfoundation.auth.AuthenticationFilter;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.excel.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Exercises the real servlet filter and domain role guard, not persisted session/menu wiring. */
class EmploymentRateAchievementAuthenticationTest {
    private static final String BASE = "/api/business/employment-rate-achievements";
    private final AuthService sessions = mock(AuthService.class);
    private final EffectivePermissionService menus = mock(EffectivePermissionService.class);
    private final EmploymentRateAchievementMapper mapper = mock(EmploymentRateAchievementMapper.class);
    private final FunctionPermissionService functions = mock(FunctionPermissionService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        var service = new EmploymentRateAchievementService(mapper, mock(EducationAchievementGuardService.class),
                functions, mock(FileStoragePort.class), json, new EmploymentRateWorkbook());
        mvc = MockMvcBuilders.standaloneSetup(new EmploymentRateAchievementController(service))
                .setControllerAdvice(new EmploymentRateInputErrorHandler(), new GlobalExceptionHandler())
                .addFilters(new AuthenticationFilter(sessions, menus, json)).build();
    }

    @Test
    void missingSessionAndMenuDenialStopBeforeDomainQueries() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        when(sessions.currentUser("teacher-session")).thenReturn(principal("R01"));
        mvc.perform(get(BASE).cookie(cookie("teacher-session")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(mapper, functions);
    }

    @Test
    void admittedSessionReachesRealServiceAndScopedList() throws Exception {
        when(sessions.currentUser("teacher-session")).thenReturn(principal("R01"));
        // Route-to-menu DB registration is independently owned by the final integration slice.
        when(menus.canAccess(eq(7L), eq(List.of("R01")), anyString())).thenReturn(true);
        when(mapper.list(anyMap(), eq(7L), eq(List.of("R01")))).thenReturn(List.of(
                Map.of("achievementId", 42L, "achievementName", "취업률 실적")));
        when(mapper.count(anyMap(), eq(7L), eq(List.of("R01")))).thenReturn(1L);
        mvc.perform(get(BASE).cookie(cookie("teacher-session")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].achievementName").value("취업률 실적"));
    }

    @Test
    void menuAdministratorCannotBypassDomainRoleAndUnapprovedR07CannotWrite() throws Exception {
        when(menus.canAccess(eq(7L), anyList(), anyString())).thenReturn(true);
        when(sessions.currentUser("admin-session")).thenReturn(principal("R09"));
        mvc.perform(get(BASE).cookie(cookie("admin-session")))
                .andExpect(status().isForbidden());
        when(sessions.currentUser("operator-session")).thenReturn(principal("R07"));
        mvc.perform(post(BASE + "/bulk-jobs").cookie(cookie("operator-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evaluationYear\":\"2025\",\"actionType\":\"GENERATE\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(mapper);
    }

    private CurrentUser principal(String role) {
        return new CurrentUser(7L, "session-fixture", null, "교원", List.of(role), List.of());
    }

    private Cookie cookie(String id) {
        return new Cookie(AuthController.SESSION_COOKIE, id);
    }
}

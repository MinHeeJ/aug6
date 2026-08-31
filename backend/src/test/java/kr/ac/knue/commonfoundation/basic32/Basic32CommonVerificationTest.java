package kr.ac.knue.commonfoundation.basic32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest({
        EvaluationOrganizationMappingController.class,
        BusinessStatusCodeController.class,
        BusinessStatusTransitionController.class,
        RejectionReasonController.class,
        DataChangeHistoryController.class,
        DeletedBusinessDataController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Basic32CommonVerificationTest {
    @Autowired MockMvc mockMvc;
    @MockBean EvaluationOrganizationMappingService evaluationOrganizationMappingService;
    @MockBean BusinessStatusCodeService businessStatusCodeService;
    @MockBean BusinessStatusTransitionService businessStatusTransitionService;
    @MockBean RejectionReasonService rejectionReasonService;
    @MockBean DataChangeHistoryService dataChangeHistoryService;
    @MockBean DeletedBusinessDataService deletedBusinessDataService;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "E0001", "시스템관리자", List.of("R09"), List.of());

    @Test
    void everyBasic32ListApiDefaultsToTwentyAndAcceptsOnlyCommonUiPageSizesForReq749() throws Exception {
        stubListServicesWithRequestedSafeSize();

        for (Endpoint endpoint : endpoints()) {
            mockMvc.perform(get(endpoint.path())
                            .requestAttr("currentUser", admin)
                            .cookie(sessionCookie()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath(endpoint.sizeJsonPath()).value(20));

            for (int size : List.of(20, 50, 100)) {
                mockMvc.perform(get(endpoint.path())
                                .requestAttr("currentUser", admin)
                                .cookie(sessionCookie())
                                .param("size", String.valueOf(size)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath(endpoint.sizeJsonPath()).value(size));
            }

            mockMvc.perform(get(endpoint.path())
                            .requestAttr("currentUser", admin)
                            .cookie(sessionCookie())
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(endpoint.sizeJsonPath()).value(20));
        }
    }

    @Test
    void everyBasic32CriteriaClampsUnsupportedPageSizesToPreserveTheTwentyFiftyHundredContract() {
        assertThat(new EvaluationOrganizationMappingSearchCriteria(0, 10, null, null, null).safeSize()).isEqualTo(20);
        assertThat(new BusinessStatusCodeSearchCriteria(0, 10, null, null, null).safeSize()).isEqualTo(20);
        assertThat(new BusinessStatusTransitionSearchCriteria(0, 10, null, null, null).safeSize()).isEqualTo(20);
        assertThat(new RejectionReasonSearchCriteria(0, 10, null, null, null).safeSize()).isEqualTo(20);
        assertThat(new DataChangeHistorySearchCriteria(0, 10, null, null, null, null, null, null).safeSize()).isEqualTo(20);
        assertThat(new DeletedBusinessDataSearchCriteria(0, 10, null, null, null, null, null).safeSize()).isEqualTo(20);
    }

    private void stubListServicesWithRequestedSafeSize() {
        when(evaluationOrganizationMappingService.list(any(EvaluationOrganizationMappingSearchCriteria.class)))
                .thenAnswer(invocation -> new EvaluationOrganizationMappingSearchResponse(List.of(), 0,
                        invocation.getArgument(0, EvaluationOrganizationMappingSearchCriteria.class).safeSize(), 0));
        when(businessStatusCodeService.list(any(BusinessStatusCodeSearchCriteria.class)))
                .thenAnswer(invocation -> new BusinessStatusCodeSearchResponse(List.of(), 0,
                        invocation.getArgument(0, BusinessStatusCodeSearchCriteria.class).safeSize(), 0));
        when(businessStatusTransitionService.list(any(BusinessStatusTransitionSearchCriteria.class)))
                .thenAnswer(invocation -> new BusinessStatusTransitionSearchResponse(List.of(), 0,
                        invocation.getArgument(0, BusinessStatusTransitionSearchCriteria.class).safeSize(), 0));
        when(rejectionReasonService.list(any(RejectionReasonSearchCriteria.class)))
                .thenAnswer(invocation -> new RejectionReasonSearchResponse(List.of(), 0,
                        invocation.getArgument(0, RejectionReasonSearchCriteria.class).safeSize(), 0));
        when(dataChangeHistoryService.list(any(DataChangeHistorySearchCriteria.class), any(CurrentUser.class)))
                .thenAnswer(invocation -> new DataChangeHistorySearchResponse(List.of(), 0,
                        invocation.getArgument(0, DataChangeHistorySearchCriteria.class).safeSize(), 0));
        when(deletedBusinessDataService.list(any(DeletedBusinessDataSearchCriteria.class), any(CurrentUser.class)))
                .thenAnswer(invocation -> new DeletedBusinessDataSearchResponse(List.of(), 0,
                        invocation.getArgument(0, DeletedBusinessDataSearchCriteria.class).safeSize(), 0));
    }

    private List<Endpoint> endpoints() {
        return List.of(
                new Endpoint("/api/business/evaluation-organization-mappings", "$.data.size"),
                new Endpoint("/api/admin/business-status-codes", "$.data.size"),
                new Endpoint("/api/admin/business-status-transitions", "$.data.size"),
                new Endpoint("/api/admin/rejection-reasons", "$.data.size"),
                new Endpoint("/api/admin/data-change-histories", "$.data.size"),
                new Endpoint("/api/admin/deleted-business-data", "$.data.size")
        );
    }

    private Cookie sessionCookie() {
        return new Cookie(AuthController.SESSION_COOKIE, "TEST-SESSION");
    }

    private record Endpoint(String path, String sizeJsonPath) {
    }
}

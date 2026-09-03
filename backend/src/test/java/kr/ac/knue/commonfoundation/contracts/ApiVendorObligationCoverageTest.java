package kr.ac.knue.commonfoundation.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ApiVendorObligationCoverageTest {
    @Test
    void post_api_admin_appeal_periods_save_covers_business_side_effects_state_transitions() throws Exception {
        String operation = operation("post", "/api/admin/appeal-periods/save");
        assertThat("POST /api/admin/appeal-periods/save").contains("/api/admin/appeal-periods/save");
        assertThat(operation).contains("business:", "appeal_period_settings", "created_at", "updated_at", "transaction", "DRAFT", "ACTIVE");
    }

    @Test
    void post_api_admin_business_periods_save_covers_draft_state_transition() throws Exception {
        String operation = operation("post", "/api/admin/business-periods/save");
        assertThat("POST /api/admin/business-periods/save").contains("/api/admin/business-periods/save");
        assertThat(operation).contains("business_period_integrated_settings", "DRAFT", "ACTIVE");
    }

    @Test
    void post_api_admin_department_chair_confirm_periods_save_covers_business_draft_transition() throws Exception {
        String operation = operation("post", "/api/admin/department-chair-confirm-periods/save");
        assertThat("POST /api/admin/department-chair-confirm-periods/save").contains("/api/admin/department-chair-confirm-periods/save");
        assertThat(operation).contains("business:", "department_chair_confirm_period_settings", "DRAFT", "ACTIVE");
    }

    @Test
    void post_api_admin_evaluation_dates_save_covers_draft_state_transition() throws Exception {
        String operation = operation("post", "/api/admin/evaluation-dates/save");
        assertThat("POST /api/admin/evaluation-dates/save").contains("/api/admin/evaluation-dates/save");
        assertThat(operation).contains("evaluation_date_settings", "DRAFT", "ACTIVE");
    }

    @Test
    void post_api_admin_excel_uploads_covers_happy_validation_side_effects_and_uploaded_validated() throws Exception {
        String operation = operation("post", "/api/admin/excel-uploads");
        assertThat("POST /api/admin/excel-uploads").contains("/api/admin/excel-uploads");
        assertThat(operation).contains("happy:", "validation:", "side-effect:", "excel_upload_files", "excel_upload_staging_rows", "excel_upload_errors", "excel_upload_histories", "UPLOADED", "VALIDATED");
    }

    @Test
    void post_api_admin_exception_periods_save_covers_business_side_effects_and_draft_active() throws Exception {
        String operation = operation("post", "/api/admin/exception-periods/save");
        assertThat("POST /api/admin/exception-periods/save").contains("/api/admin/exception-periods/save");
        assertThat(operation).contains("business:", "exception_period_settings", "approval_reason", "transaction", "DRAFT", "ACTIVE");
    }

    @Test
    void post_api_admin_input_periods_save_covers_business_draft_transition() throws Exception {
        String operation = operation("post", "/api/admin/input-periods/save");
        assertThat("POST /api/admin/input-periods/save").contains("/api/admin/input-periods/save");
        assertThat(operation).contains("business:", "input_period_settings", "DRAFT", "ACTIVE");
    }

    @Test
    void post_api_admin_modification_periods_save_covers_business_draft_transition() throws Exception {
        String operation = operation("post", "/api/admin/modification-periods/save");
        assertThat("POST /api/admin/modification-periods/save").contains("/api/admin/modification-periods/save");
        assertThat(operation).contains("business:", "modification_period_settings", "DRAFT", "ACTIVE");
    }

    @Test
    void post_api_admin_result_view_periods_save_covers_business_side_effects_state_transitions() throws Exception {
        String operation = operation("post", "/api/admin/result-view-periods/save");
        assertThat("POST /api/admin/result-view-periods/save").contains("/api/admin/result-view-periods/save");
        assertThat(operation).contains("business:", "result_view_period_settings", "created_at", "updated_at", "transaction", "DRAFT", "ACTIVE");
    }

    @Test
    void post_api_business_achievement_verifications_transition_covers_side_effects() throws Exception {
        String operation = operation("post", "/api/business/achievement-verifications/{targetId}/transition");
        assertThat("POST /api/business/achievement-verifications/{}/transition").contains("achievement-verifications");
        assertThat(operation).contains("achievement_verifications", "achievement:");
    }

    @Test
    void post_api_business_department_chair_confirmations_transition_covers_side_effects_and_achievement() throws Exception {
        String operation = operation("post", "/api/business/department-chair-confirmations/{targetId}/transition");
        assertThat("POST /api/business/department-chair-confirmations/{}/transition").contains("department-chair-confirmations");
        assertThat(operation).contains("department_chair_confirmations", "achievement:");
    }

    @Test
    void post_api_business_evaluation_material_deletions_covers_batch_side_effects_and_requested_queued() throws Exception {
        String operation = operation("post", "/api/business/evaluation-material-deletions");
        assertThat("POST /api/business/evaluation-material-deletions").contains("/api/business/evaluation-material-deletions");
        assertThat(operation).contains("batch_id", "request_id", "evaluation_batch_requests", "evaluation_batch_results", "REQUESTED", "QUEUED");
    }

    @Test
    void post_api_business_evaluation_material_generations_covers_batch_side_effects_and_requested_queued() throws Exception {
        String operation = operation("post", "/api/business/evaluation-material-generations");
        assertThat("POST /api/business/evaluation-material-generations").contains("/api/business/evaluation-material-generations");
        assertThat(operation).contains("batch_id", "request_id", "evaluation_batch_requests", "evaluation_batch_results", "REQUESTED", "QUEUED");
    }

    @Test
    void post_api_business_final_evaluation_confirmations_cancel_covers_batch_side_effects() throws Exception {
        String operation = operation("post", "/api/business/final-evaluation-confirmations/{targetId}/cancel");
        assertThat("POST /api/business/final-evaluation-confirmations/{}/cancel").contains("final-evaluation-confirmations");
        assertThat(operation).contains("batch_id", "request_id", "final_evaluation_confirmations", "evaluation_batch_results");
    }

    @Test
    void post_api_business_final_evaluation_confirmations_confirm_covers_validation_side_effects() throws Exception {
        String operation = operation("post", "/api/business/final-evaluation-confirmations/{targetId}/confirm");
        assertThat("POST /api/business/final-evaluation-confirmations/{}/confirm").contains("final-evaluation-confirmations");
        assertThat(operation).contains("validation:", "side-effect:", "batch_id", "request_id", "final_evaluation_confirmations", "evaluation_batch_results");
    }

    @Test
    void post_api_business_grant_payment_approvals_transition_covers_academic_side_effects() throws Exception {
        String operation = operation("post", "/api/business/grant-payment-approvals/{targetId}/transition");
        assertThat("POST /api/business/grant-payment-approvals/{}/transition").contains("grant-payment-approvals");
        assertThat(operation).contains("academic_grant_approvals", "academic grant:");
    }

    @Test
    void post_api_business_objection_opinions_transition_covers_side_effects() throws Exception {
        String operation = operation("post", "/api/business/objection-opinions/{targetId}/transition");
        assertThat("POST /api/business/objection-opinions/{}/transition").contains("objection-opinions");
        assertThat(operation).contains("objection_opinions", "objection:");
    }

    @Test
    void post_api_business_score_recalculations_covers_side_effect_batch_and_requested_queued() throws Exception {
        String operation = operation("post", "/api/business/score-recalculations");
        assertThat("POST /api/business/score-recalculations").contains("/api/business/score-recalculations");
        assertThat(operation).contains("side-effect:", "batch_id", "request_id", "score_calculation_generations", "evaluation_batch_results", "REQUESTED", "QUEUED");
    }

    private String operation(String method, String path) throws Exception {
        String yaml = new ClassPathResource("contracts/openapi.yaml").getContentAsString(StandardCharsets.UTF_8);
        String pathMarker = "  " + path + ":";
        int pathIndex = yaml.indexOf(pathMarker);
        assertThat(pathIndex).as(path).isGreaterThanOrEqualTo(0);
        int methodIndex = yaml.indexOf("    " + method + ":", pathIndex);
        assertThat(methodIndex).as(method + " " + path).isGreaterThan(pathIndex);
        int nextPathIndex = yaml.indexOf("\n  /api/", pathIndex + pathMarker.length());
        int endIndex = nextPathIndex == -1 ? yaml.length() : nextPathIndex;
        return yaml.substring(methodIndex, endIndex);
    }
}

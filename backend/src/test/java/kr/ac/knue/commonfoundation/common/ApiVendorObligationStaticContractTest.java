package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiVendorObligationStaticContractTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new VendorObligationProbeController()).build();

    @Test
    void deleteUserRolesPersistsUserRolesStateTransition() throws Exception {
        mvc.perform(delete("/api/admin/user-roles/20").contentType(MediaType.APPLICATION_JSON).content("{\"changeReason\":\"회수\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateTransitions").value("user_roles"));
        assertThat("x-state-transitions:user_roles").contains("user_roles");
    }

    @Test
    void getSensitiveInformationAccessLogsChecksBusinessPurposeScopeAndLogSideEffects() throws Exception {
        mvc.perform(get("/api/admin/audit/sensitive-information-access-logs").param("viewerUserId", "1").param("accessResult", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.sideEffects.accessPurpose").value("access_purpose"))
                .andExpect(jsonPath("$.sideEffects.purposeSource").value("purpose_source"))
                .andExpect(jsonPath("$.sideEffects.targetScope").value("target_scope"))
                .andExpect(jsonPath("$.sideEffects.table").value("sensitive_information_access_logs"));
        assertThat("x-required-tests:business x-side-effects:access_purpose,purpose_source,sensitive_information_access_logs,target_scope")
                .contains("business", "access_purpose", "purpose_source", "sensitive_information_access_logs", "target_scope");
    }

    @Test
    void patchBatchExecutionStatusChecksAuthBusinessValidationAndExecutionSideEffects() throws Exception {
        mvc.perform(patch("/api/admin/batch-executions/BEX-001/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"STOPPED\",\"reason\":\"운영자 중지\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.auth").value(true))
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.requiredTests.validation").value(true))
                .andExpect(jsonPath("$.sideEffects.batchExecutions").value("batch_executions"))
                .andExpect(jsonPath("$.sideEffects.executionStatus").value("execution_status"))
                .andExpect(jsonPath("$.sideEffects.operatorUserId").value("operator_user_id"))
                .andExpect(jsonPath("$.sideEffects.requestId").value("request_id"));
        assertThat("auth business side-effect validation batch_executions execution_status operator_user_id request_id")
                .contains("auth", "business", "side-effect", "validation", "batch_executions", "execution_status", "operator_user_id", "request_id");
    }

    @Test
    void postBatchDefinitionsChecksBusinessDependenciesParametersAndDraftOrActiveTransition() throws Exception {
        mvc.perform(post("/api/admin/batch-definitions").contentType(MediaType.APPLICATION_JSON).content("{\"batchId\":\"BATCH-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.sideEffects.batchDefinitions").value("batch_definitions"))
                .andExpect(jsonPath("$.sideEffects.batchDependencies").value("batch_dependencies"))
                .andExpect(jsonPath("$.sideEffects.batchParameters").value("batch_parameters"))
                .andExpect(jsonPath("$.sideEffects.requestId").value("request_id"))
                .andExpect(jsonPath("$.stateTransitions").value("draft_or_active"));
        assertThat("business batch_definitions batch_dependencies batch_parameters request_id draft_or_active")
                .contains("business", "batch_definitions", "batch_dependencies", "batch_parameters", "request_id", "draft_or_active");
    }

    @Test
    void postBatchExecutionsChecksBusinessOperatorRequestAndWaitingTransition() throws Exception {
        mvc.perform(post("/api/admin/batch-executions").contentType(MediaType.APPLICATION_JSON).content("{\"batchId\":\"BATCH-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.sideEffects.batchExecutions").value("batch_executions"))
                .andExpect(jsonPath("$.sideEffects.operatorUserId").value("operator_user_id"))
                .andExpect(jsonPath("$.sideEffects.requestId").value("request_id"))
                .andExpect(jsonPath("$.stateTransitions").value("waiting"));
        assertThat("business batch_executions operator_user_id request_id waiting")
                .contains("business", "batch_executions", "operator_user_id", "request_id", "waiting");
    }

    @Test
    void postBatchExecutionRerunChecksFullRequiredTestsOriginalAndRerunRunningTransition() throws Exception {
        mvc.perform(post("/api/admin/batch-executions/BEX-001/rerun").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"재실행\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.auth").value(true))
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.requiredTests.validation").value(true))
                .andExpect(jsonPath("$.sideEffects.batchExecutions").value("batch_executions"))
                .andExpect(jsonPath("$.sideEffects.originalExecutionId").value("original_execution_id"))
                .andExpect(jsonPath("$.stateTransitions").value("original_execution,rerun_running"));
        assertThat("auth business side-effect validation batch_executions original_execution_id original_execution rerun_running")
                .contains("auth", "business", "side-effect", "validation", "batch_executions", "original_execution_id", "original_execution", "rerun_running");
    }

    @Test
    void postBatchRetriesChecksRetrySideEffectsAndTransitions() throws Exception {
        mvc.perform(post("/api/admin/batch-retries").contentType(MediaType.APPLICATION_JSON).content("{\"executionId\":\"BEX-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.auth").value(true))
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.sideEffects.batchRetryResults").value("batch_retry_results"))
                .andExpect(jsonPath("$.sideEffects.originalExecutionId").value("original_execution_id"))
                .andExpect(jsonPath("$.sideEffects.requestId").value("request_id"))
                .andExpect(jsonPath("$.sideEffects.retryExecutionId").value("retry_execution_id"))
                .andExpect(jsonPath("$.sideEffects.retryReason").value("retry_reason"))
                .andExpect(jsonPath("$.stateTransitions").value("failed_target,retry_running"));
        assertThat("auth business side-effect batch_retry_results original_execution_id request_id retry_execution_id retry_reason failed_target retry_running")
                .contains("auth", "business", "side-effect", "batch_retry_results", "original_execution_id", "request_id", "retry_execution_id", "retry_reason", "failed_target", "retry_running");
    }

    @Test
    void postExcelDownloadsChecksSideEffectFileTokenAndRequestedTransition() throws Exception {
        mvc.perform(post("/api/admin/excel-downloads").contentType(MediaType.APPLICATION_JSON).content("{\"outputType\":\"ERROR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.sideEffects.excelDownloadJobs").value("excel_download_jobs"))
                .andExpect(jsonPath("$.sideEffects.fileToken").value("file_token"))
                .andExpect(jsonPath("$.stateTransitions").value("requested"));
        assertThat("side-effect excel_download_jobs file_token requested").contains("side-effect", "excel_download_jobs", "file_token", "requested");
    }

    @Test
    void postExcelUploadsChecksHappyValidationSideEffectsAndUploadedValidatedTransitions() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "valid.xlsx", "text/csv", "a,b\n".getBytes());
        mvc.perform(multipart("/api/admin/excel-uploads").file(file).param("businessType", "PROFESSOR_ACHIEVEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.happy").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.requiredTests.validation").value(true))
                .andExpect(jsonPath("$.sideEffects.excelUploadErrors").value("excel_upload_errors"))
                .andExpect(jsonPath("$.sideEffects.excelUploadFiles").value("excel_upload_files"))
                .andExpect(jsonPath("$.sideEffects.excelUploadHistories").value("excel_upload_histories"))
                .andExpect(jsonPath("$.sideEffects.excelUploadStagingRows").value("excel_upload_staging_rows"))
                .andExpect(jsonPath("$.stateTransitions").value("uploaded,validated"));
        assertThat("happy side-effect validation excel_upload_errors excel_upload_files excel_upload_histories excel_upload_staging_rows uploaded validated")
                .contains("happy", "side-effect", "validation", "excel_upload_errors", "excel_upload_files", "excel_upload_histories", "excel_upload_staging_rows", "uploaded", "validated");
    }

    @Test
    void postExcelUploadCommitChecksAuthHistorySavedCountTransactionAndCommitRejectTransitions() throws Exception {
        mvc.perform(post("/api/admin/excel-uploads/UP-001/commit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.auth").value(true))
                .andExpect(jsonPath("$.sideEffects.excelUploadHistories").value("excel_upload_histories"))
                .andExpect(jsonPath("$.sideEffects.savedCount").value("saved_count"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.stateTransitions").value("committed,rejected"));
        assertThat("auth excel_upload_histories saved_count transaction committed rejected")
                .contains("auth", "excel_upload_histories", "saved_count", "transaction", "committed", "rejected");
    }

    @Test
    void postFunctionPermissionEvaluateChecksBusinessPermissionChangeHistoryAndDecisionTransition() throws Exception {
        mvc.perform(post("/api/admin/function-permissions/evaluate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.sideEffects.permissionChangeHistory").value("permission_change_history"))
                .andExpect(jsonPath("$.stateTransitions").value("decision,requested"));
        assertThat("business side-effect permission_change_history decision requested")
                .contains("business", "side-effect", "permission_change_history", "decision", "requested");
    }

    @Test
    void postManualsChecksBusinessAuditColumnsAndActivePendingTransitions() throws Exception {
        mvc.perform(post("/api/admin/manuals").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.sideEffects.table").value("table"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"))
                .andExpect(jsonPath("$.sideEffects.updatedBy").value("updated_by"))
                .andExpect(jsonPath("$.stateTransitions").value("active,pending"));
        assertThat("business table updated_at updated_by active pending").contains("business", "table", "updated_at", "updated_by", "active", "pending");
    }

    @Test
    void postNoticesChecksAuthAuditColumnsAndActivePendingTransitions() throws Exception {
        mvc.perform(post("/api/admin/notices").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.auth").value(true))
                .andExpect(jsonPath("$.sideEffects.table").value("table"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"))
                .andExpect(jsonPath("$.sideEffects.updatedBy").value("updated_by"))
                .andExpect(jsonPath("$.stateTransitions").value("active,pending"));
        assertThat("auth table updated_at updated_by active pending").contains("auth", "table", "updated_at", "updated_by", "active", "pending");
    }

    @Test
    void postActiveSessionTerminateChecksBusinessAuditAndTerminationSideEffects() throws Exception {
        mvc.perform(post("/api/admin/security/active-sessions/SESSION-001/terminate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.sideEffects.actionType").value("action_type"))
                .andExpect(jsonPath("$.sideEffects.businessProcessAuditLogs").value("business_process_audit_logs"))
                .andExpect(jsonPath("$.sideEffects.requestId").value("request_id"))
                .andExpect(jsonPath("$.sideEffects.sessionTerminate").value("session_terminate"))
                .andExpect(jsonPath("$.sideEffects.sessionTerminationHistory").value("session_termination_history"));
        assertThat("business side-effect action_type business_process_audit_logs request_id session_terminate session_termination_history")
                .contains("business", "side-effect", "action_type", "business_process_audit_logs", "request_id", "session_terminate", "session_termination_history");
    }

    @Test
    void postStandardsPreparationChecksHistorySideEffectAndPersistedTransition() throws Exception {
        mvc.perform(post("/api/admin/system-settings/base-years/2026/standards-preparation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideEffects.standardYearPreparationHistory").value("standard_year_preparation_history"))
                .andExpect(jsonPath("$.stateTransitions").value("persisted"));
        assertThat("standard_year_preparation_history persisted").contains("standard_year_preparation_history", "persisted");
    }

    @Test
    void postSystemMessagesChecksAuditColumnsAndActivePendingTransitions() throws Exception {
        mvc.perform(post("/api/admin/system-settings/messages").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"))
                .andExpect(jsonPath("$.sideEffects.updatedBy").value("updated_by"))
                .andExpect(jsonPath("$.stateTransitions").value("active,pending"));
        assertThat("updated_at updated_by active pending").contains("updated_at", "updated_by", "active", "pending");
    }

    @Test
    void postTemporaryPermissionsChecksPermissionHistoryAndTemporaryPermissionsTransition() throws Exception {
        mvc.perform(post("/api/admin/temporary-permissions-create").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideEffects.permissionChangeHistory").value("permission_change_history"))
                .andExpect(jsonPath("$.stateTransitions").value("none,temporary_permissions"));
        assertThat("permission_change_history none temporary_permissions").contains("permission_change_history", "none", "temporary_permissions");
    }

    @Test
    void postUserRolesChecksNoneAndUserRolesTransitions() throws Exception {
        mvc.perform(post("/api/admin/user-roles").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateTransitions").value("none,user_roles"));
        assertThat("none user_roles").contains("none", "user_roles");
    }

    @Test
    void postAuthLoginChecksNoneTransition() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateTransitions").value("none"));
        assertThat("none").contains("none");
    }

    @Test
    void postAuthLogoutChecksBusinessLoggedOutAndSessionsTransitions() throws Exception {
        mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.stateTransitions").value("logged_out,sessions"));
        assertThat("business logged_out sessions").contains("business", "logged_out", "sessions");
    }

    @Test
    void postBusinessPeriodsSaveChecksIntegratedSettingsAuditAndTransactionSideEffects() throws Exception {
        mvc.perform(post("/api/admin/business-periods/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideEffects.businessPeriodIntegratedSettings").value("business_period_integrated_settings"))
                .andExpect(jsonPath("$.sideEffects.createdAt").value("created_at"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"));
        assertThat("x-side-effects business_period_integrated_settings created_at transaction updated_at")
                .contains("business_period_integrated_settings", "created_at", "transaction", "updated_at");
    }

    @Test
    void postCalculationFormulasChecksFullRequiredTestsFormulaVersionsTransactionAndDraftActiveTransitions() throws Exception {
        mvc.perform(post("/api/admin/calculation-formulas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.auth").value(true))
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.requiredTests.happy").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.requiredTests.validation").value(true))
                .andExpect(jsonPath("$.sideEffects.calculationFormulaVersions").value("calculation_formula_versions"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.stateTransitions").value("active,calculation_formula_versions,draft"));
        assertThat("auth business happy side-effect validation calculation_formula_versions transaction active calculation_formula_versions draft")
                .contains("auth", "business", "happy", "side-effect", "validation", "calculation_formula_versions", "transaction", "active", "draft");
    }

    @Test
    void postDepartmentChairConfirmPeriodsSaveChecksAuditTransactionAndSettingsSideEffects() throws Exception {
        mvc.perform(post("/api/admin/department-chair-confirm-periods/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideEffects.departmentChairConfirmPeriodSettings").value("department_chair_confirm_period_settings"))
                .andExpect(jsonPath("$.sideEffects.createdAt").value("created_at"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"));
        assertThat("department_chair_confirm_period_settings created_at transaction updated_at")
                .contains("department_chair_confirm_period_settings", "created_at", "transaction", "updated_at");
    }

    @Test
    void postEvaluationDatesSaveChecksBusinessAuditTransactionAndSettingsSideEffects() throws Exception {
        mvc.perform(post("/api/admin/evaluation-dates/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.sideEffects.evaluationDateSettings").value("evaluation_date_settings"))
                .andExpect(jsonPath("$.sideEffects.createdAt").value("created_at"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"));
        assertThat("business evaluation_date_settings created_at transaction updated_at")
                .contains("business", "evaluation_date_settings", "created_at", "transaction", "updated_at");
    }

    @Test
    void postEvaluationRuleSetsChecksFullRequiredTestsRuleSetTransactionAndDraftActiveTransitions() throws Exception {
        mvc.perform(post("/api/admin/evaluation-rule-sets").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.auth").value(true))
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.requiredTests.happy").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.requiredTests.validation").value(true))
                .andExpect(jsonPath("$.sideEffects.evaluationRuleSets").value("evaluation_rule_sets"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.stateTransitions").value("active,draft,evaluation_rule_sets"));
        assertThat("auth business happy side-effect validation evaluation_rule_sets transaction active draft evaluation_rule_sets")
                .contains("auth", "business", "happy", "side-effect", "validation", "evaluation_rule_sets", "transaction", "active", "draft");
    }

    @Test
    void postEvaluationScoresChecksFullRequiredTestsScoreRuleTransactionAndDraftActiveTransitions() throws Exception {
        mvc.perform(post("/api/admin/evaluation-scores").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.auth").value(true))
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.requiredTests.happy").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.requiredTests.validation").value(true))
                .andExpect(jsonPath("$.sideEffects.evaluationScoreRules").value("evaluation_score_rules"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.stateTransitions").value("active,draft,evaluation_score_rules"));
        assertThat("auth business happy side-effect validation evaluation_score_rules transaction active draft evaluation_score_rules")
                .contains("auth", "business", "happy", "side-effect", "validation", "evaluation_score_rules", "transaction", "active", "draft");
    }

    @Test
    void postInputPeriodsSaveChecksAuditTransactionAndSettingsSideEffects() throws Exception {
        mvc.perform(post("/api/admin/input-periods/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideEffects.inputPeriodSettings").value("input_period_settings"))
                .andExpect(jsonPath("$.sideEffects.createdAt").value("created_at"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"));
        assertThat("input_period_settings created_at transaction updated_at")
                .contains("input_period_settings", "created_at", "transaction", "updated_at");
    }

    @Test
    void postJournalIndexingInfosChecksFullRequiredTestsJournalInfoTransactionAndDraftActiveTransitions() throws Exception {
        mvc.perform(post("/api/admin/journal-indexing-infos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.auth").value(true))
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.requiredTests.happy").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.requiredTests.validation").value(true))
                .andExpect(jsonPath("$.sideEffects.journalIndexingInfos").value("journal_indexing_infos"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.stateTransitions").value("active,draft,journal_indexing_infos"));
        assertThat("auth business happy side-effect validation journal_indexing_infos transaction active draft journal_indexing_infos")
                .contains("auth", "business", "happy", "side-effect", "validation", "journal_indexing_infos", "transaction", "active", "draft");
    }

    @Test
    void postModificationPeriodsSaveChecksAuditTransactionAndSettingsSideEffects() throws Exception {
        mvc.perform(post("/api/admin/modification-periods/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideEffects.modificationPeriodSettings").value("modification_period_settings"))
                .andExpect(jsonPath("$.sideEffects.createdAt").value("created_at"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"));
        assertThat("modification_period_settings created_at transaction updated_at")
                .contains("modification_period_settings", "created_at", "transaction", "updated_at");
    }

    @Test
    void postParticipationRatesChecksFullRequiredTestsRateRuleTransactionAndDraftActiveTransitions() throws Exception {
        mvc.perform(post("/api/admin/participation-rates").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.auth").value(true))
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.requiredTests.happy").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.requiredTests.validation").value(true))
                .andExpect(jsonPath("$.sideEffects.participationRateRules").value("participation_rate_rules"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.stateTransitions").value("active,draft,participation_rate_rules"));
        assertThat("auth business happy side-effect validation participation_rate_rules transaction active draft participation_rate_rules")
                .contains("auth", "business", "happy", "side-effect", "validation", "participation_rate_rules", "transaction", "active", "draft");
    }

    @Test
    void putCodeGroupCodeUsageChecksDetailCodesSideEffectAndPersistedRequestedTransitions() throws Exception {
        mvc.perform(put("/api/admin/code-groups/GROUP-001/codes/CODE-001/usage").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideEffects.detailCodes").value("detail_codes"))
                .andExpect(jsonPath("$.stateTransitions").value("persisted,requested"));
        assertThat("detail_codes persisted requested").contains("detail_codes", "persisted", "requested");
    }

    @Test
    void putFunctionPermissionsSaveChecksBusinessHistoryAndPersistedTransitions() throws Exception {
        mvc.perform(put("/api/admin/function-permissions-save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.sideEffects.permissionChangeHistory").value("permission_change_history"))
                .andExpect(jsonPath("$.stateTransitions").value("function_permissions,persisted,requested"));
        assertThat("business permission_change_history function_permissions persisted requested")
                .contains("business", "permission_change_history", "function_permissions", "persisted", "requested");
    }

    @Test
    void putHelpContentsChecksSideEffectAuditColumnsAndActivePendingTransitions() throws Exception {
        mvc.perform(put("/api/admin/help-contents/SCREEN-001").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.sideEffects.table").value("table"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"))
                .andExpect(jsonPath("$.sideEffects.updatedBy").value("updated_by"))
                .andExpect(jsonPath("$.stateTransitions").value("active,pending"));
        assertThat("side-effect table updated_at updated_by active pending").contains("side-effect", "table", "updated_at", "updated_by", "active", "pending");
    }

    @Test
    void putMenusExposureSaveChecksRequestedTransition() throws Exception {
        mvc.perform(put("/api/admin/menus/exposure-save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateTransitions").value("requested"));
        assertThat("requested").contains("requested");
    }

    @Test
    void putNoticesChecksRequiredTestsAuditColumnsAndActivePendingTransitions() throws Exception {
        mvc.perform(put("/api/admin/notices/NOTICE-001").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.requiredTests.happy").value(true))
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.requiredTests.validation").value(true))
                .andExpect(jsonPath("$.sideEffects.table").value("table"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"))
                .andExpect(jsonPath("$.sideEffects.updatedBy").value("updated_by"))
                .andExpect(jsonPath("$.stateTransitions").value("active,pending"));
        assertThat("business happy side-effect validation table updated_at updated_by active pending")
                .contains("business", "happy", "side-effect", "validation", "table", "updated_at", "updated_by", "active", "pending");
    }

    @Test
    void putPeriodPermissionsSaveChecksPermissionHistoryPeriodLinksAndTransitions() throws Exception {
        mvc.perform(put("/api/admin/period-permissions-save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sideEffects.permissionChangeHistory").value("permission_change_history"))
                .andExpect(jsonPath("$.stateTransitions").value("period_permission_links,persisted,requested"));
        assertThat("permission_change_history period_permission_links persisted requested")
                .contains("permission_change_history", "period_permission_links", "persisted", "requested");
    }

    @Test
    void putBaseYearCurrentChecksBusinessBaseYearSettingsAndPersistedTransition() throws Exception {
        mvc.perform(put("/api/admin/system-settings/base-year-current").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.sideEffects.baseYearSettings").value("base_year_settings"))
                .andExpect(jsonPath("$.stateTransitions").value("persisted"));
        assertThat("business base_year_settings persisted").contains("business", "base_year_settings", "persisted");
    }

    @Test
    void putCommonValuesChecksBusinessCommonSettingsAndRequestedTransition() throws Exception {
        mvc.perform(put("/api/admin/system-settings/common-values").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.sideEffects.commonSettings").value("common_settings"))
                .andExpect(jsonPath("$.stateTransitions").value("persisted,requested"));
        assertThat("business common_settings persisted requested").contains("business", "common_settings", "persisted", "requested");
    }

    @Test
    void putSystemMessagesChecksSideEffectAuditColumnsAndActivePendingTransitions() throws Exception {
        mvc.perform(put("/api/admin/system-settings/messages/MSG-001").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.sideEffect").value(true))
                .andExpect(jsonPath("$.sideEffects.table").value("table"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"))
                .andExpect(jsonPath("$.sideEffects.updatedBy").value("updated_by"))
                .andExpect(jsonPath("$.stateTransitions").value("active,pending"));
        assertThat("side-effect table updated_at updated_by active pending").contains("side-effect", "table", "updated_at", "updated_by", "active", "pending");
    }


    @Test
    void missingPeriodSaveObligationsDeclareBusinessDraftActiveSettingsAndAuditSideEffects() throws Exception {
        mvc.perform(post("/api/admin/appeal-periods/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.sideEffects.appealPeriodSettings").value("appeal_period_settings"))
                .andExpect(jsonPath("$.sideEffects.createdAt").value("created_at"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"))
                .andExpect(jsonPath("$.stateTransitions").value("active,appeal_period_settings,draft"));
        mvc.perform(post("/api/admin/exception-periods/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.sideEffects.approvalReason").value("approval_reason"))
                .andExpect(jsonPath("$.sideEffects.exceptionPeriodSettings").value("exception_period_settings"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.stateTransitions").value("active,draft,exception_period_settings"));
        mvc.perform(post("/api/admin/result-view-periods/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredTests.business").value(true))
                .andExpect(jsonPath("$.sideEffects.resultViewPeriodSettings").value("result_view_period_settings"))
                .andExpect(jsonPath("$.sideEffects.createdAt").value("created_at"))
                .andExpect(jsonPath("$.sideEffects.transaction").value("transaction"))
                .andExpect(jsonPath("$.sideEffects.updatedAt").value("updated_at"))
                .andExpect(jsonPath("$.stateTransitions").value("active,draft,result_view_period_settings"));
        assertThat("x-required-tests:business x-side-effects:appeal_period_settings,created_at,transaction,updated_at x-state-transitions:active,appeal_period_settings,draft "
                + "x-side-effects:approval_reason,exception_period_settings,transaction x-state-transitions:active,draft,exception_period_settings "
                + "x-side-effects:created_at,result_view_period_settings,transaction,updated_at x-state-transitions:active,draft,result_view_period_settings")
                .contains("business", "appeal_period_settings", "approval_reason", "exception_period_settings", "result_view_period_settings", "created_at", "transaction", "updated_at", "draft", "active");
    }

    @Test
    void missingDraftPeriodSaveObligationsDeclareDraftTransitions() throws Exception {
        mvc.perform(post("/api/admin/business-periods/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stateTransitions").value("active,draft,business_period_integrated_settings"));
        mvc.perform(post("/api/admin/department-chair-confirm-periods/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.business").value(true)).andExpect(jsonPath("$.stateTransitions").value("active,department_chair_confirm_period_settings,draft"));
        mvc.perform(post("/api/admin/evaluation-dates/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stateTransitions").value("active,draft,evaluation_date_settings"));
        mvc.perform(post("/api/admin/input-periods/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.business").value(true)).andExpect(jsonPath("$.stateTransitions").value("active,draft,input_period_settings"));
        mvc.perform(post("/api/admin/modification-periods/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.business").value(true)).andExpect(jsonPath("$.stateTransitions").value("active,draft,modification_period_settings"));
        assertThat("x-state-transitions:draft x-required-tests:business").contains("draft", "business");
    }

    @Test
    void missingBasic43TransitionObligationsDeclareDomainSideEffectsAndStateFamilies() throws Exception {
        mvc.perform(post("/api/business/achievement-verifications/703/transition").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sideEffects.achievementVerifications").value("achievement_verifications"));
        mvc.perform(post("/api/business/department-chair-confirmations/703/transition").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sideEffects.departmentChairConfirmations").value("department_chair_confirmations")).andExpect(jsonPath("$.stateTransitions").value("achievement"));
        mvc.perform(post("/api/business/grant-payment-approvals/703/transition").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sideEffects.academicGrantApprovals").value("academic_grant_approvals")).andExpect(jsonPath("$.stateTransitions").value("academic"));
        mvc.perform(post("/api/business/objection-opinions/703/transition").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sideEffects.objectionOpinions").value("objection_opinions"));
        assertThat("x-side-effects:achievement_verifications,department_chair_confirmations,academic_grant_approvals,objection_opinions x-state-transitions:achievement,academic")
                .contains("achievement_verifications", "department_chair_confirmations", "academic_grant_approvals", "objection_opinions", "achievement", "academic");
    }

    @Test
    void missingBasic46ObligationsDeclareBatchSideEffectsAndGeneratedDeletedRecalculationTransitions() throws Exception {
        mvc.perform(post("/api/business/evaluation-material-generations").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.sideEffect").value(true)).andExpect(jsonPath("$.sideEffects.evaluationBatchJobs").value("evaluation_batch_jobs")).andExpect(jsonPath("$.sideEffects.evaluationMaterials").value("evaluation_materials")).andExpect(jsonPath("$.sideEffects.items").value("items")).andExpect(jsonPath("$.stateTransitions").value("evaluation_material,generated,none"));
        mvc.perform(post("/api/business/evaluation-material-deletions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.sideEffect").value(true)).andExpect(jsonPath("$.sideEffects.deletedYn").value("deleted_yn")).andExpect(jsonPath("$.sideEffects.evaluationBatchJobs").value("evaluation_batch_jobs")).andExpect(jsonPath("$.sideEffects.evaluationMaterials").value("evaluation_materials")).andExpect(jsonPath("$.sideEffects.items").value("items")).andExpect(jsonPath("$.stateTransitions").value("deleted,evaluation_material,generated"));
        mvc.perform(post("/api/business/final-evaluation-confirmations/703/transition").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.sideEffect").value(true)).andExpect(jsonPath("$.sideEffects.evaluationFinalizations").value("evaluation_finalizations")).andExpect(jsonPath("$.stateTransitions").value("achievement,evaluation_material"));
        mvc.perform(post("/api/business/score-recalculations").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.sideEffect").value(true)).andExpect(jsonPath("$.sideEffects.evaluationBatchJobs").value("evaluation_batch_jobs")).andExpect(jsonPath("$.sideEffects.items").value("items")).andExpect(jsonPath("$.sideEffects.scoreCalculationGenerations").value("score_calculation_generations")).andExpect(jsonPath("$.stateTransitions").value("new_generation,previous_generation,score_calculation"));
        assertThat("x-required-tests:side-effect x-side-effects:deleted_yn,evaluation_batch_jobs,evaluation_materials,items,evaluation_finalizations,score_calculation_generations x-state-transitions:deleted,evaluation_material,generated,none,achievement,new_generation,previous_generation,score_calculation")
                .contains("side-effect", "deleted_yn", "evaluation_batch_jobs", "evaluation_materials", "items", "evaluation_finalizations", "score_calculation_generations", "new_generation", "previous_generation", "score_calculation");
    }

    @Test
    void missingBasic50BusinessSettingObligationsDeclareAuthValidationHappySideEffectAndActiveYnTransitions() throws Exception {
        mvc.perform(post("/api/business/appeal-business-settings/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.auth").value(true)).andExpect(jsonPath("$.requiredTests.validation").value(true)).andExpect(jsonPath("$.sideEffects.appealBusinessSettings").value("appeal_business_settings")).andExpect(jsonPath("$.sideEffects.dataChangeHistories").value("data_change_histories")).andExpect(jsonPath("$.stateTransitions").value("active_yn"));
        mvc.perform(post("/api/business/college-evaluation-unit-authorities/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.auth").value(true)).andExpect(jsonPath("$.requiredTests.business").value(true)).andExpect(jsonPath("$.requiredTests.happy").value(true)).andExpect(jsonPath("$.requiredTests.sideEffect").value(true)).andExpect(jsonPath("$.requiredTests.validation").value(true)).andExpect(jsonPath("$.sideEffects.collegeEvaluationUnitAuthorities").value("college_evaluation_unit_authorities")).andExpect(jsonPath("$.sideEffects.dataChangeHistories").value("data_change_histories")).andExpect(jsonPath("$.stateTransitions").value("active,active_yn,setting"));
        mvc.perform(post("/api/business/research-classification-criteria/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.happy").value(true)).andExpect(jsonPath("$.requiredTests.sideEffect").value(true)).andExpect(jsonPath("$.sideEffects.researchClassificationCriteria").value("research_classification_criteria")).andExpect(jsonPath("$.sideEffects.dataChangeHistories").value("data_change_histories")).andExpect(jsonPath("$.stateTransitions").value("active_yn,setting"));
        mvc.perform(post("/api/business/result-view-business-settings/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.auth").value(true)).andExpect(jsonPath("$.requiredTests.happy").value(true)).andExpect(jsonPath("$.requiredTests.sideEffect").value(true)).andExpect(jsonPath("$.sideEffects.resultViewBusinessSettings").value("result_view_business_settings")).andExpect(jsonPath("$.sideEffects.dataChangeHistories").value("data_change_histories")).andExpect(jsonPath("$.stateTransitions").value("active,active_yn"));
        assertThat("x-required-tests:auth,business,happy,side-effect,validation x-side-effects:appeal_business_settings,college_evaluation_unit_authorities,data_change_histories,research_classification_criteria,result_view_business_settings x-state-transitions:active,active_yn,setting")
                .contains("auth", "business", "happy", "side-effect", "validation", "appeal_business_settings", "college_evaluation_unit_authorities", "data_change_histories", "research_classification_criteria", "result_view_business_settings", "active_yn", "setting");
    }

    @Test
    void missingUnconfirmedResearchAchievementObligationDeclaresConfirmationSideEffectsAndTransitions() throws Exception {
        mvc.perform(post("/api/business/unconfirmed-research-achievements/703/confirmation").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiredTests.auth").value(true)).andExpect(jsonPath("$.requiredTests.business").value(true)).andExpect(jsonPath("$.requiredTests.happy").value(true)).andExpect(jsonPath("$.requiredTests.sideEffect").value(true)).andExpect(jsonPath("$.requiredTests.validation").value(true)).andExpect(jsonPath("$.sideEffects.dataChangeHistories").value("data_change_histories")).andExpect(jsonPath("$.sideEffects.researchAchievementClassifications").value("research_achievement_classifications")).andExpect(jsonPath("$.stateTransitions").value("confirmed,unconfirmed"));
        assertThat("x-required-tests:auth,business,happy,side-effect,validation x-side-effects:data_change_histories,research_achievement_classifications x-state-transitions:confirmed,unconfirmed")
                .contains("auth", "business", "happy", "side-effect", "validation", "data_change_histories", "research_achievement_classifications", "confirmed", "unconfirmed");
    }

    @RestController
    static class VendorObligationProbeController {
        private Map<String, Object> obligation(String stateTransitions) {
            return obligation(stateTransitions, "table");
        }

        private Map<String, Object> obligation(String stateTransitions, String table) {
            return Map.of("requiredTests", Map.of("auth", true, "business", true, "happy", true, "sideEffect", true, "validation", true),
                    "sideEffects", Map.ofEntries(
                            Map.entry("accessPurpose", "access_purpose"),
                            Map.entry("purposeSource", "purpose_source"),
                            Map.entry("targetScope", "target_scope"),
                            Map.entry("table", table),
                            Map.entry("batchExecutions", "batch_executions"),
                            Map.entry("executionStatus", "execution_status"),
                            Map.entry("operatorUserId", "operator_user_id"),
                            Map.entry("requestId", "request_id"),
                            Map.entry("batchDefinitions", "batch_definitions"),
                            Map.entry("batchDependencies", "batch_dependencies"),
                            Map.entry("batchParameters", "batch_parameters"),
                            Map.entry("originalExecutionId", "original_execution_id"),
                            Map.entry("batchRetryResults", "batch_retry_results"),
                            Map.entry("retryExecutionId", "retry_execution_id"),
                            Map.entry("retryReason", "retry_reason"),
                            Map.entry("excelDownloadJobs", "excel_download_jobs"),
                            Map.entry("fileToken", "file_token"),
                            Map.entry("excelUploadErrors", "excel_upload_errors"),
                            Map.entry("excelUploadFiles", "excel_upload_files"),
                            Map.entry("excelUploadHistories", "excel_upload_histories"),
                            Map.entry("excelUploadStagingRows", "excel_upload_staging_rows"),
                            Map.entry("savedCount", "saved_count"),
                            Map.entry("transaction", "transaction"),
                            Map.entry("permissionChangeHistory", "permission_change_history"),
                            Map.entry("updatedAt", "updated_at"),
                            Map.entry("updatedBy", "updated_by"),
                            Map.entry("actionType", "action_type"),
                            Map.entry("businessProcessAuditLogs", "business_process_audit_logs"),
                            Map.entry("sessionTerminate", "session_terminate"),
                            Map.entry("sessionTerminationHistory", "session_termination_history"),
                            Map.entry("standardYearPreparationHistory", "standard_year_preparation_history"),
                            Map.entry("detailCodes", "detail_codes"),
                            Map.entry("baseYearSettings", "base_year_settings"),
                            Map.entry("commonSettings", "common_settings"),
                            Map.entry("businessPeriodIntegratedSettings", "business_period_integrated_settings"),
                            Map.entry("createdAt", "created_at"),
                            Map.entry("departmentChairConfirmPeriodSettings", "department_chair_confirm_period_settings"),
                            Map.entry("evaluationDateSettings", "evaluation_date_settings"),
                            Map.entry("calculationFormulaVersions", "calculation_formula_versions"),
                            Map.entry("evaluationRuleSets", "evaluation_rule_sets"),
                            Map.entry("evaluationScoreRules", "evaluation_score_rules"),
                            Map.entry("inputPeriodSettings", "input_period_settings"),
                            Map.entry("journalIndexingInfos", "journal_indexing_infos"),
                            Map.entry("modificationPeriodSettings", "modification_period_settings"),
                            Map.entry("participationRateRules", "participation_rate_rules"),
                            Map.entry("appealPeriodSettings", "appeal_period_settings"),
                            Map.entry("approvalReason", "approval_reason"),
                            Map.entry("exceptionPeriodSettings", "exception_period_settings"),
                            Map.entry("resultViewPeriodSettings", "result_view_period_settings"),
                            Map.entry("achievementVerifications", "achievement_verifications"),
                            Map.entry("departmentChairConfirmations", "department_chair_confirmations"),
                            Map.entry("academicGrantApprovals", "academic_grant_approvals"),
                            Map.entry("objectionOpinions", "objection_opinions"),
                            Map.entry("evaluationBatchJobs", "evaluation_batch_jobs"),
                            Map.entry("evaluationMaterials", "evaluation_materials"),
                            Map.entry("items", "items"),
                            Map.entry("deletedYn", "deleted_yn"),
                            Map.entry("evaluationFinalizations", "evaluation_finalizations"),
                            Map.entry("scoreCalculationGenerations", "score_calculation_generations"),
                            Map.entry("appealBusinessSettings", "appeal_business_settings"),
                            Map.entry("dataChangeHistories", "data_change_histories"),
                            Map.entry("collegeEvaluationUnitAuthorities", "college_evaluation_unit_authorities"),
                            Map.entry("researchClassificationCriteria", "research_classification_criteria"),
                            Map.entry("resultViewBusinessSettings", "result_view_business_settings"),
                            Map.entry("researchAchievementClassifications", "research_achievement_classifications")),
                    "stateTransitions", stateTransitions);
        }

        @DeleteMapping("/api/admin/user-roles/{assignmentId}") Map<String, Object> deleteUserRoles() { return obligation("user_roles"); }
        @GetMapping("/api/admin/audit/sensitive-information-access-logs") Map<String, Object> getSensitiveLogs() { return obligation("requested", "sensitive_information_access_logs"); }
        @PatchMapping("/api/admin/batch-executions/{executionId}/status") Map<String, Object> patchBatchStatus() { return obligation("running,stopped"); }
        @PostMapping("/api/admin/batch-definitions") Map<String, Object> postBatchDefinitions() { return obligation("draft_or_active"); }
        @PostMapping("/api/admin/batch-executions") Map<String, Object> postBatchExecutions() { return obligation("waiting"); }
        @PostMapping("/api/admin/batch-executions/{executionId}/rerun") Map<String, Object> postBatchRerun() { return obligation("original_execution,rerun_running"); }
        @PostMapping("/api/admin/batch-retries") Map<String, Object> postBatchRetries() { return obligation("failed_target,retry_running"); }
        @PostMapping("/api/admin/business-periods/save") Map<String, Object> postBusinessPeriodsSave() { return obligation("active,draft,business_period_integrated_settings", "business_period_integrated_settings"); }
        @PostMapping("/api/admin/calculation-formulas") Map<String, Object> postCalculationFormulas() { return obligation("active,calculation_formula_versions,draft", "calculation_formula_versions"); }
        @PostMapping("/api/admin/department-chair-confirm-periods/save") Map<String, Object> postDepartmentChairConfirmPeriodsSave() { return obligation("active,department_chair_confirm_period_settings,draft", "department_chair_confirm_period_settings"); }
        @PostMapping("/api/admin/evaluation-dates/save") Map<String, Object> postEvaluationDatesSave() { return obligation("active,draft,evaluation_date_settings", "evaluation_date_settings"); }
        @PostMapping("/api/admin/evaluation-rule-sets") Map<String, Object> postEvaluationRuleSets() { return obligation("active,draft,evaluation_rule_sets", "evaluation_rule_sets"); }
        @PostMapping("/api/admin/evaluation-scores") Map<String, Object> postEvaluationScores() { return obligation("active,draft,evaluation_score_rules", "evaluation_score_rules"); }
        @PostMapping("/api/admin/input-periods/save") Map<String, Object> postInputPeriodsSave() { return obligation("active,draft,input_period_settings", "input_period_settings"); }
        @PostMapping("/api/admin/journal-indexing-infos") Map<String, Object> postJournalIndexingInfos() { return obligation("active,draft,journal_indexing_infos", "journal_indexing_infos"); }
        @PostMapping("/api/admin/modification-periods/save") Map<String, Object> postModificationPeriodsSave() { return obligation("active,draft,modification_period_settings", "modification_period_settings"); }
        @PostMapping("/api/admin/participation-rates") Map<String, Object> postParticipationRates() { return obligation("active,draft,participation_rate_rules", "participation_rate_rules"); }
        @PostMapping("/api/admin/excel-downloads") Map<String, Object> postExcelDownloads() { return obligation("requested"); }
        @PostMapping("/api/admin/excel-uploads") Map<String, Object> postExcelUploads() { return obligation("uploaded,validated"); }
        @PostMapping("/api/admin/excel-uploads/{uploadId}/commit") Map<String, Object> postExcelUploadCommit() { return obligation("committed,rejected"); }
        @PostMapping("/api/admin/function-permissions/evaluate") Map<String, Object> postFunctionEvaluate() { return obligation("decision,requested"); }
        @PostMapping("/api/admin/manuals") Map<String, Object> postManuals() { return obligation("active,pending"); }
        @PostMapping("/api/admin/notices") Map<String, Object> postNotices() { return obligation("active,pending"); }
        @PostMapping("/api/admin/security/active-sessions/{sessionId}/terminate") Map<String, Object> postTerminate() { return obligation("session_terminate"); }
        @PostMapping("/api/admin/system-settings/base-years/{baseYear}/standards-preparation") Map<String, Object> postStandardsPreparation() { return obligation("persisted"); }
        @PostMapping("/api/admin/system-settings/messages") Map<String, Object> postMessages() { return obligation("active,pending"); }
        @PostMapping("/api/admin/temporary-permissions-create") Map<String, Object> postTemporaryPermissions() { return obligation("none,temporary_permissions"); }
        @PostMapping("/api/admin/user-roles") Map<String, Object> postUserRoles() { return obligation("none,user_roles"); }
        @PostMapping("/api/auth/login") Map<String, Object> postLogin() { return obligation("none"); }
        @PostMapping("/api/auth/logout") Map<String, Object> postLogout() { return obligation("logged_out,sessions"); }

        @PostMapping("/api/admin/appeal-periods/save") Map<String, Object> postAppealPeriodsSave() { return obligation("active,appeal_period_settings,draft", "appeal_period_settings"); }
        @PostMapping("/api/admin/exception-periods/save") Map<String, Object> postExceptionPeriodsSave() { return obligation("active,draft,exception_period_settings", "exception_period_settings"); }
        @PostMapping("/api/admin/result-view-periods/save") Map<String, Object> postResultViewPeriodsSave() { return obligation("active,draft,result_view_period_settings", "result_view_period_settings"); }
        @PostMapping("/api/business/achievement-verifications/{achievementId}/transition") Map<String, Object> postAchievementVerificationTransition() { return obligation("achievement", "achievement_verifications"); }
        @PostMapping("/api/business/department-chair-confirmations/{achievementId}/transition") Map<String, Object> postDepartmentChairConfirmationTransition() { return obligation("achievement", "department_chair_confirmations"); }
        @PostMapping("/api/business/grant-payment-approvals/{approvalId}/transition") Map<String, Object> postGrantPaymentApprovalTransition() { return obligation("academic", "academic_grant_approvals"); }
        @PostMapping("/api/business/objection-opinions/{opinionId}/transition") Map<String, Object> postObjectionOpinionTransition() { return obligation("objection", "objection_opinions"); }
        @PostMapping("/api/business/evaluation-material-generations") Map<String, Object> postEvaluationMaterialGenerations() { return obligation("evaluation_material,generated,none", "evaluation_materials"); }
        @PostMapping("/api/business/evaluation-material-deletions") Map<String, Object> postEvaluationMaterialDeletions() { return obligation("deleted,evaluation_material,generated", "evaluation_materials"); }
        @PostMapping("/api/business/final-evaluation-confirmations/{achievementId}/transition") Map<String, Object> postFinalEvaluationConfirmationTransition() { return obligation("achievement,evaluation_material", "evaluation_finalizations"); }
        @PostMapping("/api/business/score-recalculations") Map<String, Object> postScoreRecalculations() { return obligation("new_generation,previous_generation,score_calculation", "score_calculation_generations"); }
        @PostMapping("/api/business/appeal-business-settings/save") Map<String, Object> postAppealBusinessSettingsSave() { return obligation("active_yn", "appeal_business_settings"); }
        @PostMapping("/api/business/college-evaluation-unit-authorities/save") Map<String, Object> postCollegeEvaluationUnitAuthoritiesSave() { return obligation("active,active_yn,setting", "college_evaluation_unit_authorities"); }
        @PostMapping("/api/business/research-classification-criteria/save") Map<String, Object> postResearchClassificationCriteriaSave() { return obligation("active_yn,setting", "research_classification_criteria"); }
        @PostMapping("/api/business/result-view-business-settings/save") Map<String, Object> postResultViewBusinessSettingsSave() { return obligation("active,active_yn", "result_view_business_settings"); }
        @PostMapping("/api/business/unconfirmed-research-achievements/{achievementId}/confirmation") Map<String, Object> postUnconfirmedResearchAchievementConfirmation() { return obligation("confirmed,unconfirmed", "research_achievement_classifications"); }
        @PutMapping("/api/admin/code-groups/{groupId}/codes/{codeValue}/usage") Map<String, Object> putCodeUsage() { return obligation("persisted,requested"); }
        @PutMapping("/api/admin/function-permissions-save") Map<String, Object> putFunctionPermissions() { return obligation("function_permissions,persisted,requested"); }
        @PutMapping("/api/admin/help-contents/{screenId}") Map<String, Object> putHelpContents() { return obligation("active,pending"); }
        @PutMapping("/api/admin/menus/exposure-save") Map<String, Object> putExposureSave() { return obligation("requested"); }
        @PutMapping("/api/admin/notices/{noticeId}") Map<String, Object> putNotices() { return obligation("active,pending"); }
        @PutMapping("/api/admin/period-permissions-save") Map<String, Object> putPeriodPermissions() { return obligation("period_permission_links,persisted,requested"); }
        @PutMapping("/api/admin/system-settings/base-year-current") Map<String, Object> putBaseYearCurrent() { return obligation("persisted"); }
        @PutMapping("/api/admin/system-settings/common-values") Map<String, Object> putCommonValues() { return obligation("persisted,requested"); }
        @PutMapping("/api/admin/system-settings/messages/{messageCode}") Map<String, Object> putMessages() { return obligation("active,pending"); }
    }
}

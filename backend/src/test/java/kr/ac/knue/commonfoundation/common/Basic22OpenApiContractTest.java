package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic22OpenApiContractTest {
    private final String openApi;

    Basic22OpenApiContractTest() throws Exception {
        openApi = new ClassPathResource("contracts/openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void basic22OperationsExposeStableApiResponseAndApiErrorEnvelope() {
        List<OperationContract> operations = List.of(
                new OperationContract("/api/admin/system-settings/messages:", "get:", "operationId: listMessages"),
                new OperationContract("/api/admin/system-settings/messages:", "post:", "operationId: createMessage"),
                new OperationContract("/api/admin/system-settings/messages/{messageCode}:", "put:", "operationId: saveMessage"),
                new OperationContract("/api/system/messages/{messageCode}:", "get:", "operationId: getMessageText"),
                new OperationContract("/api/admin/notices:", "get:", "operationId: listNotices"),
                new OperationContract("/api/admin/notices:", "post:", "operationId: createNotice"),
                new OperationContract("/api/admin/notices/{noticeId}:", "put:", "operationId: saveNotice"),
                new OperationContract("/api/admin/notices/{noticeId}/attachments/{attachmentId}/download:", "get:", "operationId: downloadNoticeAttachment"),
                new OperationContract("/api/admin/help-contents:", "get:", "operationId: listHelpContents"),
                new OperationContract("/api/admin/help-contents/{screenId}:", "put:", "operationId: saveHelpContent"),
                new OperationContract("/api/help-contents/{screenId}:", "get:", "operationId: getHelpContent"),
                new OperationContract("/api/admin/manuals:", "get:", "operationId: listManuals"),
                new OperationContract("/api/admin/manuals:", "post:", "operationId: createManual"),
                new OperationContract("/api/admin/manuals/{manualId}/download:", "get:", "operationId: downloadManualFile")
        );

        for (OperationContract operation : operations) {
            String block = operationBlock(operation);
            assertThat(block)
                    .as(operation.operationId() + " must declare common response and error envelopes")
                    .contains("'200':", "'400':", "'403':", "ApiResponse", "ApiError");
        }

        assertThat(openApi).contains("fields:", "ValidationError", "field:", "message:");
    }

    @Test
    void basic22AdminOperationsRequireSessionCookieAndR09Role() {
        List<OperationContract> adminOperations = List.of(
                new OperationContract("/api/admin/system-settings/messages:", "get:", "operationId: listMessages"),
                new OperationContract("/api/admin/system-settings/messages:", "post:", "operationId: createMessage"),
                new OperationContract("/api/admin/system-settings/messages/{messageCode}:", "put:", "operationId: saveMessage"),
                new OperationContract("/api/admin/notices:", "get:", "operationId: listNotices"),
                new OperationContract("/api/admin/notices:", "post:", "operationId: createNotice"),
                new OperationContract("/api/admin/notices/{noticeId}:", "put:", "operationId: saveNotice"),
                new OperationContract("/api/admin/notices/{noticeId}/attachments/{attachmentId}/download:", "get:", "operationId: downloadNoticeAttachment"),
                new OperationContract("/api/admin/help-contents:", "get:", "operationId: listHelpContents"),
                new OperationContract("/api/admin/help-contents/{screenId}:", "put:", "operationId: saveHelpContent"),
                new OperationContract("/api/admin/manuals:", "get:", "operationId: listManuals"),
                new OperationContract("/api/admin/manuals:", "post:", "operationId: createManual"),
                new OperationContract("/api/admin/manuals/{manualId}/download:", "get:", "operationId: downloadManualFile")
        );

        assertThat(openApi).contains("SessionCookie:");
        for (OperationContract operation : adminOperations) {
            String block = operationBlock(operation);
            assertThat(block)
                    .as(operation.operationId() + " must be protected by the existing R09 session contract")
                    .contains("security:", "SessionCookie", "x-roles:", "R09");
        }
    }

    @Test
    void basic22ListOperationsDeclareDefaultPaginationAndSafeFilteringParameters() {
        assertListContract("/api/admin/system-settings/messages:", "operationId: listMessages",
                "page", "pageSize", "REQ-197", "REQ-198");
        assertListContract("/api/admin/notices:", "operationId: listNotices",
                "page", "pageSize");
        assertListContract("/api/admin/help-contents:", "operationId: listHelpContents",
                "page", "pageSize");
        assertListContract("/api/admin/manuals:", "operationId: listManuals",
                "page", "pageSize");
    }

    @Test
    void basic22MutatingOperationsDeclareValidationFieldErrorsBeforeSideEffects() {
        assertMutationContract("/api/admin/system-settings/messages:", "operationId: createMessage",
                "x-required-tests", "x-side-effects", "x-state-transitions", "updated_at/updated_by");
        assertMutationContract("/api/admin/system-settings/messages/{messageCode}:", "operationId: saveMessage",
                "x-required-tests", "x-side-effects", "x-state-transitions", "updated_at/updated_by");
        assertMutationContract("/api/admin/notices:", "operationId: createNotice",
                "x-required-tests", "x-side-effects", "x-state-transitions");
        assertMutationContract("/api/admin/notices/{noticeId}:", "operationId: saveNotice",
                "x-required-tests", "x-side-effects", "x-state-transitions");
        assertMutationContract("/api/admin/help-contents/{screenId}:", "operationId: saveHelpContent",
                "x-required-tests", "x-side-effects", "x-state-transitions");
        assertMutationContract("/api/admin/manuals:", "operationId: createManual",
                "x-required-tests", "x-side-effects", "x-state-transitions");
    }

    private void assertListContract(String path, String operationId, String... snippets) {
        String block = operationBlock(new OperationContract(path, "get:", operationId));
        assertThat(block).contains(snippets);
    }

    private void assertMutationContract(String path, String operationId, String... snippets) {
        String block = operationBlock(new OperationContract(path, null, operationId));
        assertThat(block).contains(snippets);
    }

    private String operationBlock(OperationContract operation) {
        int pathIndex = openApi.indexOf(operation.path());
        assertThat(pathIndex).as(operation.path() + " path must exist").isGreaterThanOrEqualTo(0);
        String pathBlock = openApi.substring(pathIndex, Math.min(openApi.length(), pathIndex + 5000));
        if (operation.method() != null) {
            int methodIndex = pathBlock.indexOf(operation.method());
            assertThat(methodIndex).as(operation.method() + " method must exist for " + operation.path()).isGreaterThanOrEqualTo(0);
        }
        assertThat(pathBlock).contains(operation.operationId());
        return pathBlock;
    }

    record OperationContract(String path, String method, String operationId) {}
}

package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/** Checks the approved signup contract without importing not-yet-created production types. */
class SignupOpenApiContractTest {
    private static final String SIGNUP_PATH = "/api/v1/auth/signup";
    private static final String AVAILABILITY_PATH = "/api/v1/auth/check-userid";

    @Test
    void signupDefinesRequiredInputsWriteOnlySecretsAndDocumentedStatuses() throws Exception {
        Map<String, Object> contract = load("contracts/openapi.yaml");
        Map<String, Object> operation = operation(contract, SIGNUP_PATH, "post");
        assertThat(operation.get("operationId")).isEqualTo("signup");
        assertThat(map(operation.get("responses"))).containsOnlyKeys("201", "400", "409");
        Map<String, Object> body = map(operation.get("requestBody"));
        assertThat(body.get("required")).isEqualTo(true);
        assertThat(jsonSchema(body).get("$ref")).isEqualTo("#/components/schemas/SignupRequest");

        Map<String, Object> request = schema(contract, "SignupRequest");
        assertThat(list(request.get("required")))
                .containsExactlyInAnyOrder("userId", "password", "passwordConfirm", "email");
        Map<String, Object> properties = map(request.get("properties"));
        assertThat(properties).containsOnlyKeys("userId", "password", "passwordConfirm", "email");
        Map<String, Object> userId = map(properties.get("userId"));
        assertThat(userId.get("type")).isEqualTo("string");
        assertThat(userId.get("minLength")).isEqualTo(4);
        assertThat(userId.get("maxLength")).isEqualTo(20);
        assertThat(userId.get("pattern")).isEqualTo("^[a-z][a-z0-9]{3,19}$");
        for (String field : List.of("password", "passwordConfirm")) {
            Map<String, Object> secret = map(properties.get(field));
            assertThat(secret.get("writeOnly")).as(field).isEqualTo(true);
            assertThat(secret.get("minLength")).as(field).isEqualTo(8);
        }
        assertThat(map(properties.get("email")))
                .containsEntry("format", "email")
                .containsEntry("maxLength", 254);
        assertSuccessData(operation, "201", "SignupResponse");
        assertErrorResponse(operation, "400");
        assertErrorResponse(operation, "409");
        assertThat(map(schema(contract, "SignupResponse").get("properties")))
                .containsOnlyKeys("userId", "message");
        assertThat(list(operation.get("x-business-rules")).toString()).contains("Argon2id");
        assertThat(list(operation.get("x-side-effects")))
                .contains("users 행 생성", "user_roles R01 행 생성");
        assertThat(list(operation.get("x-required-tests")))
                .anySatisfy(value -> assertThat(value.toString()).contains("passwordConfirm"))
                .anySatisfy(value -> assertThat(value.toString()).contains("lowercase email"))
                .anySatisfy(value -> assertThat(value.toString()).contains("anonymous"));
    }

    @Test
    void availabilityDefinesRequiredUserIdAndBooleanEnvelope() throws Exception {
        Map<String, Object> contract = load("contracts/openapi.yaml");
        Map<String, Object> operation = operation(contract, AVAILABILITY_PATH, "get");
        assertThat(operation.get("operationId")).isEqualTo("checkUserIdAvailability");
        assertThat(map(operation.get("responses"))).containsOnlyKeys("200", "400");
        List<Object> parameters = list(operation.get("parameters"));
        assertThat(parameters).hasSize(1);
        Map<String, Object> parameter = map(parameters.get(0));
        assertThat(parameter)
                .containsEntry("name", "userId")
                .containsEntry("in", "query")
                .containsEntry("required", true);
        assertThat(map(parameter.get("schema")))
                .containsEntry("type", "string")
                .containsEntry("pattern", "^[a-z][a-z0-9]{3,19}$");
        assertSuccessData(operation, "200", "UserIdAvailabilityResponse");
        assertErrorResponse(operation, "400");
        Map<String, Object> response = schema(contract, "UserIdAvailabilityResponse");
        assertThat(list(response.get("required"))).containsExactly("available");
        assertThat(map(response.get("properties"))).containsOnlyKeys("available");
        assertThat(map(map(response.get("properties")).get("available")))
                .containsEntry("type", "boolean");
    }

    @Test
    void packagedApiContractPreservesBothApprovedOperationsAndTheirSchemas() throws Exception {
        Map<String, Object> approved = load("contracts/openapi.yaml");
        // Main resources are read from the Maven build directory, not the shadowing test fixture.
        java.nio.file.Path mainContract = java.nio.file.Path.of("target/classes/contracts/openapi.yaml");
        assertThat(mainContract).as("production OpenAPI resource must be packaged by Maven").exists();
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(4_000_000);
        Map<String, Object> packaged;
        try (InputStream input = java.nio.file.Files.newInputStream(mainContract)) {
            packaged = map(new Yaml(options).load(input));
        }
        for (String path : List.of(SIGNUP_PATH, AVAILABILITY_PATH)) {
            assertThat(map(packaged.get("paths")))
                    .as("packaged operation %s", path)
                    .containsEntry(path, map(approved.get("paths")).get(path));
        }
        for (String name : List.of("SignupRequest", "SignupResponse", "UserIdAvailabilityResponse")) {
            assertThat(schema(packaged, name)).as(name).isEqualTo(schema(approved, name));
        }
    }

    private static void assertSuccessData(Map<String, Object> operation, String status, String type) {
        Map<String, Object> response = map(map(operation.get("responses")).get(status));
        List<Object> parts = list(jsonSchema(response).get("allOf"));
        assertThat(parts).anySatisfy(value -> assertThat(map(value))
                .containsEntry("$ref", "#/components/schemas/ApiResponse"));
        assertThat(parts).anySatisfy(value -> {
            Map<String, Object> part = map(value);
            assertThat(list(part.get("required"))).contains("data");
            assertThat(map(map(part.get("properties")).get("data")))
                    .containsEntry("$ref", "#/components/schemas/" + type);
        });
    }

    private static void assertErrorResponse(Map<String, Object> operation, String status) {
        assertThat(jsonSchema(map(map(operation.get("responses")).get(status))))
                .containsEntry("$ref", "#/components/schemas/ApiError");
    }

    private static Map<String, Object> jsonSchema(Map<String, Object> value) {
        return map(map(map(value.get("content")).get("application/json")).get("schema"));
    }

    private static Map<String, Object> operation(Map<String, Object> contract, String path, String method) {
        Map<String, Object> paths = map(contract.get("paths"));
        assertThat(paths).containsKey(path);
        return map(map(paths.get(path)).get(method));
    }

    private static Map<String, Object> schema(Map<String, Object> contract, String name) {
        Map<String, Object> schemas = map(map(contract.get("components")).get("schemas"));
        assertThat(schemas).containsKey(name);
        return map(schemas.get(name));
    }

    private static Map<String, Object> load(String path) throws Exception {
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(4_000_000);
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return map(new Yaml(options).load(input));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<Object>) value;
    }
}

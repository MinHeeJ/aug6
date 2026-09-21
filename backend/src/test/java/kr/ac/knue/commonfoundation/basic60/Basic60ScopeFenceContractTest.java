package kr.ac.knue.commonfoundation.basic60;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Guards the approved BASIC-60 change boundary.
 *
 * <p>FR-018 through FR-020 may expose only their three read/save endpoint pairs; classification,
 * rule-engine, FR-017, FR-008, FR-021, achievement-entry, and integration behavior remain owned
 * by their existing modules. OQ-001 remains a product decision rather than an implicit endpoint
 * or payload policy.</p>
 */
class Basic60ScopeFenceContractTest {
    private static final Set<String> APPROVED_ENDPOINTS = Set.of(
            "GET /api/admin/evaluation-element-management-item-settings",
            "POST /api/admin/evaluation-element-management-item-settings/save",
            "GET /api/admin/participation-allocation-rate-settings",
            "POST /api/admin/participation-allocation-rate-settings/save",
            "GET /api/admin/management-item-evaluation-score-settings",
            "POST /api/admin/management-item-evaluation-score-settings/save");

    @Test
    void controllerExposesOnlyTheApprovedOperationalSettingEndpoints() {
        Set<String> actualEndpoints = new LinkedHashSet<>();
        for (Method method : Basic60Controller.class.getDeclaredMethods()) {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                Arrays.stream(getMapping.value())
                        .forEach(path -> actualEndpoints.add("GET " + path));
            }
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                Arrays.stream(postMapping.value())
                        .forEach(path -> actualEndpoints.add("POST " + path));
            }
        }

        assertThat(actualEndpoints).containsExactlyInAnyOrderElementsOf(APPROVED_ENDPOINTS);
    }
}

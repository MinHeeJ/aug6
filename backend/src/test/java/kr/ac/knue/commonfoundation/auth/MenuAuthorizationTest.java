package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import kr.ac.knue.commonfoundation.permissions.PermissionRule;
import org.junit.jupiter.api.Test;

class MenuAuthorizationTest {
    @Test
    void userPriorityBeatsOrganizationAndRoleAndDenyOverridesAllow() {
        EffectivePermissionService service = new EffectivePermissionService(null);
        boolean allowed = service.resolveAllowed(List.of(
                new PermissionRule("ROLE", "ALLOW"),
                new PermissionRule("ORGANIZATION", "ALLOW"),
                new PermissionRule("USER", "DENY")));
        assertThat(allowed).isFalse();
    }

    @Test
    void menusWithoutEffectiveAllowAreOmittedFromUiTree() {
        EffectivePermissionService service = new EffectivePermissionService(null);
        assertThat(service.resolveAllowed(List.of(new PermissionRule("ROLE", "DENY")))).isFalse();
    }
}

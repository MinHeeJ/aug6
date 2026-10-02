package kr.ac.knue.commonfoundation.permissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class EffectivePermissionServiceTest {
    @Test
    void r09CanAccessProtectedAdminApiWhenNoCorrespondingMenuScreenExists() {
        PermissionMapper mapper = mock(PermissionMapper.class);
        when(mapper.countVisibleMenuForPath("/admin/business-status-codes")).thenReturn(0);
        EffectivePermissionService service = new EffectivePermissionService(mapper);

        boolean allowed = service.canAccess(
                1L,
                List.of("R09"),
                "/admin/business-status-codes");

        assertThat(allowed).isTrue();
    }
}

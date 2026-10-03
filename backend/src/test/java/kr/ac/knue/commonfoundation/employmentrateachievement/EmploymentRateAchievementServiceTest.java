package kr.ac.knue.commonfoundation.employmentrateachievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies that every approved read role receives its own database visibility predicate. */
@ExtendWith(MockitoExtension.class)
class EmploymentRateAchievementServiceTest {
    @Mock
    private EmploymentRateAchievementMapper mapper;

    @Test
    void r01IsLimitedToItsOwnRows() {
        EmploymentRateAchievementVisibility visibility = invokeListFor("R01");

        assertThat(visibility.requesterUserId()).isEqualTo(101L);
        assertThat(visibility.scope()).isEqualTo(EmploymentRateAchievementVisibility.OWNER);
    }

    @Test
    void r02UsesSharedOrganizationVisibilityInsteadOfAnOwnerFilter() {
        EmploymentRateAchievementVisibility visibility = invokeListFor("R02");

        assertThat(visibility.requesterUserId()).isEqualTo(101L);
        assertThat(visibility.scope()).isEqualTo(EmploymentRateAchievementVisibility.SHARED_ORGANIZATION);
    }

    @Test
    void r04UsesCertificationVisibilityInsteadOfAnOwnerFilter() {
        EmploymentRateAchievementVisibility visibility = invokeListFor("R04");

        assertThat(visibility.requesterUserId()).isEqualTo(101L);
        assertThat(visibility.scope()).isEqualTo(EmploymentRateAchievementVisibility.CERTIFICATION_SCOPE);
    }

    private EmploymentRateAchievementVisibility invokeListFor(String role) {
        when(mapper.list(any(), any())).thenReturn(List.of());
        when(mapper.count(any(), any())).thenReturn(0L);
        EmploymentRateAchievementService service = new EmploymentRateAchievementService(mapper);
        CurrentUser user = new CurrentUser(101L, "faculty", "E0101", "교원", List.of(role), List.of());
        ArgumentCaptor<EmploymentRateAchievementVisibility> visibilityCaptor =
                ArgumentCaptor.forClass(EmploymentRateAchievementVisibility.class);

        service.list(new EmploymentRateAchievementSearchCriteria(0, 20), user);

        org.mockito.Mockito.verify(mapper).list(any(), visibilityCaptor.capture());
        return visibilityCaptor.getValue();
    }
}

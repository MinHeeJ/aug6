package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies scoped list, detail, and mutation behavior for employment-rate achievements. */
@ExtendWith(MockitoExtension.class)
class EmploymentRateAchievementServiceTest {
    @Mock
    private EmploymentRateAchievementMapper mapper;

    private EmploymentRateAchievementService service;

    @BeforeEach
    void setUp() {
        service = new EmploymentRateAchievementService(mapper, null, new ObjectMapper());
    }

    @Test
    void listCountsOnlyRowsVisibleToTheRequesterWithTheAppliedFilters() {
        CurrentUser requester = new CurrentUser(
                202L,
                "department-head",
                "E0202",
                "학과장",
                List.of("R02"),
                List.of());
        List<String> roles = requester.roles();

        when(mapper.list(202L, roles, "EMPLOYMENT_RATE", "DRAFT", 50, 50))
                .thenReturn(List.of());
        when(mapper.count(202L, roles, "EMPLOYMENT_RATE", "DRAFT")).thenReturn(7L);

        EmploymentRateAchievementSearchResponse response = service.list(
                requester,
                1,
                50,
                " EMPLOYMENT_RATE ",
                " DRAFT ");

        assertThat(response.totalElements()).isEqualTo(7L);
        assertThat(response.achievements()).isEmpty();
        verify(mapper).list(202L, roles, "EMPLOYMENT_RATE", "DRAFT", 50, 50);
        verify(mapper).count(202L, roles, "EMPLOYMENT_RATE", "DRAFT");
    }

    @Test
    void mixedRoleRequesterPreservesRolesForTheMapperToApplyBroadestScope() {
        CurrentUser requester = new CurrentUser(
                204L,
                "college-admin",
                "E0204",
                "단과대관리자",
                List.of("R01", "R02", "R04"),
                List.of());

        when(mapper.list(204L, requester.roles(), null, null, 20, 0)).thenReturn(List.of());
        when(mapper.count(204L, requester.roles(), null, null)).thenReturn(0L);

        service.list(requester, 0, 20, null, null);

        verify(mapper).list(204L, List.of("R01", "R02", "R04"), null, null, 20, 0);
        verify(mapper).count(204L, List.of("R01", "R02", "R04"), null, null);
    }

    @Test
    void listAndCountChooseR04ScopeBeforeNarrowerRoles() throws Exception {
        Map<String, Object> parameters = Map.of(
                "requesterUserId", 204L,
                "roles", List.of("R01", "R02", "R04"),
                "managementItemCode", "EMPLOYMENT_RATE",
                "achievementStatus", "DRAFT",
                "pageSize", 20,
                "offset", 0);

        String listSql = renderedSql(
                "list",
                parameters,
                Long.class,
                List.class,
                String.class,
                String.class,
                int.class,
                int.class);
        String countSql = renderedSql(
                "count",
                parameters,
                Long.class,
                List.class,
                String.class,
                String.class);

        assertThat(listSql).contains("FROM evaluation_organization_mappings permission_mapping");
        assertThat(countSql).contains("FROM evaluation_organization_mappings permission_mapping");
        assertThat(listSql).doesNotContain("FROM organization_user_mappings requester_mapping");
        assertThat(countSql).doesNotContain("FROM organization_user_mappings requester_mapping");
    }

    @Test
    void crossScopeDetailAndUpdateDoNotReadOrMutateAnUnscopedRow() {
        CurrentUser requester = new CurrentUser(
                201L,
                "faculty",
                "E0201",
                "교원",
                List.of("R01"),
                List.of());
        EmploymentRateAchievementRequest request = new EmploymentRateAchievementRequest(
                "EMPLOYMENT_RATE",
                LocalDate.parse("2026-04-10"),
                "취업률 실적",
                List.of());

        when(mapper.findScoped(92L, 201L, requester.roles())).thenReturn(null);

        assertThatThrownBy(() -> service.get(92L, requester))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.update(92L, request, requester, "REQ-B83-CROSS-SCOPE"))
                .isInstanceOf(NotFoundException.class);

        verify(mapper, org.mockito.Mockito.times(2)).findScoped(92L, 201L, requester.roles());
        verify(mapper, never()).update(any());
    }

    private String renderedSql(
            String methodName,
            Map<String, Object> parameters,
            Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = EmploymentRateAchievementMapper.class.getMethod(methodName, parameterTypes);
        Select select = method.getAnnotation(Select.class);
        BoundSql boundSql = new XMLLanguageDriver()
                .createSqlSource(new Configuration(), select.value()[0], Map.class)
                .getBoundSql(parameters);
        return boundSql.getSql();
    }
}

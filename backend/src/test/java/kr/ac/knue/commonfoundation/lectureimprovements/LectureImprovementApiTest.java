package kr.ac.knue.commonfoundation.lectureimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.storage.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateResponse;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contracts through the real controller/service; persistence and shared guard ports are isolated. */
@WebMvcTest(LectureImprovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, LectureImprovementService.class})
class LectureImprovementApiTest {
    @Autowired MockMvc mvc;
    @MockBean LectureImprovementMapper mapper;
    @MockBean EducationAchievementGuardMapper guards;
    @MockBean FunctionPermissionService permissions;
    @MockBean FileStoragePort files;
    private Map<String, Object> persisted;
    private final String path = "/api/business/lecture-improvements";
    private final String body = """
            {"managementItemCode":"lecture-improvements","achievementDate":"2025-04-10",
             "achievementContent":"강의 개선","academicYear":2025,"semester":2,"attachmentIds":[]}
            """;

    @BeforeEach
    void setup() {
        persisted = new HashMap<>(Map.of(
                "achievementId", 71L, "teacherUserId", 101L, "evaluationYear", "2025",
                "achievementStatus", "DRAFT", "achievementDate", "2025-04-10",
                "achievementContent", "이전 내용", "academicYear", 2025, "semester", 1,
                "managementItemCode", "lecture-improvements", "attachmentIds", "[]"));
        when(mapper.find(eq(71L), anyBoolean())).thenAnswer(i -> new HashMap<>(persisted));
        when(mapper.find(eq(999L), anyBoolean())).thenReturn(null);
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guards.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(permissions.evaluate(any())).thenReturn(new FunctionPermissionEvaluateResponse(
                true, "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT", "R01", "READ", "ALLOW"));
        when(mapper.managementItems(anyLong(), anyString())).thenReturn(List.of(
                Map.of("managementItemCode", "lecture-improvements", "managementItemId", 1L)));
        when(mapper.organization(anyLong())).thenReturn("KNUE-DEPT-COMP");
        doAnswer(i -> { Map<String, Object> v = i.getArgument(0); v.put("id", 71L); return null; })
                .when(mapper).insertHeader(anyMap());
        doAnswer(i -> { apply(i.getArgument(0)); return null; }).when(mapper).insertDetail(anyMap());
        when(mapper.updateHeader(anyMap())).thenReturn(1);
        doAnswer(i -> { apply(i.getArgument(0)); return null; }).when(mapper).updateDetail(anyMap());
    }

    private void apply(Map<String, Object> values) {
        LectureImprovementRequest request = (LectureImprovementRequest) values.get("body");
        persisted.put("academicYear", request.academicYear());
        persisted.put("semester", request.semester());
        persisted.put("achievementContent", request.achievementContent());
        persisted.put("achievementDate", request.achievementDate().toString());
        persisted.put("attachmentIds", values.get("attachments"));
    }

    @Test
    void createWritesHeaderBeforeDetailAndSnapshotsAndReadsBackSemester() throws Exception {
        mvc.perform(post(path).requestAttr("currentUser", user("R01"))
                        .header("X-Request-Id", "lecture-create").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(2))
                .andExpect(jsonPath("$.meta.requestId").value("lecture-create"));
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(anyMap());
        order.verify(mapper).insertDetail(anyMap());
        order.verify(mapper).insertStatus(anyMap());
        order.verify(mapper).find(71L, false);
        order.verify(mapper).insertHistory(anyMap());
        mvc.perform(get(path + "/71").requestAttr("currentUser", user("R01")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.semester").value(2));
    }

    @Test
    void updateRetainsEvaluationYearAndAuditsAllChangedFieldsWithWarning() throws Exception {
        when(guards.countEvaluationDatePeriods(eq("2025"), eq(101L), any())).thenReturn(0);
        mvc.perform(put(path + "/71").requestAttr("currentUser", user("R01"))
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("2025-04-10", "2026-04-10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.evaluationYear").value("2025"))
                .andExpect(jsonPath("$.data.achievement.academicYear").value(2025))
                .andExpect(jsonPath("$.data.achievement.semester").value(2))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        ArgumentCaptor<Map<String, Object>> audit = ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHistory(audit.capture());
        assertThat(audit.getValue().get("before").toString()).contains("이전 내용", "2025-04-10", "\"semester\":1");
        assertThat(audit.getValue().get("after").toString()).contains("강의 개선", "2026-04-10", "\"semester\":2");
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void unauthorizedReadAndWriteAreForbidden() throws Exception {
        mvc.perform(get(path).requestAttr("currentUser", user("R07"))).andExpect(status().isForbidden());
        mvc.perform(post(path).requestAttr("currentUser", user("R02"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void academicYearAndSemesterAreRequiredAndSemesterIsBounded() throws Exception {
        for (String invalid : List.of(body.replace("\"academicYear\":2025,", ""),
                body.replace("\"semester\":2,", ""), body.replace("\"semester\":2", "\"semester\":3"))) {
            mvc.perform(post(path).requestAttr("currentUser", user("R01"))
                            .contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields").isNotEmpty());
        }
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void confirmedAndNonDraftRecordsAreUnchanged() throws Exception {
        for (String state : List.of("EVALUATION_CONFIRMED", "SUBMITTED")) {
            persisted.put("achievementStatus", state);
            Map<String, Object> original = new HashMap<>(persisted);
            mvc.perform(put(path + "/71").requestAttr("currentUser", user("R01"))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
            assertThat(persisted).isEqualTo(original);
        }
        verify(mapper, never()).updateHeader(anyMap());
        verify(mapper, never()).insertHistory(anyMap());
        verifyNoInteractions(permissions);
    }

    @Test
    void outsideInputPeriodAndEvaluationFinalizationPreventMutations() throws Exception {
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(put(path + "/71").requestAttr("currentUser", user("R01"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        when(guards.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guards.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        mvc.perform(put(path + "/71").requestAttr("currentUser", user("R01"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void detailScopeUnionsRolesButWritesRemainOwnerOnly() throws Exception {
        persisted.put("teacherUserId", 202L);
        when(guards.countSharedActiveOrganization(101L, 202L)).thenReturn(1);
        mvc.perform(get(path + "/71").requestAttr("currentUser", user("R01", "R02")))
                .andExpect(status().isOk());
        mvc.perform(get(path + "/71").requestAttr("currentUser", user("R01")))
                .andExpect(status().isForbidden());
        mvc.perform(put(path + "/71").requestAttr("currentUser", user("R01", "R02"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        verify(mapper, never()).updateHeader(anyMap());
    }

    @Test
    void filteredListAndCountUseIdenticalCriteriaAndRoleUnion() throws Exception {
        when(mapper.list(any(), anyLong(), anyList())).thenReturn(List.of(persisted));
        when(mapper.count(any(), anyLong(), anyList())).thenReturn(1L);
        mvc.perform(get(path).param("managementItemCode", "lecture-improvements")
                        .requestAttr("currentUser", user("R01", "R02")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.achievements[0].semester").value(1));
        ArgumentCaptor<LectureImprovementSearchCriteria> criteria =
                ArgumentCaptor.forClass(LectureImprovementSearchCriteria.class);
        verify(mapper).list(criteria.capture(), eq(101L), eq(List.of("R01", "R02")));
        verify(mapper).count(eq(criteria.getValue()), eq(101L), eq(List.of("R01", "R02")));
        assertThat(criteria.getValue().managementItemCode()).isEqualTo("lecture-improvements");
    }

    @Test
    void missingDetailIs404AndUnknownAttachmentOrAmbiguousItemIs400() throws Exception {
        mvc.perform(get(path + "/999").requestAttr("currentUser", user("R01")))
                .andExpect(status().isNotFound());
        mvc.perform(post(path).requestAttr("currentUser", user("R01"))
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("[]", "[\"missing\"]")))
                .andExpect(status().isBadRequest());
        when(mapper.managementItems(anyLong(), anyString())).thenReturn(List.of(
                Map.of("managementItemCode", "lecture-improvements", "managementItemId", 1L),
                Map.of("managementItemCode", "lecture-improvements", "managementItemId", 2L)));
        mvc.perform(post(path).requestAttr("currentUser", user("R01"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verify(mapper, never()).insertHeader(anyMap());
    }

    @Test
    void administratorBypassIsFeatureLocalAndAnonymousIs401() throws Exception {
        mvc.perform(get(path + "/71").requestAttr("currentUser", user("R09"))).andExpect(status().isOk());
        mvc.perform(post(path).requestAttr("currentUser", user("R09"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(permissions);
    }

    private CurrentUser user(String... roles) {
        return new CurrentUser(101L, "faculty", "E101", "교원", List.of(roles), List.of());
    }
}

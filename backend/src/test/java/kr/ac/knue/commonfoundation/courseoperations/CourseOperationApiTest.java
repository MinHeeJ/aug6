package kr.ac.knue.commonfoundation.courseoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.StreamUtils;

/** Real controller/service/policy contract tests; only persistence and permission adapters are doubled. */
@WebMvcTest(CourseOperationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CourseOperationService.class, CourseOperationExceptionHandler.class,
        EducationAchievementStatusTransitionPolicy.class, GlobalExceptionHandler.class})
class CourseOperationApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private CourseOperationMapper mapper;
    @MockBean private EducationAchievementGuardMapper guard;
    @MockBean private FunctionPermissionService permissions;
    private final CurrentUser faculty = actor("R01");
    private final AtomicReference<CourseOperationRow> stored = new AtomicReference<>();
    private final AtomicReference<Map<String, Object>> header = new AtomicReference<>();
    private final AtomicReference<String> beforeAudit = new AtomicReference<>();
    private final AtomicReference<String> afterAudit = new AtomicReference<>();

    @BeforeEach
    void persistenceBoundaryRetainsActualWriteArguments() {
        stored.set(row("DRAFT", 101L, "원본"));
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(1);
        when(guard.countEvaluationDatePeriods(anyString(), anyLong(), any())).thenReturn(1);
        when(mapper.countManagementItem(anyString())).thenReturn(1);
        when(mapper.findOrganization(anyLong())).thenReturn("KNUE-DEPT-COMP");
        when(mapper.countScope(anyLong(), anyLong(), anyList())).thenReturn(1);
        when(mapper.managementItems()).thenReturn(
                List.of(new CourseOperationManagementItem("COURSE_OPERATION", "강좌 운영")));
        when(mapper.find(eq(82L), anyBoolean())).thenAnswer(invocation -> stored.get());
        when(mapper.list(any(), anyLong(), anyList())).thenAnswer(invocation -> List.of(stored.get()));
        when(mapper.count(any(), anyLong(), anyList())).thenReturn(1L);
        doAnswer(invocation -> {
            Map<String, Object> values = invocation.getArgument(0);
            values.put("achievementId", 82L);
            header.set(new LinkedHashMap<>(values));
            stored.set(null);
            return null;
        }).when(mapper).insertHeader(anyMap());
        doAnswer(invocation -> {
            assertThat(invocation.<Long>getArgument(0)).isEqualTo(header.get().get("achievementId"));
            Map<String, Object> values = header.get();
            stored.set(new CourseOperationRow(82L, (String) values.get("managementNo"),
                    (Long) values.get("teacherUserId"), "faculty", (String) values.get("organizationCode"),
                    (String) values.get("evaluationYear"), (String) values.get("managementItemCode"),
                    (LocalDate) values.get("achievementDate"), invocation.getArgument(1), "DRAFT",
                    (String) values.get("attachments"), List.of(), LocalDateTime.of(2026, 4, 10, 9, 0), 101L,
                    LocalDateTime.of(2026, 4, 10, 9, 0), 101L));
            return null;
        }).when(mapper).insertDetail(anyLong(), anyString());
        when(mapper.updateHeader(anyLong(), any(), anyString(), nullable(String.class), anyLong()))
                .thenAnswer(invocation -> {
                    CourseOperationRow old = stored.get();
                    CourseOperationRequest body = invocation.getArgument(1);
                    stored.set(new CourseOperationRow(old.achievementId(), old.managementNo(), old.teacherUserId(),
                            old.teacherName(), old.organizationCode(), old.evaluationYear(), body.managementItemCode(),
                            body.achievementDate(), old.performanceDetails(), invocation.getArgument(2),
                            invocation.getArgument(3), List.of(), old.createdAt(), old.createdBy(),
                            old.updatedAt().plusSeconds(1), invocation.getArgument(4)));
                    return 1;
                });
        doAnswer(invocation -> {
            CourseOperationRow old = stored.get();
            stored.set(new CourseOperationRow(old.achievementId(), old.managementNo(), old.teacherUserId(),
                    old.teacherName(), old.organizationCode(), old.evaluationYear(), old.managementItemCode(),
                    old.achievementDate(), invocation.getArgument(1), old.achievementStatus(), old.attachmentRef(),
                    old.attachmentIds(), old.createdAt(), old.createdBy(), old.updatedAt(), old.updatedBy()));
            return null;
        }).when(mapper).updateDetail(anyLong(), anyString());
        doAnswer(invocation -> {
            beforeAudit.set(invocation.getArgument(2));
            afterAudit.set(invocation.getArgument(3));
            return null;
        }).when(mapper).insertChangeHistory(
                anyLong(), anyString(), nullable(String.class), anyString(), anyLong(), anyString());
    }

    @Test
    void createReadbackMatchesListDetailAndFullAudit() throws Exception {
        mvc.perform(as(post("/api/business/course-operations"), faculty).content(body("신규 강좌")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.achievement.performanceDetails").value("신규 강좌"))
                .andExpect(jsonPath("$.data.achievement.achievementStatus").value("DRAFT"))
                .andExpect(jsonPath("$.meta.requestId").value("course-request"));
        mvc.perform(as(get("/api/business/course-operations"), faculty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievements[0].performanceDetails").value("신규 강좌"));
        mvc.perform(as(get("/api/business/course-operations/{achievementId}", 82L), faculty))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.performanceDetails").value("신규 강좌"));
        assertThat(beforeAudit.get()).isNull();
        assertThat(json.readTree(afterAudit.get()).get("performanceDetails").asText()).isEqualTo("신규 강좌");
        var order = inOrder(mapper);
        order.verify(mapper).insertHeader(anyMap());
        order.verify(mapper).insertDetail(eq(82L), eq("신규 강좌"));
        order.verify(mapper).find(82L, false);
        verify(mapper).insertStatusHistory(82L, null, "DRAFT", 101L, "course-request");
        verify(mapper).insertChangeHistory(
                eq(82L), eq("CREATE"), isNull(), anyString(), eq(101L), eq("course-request"));
    }

    @Test
    void updateReadbackPreservesYearAndIdentityAndAuditsChangedDateAndDetail() throws Exception {
        CourseOperationRow old = stored.get();
        when(guard.countEvaluationDatePeriods(eq("2026"), anyLong(), any())).thenReturn(0);
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty)
                        .content(body("변경 내용").replace("2026-04-10", "2025-12-31")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.occurredDateWarning").value(true));
        mvc.perform(as(get("/api/business/course-operations/{achievementId}", 82L), faculty))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.performanceDetails").value("변경 내용"))
                .andExpect(jsonPath("$.data.achievementDate").value("2025-12-31"))
                .andExpect(jsonPath("$.data.evaluationYear").value("2026"));
        assertThat(stored.get().teacherUserId()).isEqualTo(old.teacherUserId());
        assertThat(stored.get().createdAt()).isEqualTo(old.createdAt());
        assertThat(stored.get().updatedAt()).isAfter(old.updatedAt());
        assertThat(json.readTree(beforeAudit.get()).get("performanceDetails").asText()).isEqualTo("원본");
        assertThat(json.readTree(afterAudit.get()).get("performanceDetails").asText()).isEqualTo("변경 내용");
        assertThat(json.readTree(beforeAudit.get()).get("achievementDate")).isNotEqualTo(
                json.readTree(afterAudit.get()).get("achievementDate"));
        verify(mapper).insertChangeHistory(
                eq(82L), eq("UPDATE"), anyString(), anyString(), eq(101L), eq("course-request"));
    }

    @Test
    void listPassesNormalizedFiltersAndSameUnionRolesToCount() throws Exception {
        CurrentUser union = actor("R01", "R02", "R04");
        mvc.perform(as(get("/api/business/course-operations"), union)
                        .param("managementNo", " CO-001 ").param("page", "2").param("pageSize", "50"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        CourseOperationSearchCriteria expected = new CourseOperationSearchCriteria(
                2, 50, 100L, "CO-001", null, null, null);
        verify(mapper).list(expected, 101L, union.roles());
        verify(mapper).count(expected, 101L, union.roles());
    }

    @Test void detailDeniesOutOfScope() throws Exception {
        when(mapper.countScope(anyLong(), anyLong(), anyList())).thenReturn(0);
        mvc.perform(as(get("/api/business/course-operations/{achievementId}", 82L), faculty))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }
    @Test void detailReturnsNotFound() throws Exception {
        stored.set(null);
        mvc.perform(as(get("/api/business/course-operations/{achievementId}", 82L), faculty))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
    @Test void listRejectsUnadmittedRole() throws Exception {
        mvc.perform(as(get("/api/business/course-operations"), actor("R07")))
                .andExpect(status().isForbidden());
        verify(mapper, never()).list(any(), anyLong(), anyList());
    }
    @Test void detailRejectsUnadmittedRole() throws Exception {
        mvc.perform(as(get("/api/business/course-operations/{achievementId}", 82L), actor("R07")))
                .andExpect(status().isForbidden());
        verify(mapper, never()).find(anyLong(), anyBoolean());
    }
    @Test void createRejectsUnadmittedRole() throws Exception {
        mvc.perform(as(post("/api/business/course-operations"), actor("R02")).content(body("내용")))
                .andExpect(status().isForbidden());
        noWrites();
    }
    @Test void updateRejectsUnadmittedRole() throws Exception {
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), actor("R04")).content(body("내용")))
                .andExpect(status().isForbidden());
        noWrites();
    }
    @Test void updateRejectsOtherOwner() throws Exception {
        stored.set(row("DRAFT", 102L, "타인 원본"));
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty).content(body("변경")))
                .andExpect(status().isForbidden());
        assertThat(stored.get().performanceDetails()).isEqualTo("타인 원본");
        noWrites();
    }
    @Test void createRequiresPerformanceDetails() throws Exception {
        mvc.perform(as(post("/api/business/course-operations"), faculty).content(missingDetails()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[?(@.field == 'performanceDetails')]").isNotEmpty());
        noWrites();
    }
    @Test void updateRequiresPerformanceDetails() throws Exception {
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty).content(missingDetails()))
                .andExpect(status().isBadRequest());
        noWrites();
    }
    @Test void createRejectsInactivePeriod() throws Exception {
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(as(post("/api/business/course-operations"), faculty).content(body("내용")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"))
                .andExpect(jsonPath("$.meta.requestId").value("course-request"));
        noWrites();
    }
    @Test void updateRejectsInactivePeriodAndKeepsOriginal() throws Exception {
        when(guard.countActiveInputPeriods(anyString(), anyLong())).thenReturn(0);
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty).content(body("변경")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"));
        assertThat(stored.get().performanceDetails()).isEqualTo("원본");
        noWrites();
    }
    @Test void updateRejectsConfirmedRowAndKeepsOriginal() throws Exception {
        stored.set(row("EVALUATION_CONFIRMED", 101L, "확정 원본"));
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty).content(body("변경")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        assertThat(stored.get().performanceDetails()).isEqualTo("확정 원본");
        noWrites();
    }
    @Test void updateRejectsFinalizationEvenWhenRowIsDraft() throws Exception {
        when(guard.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty).content(body("변경")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        noWrites();
    }
    @Test void createRejectsFinalizedEvaluation() throws Exception {
        when(guard.countEvaluationConfirmations(anyLong(), anyString())).thenReturn(1);
        mvc.perform(as(post("/api/business/course-operations"), faculty).content(body("내용")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        noWrites();
    }
    @Test void updateRejectsSubmittedRecord() throws Exception {
        stored.set(row("SUBMITTED", 101L, "제출 원본"));
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty).content(body("변경")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
        noWrites();
    }
    @Test void updateCannotPromoteToCertified() throws Exception {
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty)
                        .content(body("내용").replace("}", ",\"achievementStatus\":\"CERTIFIED\"}")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
        noWrites();
    }
    @Test void createCannotStartSubmitted() throws Exception {
        mvc.perform(as(post("/api/business/course-operations"), faculty)
                        .content(body("내용").replace("}", ",\"achievementStatus\":\"SUBMITTED\"}")))
                .andExpect(status().isConflict());
        noWrites();
    }
    @Test void updatePermitsResubmissionAndRecordsStatusHistory() throws Exception {
        stored.set(row("DEPARTMENT_REJECTED", 101L, "반려 원본"));
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty)
                        .content(body("수정 제출").replace("}", ",\"achievementStatus\":\"SUBMITTED\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.achievementStatus").value("SUBMITTED"));
        verify(mapper).insertStatusHistory(82L, "DEPARTMENT_REJECTED", "SUBMITTED", 101L, "course-request");
    }
    @Test void updateCannotMoveEvaluationYear() throws Exception {
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty)
                        .content(body("내용").replace("}", ",\"evaluationYear\":\"2025\"}")))
                .andExpect(status().isBadRequest());
        noWrites();
    }
    @Test void createValidatesActiveManagementItem() throws Exception {
        when(mapper.countManagementItem(anyString())).thenReturn(0);
        mvc.perform(as(post("/api/business/course-operations"), faculty).content(body("내용")))
                .andExpect(status().isBadRequest());
        noWrites();
    }
    @Test void createRejectsUnownedAttachmentToken() throws Exception {
        mvc.perform(as(post("/api/business/course-operations"), faculty)
                        .content(body("내용").replace("}", ",\"attachmentIds\":[\"foreign-token\"]}")))
                .andExpect(status().isBadRequest());
        noWrites();
    }
    @Test void createRequiresFunctionPermission() throws Exception {
        when(permissions.evaluate(any())).thenThrow(new ForbiddenException());
        mvc.perform(as(post("/api/business/course-operations"), faculty).content(body("내용")))
                .andExpect(status().isForbidden());
        noWrites();
    }
    @Test void listRequiresFunctionPermission() throws Exception {
        when(permissions.evaluate(any())).thenThrow(new ForbiddenException());
        mvc.perform(as(get("/api/business/course-operations"), faculty)).andExpect(status().isForbidden());
    }
    @Test void updateRequiresFunctionPermission() throws Exception {
        when(permissions.evaluate(any())).thenThrow(new ForbiddenException());
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty).content(body("내용")))
                .andExpect(status().isForbidden());
        noWrites();
    }
    @Test void administratorOverrideReachesAllFourOperations() throws Exception {
        CurrentUser admin = actor("R09");
        mvc.perform(as(get("/api/business/course-operations"), admin)).andExpect(status().isOk());
        mvc.perform(as(get("/api/business/course-operations/{achievementId}", 82L), admin)).andExpect(status().isOk());
        mvc.perform(as(post("/api/business/course-operations"), admin).content(body("관리자 등록")))
                .andExpect(status().isOk());
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), admin).content(body("관리자 수정")))
                .andExpect(status().isOk());
        verifyNoInteractions(permissions);
    }
    @Test void anonymousListIsUnauthorized() throws Exception {
        mvc.perform(get("/api/business/course-operations")).andExpect(status().isUnauthorized());
    }
    @Test void anonymousDetailIsUnauthorized() throws Exception {
        mvc.perform(get("/api/business/course-operations/{achievementId}", 82L)).andExpect(status().isUnauthorized());
    }
    @Test void anonymousCreateIsUnauthorized() throws Exception {
        mvc.perform(post("/api/business/course-operations").contentType(MediaType.APPLICATION_JSON).content(body("내용")))
                .andExpect(status().isUnauthorized());
        noWrites();
    }
    @Test void anonymousUpdateIsUnauthorized() throws Exception {
        mvc.perform(put("/api/business/course-operations/{achievementId}", 82L)
                        .contentType(MediaType.APPLICATION_JSON).content(body("내용")))
                .andExpect(status().isUnauthorized());
        noWrites();
    }
    @Test void listRejectsUnsupportedPageSize() throws Exception {
        mvc.perform(as(get("/api/business/course-operations"), faculty).param("pageSize", "10"))
                .andExpect(status().isBadRequest());
    }
    @Test void updateMissingRecordDoesNotWrite() throws Exception {
        stored.set(null);
        mvc.perform(as(put("/api/business/course-operations/{achievementId}", 82L), faculty).content(body("내용")))
                .andExpect(status().isNotFound());
        noWrites();
    }
    @Test void createStorageFailureDoesNotLeakSqlOrCredentials() throws Exception {
        doThrow(new RuntimeException("jdbc:postgresql password=secret syntax failure"))
                .when(mapper).insertDetail(anyLong(), anyString());
        mvc.perform(as(post("/api/business/course-operations"), faculty).content(body("내용")))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))));
        verify(mapper, never()).insertChangeHistory(anyLong(), anyString(), any(), anyString(), anyLong(), anyString());
    }
    @Test void classpathContractDeclaresAllOwnedOperations() throws Exception {
        try (var stream = new ClassPathResource("contracts/openapi.yaml").getInputStream()) {
            String contract = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
            assertThat(contract).contains("operationId: listCourseOperations", "operationId: getCourseOperation",
                    "operationId: createCourseOperation", "operationId: updateCourseOperation");
        }
        mvc.perform(as(get("/api/business/course-operations"), faculty)).andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, CurrentUser user) {
        return request.requestAttr("currentUser", user)
                .cookie(new Cookie(AuthController.SESSION_COOKIE, "test-session"))
                .header("X-Request-Id", "course-request").contentType(MediaType.APPLICATION_JSON);
    }
    private void noWrites() {
        verify(mapper, never()).insertHeader(anyMap());
        verify(mapper, never()).updateHeader(anyLong(), any(), anyString(), any(), anyLong());
        verify(mapper, never()).updateDetail(anyLong(), anyString());
        verify(mapper, never()).insertChangeHistory(anyLong(), anyString(), any(), anyString(), anyLong(), anyString());
    }
    private static CurrentUser actor(String... roles) {
        return new CurrentUser(101L, "faculty", "E0101", "교원", List.of(roles), List.of());
    }
    private static String body(String details) {
        return "{\"managementItemCode\":\"COURSE_OPERATION\",\"achievementDate\":\"2026-04-10\",\"performanceDetails\":\""
                + details + "\"}";
    }
    private static String missingDetails() {
        return "{\"managementItemCode\":\"COURSE_OPERATION\",\"achievementDate\":\"2026-04-10\"}";
    }
    private CourseOperationRow row(String status, Long owner, String details) {
        return new CourseOperationRow(82L, "CO-001", owner, "faculty", "KNUE-DEPT-COMP", "2026", "COURSE_OPERATION",
                LocalDate.of(2026, 4, 10), details, status, null, List.of(),
                LocalDateTime.of(2026, 4, 10, 9, 0), owner, LocalDateTime.of(2026, 4, 10, 9, 0), owner);
    }
}

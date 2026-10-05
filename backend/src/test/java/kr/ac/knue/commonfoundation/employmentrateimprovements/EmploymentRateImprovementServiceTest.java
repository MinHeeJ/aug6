package kr.ac.knue.commonfoundation.employmentrateimprovements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Verifies employment-rate-improvement audit entries contain comparable full snapshots. */
class EmploymentRateImprovementServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CurrentUser requester = new CurrentUser(
            101L,
            "faculty",
            "E0101",
            "교원",
            List.of("R01"),
            List.of());

    @Test
    void createRecordsACompleteAfterSnapshot() throws Exception {
        EmploymentRateImprovementMapper mapper = org.mockito.Mockito.mock(EmploymentRateImprovementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        EmploymentRateImprovementService service = new EmploymentRateImprovementService(
                mapper,
                guardService,
                objectMapper);
        EmploymentRateImprovementRequest request = request(
                "EMPLOYMENT_RATE_IMPROVEMENT",
                "2026-04-10",
                "2026-04-01",
                "2026-04-10",
                "2026-1",
                List.of("attachment-1", "attachment-2"));
        when(guardService.validateMutation(eq(requester), any())).thenReturn(OccurredDateValidation.accepted());
        when(mapper.findByManagementNo(any())).thenReturn(row(
                "EMPLOYMENT_RATE_IMPROVEMENT",
                "2026-04-10",
                "2026-04-01",
                "2026-04-10",
                "2026-1",
                List.of("attachment-1", "attachment-2")));

        service.create(request, requester);

        ArgumentCaptor<String> afterValue = ArgumentCaptor.forClass(String.class);
        verify(mapper).insertChangeHistory(
                eq("81"),
                eq(null),
                afterValue.capture(),
                eq(101L),
                eq("취업률 제고 실적 등록"));
        assertThat(objectMapper.readTree(afterValue.getValue())).isEqualTo(snapshot(
                "EMPLOYMENT_RATE_IMPROVEMENT",
                "2026-04-10",
                "2026-04-01",
                "2026-04-10",
                "2026-1",
                List.of("attachment-1", "attachment-2")));
    }

    @Test
    void updateRecordsCompleteComparableBeforeAndAfterSnapshots() throws Exception {
        EmploymentRateImprovementMapper mapper = org.mockito.Mockito.mock(EmploymentRateImprovementMapper.class);
        EducationAchievementGuardService guardService = org.mockito.Mockito.mock(
                EducationAchievementGuardService.class);
        EmploymentRateImprovementService service = new EmploymentRateImprovementService(
                mapper,
                guardService,
                objectMapper);
        EmploymentRateImprovementRow existing = row(
                "EMPLOYMENT_RATE_IMPROVEMENT",
                "2026-04-10",
                "2026-04-01",
                "2026-04-10",
                "2026-1",
                List.of("attachment-1"));
        EmploymentRateImprovementRow saved = row(
                "EMPLOYMENT_RATE_IMPROVEMENT_UPDATED",
                "2026-05-10",
                "2026-05-01",
                "2026-05-10",
                "2026-2",
                List.of("attachment-2"));
        EmploymentRateImprovementRequest request = request(
                "EMPLOYMENT_RATE_IMPROVEMENT_UPDATED",
                "2026-05-10",
                "2026-05-01",
                "2026-05-10",
                "2026-2",
                List.of("attachment-2"));
        when(mapper.findById(81L)).thenReturn(existing, saved);
        when(mapper.update(any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(guardService.validateMutation(eq(requester), any())).thenReturn(OccurredDateValidation.accepted());

        service.update(81L, request, requester);

        ArgumentCaptor<String> beforeValue = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> afterValue = ArgumentCaptor.forClass(String.class);
        verify(mapper).insertChangeHistory(
                eq("81"),
                beforeValue.capture(),
                afterValue.capture(),
                eq(101L),
                eq("취업률 제고 실적 수정"));
        assertThat(objectMapper.readTree(beforeValue.getValue())).isEqualTo(snapshot(
                "EMPLOYMENT_RATE_IMPROVEMENT",
                "2026-04-10",
                "2026-04-01",
                "2026-04-10",
                "2026-1",
                List.of("attachment-1")));
        assertThat(objectMapper.readTree(afterValue.getValue())).isEqualTo(snapshot(
                "EMPLOYMENT_RATE_IMPROVEMENT_UPDATED",
                "2026-05-10",
                "2026-05-01",
                "2026-05-10",
                "2026-2",
                List.of("attachment-2")));
    }

    private EmploymentRateImprovementRequest request(
            String managementItemCode,
            String achievementDate,
            String specialLectureStartDate,
            String specialLectureEndDate,
            String mockExamQuestionPeriod,
            List<String> attachmentIds) {
        return new EmploymentRateImprovementRequest(
                managementItemCode,
                LocalDate.parse(achievementDate),
                LocalDate.parse(specialLectureStartDate),
                LocalDate.parse(specialLectureEndDate),
                mockExamQuestionPeriod,
                attachmentIds);
    }

    private EmploymentRateImprovementRow row(
            String managementItemCode,
            String achievementDate,
            String specialLectureStartDate,
            String specialLectureEndDate,
            String mockExamQuestionPeriod,
            List<String> attachmentIds) {
        return new EmploymentRateImprovementRow(
                81L,
                "B83-ERI-001",
                101L,
                "faculty",
                "2026",
                managementItemCode,
                LocalDate.parse(achievementDate),
                LocalDate.parse(specialLectureStartDate),
                LocalDate.parse(specialLectureEndDate),
                mockExamQuestionPeriod,
                attachmentIds,
                "DRAFT",
                LocalDateTime.parse("2026-04-10T09:00:00"),
                LocalDateTime.parse("2026-04-10T09:00:00"));
    }

    private JsonNode snapshot(
            String managementItemCode,
            String achievementDate,
            String specialLectureStartDate,
            String specialLectureEndDate,
            String mockExamQuestionPeriod,
            List<String> attachmentIds) {
        com.fasterxml.jackson.databind.node.ObjectNode value = objectMapper.createObjectNode();
        value.put("managementItemCode", managementItemCode);
        value.put("achievementDate", achievementDate);
        value.put("specialLectureStartDate", specialLectureStartDate);
        value.put("specialLectureEndDate", specialLectureEndDate);
        value.put("mockExamQuestionPeriod", mockExamQuestionPeriod);
        com.fasterxml.jackson.databind.node.ArrayNode attachments = value.putArray("attachmentIds");
        attachmentIds.forEach(attachments::add);
        return value;
    }
}

package kr.ac.knue.commonfoundation.faculty.achievement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract coverage for the degree-completion API. These tests catch loss of a student detail row,
 * degree type validation, or unauthorized access at the public HTTP boundary.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DegreeCompletionAchievementApiContractTest {
    @Autowired
    MockMvc mockMvc;

    private final CurrentUser instructor = new CurrentUser(
            2L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());
    private final CurrentUser excelOperator = new CurrentUser(
            7L, "exceloperator", "E7001", "엑셀담당", List.of("R07"), List.of());

    @Test
    void savingDegreeCompletionKeepsEveryStudentDetailVisibleWhenTheAchievementIsReadBack() throws Exception {
        String request = """
                {
                  "managementNo":"B77-DC-NEW-001",
                  "evaluationYear":"2026",
                  "managementItemCode":"EDU_DEGREE_COMPLETION",
                  "occurredDate":"2026-08-20",
                  "students":[
                    {"degreeType":"MASTER","studentName":"김석사","thesisTitle":"교육 데이터 분석","degreeAwardedDate":"2026-08-20"},
                    {"degreeType":"DOCTOR","studentName":"이박사","thesisTitle":"학습 분석 연구","degreeAwardedDate":"2026-08-20"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", instructor)
                        .header("X-Request-Id", "REQ-B77-DC-SAVE-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.managementNo").value("B77-DC-NEW-001"))
                .andExpect(jsonPath("$.data.students[0].degreeType").value("MASTER"))
                .andExpect(jsonPath("$.data.students[0].studentName").value("김석사"))
                .andExpect(jsonPath("$.data.students[1].thesisTitle").value("학습 분석 연구"))
                .andExpect(jsonPath("$.meta.requestId").value("REQ-B77-DC-SAVE-001"));

        mockMvc.perform(get("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", instructor)
                        .param("managementNo", "B77-DC-NEW-001")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].managementNo").value("B77-DC-NEW-001"))
                .andExpect(jsonPath("$.data.items[0].students[0].degreeAwardedDate").value("2026-08-20"))
                .andExpect(jsonPath("$.data.items[0].students[1].degreeType").value("DOCTOR"));
    }

    @Test
    void savingStudentWithoutDegreeTypeReturnsFieldValidationError() throws Exception {
        String request = """
                {"managementNo":"B77-DC-INVALID-001","evaluationYear":"2026","managementItemCode":"EDU_DEGREE_COMPLETION","occurredDate":"2026-08-20","students":[{"studentName":"김석사","thesisTitle":"논문","degreeAwardedDate":"2026-08-20"}]}
                """;

        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("students[0].degreeType"));
    }

    @Test
    void r07CannotSaveDegreeCompletionAchievement() throws Exception {
        mockMvc.perform(post("/api/business/degree-completion-achievements")
                        .requestAttr("currentUser", excelOperator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"EDU_DEGREE_COMPLETION\",\"students\":[]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }
}

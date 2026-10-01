package kr.ac.knue.commonfoundation.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentGuidanceAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentGuidanceExcelContractTest {
    @Autowired MockMvc mockMvc;
    @MockBean StudentGuidanceAchievementService service;
    @MockBean StudentGuidanceExcelService excelService;
    private final CurrentUser r07 = new CurrentUser(701L, "operator7", "E7001", "담당자", List.of("R07"), List.of());
    private final CurrentUser r01 = new CurrentUser(101L, "professor1", "E1001", "홍길동", List.of("R01"), List.of());

    @Test
    void r07UploadReturnsSeparatedNormalAndErrorRows() throws Exception {
        when(excelService.upload(any(), eq(r07))).thenReturn(new StudentGuidanceExcelUploadResult("SG-UP-1", "학생지도.csv", "REJECTED", 2, 1, 1, List.of(new ExcelUploadErrorRow("ERR-1", "SG-UP-1", 3, "학생명", "", "REQUIRED_VALUE", "필수값이 누락되었습니다.", "학생명을 입력하세요."))));
        MockMultipartFile file = new MockMultipartFile("file", "학생지도.csv", "text/csv", "교번,관리항목코드,지도시작일,지도종료일,학생명\nE1001,EDU-STUDENT-GUIDANCE,2026-04-01,2026-04-30,학생가".getBytes());
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).requestAttr("currentUser", r07))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.businessType").doesNotExist()).andExpect(jsonPath("$.data.errorCount").value(1)).andExpect(jsonPath("$.data.errors[0].rowNumber").value(3));
    }

    @Test
    void r01CannotUploadAndOnlyR07CanCommitValidatedUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "학생지도.csv", "text/csv", "교번,관리항목코드,지도시작일,지도종료일,학생명\n".getBytes());
        mockMvc.perform(multipart("/api/business/student-guidance-achievements/excel-uploads").file(file).requestAttr("currentUser", r01)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/business/student-guidance-achievements/excel-uploads/SG-UP-1/commit").requestAttr("currentUser", r01)).andExpect(status().isForbidden());
        verify(excelService, never()).upload(any(), eq(r01));
        verify(excelService, never()).commit(any(), eq(r01));
    }
}

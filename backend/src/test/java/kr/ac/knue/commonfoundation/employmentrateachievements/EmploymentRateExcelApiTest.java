package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.excel.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EmploymentRateExcelApiTest {
    private static final String BASE = "/api/business/employment-rate-achievements/excel-uploads";
    private final EmploymentRateExcelRepository repository = mock(EmploymentRateExcelRepository.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var service = new EmploymentRateExcelService(repository, new EmploymentRateWorkbook(),
                mock(EducationAchievementGuardMapper.class), mock(FileStoragePort.class),
                new ObjectMapper(), mock(FunctionPermissionService.class));
        mvc = MockMvcBuilders.standaloneSetup(new EmploymentRateExcelController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void routesRequireAuthenticationAndR07EvenWithAdministratorRole() throws Exception {
        CurrentUser admin = new CurrentUser(1L, "admin", "E1", "Admin", List.of("R09"), List.of());
        for (String suffix : List.of("/template", "/histories", "/UPLOAD/errors", "/UPLOAD/errors/download")) {
            mvc.perform(get(BASE + suffix)).andExpect(status().isUnauthorized());
            mvc.perform(get(BASE + suffix).requestAttr("currentUser", admin)).andExpect(status().isForbidden());
        }
        mvc.perform(post(BASE + "/UPLOAD/commit").requestAttr("currentUser", admin))
                .andExpect(status().isForbidden());
        var file = new MockMultipartFile("file", "bad.xlsx", EmploymentRateWorkbook.MIME, new byte[] {1});
        mvc.perform(multipart(BASE).file(file).requestAttr("currentUser", admin)).andExpect(status().isForbidden());
    }

    @Test
    void authorizedTemplateReturnsXlsxAndTraceEnvelopeForHistories() throws Exception {
        CurrentUser operator = new CurrentUser(7L, "operator", "E7", "Operator", List.of("R07"), List.of());
        when(repository.currentTemplate()).thenReturn(new EmploymentRateExcelModels.Template(
                "EMPLOYMENT-RATE-V1", "1", List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조")));
        mvc.perform(get(BASE + "/template").requestAttr("currentUser", operator))
                .andExpect(status().isOk()).andExpect(content().contentType(EmploymentRateWorkbook.MIME));
        mvc.perform(get(BASE + "/histories").requestAttr("currentUser", operator).header("X-Request-Id", "REQ-EXCEL"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.requestId").value("REQ-EXCEL"));
    }
}

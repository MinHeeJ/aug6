package kr.ac.knue.commonfoundation.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic26ExcelMenuSeedTest {
    @Test
    void excelManagementMenusAreSeededUnderFileDataManagementWithR09Permissions() throws Exception {
        String sql = new ClassPathResource("db/migration/V15__basic26_excel_foundation.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .toLowerCase();

        assertThat(sql)
                .contains("'파일·데이터 관리'")
                .contains("'엑셀 관리'")
                .contains("'scr-upload-template-mgmt'")
                .contains("'/admin/excel-upload-templates'")
                .contains("'scr-excel-upload-mgmt'")
                .contains("'/admin/excel-uploads'")
                .contains("'scr-upload-history-mgmt'")
                .contains("'/admin/excel-upload-histories'")
                .contains("'scr-upload-error-mgmt'")
                .contains("'/admin/excel-upload-errors'")
                .contains("'scr-excel-download-mgmt'")
                .contains("'/admin/excel-downloads'")
                .contains("insert into menu_execution_info")
                .contains("insert into menu_permissions")
                .contains("'role', 'r09', menu_id, 'allow'");
    }
}

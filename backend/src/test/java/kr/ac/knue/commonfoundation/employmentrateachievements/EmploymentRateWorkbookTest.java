package kr.ac.knue.commonfoundation.employmentrateachievements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class EmploymentRateWorkbookTest {
    private final EmploymentRateWorkbook codec = new EmploymentRateWorkbook();

    @Test
    void templateIsRealXlsxAndRoundTripsVersionAndColumns() throws Exception {
        byte[] bytes = codec.template("EMPLOYMENT-RATE-V1", "1", List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조"));
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("교번");
            workbook.getSheetAt(0).createRow(1).createCell(0).setCellValue("00123");
            workbook.getSheetAt(0).getRow(1).createCell(1).setCellValue("FR-032");
            workbook.getSheetAt(0).getRow(1).createCell(2).setCellValue("2025-04-10");
            workbook.getSheetAt(0).getRow(1).createCell(3).setCellValue("Employment");
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            assertThat(codec.read(out.toByteArray(), "EMPLOYMENT-RATE-V1", "1",
                    List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조")))
                    .singleElement().satisfies(row -> {
                        assertThat(row.rowNumber()).isEqualTo(2);
                        assertThat(row.employeeNo()).isEqualTo("00123");
                        assertThat(row.achievementName()).isEqualTo("Employment");
                    });
        }
    }

    @Test
    void csvCannotMasqueradeAsXlsx() {
        assertThatThrownBy(() -> codec.read("교번,관리항목코드".getBytes(), "T", "1", List.of("교번")))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void rejectsOldTemplateVersion() {
        byte[] bytes = codec.template("T", "old", List.of("교번"));
        assertThatThrownBy(() -> codec.read(bytes, "T", "current", List.of("교번")))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void rejectsOutOfTemplateValuesEvenWhenExpectedColumnsAreBlank() throws Exception {
        List<String> columns = List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조");
        byte[] bytes = codec.template("T", "1", columns);
        try (XSSFWorkbook book = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            var valid = book.getSheetAt(0).createRow(1);
            valid.createCell(0).setCellValue("00123");
            valid.createCell(1).setCellValue("FR-032");
            valid.createCell(2).setCellValue("2025-04-10");
            valid.createCell(3).setCellValue("Employment");
            book.getSheetAt(0).createRow(2).createCell(5).setCellValue("unexpected data");
            var out = new java.io.ByteArrayOutputStream();
            book.write(out);
            assertThatThrownBy(() -> codec.read(out.toByteArray(), "T", "1", columns))
                    .isInstanceOf(BusinessValidationException.class);
        }
    }

    @Test
    void errorWorkbookStoresUntrustedValuesAsTextNotFormulas() throws Exception {
        var error = new EmploymentRateExcelModels.ErrorRow(2, "실적명", "=HYPERLINK(\"evil\")",
                "INVALID", "invalid", "correct input");
        try (XSSFWorkbook book = new XSSFWorkbook(new java.io.ByteArrayInputStream(codec.errors(List.of(error))))) {
            assertThat(book.getSheetAt(0).getRow(1).getCell(2).getCellType())
                    .isEqualTo(org.apache.poi.ss.usermodel.CellType.STRING);
        }
    }
}

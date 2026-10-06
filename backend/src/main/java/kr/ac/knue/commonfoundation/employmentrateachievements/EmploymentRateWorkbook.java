package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Genuine XLSX codec with versioned headers and formula-free string exports. */
@Component
public class EmploymentRateWorkbook {
    public static final String MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Renders a data table as a genuine XLSX workbook, with string cells to prevent formula injection. */
    public byte[] table(List<List<String>> rows) {
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet("Employment rate");
            for (int index = 0; index < rows.size(); index++) {
                write(sheet.createRow(index), rows.get(index));
            }
            return bytes(book);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot render employment-rate download", exception);
        }
    }

    /** Generates a blank workbook carrying persisted template identity and version. */
    public byte[] template(String id, String version, List<String> columns) {
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet("Employment rate");
            write(sheet.createRow(0), columns);
            book.getProperties().getCustomProperties().addProperty("templateId", id);
            book.getProperties().getCustomProperties().addProperty("templateVersion", version);
            return bytes(book);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot render employment-rate workbook", ex);
        }
    }

    /** Reads data only from the expected single-sheet template and rejects formulas. */
    public List<EmploymentRateExcelModels.InputRow> read(byte[] content, String id,
            String version, List<String> columns) {
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            var properties = book.getProperties().getCustomProperties();
            if (!properties.contains("templateId") || !properties.contains("templateVersion")
                    || !id.equals(properties.getProperty("templateId").getLpwstr())
                    || !version.equals(properties.getProperty("templateVersion").getLpwstr())
                    || book.getNumberOfSheets() != 1) {
                throw invalid("현행 양식 ID와 버전을 확인하세요.");
            }
            Sheet sheet = book.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null || header.getLastCellNum() != columns.size()) throw invalid("양식 열을 확인하세요.");
            for (int i = 0; i < columns.size(); i++) {
                if (!columns.get(i).equals(text(header.getCell(i)))) throw invalid("양식 열 순서를 확인하세요.");
            }
            List<EmploymentRateExcelModels.InputRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                List<String> values = new ArrayList<>();
                for (int c = 0; c < columns.size(); c++) values.add(text(row.getCell(c)));
                if (row.getLastCellNum() > columns.size()) throw invalid("양식 외 열은 허용하지 않습니다.");
                if (values.stream().allMatch(String::isBlank)) continue;
                rows.add(new EmploymentRateExcelModels.InputRow(i + 1, values.get(0), values.get(1),
                        values.get(2), values.get(3), values.get(4)));
            }
            if (rows.isEmpty()) throw invalid("데이터 행을 입력하세요.");
            return rows;
        } catch (BusinessValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("손상되거나 지원하지 않는 XLSX 파일입니다.");
        }
    }

    /** Renders diagnostics as literal strings, including untrusted input values. */
    public byte[] errors(List<EmploymentRateExcelModels.ErrorRow> errors) {
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet("Errors");
            write(sheet.createRow(0), List.of("rowNumber", "columnName", "inputValue",
                    "errorCode", "errorReason", "correctionGuide"));
            int index = 1;
            for (var error : errors) {
                write(sheet.createRow(index++), List.of(String.valueOf(error.rowNumber()),
                        error.columnName(), error.inputValue() == null ? "" : error.inputValue(),
                        error.errorCode(), error.errorReason(), error.correctionGuide()));
            }
            return bytes(book);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot render employment-rate errors", ex);
        }
    }

    private String text(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.FORMULA || cell.getCellType() == CellType.ERROR) {
            throw invalid("수식과 오류 셀은 허용하지 않습니다. 값으로 입력하세요.");
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        return new DataFormatter(Locale.ROOT).formatCellValue(cell).trim();
    }

    private void write(Row row, List<String> values) {
        for (int i = 0; i < values.size(); i++) row.createCell(i, CellType.STRING).setCellValue(values.get(i));
    }

    private byte[] bytes(XSSFWorkbook book) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        book.write(out);
        return out.toByteArray();
    }

    private BusinessValidationException invalid(String reason) {
        return new BusinessValidationException("취업률 Excel 파일이 올바르지 않습니다.",
                List.of(new ValidationError("file", reason)));
    }
}

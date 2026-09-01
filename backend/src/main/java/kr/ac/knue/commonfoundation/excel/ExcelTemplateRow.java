package kr.ac.knue.commonfoundation.excel;

import java.time.LocalDate;
import java.util.List;

public record ExcelTemplateRow(String templateId, String businessType, String templateVersion, LocalDate effectiveDate,
        String systemUseYn, String status, String fileToken, String originalFileName, List<ExcelTemplateRuleRow> rules) {
    public ExcelTemplateRow(String templateId, String businessType, String templateVersion, LocalDate effectiveDate,
            String systemUseYn, String status, String fileToken, String originalFileName) {
        this(templateId, businessType, templateVersion, effectiveDate, systemUseYn, status, fileToken, originalFileName, List.of());
    }

    public ExcelTemplateRow withRules(List<ExcelTemplateRuleRow> nextRules) {
        return new ExcelTemplateRow(templateId, businessType, templateVersion, effectiveDate, systemUseYn, status,
                fileToken, originalFileName, nextRules == null ? List.of() : nextRules);
    }
}

package kr.ac.knue.commonfoundation.basic60;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies version, confirmed-result, validation, and audit guards to operational evaluation settings.
 */
@Service
public class Basic60Service {
    private static final Set<String> USE_FLAGS = Set.of("Y", "N");
    private static final Pattern YEAR = Pattern.compile("^[0-9]{4}$");
    private static final String DRAFT = "DRAFT";
    private final Basic60Mapper mapper;

    public Basic60Service(Basic60Mapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public OperationalSettingResponses.EvaluationElementManagementItemSettingSearchResponse listElementSettings(OperationalSettingSearchCriteria criteria) {
        return new OperationalSettingResponses.EvaluationElementManagementItemSettingSearchResponse(
                mapper.listElementSettings(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countElementSettings(criteria));
    }

    @Transactional(readOnly = true)
    public OperationalSettingResponses.ParticipationAllocationRateSettingSearchResponse listParticipationSettings(OperationalSettingSearchCriteria criteria) {
        return new OperationalSettingResponses.ParticipationAllocationRateSettingSearchResponse(
                mapper.listParticipationSettings(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countParticipationSettings(criteria));
    }

    @Transactional(readOnly = true)
    public OperationalSettingResponses.ManagementItemEvaluationScoreSettingSearchResponse listScoreSettings(OperationalSettingSearchCriteria criteria) {
        return new OperationalSettingResponses.ManagementItemEvaluationScoreSettingSearchResponse(
                mapper.listScoreSettings(criteria), Math.max(criteria.page(), 0), criteria.safeSize(), mapper.countScoreSettings(criteria));
    }

    /** Builds an XLSX export from exactly the same filtered element-setting slice used by the list endpoint. */
    @Transactional(readOnly = true)
    public byte[] downloadElementSettings(OperationalSettingSearchCriteria criteria) {
        return createWorkbook("평가요소별 관리항목 설정", listElementSettings(criteria).evaluationElementManagementItemSettings());
    }

    /** Builds an XLSX export from exactly the same filtered participation-setting slice used by the list endpoint. */
    @Transactional(readOnly = true)
    public byte[] downloadParticipationSettings(OperationalSettingSearchCriteria criteria) {
        return createWorkbook("참여구분별 배분율 설정", listParticipationSettings(criteria).participationAllocationRateSettings());
    }

    /** Builds an XLSX export from exactly the same filtered score-setting slice used by the list endpoint. */
    @Transactional(readOnly = true)
    public byte[] downloadScoreSettings(OperationalSettingSearchCriteria criteria) {
        return createWorkbook("관리항목별 평가점수 설정", listScoreSettings(criteria).managementItemEvaluationScoreSettings());
    }

    /** Saves one management-item setting only when its rule version remains editable. */
    @Transactional
    public OperationalSettingRow saveElementSetting(SaveEvaluationElementManagementItemSettingRequest request, Long userId, String requestId) {
        validateCommon(request.ruleVersionId(), request.evaluationYear(), request.activeYn(), request.effectiveStartDate(), request.effectiveEndDate(), request.changeReason());
        requireDraftRuleVersion(request.ruleVersionId());
        if (Boolean.TRUE.equals(mapper.hasConfirmedElementSettingImpact(request))) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터에 영향을 주는 평가요소별 관리항목 설정은 수정할 수 없습니다.");
        }
        OperationalSettingRow before = mapper.findElementSettingByKey(request);
        mapper.upsertElementSetting(request, userId);
        OperationalSettingRow after = mapper.findElementSettingByKey(request);
        recordIfChanged("evaluation_element_management_item_settings", elementKey(request), "management_item_name", before == null ? null : before.managementItemName(), after.managementItemName(), userId, request.changeReason(), requestId);
        recordIfChanged("evaluation_element_management_item_settings", elementKey(request), "teacher_editable_part", before == null ? null : before.teacherEditablePart(), after.teacherEditablePart(), userId, request.changeReason(), requestId);
        recordIfChanged("evaluation_element_management_item_settings", elementKey(request), "active_yn", before == null ? null : before.activeYn(), after.activeYn(), userId, request.changeReason(), requestId);
        return after;
    }

    @Transactional
    public OperationalSettingRow saveParticipationSetting(SaveParticipationAllocationRateSettingRequest request, Long userId, String requestId) {
        validateCommon(request.ruleVersionId(), request.evaluationYear(), request.activeYn(), request.effectiveStartDate(), request.effectiveEndDate(), request.changeReason());
        List<ValidationError> fields = new ArrayList<>();
        if (request.researcherCount() == null || request.researcherCount() <= 0) fields.add(new ValidationError("researcherCount", "연구자 수는 1 이상이어야 합니다."));
        if (request.allocationRate() == null || request.allocationRate().compareTo(BigDecimal.ZERO) < 0 || request.allocationRate().compareTo(BigDecimal.ONE) > 0) fields.add(new ValidationError("allocationRate", "배분율은 0 이상 1 이하이어야 합니다."));
        throwIfFields(fields, "참여구분별 배분율 저장 요청이 올바르지 않습니다.");
        requireDraftRuleVersion(request.ruleVersionId());
        if (Boolean.TRUE.equals(mapper.hasConfirmedParticipationSettingImpact(request))) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터에 영향을 주는 참여구분별 배분율은 수정할 수 없습니다.");
        }
        OperationalSettingRow before = mapper.findParticipationSettingByKey(request);
        mapper.upsertParticipationSetting(request, userId);
        OperationalSettingRow after = mapper.findParticipationSettingByKey(request);
        recordIfChanged("participation_allocation_rate_settings", participationKey(request), "allocation_rate", before == null ? null : before.allocationRate(), after.allocationRate(), userId, request.changeReason(), requestId);
        recordIfChanged("participation_allocation_rate_settings", participationKey(request), "active_yn", before == null ? null : before.activeYn(), after.activeYn(), userId, request.changeReason(), requestId);
        return after;
    }

    @Transactional
    public OperationalSettingRow saveScoreSetting(SaveManagementItemEvaluationScoreSettingRequest request, Long userId, String requestId) {
        validateCommon(request.ruleVersionId(), request.evaluationYear(), request.activeYn(), request.effectiveStartDate(), request.effectiveEndDate(), request.changeReason());
        List<ValidationError> fields = new ArrayList<>();
        if (request.evaluationScore() == null || request.evaluationScore().compareTo(BigDecimal.ZERO) < 0) fields.add(new ValidationError("evaluationScore", "평가점수는 0 이상이어야 합니다."));
        if (request.maxScore() != null && request.evaluationScore() != null && request.maxScore().compareTo(request.evaluationScore()) < 0) fields.add(new ValidationError("maxScore", "상한점수는 평가점수 이상이어야 합니다."));
        throwIfFields(fields, "관리항목별 평가점수 저장 요청이 올바르지 않습니다.");
        requireDraftRuleVersion(request.ruleVersionId());
        if (Boolean.TRUE.equals(mapper.hasConfirmedScoreSettingImpact(request))) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터에 영향을 주는 관리항목별 평가점수는 수정할 수 없습니다.");
        }
        OperationalSettingRow before = mapper.findScoreSettingByKey(request);
        mapper.upsertScoreSetting(request, userId);
        OperationalSettingRow after = mapper.findScoreSettingByKey(request);
        recordIfChanged("management_item_evaluation_score_settings", scoreKey(request), "evaluation_score", before == null ? null : before.evaluationScore(), after.evaluationScore(), userId, request.changeReason(), requestId);
        recordIfChanged("management_item_evaluation_score_settings", scoreKey(request), "active_yn", before == null ? null : before.activeYn(), after.activeYn(), userId, request.changeReason(), requestId);
        return after;
    }


    private void validateCommon(Long ruleVersionId, String evaluationYear, String activeYn, java.time.LocalDate startDate, java.time.LocalDate endDate, String changeReason) {
        List<ValidationError> fields = new ArrayList<>();
        if (ruleVersionId == null) fields.add(new ValidationError("ruleVersionId", "규정버전을 선택하세요."));
        if (evaluationYear == null || !YEAR.matcher(evaluationYear.trim()).matches()) fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        if (!USE_FLAGS.contains(activeYn == null ? null : activeYn.trim())) fields.add(new ValidationError("activeYn", "Y 또는 N을 선택하세요."));
        if (startDate == null) fields.add(new ValidationError("effectiveStartDate", "적용시작일을 입력하세요."));
        if (endDate == null) fields.add(new ValidationError("effectiveEndDate", "적용종료일을 입력하세요."));
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) fields.add(new ValidationError("effectiveEndDate", "적용종료일은 시작일 이후여야 합니다."));
        if (changeReason == null || changeReason.isBlank()) fields.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        throwIfFields(fields, "운영 설정 저장 요청이 올바르지 않습니다.");
    }

    private void requireDraftRuleVersion(Long ruleVersionId) {
        String status = mapper.findRuleVersionStatus(ruleVersionId);
        if (status == null) throw new NotFoundException("규정버전을 찾을 수 없습니다.");
        if (!DRAFT.equals(status)) throw new ConflictException("CONFIRMED_RULE_LOCKED: 확정 또는 폐기된 규정버전은 수정할 수 없습니다.");
    }

    private void throwIfFields(List<ValidationError> fields, String message) {
        if (!fields.isEmpty()) throw new BusinessValidationException(message, fields);
    }

    private void recordIfChanged(String targetBusiness, String targetKey, String fieldName, Object beforeValue, Object afterValue, Long userId, String reason, String requestId) {
        String beforeString = beforeValue == null ? null : beforeValue.toString();
        String afterString = afterValue == null ? null : afterValue.toString();
        if (!Objects.equals(beforeString, afterString)) {
            mapper.insertChangeHistory(targetBusiness, targetKey, beforeString == null ? "CREATE" : "UPDATE", fieldName, beforeString, afterString, userId, reason, requestId);
        }
    }

    private String elementKey(SaveEvaluationElementManagementItemSettingRequest r) { return r.ruleVersionId() + ":" + r.targetScope() + ":" + r.areaCode() + ":" + r.itemCode() + ":" + r.evaluationYear() + ":" + r.elementCode() + ":" + r.managementItemCode(); }
    private String participationKey(SaveParticipationAllocationRateSettingRequest r) { return r.ruleVersionId() + ":" + r.targetScope() + ":" + r.managementItemCode() + ":" + r.researcherCount() + ":" + r.participationType(); }
    private String scoreKey(SaveManagementItemEvaluationScoreSettingRequest r) { return r.ruleVersionId() + ":" + r.targetScope() + ":" + r.managementItemCode() + ":" + r.organizationCode(); }

    /** Creates a dependency-free XLSX workbook so exports remain available in the shared application runtime. */
    private byte[] createWorkbook(String sheetName, List<OperationalSettingRow> rows) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            put(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            put(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            put(zip, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"" + xml(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            put(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            put(zip, "xl/worksheets/sheet1.xml", worksheet(rows));
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("운영 설정 Excel 파일을 생성하지 못했습니다.", exception);
        }
    }

    private String worksheet(List<OperationalSettingRow> rows) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        xml.append("<row r=\"1\">").append(cell("규정버전")).append(cell("적용대상")).append(cell("평가영역")).append(cell("평가항목")).append(cell("평가요소")).append(cell("관리항목")).append(cell("소속대학")).append(cell("연구자 수")).append(cell("참여구분")).append(cell("배분율")).append(cell("평가점수")).append(cell("상한점수")).append(cell("사용여부")).append(cell("적용시작일")).append(cell("적용종료일")).append("</row>");
        for (int index = 0; index < rows.size(); index++) {
            OperationalSettingRow row = rows.get(index);
            xml.append("<row r=\"").append(index + 2).append("\">")
                    .append(cell(row.versionCode())).append(cell(row.targetScope())).append(cell(row.areaCode())).append(cell(row.itemCode())).append(cell(row.elementCode())).append(cell(row.managementItemCode())).append(cell(row.organizationCode())).append(cell(row.researcherCount())).append(cell(row.participationType())).append(cell(row.allocationRate())).append(cell(row.evaluationScore())).append(cell(row.maxScore())).append(cell(row.activeYn())).append(cell(row.effectiveStartDate())).append(cell(row.effectiveEndDate())).append("</row>");
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private String cell(Object value) { return "<c t=\"inlineStr\"><is><t>" + xml(value == null ? "" : value.toString()) + "</t></is></c>"; }
    private String xml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;"); }
    private void put(ZipOutputStream zip, String name, String value) throws IOException { zip.putNextEntry(new ZipEntry(name)); zip.write(value.getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
}

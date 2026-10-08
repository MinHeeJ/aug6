package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.common.educationachievements.EducationAchievementAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Validates real workbook rows without domain writes, and revalidates the entire staging set before commit. */
@Service
public class EmploymentRateExcelService {
    private final EmploymentRateAchievementMapper mapper;
    private final EmploymentRateAchievementService achievements;
    private final EducationAchievementAccessPolicy access;
    private final EmploymentRateFileStorage storage;
    private final ObjectMapper json;

    public EmploymentRateExcelService(EmploymentRateAchievementMapper mapper,
            EmploymentRateAchievementService achievements, EducationAchievementAccessPolicy access,
            EmploymentRateFileStorage storage, ObjectMapper json) {
        this.mapper = mapper;
        this.achievements = achievements;
        this.access = access;
        this.storage = storage;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public byte[] template(CurrentUser user) {
        authorize(user, "READ");
        Map<String, Object> template = currentTemplate();
        return EmploymentRateWorkbook.write(List.of(mapper.templateColumns(template.get("templateId").toString())));
    }

    /** Returns rejected diagnostics normally so the transaction commits before the controller returns HTTP 400. */
    @Transactional
    public Map<String, Object> upload(MultipartFile file, CurrentUser user, String requestId) {
        authorize(user, "CREATE");
        long start = System.nanoTime();
        String filename = file.getOriginalFilename();
        if (file.isEmpty() || file.getSize() > 8 * 1024 * 1024 || filename == null
                || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx") || filename.contains("/")
                || filename.contains("\\") || filename.contains("..") || filename.length() > 255) {
            EmploymentRateAchievementService.invalid("file", "8MB 이하 XLSX 파일을 선택하세요.");
        }
        Map<String, Object> template = currentTemplate();
        List<String> columns = mapper.templateColumns(template.get("templateId").toString());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException failure) {
            EmploymentRateAchievementService.invalid("file", "파일을 읽을 수 없습니다.");
            throw new IllegalStateException(failure);
        }
        List<List<String>> workbook;
        List<Map<String, Object>> errors = new ArrayList<>();
        try {
            workbook = EmploymentRateWorkbook.read(bytes);
        } catch (BusinessValidationException invalid) {
            workbook = List.of();
            errors.add(Map.of("rowNumber", 0, "errorReason", invalid.getMessage()));
        }
        if (errors.isEmpty() && (workbook.isEmpty() || !workbook.get(0).equals(columns))) {
            errors.add(Map.of("rowNumber", 1, "errorReason", "현재 템플릿 열 또는 버전이 일치하지 않습니다."));
        }
        if (errors.isEmpty() && workbook.size() < 2) {
            errors.add(Map.of("rowNumber", 1, "errorReason", "데이터 행이 없습니다."));
        }
        String uploadId = UUID.randomUUID().toString();
        List<Map<String, Object>> staged = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        if (errors.isEmpty()) {
            for (int i = 1; i < workbook.size(); i++) {
                Map<String, Object> cells = new LinkedHashMap<>();
                List<String> values = workbook.get(i);
                for (int c = 0; c < columns.size(); c++) {
                    cells.put(columns.get(c), c < values.size() ? values.get(c) : "");
                }
                Map<String, Object> stage = new HashMap<>();
                stage.put("rowNumber", i + 1);
                stage.put("payload", cells);
                stage.put("status", "NORMAL");
                try {
                    Map<String, Object> prepared = prepare(cells, user, requestId, template);
                    String identity = identity(prepared);
                    if (!identities.add(identity)) throw new ConflictException("파일 내 중복 실적입니다.");
                    achievements.ensureUnique(prepared);
                } catch (BusinessValidationException | ConflictException | ForbiddenException invalid) {
                    stage.put("status", "ERROR");
                    errors.add(Map.of("rowNumber", i + 1, "errorReason", invalid.getMessage()));
                }
                staged.add(stage);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("uploadId", uploadId);
        result.put("fileName", filename);
        result.put("templateId", template.get("templateId"));
        result.put("fileToken", storage.store(bytes, requestId));
        result.put("actor", user.userId());
        result.put("requestId", requestId);
        result.put("validationStatus", errors.isEmpty() ? "VALIDATED" : "REJECTED");
        result.put("totalCount", Math.max(0, workbook.size() - 1));
        long invalidRows = errors.stream().map(e -> e.get("rowNumber")).distinct().count();
        int total = Math.max(0, workbook.size() - 1);
        result.put("successCount", Math.max(0, total - invalidRows));
        result.put("errorCount", invalidRows);
        result.put("elapsed", (System.nanoTime() - start) / 1_000_000);
        result.put("errors", errors);
        String errorToken = null;
        if (!errors.isEmpty()) {
            List<List<String>> rows = new ArrayList<>();
            rows.add(List.of("rowNumber", "errorReason"));
            errors.forEach(e -> rows.add(List.of(e.get("rowNumber").toString(), e.get("errorReason").toString())));
            errorToken = storage.store(EmploymentRateWorkbook.write(rows), requestId);
        }
        result.put("errorFileToken", errorToken);
        mapper.upload(result);
        for (Map<String, Object> row : staged) {
            mapper.stage(uploadId, ((Number) row.get("rowNumber")).intValue(),
                    serialize(row.get("payload")), row.get("status").toString());
        }
        for (Map<String, Object> error : errors) {
            mapper.error(uploadId, ((Number) error.get("rowNumber")).intValue(), error.get("errorReason").toString());
        }
        mapper.uploadHistory(result);
        result.remove("fileToken");
        result.remove("actor");
        return result;
    }

    /** Upload lock makes recommit impossible; the last failing row cannot leave earlier rows written. */
    @Transactional
    public Map<String, Object> commit(String id, CurrentUser user, String requestId) {
        authorize(user, "EXECUTE");
        Map<String, Object> upload = owned(id, user, true);
        if (!"VALIDATED".equals(upload.get("validationStatus"))) {
            throw new ConflictException("UPLOAD_NOT_COMMITTABLE: 이미 반영되었거나 오류가 있습니다.");
        }
        Map<String, Object> template = currentTemplate();
        List<Map<String, Object>> prepared = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        for (String payload : mapper.staged(id)) {
            Map<String, Object> cells;
            try {
                cells = json.readValue(payload, new com.fasterxml.jackson.core.type.TypeReference<>() { });
            } catch (Exception invalid) {
                throw new ConflictException("보존된 검증 행이 올바르지 않습니다.");
            }
            Map<String, Object> row = prepare(cells, user, requestId, template);
            if (!identities.add(identity(row))) throw new ConflictException("중복 실적입니다.");
            achievements.ensureUnique(row);
            prepared.add(row);
        }
        if (prepared.isEmpty()) throw new ConflictException("반영할 행이 없습니다.");
        for (Map<String, Object> row : prepared) achievements.persist(row, null);
        mapper.committed(id, prepared.size(), requestId);
        return Map.of("uploadId", id, "savedCount", prepared.size());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> errors(String id, CurrentUser user) {
        authorize(user, "READ");
        owned(id, user, false);
        return mapper.errors(id);
    }

    @Transactional(readOnly = true)
    public byte[] downloadErrors(String id, CurrentUser user) {
        authorize(user, "READ");
        Map<String, Object> upload = owned(id, user, false);
        return storage.read(EmploymentRateAchievementService.text(upload.get("errorFileToken")));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> histories(CurrentUser user) {
        authorize(user, "READ");
        return mapper.histories(user.userId(), user.roles().contains("R09"));
    }

    private Map<String, Object> prepare(Map<String, Object> cells, CurrentUser user, String requestId,
            Map<String, Object> template) {
        if (!template.get("templateVersion").equals(cells.get("templateVersion"))) {
            EmploymentRateAchievementService.invalid("templateVersion", "현재 양식 버전이 아닙니다.");
        }
        Long target = mapper.employee(value(cells, "employeeNo"));
        JsonNode detail;
        LocalDate date;
        try {
            date = LocalDate.parse(value(cells, "achievementDate"));
            String input = value(cells, "achievementDetail");
            detail = input.isBlank() ? json.createObjectNode() : json.readTree(input);
        } catch (Exception invalid) {
            EmploymentRateAchievementService.invalid("achievementDate", "발생일 또는 상세 JSON이 올바르지 않습니다.");
            throw new IllegalStateException(invalid);
        }
        EmploymentRateAchievementRequest request = new EmploymentRateAchievementRequest(target,
                value(cells, "evaluationYear"), value(cells, "managementItemCode"), date,
                value(cells, "achievementName"), detail, value(cells, "attachmentRef"));
        return achievements.prepare(request, target, request.evaluationYear(), user, requestId, true);
    }

    private Map<String, Object> currentTemplate() {
        Map<String, Object> template = mapper.template();
        if (template == null) throw new NotFoundException("사용 가능한 양식이 없습니다.");
        return template;
    }

    private Map<String, Object> owned(String id, CurrentUser user, boolean lock) {
        Map<String, Object> upload = mapper.findUpload(id, lock);
        if (upload == null) throw new NotFoundException("업로드가 없습니다.");
        access.requireUploadOwner(user, EmploymentRateAchievementService.number(upload.get("uploaderUserId")));
        return upload;
    }

    private void authorize(CurrentUser user, String type) {
        access.requireExcel(user);
        achievements.function(user, type, "DRAFT", Set.of("R07"));
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception failure) {
            throw new IllegalStateException("상세 직렬화 실패", failure);
        }
    }

    private String value(Map<String, Object> cells, String field) {
        return Objects.toString(cells.get(field), "").trim();
    }

    private String identity(Map<String, Object> row) {
        return row.get("teacherUserId") + "|" + row.get("evaluationYear") + "|"
                + row.get("managementItemCode") + "|" + row.get("achievementDate");
    }
}

package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** Durable validation diagnostics are separate from the atomic all-row domain commit. */
@Service
public class EmploymentRateExcelService {
    private static final Logger log = LoggerFactory.getLogger(EmploymentRateExcelService.class);
    private final EmploymentRateAchievementMapper mapper;
    private final EmploymentRateAchievementService achievements;
    private final EmploymentRateXlsxCodec codec;
    private final EmploymentRateFileStoragePort storage;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;

    public EmploymentRateExcelService(
            EmploymentRateAchievementMapper mapper,
            EmploymentRateAchievementService achievements,
            EmploymentRateXlsxCodec codec,
            EmploymentRateFileStoragePort storage,
            ObjectMapper json,
            PlatformTransactionManager manager) {
        this.mapper = mapper;
        this.achievements = achievements;
        this.codec = codec;
        this.storage = storage;
        this.json = json;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public byte[] template(CurrentUser user) {
        EmploymentRateAchievementService.requireRole(user, "R07");
        return codec.write(List.of(columns()));
    }

    private Map<String, Object> templateRow() {
        Map<String, Object> template = mapper.template();
        if (template == null) {
            throw new ConflictException("TEMPLATE_NOT_ACTIVE: 활성 업로드 양식이 없습니다.");
        }
        return template;
    }

    private List<String> columns() {
        return mapper.columns((String) templateRow().get("templateId"));
    }

    /** Validation never creates business rows; all raw rows, original and diagnostic XLSX are retained. */
    public Map<String, Object> upload(MultipartFile file, CurrentUser user, String requestId) {
        EmploymentRateAchievementService.requireRole(user, "R07");
        String filename = file.getOriginalFilename();
        if (file.isEmpty() || file.getSize() > EmploymentRateXlsxCodec.MAX_BYTES || filename == null
                || !filename.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")
                || filename.contains("/") || filename.contains("\\") || filename.length() > 255) {
            throw new IllegalArgumentException("5MB 이하의 .xlsx 파일을 선택하세요.");
        }
        byte[] original;
        try {
            original = file.getBytes();
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("업로드 파일을 읽지 못했습니다.");
        }
        List<List<String>> rows = codec.read(original);
        List<String> columns = columns();
        if (rows.isEmpty() || !rows.get(0).equals(columns)) {
            throw new IllegalArgumentException("업로드 양식의 열과 버전을 확인하세요.");
        }
        if (rows.size() < 2) {
            throw new IllegalArgumentException("실적 행이 없는 파일입니다.");
        }
        String token = UUID.randomUUID().toString();
        String uploadId = "ER-UP-" + UUID.randomUUID();
        storage.put(token, original);
        try {
            return transaction.execute(status -> validate(rows, columns, filename, token, uploadId, user, requestId));
        } catch (RuntimeException exception) {
            cleanup(token, requestId);
            cleanup(token + "-errors", requestId);
            throw exception;
        }
    }

    private Map<String, Object> validate(
            List<List<String>> rows, List<String> columns, String filename, String token,
            String uploadId, CurrentUser user, String requestId) {
        List<Map<String, Object>> staging = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        List<List<String>> diagnostics = new ArrayList<>();
        diagnostics.add(List.of("행번호", "오류사유"));
        Set<String> identities = new HashSet<>();
        for (int i = 1; i < rows.size(); i++) {
            Map<String, Object> raw = new LinkedHashMap<>();
            for (int c = 0; c < columns.size(); c++) {
                raw.put(columns.get(c), c < rows.get(i).size() ? rows.get(i).get(c) : "");
            }
            raw.put("requestId", requestId);
            String errorReason = null;
            try {
                Map<String, Object> prepared = prepare(raw, user, requestId);
                achievements.checkDuplicate(prepared);
                if (!identities.add(identity(prepared))) {
                    throw new ConflictException("동일 파일 내 중복 실적입니다.");
                }
            } catch (RuntimeException exception) {
                errorReason = safeReason(exception);
            }
            Map<String, Object> stage = parameters(uploadId, user);
            stage.put("rowId", UUID.randomUUID().toString());
            stage.put("rowNumber", i + 1);
            stage.put("payload", achievements.serialize(raw));
            stage.put("validationStatus", errorReason == null ? "NORMAL" : "ERROR");
            staging.add(stage);
            if (errorReason != null) {
                Map<String, Object> error = new LinkedHashMap<>(stage);
                error.put("errorId", UUID.randomUUID().toString());
                error.put("errorCode", "ROW_INVALID");
                error.put("errorReason", errorReason);
                errors.add(error);
                diagnostics.add(List.of(String.valueOf(i + 1), errorReason));
            }
        }
        storage.put(token + "-errors", codec.write(diagnostics));
        Map<String, Object> p = parameters(uploadId, user);
        p.put("templateId", templateRow().get("templateId"));
        p.put("filename", filename);
        p.put("fileToken", token);
        p.put("validationStatus", errors.isEmpty() ? "VALIDATED" : "REJECTED");
        p.put("totalCount", staging.size());
        p.put("successCount", staging.size() - errors.size());
        p.put("errorCount", errors.size());
        p.put("savedCount", 0);
        mapper.upload(p);
        staging.forEach(mapper::stage);
        errors.forEach(mapper::error);
        mapper.history(p);
        return Map.of("uploadId", uploadId, "totalCount", staging.size(),
                "successCount", staging.size() - errors.size(), "errorCount", errors.size(),
                "savedCount", 0, "errors", mapper.errors(uploadId));
    }

    private Map<String, Object> prepare(Map<String, Object> raw, CurrentUser user, String requestId) {
        Map<String, Object> teacher = mapper.teacher(String.valueOf(raw.get("교번")));
        if (teacher == null) {
            throw new IllegalArgumentException("존재하지 않는 교번입니다.");
        }
        LocalDate date = LocalDate.parse(String.valueOf(raw.get("업적발생일")));
        EmploymentRateAchievementRequest request = new EmploymentRateAchievementRequest(
                String.valueOf(raw.get("관리항목코드")), date, String.valueOf(raw.get("실적명")),
                null, String.valueOf(raw.get("첨부참조")));
        return achievements.prepare(request, ((Number) teacher.get("teacherUserId")).longValue(),
                String.valueOf(date.getYear()), user, requestId);
    }

    /** Upload row lock serializes retry/commit; all guards run again before the first insert. */
    public Map<String, Object> commit(String id, CurrentUser user, String requestId) {
        EmploymentRateAchievementService.requireRole(user, "R07");
        owned(mapper.uploadInfo(id), user);
        try {
            return transaction.execute(status -> {
                Map<String, Object> upload = owned(mapper.lockUpload(id), user);
                if (!"VALIDATED".equals(upload.get("validationStatus")) || !mapper.errors(id).isEmpty()) {
                    throw new ConflictException("UPLOAD_NOT_COMMITTABLE: 오류 또는 이미 반영된 업로드입니다.");
                }
                List<Map<String, Object>> prepared = new ArrayList<>();
                Set<String> keys = new HashSet<>();
                for (Map<String, Object> stage : mapper.stages(id)) {
                    Map<String, Object> raw;
                    try {
                        raw = json.readValue((String) stage.get("payload"),
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { });
                    } catch (Exception exception) {
                        throw new ConflictException("STAGING_INVALID: 보존된 행을 확인하세요.");
                    }
                    Map<String, Object> row = prepare(raw, user, requestId);
                    achievements.checkDuplicate(row);
                    if (!keys.add(identity(row))) {
                        throw new ConflictException("DUPLICATE_DATA: 파일 내 중복입니다.");
                    }
                    prepared.add(row);
                }
                if (prepared.isEmpty()) {
                    throw new ConflictException("STAGING_EMPTY: 반영할 행이 없습니다.");
                }
                for (Map<String, Object> row : prepared) {
                    achievements.write(row, null);
                }
                mapper.committed(id);
                Map<String, Object> p = parameters(id, user);
                p.put("totalCount", prepared.size());
                p.put("successCount", prepared.size());
                p.put("errorCount", 0);
                p.put("savedCount", prepared.size());
                mapper.history(p);
                return Map.of("uploadId", id, "savedCount", prepared.size());
            });
        } catch (RuntimeException failure) {
            // Business rollback completes before this independent durable diagnostic transaction.
            transaction.execute(status -> {
                Map<String, Object> upload = owned(mapper.lockUpload(id), user);
                if (!"COMMITTED".equals(upload.get("validationStatus")) && mapper.errors(id).isEmpty()) {
                    Map<String, Object> error = parameters(id, user);
                    error.put("rowNumber", 0);
                    error.put("errorId", UUID.randomUUID().toString());
                    error.put("errorCode", "COMMIT_FAILED");
                    error.put("errorReason", safeReason(failure));
                    mapper.error(error);
                }
                return null;
            });
            if (failure instanceof org.springframework.dao.DataIntegrityViolationException) {
                throw new ConflictException("DUPLICATE_DATA: 반영 중 중복 또는 데이터 충돌이 발생했습니다.");
            }
            throw failure;
        }
    }

    public List<Map<String, Object>> histories(CurrentUser user) {
        EmploymentRateAchievementService.requireRole(user, "R07");
        return mapper.histories(Map.of("userId", user.userId(), "admin", user.roles().contains("R09")));
    }

    public List<Map<String, Object>> errors(String id, CurrentUser user) {
        EmploymentRateAchievementService.requireRole(user, "R07");
        owned(mapper.uploadInfo(id), user);
        return mapper.errors(id);
    }

    public byte[] errorsFile(String id, CurrentUser user) {
        errors(id, user);
        String token = (String) mapper.uploadInfo(id).get("fileToken");
        // Original validation diagnostic is retained; later commit diagnostics are returned separately in errors().
        return storage.read(token + "-errors");
    }

    private Map<String, Object> owned(Map<String, Object> upload, CurrentUser user) {
        if (upload == null) {
            throw new NotFoundException("업로드를 찾을 수 없습니다.");
        }
        if (!user.roles().contains("R09") && !user.userId().equals(upload.get("uploaderUserId"))) {
            throw new ForbiddenException();
        }
        return upload;
    }

    private String identity(Map<String, Object> row) {
        return achievements.serialize(List.of(row.get("teacherUserId"), row.get("managementItemCode"),
                row.get("achievementDate").toString(), row.get("achievementName")));
    }

    private Map<String, Object> parameters(String uploadId, CurrentUser user) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("uploadId", uploadId);
        p.put("actorId", user.userId());
        return p;
    }

    private String safeReason(RuntimeException exception) {
        if (exception instanceof IllegalArgumentException || exception instanceof ConflictException) {
            return exception.getMessage().length() > 450 ? "입력값을 확인하세요." : exception.getMessage();
        }
        return "권한, 관리항목, 입력기간 또는 실적 입력값을 확인하세요.";
    }

    private void cleanup(String token, String requestId) {
        try {
            storage.delete(token);
        } catch (RuntimeException failure) {
            log.error("Excel new-file compensation failed, requestId={}", requestId, failure);
        }
    }
}

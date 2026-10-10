package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.*;
import kr.ac.knue.commonfoundation.excel.FileStoragePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

/** Owns scoped reads, atomic ledger/history writes, and validation-before-commit Excel transactions. */
@Service
public class EmploymentRateAchievementService {
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardMapper guards;
    private final ObjectMapper json;
    private final FileStoragePort files;
    private final TransactionTemplate transaction;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardMapper guards,
            ObjectMapper json,
            FileStoragePort files,
            PlatformTransactionManager manager) {
        this.mapper = mapper;
        this.guards = guards;
        this.json = json;
        this.files = files;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(Map<String, Object> filters, CurrentUser user) {
        authorize(user, "READ", "R01", "R02", "R04");
        Map<String, Object> query = query(filters, user);
        return Map.of("achievements", mapper.list(query), "totalElements", mapper.count(query),
                "page", query.get("page"), "pageSize", query.get("pageSize"), "managementItems", mapper.managementItems());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id, CurrentUser user) {
        authorize(user, "READ", "R01", "R02", "R04");
        Map<String, Object> row = existing(id, false);
        requireScope(row, user);
        return row;
    }

    /** Path identity, owner and evaluation year remain immutable during update. */
    @Transactional
    public Map<String, Object> save(Long id, EmploymentRateAchievementRequest request, CurrentUser user, String requestId) {
        role(user, "R01");
        Map<String, Object> before = id == null ? null : existing(id, true);
        Long teacher = before == null ? user.userId() : number(before.get("teacherUserId"));
        if (!user.roles().contains("R09") && !teacher.equals(user.userId())) {
            throw new ForbiddenException();
        }
        String year = before == null ? request.evaluationYear() == null
                ? String.valueOf(request.achievementDate().getYear()) : request.evaluationYear()
                : before.get("evaluationYear").toString();
        Map<String, Object> row = values(request, teacher, year, user.userId());
        mutationGuards(row, before);
        authorize(user, before == null ? "CREATE" : "UPDATE", "R01");
        String next = before == null ? "DRAFT" : before.get("achievementStatus").toString();
        if (request.achievementStatus() != null && !request.achievementStatus().equals(next)) {
            if (before == null || !"SUBMITTED".equals(request.achievementStatus())
                    || !Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED").contains(next)) {
                throw conflict("INVALID_STATE_TRANSITION", "허용되지 않은 상태 전이입니다.");
            }
            next = "SUBMITTED";
        }
        row.put("achievementStatus", next);
        row.put("achievementId", id);
        if (mapper.duplicate(row) > 0) {
            throw conflict("DUPLICATE_ACHIEVEMENT", "중복 실적은 저장할 수 없습니다.");
        }
        return persist(row, before, requestId);
    }

    private Map<String, Object> persist(Map<String, Object> row, Map<String, Object> before, String requestId) {
        if (before == null) {
            row.put("managementNo", "ERA-" + UUID.randomUUID());
            mapper.insert(row);
        } else if (mapper.update(row) != 1) {
            throw conflict("INVALID_STATE_TRANSITION", "수정 가능한 상태가 아닙니다.");
        }
        Map<String, Object> saved = existing(number(row.get("achievementId")), false);
        Map<String, Object> history = new HashMap<>(row);
        history.put("achievementId", row.get("achievementId").toString());
        history.put("requestId", requestId);
        history.put("changeType", before == null ? "CREATE" : "UPDATE");
        history.put("before", before == null ? null : serialize(before));
        history.put("after", serialize(saved));
        mapper.history(history);
        if (before == null || !Objects.equals(before.get("achievementStatus"), saved.get("achievementStatus"))) {
            history.put("previousStatus", before == null ? null : before.get("achievementStatus"));
            mapper.statusHistory(history);
        }
        boolean warning = guards.countEvaluationDatePeriods(row.get("evaluationYear").toString(),
                number(row.get("teacherUserId")), (LocalDate) row.get("achievementDate")) == 0;
        return Map.of("achievement", saved, "occurredDateWarning", warning,
                "warningMessage", warning ? "업적발생일이 평가대상 기간 밖입니다. 저장은 허용됩니다." : "");
    }

    private void mutationGuards(Map<String, Object> row, Map<String, Object> before) {
        String year = row.get("evaluationYear").toString();
        Long teacher = number(row.get("teacherUserId"));
        if (guards.countActiveInputPeriods(year, teacher) == 0) {
            throw conflict("PERIOD_NOT_ACTIVE", "활성 입력기간이 아닙니다.");
        }
        if (guards.countEvaluationConfirmations(teacher, year) > 0
                || before != null && "EVALUATION_CONFIRMED".equals(before.get("achievementStatus"))) {
            throw conflict("CONFIRMED_DATA_LOCKED", "평가확정 실적은 변경할 수 없습니다.");
        }
        if (before != null && !Set.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED")
                .contains(before.get("achievementStatus"))) {
            throw conflict("INVALID_STATE_TRANSITION", "수정 가능한 상태가 아닙니다.");
        }
        if (mapper.managementItem(row) != 1) {
            invalid("managementItemCode", "평가연도의 유효한 교육영역 관리항목을 선택하세요.");
        }
        if (row.get("organizationCode") == null) {
            throw new ForbiddenException();
        }
        if (row.get("attachmentRef") != null && mapper.attachment(row) == 0) {
            invalid("attachmentRef", "대상 교원이 소유한 파일 참조만 사용할 수 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public byte[] download(Map<String, Object> filters, CurrentUser user) {
        authorize(user, "READ", "R01", "R02", "R04", "R07");
        Map<String, Object> query = query(filters, user);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조"));
        for (Map<String, Object> row : mapper.list(query)) {
            rows.add(List.of(text(row.get("employeeNo")), text(row.get("managementItemCode")),
                    text(row.get("achievementDate")), text(row.get("title")), text(row.get("attachmentRef"))));
        }
        return EmploymentRateWorkbook.write(rows);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> job(String id, CurrentUser user) {
        authorize(user, "READ", "R07");
        Map<String, Object> job = mapper.job(id);
        if (job == null) {
            throw new NotFoundException("작업이 없습니다.");
        }
        owner(job.get("requesterUserId"), user);
        job = new HashMap<>(job);
        job.put("items", mapper.jobItems(id));
        return job;
    }

    public void bulk(EmploymentRateBulkJobRequest body, CurrentUser user) {
        authorize(user, "EXECUTE", "R07");
        if (!body.targetConditionJson().isObject()) {
            invalid("targetConditionJson", "대상조건은 객체로 입력하세요.");
        }
        throw conflict("BULK_POLICY_NOT_APPROVED", "생성조건과 삭제 허용 상태가 승인되지 않았습니다.");
    }

    @Transactional(readOnly = true)
    public byte[] template(CurrentUser user) {
        authorize(user, "READ", "R07");
        String template = templateId();
        return EmploymentRateWorkbook.write(List.of(mapper.templateColumns(template)));
    }

    /** Validation persists only evidence. A failing workbook never creates a ledger row. */
    public Map<String, Object> upload(MultipartFile file, CurrentUser user, String requestId) {
        authorize(user, "EXECUTE", "R07");
        if (file.isEmpty() || file.getSize() > 8 * 1024 * 1024 || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            invalid("file", "8MB 이하의 XLSX 파일을 선택하세요.");
        }
        String template = templateId();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException failure) {
            throw new EmploymentRateException("INVALID_WORKBOOK", "파일을 읽을 수 없습니다.", 400);
        }
        List<List<String>> workbook;
        List<Map<String, Object>> staged = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        try {
            workbook = EmploymentRateWorkbook.read(bytes);
            if (workbook.size() < 2 || !workbook.get(0).equals(mapper.templateColumns(template))) {
                throw new EmploymentRateException("INVALID_WORKBOOK", "템플릿 열과 데이터 행을 확인하세요.", 400);
            }
            Set<String> identities = new HashSet<>();
            for (int index = 1; index < workbook.size(); index++) {
                List<String> cells = workbook.get(index);
                Map<String, Object> stage = new HashMap<>();
                stage.put("rowNumber", index + 1);
                try {
                    if (cells.size() < 4 || cells.size() > 5 || cells.get(3).isBlank()) {
                        invalid("row", "필수 열과 실적명을 확인하세요.");
                    }
                    Long teacher = mapper.teacher(cells.get(0));
                    if (teacher == null) {
                        invalid("employeeNo", "등록된 교원의 교번이 아닙니다.");
                    }
                    LocalDate date = LocalDate.parse(cells.get(2));
                    var request = new EmploymentRateAchievementRequest(cells.get(1), date, null, cells.get(3),
                            cells.size() == 5 ? nullable(cells.get(4)) : null, null);
                    Map<String, Object> row = values(request, teacher, String.valueOf(date.getYear()), user.userId());
                    requireScope(row, user);
                    mutationGuards(row, null);
                    row.put("achievementStatus", "DRAFT");
                    String key = teacher + "|" + date + "|" + cells.get(1) + "|" + cells.get(3);
                    if (!identities.add(key) || mapper.duplicate(row) > 0) {
                        invalid("row", "중복 실적입니다. 기존 값은 유지됩니다.");
                    }
                    stage.put("payload", serialize(row));
                    stage.put("validationStatus", "NORMAL");
                } catch (RuntimeException failure) {
                    stage.put("payload", serialize(Map.of("cells", cells)));
                    stage.put("validationStatus", "ERROR");
                    errors.add(Map.of("rowNumber", index + 1, "reason", safeReason(failure),
                            "inputValue", String.join(" / ", cells)));
                }
                staged.add(stage);
            }
        } catch (EmploymentRateException failure) {
            errors.add(Map.of("rowNumber", 1, "reason", failure.getMessage(), "inputValue", ""));
        }
        String uploadId = UUID.randomUUID().toString();
        String token = files.save(bytes);
        Map<String, Object> values = new HashMap<>();
        values.put("uploadId", uploadId);
        values.put("templateId", template);
        values.put("fileToken", token);
        values.put("fileName", PathName(file.getOriginalFilename()));
        values.put("actorId", user.userId());
        values.put("validationStatus", errors.isEmpty() ? "VALIDATED" : "REJECTED");
        values.put("totalCount", Math.max(staged.size(), errors.isEmpty() ? 0 : 1));
        values.put("errorCount", errors.size());
        values.put("successCount", staged.stream().filter(s -> "NORMAL".equals(s.get("validationStatus"))).count());
        try {
            transaction.executeWithoutResult(status -> {
                mapper.upload(values);
                for (Map<String, Object> stage : staged) {
                    stage.put("uploadId", uploadId);
                    stage.put("rowId", UUID.randomUUID().toString());
                    mapper.staging(stage);
                }
                for (Map<String, Object> error : errors) {
                    var diagnostic = new HashMap<>(error);
                    diagnostic.put("uploadId", uploadId);
                    diagnostic.put("errorId", UUID.randomUUID().toString());
                    mapper.error(diagnostic);
                }
                mapper.uploadHistory(values);
            });
        } catch (RuntimeException failure) {
            files.delete(token);
            throw failure;
        }
        Map<String, Object> result = Map.of("uploadId", uploadId, "validationStatus", values.get("validationStatus"),
                "totalCount", values.get("totalCount"), "successCount", values.get("successCount"),
                "errorCount", errors.size(), "savedCount", 0, "rows", staged,
                "errorsUrl", "/api/business/employment-rate-achievements/excel-uploads/" + uploadId + "/errors",
                "errorDownloadUrl", "/api/business/employment-rate-achievements/excel-uploads/"
                        + uploadId + "/errors/download", "requestId", requestId);
        if (!errors.isEmpty()) {
            throw new EmploymentRateException("EXCEL_VALIDATION_FAILED", "오류행이 있어 반영하지 않았습니다.", 400, result);
        }
        return result;
    }

    /** Revalidates every staged row before the first write; transaction failure leaves diagnostics intact. */
    public Map<String, Object> commit(String uploadId, CurrentUser user, String requestId) {
        authorize(user, "EXECUTE", "R07");
        return transaction.execute(status -> {
            Map<String, Object> upload = ownedUpload(uploadId, user, true);
            if (!"VALIDATED".equals(upload.get("validationStatus"))) {
                throw conflict("UPLOAD_NOT_VALIDATED", "검증 성공한 미반영 업로드만 반영할 수 있습니다.");
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            Set<String> keys = new HashSet<>();
            for (Map<String, Object> stage : mapper.stagingRows(uploadId)) {
                if (!"NORMAL".equals(stage.get("validationStatus"))) {
                    throw conflict("UPLOAD_NOT_VALIDATED", "오류행이 있어 반영하지 않았습니다.");
                }
                Map<String, Object> row = parse(stage.get("payload").toString());
                row.put("teacherUserId", number(row.get("teacherUserId")));
                row.put("achievementDate", LocalDate.parse(row.get("achievementDate").toString()));
                row.put("actorId", user.userId());
                requireScope(row, user);
                mutationGuards(row, null);
                String key = row.get("teacherUserId") + "|" + row.get("evaluationYear") + "|"
                        + row.get("managementItemCode") + "|" + row.get("achievementDate") + "|" + row.get("title");
                if (!keys.add(key) || mapper.duplicate(row) > 0) {
                    throw conflict("DUPLICATE_ACHIEVEMENT", "중복행으로 전체 반영을 취소했습니다.");
                }
                rows.add(row);
            }
            if (rows.isEmpty()) {
                throw conflict("UPLOAD_NOT_VALIDATED", "반영할 행이 없습니다.");
            }
            for (Map<String, Object> row : rows) {
                persist(row, null, requestId);
            }
            mapper.committed(uploadId);
            mapper.savedCount(Map.of("uploadId", uploadId, "savedCount", rows.size()));
            return Map.of("uploadId", uploadId, "savedCount", rows.size(), "validationStatus", "COMMITTED");
        });
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> histories(CurrentUser user) {
        authorize(user, "READ", "R07");
        return mapper.histories(query(Map.of(), user));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> errors(String uploadId, CurrentUser user) {
        authorize(user, "READ", "R07");
        ownedUpload(uploadId, user, false);
        return mapper.errors(uploadId);
    }

    public byte[] errorDownload(String uploadId, CurrentUser user) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("행번호", "열", "오류사유"));
        for (Map<String, Object> row : errors(uploadId, user)) {
            rows.add(List.of(text(row.get("rowNumber")), text(row.get("columnName")), text(row.get("errorReason"))));
        }
        return EmploymentRateWorkbook.write(rows);
    }

    private Map<String, Object> ownedUpload(String id, CurrentUser user, boolean lock) {
        Map<String, Object> upload = mapper.findUpload(Map.of("uploadId", id, "lock", lock));
        if (upload == null) {
            throw new NotFoundException("업로드가 없습니다.");
        }
        owner(upload.get("uploaderUserId"), user);
        return upload;
    }

    private void owner(Object owner, CurrentUser user) {
        if (!user.roles().contains("R09") && !number(owner).equals(user.userId())) {
            throw new ForbiddenException();
        }
    }

    private void requireScope(Map<String, Object> row, CurrentUser user) {
        Map<String, Object> scope = new HashMap<>(row);
        scope.put("roles", user.roles());
        scope.put("userId", user.userId());
        if (mapper.scope(scope) == 0) {
            throw new ForbiddenException();
        }
    }

    private Map<String, Object> existing(Long id, boolean lock) {
        Map<String, Object> row = mapper.find(Map.of("achievementId", id, "lock", lock));
        if (row == null) {
            throw new NotFoundException("실적이 없습니다.");
        }
        return row;
    }

    private Map<String, Object> values(EmploymentRateAchievementRequest request, Long teacher, String year, Long actor) {
        Map<String, Object> values = new HashMap<>();
        values.put("teacherUserId", teacher);
        values.put("organizationCode", mapper.organization(teacher));
        values.put("evaluationYear", year);
        values.put("managementItemCode", request.managementItemCode().trim());
        values.put("achievementDate", request.achievementDate());
        values.put("title", request.title().trim());
        values.put("attachmentRef", nullable(request.attachmentRef()));
        values.put("actorId", actor);
        return values;
    }

    private Map<String, Object> query(Map<String, Object> filters, CurrentUser user) {
        Map<String, Object> query = new HashMap<>();
        filters.forEach((key, value) -> {
            if (value != null && !value.toString().isBlank()) {
                query.put(key, value);
            }
        });
        int page = Integer.parseInt(query.getOrDefault("page", 0).toString());
        int size = Integer.parseInt(query.getOrDefault("pageSize", 20).toString());
        if (page < 0 || !List.of(20, 50, 100).contains(size)) {
            invalid("pageSize", "20, 50, 100건과 0 이상의 페이지를 선택하세요.");
        }
        query.put("page", page);
        query.put("pageSize", size);
        query.put("pageOffset", (long) page * size);
        query.put("userId", user.userId());
        query.put("roles", user.roles());
        return query;
    }

    private void authorize(CurrentUser user, String function, String... roles) {
        role(user, roles);
        if (!user.roles().contains("R09") && mapper.function(Map.of("roles", user.roles(), "functionType", function)) == 0) {
            throw new ForbiddenException();
        }
    }

    static void role(CurrentUser user, String... roles) {
        if (user == null) {
            throw new UnauthenticatedException();
        }
        if (user.roles() == null || !user.roles().contains("R09")
                && user.roles().stream().noneMatch(List.of(roles)::contains)) {
            throw new ForbiddenException();
        }
    }

    private String templateId() {
        String id = mapper.template();
        if (id == null) {
            throw new NotFoundException("활성 템플릿이 없습니다.");
        }
        return id;
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("이력 직렬화에 실패했습니다.", failure);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String value) {
        try {
            return json.readValue(value, HashMap.class);
        } catch (java.io.IOException failure) {
            throw conflict("INVALID_STAGING", "검증 자료를 읽을 수 없습니다.");
        }
    }

    private static Long number(Object value) {
        return Long.valueOf(value.toString());
    }

    private static String nullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String text(Object value) {
        return Objects.toString(value, "");
    }

    private static String PathName(String value) {
        return value.replace('\\', '/').substring(value.replace('\\', '/').lastIndexOf('/') + 1);
    }

    private static String safeReason(RuntimeException failure) {
        if (failure instanceof EmploymentRateException || failure instanceof BusinessValidationException) {
            return failure.getMessage();
        }
        if (failure instanceof ForbiddenException) {
            return "대상 교원에 대한 범위 권한이 없습니다.";
        }
        return "행의 교번·관리항목·날짜·실적명을 확인하세요.";
    }

    private static void invalid(String field, String message) {
        throw new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private static EmploymentRateException conflict(String code, String message) {
        return new EmploymentRateException(code, message, 409);
    }
}

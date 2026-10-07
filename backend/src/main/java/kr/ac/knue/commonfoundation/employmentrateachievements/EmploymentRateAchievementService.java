package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.common.storage.FileStoragePort;
import kr.ac.knue.commonfoundation.common.storage.FileStorageTransactionSupport;
import kr.ac.knue.commonfoundation.common.storage.StoredFile;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** Owns scoped CRUD, complete snapshots, and retained XLSX validation separate from atomic domain commit. */
@Service
public class EmploymentRateAchievementService {
    private static final String SCREEN = "SCR-EMPLOYMENT-RATE-ACHIEVEMENT";
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardMapper guard;
    private final FunctionPermissionService permissions;
    private final FileStoragePort storage;
    private final FileStorageTransactionSupport files;
    private final ObjectMapper json;
    private final TransactionTemplate diagnostics;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardMapper guard,
            FunctionPermissionService permissions,
            FileStoragePort storage,
            FileStorageTransactionSupport files,
            ObjectMapper json,
            PlatformTransactionManager transactions) {
        this.mapper = mapper;
        this.guard = guard;
        this.permissions = permissions;
        this.storage = storage;
        this.files = files;
        this.json = json;
        this.diagnostics = new TransactionTemplate(transactions);
        this.diagnostics.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Uses the same SQL scope and predicates for list, count and detail; roles contribute a union. */
    @Transactional(readOnly = true)
    public Map<String, Object> list(Map<String, Object> filters, CurrentUser user) {
        requireRole(user, "R01", "R02", "R04");
        function(user, "READ", "DRAFT", "R01", "R02", "R04");
        return search(filters, user);
    }

    private Map<String, Object> search(Map<String, Object> filters, CurrentUser user) {
        Map<String, Object> query = query(user);
        query.putAll(filters);
        int page = ((Number) filters.getOrDefault("page", 0)).intValue();
        int size = ((Number) filters.getOrDefault("pageSize", 20)).intValue();
        if (page < 0 || !List.of(20, 50, 100).contains(size)) invalid("pageSize", "표시 건수와 페이지를 확인하세요.");
        query.put("pageOffset", (long) page * size);
        query.put("pageSize", size);
        List<Map<String, Object>> rows = mapper.list(query).stream().map(this::response).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("achievements", rows);
        result.put("page", page);
        result.put("pageSize", size);
        result.put("totalElements", mapper.count(query));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id, CurrentUser user) {
        requireRole(user, "R01", "R02", "R04");
        function(user, "READ", "DRAFT", "R01", "R02", "R04");
        Map<String, Object> query = query(user);
        query.put("achievementId", id);
        Map<String, Object> row = mapper.find(query);
        if (row == null) throw new NotFoundException("실적을 찾을 수 없습니다.");
        return response(row);
    }

    /** Generates the header key directly, and stores the initial status and snapshot in the same transaction. */
    @Transactional
    public Map<String, Object> create(EmploymentRateAchievementRequest request, CurrentUser user, String requestId) {
        requireRole(user, "R01");
        return save(null, request, user, user.userId(), null, requestId, false);
    }

    /** Locks the scoped source; owner/year cannot be changed by PUT or by an out-of-year occurrence date. */
    @Transactional
    public Map<String, Object> update(
            Long id, EmploymentRateAchievementRequest request, CurrentUser user, String requestId) {
        requireRole(user, "R01");
        Map<String, Object> query = query(user);
        query.put("achievementId", id);
        Map<String, Object> before = mapper.lock(query);
        if (before == null) throw new NotFoundException("실적을 찾을 수 없습니다.");
        Long owner = ((Number) before.get("teacherUserId")).longValue();
        if (!user.roles().contains("R09") && !owner.equals(user.userId())) throw new ForbiddenException();
        return save(before, request, user, owner, null, requestId, false);
    }

    private Map<String, Object> save(
            Map<String, Object> before,
            EmploymentRateAchievementRequest request,
            CurrentUser user,
            Long owner,
            String savedOrganization,
            String requestId,
            boolean excel) {
        if (request == null || request.managementItemCode() == null || request.managementItemCode().isBlank()) {
            invalid("managementItemCode", "관리항목을 입력하세요.");
        }
        if (request.achievementDate() == null) invalid("achievementDate", "업적발생일을 입력하세요.");
        String year = before == null ? String.valueOf(request.achievementDate().getYear())
                : before.get("evaluationYear").toString();
        String organization = before == null
                ? (savedOrganization == null ? organization(owner) : savedOrganization)
                : before.get("organizationCode").toString();
        String status = before == null ? "DRAFT" : before.get("achievementStatus").toString();
        guards(owner, year, status);
        function(user, excel ? "EXECUTE" : before == null ? "CREATE" : "UPDATE", status, excel ? "R07" : "R01");
        Map<String, Object> row = query(user);
        row.put("teacherUserId", owner);
        row.put("organizationCode", organization);
        row.put("evaluationYear", year);
        row.put("managementItemCode", request.managementItemCode().trim());
        row.put("achievementDate", request.achievementDate());
        row.put("achievementName", request.achievementName());
        row.put("requestId", requestId);
        row.put("achievementId", before == null ? null : before.get("achievementId"));
        validateItem(row);
        List<String> attachments = request.attachmentIds() == null ? List.of() : request.attachmentIds();
        validateAttachments(attachments, owner);
        row.put("attachmentJson", serialize(attachments));
        if (mapper.duplicate(row) > 0) throw new ConflictException("DUPLICATE_ACHIEVEMENT: 중복 실적입니다.");
        if (before == null) {
            row.put("managementNo", "ERA-" + UUID.randomUUID());
            mapper.insert(row);
            mapper.initialStatus(row);
        } else if (mapper.update(row) != 1) {
            throw new ConflictException("실적 상태가 변경되었습니다.");
        }
        Map<String, Object> lookup = query(user);
        // Excel operators do not acquire ordinary read scope: this internal lookup follows validated target scope.
        if (excel) lookup.put("admin", true);
        lookup.put("achievementId", row.get("achievementId"));
        Map<String, Object> after = mapper.find(lookup);
        if (after == null) throw new IllegalStateException("저장 결과를 찾을 수 없습니다.");
        Map<String, Object> history = new HashMap<>();
        history.put("targetKey", row.get("achievementId").toString());
        history.put("changeType", before == null ? "CREATE" : "UPDATE");
        history.put("beforeValue", before == null ? null : serialize(response(before)));
        history.put("afterValue", serialize(response(after)));
        history.put("userId", user.userId());
        history.put("requestId", requestId);
        mapper.history(history);
        boolean warning = guard.countEvaluationDatePeriods(year, owner, request.achievementDate()) == 0;
        Map<String, Object> result = new HashMap<>();
        result.put("achievement", response(after));
        result.put("occurredDateWarning", warning);
        result.put("warningMessage", warning ? "업적발생일이 평가대상 기간 밖입니다." : null);
        return result;
    }

    private void guards(Long owner, String year, String status) {
        if (guard.countActiveInputPeriods(year, owner) == 0) {
            throw new ConflictException("PERIOD_NOT_ACTIVE: 활성 입력기간이 아닙니다.");
        }
        if ("EVALUATION_CONFIRMED".equals(status) || guard.countEvaluationConfirmations(owner, year) > 0) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 변경할 수 없습니다.");
        }
        if (!List.of("DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED").contains(status)) {
            throw new ConflictException("NOT_EDITABLE: 작성중 또는 반려 실적만 수정할 수 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public byte[] download(Map<String, Object> filters, CurrentUser user) {
        requireRole(user, "R01", "R02", "R04", "R07");
        function(user, "READ", "DRAFT", "R01", "R02", "R04", "R07");
        Map<String, Object> query = query(user);
        if (user.roles().contains("R07")) query.put("certification", true);
        query.putAll(filters);
        int size = ((Number) filters.getOrDefault("pageSize", 20)).intValue();
        int page = ((Number) filters.getOrDefault("page", 0)).intValue();
        if (!List.of(20, 50, 100).contains(size) || page < 0) invalid("pageSize", "페이지를 확인하세요.");
        query.put("pageSize", size);
        query.put("pageOffset", (long) page * size);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("관리번호", "관리항목코드", "업적발생일", "실적명", "상태"));
        for (Map<String, Object> row : mapper.list(query)) {
            rows.add(List.of(text(row, "managementNo"), text(row, "managementItemCode"),
                    text(row, "achievementDate"), text(row, "achievementName"), text(row, "achievementStatus")));
        }
        return EmploymentRateXlsxCodec.write(rows);
    }

    /** Unapproved bulk eligibility never produces a job or domain mutation. */
    public Map<String, Object> createJob(EmploymentRateBulkJobRequest request, CurrentUser user) {
        requireRole(user, "R07");
        if (request == null || request.evaluationYear() == null || !request.evaluationYear().matches("[0-9]{4}")) {
            invalid("evaluationYear", "평가연도를 입력하세요.");
        }
        if (!List.of("GENERATE", "DELETE").contains(request.actionType())) invalid("actionType", "작업을 선택하세요.");
        function(user, "EXECUTE", "DRAFT", "R07");
        throw new ConflictException("POLICY_NOT_APPROVED: 생성자격·삭제상태 정책 및 서버 확인 계약이 미승인입니다.");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> job(String id, CurrentUser user) {
        requireRole(user, "R07");
        function(user, "READ", "DRAFT", "R07");
        Map<String, Object> query = query(user);
        query.put("jobId", id);
        Map<String, Object> job = mapper.job(query);
        if (job == null) throw new NotFoundException("작업을 찾을 수 없습니다.");
        Map<String, Object> result = new HashMap<>(job);
        result.put("items", mapper.jobItems(id));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> managementItems(String year, CurrentUser user) {
        requireRole(user, "R01", "R02", "R04");
        Map<String, Object> query = query(user);
        query.put("evaluationYear", year);
        query.put("organizationCode", organization(user.userId()));
        return mapper.managementItems(query);
    }

    @Transactional(readOnly = true)
    public byte[] template(CurrentUser user) {
        requireRole(user, "R07");
        function(user, "READ", "DRAFT", "R07");
        Map<String, Object> template = activeTemplate();
        return EmploymentRateXlsxCodec.write(List.of(mapper.templateColumns(template.get("templateId").toString())));
    }

    /** Retains diagnostics in an independent transaction before returning a validation error to transport. */
    public Map<String, Object> upload(MultipartFile file, CurrentUser user, String requestId) {
        requireRole(user, "R07");
        function(user, "EXECUTE", "DRAFT", "R07");
        if (file == null || file.isEmpty() || file.getSize() > 10 * 1024 * 1024) invalid("file", "파일 크기를 확인하세요.");
        Map<String, Object> result = diagnostics.execute(transaction -> stage(file, user, requestId));
        if (((Number) result.get("errorCount")).intValue() > 0) {
            throw new BusinessValidationException("오류 행이 있어 0건 반영했습니다. 업로드 이력에서 오류를 확인하세요.",
                    List.of(new ValidationError("uploadId", result.get("uploadId").toString())));
        }
        return result;
    }

    private Map<String, Object> stage(MultipartFile file, CurrentUser user, String requestId) {
        Map<String, Object> template = activeTemplate();
        String id = UUID.randomUUID().toString();
        List<Map<String, Object>> payloads = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("파일을 읽을 수 없습니다.");
        }
        try {
            List<List<String>> workbook = EmploymentRateXlsxCodec.read(bytes);
            List<String> columns = mapper.templateColumns(template.get("templateId").toString());
            if (workbook.isEmpty() || !columns.equals(workbook.get(0))) {
                throw new IllegalArgumentException("현재 양식의 열 이름·순서와 일치하지 않습니다.");
            }
            Set<String> identities = new HashSet<>();
            for (int i = 1; i < workbook.size(); i++) {
                List<String> values = workbook.get(i);
                if (values.stream().allMatch(String::isBlank)) continue;
                Map<String, Object> payload = new HashMap<>();
                String failure = null;
                try {
                    Map<String, Object> target = mapper.employee(cell(values, 0));
                    if (target == null) throw new IllegalArgumentException("교번이 존재하지 않습니다.");
                    Long owner = ((Number) target.get("teacherUserId")).longValue();
                    String organization = organization(owner);
                    Map<String, Object> scope = query(user);
                    scope.put("organizationCode", organization);
                    if (!user.roles().contains("R09") && mapper.uploadScope(scope) == 0) throw new ForbiddenException();
                    String dateValue = cell(values, 2);
                    LocalDate date = dateValue.matches("[0-9]+(\\.[0-9]+)?")
                            ? LocalDate.of(1899, 12, 30).plusDays((long) Double.parseDouble(dateValue))
                            : LocalDate.parse(dateValue);
                    payload.put("teacherUserId", owner);
                    payload.put("organizationCode", organization);
                    payload.put("evaluationYear", String.valueOf(date.getYear()));
                    payload.put("managementItemCode", cell(values, 1));
                    payload.put("achievementDate", date.toString());
                    payload.put("achievementName", cell(values, 3));
                    List<String> attachments = cell(values, 4).isBlank() ? List.of() : List.of(cell(values, 4));
                    payload.put("attachmentIds", attachments);
                    validateItem(payload);
                    validateAttachments(attachments, owner);
                    guards(owner, date.getYear() + "", "DRAFT");
                    if (cell(values, 3).length() > 500) throw new IllegalArgumentException("실적명이 너무 깁니다.");
                    Map<String, Object> db = new HashMap<>(payload);
                    db.put("achievementDate", date);
                    if (!identities.add(serialize(List.of(owner, date, cell(values, 1), cell(values, 3))))
                            || mapper.duplicate(db) > 0) throw new IllegalArgumentException("중복 실적입니다.");
                } catch (RuntimeException exception) {
                    failure = exception instanceof ForbiddenException ? "대상 범위 권한이 없습니다."
                            : exception instanceof BusinessValidationException ? "관리항목 또는 첨부 입력을 확인하세요."
                            : exception.getMessage();
                }
                payload.put("rowNumber", i + 1);
                payload.put("inputValue", String.join(" | ", values));
                payloads.add(payload);
                errors.add(failure);
            }
            if (payloads.isEmpty()) throw new IllegalArgumentException("등록할 행이 없습니다.");
        } catch (RuntimeException exception) {
            payloads.clear();
            errors.clear();
            payloads.add(new HashMap<>(Map.of("rowNumber", 1, "inputValue", "")));
            errors.add(exception.getMessage());
        }
        int errorCount = (int) errors.stream().filter(value -> value != null).count();
        Map<String, Object> upload = new HashMap<>(template);
        upload.put("uploadId", id);
        upload.put("userId", user.userId());
        upload.put("originalFileName", safeName(file.getOriginalFilename()));
        upload.put("validationStatus", errorCount == 0 ? "VALIDATED" : "REJECTED");
        upload.put("totalCount", payloads.size());
        upload.put("errorCount", errorCount);
        upload.put("successCount", payloads.size() - errorCount);
        upload.put("savedCount", 0);
        try {
            StoredFile stored = files.save(user.userId(), safeName(file.getOriginalFilename()),
                    EmploymentRateXlsxCodec.CONTENT_TYPE, bytes);
            upload.put("fileToken", stored.fileId());
            if (errorCount > 0) {
                List<List<String>> errorRows = new ArrayList<>();
                errorRows.add(List.of("행", "오류"));
                for (int i = 0; i < errors.size(); i++) {
                    if (errors.get(i) != null) errorRows.add(List.of(payloads.get(i).get("rowNumber").toString(), errors.get(i)));
                }
                StoredFile errorFile = files.save(user.userId(), "errors.xlsx", EmploymentRateXlsxCodec.CONTENT_TYPE,
                        EmploymentRateXlsxCodec.write(errorRows));
                upload.put("errorFileId", errorFile.fileId());
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("업로드 파일 보존에 실패했습니다.");
        }
        mapper.insertUpload(upload);
        for (int i = 0; i < payloads.size(); i++) {
            Map<String, Object> row = new HashMap<>(payloads.get(i));
            if (upload.containsKey("errorFileId")) row.put("errorFileId", upload.get("errorFileId"));
            row.put("requestId", requestId);
            Map<String, Object> staging = new HashMap<>();
            staging.put("uploadId", id);
            staging.put("stagingId", UUID.randomUUID().toString());
            staging.put("rowNumber", row.get("rowNumber"));
            staging.put("payload", serialize(row));
            staging.put("validationStatus", errors.get(i) == null ? "NORMAL" : "ERROR");
            mapper.insertStaging(staging);
            if (errors.get(i) != null) {
                staging.put("errorId", UUID.randomUUID().toString());
                staging.put("inputValue", row.get("inputValue"));
                staging.put("errorReason", errors.get(i));
                mapper.insertError(staging);
            }
        }
        mapper.insertUploadHistory(upload);
        Map<String, Object> result = new HashMap<>(upload);
        result.remove("fileToken");
        result.put("rows", payloads);
        return result;
    }

    /** Revalidates every row before any insert; upload ownership, type and row lock prevent repeated commits. */
    @Transactional
    public Map<String, Object> commit(String id, CurrentUser user, String requestId) {
        requireRole(user, "R07");
        function(user, "EXECUTE", "DRAFT", "R07");
        Map<String, Object> query = query(user);
        query.put("uploadId", id);
        Map<String, Object> upload = mapper.lockUpload(query);
        if (upload == null) throw new NotFoundException("업로드를 찾을 수 없습니다.");
        if (!"VALIDATED".equals(upload.get("validationStatus"))) throw new ConflictException("반영 가능한 업로드가 아닙니다.");
        List<Map<String, Object>> rows = mapper.staging(id).stream().map(row -> parse(text(row, "payload"))).toList();
        if (rows.isEmpty()) throw new ConflictException("검증된 행이 없습니다.");
        for (Map<String, Object> row : rows) {
            Long owner = ((Number) row.get("teacherUserId")).longValue();
            Map<String, Object> scope = query(user);
            scope.put("organizationCode", row.get("organizationCode"));
            if (!user.roles().contains("R09") && mapper.uploadScope(scope) == 0) throw new ForbiddenException();
            guards(owner, text(row, "evaluationYear"), "DRAFT");
            validateItem(row);
            validateAttachments(attachmentList(row.get("attachmentIds")), owner);
            Map<String, Object> identity = new HashMap<>(row);
            identity.put("achievementDate", LocalDate.parse(text(row, "achievementDate")));
            if (mapper.duplicate(identity) > 0) throw new ConflictException("중복 실적으로 전체 반영이 취소되었습니다.");
        }
        for (Map<String, Object> row : rows) {
            EmploymentRateAchievementRequest request = new EmploymentRateAchievementRequest(
                    text(row, "managementItemCode"), LocalDate.parse(text(row, "achievementDate")),
                    text(row, "achievementName"), attachmentList(row.get("attachmentIds")));
            save(null, request, user, ((Number) row.get("teacherUserId")).longValue(),
                    text(row, "organizationCode"), requestId, true);
        }
        mapper.commitUpload(id);
        mapper.commitHistory(id);
        return Map.of("uploadId", id, "savedCount", rows.size(), "validationStatus", "COMMITTED");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> histories(CurrentUser user) {
        requireRole(user, "R07");
        return mapper.histories(query(user));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> errors(String id, CurrentUser user) {
        ownedUpload(id, user);
        return mapper.errors(id);
    }

    @Transactional(readOnly = true)
    public byte[] errorFile(String id, CurrentUser user) {
        ownedUpload(id, user);
        String file = mapper.errorFile(id);
        if (file == null) throw new NotFoundException("오류 파일이 없습니다.");
        try {
            return storage.read(file, user.userId());
        } catch (IOException exception) {
            throw new NotFoundException("오류 파일을 읽을 수 없습니다.");
        }
    }

    private void ownedUpload(String id, CurrentUser user) {
        requireRole(user, "R07");
        Map<String, Object> query = query(user);
        query.put("uploadId", id);
        if (mapper.upload(query) == null) throw new NotFoundException("업로드를 찾을 수 없습니다.");
    }

    private Map<String, Object> activeTemplate() {
        Map<String, Object> template = mapper.template();
        if (template == null) throw new ConflictException("활성 업무 양식이 없습니다.");
        return template;
    }

    private void validateItem(Map<String, Object> row) {
        if (mapper.managementItemCount(row) != 1) invalid("managementItemCode", "활성 관리항목이 없거나 모호합니다.");
    }

    private void validateAttachments(List<String> ids, Long owner) {
        for (String id : ids) {
            try {
                if (id == null || storage.find(id, owner) == null) invalid("attachmentIds", "본인 소유 파일을 확인하세요.");
            } catch (IOException | IllegalArgumentException exception) {
                invalid("attachmentIds", "본인 소유 파일을 확인하세요.");
            }
        }
    }

    private String organization(Long owner) {
        List<String> organizations = mapper.organizations(owner);
        if (organizations.size() != 1) invalid("organizationCode", "대상 소속을 유일하게 결정할 수 없습니다.");
        return organizations.get(0);
    }

    private void function(CurrentUser user, String action, String status, String... roles) {
        for (String role : user.roles()) {
            if (!role.equals("R09") && !List.of(roles).contains(role)) continue;
            try {
                permissions.evaluate(new FunctionPermissionEvaluateRequest(SCREEN, role, action, status, "FACULTY_ACHIEVEMENT"));
                return;
            } catch (ForbiddenException exception) {
                // A denied role must not hide another allowed role on the same principal.
            }
        }
        throw new ForbiddenException();
    }

    public static void requireRole(CurrentUser user, String... roles) {
        if (user == null) throw new UnauthenticatedException();
        if (user.roles() == null || user.roles().stream().noneMatch(
                role -> role.equals("R09") || List.of(roles).contains(role))) throw new ForbiddenException();
    }

    private Map<String, Object> query(CurrentUser user) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.userId());
        result.put("admin", user.roles().contains("R09"));
        result.put("self", user.roles().contains("R01"));
        result.put("department", user.roles().contains("R02"));
        result.put("certification", user.roles().contains("R04"));
        return result;
    }

    private Map<String, Object> response(Map<String, Object> row) {
        Map<String, Object> result = new HashMap<>(row);
        result.put("attachmentIds", attachmentList(row.get("attachmentIds")));
        for (String key : List.of("achievementDate", "createdAt", "updatedAt")) {
            if (result.get(key) != null) result.put(key, result.get(key).toString());
        }
        return result;
    }

    private List<String> attachmentList(Object value) {
        if (value == null) return List.of();
        if (value instanceof List<?> list) return list.stream().map(Object::toString).toList();
        try {
            return json.readValue(value.toString(), new TypeReference<List<String>>() { });
        } catch (IOException exception) {
            throw new IllegalStateException("첨부 메타데이터가 올바르지 않습니다.", exception);
        }
    }

    private Map<String, Object> parse(String value) {
        try {
            return json.readValue(value, new TypeReference<Map<String, Object>>() { });
        } catch (IOException exception) {
            throw new IllegalStateException("검증된 행을 읽을 수 없습니다.", exception);
        }
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalStateException("실적 직렬화에 실패했습니다.", exception);
        }
    }

    private static String text(Map<String, Object> row, String key) {
        return row.get(key) == null ? "" : row.get(key).toString();
    }

    private static String cell(List<String> row, int column) {
        return column < row.size() ? row.get(column).trim() : "";
    }

    private static String safeName(String name) {
        if (name == null) return "upload.xlsx";
        String safe = name.replace('\\', '/');
        return safe.substring(safe.lastIndexOf('/') + 1);
    }

    private static void invalid(String field, String message) {
        throw new BusinessValidationException("입력값을 확인하세요.", List.of(new ValidationError(field, message)));
    }
}

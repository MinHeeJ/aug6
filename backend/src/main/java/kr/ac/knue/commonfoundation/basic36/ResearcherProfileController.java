package kr.ac.knue.commonfoundation.basic36;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResearcherProfileController {
    private static final List<String> READONLY_FIELDS = List.of("employeeNo", "employee_no", "organizationCode", "organization_code", "appointmentId", "appointment_id", "name", "rankName", "rank_name");
    private final ResearcherProfileService service;
    private final ObjectMapper objectMapper;

    public ResearcherProfileController(ResearcherProfileService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/researcher-profiles")
    public ApiResponse<ResearcherProfileSearchResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String employeeNo,
            @RequestParam(required = false) String name, @RequestParam(required = false) String organizationCode,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        CurrentUser user = requireAnyRole(request, "R01", "R04", "R09");
        validateSize(size);
        return ApiResponse.ok(service.list(new ResearcherProfileSearchCriteria(page, size, employeeNo, name, organizationCode, user.employeeNo(), false), user), effectiveRequestId(requestId));
    }

    @GetMapping("/api/researcher-profiles/{employeeNo}")
    public ApiResponse<ResearcherProfileDetail> get(@PathVariable String employeeNo, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        CurrentUser user = requireAnyRole(request, "R01", "R04", "R09");
        return ApiResponse.ok(service.get(employeeNo, user), effectiveRequestId(requestId));
    }

    @PutMapping("/api/researcher-profiles/{employeeNo}/research-fields")
    public ApiResponse<ResearcherProfileSaveResponse> saveResearchFields(@PathVariable String employeeNo, @RequestBody Map<String, Object> body, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        validateReadonlyPayload(body);
        return ApiResponse.ok(service.saveResearchFields(employeeNo, toRequest(body), requireAnyRole(request, "R01", "R04", "R09")), effectiveRequestId(requestId));
    }

    @PutMapping("/api/researcher-profiles/{employeeNo}/careers")
    public ApiResponse<ResearcherProfileSaveResponse> saveCareers(@PathVariable String employeeNo, @RequestBody Map<String, Object> body, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        validateReadonlyPayload(body);
        return ApiResponse.ok(service.saveCareers(employeeNo, toRequest(body), requireAnyRole(request, "R01", "R04", "R09")), effectiveRequestId(requestId));
    }

    @PutMapping("/api/researcher-profiles/{employeeNo}/degrees")
    public ApiResponse<ResearcherProfileSaveResponse> saveDegrees(@PathVariable String employeeNo, @RequestBody Map<String, Object> body, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        validateReadonlyPayload(body);
        return ApiResponse.ok(service.saveDegrees(employeeNo, toRequest(body), requireAnyRole(request, "R01", "R04", "R09")), effectiveRequestId(requestId));
    }

    @PutMapping("/api/researcher-profiles/{employeeNo}/certifications")
    public ApiResponse<ResearcherProfileSaveResponse> saveCertifications(@PathVariable String employeeNo, @RequestBody Map<String, Object> body, @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        validateReadonlyPayload(body);
        return ApiResponse.ok(service.saveCertifications(employeeNo, toRequest(body), requireAnyRole(request, "R01", "R04", "R09")), effectiveRequestId(requestId));
    }

    @GetMapping("/api/admin/researcher-profiles/degree-prerequisite-missing")
    public ApiResponse<ResearcherProfileSearchResponse> degreeMissing(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String employeeNo, @RequestParam(required = false) String name, @RequestParam(required = false) String organizationCode,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        requireAnyRole(request, "R04", "R09");
        validateSize(size);
        return ApiResponse.ok(service.listDegreePrerequisiteMissing(new ResearcherProfileSearchCriteria(page, size, employeeNo, name, organizationCode, null, false)), effectiveRequestId(requestId));
    }

    private CurrentUser requireAnyRole(HttpServletRequest request, String... roles) {
        Object user = request.getAttribute("currentUser");
        if (user instanceof CurrentUser currentUser) {
            for (String role : roles) if (currentUser.roles().contains(role)) return currentUser;
            throw new ForbiddenException();
        }
        throw new UnauthenticatedException();
    }

    private ResearcherProfileTabSaveRequest toRequest(Map<String, Object> body) {
        return objectMapper.convertValue(body, ResearcherProfileTabSaveRequest.class);
    }

    private void validateSize(int size) {
        if (size != 20 && size != 50 && size != 100) throw new BusinessValidationException("목록 표시 건수가 올바르지 않습니다.", List.of(new ValidationError("size", "20, 50, 100건 중 하나를 선택하세요.")));
    }

    private void validateReadonlyPayload(Map<String, Object> payload) {
        List<ValidationError> errors = new ArrayList<>();
        collectReadonlyErrors(payload, "", errors);
        if (!errors.isEmpty()) throw new BusinessValidationException("KORUS 조회 전용 항목은 수정할 수 없습니다.", errors);
    }

    private void collectReadonlyErrors(Object value, String path, List<ValidationError> errors) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String nextPath = path.isBlank() ? key : path + "." + key;
                if (READONLY_FIELDS.contains(key)) errors.add(new ValidationError(nextPath, "KORUS 조회 전용 항목은 수정할 수 없습니다."));
                collectReadonlyErrors(entry.getValue(), nextPath, errors);
            }
        } else if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) collectReadonlyErrors(list.get(i), path + "[" + i + "]", errors);
        }
    }

    private String effectiveRequestId(String requestId) { return requestId != null && !requestId.trim().isBlank() ? requestId.trim() : UUID.randomUUID().toString(); }
}

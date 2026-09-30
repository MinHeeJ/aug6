package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Owns the FR-028 HTTP boundary for degree-completion lists, details, and saves. */
@RestController
public class DegreeCompletionAchievementController {
    private final DegreeCompletionAchievementService service;
    public DegreeCompletionAchievementController(DegreeCompletionAchievementService service){this.service=service;}
    /** Implements listDegreeCompletionAchievements. */
    @GetMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionDtos.SearchResponse> listDegreeCompletionAchievements(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(required=false)String evaluationYear,@RequestParam(required=false)String organizationCode,@RequestParam(required=false)String certificationStatus,@RequestParam(required=false)String managementItemCode,@RequestHeader(value="X-Request-Id",required=false)String requestId,HttpServletRequest request){validateSize(size);return ApiResponse.ok(service.list(new DegreeCompletionDtos.SearchCriteria(page,size,evaluationYear,organizationCode,certificationStatus,managementItemCode),readUser(request)),requestId(requestId));}
    /** Implements saveDegreeCompletionAchievement with complete guided-student data. */
    @PostMapping("/api/business/degree-completion-achievements")
    public ApiResponse<DegreeCompletionDtos.Row> saveDegreeCompletionAchievement(@Valid @RequestBody DegreeCompletionDtos.SaveRequest body,@RequestHeader(value="X-Request-Id",required=false)String requestId,HttpServletRequest request){return ApiResponse.ok(service.save(body,user(request)),requestId(requestId));}
    /** Returns the selected achievement and its detail sub-table. */
    @GetMapping("/api/business/degree-completion-achievements/{achievementId}")
    public ApiResponse<DegreeCompletionDtos.Row> detail(@PathVariable Long achievementId,@RequestHeader(value="X-Request-Id",required=false)String requestId,HttpServletRequest request){return ApiResponse.ok(service.find(achievementId,user(request)),requestId(requestId));}
    private CurrentUser user(HttpServletRequest r){Object v=r.getAttribute("currentUser");if(!(v instanceof CurrentUser u))throw new UnauthenticatedException();if(u.roles().stream().noneMatch(x->List.of("R01","R02","R04").contains(x)))throw new ForbiddenException();return u;}
    private CurrentUser readUser(HttpServletRequest r){Object v=r.getAttribute("currentUser");if(!(v instanceof CurrentUser u))throw new UnauthenticatedException();if(u.roles().stream().noneMatch(x->List.of("R01","R02","R04","R09").contains(x)))throw new ForbiddenException();return u;}
    private void validateSize(int s){if(s!=20&&s!=50&&s!=100)throw new BusinessValidationException("목록 표시 건수가 올바르지 않습니다.",List.of(new ValidationError("size","20, 50, 100건 중 하나를 선택하세요.")));}
    private String requestId(String v){return v!=null&&!v.isBlank()?v.trim():UUID.randomUUID().toString();}
}

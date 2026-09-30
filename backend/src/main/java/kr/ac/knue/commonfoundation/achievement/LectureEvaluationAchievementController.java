package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the R01/R02/R04 lecture-evaluation achievement list and save contract. */
@RestController
public class LectureEvaluationAchievementController {
 private final LectureEvaluationAchievementService service; public LectureEvaluationAchievementController(LectureEvaluationAchievementService service){this.service=service;}
 @GetMapping("/api/business/lecture-evaluation-achievements") public ApiResponse<LectureEvaluationAchievementResponse.Search> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int pageSize,@RequestParam(required=false) String managementNo,@RequestParam(required=false) String teacherName,@RequestParam(required=false) String managementItemCode,@RequestParam(required=false) LocalDate occurredDateFrom,@RequestParam(required=false) LocalDate occurredDateTo,@RequestParam(required=false) EducationAchievementStatus certificationStatus,HttpServletRequest request){ validatePage(page,pageSize); return ApiResponse.ok(service.list(new LectureEvaluationAchievementSearchCriteria(page,pageSize,managementNo,teacherName,managementItemCode,occurredDateFrom,occurredDateTo,certificationStatus),readUser(request))); }
 @PostMapping("/api/business/lecture-evaluation-achievements") public ApiResponse<LectureEvaluationAchievementResponse.Row> save(@Valid @RequestBody LectureEvaluationAchievementRequest body,HttpServletRequest request){return ApiResponse.ok(service.save(body,user(request),request.getHeader("X-Request-Id")),request.getHeader("X-Request-Id"));}
 private CurrentUser user(HttpServletRequest request){CurrentUser user=authenticatedUser(request);if(user.roles()==null||user.roles().stream().noneMatch(r->r.equals("R01")||r.equals("R02")||r.equals("R04")))throw new ForbiddenException();return user;}
 private CurrentUser readUser(HttpServletRequest request){CurrentUser user=authenticatedUser(request);if(user.roles()==null||user.roles().stream().noneMatch(r->r.equals("R01")||r.equals("R02")||r.equals("R04")||r.equals("R09")))throw new ForbiddenException();return user;}
 private CurrentUser authenticatedUser(HttpServletRequest request){Object value=request.getAttribute("currentUser");if(!(value instanceof CurrentUser user))throw new UnauthenticatedException();return user;}
 private void validatePage(int page,int pageSize){if(page<0||!(pageSize==20||pageSize==50||pageSize==100))throw new BusinessValidationException("검색조건이 올바르지 않습니다.",java.util.List.of(new ValidationError("pageSize","pageSize는 20, 50, 100 중 하나여야 합니다.")));}
}

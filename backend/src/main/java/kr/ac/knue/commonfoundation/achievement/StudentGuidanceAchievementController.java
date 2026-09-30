package kr.ac.knue.commonfoundation.achievement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
/** Owns individual student-guidance reads/writes and the R07-only Excel upload confirmation flow. */
@RestController
public class StudentGuidanceAchievementController {
 private final StudentGuidanceAchievementService service; private final kr.ac.knue.commonfoundation.excel.ExcelOperationsService excelOperationsService;
 public StudentGuidanceAchievementController(StudentGuidanceAchievementService service, kr.ac.knue.commonfoundation.excel.ExcelOperationsService excelOperationsService){this.service=service;this.excelOperationsService=excelOperationsService;}
 @GetMapping("/api/business/student-guidance-achievements") public ApiResponse<java.util.List<StudentGuidanceAchievementRow>> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int pageSize,HttpServletRequest request){return ApiResponse.ok(service.list(facultyOrAdministrator(request),page,pageSize));}
 @PostMapping("/api/business/student-guidance-achievements") public ApiResponse<StudentGuidanceAchievementRow> save(@Valid @RequestBody StudentGuidanceSaveRequest body,HttpServletRequest request){return ApiResponse.ok(service.save(body,faculty(request)));}
 /** Downloads the approved STUDENT_GUIDANCE header template after the same R07 authorization used for upload. */
 @GetMapping(value="/api/business/student-guidance-achievements/excel-template", produces="text/csv") public org.springframework.http.ResponseEntity<byte[]> template(HttpServletRequest request){CurrentUser user=r07OrAdministrator(request);kr.ac.knue.commonfoundation.excel.ExcelDownloadFile file=excelOperationsService.downloadUploadTemplate("B77-STUDENT-GUIDANCE-TEMPLATE",user.userId());return org.springframework.http.ResponseEntity.ok().header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,org.springframework.http.ContentDisposition.attachment().filename(file.originalFileName(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).contentType(org.springframework.http.MediaType.parseMediaType(file.contentType())).body(file.content());}
 @GetMapping("/api/business/student-guidance-achievements/excel-upload-histories") public ApiResponse<java.util.List<kr.ac.knue.commonfoundation.excel.ExcelUploadHistoryRow>> histories(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int pageSize,HttpServletRequest request){return ApiResponse.ok(service.histories(r07OrAdministrator(request),page,pageSize));}
 @PostMapping(value="/api/business/student-guidance-achievements/excel-uploads",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ApiResponse<StudentGuidanceUploadResult> upload(@RequestParam String templateId,@RequestPart("file") MultipartFile file,HttpServletRequest request){return ApiResponse.ok(service.upload(templateId,file,r07(request)));}
 @PostMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/commit") public ApiResponse<StudentGuidanceUploadResult> commit(@PathVariable String uploadId,HttpServletRequest request){return ApiResponse.ok(service.commit(uploadId,r07(request)));}
 private CurrentUser user(HttpServletRequest r){Object v=r.getAttribute("currentUser");if(!(v instanceof CurrentUser u))throw new UnauthenticatedException();return u;}
 /** Keeps R07 limited to the Excel workflow; individual achievement reads and writes require a faculty verification role. */
 private CurrentUser faculty(HttpServletRequest r){CurrentUser u=user(r);if(u.roles()==null||u.roles().stream().noneMatch(role->role.equals("R01")||role.equals("R02")||role.equals("R04")))throw new ForbiddenException();return u;}
 private CurrentUser facultyOrAdministrator(HttpServletRequest r){CurrentUser u=user(r);if(u.roles()==null||u.roles().stream().noneMatch(role->role.equals("R01")||role.equals("R02")||role.equals("R04")||role.equals("R09")))throw new ForbiddenException();return u;}
 private CurrentUser r07(HttpServletRequest r){CurrentUser u=user(r);if(u.roles()==null||!u.roles().contains("R07"))throw new ForbiddenException();return u;}
 private CurrentUser r07OrAdministrator(HttpServletRequest r){CurrentUser u=user(r);if(u.roles()==null||u.roles().stream().noneMatch(role->role.equals("R07")||role.equals("R09")))throw new ForbiddenException();return u;}
}

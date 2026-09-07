package kr.ac.knue.commonfoundation.schoolinfo;

import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SchoolInfoController {
    private final SchoolInfoService service;

    public SchoolInfoController(SchoolInfoService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/school-info")
    public ApiResponse<SchoolInfoSearchResponse> searchSchoolInfo(
            @RequestParam(required = false) String schoolName,
            @RequestParam(required = false) String educationOfficeCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ApiResponse.ok(service.search(new SchoolInfoQuery(schoolName, educationOfficeCode, page, size)));
    }
}

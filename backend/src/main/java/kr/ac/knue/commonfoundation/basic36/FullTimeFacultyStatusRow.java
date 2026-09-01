package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDate;

public record FullTimeFacultyStatusRow(String employeeNo, String name, String collegeCode, String collegeName,
                                       String departmentCode, String departmentName, String rankName,
                                       LocalDate retirementDate) {
}

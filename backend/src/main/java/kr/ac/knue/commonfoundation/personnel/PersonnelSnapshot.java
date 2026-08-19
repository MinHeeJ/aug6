package kr.ac.knue.commonfoundation.personnel;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PersonnelSnapshot(String employeeNo, String name, String organizationCode, String rankName,
                                String employmentStatus, String positionName, LocalDate retirementDate,
                                LocalDateTime lastSyncedAt) {
}

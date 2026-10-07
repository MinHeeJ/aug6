package kr.ac.knue.commonfoundation.common.education;

import java.time.LocalDate;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;

/** Shared boundary validation and explicit API integer-to-database semester conversion. */
public final class EducationAchievementValidation {
    private EducationAchievementValidation() {
    }

    /** Both optional endpoints must be supplied together and form an inclusive ordered range. */
    public static void validateSpecialLectureDates(LocalDate start, LocalDate end) {
        if ((start == null) != (end == null) || (start != null && start.isAfter(end))) {
            throw invalid("specialLectureEndDate", "특강 시작일과 종료일을 함께 입력하고 기간을 확인하세요.");
        }
    }

    /** The SQL column is varchar(1), never an integer implicitly coerced by the mapper. */
    public static String semesterCode(Integer semester) {
        if (semester == null || (semester != 1 && semester != 2)) {
            throw invalid("semester", "학기는 1 또는 2여야 합니다.");
        }
        return semester.toString();
    }

    /** Converts a stored semester to the approved integer response field. */
    public static Integer semester(String code) {
        if (!"1".equals(code) && !"2".equals(code)) {
            throw new IllegalArgumentException("Invalid stored semester code");
        }
        return Integer.valueOf(code);
    }

    private static BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }
}

package kr.ac.knue.commonfoundation.employmentrateimprovements;

/** DB-owned input metadata; no client-provided type or editable flag is trusted. */
public record ManagementItemOption(
        Long managementItemId, String code, String name, String evaluationYear,
        String requiredYn, String dataType, String teacherEditableYn) {
}

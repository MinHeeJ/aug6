package kr.ac.knue.commonfoundation.achievement;

import java.util.Map;

/** Builds degree-completion search SQL only with predicates that have supplied filter values. */
public final class DegreeCompletionAchievementSql {
    private DegreeCompletionAchievementSql() {
    }

    public static String list(Map<String, Object> parameters) {
        return select(parameters)
                + " ORDER BY achievement.occurred_date DESC, achievement.achievement_id DESC "
                + "LIMIT #{criteria.size} OFFSET #{criteria.page} * #{criteria.size}";
    }

    public static String count(Map<String, Object> parameters) {
        return "SELECT COUNT(*) " + fromAndWhere(parameters);
    }

    private static String select(Map<String, Object> parameters) {
        return "SELECT achievement.achievement_id AS \"achievementId\", "
                + "CONCAT('DC-', achievement.achievement_id) AS \"managementNo\", "
                + "achievement.teacher_user_id AS \"teacherUserId\", "
                + "COALESCE(personnel.name, users.login_id) AS \"teacherName\", "
                + "achievement.organization_code AS \"organizationCode\", "
                + "achievement.evaluation_year AS \"evaluationYear\", "
                + "achievement.management_item_code AS \"managementItemCode\", "
                + "achievement.occurred_date AS \"occurredDate\", "
                + "achievement.achievement_detail::text AS \"achievementDetail\", "
                + "achievement.certification_status AS \"certificationStatus\", "
                + "achievement.attachment_ref AS \"attachmentRef\", "
                + "achievement.updated_at AS \"updatedAt\" " + fromAndWhere(parameters);
    }

    private static String fromAndWhere(Map<String, Object> parameters) {
        DegreeCompletionAchievementModels.SearchCriteria criteria =
                (DegreeCompletionAchievementModels.SearchCriteria) parameters.get("criteria");
        StringBuilder sql = new StringBuilder("FROM degree_completion_achievements achievement ")
                .append("JOIN users ON users.user_id = achievement.teacher_user_id ")
                .append("LEFT JOIN korus_personnel_snapshots personnel ")
                .append("ON personnel.employee_no = users.employee_no ")
                .append("WHERE achievement.deleted_yn = 'N' ")
                .append("AND (achievement.teacher_user_id = #{userId} ")
                .append("OR (#{managerScope} = TRUE AND EXISTS ( ")
                .append("SELECT 1 FROM evaluation_organization_mappings mapping ")
                .append("WHERE mapping.user_id = #{userId} ")
                .append("AND mapping.organization_code = achievement.organization_code ")
                .append("AND mapping.business_type = 'FACULTY_ACHIEVEMENT' ")
                .append("AND mapping.data_scope IN ('DEPARTMENT', 'COLLEGE', 'BUSINESS', 'ALL')))) ");
        if (hasText(criteria.managementNo())) {
            sql.append("AND CAST(achievement.achievement_id AS varchar) ")
                    .append("ILIKE CONCAT('%', #{criteria.managementNo}, '%') ");
        }
        if (hasText(criteria.teacherName())) {
            sql.append("AND COALESCE(personnel.name, users.login_id) ILIKE CONCAT('%', #{criteria.teacherName}, '%') ");
        }
        if (hasText(criteria.certificationStatus())) {
            sql.append("AND achievement.certification_status = #{criteria.certificationStatus} ");
        }
        return sql.toString();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

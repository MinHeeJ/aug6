-- Phase 3 adds the evaluation-confirmed degree-completion scenario required to verify the immutable detail UI.
INSERT INTO education_achievements (
    faculty_user_id, achievement_type, management_item_code, occurrence_date, certification_status,
    created_by, updated_by, change_reason
)
SELECT faculty.user_id, 'DEGREE_COMPLETION', 'EDU-DEGREE-COMPLETION-04', DATE '2026-03-31',
       'EVALUATION_CONFIRMED', admin.user_id, admin.user_id, 'B73-PHASE3 석박사 평가확정 경계'
FROM users faculty
JOIN users admin ON admin.login_id = 'admin'
WHERE faculty.login_id = 'professor1'
  AND NOT EXISTS (
      SELECT 1
      FROM education_achievements existing
      WHERE existing.faculty_user_id = faculty.user_id
        AND existing.change_reason = 'B73-PHASE3 석박사 평가확정 경계'
  );

INSERT INTO degree_completion_student_details (
    achievement_id, degree_type, student_name, thesis_title, degree_awarded_date, created_by, updated_by
)
SELECT achievement.achievement_id, 'DOCTOR', '윤학생', '교육평가 개선 연구', DATE '2026-03-31',
       admin.user_id, admin.user_id
FROM education_achievements achievement
JOIN users admin ON admin.login_id = 'admin'
WHERE achievement.change_reason = 'B73-PHASE3 석박사 평가확정 경계'
  AND NOT EXISTS (
      SELECT 1
      FROM degree_completion_student_details detail
      WHERE detail.achievement_id = achievement.achievement_id
  );

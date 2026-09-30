INSERT INTO lecture_evaluation_achievements (
    achievement_id,
    evaluation_year,
    target_user_id,
    organization_code,
    management_item_code,
    occurred_date,
    achievement_detail,
    certification_status,
    attachment_ref,
    deleted_yn,
    created_by,
    updated_by
)
SELECT
    seed.achievement_id,
    '2026',
    professor.user_id,
    'KNUE-DEPT-COMP',
    seed.management_item_code,
    seed.occurred_date,
    seed.achievement_detail::jsonb,
    seed.certification_status,
    seed.attachment_ref,
    'N',
    admin.user_id,
    admin.user_id
FROM users admin
CROSS JOIN users professor
CROSS JOIN (
    VALUES
        (790001, 'B77-LE-001', DATE '2026-03-15', '{"fixtureId":"B77-LE-001","case":"NORMAL"}', 'DRAFTING', 'B77-FILE-LE-001'),
        (790002, 'B77-LE-002', DATE '2026-02-28', '{"fixtureId":"B77-LE-002","case":"DATE_BOUNDARY"}', 'DEPARTMENT_REJECTED', NULL),
        (790003, 'B77-LE-003', DATE '2026-01-31', '{"fixtureId":"B77-LE-003","case":"CONFIRMED_LOCK"}', 'EVALUATION_CONFIRMED', 'B77-FILE-LE-003')
) AS seed(
    achievement_id,
    management_item_code,
    occurred_date,
    achievement_detail,
    certification_status,
    attachment_ref
)
WHERE admin.login_id = 'admin'
  AND professor.login_id = 'professor1'
ON CONFLICT (achievement_id) DO UPDATE SET
    evaluation_year = EXCLUDED.evaluation_year,
    target_user_id = EXCLUDED.target_user_id,
    organization_code = EXCLUDED.organization_code,
    management_item_code = EXCLUDED.management_item_code,
    occurred_date = EXCLUDED.occurred_date,
    achievement_detail = EXCLUDED.achievement_detail,
    certification_status = EXCLUDED.certification_status,
    attachment_ref = EXCLUDED.attachment_ref,
    deleted_yn = EXCLUDED.deleted_yn,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

SELECT setval(
    pg_get_serial_sequence('lecture_evaluation_achievements', 'achievement_id'),
    COALESCE((SELECT MAX(achievement_id) FROM lecture_evaluation_achievements), 1),
    true
);

INSERT INTO education_achievement_status_histories (
    achievement_type,
    achievement_id,
    previous_status,
    next_status,
    action_type,
    reason_code,
    opinion,
    processed_by,
    processed_at
)
SELECT
    'LECTURE_EVALUATION',
    seed.achievement_id,
    seed.previous_status,
    seed.next_status,
    seed.action_type,
    seed.reason_code,
    seed.opinion,
    admin.user_id,
    TIMESTAMP '2026-03-01 09:00:00' + sequence_value.history_sequence * INTERVAL '1 hour'
FROM users admin
CROSS JOIN (
    VALUES
        (790001, 'DRAFTING', 'SUBMITTED', 'SUBMIT', NULL, 'B77-LE-001 제출'),
        (790002, 'SUBMITTED', 'DEPARTMENT_REJECTED', 'DEPARTMENT_REJECT', 'INCOMPLETE_EVIDENCE', 'B77-LE-002 보완 요청'),
        (790003, 'CERTIFIED', 'EVALUATION_CONFIRMED', 'CONFIRM_EVALUATION', NULL, 'B77-LE-003 평가확정')
) AS seed(
    achievement_id,
    previous_status,
    next_status,
    action_type,
    reason_code,
    opinion
)
CROSS JOIN LATERAL (
    SELECT CASE seed.achievement_id
        WHEN 790001 THEN 1
        WHEN 790002 THEN 2
        ELSE 3
    END AS history_sequence
) sequence_value
WHERE admin.login_id = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM education_achievement_status_histories existing
      WHERE existing.achievement_type = 'LECTURE_EVALUATION'
        AND existing.achievement_id = seed.achievement_id
        AND existing.action_type = seed.action_type
  );

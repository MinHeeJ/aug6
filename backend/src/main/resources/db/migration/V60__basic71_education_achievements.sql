CREATE TABLE IF NOT EXISTS education_achievements (
  achievement_id bigserial PRIMARY KEY,
  achievement_type varchar(50) NOT NULL,
  teacher_user_id bigint NOT NULL,
  evaluation_year varchar(10) NOT NULL,
  management_item_code varchar(100) NOT NULL,
  achievement_occurred_on date NOT NULL,
  achievement_detail_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  certification_status varchar(40) NOT NULL DEFAULT 'DRAFT',
  deleted_yn char(1) NOT NULL DEFAULT 'N',
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by bigint NOT NULL,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by bigint NOT NULL,
  deleted_at timestamp,
  deleted_by bigint,
  CONSTRAINT ck_education_achievement_type CHECK (achievement_type IN ('LECTURE_EVALUATION','LECTURE_PERFORMANCE','STUDENT_GUIDANCE','GRADUATE_DEGREE_COMPLETION')),
  CONSTRAINT ck_education_achievement_status CHECK (certification_status IN ('DRAFT','SUBMITTED','DEPARTMENT_CONFIRMED','DEPARTMENT_REJECTED','CERTIFIED','CERTIFICATION_REJECTED','EVALUATION_CONFIRMED')),
  CONSTRAINT ck_education_achievement_deleted CHECK (deleted_yn IN ('Y','N'))
);
COMMENT ON TABLE education_achievements IS '교원의 교육영역 실적 원본과 인증 상태를 보관한다.';
COMMENT ON COLUMN education_achievements.certification_status IS 'DRAFT:작성중|SUBMITTED:제출|DEPARTMENT_CONFIRMED:학과장확인|DEPARTMENT_REJECTED:학과장미승인|CERTIFIED:인증|CERTIFICATION_REJECTED:인증반려|EVALUATION_CONFIRMED:평가확정';
COMMENT ON COLUMN education_achievements.teacher_user_id IS 'user_accounts.user_id 참조 의도 (FK 미선언)';
CREATE INDEX IF NOT EXISTS idx_education_achievements_teacher_type ON education_achievements(teacher_user_id, achievement_type, achievement_occurred_on DESC) WHERE deleted_yn='N';

CREATE TABLE IF NOT EXISTS education_achievement_attachments (
  education_achievement_attachment_id bigserial PRIMARY KEY,
  achievement_id bigint NOT NULL REFERENCES education_achievements(achievement_id),
  file_token varchar(200) NOT NULL,
  sort_order integer NOT NULL DEFAULT 1,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (achievement_id, file_token)
);
COMMENT ON TABLE education_achievement_attachments IS '교육영역 실적의 권한 검증 가능한 파일 토큰 연결을 보관한다.';
CREATE INDEX IF NOT EXISTS idx_education_achievement_attachments_achievement ON education_achievement_attachments(achievement_id, sort_order);

CREATE TABLE IF NOT EXISTS graduate_degree_completion_students (
  degree_completion_student_id bigserial PRIMARY KEY,
  achievement_id bigint NOT NULL REFERENCES education_achievements(achievement_id),
  degree_type_code varchar(30) NOT NULL,
  student_name varchar(100) NOT NULL,
  thesis_title varchar(500) NOT NULL,
  degree_awarded_on date NOT NULL,
  sort_order integer NOT NULL DEFAULT 1
);
COMMENT ON TABLE graduate_degree_completion_students IS '석·박사 배출 실적에 속한 지도학생과 학위 수여 정보를 보관한다.';
CREATE INDEX IF NOT EXISTS idx_degree_completion_students_achievement ON graduate_degree_completion_students(achievement_id, sort_order);

CREATE TABLE IF NOT EXISTS education_achievement_status_histories (
  education_achievement_status_history_id bigserial PRIMARY KEY,
  achievement_id bigint NOT NULL REFERENCES education_achievements(achievement_id),
  previous_status varchar(40) NOT NULL,
  next_status varchar(40) NOT NULL,
  action_type varchar(50) NOT NULL,
  processed_by bigint NOT NULL,
  processed_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reason_code varchar(100),
  opinion varchar(1000),
  request_id varchar(100) NOT NULL
);
COMMENT ON TABLE education_achievement_status_histories IS '교육영역 실적의 상태전이 처리자·시점·사유를 불변 이력으로 기록한다.';
CREATE INDEX IF NOT EXISTS idx_education_achievement_status_history_achievement ON education_achievement_status_histories(achievement_id, processed_at DESC);

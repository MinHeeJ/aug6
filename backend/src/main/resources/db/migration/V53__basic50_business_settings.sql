CREATE TABLE IF NOT EXISTS appeal_business_settings (
    setting_id BIGSERIAL PRIMARY KEY,
    evaluation_year VARCHAR(4) NOT NULL,
    organization_code VARCHAR(64) NOT NULL,
    evaluation_unit_code VARCHAR(64) NOT NULL,
    effective_start_date DATE NOT NULL,
    effective_end_date DATE NOT NULL,
    manager_user_id BIGINT NOT NULL,
    manager_name VARCHAR(100) GENERATED ALWAYS AS ('USER-' || manager_user_id::text) STORED,
    target_scope VARCHAR(40) NOT NULL,
    active_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (active_yn IN ('Y','N')),
    change_reason VARCHAR(500) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_appeal_business_settings_scope UNIQUE (evaluation_year, organization_code, evaluation_unit_code, target_scope)
);
COMMENT ON TABLE appeal_business_settings IS '소속별 이의신청 기간 적용과 처리담당자·사용상태를 관리하는 업무 설정.';
COMMENT ON COLUMN appeal_business_settings.active_yn IS 'Y:사용|N:미사용';
COMMENT ON COLUMN appeal_business_settings.manager_user_id IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN appeal_business_settings.created_by IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN appeal_business_settings.updated_by IS 'users.user_id 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS result_view_business_settings (
    setting_id BIGSERIAL PRIMARY KEY,
    evaluation_year VARCHAR(4) NOT NULL,
    organization_code VARCHAR(64) NOT NULL,
    evaluation_unit_code VARCHAR(64) NOT NULL,
    effective_start_date DATE NOT NULL,
    effective_end_date DATE NOT NULL,
    manager_user_id BIGINT NOT NULL,
    manager_name VARCHAR(100) GENERATED ALWAYS AS ('USER-' || manager_user_id::text) STORED,
    target_scope VARCHAR(40) NOT NULL,
    active_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (active_yn IN ('Y','N')),
    change_reason VARCHAR(500) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_result_view_business_settings_scope UNIQUE (evaluation_year, organization_code, evaluation_unit_code, target_scope)
);
COMMENT ON TABLE result_view_business_settings IS '소속별 개인평가결과 조회기간 적용과 처리담당자·사용상태를 관리하는 업무 설정.';
COMMENT ON COLUMN result_view_business_settings.active_yn IS 'Y:사용|N:미사용';
COMMENT ON COLUMN result_view_business_settings.manager_user_id IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN result_view_business_settings.created_by IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN result_view_business_settings.updated_by IS 'users.user_id 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS college_evaluation_unit_authorities (
    authority_id BIGSERIAL PRIMARY KEY,
    evaluation_year VARCHAR(4) NOT NULL,
    organization_code VARCHAR(64) NOT NULL,
    evaluation_unit_code VARCHAR(64) NOT NULL,
    manager_user_id BIGINT NOT NULL,
    manager_name VARCHAR(100) GENERATED ALWAYS AS ('USER-' || manager_user_id::text) STORED,
    input_allowed_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (input_allowed_yn IN ('Y','N')),
    output_allowed_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (output_allowed_yn IN ('Y','N')),
    modify_allowed_yn CHAR(1) NOT NULL DEFAULT 'N' CHECK (modify_allowed_yn IN ('Y','N')),
    teacher_user_id BIGINT,
    teacher_name VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN teacher_user_id IS NULL THEN NULL ELSE 'USER-' || teacher_user_id::text END) STORED,
    effective_start_date DATE NOT NULL,
    effective_end_date DATE NOT NULL,
    active_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (active_yn IN ('Y','N')),
    change_reason VARCHAR(500) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_college_evaluation_unit_authorities_scope UNIQUE (evaluation_year, organization_code, evaluation_unit_code, manager_user_id, teacher_user_id)
);
COMMENT ON TABLE college_evaluation_unit_authorities IS '소속대학별 업무담당자와 평가단위별 입력·출력·수정 및 개별 교원 수정권한을 관리한다.';
COMMENT ON COLUMN college_evaluation_unit_authorities.active_yn IS 'Y:사용|N:미사용';
COMMENT ON COLUMN college_evaluation_unit_authorities.input_allowed_yn IS 'Y:허용|N:차단';
COMMENT ON COLUMN college_evaluation_unit_authorities.output_allowed_yn IS 'Y:허용|N:차단';
COMMENT ON COLUMN college_evaluation_unit_authorities.modify_allowed_yn IS 'Y:허용|N:차단';
COMMENT ON COLUMN college_evaluation_unit_authorities.manager_user_id IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN college_evaluation_unit_authorities.teacher_user_id IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN college_evaluation_unit_authorities.created_by IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN college_evaluation_unit_authorities.updated_by IS 'users.user_id 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS research_classification_criteria (
    criterion_id BIGSERIAL PRIMARY KEY,
    area_code VARCHAR(40) NOT NULL,
    area_name VARCHAR(100) NOT NULL,
    management_criterion_code VARCHAR(64) NOT NULL UNIQUE,
    management_criterion_name VARCHAR(200) NOT NULL,
    parent_criterion_code VARCHAR(64),
    active_yn CHAR(1) NOT NULL DEFAULT 'Y' CHECK (active_yn IN ('Y','N')),
    change_reason VARCHAR(500) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE research_classification_criteria IS '연구실적 영역과 관리기준 계층 및 사용상태를 관리하는 기준정보.';
COMMENT ON COLUMN research_classification_criteria.active_yn IS 'Y:사용|N:미사용';
COMMENT ON COLUMN research_classification_criteria.parent_criterion_code IS 'research_classification_criteria.management_criterion_code 참조 의도 (FK 미선언)';
COMMENT ON COLUMN research_classification_criteria.created_by IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN research_classification_criteria.updated_by IS 'users.user_id 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS research_achievement_classifications (
    achievement_id BIGSERIAL PRIMARY KEY,
    evaluation_year VARCHAR(4) NOT NULL,
    organization_code VARCHAR(64) NOT NULL,
    teacher_user_id BIGINT NOT NULL,
    teacher_name VARCHAR(100) NOT NULL,
    title VARCHAR(300) NOT NULL,
    area_code VARCHAR(40),
    management_criterion_code VARCHAR(64),
    classification_code VARCHAR(64),
    confirmation_status VARCHAR(20) NOT NULL DEFAULT 'UNCONFIRMED' CHECK (confirmation_status IN ('UNCONFIRMED','CONFIRMED')),
    achievement_date DATE NOT NULL,
    source_system VARCHAR(40) NOT NULL DEFAULT 'KORUS',
    confirmed_at TIMESTAMP,
    confirmed_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE research_achievement_classifications IS '외부 또는 입력 원천 연구실적의 분류 확인 상태만 관리하는 목록. 원천자료 자체는 생성·수정하지 않는다.';
COMMENT ON COLUMN research_achievement_classifications.confirmation_status IS 'UNCONFIRMED:미확인|CONFIRMED:확인완료';
COMMENT ON COLUMN research_achievement_classifications.teacher_user_id IS 'users.user_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN research_achievement_classifications.confirmed_by IS 'users.user_id 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS personal_achievement_score_views (
    score_id BIGSERIAL PRIMARY KEY,
    teacher_user_id BIGINT NOT NULL,
    teacher_name VARCHAR(100) NOT NULL,
    evaluation_year VARCHAR(4) NOT NULL,
    area_code VARCHAR(40) NOT NULL,
    area_name VARCHAR(100) NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    score NUMERIC(10,2) NOT NULL,
    calculation_detail VARCHAR(1000) NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(200) NOT NULL,
    evidence_url VARCHAR(300) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE personal_achievement_score_views IS '교원 개인 업적점수와 영역·항목별 산출내역 및 세부규정 조회용 읽기 모델.';
COMMENT ON COLUMN personal_achievement_score_views.teacher_user_id IS 'users.user_id 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS evaluation_result_locks (
    lock_id BIGSERIAL PRIMARY KEY,
    evaluation_year VARCHAR(4) NOT NULL,
    organization_code VARCHAR(64) NOT NULL,
    evaluation_unit_code VARCHAR(64) NOT NULL,
    confirmation_status VARCHAR(20) NOT NULL CHECK (confirmation_status IN ('DRAFT','CONFIRMED')),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE evaluation_result_locks IS '평가확정 결과에 영향을 주는 설정 변경 차단 판정용 업무 잠금 정보.';
COMMENT ON COLUMN evaluation_result_locks.confirmation_status IS 'DRAFT:작성중|CONFIRMED:평가확정';

CREATE INDEX IF NOT EXISTS idx_appeal_business_settings_search ON appeal_business_settings (evaluation_year, organization_code, evaluation_unit_code);
CREATE INDEX IF NOT EXISTS idx_result_view_business_settings_search ON result_view_business_settings (evaluation_year, organization_code, evaluation_unit_code);
CREATE INDEX IF NOT EXISTS idx_college_evaluation_unit_authorities_search ON college_evaluation_unit_authorities (evaluation_year, organization_code, evaluation_unit_code);
CREATE INDEX IF NOT EXISTS idx_research_classification_criteria_area ON research_classification_criteria (area_code, management_criterion_code);
CREATE INDEX IF NOT EXISTS idx_research_achievement_classifications_search ON research_achievement_classifications (evaluation_year, organization_code, confirmation_status);
CREATE INDEX IF NOT EXISTS idx_personal_achievement_score_views_search ON personal_achievement_score_views (teacher_user_id, evaluation_year, area_code);
CREATE INDEX IF NOT EXISTS idx_evaluation_result_locks_scope ON evaluation_result_locks (evaluation_year, organization_code, evaluation_unit_code, confirmation_status);

INSERT INTO appeal_business_settings (evaluation_year, organization_code, evaluation_unit_code, effective_start_date, effective_end_date, manager_user_id, target_scope, active_yn, change_reason, created_by, updated_by) VALUES
('2027','COL-EDU','UNIT-EDU','2027-06-01','2027-06-15',4,'COLLEGE','Y','FR-015 정상 설정',9,9),
('2027','COL-SCI','UNIT-RES','2027-06-10','2027-06-20',5,'EVALUATION_UNIT','Y','FR-015 경계 설정',9,9),
('2025','COL-LOCK','UNIT-OLD','2025-06-01','2025-06-10',6,'COLLEGE','N','FR-015 확정연도 예시',9,9)
ON CONFLICT (evaluation_year, organization_code, evaluation_unit_code, target_scope) DO NOTHING;
INSERT INTO result_view_business_settings (evaluation_year, organization_code, evaluation_unit_code, effective_start_date, effective_end_date, manager_user_id, target_scope, active_yn, change_reason, created_by, updated_by) VALUES
('2027','COL-EDU','UNIT-EDU','2027-07-01','2027-07-31',4,'SELF','Y','FR-016 정상 설정',9,9),
('2027','COL-SCI','UNIT-RES','2027-07-05','2027-07-20',5,'COLLEGE','Y','FR-016 경계 설정',9,9),
('2025','COL-LOCK','UNIT-OLD','2025-07-01','2025-07-10',6,'SELF','N','FR-016 확정연도 예시',9,9)
ON CONFLICT (evaluation_year, organization_code, evaluation_unit_code, target_scope) DO NOTHING;
INSERT INTO college_evaluation_unit_authorities (evaluation_year, organization_code, evaluation_unit_code, manager_user_id, input_allowed_yn, output_allowed_yn, modify_allowed_yn, teacher_user_id, effective_start_date, effective_end_date, active_yn, change_reason, created_by, updated_by) VALUES
('2027','COL-EDU','UNIT-EDU',4,'Y','Y','N',NULL,'2027-03-01','2028-02-29','Y','FR-021 담당자 지정',9,9),
('2027','COL-SCI','UNIT-RES',5,'N','Y','Y',2,'2027-03-01','2028-02-29','Y','FR-021 개별 교원 수정권한',9,9),
('2025','COL-LOCK','UNIT-OLD',6,'N','Y','N',NULL,'2025-03-01','2026-02-28','N','FR-021 확정연도 예시',9,9)
ON CONFLICT (evaluation_year, organization_code, evaluation_unit_code, manager_user_id, teacher_user_id) DO NOTHING;
INSERT INTO research_classification_criteria (area_code, area_name, management_criterion_code, management_criterion_name, parent_criterion_code, active_yn, change_reason, created_by, updated_by) VALUES
('RESEARCH','연구','JOURNAL','학술지 논문',NULL,'Y','FR-002 정상 기준',9,9),
('RESEARCH','연구','CONFERENCE','학술대회 발표',NULL,'Y','FR-002 경계 기준',9,9),
('RESEARCH','연구','LEGACY','사용중지 기준',NULL,'N','FR-002 상태 차이 기준',9,9)
ON CONFLICT (management_criterion_code) DO NOTHING;
INSERT INTO research_achievement_classifications (evaluation_year, organization_code, teacher_user_id, teacher_name, title, area_code, management_criterion_code, classification_code, confirmation_status, achievement_date, source_system, confirmed_by, confirmed_at) VALUES
('2027','COL-EDU',1,'교원1','AI 교육 연구','RESEARCH',NULL,NULL,'UNCONFIRMED','2027-04-01','KORUS',NULL,NULL),
('2027','COL-SCI',2,'교원2','분류 경계 연구','RESEARCH',NULL,NULL,'UNCONFIRMED','2027-04-02','KORUS',NULL,NULL),
('2027','COL-EDU',3,'교원3','확인완료 연구','RESEARCH','JOURNAL','JOURNAL','CONFIRMED','2027-04-03','KORUS',9,CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
INSERT INTO personal_achievement_score_views (teacher_user_id, teacher_name, evaluation_year, area_code, area_name, item_code, item_name, score, calculation_detail, rule_code, rule_name, evidence_url) VALUES
(1,'교원1','2027','EDU','교육','EDU-LECTURE','강의실적',25.00,'강의시간 10시간 × 2.5','RULE-EDU-01','강의실적 세부규정','/admin/score-calculation-histories?scoreId=1'),
(1,'교원1','2027','RESEARCH','연구','RES-JOURNAL','논문실적',40.00,'KCI 논문 2편 × 20','RULE-RES-01','논문실적 세부규정','/admin/score-calculation-histories?scoreId=2'),
(2,'교원2','2027','SERVICE','봉사','SVC-COMMITTEE','위원회 활동',15.00,'위원회 3건 × 5','RULE-SVC-01','봉사실적 세부규정','/admin/score-calculation-histories?scoreId=3')
ON CONFLICT DO NOTHING;
INSERT INTO evaluation_result_locks (evaluation_year, organization_code, evaluation_unit_code, confirmation_status) VALUES
('2025','COL-LOCK','UNIT-OLD','CONFIRMED'),
('2025','COL-EDU','UNIT-EDU','CONFIRMED'),
('2026','COL-SCI','UNIT-RES','DRAFT')
ON CONFLICT DO NOTHING;

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by) VALUES
(550, NULL, 'SCREEN', '소속대학·평가단위 권한 관리', 550, 'SCR-COLLEGE-EVALUATION-UNIT-AUTHORITY', '/admin/college-evaluation-unit-authorities', 'shield-check', 'BUSINESS', '소속대학별 담당자와 평가단위 행위권한 관리', 'Y', 'ACTIVE', 1),
(551, NULL, 'SCREEN', '이의신청 기간·처리권한 관리', 551, 'SCR-APPEAL-BUSINESS-SETTING', '/admin/appeal-business-settings', 'calendar-check', 'BUSINESS', '소속별 이의신청 운영 설정 관리', 'Y', 'ACTIVE', 1),
(552, NULL, 'SCREEN', '개인평가결과 조회기간·처리권한 관리', 552, 'SCR-RESULT-VIEW-BUSINESS-SETTING', '/admin/result-view-business-settings', 'calendar-range', 'BUSINESS', '개인평가결과 공개 운영 설정 관리', 'Y', 'ACTIVE', 1),
(553, NULL, 'SCREEN', '개인 업적점수·세부규정 조회', 553, 'SCR-PERSONAL-ACHIEVEMENT-SCORE', '/achievement/personal-scores', 'chart-no-axes-combined', 'BUSINESS', '교원 개인 업적점수와 산출근거 조회', 'Y', 'ACTIVE', 1),
(554, NULL, 'SCREEN', '연구실적 분류기준 설정', 554, 'SCR-RESEARCH-CLASSIFICATION-CRITERION', '/admin/research-classification-criteria', 'list-tree', 'BUSINESS', '연구실적 분류 기준정보 관리', 'Y', 'ACTIVE', 1),
(555, NULL, 'SCREEN', '미확인 연구실적 목록', 555, 'SCR-UNCONFIRMED-RESEARCH-ACHIEVEMENT', '/admin/unconfirmed-research-achievements', 'search-check', 'BUSINESS', '미확인 연구실적 검색 및 확인상태 전환', 'Y', 'ACTIVE', 1)
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name, screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', role_seed.role_code, m.menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-50 업무 메뉴 권한'
FROM menus m
JOIN (VALUES ('R03'), ('R04'), ('R09')) AS role_seed(role_code) ON m.screen_id IN ('SCR-COLLEGE-EVALUATION-UNIT-AUTHORITY','SCR-APPEAL-BUSINESS-SETTING','SCR-RESULT-VIEW-BUSINESS-SETTING')
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', role_seed.role_code, m.menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-50 조회 업무 메뉴 권한'
FROM menus m
JOIN (VALUES ('R01'), ('R04'), ('R09')) AS role_seed(role_code) ON m.screen_id = 'SCR-PERSONAL-ACHIEVEMENT-SCORE'
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', role_seed.role_code, m.menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-50 연구실적 업무 메뉴 권한'
FROM menus m
JOIN (VALUES ('R04'), ('R09')) AS role_seed(role_code) ON m.screen_id IN ('SCR-RESEARCH-CLASSIFICATION-CRITERION','SCR-UNCONFIRMED-RESEARCH-ACHIEVEMENT')
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;

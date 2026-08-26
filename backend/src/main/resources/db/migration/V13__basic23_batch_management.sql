CREATE TABLE IF NOT EXISTS batch_definitions (
    batch_id varchar(100) PRIMARY KEY,
    batch_type varchar(50) NOT NULL,
    schedule_cycle varchar(100) NOT NULL,
    max_execution_seconds integer,
    owner_user_id bigint NOT NULL,
    request_id varchar(100),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    CONSTRAINT fk_batch_definitions_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_batch_definitions_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_batch_definitions_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);
COMMENT ON TABLE batch_definitions IS '평가자료 생성·연계·점수산출 작업의 배치ID, 업무유형, 실행주기, 최대실행시간과 담당자를 관리한다.';
COMMENT ON COLUMN batch_definitions.owner_user_id IS 'users.user_id 참조 의도 (담당자)';
COMMENT ON COLUMN batch_definitions.request_id IS 'BatchDefinitionService.saveBatchDefinition 처리 시 요청 식별자를 애플리케이션에서 갱신';

CREATE TABLE IF NOT EXISTS batch_dependencies (
    predecessor_batch_id varchar(100) NOT NULL,
    successor_batch_id varchar(100) NOT NULL,
    request_id varchar(100),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    PRIMARY KEY (predecessor_batch_id, successor_batch_id),
    CONSTRAINT fk_batch_dependencies_predecessor FOREIGN KEY (predecessor_batch_id) REFERENCES batch_definitions(batch_id),
    CONSTRAINT fk_batch_dependencies_successor FOREIGN KEY (successor_batch_id) REFERENCES batch_definitions(batch_id),
    CONSTRAINT fk_batch_dependencies_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_batch_dependencies_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id),
    CONSTRAINT chk_batch_dependencies_not_self CHECK (predecessor_batch_id <> successor_batch_id)
);
COMMENT ON TABLE batch_dependencies IS '배치ID를 기준으로 선행·후행 배치 정의 관계를 관리한다.';
COMMENT ON COLUMN batch_dependencies.request_id IS 'BatchDefinitionService.saveBatchDefinition 처리 시 관계 변경 요청 식별자를 애플리케이션에서 갱신';

CREATE TABLE IF NOT EXISTS batch_parameters (
    batch_id varchar(100) PRIMARY KEY,
    parameter_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    request_id varchar(100),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    CONSTRAINT fk_batch_parameters_batch_id FOREIGN KEY (batch_id) REFERENCES batch_definitions(batch_id),
    CONSTRAINT fk_batch_parameters_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_batch_parameters_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);
COMMENT ON TABLE batch_parameters IS '배치 정의별 실행조건과 파라미터 JSON을 배치ID 단위로 보존한다.';
COMMENT ON COLUMN batch_parameters.parameter_json IS 'BatchDefinitionService.saveBatchDefinition 처리 시 해당 batch_id의 실행 파라미터로 갱신';
COMMENT ON COLUMN batch_parameters.request_id IS 'BatchDefinitionService.saveBatchDefinition 처리 시 파라미터 변경 요청 식별자를 애플리케이션에서 갱신';

CREATE TABLE IF NOT EXISTS batch_executions (
    execution_id varchar(100) PRIMARY KEY,
    batch_id varchar(100) NOT NULL,
    process_type varchar(20) NOT NULL CHECK (process_type IN ('MANUAL_RUN','STOP','RERUN')),
    reason varchar(500) NOT NULL,
    operator_user_id bigint NOT NULL,
    execution_status varchar(20) NOT NULL CHECK (execution_status IN ('WAITING','RUNNING','STOPPED','COMPLETED','FAILED')),
    original_execution_id varchar(100),
    request_id varchar(100),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_batch_executions_batch_id FOREIGN KEY (batch_id) REFERENCES batch_definitions(batch_id),
    CONSTRAINT fk_batch_executions_operator_user_id FOREIGN KEY (operator_user_id) REFERENCES users(user_id),
    CONSTRAINT fk_batch_executions_original_execution_id FOREIGN KEY (original_execution_id) REFERENCES batch_executions(execution_id)
);
COMMENT ON TABLE batch_executions IS '배치 수동실행·중지·재실행의 상태, 사유, 운영자와 원실행 연결을 기록한다.';
COMMENT ON COLUMN batch_executions.process_type IS 'MANUAL_RUN:수동실행|STOP:중지|RERUN:재실행';
COMMENT ON COLUMN batch_executions.execution_status IS 'WAITING:대기|RUNNING:실행중|STOPPED:중지|COMPLETED:완료|FAILED:실패';
COMMENT ON COLUMN batch_executions.original_execution_id IS 'batch_executions.execution_id 참조 의도 (재실행 원실행)';

CREATE TABLE IF NOT EXISTS batch_execution_results (
    execution_id varchar(100) PRIMARY KEY,
    started_at timestamp,
    ended_at timestamp,
    total_count integer,
    success_count integer,
    failure_count integer,
    excluded_count integer,
    elapsed_millis bigint,
    CONSTRAINT fk_batch_execution_results_execution_id FOREIGN KEY (execution_id) REFERENCES batch_executions(execution_id)
);
COMMENT ON TABLE batch_execution_results IS '배치 실행ID별 시작·종료시간, 처리·성공·실패·제외 건수와 소요시간 결과를 조회 전용으로 보존한다.';
COMMENT ON COLUMN batch_execution_results.elapsed_millis IS '배치 실행 완료 시 실행 런타임에서 산출해 저장하는 파생 소요시간';

CREATE TABLE IF NOT EXISTS batch_execution_logs (
    execution_id varchar(100) PRIMARY KEY,
    log_file_ref varchar(500) NOT NULL,
    CONSTRAINT fk_batch_execution_logs_execution_id FOREIGN KEY (execution_id) REFERENCES batch_executions(execution_id)
);
COMMENT ON TABLE batch_execution_logs IS '배치 실행ID에 연결된 로그파일 참조를 조회 전용으로 보존한다.';
COMMENT ON COLUMN batch_execution_logs.log_file_ref IS '실행 런타임에서 생성한 로그파일 참조이며 API는 수정·삭제하지 않는다';

CREATE TABLE IF NOT EXISTS batch_retry_targets (
    original_execution_id varchar(100) NOT NULL,
    failed_item_key varchar(200),
    failure_reason varchar(500),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_batch_retry_targets_original_item UNIQUE (original_execution_id, failed_item_key),
    CONSTRAINT fk_batch_retry_targets_original_execution FOREIGN KEY (original_execution_id) REFERENCES batch_executions(execution_id)
);
COMMENT ON TABLE batch_retry_targets IS '실패 배치 또는 실패 건 단위로 재처리 선택 후보를 관리한다.';
COMMENT ON COLUMN batch_retry_targets.failed_item_key IS '개별 실패 건 식별자이며 전체 실패 배치 대상이면 null일 수 있다';

CREATE TABLE IF NOT EXISTS batch_retry_results (
    retry_execution_id varchar(100) PRIMARY KEY,
    original_execution_id varchar(100) NOT NULL,
    failed_item_key varchar(200),
    retry_reason varchar(500) NOT NULL,
    request_id varchar(100),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    CONSTRAINT fk_batch_retry_results_retry_execution FOREIGN KEY (retry_execution_id) REFERENCES batch_executions(execution_id),
    CONSTRAINT fk_batch_retry_results_original_execution FOREIGN KEY (original_execution_id) REFERENCES batch_executions(execution_id),
    CONSTRAINT fk_batch_retry_results_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)
);
COMMENT ON TABLE batch_retry_results IS '원실행ID와 연결된 재처리 실행 결과를 원실행 결과와 별도 행으로 보존한다.';
COMMENT ON COLUMN batch_retry_results.failed_item_key IS 'batch_retry_targets.failed_item_key 참조 의도 (개별 실패 건 재처리 대상)';
COMMENT ON COLUMN batch_retry_results.retry_reason IS '재처리 사유 필수값';
COMMENT ON COLUMN batch_retry_results.request_id IS 'BatchRetryService.createBatchRetry 처리 시 요청 식별자를 애플리케이션에서 갱신';

CREATE INDEX IF NOT EXISTS idx_batch_definitions_search ON batch_definitions (batch_id, batch_type, schedule_cycle);
CREATE INDEX IF NOT EXISTS idx_batch_dependencies_predecessor ON batch_dependencies (predecessor_batch_id);
CREATE INDEX IF NOT EXISTS idx_batch_dependencies_successor ON batch_dependencies (successor_batch_id);
CREATE INDEX IF NOT EXISTS idx_batch_parameters_batch_id ON batch_parameters (batch_id);
CREATE INDEX IF NOT EXISTS idx_batch_executions_status ON batch_executions (execution_status, batch_id);
CREATE INDEX IF NOT EXISTS idx_batch_execution_results_started_at ON batch_execution_results (started_at);
CREATE INDEX IF NOT EXISTS idx_batch_execution_logs_execution_id ON batch_execution_logs (execution_id);
CREATE INDEX IF NOT EXISTS idx_batch_retry_targets_original_execution ON batch_retry_targets (original_execution_id);
CREATE INDEX IF NOT EXISTS idx_batch_retry_results_original_execution ON batch_retry_results (original_execution_id);

INSERT INTO batch_definitions (batch_id, batch_type, schedule_cycle, max_execution_seconds, owner_user_id, request_id, created_by, updated_by)
SELECT 'SEED-BATCH-DEF-001', 'EVALUATION_DATA', 'DAILY 02:00', 3600, u.user_id, 'seed-basic23', u.user_id, u.user_id
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (batch_id) DO NOTHING;

INSERT INTO batch_parameters (batch_id, parameter_json, request_id, created_by, updated_by)
SELECT 'SEED-BATCH-DEF-001', '{"year":2026}'::jsonb, 'seed-basic23', u.user_id, u.user_id
FROM users u
WHERE u.login_id = 'admin'
ON CONFLICT (batch_id) DO NOTHING;

INSERT INTO batch_executions (execution_id, batch_id, process_type, reason, operator_user_id, execution_status, original_execution_id, request_id)
SELECT execution_id, 'SEED-BATCH-DEF-001', process_type, reason, u.user_id, execution_status, original_execution_id, 'seed-basic23'
FROM users u
CROSS JOIN (VALUES
    ('SEED-BATCH-EXEC-001', 'MANUAL_RUN', '배치 실행 관리 중지 검증용 실행', 'RUNNING', NULL),
    ('SEED-BATCH-RESULT-SUCCESS-001', 'MANUAL_RUN', '배치 결과 조회 성공 seed', 'COMPLETED', NULL),
    ('SEED-BATCH-RESULT-FAILED-001', 'MANUAL_RUN', '배치 결과 조회 실패 seed', 'FAILED', NULL)
) seed(execution_id, process_type, reason, execution_status, original_execution_id)
WHERE u.login_id = 'admin'
ON CONFLICT (execution_id) DO NOTHING;

INSERT INTO batch_execution_results (execution_id, started_at, ended_at, total_count, success_count, failure_count, excluded_count, elapsed_millis) VALUES
('SEED-BATCH-RESULT-SUCCESS-001', '2026-08-26 02:00:00', '2026-08-26 02:05:30', 120, 118, 1, 1, 330000),
('SEED-BATCH-RESULT-FAILED-001', '2026-08-26 03:00:00', '2026-08-26 03:01:10', 40, 25, 15, 0, 70000)
ON CONFLICT (execution_id) DO NOTHING;

INSERT INTO batch_execution_logs (execution_id, log_file_ref) VALUES
('SEED-BATCH-RESULT-SUCCESS-001', 'logs/batch/SEED-BATCH-RESULT-SUCCESS-001.log'),
('SEED-BATCH-RESULT-FAILED-001', 'logs/batch/SEED-BATCH-RESULT-FAILED-001.log')
ON CONFLICT (execution_id) DO NOTHING;

INSERT INTO batch_retry_targets (original_execution_id, failed_item_key, failure_reason) VALUES
('SEED-BATCH-RESULT-FAILED-001', 'FAILED-ITEM-001', '검증용 실패 건')
ON CONFLICT (original_execution_id, failed_item_key) DO NOTHING;

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by) VALUES
(180,NULL,'MAIN','시스템 운영 관리',2,NULL,NULL,'activity','SYSTEM','배치작업 등 시스템 운영 관리 대메뉴','Y','ACTIVE',1),
(190,180,'MIDDLE','배치작업 관리',1,NULL,NULL,'calendar-clock','SYSTEM','배치 정의·실행·결과·재처리 관리','Y','ACTIVE',1),
(191,190,'SCREEN','배치 정의 관리',1,'SCR-BATCH-DEFINITION-MGMT','/admin/batch-definitions','settings-2','SYSTEM','배치 정의·실행주기·선후행·파라미터 관리','Y','ACTIVE',1),
(192,190,'SCREEN','배치 실행 관리',2,'SCR-BATCH-EXECUTION-MGMT','/admin/batch-executions','play-circle','SYSTEM','배치 수동실행·중지·재실행 관리','Y','ACTIVE',1),
(193,190,'SCREEN','배치 결과 조회',3,'SCR-BATCH-RESULT-MGMT','/admin/batch-results','list-checks','SYSTEM','배치 실행 결과와 로그 조회','Y','ACTIVE',1),
(194,190,'SCREEN','배치 오류 재처리',4,'SCR-BATCH-RETRY-MGMT','/admin/batch-retries','rotate-ccw','SYSTEM','실패 배치 재처리 관리','Y','ACTIVE',1)
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name, parent_menu_id = EXCLUDED.parent_menu_id, url = EXCLUDED.url, screen_id = EXCLUDED.screen_id, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by)
SELECT menu_id, screen_id, url, icon, business_category, description, 1 FROM menus WHERE menu_id BETWEEN 191 AND 194
ON CONFLICT (menu_id) DO UPDATE SET screen_id = EXCLUDED.screen_id, url = EXCLUDED.url, icon = EXCLUDED.icon, description = EXCLUDED.description, updated_at = CURRENT_TIMESTAMP;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', 'R09', menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-23 배치작업 관리 메뉴 접근'
FROM menus WHERE menu_id BETWEEN 191 AND 194
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed, status = EXCLUDED.status, updated_at = CURRENT_TIMESTAMP;

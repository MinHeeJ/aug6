CREATE TABLE IF NOT EXISTS basic48_seed_fixture_registry (
    seed_id varchar(20) PRIMARY KEY,
    target_table varchar(80) NOT NULL,
    fixture_purpose varchar(500) NOT NULL,
    minimum_case_count integer NOT NULL DEFAULT 3,
    lifecycle_status varchar(30) NOT NULL DEFAULT 'RESERVED',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_basic48_seed_fixture_registry_id CHECK (seed_id IN ('B48-SEED-001','B48-SEED-002','B48-SEED-003','B48-SEED-004')),
    CONSTRAINT ck_basic48_seed_fixture_registry_count CHECK (minimum_case_count >= 3),
    CONSTRAINT ck_basic48_seed_fixture_registry_status CHECK (lifecycle_status IN ('RESERVED','MATERIALIZED'))
);
COMMENT ON TABLE basic48_seed_fixture_registry IS 'BASIC-48 점수 이력·시점 데이터 조회 기능의 seed fixture 명명 규칙을 선행 등록하는 기반 테이블. 실제 조회 데이터는 후속 user-story migration에서 각 업무 테이블에 증분 추가한다.';
COMMENT ON COLUMN basic48_seed_fixture_registry.seed_id IS 'B48-SEED-001:시점데이터|B48-SEED-002:점수산출이력|B48-SEED-003:점수조정이력|B48-SEED-004:재계산이력';
COMMENT ON COLUMN basic48_seed_fixture_registry.target_table IS '후속 migration이 seed 데이터를 적재할 BASIC-48 조회 전용 테이블명';
COMMENT ON COLUMN basic48_seed_fixture_registry.fixture_purpose IS '정상, 경계, 권한·상태 차이 케이스를 포함해야 하는 fixture 목적';
COMMENT ON COLUMN basic48_seed_fixture_registry.minimum_case_count IS 'SED-01에 따라 각 seed fixture가 제공해야 하는 최소 검증 row 수';
COMMENT ON COLUMN basic48_seed_fixture_registry.lifecycle_status IS 'RESERVED:명명예약|MATERIALIZED:데이터적재완료';

CREATE INDEX IF NOT EXISTS idx_basic48_seed_fixture_registry_target
    ON basic48_seed_fixture_registry(target_table, lifecycle_status);

INSERT INTO basic48_seed_fixture_registry (
    seed_id,
    target_table,
    fixture_purpose,
    minimum_case_count,
    lifecycle_status
)
VALUES
    ('B48-SEED-001', 'biz_eval_snapshots', '확정 시점, 확정 취소 후 재확정, 다른 평가연도 snapshot 조회 검증', 3, 'RESERVED'),
    ('B48-SEED-002', 'biz_score_calc_hist', '단독 저자, 공동 저자 배분율, 상한 적용 산출근거 조회 검증', 3, 'RESERVED'),
    ('B48-SEED-003', 'biz_score_adj_hist', '상향 조정, 하향 조정, 평가백분율 조정 이력 조회 검증', 3, 'RESERVED'),
    ('B48-SEED-004', 'biz_recalc_hist', '산식버전 변경, 대상 범위, 변경 없음 재계산 이력 조회 검증', 3, 'RESERVED')
ON CONFLICT (seed_id) DO UPDATE SET
    target_table = EXCLUDED.target_table,
    fixture_purpose = EXCLUDED.fixture_purpose,
    minimum_case_count = EXCLUDED.minimum_case_count,
    lifecycle_status = EXCLUDED.lifecycle_status,
    updated_at = CURRENT_TIMESTAMP;

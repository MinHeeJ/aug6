-- BASIC-36 교수업적평가·학술지원금 파일럿 foundation migration.
-- Phase 1 intentionally creates only a readiness marker so later phases can add
-- API-specific business tables through additional incremental migrations without
-- modifying earlier Flyway files or existing common contracts.

CREATE TABLE IF NOT EXISTS basic36_foundation_readiness (
    readiness_key varchar(100) PRIMARY KEY,
    readiness_status varchar(20) NOT NULL,
    preserved_contracts text NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_basic36_foundation_readiness_status CHECK (readiness_status IN ('READY', 'PRECONDITION_FAILED'))
);

COMMENT ON TABLE basic36_foundation_readiness IS 'BASIC-36 교수업적평가·학술지원금 파일럿의 선행 공통계약 확인 상태를 보존하는 foundation marker.';
COMMENT ON COLUMN basic36_foundation_readiness.readiness_status IS 'READY:준비완료|PRECONDITION_FAILED:선행조건실패';
COMMENT ON COLUMN basic36_foundation_readiness.preserved_contracts IS '기존 인증·권한·메뉴·코드·감사·배치 계약 재사용 확인 항목';

INSERT INTO basic36_foundation_readiness (readiness_key, readiness_status, preserved_contracts)
VALUES (
    'BASIC-36-PHASE-1',
    'READY',
    'backend/frontend/infra 단일 저장소, 세션 인증, 메뉴 권한, 코드, 감사, 배치 공통계약 재사용'
)
ON CONFLICT (readiness_key) DO NOTHING;

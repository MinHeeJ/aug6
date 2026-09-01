CREATE TABLE IF NOT EXISTS faculty_search_results (
    faculty_id varchar(100) PRIMARY KEY,
    faculty_name varchar(200) NOT NULL,
    organization_code varchar(50),
    organization_name varchar(200),
    rank_name varchar(100),
    employment_status varchar(30),
    position_name varchar(100),
    appointment_id varchar(100),
    source_snapshot_employee_no varchar(50),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_faculty_search_results_snapshot FOREIGN KEY (source_snapshot_employee_no) REFERENCES korus_personnel_snapshots(employee_no),
    CONSTRAINT fk_faculty_search_results_organization FOREIGN KEY (organization_code) REFERENCES organizations(organization_code)
);
COMMENT ON TABLE faculty_search_results IS '연구자 프로필 관리의 교원 검색 목록을 위한 KORUS 교원 snapshot 기반 조회 projection. DTO alias와 화면 목록 응답의 기준 행을 보존한다.';
COMMENT ON COLUMN faculty_search_results.faculty_id IS 'korus_personnel_snapshots.employee_no 참조 의도 (projection PK)';
COMMENT ON COLUMN faculty_search_results.employment_status IS 'ACTIVE:재직|LEAVE:휴직|RETIRED:퇴직';
COMMENT ON COLUMN faculty_search_results.source_snapshot_employee_no IS 'korus_personnel_snapshots.employee_no 참조 의도';
COMMENT ON COLUMN faculty_search_results.updated_at IS 'KORUS snapshot seed 또는 동기화 projection refresh 시 애플리케이션/마이그레이션에서 갱신';

CREATE TABLE IF NOT EXISTS degree_deficiency_targets (
    target_id varchar(100) PRIMARY KEY,
    researcher_profile_id varchar(100) NOT NULL,
    faculty_id varchar(100) NOT NULL,
    faculty_name varchar(200) NOT NULL,
    organization_code varchar(50),
    organization_name varchar(200),
    final_degree_type varchar(20),
    deficiency_reason varchar(500) NOT NULL,
    source_degree_id bigint,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_degree_deficiency_targets_faculty FOREIGN KEY (faculty_id) REFERENCES faculty_search_results(faculty_id),
    CONSTRAINT fk_degree_deficiency_targets_organization FOREIGN KEY (organization_code) REFERENCES organizations(organization_code),
    CONSTRAINT fk_degree_deficiency_targets_source_degree FOREIGN KEY (source_degree_id) REFERENCES researcher_degrees(degree_id),
    CONSTRAINT ck_degree_deficiency_targets_final_degree CHECK (final_degree_type IS NULL OR final_degree_type IN ('BACHELOR','MASTER','DOCTOR'))
);
COMMENT ON TABLE degree_deficiency_targets IS '연구자 프로필 관리의 선행학위 미충족 대상 조회 projection. 박사 선행학위 검증 결과와 표시 사유를 목록 API 응답 형태로 보존한다.';
COMMENT ON COLUMN degree_deficiency_targets.researcher_profile_id IS 'researcher_profiles.researcher_registration_no 또는 employee_no 참조 의도 (FK 미선언)';
COMMENT ON COLUMN degree_deficiency_targets.final_degree_type IS 'BACHELOR:학사|MASTER:석사|DOCTOR:박사';
COMMENT ON COLUMN degree_deficiency_targets.source_degree_id IS 'researcher_degrees.degree_id 참조 의도';
COMMENT ON COLUMN degree_deficiency_targets.updated_at IS 'ResearcherProfileService.saveDegrees 또는 projection seed refresh 시 애플리케이션/마이그레이션에서 갱신';

CREATE INDEX IF NOT EXISTS idx_faculty_search_results_keyword ON faculty_search_results(faculty_id, faculty_name, organization_code);
CREATE INDEX IF NOT EXISTS idx_faculty_search_results_organization ON faculty_search_results(organization_code);
CREATE INDEX IF NOT EXISTS idx_degree_deficiency_targets_profile ON degree_deficiency_targets(researcher_profile_id);
CREATE INDEX IF NOT EXISTS idx_degree_deficiency_targets_faculty ON degree_deficiency_targets(faculty_id);
CREATE INDEX IF NOT EXISTS idx_degree_deficiency_targets_organization ON degree_deficiency_targets(organization_code);

INSERT INTO faculty_search_results (faculty_id, faculty_name, organization_code, organization_name, rank_name, employment_status, position_name, appointment_id, source_snapshot_employee_no, updated_at)
SELECT k.employee_no,
       k.name,
       k.organization_code,
       org.organization_name,
       k.rank_name,
       k.employment_status,
       k.position_name,
       k.appointment_id,
       k.employee_no,
       CURRENT_TIMESTAMP
FROM korus_personnel_snapshots k
LEFT JOIN organizations org ON org.organization_code = k.organization_code
WHERE k.status = 'ACTIVE'
ON CONFLICT (faculty_id) DO UPDATE SET
    faculty_name = EXCLUDED.faculty_name,
    organization_code = EXCLUDED.organization_code,
    organization_name = EXCLUDED.organization_name,
    rank_name = EXCLUDED.rank_name,
    employment_status = EXCLUDED.employment_status,
    position_name = EXCLUDED.position_name,
    appointment_id = EXCLUDED.appointment_id,
    source_snapshot_employee_no = EXCLUDED.source_snapshot_employee_no,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO degree_deficiency_targets (target_id, researcher_profile_id, faculty_id, faculty_name, organization_code, organization_name, final_degree_type, deficiency_reason, source_degree_id, updated_at)
SELECT COALESCE(d.degree_id::text, k.employee_no || '-DEGREE-DEFICIENCY'),
       COALESCE(p.researcher_registration_no, k.employee_no),
       k.employee_no,
       k.name,
       k.organization_code,
       org.organization_name,
       p.final_degree_type,
       '박사 최종학위의 학사·석사·박사 선행학위 입력 여부를 확인하세요.',
       d.degree_id,
       CURRENT_TIMESTAMP
FROM korus_personnel_snapshots k
JOIN faculty_search_results f ON f.faculty_id = k.employee_no
LEFT JOIN organizations org ON org.organization_code = k.organization_code
JOIN researcher_profiles p ON p.employee_no = k.employee_no
LEFT JOIN LATERAL (
    SELECT degree_id
    FROM researcher_degrees degree
    WHERE degree.employee_no = k.employee_no
      AND degree.degree_type = 'DOCTOR'
    ORDER BY degree.degree_id
    LIMIT 1
) d ON TRUE
WHERE k.status = 'ACTIVE'
  AND p.degree_prerequisite_missing_yn = 'Y'
ON CONFLICT (target_id) DO UPDATE SET
    researcher_profile_id = EXCLUDED.researcher_profile_id,
    faculty_id = EXCLUDED.faculty_id,
    faculty_name = EXCLUDED.faculty_name,
    organization_code = EXCLUDED.organization_code,
    organization_name = EXCLUDED.organization_name,
    final_degree_type = EXCLUDED.final_degree_type,
    deficiency_reason = EXCLUDED.deficiency_reason,
    source_degree_id = EXCLUDED.source_degree_id,
    updated_at = CURRENT_TIMESTAMP;

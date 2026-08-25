CREATE SEQUENCE IF NOT EXISTS notices_notice_id_seq;
CREATE SEQUENCE IF NOT EXISTS notice_targets_notice_target_id_seq;
CREATE SEQUENCE IF NOT EXISTS notice_attachments_attachment_id_seq;

CREATE TABLE IF NOT EXISTS notices (
    notice_id bigint PRIMARY KEY DEFAULT nextval('notices_notice_id_seq'),
    title varchar(200) NOT NULL,
    content text NOT NULL,
    publish_start_date date NOT NULL,
    publish_end_date date NOT NULL,
    important_yn varchar(1) NOT NULL CHECK (important_yn IN ('Y','N')),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    change_reason varchar(500),
    CONSTRAINT chk_notices_publish_period CHECK (publish_end_date >= publish_start_date),
    CONSTRAINT fk_notices_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_notices_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);
COMMENT ON TABLE notices IS '평가일정·점검·업무안내 공지의 제목, 본문, 게시기간, 중요여부와 등록·수정 메타정보를 관리한다.';
COMMENT ON COLUMN notices.important_yn IS 'Y:중요|N:일반';
COMMENT ON COLUMN notices.status IS 'ACTIVE:활성|INACTIVE:비활성|DELETED:삭제';
COMMENT ON COLUMN notices.content IS '공지 본문이며 업무 승인·확인 상태를 변경하지 않는다.';

CREATE TABLE IF NOT EXISTS notice_targets (
    notice_target_id bigint PRIMARY KEY DEFAULT nextval('notice_targets_notice_target_id_seq'),
    notice_id bigint NOT NULL REFERENCES notices(notice_id) ON DELETE CASCADE,
    target_type varchar(20) NOT NULL CHECK (target_type IN ('ROLE','ORGANIZATION')),
    target_id varchar(100) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    CONSTRAINT uq_notice_targets_notice_type_id UNIQUE (notice_id, target_type, target_id),
    CONSTRAINT fk_notice_targets_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)
);
COMMENT ON TABLE notice_targets IS '공지사항 노출 대상 역할과 조직 조건을 저장하며 역할·조직 기준정보 자체는 변경하지 않는다.';
COMMENT ON COLUMN notice_targets.target_type IS 'ROLE:역할|ORGANIZATION:조직';
COMMENT ON COLUMN notice_targets.target_id IS 'roles.role_code 또는 organizations.organization_code 참조 의도 (FK 미선언)';

CREATE TABLE IF NOT EXISTS notice_attachments (
    attachment_id bigint PRIMARY KEY DEFAULT nextval('notice_attachments_attachment_id_seq'),
    notice_id bigint NOT NULL REFERENCES notices(notice_id) ON DELETE CASCADE,
    original_file_name varchar(255) NOT NULL,
    stored_file_name varchar(255) NOT NULL,
    content_type varchar(100) NOT NULL DEFAULT 'application/octet-stream',
    file_size bigint NOT NULL DEFAULT 0,
    file_content bytea NOT NULL DEFAULT ''::bytea,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    CONSTRAINT fk_notice_attachments_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)
);
COMMENT ON TABLE notice_attachments IS '공지 첨부파일의 원본 파일명과 내부 저장 식별자 및 파일 내용을 보관하며 내부 경로와 실제 저장명은 외부에 노출하지 않는다.';
COMMENT ON COLUMN notice_attachments.original_file_name IS '사용자 다운로드에만 사용하는 원본 파일명으로 별도 보존한다.';
COMMENT ON COLUMN notice_attachments.stored_file_name IS '애플리케이션에서 생성한 내부 저장 식별자이며 API 응답에 노출하지 않는다.';
COMMENT ON COLUMN notice_attachments.file_content IS 'NoticeManagementService.create/save 시 애플리케이션에서 갱신';

CREATE INDEX IF NOT EXISTS idx_notices_publish_status ON notices (status, publish_start_date, publish_end_date);
CREATE INDEX IF NOT EXISTS idx_notice_targets_notice ON notice_targets (notice_id);
CREATE INDEX IF NOT EXISTS idx_notice_targets_type_id ON notice_targets (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_notice_attachments_notice ON notice_attachments (notice_id);

INSERT INTO notices (notice_id, title, content, publish_start_date, publish_end_date, important_yn, status, created_by, updated_by, change_reason)
VALUES
(1, '시스템 점검 안내', '공지 검증용 게시기간 내 공지입니다.', CURRENT_DATE - INTERVAL '1 day', CURRENT_DATE + INTERVAL '7 days', 'Y', 'ACTIVE', 1, 1, 'BASIC-22 공지 검증 seed'),
(2, '지난 공지 안내', '공지 검증용 게시기간 밖 공지입니다.', CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE - INTERVAL '5 days', 'N', 'ACTIVE', 1, 1, 'BASIC-22 공지 검증 seed')
ON CONFLICT (notice_id) DO UPDATE SET title = EXCLUDED.title,
    content = EXCLUDED.content,
    publish_start_date = EXCLUDED.publish_start_date,
    publish_end_date = EXCLUDED.publish_end_date,
    important_yn = EXCLUDED.important_yn,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO notice_targets (notice_id, target_type, target_id, created_by)
VALUES
(1, 'ROLE', 'R09', 1),
(1, 'ORGANIZATION', 'ORG001', 1),
(2, 'ROLE', 'R09', 1),
(2, 'ORGANIZATION', 'ORG001', 1)
ON CONFLICT (notice_id, target_type, target_id) DO NOTHING;

INSERT INTO notice_attachments (attachment_id, notice_id, original_file_name, stored_file_name, content_type, file_size, file_content, created_by)
VALUES (1, 1, '점검안내.txt', 'seed-notice-maintenance', 'text/plain', 35, convert_to('공지 검증용 첨부파일입니다.', 'UTF8'), 1)
ON CONFLICT (attachment_id) DO UPDATE SET original_file_name = EXCLUDED.original_file_name,
    content_type = EXCLUDED.content_type,
    file_size = EXCLUDED.file_size,
    file_content = EXCLUDED.file_content;

SELECT setval('notices_notice_id_seq', GREATEST((SELECT COALESCE(MAX(notice_id), 0) FROM notices), 1));
SELECT setval('notice_targets_notice_target_id_seq', GREATEST((SELECT COALESCE(MAX(notice_target_id), 0) FROM notice_targets), 1));
SELECT setval('notice_attachments_attachment_id_seq', GREATEST((SELECT COALESCE(MAX(attachment_id), 0) FROM notice_attachments), 1));

CREATE TABLE IF NOT EXISTS message_codes (
    message_code varchar(100) PRIMARY KEY,
    message_type varchar(50) NOT NULL CHECK (message_type IN ('SAVE','DELETE','APPROVAL','REJECT','ERROR','SESSION_EXPIRED')),
    user_message varchar(1000) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    change_reason varchar(500),
    CONSTRAINT fk_message_codes_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_message_codes_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);
COMMENT ON TABLE message_codes IS '저장·삭제·승인·반려·오류·세션만료 등 업무화면에서 사용할 메시지코드와 사용자 안내 문구를 관리한다.';
COMMENT ON COLUMN message_codes.message_type IS 'SAVE:저장|DELETE:삭제|APPROVAL:승인|REJECT:반려|ERROR:오류|SESSION_EXPIRED:세션만료';
COMMENT ON COLUMN message_codes.user_message IS '사용자 화면에 표시하는 안내 문구이며 시스템 로그 문구와 분리한다.';

CREATE INDEX IF NOT EXISTS idx_message_codes_type_code ON message_codes (message_type, message_code);

CREATE TABLE IF NOT EXISTS help_contents (
    help_content_id bigserial PRIMARY KEY,
    screen_id varchar(100) NOT NULL UNIQUE,
    business_description text NOT NULL,
    input_criteria text NOT NULL,
    faq text NOT NULL DEFAULT '',
    contact varchar(200) NOT NULL DEFAULT '',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    change_reason varchar(500),
    CONSTRAINT fk_help_contents_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    CONSTRAINT fk_help_contents_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);
COMMENT ON TABLE help_contents IS '화면ID별 업무 설명, 입력 기준, FAQ, 연락처를 관리하며 업무화면은 자기 screen_id의 도움말만 조회한다.';
COMMENT ON COLUMN help_contents.screen_id IS 'menus.screen_id 참조 의도 (FK 미선언)';
COMMENT ON COLUMN help_contents.business_description IS '도움말 업무 설명이며 실제 입력 검증규칙을 변경하지 않는다.';
COMMENT ON COLUMN help_contents.input_criteria IS '도움말 입력 기준이며 실제 입력 검증규칙을 변경하지 않는다.';
COMMENT ON COLUMN help_contents.faq IS '화면별 자주 묻는 질문이며 사용자 매뉴얼 파일과 분리한다.';

CREATE INDEX IF NOT EXISTS idx_help_contents_screen_id ON help_contents (screen_id);

INSERT INTO message_codes (message_code, message_type, user_message, created_by, updated_by, change_reason) VALUES
('SAVE.SUCCESS','SAVE','저장되었습니다.',1,1,'BASIC-22 메시지 유형 seed'),
('DELETE.SUCCESS','DELETE','삭제 처리되었습니다.',1,1,'BASIC-22 메시지 유형 seed'),
('APPROVAL.SUCCESS','APPROVAL','승인 처리되었습니다.',1,1,'BASIC-22 메시지 유형 seed'),
('REJECT.SUCCESS','REJECT','반려 처리되었습니다.',1,1,'BASIC-22 메시지 유형 seed'),
('ERROR.GENERAL','ERROR','오류가 발생했습니다. 입력값을 확인한 뒤 다시 시도하세요.',1,1,'BASIC-22 메시지 유형 seed'),
('SESSION.EXPIRED','SESSION_EXPIRED','세션이 만료되었습니다. 다시 로그인하세요.',1,1,'BASIC-22 메시지 유형 seed')
ON CONFLICT (message_code) DO UPDATE SET message_type = EXCLUDED.message_type,
    user_message = EXCLUDED.user_message,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO help_contents (screen_id, business_description, input_criteria, faq, contact, created_by, updated_by, change_reason) VALUES
('SCR-MESSAGE-MGMT','메시지코드와 사용자 문구를 관리합니다.','메시지코드, 메시지 유형, 사용자 문구, 변경 사유는 저장 전 확인합니다.','저장 후 업무화면은 메시지코드로 최신 문구를 조회합니다.','system-admin@knue.ac.kr',1,1,'BASIC-22 도움말 검증 seed')
ON CONFLICT (screen_id) DO UPDATE SET business_description = EXCLUDED.business_description,
    input_criteria = EXCLUDED.input_criteria,
    faq = EXCLUDED.faq,
    contact = EXCLUDED.contact,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO menus (menu_id, parent_menu_id, menu_type, menu_name, display_order, screen_id, url, icon, business_category, description, system_use_yn, status, updated_by, change_reason) VALUES
(150,100,'MIDDLE','시스템 환경설정',5,NULL,NULL,'settings-2','SYSTEM','공통 운영 환경 관리','Y','ACTIVE',1,'BASIC-22 메뉴 seed'),
(155,100,'MIDDLE','공지·도움말 관리',6,NULL,NULL,'help-circle','SYSTEM','공지사항·도움말·매뉴얼 관리','Y','ACTIVE',1,'BASIC-22 메뉴 seed'),
(151,150,'SCREEN','메시지 관리',1,'SCR-MESSAGE-MGMT','/admin/messages','message-square','SYSTEM','메시지코드와 사용자 문구 관리','Y','ACTIVE',1,'BASIC-22 메뉴 seed'),
(152,155,'SCREEN','공지사항 관리',1,'SCR-NOTICE-MGMT','/admin/notices','megaphone','SYSTEM','공지사항 관리','Y','ACTIVE',1,'BASIC-22 메뉴 seed'),
(153,155,'SCREEN','도움말 관리',2,'SCR-HELP-MGMT','/admin/help-contents','help-circle','SYSTEM','화면 도움말 관리','Y','ACTIVE',1,'BASIC-22 메뉴 seed'),
(154,155,'SCREEN','매뉴얼 관리',3,'SCR-MANUAL-MGMT','/admin/manuals','book-open','SYSTEM','매뉴얼 관리','Y','ACTIVE',1,'BASIC-22 메뉴 seed')
ON CONFLICT (menu_id) DO UPDATE SET menu_name = EXCLUDED.menu_name,
    parent_menu_id = EXCLUDED.parent_menu_id,
    url = EXCLUDED.url,
    screen_id = EXCLUDED.screen_id,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

INSERT INTO menu_execution_info (menu_id, screen_id, url, icon, business_category, description, updated_by)
SELECT menu_id, screen_id, url, icon, business_category, description, 1
FROM menus
WHERE screen_id IN ('SCR-MESSAGE-MGMT','SCR-NOTICE-MGMT','SCR-HELP-MGMT','SCR-MANUAL-MGMT')
ON CONFLICT (menu_id) DO UPDATE SET screen_id = EXCLUDED.screen_id,
    url = EXCLUDED.url,
    icon = EXCLUDED.icon,
    business_category = EXCLUDED.business_category,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by;

INSERT INTO menu_permissions (target_type, target_id, menu_id, access_allowed, status, created_by, updated_by, change_reason)
SELECT 'ROLE', 'R09', menu_id, 'ALLOW', 'ACTIVE', 1, 1, 'BASIC-22 시스템관리자 메뉴 접근'
FROM menus
WHERE screen_id IN ('SCR-MESSAGE-MGMT','SCR-NOTICE-MGMT','SCR-HELP-MGMT','SCR-MANUAL-MGMT')
ON CONFLICT (target_type, target_id, menu_id) DO UPDATE SET access_allowed = EXCLUDED.access_allowed,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = EXCLUDED.updated_by,
    change_reason = EXCLUDED.change_reason;

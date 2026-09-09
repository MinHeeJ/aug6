ALTER TABLE mail_delivery_attempts
    ADD COLUMN IF NOT EXISTS requester_ip varchar(64);

COMMENT ON COLUMN mail_delivery_attempts.requester_ip IS '재발송 요청 IP. 비회원 재발송 rate limit 산정에만 사용하며 SMTP 비밀정보를 포함하지 않는다.';

CREATE INDEX IF NOT EXISTS idx_mail_delivery_attempts_requester_ip_requested
    ON mail_delivery_attempts(requester_ip, requested_at DESC)
    WHERE requester_ip IS NOT NULL;

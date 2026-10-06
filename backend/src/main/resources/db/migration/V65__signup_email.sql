-- Legacy accounts remain valid without an email; signup requires a normalized email.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email varchar(254);

ALTER TABLE users
    ADD CONSTRAINT ck_users_email_lowercase
    CHECK (email IS NULL OR email = lower(email));

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_signup_email
    ON users (email);

COMMENT ON COLUMN users.email IS '회원가입 이메일. SignupService가 생성 시 소문자로 정규화하며 기존 미보유 계정은 NULL을 보존한다.';

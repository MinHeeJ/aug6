-- Extend the existing account store; legacy accounts have no invented email address.
-- Existing-account email remediation remains a separate deployment policy decision.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email varchar(254);

COMMENT ON COLUMN users.email IS '회원가입 이메일. 소문자로 정규화하며 기존 미등록 계정은 NULL을 유지한다.';

ALTER TABLE users
    ADD CONSTRAINT ck_users_email_lowercase
    CHECK (email = lower(email));

ALTER TABLE users
    ADD CONSTRAINT uq_users_email
    UNIQUE (email);

-- Distinct Argon2id hashes were derived with independent random salts and secrets.
-- No plaintext fixture credentials are persisted or published by this migration.
INSERT INTO users (
    login_id,
    password_hash,
    email,
    system_use_yn,
    status
)
VALUES
    (
        'testuser1',
        '$argon2id$v=19$m=19456,t=2,p=1$08Uw2fSIIwd5zM0FoPgG4A$3EEKaaNs/Hm3U/IoxGO97DogdOnQorVIKsgJb3phmQM',
        'testuser1@example.com',
        'Y',
        'ACTIVE'
    ),
    (
        'testuser2',
        '$argon2id$v=19$m=19456,t=2,p=1$ac/9AlW5W/fl9ZZIAyUCCg$0NGV/+zonq2+c1I1AlgZVVptDwpL2vuBAkRYGa43Ycw',
        'testuser2@example.com',
        'Y',
        'ACTIVE'
    ),
    (
        'testuser3',
        '$argon2id$v=19$m=19456,t=2,p=1$o62jUo/eevWLIl+QUAAxRA$HlSc/SYkZStVWVw+JPm91FTEMzOqGFTI4SD4IoJGhg0',
        'testuser3@example.com',
        'Y',
        'ACTIVE'
    );

-- Resolve generated bigint identities rather than treating public login IDs as keys.
-- Anonymous registration has no administrator/approver identity to fabricate.
INSERT INTO user_roles (
    user_id,
    role_code,
    assignment_type,
    status
)
SELECT
    u.user_id,
    'R01',
    'MANUAL',
    'ACTIVE'
FROM users u
WHERE u.login_id IN ('testuser1', 'testuser2', 'testuser3');

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email varchar(254);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email
    ON users (email)
    WHERE email IS NOT NULL;

COMMENT ON COLUMN users.email IS '회원가입 이메일. SignupService가 소문자로 정규화하여 저장하고 고유성을 보장한다.';

INSERT INTO users (
    login_id,
    password_hash,
    email,
    system_use_yn,
    status,
    change_reason
)
VALUES
    (
        'testuser1',
        '$argon2id$v=19$m=65536,t=3,p=1$c2lnbnVwLWZpeHR1cmUtc2FsdA$H2wWP4M5JhCEu0OsVKy77h06TC9XdlpMtPdbyWEGmTU',
        'testuser1@example.com',
        'Y',
        'ACTIVE',
        'BASIC-85 회원가입 중복 검증 fixture'
    ),
    (
        'testuser2',
        '$argon2id$v=19$m=65536,t=3,p=1$c2lnbnVwLWZpeHR1cmUtc2FsdA$H2wWP4M5JhCEu0OsVKy77h06TC9XdlpMtPdbyWEGmTV',
        'testuser2@example.com',
        'Y',
        'ACTIVE',
        'BASIC-85 이메일 중복 검증 fixture'
    ),
    (
        'testuser3',
        '$argon2id$v=19$m=65536,t=3,p=1$c2lnbnVwLWZpeHR1cmUtc2FsdA$H2wWP4M5JhCEu0OsVKy77h06TC9XdlpMtPdbyWEGmTQ',
        'testuser3@example.com',
        'Y',
        'ACTIVE',
        'BASIC-85 기본 역할 검증 fixture'
    )
ON CONFLICT (login_id) DO NOTHING;

INSERT INTO user_roles (
    user_id,
    role_code,
    assignment_type,
    status,
    change_reason
)
SELECT
    u.user_id,
    'R01',
    'MANUAL',
    'ACTIVE',
    'BASIC-85 회원가입 fixture 기본 역할'
FROM users u
WHERE u.login_id IN ('testuser1', 'testuser2', 'testuser3')
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.user_id
        AND ur.role_code = 'R01'
        AND ur.assignment_type = 'MANUAL'
        AND ur.status = 'ACTIVE'
  );

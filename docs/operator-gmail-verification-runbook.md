# BASIC-58 운영자 Gmail SMTP 이메일 인증 Runbook

이 문서는 회원가입 이메일 인증 메일을 운영자가 설정한 Gmail SMTP 계정으로 실제 수신자 메일함에 발송하고 검증하는 절차입니다. MailHog, 단위 테스트, 개발용 수신함 결과는 실제 Gmail 외부 발송 검증 증거로 사용할 수 없습니다.

## 1. Google 발송 계정 준비

1. 서비스 운영자가 관리하는 발송 전용 Google 계정을 준비합니다.
2. 계정에서 2단계 인증을 활성화합니다.
3. 보안 정책이 앱 비밀번호를 허용하는지 확인합니다.
4. 앱 비밀번호가 허용되면 발급받아 배포 비밀 저장소 또는 로컬 비밀 환경변수에만 저장합니다.
5. 앱 비밀번호가 허용되지 않는 계정이면 일반 로그인 비밀번호로 대체하지 말고, 정책상 Gmail SMTP 발송 계정 준비가 미완료임을 기록합니다.

## 2. 필요한 환경변수

아래 값은 source, Dockerfile, 이미지, 로그, 오류 응답에 원문 비밀을 남기지 않고 운영 환경 또는 Preview 환경의 secret으로 주입합니다.

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_FROM=
MAIL_FROM_VERIFIED_ALIAS=false
MAIL_VERIFICATION_BASE_URL=
MAIL_CONNECT_TIMEOUT=5s
MAIL_READ_TIMEOUT=5s
MAIL_WRITE_TIMEOUT=5s
MAIL_CONNECT_TIMEOUT_MS=5000
MAIL_READ_TIMEOUT_MS=5000
MAIL_WRITE_TIMEOUT_MS=5000
EMAIL_VERIFICATION_RESEND_THROTTLE_SECONDS=60
EMAIL_VERIFICATION_RESEND_EMAIL_HOURLY_LIMIT=5
EMAIL_VERIFICATION_RESEND_IP_HOURLY_LIMIT=20
```

규칙:

- `MAIL_FROM`이 비어 있으면 `MAIL_USERNAME`과 같은 Gmail 주소를 발신자로 사용합니다.
- `MAIL_FROM`이 `MAIL_USERNAME`과 다르면 Google 계정에서 검증된 별칭인 경우에만 `MAIL_FROM_VERIFIED_ALIAS=true`를 설정합니다.
- STARTTLS를 사용하고 TLS 인증서 검증을 비활성화하지 않습니다.
- SMTP connect/read/write timeout은 반드시 설정합니다.
- Gmail 인증 정보가 없거나 잘못되어도 MailHog로 자동 전환하거나 발송 성공으로 처리하지 않습니다.

## 3. Docker Compose / Preview 전달 경로 확인

전달 경로는 다음과 같습니다.

```text
운영자 비밀 설정
  -> Docker Compose 또는 Preview secret 환경변수
  -> backend 컨테이너 environment
  -> Spring mail / mail.* 설정
  -> Gmail SMTP
```

값 원문을 출력하지 말고 변수명과 존재 여부만 확인합니다.

```bash
# backend 컨테이너가 실행 중인 상태에서 실행
for key in MAIL_HOST MAIL_PORT MAIL_USERNAME MAIL_PASSWORD MAIL_SMTP_AUTH MAIL_SMTP_STARTTLS_ENABLE MAIL_FROM MAIL_VERIFICATION_BASE_URL; do
  docker compose -f infra/docker-compose.yml exec backend sh -lc "if printenv '$key' >/dev/null && [ -n \"\$(printenv '$key')\" ]; then echo '$key=present'; else echo '$key=missing'; fi"
done
```

예상 결과:

- `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS_ENABLE=true`가 전달됩니다.
- `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_VERIFICATION_BASE_URL`은 운영/Preview secret 설정 후 `present`로 표시됩니다.
- 비밀 원문은 터미널, 로그, PR 본문, 문서에 기록하지 않습니다.

## 4. backend 적용 확인

```bash
docker compose -f infra/docker-compose.yml up --build -d backend
docker compose -f infra/docker-compose.yml logs backend | grep -Ei 'started|mail|smtp|verification' || true
curl -i http://localhost:3000/api/health
```

예상 결과:

- backend가 정상 기동합니다.
- Gmail 설정 오류가 있으면 설정 미완료 또는 인증 실패로 기록되고 신규 가입 계정은 `PENDING_EMAIL` 상태를 유지합니다.
- `/api/health`는 HTTP 200 `ApiResponse.success=true`를 반환합니다.

## 5. 실제 수신자 검증 절차

1. 운영자가 승인한 실제 수신 이메일 주소를 준비합니다. Gmail, 네이버, 회사 이메일 등 사용자가 실제 사용하는 주소를 사용할 수 있습니다.
2. `/signup` 화면 또는 `POST /api/auth/signup`으로 신규 가입을 신청합니다.
3. DB 또는 관리자 조회로 신규 계정이 `account_status=PENDING_EMAIL`, `email_verified_yn=N`인지 확인합니다.
4. 실제 수신자 메일함에서 인증 메일을 확인합니다. MailHog 또는 테스트 로그는 이 단계의 증거가 아닙니다.
5. 메일 본문에 서비스명, 가입 login ID, 인증 링크/버튼, 24시간 만료 안내, 본인이 아니면 무시하라는 안내가 있는지 확인합니다.
6. 메일 본문과 로그에 비밀번호, password hash, raw token, SMTP 앱 비밀번호가 없는지 확인합니다.
7. 수신한 링크를 열어 `/email-verification` 화면과 `POST /api/auth/email-verifications/verify`가 계정을 활성화하는지 확인합니다.
8. 활성화 후 해당 계정으로 로그인하고, 기본 일반 사용자 권한만 부여되었는지 확인합니다.

## 6. 미실행 또는 실패 기록 기준

다음 중 하나라도 충족하지 못하면 Gmail 외부 발송 검증을 완료로 표시하지 않습니다.

- Gmail 앱 비밀번호 또는 secret 설정이 없음
- Preview/backend 컨테이너에 secret 전달 확인이 안 됨
- 운영자 승인 실제 수신자 메일함에서 메일 수신 확인이 안 됨
- 수신 링크로 계정 활성화까지 확인하지 못함

이 경우 `docs/gmail-external-delivery-status.md`의 상태를 `external-delivery-configuration-incomplete` 또는 `real-inbox-verification-not-executed`로 유지합니다.

## 7. 테스트 프로필 격리

자동화 테스트는 `test` Spring profile을 사용합니다. `test` profile에서는 Gmail sender bean을 사용하지 않고 외부 SMTP 발송을 수행하지 않습니다. 테스트는 메일 발송 요청 결과와 DB side effect를 검증하되 실제 외부 수신 검증 완료로 보고하지 않습니다.

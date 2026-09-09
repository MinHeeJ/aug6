#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

required_compose_vars=(
  MAIL_HOST
  MAIL_PORT
  MAIL_USERNAME
  MAIL_PASSWORD
  MAIL_SMTP_AUTH
  MAIL_SMTP_STARTTLS_ENABLE
  MAIL_FROM
  MAIL_FROM_VERIFIED_ALIAS
  MAIL_VERIFICATION_BASE_URL
  MAIL_CONNECT_TIMEOUT
  MAIL_READ_TIMEOUT
  MAIL_WRITE_TIMEOUT
  MAIL_CONNECT_TIMEOUT_MS
  MAIL_READ_TIMEOUT_MS
  MAIL_WRITE_TIMEOUT_MS
  EMAIL_VERIFICATION_RESEND_THROTTLE_SECONDS
  EMAIL_VERIFICATION_RESEND_EMAIL_HOURLY_LIMIT
  EMAIL_VERIFICATION_RESEND_IP_HOURLY_LIMIT
)

for var in "${required_compose_vars[@]}"; do
  if ! grep -q "${var}:" infra/docker-compose.yml; then
    echo "missing compose backend environment variable: ${var}" >&2
    exit 1
  fi
done

if ! grep -q '@Profile("!test")' backend/src/main/java/kr/ac/knue/commonfoundation/emailverification/GmailVerificationMailSender.java; then
  echo "GmailVerificationMailSender must be disabled for Spring test profile" >&2
  exit 1
fi

if ! grep -q 'username: ""' backend/src/test/resources/application-test.yml; then
  echo "test profile must leave mail username empty" >&2
  exit 1
fi

if ! grep -q 'real-inbox-verification-not-executed' docs/gmail-external-delivery-status.md; then
  echo "Gmail external delivery status must remain not executed until real inbox/link proof exists" >&2
  exit 1
fi

for forbidden in '/api/auth/password-reset' '/api/auth/reset-password' '/api/users/email-change' '/api/auth/email-change' '/admin/smtp-settings' '/smtp-settings'; do
  if grep -R -n --exclude-dir=node_modules --exclude-dir=target --exclude-dir=build --exclude-dir=dist --exclude='basic58-phase6-runtime-guards.sh' "$forbidden" backend frontend docs README.md; then
    echo "forbidden BASIC-58 out-of-scope surface found: ${forbidden}" >&2
    exit 1
  fi
done

echo "BASIC-58 phase 6 runtime/documentation guards passed"

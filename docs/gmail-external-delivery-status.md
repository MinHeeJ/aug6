# BASIC-58 Gmail External Delivery Status

status: real-inbox-verification-not-executed
configuration_status: external-delivery-configuration-incomplete
last_updated: 2026-09-09

## Current evidence

- Docker Compose now propagates Gmail SMTP variable names to the backend container without hardcoding secret values.
- Spring Boot configuration reads Gmail SMTP host, port, credentials, STARTTLS, sender alias flag, verification base URL, and timeout settings from environment-backed properties.
- Automated tests use the `test` profile and must not perform external SMTP delivery.
- No operator-approved real recipient inbox receipt has been observed in this repository run.
- No received verification link has been activated by an operator-approved real recipient in this repository run.

## Completion criteria

Change this status only after all checks below are true and evidence is recorded without exposing secrets:

1. The operator has configured a sending Gmail account with 2-step verification and an app password, or equivalent Google-approved SMTP credential.
2. `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_VERIFICATION_BASE_URL` are injected into the Preview or deployment backend container as secrets.
3. A signup request creates a `PENDING_EMAIL` account and requests Gmail delivery without claiming inbox arrival.
4. The operator-approved recipient confirms the email arrived in a real inbox, not MailHog or test logs.
5. The recipient opens the verification link and the application activates the account through `/email-verification` and `/api/auth/email-verifications/verify`.
6. The account can log in after activation and could not log in before activation.

Until those checks pass, report BASIC-58 Gmail external delivery as not executed, not verified.

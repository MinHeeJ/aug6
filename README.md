# aug6 common foundation

한국교원대학교 교수업적평가시스템 공통기능 1차 범위 애플리케이션입니다.

## 실행

```bash
docker compose -f infra/docker-compose.yml up --build
```

프론트엔드 branch preview 포트: http://localhost:3000

Compose는 다음 서비스를 실행합니다.

- `database`: PostgreSQL 16, host port 미노출
- `backend`: Spring Boot 3.3 / Java 17 executable boot jar, 내부 8080
- `frontend`: React 18 / Vite 5 정적 빌드 + nginx, host `3000:80`, `/api/` backend reverse proxy

## 시드 관리자 계정

- loginId: `admin`
- password: `admin`
- role: `R09 시스템관리자`

Docker Compose 실행 직후 위 계정으로 `/login`에서 로그인할 수 있어야 합니다.

## Health 확인

```bash
curl -i http://localhost:3000/api/health
```

예상 결과:

- HTTP 200
- JSON envelope `success=true`
- `data.status=UP`

## 인증과 health smoke

```bash
bash tests/smoke/auth-health.sh
```

스크립트는 다음을 확인합니다.

1. `GET /api/health`가 200과 `success=true`를 반환합니다.
2. 세션 없이 `GET /api/auth/me`를 호출하면 401을 반환합니다.
3. `POST /api/auth/login`에 `admin`/`admin`을 보내면 200과 HttpOnly SameSite=Lax 세션 쿠키를 반환합니다.
4. 세션 쿠키로 `GET /api/auth/me`를 호출하면 현재 사용자 `admin`과 R09 역할이 반환됩니다.
5. `POST /api/auth/logout` 후 같은 쿠키로 `/api/auth/me`를 호출하면 401을 반환합니다.

수동 curl 예시:

```bash
curl -i -c /tmp/knue.cookies \
  -H 'Content-Type: application/json' \
  -d '{"loginId":"admin","password":"admin"}' \
  http://localhost:3000/api/auth/login

curl -i -b /tmp/knue.cookies http://localhost:3000/api/auth/me
curl -i -b /tmp/knue.cookies -X POST http://localhost:3000/api/auth/logout
```

## 1차 목표 화면 검증

시드 관리자 로그인 후 다음 9개 보호 화면이 렌더링되어야 합니다. 각 화면은 TailwindAdmin-style mini-sidebar shell 안에서 breadcrumb/title card, 검색조건, 목록 또는 tree/table, 상세/편집 card, loading/empty/error/permission/success 상태를 제공합니다.

| route | screen_id | 예상 결과 |
|---|---|---|
| `/admin/users` | `SCR-USER-MGMT` | 사용자 검색, KORUS 조회 전용 필드, 시스템 사용여부/역할 저장 UI 표시 |
| `/admin/organizations` | `SCR-ORG-MGMT` | 조직 검색, 조직 tree, 상위조직 관계 적용기간 저장 UI 표시 |
| `/admin/roles` | `SCR-ROLE-MGMT` | R01~R09 역할 목록과 역할 메타정보 편집 UI 표시 |
| `/admin/user-roles` | `SCR-USER-ROLE-MGMT` | 사용자 역할 부여/변경/회수와 현재 역할 조회 UI 표시 |
| `/admin/menu-permissions` | `SCR-MENU-PERMISSION-MGMT` | 역할/조직/사용자별 메뉴 권한 조회·저장 UI 표시 |
| `/admin/menu-structure` | `SCR-MENU-STRUCTURE-MGMT` | 메뉴 계층 조회, 부모 변경, 표시순서 저장 UI 표시 |
| `/admin/menu-info` | `SCR-MENU-INFO-MGMT` | 메뉴 실행정보 조회·수정 UI 표시 |
| `/admin/code-groups` | `SCR-CODE-GROUP-MGMT` | 코드그룹 조회·등록·수정과 상세코드 연결 UI 표시 |
| `/admin/detail-codes` | `SCR-DETAIL-CODE-MGMT` | 코드그룹별 상세코드 관리 route가 메뉴 권한으로 보호됨 |

## 통합 E2E smoke

앱을 실행한 뒤 Playwright 설정의 baseURL을 `http://localhost:3000`으로 두고 다음 spec을 실행합니다.

```bash
npx playwright test tests/e2e/common-foundation.spec.ts
```

검증 내용:

- `admin`/`admin` 로그인
- 9개 관리 route의 `data-screen-id` 렌더링
- 대표 조회 API 2xx와 `success=true`
- 사용자 단위 메뉴 DENY 저장 후 메뉴 숨김과 직접 API 403 확인
- 테스트 종료 시 DENY 권한을 ALLOW로 원복

## BASIC-58 회원가입 이메일 인증 운영 확인

회원가입 인증 메일은 운영자가 설정한 Gmail SMTP 계정으로 발송합니다. 비밀값은 source, Dockerfile, 이미지, 로그, 오류 응답, 문서에 원문으로 남기지 않습니다.

필수 운영 변수는 다음과 같습니다.

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
```

- `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_VERIFICATION_BASE_URL`은 Docker Compose 또는 Preview secret으로 backend 컨테이너에 전달해야 합니다.
- `MAIL_FROM`은 기본적으로 `MAIL_USERNAME`과 같아야 하며, 다를 경우 Google에서 검증된 별칭일 때만 `MAIL_FROM_VERIFIED_ALIAS=true`로 설정합니다.
- Gmail credential이 없거나 잘못되면 MailHog로 전환하지 않고 신규 가입 계정을 `PENDING_EMAIL` 상태로 유지합니다.
- 실제 Gmail 외부 발송 완료는 운영자 승인 실제 수신함에서 메일 수신 및 인증 링크 활성화까지 성공한 뒤에만 주장할 수 있습니다.

상세 절차와 현재 검증 상태:

- `docs/operator-gmail-verification-runbook.md`
- `docs/gmail-external-delivery-status.md`

## Backend / Frontend 품질 게이트

로컬 의존성 설치가 완료된 환경에서는 다음을 실행합니다.

```bash
cd backend && mvn test
cd ../frontend && npm run test -- --run
cd .. && docker compose -f infra/docker-compose.yml config
```

이번 산출물은 `.aiops-spec/` 입력을 런타임 코드나 빌드 설정에서 참조하지 않으며, 브라우저 API 호출은 `/api/...` 상대경로만 사용합니다.

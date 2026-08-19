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

## Backend / Frontend 품질 게이트

로컬 의존성 설치가 완료된 환경에서는 다음을 실행합니다.

```bash
cd backend && mvn test
cd ../frontend && npm run test -- --run
cd .. && docker compose -f infra/docker-compose.yml config
```

이번 산출물은 `.aiops-spec/` 입력을 런타임 코드나 빌드 설정에서 참조하지 않으며, 브라우저 API 호출은 `/api/...` 상대경로만 사용합니다.

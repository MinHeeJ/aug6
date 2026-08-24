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

## BASIC-16 공통 파일 운영 검증

신규 네 화면은 기존 관리자 shell, 색상 token, 글꼴, 버튼, 아이콘 패턴을 재사용합니다.

| route | screen_id | Phase 7 확인 항목 |
|---|---|---|
| `/admin/file-policies` | `SCR-FILE-POLICY-MGMT` | 저장 확인 메시지, 필수 입력 표시, 파일정책 저장 결과 안내, R09 저장 권한 |
| `/admin/attachments` | `SCR-ATTACHMENT-METADATA` | 조회/empty/error/permission/success 상태, 다운로드 권한 재검증, 내부 저장정보 비노출 |
| `/admin/attachments/delete` | `SCR-ATTACHMENT-DELETE` | 삭제 확인 modal, 삭제사유 필수, 평가확정 삭제 차단, 논리삭제 결과 안내 |
| `/admin/attachment-integrity` | `SCR-ATTACHMENT-INTEGRITY` | 10초 이상 진행 안내, 완료 알림, 결과 목록, 엑셀 다운로드 결과 안내 |

정적 UI/KWCAG 2.1 smoke와 주요 페이지 3MB 예산 확인:

```bash
bash tests/smoke/basic16-phase7-ui-smoke.sh
```

`frontend/dist`가 있으면 JS/CSS/HTML 합산 크기가 3MB 이하인지 확인합니다. 빌드 산출물이 없으면 route, data-screen-id, data-testid, semantic/ARIA 표시만 확인하고 빌드 후 재실행하도록 안내합니다.

## PostgreSQL 백업·복구 리허설

Compose에는 `backup` profile의 `postgres-backup` 서비스가 포함되어 있습니다. 이 서비스는 PostgreSQL 16 이미지로 1일 1회 `pg_dump -Fc` 백업을 만들고 `BACKUP_RETENTION_DAYS`(기본 14일, 허용 7~30일)보다 오래된 백업을 삭제합니다. 인프라 서비스에는 host port를 노출하지 않습니다.

백업 sidecar 실행:

```bash
BACKUP_RETENTION_DAYS=14 docker compose -f infra/docker-compose.yml --profile backup up -d postgres-backup
```

복구 리허설 quickstart smoke:

```bash
BACKUP_RETENTION_DAYS=14 bash tests/smoke/postgres-backup-restore-rehearsal.sh
```

스크립트는 실행 중인 `database` 서비스에 대해 백업 파일 생성, 보관기간 범위(7~30일), 임시 리허설 DB 복원, 기본 catalog 조회, 리허설 DB 정리를 검증합니다. DB 비밀번호 원문은 로그로 출력하지 않습니다.

## Backend / Frontend 품질 게이트

로컬 의존성 설치가 완료된 환경에서는 다음을 실행합니다.

```bash
cd backend && mvn test
cd ../frontend && npm run test -- --run
cd .. && docker compose -f infra/docker-compose.yml config
```

이번 산출물은 `.aiops-spec/` 입력을 런타임 코드나 빌드 설정에서 참조하지 않으며, 브라우저 API 호출은 `/api/...` 상대경로만 사용합니다.

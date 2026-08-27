# BASIC-29 Phase 6 권한변경 로그 조회 Smoke

## Scope

- Task: T028
- Route: `/admin/audit/permission-change-logs`
- Screen: `SCR-PERMISSION-CHANGE-LOG`
- API: `GET /api/admin/audit/permission-change-logs`
- Primary table: `permission_change_history`

## Preconditions

- Docker Compose profile has applied backend migrations through `V20__basic29_permission_change_logs.sql`.
- Seed administrator `admin / admin` exists and has R09 menu access.
- Seed row `SEED-PERMISSION-CHANGE-001 권한변경 로그 조회 smoke` exists in `permission_change_history`.

## Smoke fixture

1. Login as `admin / admin`.
2. Open `/admin/audit/permission-change-logs`.
3. Verify the page root has `data-screen-id="SCR-PERMISSION-CHANGE-LOG"`.
4. Verify the search controls are visible:
   - 권한유형
   - 변경대상
   - 승인자 ID
   - 처리자 ID
   - 기간 시작/종료
   - 표시 건수 with 20/50/100 options
5. Search with `targetType=FUNCTION`, `approverUserId=1`, `changedBy=1`.
6. Verify rows display target type, target id, before/after JSON snapshots, approver, changer, reason, and changedAt.
7. Verify no 권한 부여, 권한 변경, 권한 회수, 삭제 CTA is present.

## Durable E2E artifact

- `tests/e2e/basic29-permission-change-logs.spec.ts`

## Execution note

This code generation phase authored the E2E artifact but did not execute Playwright because the runner instruction forbids package-manager/test execution during codegen. The later cross-cutting verification phase can run the spec after dependencies and preview services are materialized.

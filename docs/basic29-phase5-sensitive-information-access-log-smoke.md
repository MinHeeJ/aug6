# BASIC-29 Phase 5 UI Smoke Fixture: 중요정보 조회 로그

Scope: Phase 5 tasks T019~T023 only.

Route: `/admin/audit/sensitive-information-access-logs`
Screen ID: `SCR-SENSITIVE-INFO-ACCESS-LOG`
API operation: `GET /api/admin/audit/sensitive-information-access-logs`
Primary table: `sensitive_information_access_logs`
Seed marker: `SEED-SENSITIVE-ACCESS-001` via request_id `REQ-SENSITIVE-SEED`

Preserved behavior:
- Existing BASIC-29 active session, session termination history, and business process log routes remain unchanged.
- The new screen is read-only and does not expose create, update, delete, original-value reveal, account plaintext, or evaluation-result plaintext actions.
- Frontend calls use relative `/api/...` paths only.

Requested behavior implemented:
- R09 route entry is registered in the admin route inventory.
- The screen provides informationType, viewer, fromDate, toDate, accessResult, page, and size query controls.
- The list displays information type, viewer, target scope, access purpose, purpose source, access timestamp, result, and request ID.
- Loading, empty, error, permission, and success states are represented through existing shared UI state components.
- The backend endpoint applies R09-only access, allowed 20/50/100 pagination, dynamic MyBatis predicates, and validation for informationType, accessResult, and date range.

Verification notes:
- Backend JUnit/MockMvc and mapper contract tests were authored under `backend/src/test/java/kr/ac/knue/commonfoundation/audit/`.
- Frontend Vitest route/state/API-path test was authored at `frontend/src/pages/admin/SCR-SENSITIVE-INFO-ACCESS-LOG.test.tsx`.
- Repository has no top-level `e2e/` Playwright project. Browser-level E2E execution was therefore left as a follow-up; the reusable route/state test fixture is present for this phase.
- Per codegen instruction, `mvn test`, npm commands, package managers, dev servers, and watchers were not run in this phase.

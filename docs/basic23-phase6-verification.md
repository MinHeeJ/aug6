# BASIC-23 Phase 6 Cross-cutting Verification

## Scope

This document records reviewer-facing verification coverage for Phase 6 tasks T026-T034.
It does not reference runner-owned input paths from generated source or runtime code.

## Checklist

- T026: Four batch list APIs default to 20 rows and accept the approved 20/50/100 sizes.
  - Backend: `BatchCrossCuttingGuardApiTest` covers default and explicit sizes.
  - Frontend: each batch screen test covers default API helper size and `pageSizeOptions`.
- T027: Excel download remains an OQ because no common export pattern is present in this repository.
  - Each batch screen exposes a reviewer-visible `REQ-386 OQ` message through the export UI hook or screen API metadata.
- T028: Required field 표시/차단 checks are covered for definition save, execution run/stop/rerun, and retry forms.
- T029: 저장/일괄처리 전 확인 and post-action 안내 messages are covered by frontend tests.
- T030: 10초 이상 execution/retry work exposes progress state and completion guidance.
- T031: `X-Request-Id` is passed to mutation services for persistence logs and echoed as `meta.requestId` in save/run/stop/rerun/retry responses.
- T032: Accessibility/performance/browser checklist for reviewer smoke:
  - Keyboard-visible controls use semantic `button`, `input`, `select`, `textarea`, `label`, and table markup.
  - Main pages are Vite route chunks and should remain under the 3MB page target after `npm run build`.
  - Manual smoke targets: Edge, Chrome, Safari, Opera, Whale desktop; iPadOS and Android tablet.
  - Frontend API calls remain relative `/api/...` paths only.
- T033: Retry creates separate retry execution/result rows and does not overwrite original execution result/log data.
- T034: Executable smoke guard is available at `tests/smoke/basic23-cross-cutting.sh`.

## Execution notes

Attempted frontend test command:

```bash
cd frontend && npm run test -- --run src/pages/admin/SCR-BATCH-DEFINITION-MGMT.test.tsx src/pages/admin/SCR-BATCH-EXECUTION-MGMT.test.tsx src/pages/admin/SCR-BATCH-RESULT-MGMT.test.tsx src/pages/admin/SCR-BATCH-RETRY-MGMT.test.tsx
```

Observed blocker:

```text
sh: 1: vitest: not found
```

The dependency tree is not materialized in this worktree. Per codegen constraints, package managers were not run and no lockfile was fabricated.
A later verification/repair phase should run `npm ci` and then the frontend test/typecheck/build commands.
Backend MockMvc tests were authored but `mvn test` was not run because this codegen phase explicitly disallows long-running Maven test execution.

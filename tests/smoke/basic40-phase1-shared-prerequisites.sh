#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "FAIL: $1" >&2
  exit 1
}
pass() {
  echo "PASS: $1"
}

[[ -d backend ]] || fail "backend directory missing"
[[ -d frontend ]] || fail "frontend directory missing"
[[ -f infra/docker-compose.yml ]] || fail "infra/docker-compose.yml missing"
[[ -f backend/pom.xml ]] || fail "backend/pom.xml missing"
[[ -f frontend/package.json ]] || fail "frontend/package.json missing"
[[ -f backend/.dockerignore ]] || fail "backend/.dockerignore missing"
[[ -f frontend/.dockerignore ]] || fail "frontend/.dockerignore missing"
pass "required repository structure exists"

compose=infra/docker-compose.yml
grep -q '^services:' "$compose" || fail "compose services root missing"
grep -q '^  database:' "$compose" || fail "database service missing"
grep -q '^  backend:' "$compose" || fail "backend service missing"
grep -q '^  frontend:' "$compose" || fail "frontend service missing"
grep -q 'postgres:16' "$compose" || fail "PostgreSQL 16 image missing"
grep -q 'context: ../backend' "$compose" || fail "backend static build context missing"
grep -q 'context: ../frontend' "$compose" || fail "frontend static build context missing"
if grep -Eq '5432:5432|context: *\$\{' "$compose"; then
  fail "compose violates database host port or build.context interpolation rule"
fi
pass "compose topology matches canonical runtime"

for p in   backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationPort.java   backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationFilter.java   backend/src/main/java/kr/ac/knue/commonfoundation/permissions/EffectivePermissionService.java   backend/src/main/java/kr/ac/knue/commonfoundation/permissions/PermissionMapper.java   backend/src/main/java/kr/ac/knue/commonfoundation/excel/ExcelOperationsService.java   backend/src/main/java/kr/ac/knue/commonfoundation/batch/BatchDefinitionController.java   backend/src/main/java/kr/ac/knue/commonfoundation/audit/BusinessProcessLogController.java   frontend/src/app/router.tsx   frontend/src/components/layout/AdminShell.tsx   frontend/src/api/apiClient.ts; do
  [[ -e "$p" ]] || fail "common reuse target missing: $p"
done
pass "common backend/frontend reuse targets exist"

python3 - <<'PY'
from pathlib import Path
import re, sys
seed='\\n'.join(p.read_text(errors='ignore') for p in Path('backend/src/main/resources/db/migration').glob('*.sql'))
missing=[role for role in ['R01','R02','R03','R04','R05','R06','R07','R08','R09'] if role not in seed]
if missing:
    sys.exit('missing role seed codes: '+','.join(missing))
files=sorted(p.name for p in Path('backend/src/main/resources/db/migration').glob('V*__*.sql'))
if any('basic40' in name.lower() for name in files):
    sys.exit('BASIC-40 migration exists before reserved implementation phase')
versions=[]
for name in files:
    m=re.match(r'V(\d+)', name)
    if m:
        versions.append(int(m.group(1)))
if max(versions) != 40:
    sys.exit(f'expected current max migration V40 before BASIC-40, got V{max(versions)}')
PY
pass "role seeds and migration reservation boundary are valid"

fixture=backend/src/test/resources/contracts/openapi.yaml
[[ -f "$fixture" ]] || fail "durable OpenAPI fixture missing"
for op in listAppealPeriods saveAppealPeriod listResultViewPeriods saveResultViewPeriod listExceptionPeriods saveExceptionPeriod; do
  grep -q "operationId: $op" "$fixture" || fail "OpenAPI fixture missing $op"
done
pass "BASIC-40 effective OpenAPI operations are locked in durable fixture"

[[ -f docs/requirements/basic40/effective-contract-lock.json ]] || fail "docs contract lock missing"
[[ -f backend/src/test/resources/requirements/basic40/effective-contract-lock.json ]] || fail "backend contract lock missing"
grep -q 'V41__basic40_remaining_period_controls.sql' docs/requirements/basic40/effective-contract-lock.json || fail "migration reservation missing from contract lock"
pass "effective contract lock and migration reservation exist"

if grep -R --include='*.ts' --include='*.tsx' -E 'http://localhost:8080|127\.0\.0\.1:8080' frontend/src >/dev/null; then
  fail "frontend source contains forbidden absolute backend URL"
fi
pass "frontend API source keeps relative path contract"

pass "BASIC-40 Phase 1 shared prerequisites READY"

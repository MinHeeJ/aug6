#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

REGISTRY=".specify/specs/001-feature/requirement-registry.json"
EFFECTIVE_DIR="docs/requirements/basic37/effective-contract"
OPENAPI_DOC="$EFFECTIVE_DIR/contracts/openapi.yaml"
OPENAPI_MAIN="backend/src/main/resources/contracts/openapi.yaml"
OPENAPI_TEST="backend/src/test/resources/contracts/openapi.yaml"

python3 - <<'PY'
import json
from pathlib import Path
registry_path = Path('.specify/specs/001-feature/requirement-registry.json')
assert registry_path.exists(), 'missing requirement registry at .specify/specs/001-feature/requirement-registry.json'
registry = json.loads(registry_path.read_text())
requirements = {item['canonicalId']: item for item in registry.get('requirements', [])}
expected = [f'REQ-{number}' for number in range(1149, 1162)]
missing = [req for req in expected if req not in requirements]
assert not missing, f'missing BASIC-37 requirements: {missing}'
for req in expected:
    item = requirements[req]
    assert item.get('implementationIssueLabel'), f'{req} missing implementationIssueLabel'
    labels = item.get('testLabels') or []
    assert labels, f'{req} missing testLabels'
    assert any(str(label).startswith('basic37:') for label in labels), f'{req} missing basic37 test label'
phase_tasks = {task['taskId']: task for task in registry.get('phaseTasks', [])}
for task_id in ['T001', 'T002']:
    assert task_id in phase_tasks, f'{task_id} trace missing from phaseTasks'
    assert phase_tasks[task_id].get('canonicalIds') == ['REQ-1161'], f'{task_id} must trace REQ-1161'
PY

for path in \
  "$EFFECTIVE_DIR/data-model.md" \
  "$EFFECTIVE_DIR/ui-design.md" \
  "$OPENAPI_DOC" \
  "$EFFECTIVE_DIR/approved-change-set.json"; do
  test -s "$path" || { echo "missing effective contract artifact: $path" >&2; exit 1; }
done

cmp -s "$OPENAPI_DOC" "$OPENAPI_MAIN" || { echo "backend main OpenAPI is not the BASIC-37 effective contract" >&2; exit 1; }
cmp -s "$OPENAPI_DOC" "$OPENAPI_TEST" || { echo "backend test OpenAPI is not the BASIC-37 effective contract" >&2; exit 1; }

python3 - <<'PY'
from pathlib import Path
openapi = Path('docs/requirements/basic37/effective-contract/contracts/openapi.yaml').read_text()
required_snippets = [
    'SessionCookie:',
    'type: apiKey',
    'in: cookie',
    'ApiResponse:',
    'success:',
    '- true',
    'ApiError:',
    '- false',
    'fields:',
    '/api/admin/researcher-profiles/faculty-search:',
    'operationId: listFacultySearchResults',
    '/api/admin/researcher-profiles:',
    'operationId: listResearcherProfiles',
    '/api/admin/researcher-profiles/degree-deficiencies:',
    'operationId: listDegreeDeficiencyTargets',
    '/api/admin/batch-results:',
    'operationId: listBatchResults',
    '/api/admin/excel-upload-templates:',
    'operationId: listUploadTemplates',
]
missing = [snippet for snippet in required_snippets if snippet not in openapi]
assert not missing, f'OpenAPI effective contract missing snippets: {missing}'
for req in [f'REQ-{number}' for number in range(1149, 1155)]:
    assert req in openapi, f'{req} missing from OpenAPI trace'
PY

echo "PASS basic37 phase1 baseline contract setup"

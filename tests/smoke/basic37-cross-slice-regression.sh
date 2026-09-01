#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "FAIL $1" >&2
  exit 1
}

require_file() {
  local file="$1"
  [ -f "$file" ] || fail "missing required file: $file"
  echo "OK file $file"
}

require_grep() {
  local pattern="$1"
  local file="$2"
  local label="$3"
  grep -Eq "$pattern" "$file" || fail "$label"
  echo "OK $label"
}

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

require_file backend/src/test/java/kr/ac/knue/commonfoundation/basic36/ResearcherProfileAdminLookupApiTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/basic36/ResearcherProfileMapperAliasAuditTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/seed/Basic37SharedSeedFixtureTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/basic37/BatchAndUploadTemplateMapperAliasAuditTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchResultManagementApiTest.java
require_file backend/src/test/java/kr/ac/knue/commonfoundation/excel/ExcelOperationsApiTest.java
require_file tests/e2e/basic26-excel-operations.spec.ts
require_file tests/smoke/basic23-cross-cutting.sh
require_file frontend/src/pages/admin/SCR-BATCH-RESULT-MGMT.test.tsx
require_file frontend/src/pages/admin/ExcelOperationsPages.test.tsx

# T025 — researcher faculty search API, mapper projection, table, and BASIC37 faculty seed stay wired into regression.
require_grep "listFacultySearchResultsReturnsApiResponseRowsForReq1149" backend/src/test/java/kr/ac/knue/commonfoundation/basic36/ResearcherProfileAdminLookupApiTest.java "T025 MockMvc faculty-search ApiResponse contract is covered"
require_grep "listFacultySearchResults\(" backend/src/main/java/kr/ac/knue/commonfoundation/basic36/ResearcherProfileController.java "T025 listFacultySearchResults controller operation exists"
require_grep "<select id=\"listFacultySearchResults\"" backend/src/main/resources/mapper/basic36/ResearcherProfileMapper.xml "T025 listFacultySearchResults mapper select exists"
require_grep "FacultySearchResultRow|facultyId" backend/src/main/resources/mapper/basic36/ResearcherProfileMapper.xml "T025 faculty_search_results projection row is queried"
require_grep "BASIC37-SEED-FACULTY-001" backend/src/test/java/kr/ac/knue/commonfoundation/basic36/ResearcherProfileAdminLookupApiTest.java "T025 faculty seed smoke assertion is present"
require_grep "BASIC37-SEED-FACULTY-" backend/src/test/java/kr/ac/knue/commonfoundation/seed/Basic37SharedSeedFixtureTest.java "T025 faculty seed fixture is counted by CI smoke"

# T026 — researcher profile list API, mapper projection, table, and BASIC37 researcher seed stay wired into regression.
require_grep "listResearcherProfilesReturnsApiResponseRowsForReq1150" backend/src/test/java/kr/ac/knue/commonfoundation/basic36/ResearcherProfileAdminLookupApiTest.java "T026 MockMvc researcher-profile ApiResponse contract is covered"
require_grep "listResearcherProfiles\(" backend/src/main/java/kr/ac/knue/commonfoundation/basic36/ResearcherProfileController.java "T026 listResearcherProfiles controller operation exists"
require_grep "<select id=\"listResearcherProfiles\"" backend/src/main/resources/mapper/basic36/ResearcherProfileMapper.xml "T026 listResearcherProfiles mapper select exists"
require_grep "researcher_profiles" backend/src/main/resources/mapper/basic36/ResearcherProfileMapper.xml "T026 researcher_profiles projection is queried"
require_grep "BASIC37-SEED-RESEARCHER-001" backend/src/test/java/kr/ac/knue/commonfoundation/basic36/ResearcherProfileAdminLookupApiTest.java "T026 researcher seed smoke assertion is present"
require_grep "BASIC37-SEED-RESEARCHER-" backend/src/test/java/kr/ac/knue/commonfoundation/seed/Basic37SharedSeedFixtureTest.java "T026 researcher seed fixture is counted by CI smoke"

# T027 — degree deficiency API, mapper projection, table, and BASIC37 degree seed stay wired into regression.
require_grep "listDegreeDeficiencyTargetsReturnsApiResponseRowsForReq1151" backend/src/test/java/kr/ac/knue/commonfoundation/basic36/ResearcherProfileAdminLookupApiTest.java "T027 MockMvc degree-deficiency ApiResponse contract is covered"
require_grep "listDegreeDeficiencyTargets\(" backend/src/main/java/kr/ac/knue/commonfoundation/basic36/ResearcherProfileController.java "T027 listDegreeDeficiencyTargets controller operation exists"
require_grep "<select id=\"listDegreeDeficiencyTargets\"" backend/src/main/resources/mapper/basic36/ResearcherProfileMapper.xml "T027 listDegreeDeficiencyTargets mapper select exists"
require_grep "DegreeDeficiencyTargetRow|targetId" backend/src/main/resources/mapper/basic36/ResearcherProfileMapper.xml "T027 degree_deficiency_targets projection row is queried"
require_grep "BASIC37-SEED-DEGREE-001" backend/src/test/java/kr/ac/knue/commonfoundation/basic36/ResearcherProfileAdminLookupApiTest.java "T027 degree seed smoke assertion is present"
require_grep "BASIC37-SEED-DEGREE-" backend/src/test/java/kr/ac/knue/commonfoundation/seed/Basic37SharedSeedFixtureTest.java "T027 degree seed fixture is counted by CI smoke"

# T028 — BASIC-37 batch result route/API smoke is connected alongside the existing BASIC-23 batch cross-cutting smoke.
require_grep "basic37ListBatchResultsContractReturnsApiResponseForSeedExecutionWithoutDtoMappingLeak" backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchResultManagementApiTest.java "T028 BASIC-37 batch result list API smoke is covered"
require_grep "basic37GetBatchResultLogContractReturnsReadonlyLogRefForSelectedExecution" backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchResultManagementApiTest.java "T028 BASIC-37 batch result log API smoke is covered"
require_grep "BASIC37-SEED-BATCH-001" backend/src/test/java/kr/ac/knue/commonfoundation/batch/BatchResultManagementApiTest.java "T028 BASIC-37 batch seed smoke assertion is present"
require_grep "batch_execution_results|batch_execution_logs" backend/src/main/resources/mapper/batch/BatchResultMapper.xml "T028 batch mapper alias audit is tied to BASIC-37 smoke"
require_grep "batch23 cross-cutting|basic23 cross-cutting|basic23" tests/smoke/basic23-cross-cutting.sh "T028 existing BASIC-23 smoke remains available"
require_grep "batch-result" frontend/src/pages/admin/SCR-BATCH-RESULT-MGMT.test.tsx "T028 batch result route/component smoke remains present"

# T029 — BASIC-37 upload-template route/API smoke is connected alongside the existing BASIC-26 Excel smoke.
require_grep "basic37ListUploadTemplatesReturnsSeedTemplateRulesInApiResponseEnvelope" backend/src/test/java/kr/ac/knue/commonfoundation/excel/ExcelOperationsApiTest.java "T029 BASIC-37 upload-template API smoke is covered"
require_grep "BASIC37-SEED-EXCEL-TEMPLATE-001" backend/src/test/java/kr/ac/knue/commonfoundation/excel/ExcelOperationsApiTest.java "T029 BASIC-37 upload-template seed smoke assertion is present"
require_grep "listUploadTemplates" backend/src/test/java/kr/ac/knue/commonfoundation/basic37/BatchAndUploadTemplateMapperAliasAuditTest.java "T029 upload-template mapper audit is tied to BASIC-37 smoke"
require_grep "excel-upload-templates" tests/e2e/basic26-excel-operations.spec.ts "T029 existing BASIC-26 Excel route smoke remains available"
require_grep "SEED-EXCEL-TEMPLATE-001|BASIC37-SEED-EXCEL-TEMPLATE-001" frontend/src/pages/admin/ExcelOperationsPages.test.tsx "T029 upload-template route/component smoke displays seeded template data"


bash tests/smoke/basic23-cross-cutting.sh >/tmp/basic37-basic23-smoke.txt
cat /tmp/basic37-basic23-smoke.txt
rm -f /tmp/basic37-basic23-smoke.txt

echo "basic37 cross-slice regression smoke passed"

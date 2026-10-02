# API runtime repeated failures

- source_fingerprint: `559458f39be9628918fe7c886af9e7a2d32a53e5efe79e0455366e0c40f77354`
- contract_fingerprint: `6c98d96cdfef24fceefebacd774427732daef6985186bf1a63e344d1d5ad1e71`

## Repeated failures

### GET /api/admin/school-info (get-api-admin-school-info)

- failure_signature: `e819b90b5af100be56d894984adf69232fa152fc0b78243862ca3042d5123140`
- category: `http_status_mismatch`
- reason: expected [200], got 502
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/student-guidance-achievements/excel-uploads/histories (get-api-business-student-guidance-achievements-excel-uploads-histories)

- failure_signature: `43d48530a28d2c2dd5c18d5413c7f5466fc3cc4ab5ccdccd3f198f672757d24b`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

## Coverage

- required_operations: 5
- executed_operations: 5

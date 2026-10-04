# API runtime repeated failures

- source_fingerprint: `f0666b9868367cf44b9f6b33567bbd045c6d1736839b42fcd00c30e0e40a8c78`
- contract_fingerprint: `d206dafcb44e568f93ed40963037f50a28c5d35272ca4ddcbceabe50604ff511`

## Repeated failures

### GET /api/admin/school-info (get-api-admin-school-info)

- failure_signature: `e819b90b5af100be56d894984adf69232fa152fc0b78243862ca3042d5123140`
- category: `http_status_mismatch`
- reason: expected [200], got 502
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/lecture-improvements (get-api-business-lecture-improvements)

- failure_signature: `0904ccd2039938cf4d5e38a4cea61ca96ad72a41809c4a62bb4e04089a9c9009`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/employment-rate-improvements (get-api-business-employment-rate-improvements)

- failure_signature: `41bf953fb352665a1af1ba50aee6c84e8fbaa3bd1a9cf7147c7d17bb1e6ebb92`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/employment-rate-achievements (get-api-business-employment-rate-achievements)

- failure_signature: `5323b27c4bc6be3a88c0e81f9033124709a1f86266f39a22b74f34868f03fd3a`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/employment-rate-achievements/download (get-api-business-employment-rate-achievements-download)

- failure_signature: `eb44713c11dbe7343560ce753b4b8b5189ccb21d90ed78a74a322f1318ccff43`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/course-operations (get-api-business-course-operations)

- failure_signature: `9119b98421fd6d56653b498f48e4adbb9b5570bf6573b121930be349a0b17e19`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/degree-completion-achievements (get-api-business-degree-completion-achievements)

- failure_signature: `edb2fe3301b9356ac693961134a5b32785766bbb77570d1f3aff84d9b9fb806c`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/lecture-achievements (get-api-business-lecture-achievements)

- failure_signature: `09b4c123da0f22012fa8e26f19337b2e531d0491d643fb1010ca3a511828301c`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/lecture-evaluation-achievements (get-api-business-lecture-evaluation-achievements)

- failure_signature: `aac9c11412d023be39742a53f35d26fb6953ef285ca53d9c194b7283e5c74f0c`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

### GET /api/business/student-guidance-achievements (get-api-business-student-guidance-achievements)

- failure_signature: `220ece63f5b96258576bfccdae0dec009c0011cdb97c57f4ef5b9f85218980fa`
- category: `http_status_mismatch`
- reason: expected [200], got 403
- action: same cause reached the second failure; no third retry or repair request

## Coverage

- required_operations: 10
- executed_operations: 10

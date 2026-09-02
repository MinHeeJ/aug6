#!/usr/bin/env python3
"""BASIC-43 cross-cutting validation for OpenAPI requirement mapping.

This check intentionally uses durable repository fixtures only. It validates that
runtime and test OpenAPI contracts carry the BASIC-43 operations and that every
BASIC-43 canonical requirement is accounted for through either operation-level
x-related-requirements or top-level x-uncovered-requirements/x-requirement-accounting.
"""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

import yaml

EXPECTED_OPERATIONS = {
    "listDepartmentChairConfirmTargets": {
        "REQ-1336",
        "REQ-1339",
    },
    "saveDepartmentChairConfirmTargetsTransition": {
        "REQ-1337",
        "REQ-1338",
        "REQ-1339",
        "REQ-1362",
    },
    "listAchievementVerificationTargets": {
        "REQ-1341",
        "REQ-1344",
    },
    "saveAchievementVerificationTargetsTransition": {
        "REQ-1342",
        "REQ-1343",
        "REQ-1344",
        "REQ-1346",
        "REQ-1362",
    },
    "listGrantPaymentApprovals": {
        "REQ-1347",
        "REQ-1349",
    },
    "saveGrantPaymentApprovalsTransition": {
        "REQ-1348",
        "REQ-1349",
        "REQ-1351",
        "REQ-1352",
        "REQ-1362",
    },
    "listObjectionOpinions": {
        "REQ-1353",
        "REQ-1355",
    },
    "saveObjectionOpinionsTransition": {
        "REQ-1354",
        "REQ-1355",
        "REQ-1356",
        "REQ-1359",
        "REQ-1362",
    },
}

EXPECTED_REQUIREMENTS = {f"REQ-{number}" for number in range(1259, 1363)}
CONTRACT_PATHS = [
    Path("backend/src/main/resources/contracts/openapi.yaml"),
    Path("backend/src/test/resources/contracts/openapi.yaml"),
]


def collect_operations(contract: dict[str, Any]) -> dict[str, dict[str, Any]]:
    operations: dict[str, dict[str, Any]] = {}
    for path_item in (contract.get("paths") or {}).values():
        if not isinstance(path_item, dict):
            continue
        for value in path_item.values():
            if isinstance(value, dict) and value.get("operationId"):
                operations[value["operationId"]] = value
    return operations


def collect_accounted_requirements(contract: dict[str, Any]) -> set[str]:
    accounted: set[str] = set()
    for operation in collect_operations(contract).values():
        accounted.update(operation.get("x-related-requirements") or [])
    for extension_name in ("x-uncovered-requirements", "x-requirement-accounting"):
        for item in contract.get(extension_name) or []:
            if isinstance(item, dict) and item.get("canonical_id"):
                accounted.add(str(item["canonical_id"]))
    return accounted


def validate_contract(path: Path) -> list[str]:
    errors: list[str] = []
    if not path.exists():
        return [f"{path}: file is missing"]

    contract = yaml.safe_load(path.read_text(encoding="utf-8"))
    operations = collect_operations(contract)

    for operation_id, required_ids in EXPECTED_OPERATIONS.items():
        operation = operations.get(operation_id)
        if operation is None:
            errors.append(f"{path}: missing operationId {operation_id}")
            continue
        related_ids = set(operation.get("x-related-requirements") or [])
        missing_related = sorted(required_ids - related_ids)
        if missing_related:
            errors.append(
                f"{path}: {operation_id} missing x-related-requirements "
                f"{', '.join(missing_related)}"
            )
        if operation_id.startswith("save"):
            for extension_name in ("x-business-rules", "x-side-effects", "x-required-tests"):
                if not operation.get(extension_name):
                    errors.append(f"{path}: {operation_id} missing {extension_name}")
        if operation.get("security") != [{"SessionCookie": []}]:
            errors.append(f"{path}: {operation_id} must use SessionCookie security")
        if operation.get("x-roles") != ["R09"]:
            errors.append(f"{path}: {operation_id} must be scoped to R09 only")

    missing_accounting = sorted(EXPECTED_REQUIREMENTS - collect_accounted_requirements(contract))
    if missing_accounting:
        errors.append(
            f"{path}: missing BASIC-43 canonical accounting for "
            f"{', '.join(missing_accounting)}"
        )
    return errors


def main() -> int:
    errors: list[str] = []
    for contract_path in CONTRACT_PATHS:
        errors.extend(validate_contract(contract_path))

    if errors:
        print("BASIC-43 cross-cutting validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1

    print("BASIC-43 OpenAPI requirement mapping consistency: PASS")
    print(f"Validated {len(EXPECTED_OPERATIONS)} operations and {len(EXPECTED_REQUIREMENTS)} canonical IDs.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

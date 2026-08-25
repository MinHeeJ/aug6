import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { privacyPolicyApi } from "../../api/apiClient";
import {
  buildPrivacyPolicySavePayload,
  getPrivacyPolicyRouteContract,
  PrivacyPolicyManagementPage,
  validatePrivacyPolicyForm,
} from "./SCR-PRIVACY-POLICY-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    privacyPolicyApi: {
      listPrivacyFieldPolicies: vi.fn(),
      savePrivacyFieldPolicies: vi.fn(),
    },
  };
});

const selectedPolicy = {
  policyId: 1,
  fieldKey: "researcher_registration_no",
  privacyGrade: "SENSITIVE" as const,
  encryptionRequiredYn: "Y" as const,
  maskingRule: "LAST4",
  logExclusionYn: "Y" as const,
  changeReason: "초기 정책",
  updatedAt: "2026-08-25T09:00:00",
};

describe("SCR-PRIVACY-POLICY-MGMT cross-cutting UI contract", () => {
  it("declares route, screen id, page-size options, and relative API operation contract", () => {
    expect(getPrivacyPolicyRouteContract()).toEqual({
      route: "/admin/privacy/policies",
      screenId: "SCR-PRIVACY-POLICY-MGMT",
      operations: ["listPrivacyFieldPolicies", "savePrivacyFieldPolicies"],
      pageSizes: [20, 50, 100],
      defaultPageSize: 20,
    });
  });

  it("필수값 누락 시 저장 차단을 위한 field-level validation 결과를 만든다", () => {
    const errors = validatePrivacyPolicyForm({
      fieldKey: "",
      privacyGrade: "SENSITIVE",
      encryptionRequiredYn: "Y",
      maskingRule: "LAST4",
      logExclusionYn: "Y",
      changeReason: "",
    });

    expect(errors).toEqual({
      fieldKey: "개인정보 필드를 입력하세요.",
      changeReason: "변경 사유를 입력하세요.",
    });
  });

  it("저장 payload에는 정책 필드만 포함하고 실제 개인정보 원문 값은 포함하지 않는다", () => {
    const payload = buildPrivacyPolicySavePayload({
      fieldKey: selectedPolicy.fieldKey,
      privacyGrade: selectedPolicy.privacyGrade,
      encryptionRequiredYn: selectedPolicy.encryptionRequiredYn,
      maskingRule: selectedPolicy.maskingRule,
      logExclusionYn: selectedPolicy.logExclusionYn,
      changeReason: "저장 확인 및 결과 안내 검증",
    });

    expect(payload).toEqual({
      fieldKey: "researcher_registration_no",
      privacyGrade: "SENSITIVE",
      encryptionRequiredYn: "Y",
      maskingRule: "LAST4",
      logExclusionYn: "Y",
      changeReason: "저장 확인 및 결과 안내 검증",
    });
    expect(payload).not.toHaveProperty("actualValue");
    expect(payload).not.toHaveProperty("originalValue");
    expect(payload).not.toHaveProperty("plainValue");
  });
});

describe("PrivacyPolicyManagementPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(privacyPolicyApi.listPrivacyFieldPolicies).mockResolvedValue({
      success: true,
      data: { policies: [selectedPolicy], page: 0, size: 20, totalElements: 1 },
      meta: {},
    });
    vi.mocked(privacyPolicyApi.savePrivacyFieldPolicies).mockResolvedValue({
      success: true,
      data: [selectedPolicy],
      meta: {},
    });
  });

  it("기본 20건 정책 목록을 조회하고 실제 개인정보 원문 열을 표시하지 않는다", async () => {
    render(<PrivacyPolicyManagementPage />);

    expect(
      await screen.findByText("researcher_registration_no"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("privacy-policy-page-size-select")).toHaveValue(
      "20",
    );
    expect(screen.queryByText("actualValue")).not.toBeInTheDocument();
    expect(screen.queryByText("originalValue")).not.toBeInTheDocument();
    expect(privacyPolicyApi.listPrivacyFieldPolicies).toHaveBeenCalledWith({
      fieldKey: "",
      privacyGrade: "",
      encryptionRequiredYn: "",
      page: 0,
      size: 20,
    });
  });

  it("행 선택 후 저장 확인을 거쳐 저장하고 성공 재조회를 수행한다", async () => {
    render(<PrivacyPolicyManagementPage />);
    fireEvent.click(await screen.findByTestId("privacy-policy-row-1"));
    fireEvent.change(screen.getByTestId("privacy-policy-change-reason-input"), {
      target: { value: "암호화 정책 변경" },
    });
    fireEvent.click(screen.getByTestId("privacy-policy-save-button"));

    await waitFor(() =>
      expect(privacyPolicyApi.savePrivacyFieldPolicies).toHaveBeenCalledWith([
        expect.objectContaining({
          fieldKey: "researcher_registration_no",
          encryptionRequiredYn: "Y",
          logExclusionYn: "Y",
          changeReason: "암호화 정책 변경",
        }),
      ]),
    );
    await waitFor(() =>
      expect(privacyPolicyApi.listPrivacyFieldPolicies).toHaveBeenCalledTimes(
        2,
      ),
    );
    expect(await screen.findByText("저장되었습니다")).toBeInTheDocument();
  });
});

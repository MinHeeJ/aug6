import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PrivacyPermissionManagementPage } from "./SCR-PRIVACY-ACCESS-PERMISSION-MGMT";
import { privacyPermissionApi } from "../../api/apiClient";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    privacyPermissionApi: {
      listPrivacyAccessPermissions: vi.fn(),
      savePrivacyAccessPermissions: vi.fn(),
      evaluatePrivacyAccessPermission: vi.fn(),
    },
  };
});

const permission = {
  permissionId: 1,
  roleCode: "R09",
  roleName: "시스템관리자",
  fieldKey: "researcher_registration_no",
  rawViewAllowedYn: "Y" as const,
  maskedViewAllowedYn: "Y" as const,
  exportAllowedYn: "Y" as const,
  accountViewAllowedYn: "Y" as const,
  changeReason: "초기 권한",
  updatedAt: "2026-08-25T09:00:00",
};

describe("PrivacyPermissionManagementPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(
      privacyPermissionApi.listPrivacyAccessPermissions,
    ).mockResolvedValue({
      success: true,
      data: { permissions: [permission], page: 0, size: 20, totalElements: 1 },
      meta: {},
    });
    vi.mocked(
      privacyPermissionApi.savePrivacyAccessPermissions,
    ).mockResolvedValue({
      success: true,
      data: [permission],
      meta: {},
    });
    vi.mocked(
      privacyPermissionApi.evaluatePrivacyAccessPermission,
    ).mockResolvedValue({
      success: true,
      data: {
        roleCode: "R01",
        fieldKey: "researcher_registration_no",
        accessType: "RAW_VIEW",
        allowed: false,
        reason: "미설정 역할·필드 조합은 기본 차단됩니다.",
        rawValueExposed: false,
      },
      meta: {},
    });
  });

  it("역할과 field_key 조건으로 기본 20건 권한 matrix를 조회한다", async () => {
    render(<PrivacyPermissionManagementPage />);

    expect(
      await screen.findByText("researcher_registration_no"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("privacy-permission-page-size-select"),
    ).toHaveValue("20");
    expect(
      privacyPermissionApi.listPrivacyAccessPermissions,
    ).toHaveBeenCalledWith({
      roleCode: "R09",
      fieldKey: "",
      page: 0,
      size: 20,
    });
  });

  it("행 선택 후 독립 권한 플래그를 저장하고 성공 재조회를 수행한다", async () => {
    render(<PrivacyPermissionManagementPage />);
    fireEvent.click(await screen.findByTestId("privacy-permission-row-1"));
    fireEvent.change(screen.getByTestId("privacy-permission-masked-select"), {
      target: { value: "N" },
    });
    fireEvent.change(
      screen.getByTestId("privacy-permission-change-reason-input"),
      {
        target: { value: "원문과 출력만 허용" },
      },
    );
    fireEvent.click(screen.getByTestId("privacy-permission-save-button"));

    await waitFor(() =>
      expect(
        privacyPermissionApi.savePrivacyAccessPermissions,
      ).toHaveBeenCalledWith([
        expect.objectContaining({
          roleCode: "R09",
          fieldKey: "researcher_registration_no",
          rawViewAllowedYn: "Y",
          maskedViewAllowedYn: "N",
          exportAllowedYn: "Y",
          accountViewAllowedYn: "Y",
          changeReason: "원문과 출력만 허용",
        }),
      ]),
    );
    await waitFor(() =>
      expect(
        privacyPermissionApi.listPrivacyAccessPermissions,
      ).toHaveBeenCalledTimes(2),
    );
    expect(await screen.findByText("저장되었습니다")).toBeInTheDocument();
  });

  it("판정 확인 영역은 미설정 차단 결과를 표시하고 원문값을 표시하지 않는다", async () => {
    render(<PrivacyPermissionManagementPage />);
    await screen.findByText("researcher_registration_no");
    fireEvent.change(
      screen.getByTestId("privacy-permission-evaluate-field-key-input"),
      {
        target: { value: "researcher_registration_no" },
      },
    );
    fireEvent.change(
      screen.getByTestId("privacy-permission-evaluate-purpose-input"),
      {
        target: { value: "업무 확인" },
      },
    );
    fireEvent.click(screen.getByTestId("privacy-permission-evaluate-button"));

    expect(
      await screen.findByTestId("privacy-permission-evaluate-result"),
    ).toHaveTextContent("차단");
    expect(screen.queryByText("rawValue")).not.toBeInTheDocument();
    expect(screen.queryByText("plainValue")).not.toBeInTheDocument();
  });
});

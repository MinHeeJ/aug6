import { act, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { I18nProvider, i18n } from "../../i18n";
import {
  createEmptyUserManagementState,
  getUserManagementRouteContract,
  reduceUserManagementState,
  UserManagementPage,
  userManagementApi,
} from "./SCR-USER-MGMT";

afterEach(() => {
  vi.restoreAllMocks();
  window.sessionStorage.clear();
  void i18n.changeLanguage("ko");
});

describe("SCR-USER-MGMT i18n rendering", () => {
  it("/admin/users 정적 label은 i18next resource key로 한국어와 영어를 전환하고 DB 조회값은 원문을 유지한다", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          success: true,
          data: {
            users: [
              {
                userId: 7,
                loginId: "db-user",
                employeeNo: "E1007",
                name: "홍길동",
                organizationName: "사범대학",
                rankName: "교수",
                employmentStatus: "ACTIVE",
                positionName: "학과장",
                systemUseYn: "Y",
                status: "ACTIVE",
                roleCodes: ["R09"],
              },
            ],
            availableRoles: [{ roleCode: "R09", roleName: "시스템관리자" }],
            page: 0,
            size: 20,
            totalElements: 1,
          },
          meta: {},
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    render(
      <I18nProvider>
        <UserManagementPage />
      </I18nProvider>,
    );

    expect(
      screen.getByRole("heading", { name: "사용자 관리" }),
    ).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("홍길동")).toBeInTheDocument());

    await act(async () => {
      await i18n.changeLanguage("en");
    });

    expect(
      screen.getByRole("heading", { name: "User Management" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Search Criteria")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Search/ })).toBeInTheDocument();
    expect(screen.queryByText("Hong Gil-dong")).not.toBeInTheDocument();
    expect(screen.getByText("홍길동")).toBeInTheDocument();
  });
});

describe("SCR-USER-MGMT route contract and state handling", () => {
  it("declares the required route, screen id, Korean labels and relative API operations", () => {
    expect(getUserManagementRouteContract()).toEqual({
      route: "/admin/users",
      screenId: "SCR-USER-MGMT",
      operations: ["searchUsers", "updateUserAccount", "updateUserRoles"],
    });
    expect(userManagementApi.paths.search({ name: "홍길동" })).toBe(
      "/api/admin/users?name=%ED%99%8D%EA%B8%B8%EB%8F%99",
    );
    expect(userManagementApi.paths.updateAccount(2)).toBe(
      "/api/admin/users/2/account",
    );
    expect(userManagementApi.paths.updateRoles(2)).toBe(
      "/api/admin/users/2/roles",
    );
  });

  it("represents loading, empty, error, permission and success states for the user screen", () => {
    const loading = reduceUserManagementState(
      createEmptyUserManagementState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceUserManagementState(loading, {
      type: "loaded",
      users: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceUserManagementState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceUserManagementState(error, { type: "permission" });
    expect(permission.status).toBe("permission");
    const success = reduceUserManagementState(permission, {
      type: "success",
      message: "사용자 정보가 저장되었습니다.",
    });
    expect(success.status).toBe("success");
    expect(success.message).toBe("사용자 정보가 저장되었습니다.");
  });
});

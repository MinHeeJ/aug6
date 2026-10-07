import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  authApi,
  type CurrentUser,
} from "../../api/apiClient";
import { AuthProvider } from "../../app/AuthProvider";
import { AppRouter } from "../../app/router";
import {
  CourseOperationManagementPage,
  type CourseOperation,
} from "./SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    apiRequest: vi.fn(),
    authApi: { ...actual.authApi, me: vi.fn() },
  };
});
const row: CourseOperation = {
  achievementId: 81,
  managementNo: "CO-81",
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2026",
  managementItemCode: "course-operations",
  achievementDate: "2026-04-10",
  performanceDetails: "개설 내역",
  achievementStatus: "DRAFT",
  attachmentIds: [],
};
const user: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: ["R01"],
  menus: [
    {
      menuId: 990,
      menuName: "강좌 개설·운영",
      displayOrder: 1,
      children: [],
      url: "/faculty/education/course-operations",
      screenId: "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT",
    },
  ],
};
const response = (data: unknown) => ({ success: true, data, meta: {} });

function wire(current = row) {
  vi.mocked(apiRequest).mockImplementation(async (path, init) => {
    if (init?.method)
      return response({
        achievement: { ...current, performanceDetails: "수정 내역" },
        occurredDateWarning: true,
        warningMessage: "평가대상 기간 밖입니다.",
      }) as never;
    if (path.endsWith("/81")) return response(current) as never;
    return response({
      achievements: [current],
      totalElements: 1,
      managementItems: ["course-operations"],
    }) as never;
  });
}

describe("course operations", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    window.history.replaceState({}, "", "/");
  });

  it("loads real list, selects detail, sends PUT to selected ID and displays warning", async () => {
    wire();
    render(<CourseOperationManagementPage user={user} />);
    await screen.findByText("CO-81");
    fireEvent.click(screen.getByTestId("course-detail-81"));
    await waitFor(() =>
      expect(screen.getByTestId("course-performance-details")).toHaveValue(
        "개설 내역",
      ),
    );
    fireEvent.change(screen.getByTestId("course-performance-details"), {
      target: { value: "수정 내역" },
    });
    fireEvent.submit(screen.getByTestId("course-detail-panel"));
    await screen.findByText("저장되었습니다. 평가대상 기간 밖입니다.");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/course-operations/81");
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({
      managementItemCode: "course-operations",
      achievementDate: "2026-04-10",
      performanceDetails: "수정 내역",
      attachmentIds: [],
    });
  });

  it("creates with POST and does not submit server-controlled identity fields", async () => {
    wire();
    render(<CourseOperationManagementPage user={user} />);
    await screen.findByText("CO-81");
    fireEvent.change(screen.getByTestId("course-achievement-date"), {
      target: { value: "2026-04-10" },
    });
    fireEvent.change(screen.getByTestId("course-management-item"), {
      target: { value: "course-operations" },
    });
    fireEvent.change(screen.getByTestId("course-performance-details"), {
      target: { value: "등록 내역" },
    });
    fireEvent.submit(screen.getByTestId("course-detail-panel"));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/course-operations",
        expect.objectContaining({ method: "POST" }),
      ),
    );
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "POST");
    expect(JSON.parse(String(call?.[1]?.body))).not.toHaveProperty(
      "achievementId",
    );
    expect(JSON.parse(String(call?.[1]?.body))).not.toHaveProperty(
      "evaluationYear",
    );
  });

  it("confirmed detail is readonly and cannot be saved", async () => {
    wire({ ...row, achievementStatus: "EVALUATION_CONFIRMED" });
    render(<CourseOperationManagementPage user={user} />);
    await screen.findByText("CO-81");
    fireEvent.click(screen.getByTestId("course-detail-81"));
    await screen.findByText("현재 상태 또는 소유권으로 수정할 수 없습니다.");
    expect(screen.getByTestId("course-save-button")).toBeDisabled();
    expect(screen.getByTestId("course-performance-details")).toBeDisabled();
  });

  it("shows loading, empty, API error, and permission states without fabricated rows", async () => {
    vi.mocked(apiRequest).mockResolvedValue(
      response({
        achievements: [],
        managementItems: [],
        totalElements: 0,
      }) as never,
    );
    const view = render(<CourseOperationManagementPage user={user} />);
    expect(screen.getByText("불러오는 중")).toBeInTheDocument();
    await screen.findByText("데이터 없음");
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(500, "조회 실패"),
    );
    fireEvent.click(screen.getByTestId("course-search-button"));
    await screen.findByText("조회 실패");
    view.unmount();
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "접근 불가"),
    );
    render(<CourseOperationManagementPage user={user} />);
    await screen.findByText("권한 없음");
  });

  it("server field validation is displayed and cancelled confirmation sends no mutation", async () => {
    wire();
    render(<CourseOperationManagementPage user={user} />);
    await screen.findByText("CO-81");
    vi.mocked(window.confirm).mockReturnValueOnce(false);
    fireEvent.submit(screen.getByTestId("course-detail-panel"));
    expect(
      vi.mocked(apiRequest).mock.calls.some(([, init]) => init?.method),
    ).toBe(false);
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(400, "입력 오류", {
        code: "VALIDATION_ERROR",
        message: "입력 오류",
        fields: [{ field: "performanceDetails", message: "실적내역 필수" }],
      }),
    );
    fireEvent.submit(screen.getByTestId("course-detail-panel"));
    await screen.findByText("실적내역 필수");
  });

  it("authorized database-menu entry reaches the registered screen through the existing router", async () => {
    wire();
    vi.mocked(authApi.me).mockResolvedValue(response(user) as never);
    render(
      <AuthProvider>
        <AppRouter />
      </AuthProvider>,
    );
    fireEvent.click(await screen.findByRole("button", { name: "모바일 메뉴" }));
    const link = await screen.findByRole("link", { name: "강좌 개설·운영" });
    fireEvent.click(link);
    await screen.findByTestId("course-operations-screen");
    expect(window.location.pathname).toBe(
      "/faculty/education/course-operations",
    );
    await screen.findByText("CO-81");
  });
});

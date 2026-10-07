import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { AppRouter } from "../../app/router";
import { CourseOperationsPage } from "./SCR-COURSE-OPERATIONS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({
    status: "authenticated",
    user,
    error: null,
    logout: vi.fn(),
  }),
}));
const path = "/faculty/course-offering-operation-achievements";
const user: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  employeeNo: "E101",
  name: "교원",
  roles: ["R01"],
  menus: [
    {
      menuId: 800,
      menuName: "강좌 실적",
      displayOrder: 1,
      url: path,
      children: [],
    },
  ],
};
const row = {
  achievementId: 82,
  managementNo: "CO-fixture",
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2026",
  managementItemCode: "ATTENDANCE",
  achievementDate: "2026-04-10",
  performanceDetails: "기존 내역",
  achievementStatus: "DRAFT",
  attachmentIds: [],
};
const list = {
  success: true,
  data: { achievements: [row], page: 0, pageSize: 20, totalElements: 1 },
  meta: {},
};

describe("course operations", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    window.history.replaceState({}, "", path);
  });
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/");
  });

  it("uses the real router and server-provided menu to reach the screen", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list);
    window.history.replaceState({}, "", "/");
    render(<AppRouter />);
    fireEvent.click(
      screen.getByRole("button", { name: "모바일 메뉴", exact: true }),
    );
    fireEvent.click(screen.getByRole("link", { name: "강좌 실적" }));
    expect(
      await screen.findByRole("heading", { name: "강좌 개설·운영 실적 관리" }),
    ).toBeInTheDocument();
    expect(await screen.findByText("CO-fixture")).toBeInTheDocument();
    expect(window.location.pathname).toBe(path);
  });

  it("loads detail from the selected id and updates through PUT without payload identity", async () => {
    vi.mocked(apiRequest).mockImplementation(async (url, init) => {
      if (init?.method === "PUT") {
        return {
          success: true,
          data: {
            achievement: { ...row, performanceDetails: "변경 내역" },
            occurredDateWarning: true,
            warningMessage: "발생일 경고",
          },
          meta: {},
        };
      }
      return url.endsWith("/82")
        ? { success: true, data: row, meta: {} }
        : list;
    });
    render(<CourseOperationsPage user={user} />);
    await screen.findByText("CO-fixture");
    fireEvent.click(screen.getByTestId("course-detail-82"));
    await waitFor(() =>
      expect(screen.getByTestId("course-performance-details")).toHaveValue(
        "기존 내역",
      ),
    );
    fireEvent.change(screen.getByTestId("course-performance-details"), {
      target: { value: "변경 내역" },
    });
    fireEvent.click(screen.getByTestId("course-save"));
    expect(await screen.findByText("발생일 경고")).toBeInTheDocument();
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/course-operations/82");
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({
      managementItemCode: "ATTENDANCE",
      achievementDate: "2026-04-10",
      performanceDetails: "변경 내역",
      attachmentIds: [],
    });
  });

  it("creates new rows through POST and blocks missing required fields", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list);
    render(<CourseOperationsPage user={user} />);
    await screen.findByText("CO-fixture");
    fireEvent.click(screen.getByTestId("course-save"));
    expect(screen.getByText("실적내역을 입력하세요.")).toBeInTheDocument();
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([, init]) => init?.method === "POST"),
    ).toBe(false);
    fireEvent.change(screen.getByTestId("course-management-item"), {
      target: { value: "ATTENDANCE" },
    });
    fireEvent.change(screen.getByTestId("course-date"), {
      target: { value: "2026-04-10" },
    });
    fireEvent.change(screen.getByTestId("course-performance-details"), {
      target: { value: "신규 내역" },
    });
    vi.mocked(apiRequest).mockResolvedValueOnce({
      success: true,
      data: { achievement: row, occurredDateWarning: false },
      meta: {},
    });
    fireEvent.click(screen.getByTestId("course-save"));
    expect(await screen.findByText("저장되었습니다.")).toBeInTheDocument();
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/course-operations",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("locks finalized detail and makes reviewer-only users read-only", async () => {
    vi.mocked(apiRequest).mockImplementation(async (url) =>
      url.endsWith("/82")
        ? {
            success: true,
            data: { ...row, achievementStatus: "EVALUATION_CONFIRMED" },
            meta: {},
          }
        : list,
    );
    render(<CourseOperationsPage user={user} />);
    await screen.findByText("CO-fixture");
    fireEvent.click(screen.getByTestId("course-detail-82"));
    await screen.findByText("확정·제출 실적은 수정할 수 없습니다.");
    expect(screen.getByTestId("course-save")).toBeDisabled();
    expect(screen.getByTestId("course-performance-details")).toBeDisabled();
  });

  it("shows loading then empty, API failure and permission-denied states", async () => {
    let complete: (value: typeof list) => void = () => undefined;
    vi.mocked(apiRequest).mockReturnValue(
      new Promise((resolve) => {
        complete = resolve;
      }),
    );
    render(<CourseOperationsPage user={user} />);
    expect(screen.getByText("불러오는 중")).toBeInTheDocument();
    complete({
      ...list,
      data: { ...list.data, achievements: [], totalElements: 0 },
    });
    expect(await screen.findByText("데이터 없음")).toBeInTheDocument();
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(500, "조회 실패"),
    );
    fireEvent.click(screen.getByTestId("course-search"));
    expect(await screen.findByText("조회 실패")).toBeInTheDocument();
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(403, "권한 없음"),
    );
    fireEvent.click(screen.getByTestId("course-search"));
    expect(
      await screen.findByText(
        "강좌 개설·운영 실적에 접근할 권한 또는 데이터 범위가 없습니다.",
      ),
    ).toBeInTheDocument();
  });
});

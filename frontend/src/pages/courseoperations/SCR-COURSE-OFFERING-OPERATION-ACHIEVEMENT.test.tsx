import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { useAuth } from "../../app/AuthProvider";
import { AppRouter } from "../../app/router";
import { CourseOperationManagementPage } from "./SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
vi.mock("../../app/AuthProvider", () => ({ useAuth: vi.fn() }));

const row = {
  achievementId: 82,
  managementNo: "CO-001",
  teacherUserId: 101,
  teacherName: "교원",
  organizationCode: "KNUE-DEPT-COMP",
  evaluationYear: "2026",
  managementItemCode: "COURSE_OPERATION",
  achievementDate: "2026-04-10",
  performanceDetails: "목록 내용",
  achievementStatus: "DRAFT",
  attachmentIds: [],
};
const list = {
  achievements: [row],
  page: 0,
  pageSize: 20,
  totalElements: 1,
  managementItems: [
    { managementItemCode: "COURSE_OPERATION", managementItemName: "강좌 운영" },
  ],
};
let roles = ["R01"];
const ok = (data: unknown) => ({ success: true, data, meta: {} });

function fill() {
  fireEvent.change(screen.getByTestId("course-management-item"), {
    target: { value: "COURSE_OPERATION" },
  });
  fireEvent.change(screen.getByTestId("course-achievement-date"), {
    target: { value: "2026-04-10" },
  });
  fireEvent.change(screen.getByTestId("course-performance-details"), {
    target: { value: "입력 내용" },
  });
}

describe("강좌 개설·운영 실적", () => {
  beforeEach(() => {
    roles = ["R01"];
    vi.mocked(apiRequest).mockReset();
    vi.mocked(apiRequest).mockImplementation(
      async (path) =>
        (path.includes("?")
          ? ok(list)
          : ok({ ...row, performanceDetails: "상세 API 내용" })) as never,
    );
    vi.mocked(useAuth).mockImplementation(() => ({
      status: "authenticated",
      error: null,
      user: {
        userId: 101,
        loginId: "faculty",
        employeeNo: "E0101",
        name: "교원",
        roles,
        menus: [
          {
            menuId: 1,
            menuName: "강좌 개설·운영 실적 관리",
            menuType: "SCREEN",
            url: "/faculty/education/course-operations",
            screenId: "SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT",
            children: [],
          },
        ],
      } as CurrentUser,
      login: vi.fn(),
      logout: vi.fn(),
      refresh: vi.fn(),
    }));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    window.history.replaceState({}, "", "/faculty/education/course-operations");
  });

  it("authorized navigation entry renders the real course screen in the existing shell", async () => {
    render(<AppRouter />);
    expect(
      await screen.findByTestId("course-operation-page"),
    ).toBeInTheDocument();
    await screen.findByText("CO-001");
    expect(
      screen.getAllByText("강좌 개설·운영 실적 관리").length,
    ).toBeGreaterThan(0);
  });

  it("loads details from the selected id rather than copying list data", async () => {
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    fireEvent.click(screen.getByTestId("course-detail-82"));
    expect(
      await screen.findByDisplayValue("상세 API 내용"),
    ).toBeInTheDocument();
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/course-operations/82",
    );
    expect(screen.getByTestId("course-evaluation-year")).toBeDisabled();
  });

  it("create confirms, sends approved fields, then reloads detail and list", async () => {
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (init?.method === "POST")
        return ok({ achievement: row, occurredDateWarning: false }) as never;
      return (
        path.includes("?")
          ? ok(list)
          : ok({ ...row, performanceDetails: "입력 내용" })
      ) as never;
    });
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    fill();
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("저장되었습니다.");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "POST");
    expect(call?.[0]).toBe("/api/business/course-operations");
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({
      managementItemCode: "COURSE_OPERATION",
      achievementDate: "2026-04-10",
      performanceDetails: "입력 내용",
      attachmentIds: [],
    });
    expect(window.confirm).toHaveBeenCalled();
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/course-operations/82",
    );
  });

  it("update uses the selected path and excludes immutable year and owner from payload", async () => {
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (init?.method === "PUT") {
        return ok({
          achievement: row,
          occurredDateWarning: true,
          warningMessage: "발생일 경고",
        }) as never;
      }
      return (path.includes("?") ? ok(list) : ok(row)) as never;
    });
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    fireEvent.click(screen.getByTestId("course-detail-82"));
    await screen.findByDisplayValue("목록 내용");
    fireEvent.change(screen.getByTestId("course-performance-details"), {
      target: { value: "수정 내역" },
    });
    fireEvent.click(screen.getByTestId("course-save"));
    await screen.findByText("발생일 경고");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/course-operations/82");
    const body = JSON.parse(String(call?.[1]?.body));
    expect(body.performanceDetails).toBe("수정 내역");
    expect(body).not.toHaveProperty("evaluationYear");
    expect(body).not.toHaveProperty("teacherUserId");
    expect(body).not.toHaveProperty("achievementId");
  });

  it("cancelled confirmation performs no mutation", async () => {
    vi.mocked(window.confirm).mockReturnValue(false);
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    fill();
    fireEvent.click(screen.getByTestId("course-save"));
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([, init]) => init?.method === "POST"),
    ).toBe(false);
  });

  it("missing performance details blocks save and displays a field error", async () => {
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    fireEvent.click(screen.getByTestId("course-save"));
    expect(
      await screen.findByText("실적내역을 입력하세요."),
    ).toBeInTheDocument();
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([, init]) => init?.method === "POST"),
    ).toBe(false);
  });

  it("confirmed row disables detail and attachment changes", async () => {
    vi.mocked(apiRequest).mockImplementation(
      async (path) =>
        (path.includes("?")
          ? ok(list)
          : ok({ ...row, achievementStatus: "EVALUATION_CONFIRMED" })) as never,
    );
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    fireEvent.click(screen.getByTestId("course-detail-82"));
    await screen.findByTestId("course-lock");
    expect(screen.getByTestId("course-performance-details")).toBeDisabled();
    expect(screen.getByTestId("course-save")).toBeDisabled();
    expect(screen.queryByTestId("course-submit")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("course-attachment-tab"));
    expect(screen.getByTestId("course-attachments")).toBeDisabled();
  });

  it("submitted row is read-only", async () => {
    vi.mocked(apiRequest).mockImplementation(
      async (path) =>
        (path.includes("?")
          ? ok(list)
          : ok({ ...row, achievementStatus: "SUBMITTED" })) as never,
    );
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    fireEvent.click(screen.getByTestId("course-detail-82"));
    await screen.findByTestId("course-lock");
    expect(screen.getByTestId("course-save")).toBeDisabled();
  });

  it("another owner's row remains read-only for R01 even if R02 permits the read", async () => {
    roles = ["R01", "R02"];
    vi.mocked(apiRequest).mockImplementation(
      async (path) =>
        (path.includes("?")
          ? ok(list)
          : ok({ ...row, teacherUserId: 102 })) as never,
    );
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    fireEvent.click(screen.getByTestId("course-detail-82"));
    await screen.findByText("조회 전용입니다.");
    expect(screen.getByTestId("course-save")).toBeDisabled();
  });

  it("R02 sees read-only fields and no mutation controls", async () => {
    roles = ["R02"];
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    expect(screen.queryByTestId("course-save")).not.toBeInTheDocument();
    expect(screen.queryByTestId("course-new")).not.toBeInTheDocument();
    expect(screen.getByTestId("course-performance-details")).toBeDisabled();
  });

  it("R09 override retains mutation controls", async () => {
    roles = ["R09"];
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    expect(screen.getByTestId("course-save")).not.toBeDisabled();
  });

  it("R07 cannot enter the screen or issue list requests", async () => {
    roles = ["R07"];
    render(<CourseOperationManagementPage />);
    expect(
      await screen.findByText("강좌 운영 실적 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("empty list and absent options are rendered without invented choices", async () => {
    vi.mocked(apiRequest).mockResolvedValue(
      ok({
        ...list,
        achievements: [],
        managementItems: [],
        totalElements: 0,
      }) as never,
    );
    render(<CourseOperationManagementPage />);
    expect(await screen.findByText("실적이 없습니다")).toBeInTheDocument();
    expect(
      screen.getByTestId("course-management-item").querySelectorAll("option"),
    ).toHaveLength(1);
  });

  it("loading remains visible until the API resolves", async () => {
    vi.mocked(apiRequest).mockReturnValue(new Promise(() => {}));
    render(<CourseOperationManagementPage />);
    expect(await screen.findByText("실적 조회 중")).toBeInTheDocument();
    expect(screen.getByTestId("course-save")).toBeDisabled();
  });

  it("failed request displays the existing error state", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new Error("failure"));
    render(<CourseOperationManagementPage />);
    expect(await screen.findByText("실적 처리 오류")).toBeInTheDocument();
  });

  it("403 displays permission state", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new ApiClientError(403, "범위 밖"));
    render(<CourseOperationManagementPage />);
    expect(
      await screen.findByText("강좌 운영 실적 권한이 없습니다"),
    ).toBeInTheDocument();
  });

  it("server validation errors map the array field name to its form control", async () => {
    vi.mocked(apiRequest).mockImplementation(async (_path, init) => {
      if (init?.method === "POST")
        throw new ApiClientError(400, "입력 오류", {
          code: "VALIDATION_ERROR",
          message: "입력 오류",
          fields: [{ field: "performanceDetails", message: "서버 필수 검증" }],
        });
      return ok(list) as never;
    });
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    fill();
    fireEvent.click(screen.getByTestId("course-save"));
    expect(await screen.findByText("서버 필수 검증")).toBeInTheDocument();
  });

  it("409 period error preserves unsaved form contents", async () => {
    vi.mocked(apiRequest).mockImplementation(async (_path, init) => {
      if (init?.method === "POST")
        throw new ApiClientError(409, "활성 입력기간이 아닙니다.");
      return ok(list) as never;
    });
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    fill();
    fireEvent.click(screen.getByTestId("course-save"));
    expect(
      await screen.findByText("활성 입력기간이 아닙니다."),
    ).toBeInTheDocument();
    expect(screen.getByTestId("course-performance-details")).toHaveValue(
      "입력 내용",
    );
  });

  it("page size changes use the approved size and reset to the first page", async () => {
    render(<CourseOperationManagementPage />);
    await screen.findByText("CO-001");
    fireEvent.change(screen.getByTestId("course-page-size"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        expect.stringContaining("page=0&pageSize=50"),
      ),
    );
  });
});

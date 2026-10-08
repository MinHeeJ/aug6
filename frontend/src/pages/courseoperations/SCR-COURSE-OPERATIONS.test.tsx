import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { CourseOperationsPage } from "./SCR-COURSE-OPERATIONS";
import { AppRouter } from "../../app/router";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const { auth } = vi.hoisted(() => ({
  auth: { status: "authenticated", user: null as CurrentUser | null },
}));
vi.mock("../../app/AuthProvider", () => ({ useAuth: () => auth }));
const teacher: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: ["R01"],
  menus: [],
};
const row = {
  achievementId: 12,
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2026",
  managementItemCode: "COURSE_OPERATION",
  achievementDate: "2026-04-10",
  performanceDetails: "원본 실적",
  achievementStatus: "DRAFT",
  attachmentIds: ["opaque-attachment"],
};
const response = (data: unknown) => ({ success: true, data, meta: {} });
const list = (achievements = [row]) =>
  response({ achievements, totalElements: achievements.length });

function fill() {
  fireEvent.change(screen.getByTestId("course-managementItemCode"), {
    target: { value: "COURSE_OPERATION" },
  });
  fireEvent.change(screen.getByTestId("course-achievementDate"), {
    target: { value: "2026-04-11" },
  });
  fireEvent.change(screen.getByTestId("course-performance-details"), {
    target: { value: "새 실적" },
  });
}

describe("SCR-COURSE-OPERATIONS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    auth.user = teacher;
    window.history.replaceState({}, "", "/faculty/education/course-operations");
  });

  it("loads the intended authorized route inside the existing shell", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list());
    render(<AppRouter />);
    expect(await screen.findByText("원본 실적")).toBeInTheDocument();
    expect(screen.getByTestId("course-operations-page")).toBeInTheDocument();
  });

  it("does not call individual APIs for R07 and renders permission state", () => {
    render(<CourseOperationsPage user={{ ...teacher, roles: ["R07"] }} />);
    expect(
      screen.getByText("강좌 개설·운영 실적 조회 권한이 없습니다."),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("shows loading and empty states", async () => {
    let resolve!: (value: ReturnType<typeof list>) => void;
    vi.mocked(apiRequest).mockImplementation(
      () =>
        new Promise((done) => {
          resolve = done;
        }) as never,
    );
    render(<CourseOperationsPage user={teacher} />);
    expect(screen.getByText("불러오는 중")).toBeInTheDocument();
    resolve(list([]));
    expect(await screen.findByText("데이터 없음")).toBeInTheDocument();
  });

  it("shows request failure and permits retry", async () => {
    vi.mocked(apiRequest)
      .mockRejectedValueOnce(new Error("목록 실패"))
      .mockResolvedValue(list());
    render(<CourseOperationsPage user={teacher} />);
    expect(await screen.findByText("목록 실패")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("course-search-button"));
    expect(await screen.findByText("원본 실적")).toBeInTheDocument();
  });

  it("shows server permission denial", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "범위 없음"),
    );
    render(<CourseOperationsPage user={teacher} />);
    expect(
      await screen.findByText("강좌 개설·운영 실적 조회 권한이 없습니다."),
    ).toBeInTheDocument();
  });

  it("blocks missing required fields before confirmation or API write", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list([]));
    render(<CourseOperationsPage user={teacher} />);
    await screen.findByText("데이터 없음");
    fireEvent.click(screen.getByTestId("course-save-button"));
    expect(screen.getByText("실적내역을 입력하세요.")).toBeInTheDocument();
    expect(window.confirm).not.toHaveBeenCalled();
    expect(apiRequest).toHaveBeenCalledTimes(1);
  });

  it("creates only via POST and refreshes after success", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list([]))
      .mockResolvedValueOnce(
        response({ achievement: row, occurredDateWarning: false }),
      )
      .mockResolvedValueOnce(list());
    render(<CourseOperationsPage user={teacher} />);
    await screen.findByText("데이터 없음");
    fill();
    fireEvent.click(screen.getByTestId("course-save-button"));
    expect(await screen.findByText("저장되었습니다.")).toBeInTheDocument();
    expect(apiRequest).toHaveBeenNthCalledWith(
      2,
      "/api/business/course-operations",
      {
        method: "POST",
        body: JSON.stringify({
          managementItemCode: "COURSE_OPERATION",
          achievementDate: "2026-04-11",
          performanceDetails: "새 실적",
          attachmentIds: [],
        }),
      },
    );
    await screen.findByTestId("course-row-12");
  });

  it("cancellation does not mutate", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list([]));
    vi.mocked(window.confirm).mockReturnValue(false);
    render(<CourseOperationsPage user={teacher} />);
    await screen.findByText("데이터 없음");
    fill();
    fireEvent.click(screen.getByTestId("course-save-button"));
    expect(apiRequest).toHaveBeenCalledTimes(1);
  });

  it("fetches selected detail and uses PUT without mutable year or body ID", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list())
      .mockResolvedValueOnce(response(row))
      .mockResolvedValueOnce(
        response({
          achievement: { ...row, performanceDetails: "수정" },
          occurredDateWarning: true,
          warningMessage: "평가대상 기간 밖 경고",
        }),
      )
      .mockResolvedValueOnce(list());
    render(<CourseOperationsPage user={teacher} />);
    await screen.findByText("원본 실적");
    fireEvent.click(screen.getByTestId("course-detail-12"));
    await waitFor(() =>
      expect(screen.getByTestId("course-performance-details")).toHaveValue(
        "원본 실적",
      ),
    );
    expect(screen.getByTestId("course-evaluationYear")).toBeDisabled();
    fireEvent.change(screen.getByTestId("course-performance-details"), {
      target: { value: "수정" },
    });
    fireEvent.click(screen.getByTestId("course-save-button"));
    expect(
      await screen.findByText("평가대상 기간 밖 경고"),
    ).toBeInTheDocument();
    const call = vi.mocked(apiRequest).mock.calls[2];
    expect(call[0]).toBe("/api/business/course-operations/12");
    expect(call[1]?.method).toBe("PUT");
    expect(JSON.parse(String(call[1]?.body))).toEqual({
      managementItemCode: "COURSE_OPERATION",
      achievementDate: "2026-04-10",
      performanceDetails: "수정",
      attachmentIds: ["opaque-attachment"],
    });
  });

  it.each(["EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED"])(
    "locks %s rows",
    async (status) => {
      vi.mocked(apiRequest)
        .mockResolvedValueOnce(list())
        .mockResolvedValueOnce(response({ ...row, achievementStatus: status }));
      render(<CourseOperationsPage user={teacher} />);
      await screen.findByText("원본 실적");
      fireEvent.click(screen.getByTestId("course-detail-12"));
      await screen.findByTestId("course-lock-message");
      expect(screen.getByTestId("course-save-button")).toBeDisabled();
      expect(screen.getByTestId("course-performance-details")).toBeDisabled();
    },
  );

  it("hides mutation controls for department readers", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list());
    render(<CourseOperationsPage user={{ ...teacher, roles: ["R02"] }} />);
    await screen.findByText("원본 실적");
    expect(screen.queryByTestId("course-save-button")).not.toBeInTheDocument();
    expect(screen.queryByTestId("course-new-button")).not.toBeInTheDocument();
  });

  it("locks another teacher's selected row", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list())
      .mockResolvedValueOnce(response({ ...row, teacherUserId: 102 }));
    render(
      <CourseOperationsPage user={{ ...teacher, roles: ["R01", "R02"] }} />,
    );
    await screen.findByText("원본 실적");
    fireEvent.click(screen.getByTestId("course-detail-12"));
    await screen.findByText("타인의 실적은 읽기 전용입니다.");
    expect(screen.getByTestId("course-save-button")).toBeDisabled();
  });

  it("renders server field validation and retains attempted input", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list([]))
      .mockRejectedValueOnce(
        new ApiClientError(400, "입력 오류", {
          code: "VALIDATION_ERROR",
          message: "입력 오류",
          fields: [{ field: "performanceDetails", message: "실적내역 오류" }],
        }),
      );
    render(<CourseOperationsPage user={teacher} />);
    await screen.findByText("데이터 없음");
    fill();
    fireEvent.click(screen.getByTestId("course-save-button"));
    expect(await screen.findByText("실적내역 오류")).toBeInTheDocument();
    expect(screen.getByTestId("course-performance-details")).toHaveValue(
      "새 실적",
    );
  });

  it("renders period conflict and retains input", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list([]))
      .mockRejectedValueOnce(new ApiClientError(409, "PERIOD_NOT_ACTIVE"));
    render(<CourseOperationsPage user={teacher} />);
    await screen.findByText("데이터 없음");
    fill();
    fireEvent.click(screen.getByTestId("course-save-button"));
    expect(await screen.findByText("PERIOD_NOT_ACTIVE")).toBeInTheDocument();
    expect(screen.getByTestId("course-performance-details")).toHaveValue(
      "새 실적",
    );
  });

  it("allows R09 direct route and mutation controls", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list());
    render(<CourseOperationsPage user={{ ...teacher, roles: ["R09"] }} />);
    await screen.findByText("원본 실적");
    expect(screen.getByTestId("course-save-button")).toBeEnabled();
  });

  it("sends search and page-size filters to the same list", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list([]));
    render(<CourseOperationsPage user={teacher} />);
    await screen.findByText("데이터 없음");
    fireEvent.change(screen.getByTestId("course-filter-input"), {
      target: { value: "COURSE_OPERATION" },
    });
    fireEvent.click(screen.getByTestId("course-search-button"));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenLastCalledWith(
        "/api/business/course-operations?page=0&pageSize=20&managementItemCode=COURSE_OPERATION",
      ),
    );
    fireEvent.change(screen.getByTestId("course-page-size"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(apiRequest).toHaveBeenLastCalledWith(
        "/api/business/course-operations?page=0&pageSize=50&managementItemCode=COURSE_OPERATION",
      ),
    );
  });
});

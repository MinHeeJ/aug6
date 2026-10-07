import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import type { CurrentUser } from "../../api/apiClient";
import {
  CourseOperationsPage,
  type CourseOperation,
} from "./SCR-COURSE-OPERATIONS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});

const row: CourseOperation = {
  achievementId: 82,
  managementNo: "CO-TEST",
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2026",
  managementItemCode: "COURSE_OPERATION",
  achievementDate: "2026-04-10",
  performanceDetails: "기존 실적내역",
  achievementStatus: "DRAFT",
  attachmentIds: [],
};
const items = [
  {
    managementItemCode: "COURSE_OPERATION",
    managementItemName: "강좌 운영",
    evaluationYear: "2026",
    requiredYn: "Y",
    dataType: "TEXT",
    teacherEditableYn: "Y",
  },
];
function actor(roles = ["R01"]): CurrentUser {
  return { userId: 101, loginId: "faculty", name: "교원", roles, menus: [] };
}
function lists(rows = [row]) {
  return {
    success: true,
    data: {
      achievements: rows,
      totalElements: rows.length,
      managementItems: items,
    },
    meta: {},
  };
}
function setup(selected = row) {
  vi.mocked(apiRequest).mockImplementation(async (url) => {
    if (url.endsWith("/82")) return { success: true, data: selected, meta: {} };
    return lists([selected]);
  });
}
function fill() {
  fireEvent.change(screen.getByTestId("course-management-item"), {
    target: { value: "COURSE_OPERATION" },
  });
  fireEvent.change(screen.getByTestId("course-date"), {
    target: { value: "2026-04-10" },
  });
  fireEvent.change(screen.getByTestId("course-performance-details"), {
    target: { value: "새 내역" },
  });
}

describe("SCR-COURSE-OPERATIONS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    setup();
  });

  it("loads source-backed options and reads the selected domain ID", async () => {
    render(<CourseOperationsPage user={actor()} />);
    await screen.findByText("CO-TEST");
    fireEvent.click(screen.getByTestId("course-detail-82"));
    await waitFor(() =>
      expect(screen.getByTestId("course-performance-details")).toHaveValue(
        "기존 실적내역",
      ),
    );
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/course-operations/82",
    );
    expect(
      screen.getByRole("option", { name: "강좌 운영 (2026) · TEXT" }),
    ).toBeInTheDocument();
  });

  it("confirms create and refreshes with warning", async () => {
    vi.mocked(apiRequest).mockImplementation(async (_, init) =>
      init?.method === "POST"
        ? {
            success: true,
            data: {
              achievementId: 82,
              achievement: row,
              occurredDateWarning: true,
              warningMessage: "평가대상 기간 밖입니다.",
            },
            meta: {},
          }
        : lists(),
    );
    render(<CourseOperationsPage user={actor()} />);
    await screen.findByText("CO-TEST");
    fill();
    fireEvent.click(screen.getByTestId("course-save"));
    expect(
      screen.getByRole("dialog", { name: "저장 확인" }),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: "POST" }),
    );
    fireEvent.click(screen.getByTestId("course-confirm"));
    await screen.findByText("저장했습니다. 평가대상 기간 밖입니다.");
    expect(apiRequest).toHaveBeenCalledWith("/api/business/course-operations", {
      method: "POST",
      body: JSON.stringify({
        managementItemCode: "COURSE_OPERATION",
        achievementDate: "2026-04-10",
        performanceDetails: "새 내역",
        attachmentIds: [],
      }),
    });
  });

  it("updates by selected ID without owner/year/status payload", async () => {
    vi.mocked(apiRequest).mockImplementation(async (url, init) => {
      if (init?.method === "PUT")
        return {
          success: true,
          data: {
            achievementId: 82,
            achievement: { ...row, performanceDetails: "새 내역" },
            occurredDateWarning: false,
          },
          meta: {},
        };
      return url.endsWith("/82")
        ? { success: true, data: row, meta: {} }
        : lists();
    });
    render(<CourseOperationsPage user={actor()} />);
    await screen.findByText("CO-TEST");
    fireEvent.click(screen.getByTestId("course-detail-82"));
    await waitFor(() =>
      expect(screen.getByTestId("course-performance-details")).toHaveValue(
        "기존 실적내역",
      ),
    );
    fill();
    fireEvent.click(screen.getByTestId("course-save"));
    fireEvent.click(screen.getByTestId("course-confirm"));
    await screen.findByText("저장했습니다.");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/course-operations/82");
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({
      managementItemCode: "COURSE_OPERATION",
      achievementDate: "2026-04-10",
      performanceDetails: "새 내역",
      attachmentIds: [],
    });
  });

  it("blocks missing fields before confirmation", async () => {
    render(<CourseOperationsPage user={actor()} />);
    await screen.findByText("CO-TEST");
    fireEvent.click(screen.getByTestId("course-save"));
    expect(screen.getByText("실적내역을 입력하세요.")).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it.each(["EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED"])(
    "locks %s details and all inputs",
    async (state) => {
      setup({ ...row, achievementStatus: state });
      render(<CourseOperationsPage user={actor()} />);
      await screen.findByText("CO-TEST");
      fireEvent.click(screen.getByTestId("course-detail-82"));
      await screen.findByText("제출·확정 상태에서는 수정할 수 없습니다.");
      expect(screen.getByTestId("course-save")).toBeDisabled();
      expect(screen.getByTestId("course-performance-details")).toBeDisabled();
      expect(screen.getByTestId("course-date")).toBeDisabled();
    },
  );

  it.each(["R02", "R04"])("provides read-only %s view", async (role) => {
    render(<CourseOperationsPage user={actor([role])} />);
    await screen.findByText("CO-TEST");
    expect(screen.getByText("조회 전용 화면입니다.")).toBeInTheDocument();
    expect(screen.queryByTestId("course-save")).not.toBeInTheDocument();
    expect(screen.queryByTestId("course-new-button")).not.toBeInTheDocument();
  });

  it("never calls individual APIs as R07", () => {
    render(<CourseOperationsPage user={actor(["R07"])} />);
    expect(
      screen.getByText("강좌 운영 실적 접근 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("administrator remains admitted", async () => {
    render(<CourseOperationsPage user={actor(["R09"])} />);
    await screen.findByText("CO-TEST");
    expect(screen.getByTestId("course-save")).toBeEnabled();
  });

  it("disables other owner's record for multi-role teacher", async () => {
    setup({ ...row, teacherUserId: 202 });
    render(<CourseOperationsPage user={actor(["R01", "R02"])} />);
    await screen.findByText("CO-TEST");
    fireEvent.click(screen.getByTestId("course-detail-82"));
    await waitFor(() =>
      expect(screen.getByTestId("course-save")).toBeDisabled(),
    );
  });

  it("shows empty state", async () => {
    vi.mocked(apiRequest).mockResolvedValue(lists([]));
    render(<CourseOperationsPage user={actor()} />);
    await screen.findByText("조회된 강좌 운영 실적이 없습니다.");
  });

  it("shows loading while request is pending", () => {
    vi.mocked(apiRequest).mockReturnValue(new Promise(() => {}));
    render(<CourseOperationsPage user={actor()} />);
    expect(screen.getByText("불러오는 중")).toBeInTheDocument();
  });

  it("shows failed request without fabricated rows", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new Error("조회 실패"));
    render(<CourseOperationsPage user={actor()} />);
    await screen.findByText("조회 실패");
    expect(screen.queryByText("CO-TEST")).not.toBeInTheDocument();
  });

  it("shows permission denial on server response", async () => {
    vi.mocked(apiRequest).mockRejectedValue(new ApiClientError(403, "범위 밖"));
    render(<CourseOperationsPage user={actor()} />);
    await screen.findByText("강좌 운영 실적 접근 권한이 없습니다");
  });

  it("renders server field-object validation without discarding entered values", async () => {
    vi.mocked(apiRequest).mockImplementation(async (_, init) => {
      if (init?.method === "POST")
        throw new ApiClientError(400, "검증 실패", {
          code: "VALIDATION_ERROR",
          message: "검증 실패",
          fields: { performanceDetails: "실적내역 검증 오류" } as never,
        });
      return lists();
    });
    render(<CourseOperationsPage user={actor()} />);
    await screen.findByText("CO-TEST");
    fill();
    fireEvent.click(screen.getByTestId("course-save"));
    fireEvent.click(screen.getByTestId("course-confirm"));
    await screen.findByText("실적내역 검증 오류");
    expect(screen.getByTestId("course-performance-details")).toHaveValue(
      "새 내역",
    );
  });

  it("uses the requested page size and search filter", async () => {
    render(<CourseOperationsPage user={actor()} />);
    await screen.findByText("CO-TEST");
    fireEvent.change(screen.getByTestId("course-page-size"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/course-operations?page=0&pageSize=50",
      ),
    );
    fireEvent.change(screen.getByTestId("course-search"), {
      target: { value: "CO-TEST" },
    });
    fireEvent.click(screen.getByTestId("course-search-button"));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/course-operations?page=0&pageSize=50&managementNo=CO-TEST",
      ),
    );
  });
});

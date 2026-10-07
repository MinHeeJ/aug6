import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { CourseOperationsPage } from "./SCR-COURSE-OFFERING-OPERATION-ACHIEVEMENT";
import { AppRouter } from "../../app/router";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const owner: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: ["R01"],
  menus: [
    {
      menuId: 330,
      menuName: "강좌 개설·운영 실적 관리",
      screenId: "SCR-COURSE-OPERATIONS",
      url: "/faculty/education/course-operations",
      displayOrder: 1,
      children: [],
    },
  ],
};
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({ status: "authenticated", user: owner, logout: vi.fn() }),
}));
const row = {
  achievementId: 31,
  managementNo: "CO-fixture",
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2026",
  managementItemCode: "COURSE_OPERATION",
  achievementDate: "2026-04-10",
  performanceDetails: "강좌 운영 내역",
  achievementStatus: "DRAFT",
  attachmentRef: null,
};
const result = (data: unknown) => ({ success: true, data, meta: {} });

function mockResponses(selected = row, total = 1) {
  vi.mocked(apiRequest).mockImplementation(async (path, init) => {
    if (init?.method === "POST" || init?.method === "PUT") {
      return result({
        achievement: { ...selected, ...JSON.parse(init.body as string) },
        occurredDateWarning: true,
        warningMessage: "평가대상 기간 밖입니다.",
      }) as never;
    }
    if (path === "/api/business/course-operations/31")
      return result(selected) as never;
    return result({
      achievements: [selected],
      page: 0,
      pageSize: 20,
      totalElements: total,
    }) as never;
  });
}

async function select() {
  await screen.findByText("CO-fixture");
  fireEvent.click(screen.getByTestId("course-detail-31"));
  await waitFor(() =>
    expect(screen.getByTestId("course-performance-details")).toHaveValue(
      "강좌 운영 내역",
    ),
  );
}

function fill() {
  fireEvent.change(screen.getByTestId("course-management-item-code"), {
    target: { value: "COURSE_OPERATION" },
  });
  fireEvent.change(screen.getByTestId("course-achievement-date"), {
    target: { value: "2026-04-10" },
  });
  fireEvent.change(screen.getByTestId("course-performance-details"), {
    target: { value: "새 운영 실적" },
  });
}

describe("SCR-COURSE-OPERATIONS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    window.history.replaceState({}, "", "/");
  });

  it("reads the selected detail from its real id and updates without immutable fields", async () => {
    mockResponses();
    render(<CourseOperationsPage user={owner} />);
    await select();
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/course-operations/31",
    );
    fireEvent.change(screen.getByTestId("course-performance-details"), {
      target: { value: "수정 내역" },
    });
    fireEvent.click(screen.getByTestId("course-save-button"));
    await screen.findByText("실적이 저장되었습니다.");
    const write = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(write?.[0]).toBe("/api/business/course-operations/31");
    expect(JSON.parse(write?.[1]?.body as string)).toEqual({
      managementItemCode: "COURSE_OPERATION",
      achievementDate: "2026-04-10",
      performanceDetails: "수정 내역",
      attachmentRef: null,
    });
    expect(window.confirm).toHaveBeenCalled();
    expect(screen.getByText("평가대상 기간 밖입니다.")).toBeInTheDocument();
  });

  it("blocks missing required fields before confirmation, then creates and refreshes", async () => {
    mockResponses();
    render(<CourseOperationsPage user={owner} />);
    await screen.findByText("CO-fixture");
    fireEvent.click(screen.getByTestId("course-save-button"));
    expect(screen.getAllByText("필수 입력 항목입니다.")).toHaveLength(3);
    expect(window.confirm).not.toHaveBeenCalled();
    fill();
    fireEvent.click(screen.getByTestId("course-save-button"));
    await screen.findByText("실적이 저장되었습니다.");
    const write = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "POST");
    expect(write?.[0]).toBe("/api/business/course-operations");
    expect(JSON.parse(write?.[1]?.body as string)).not.toHaveProperty(
      "achievementId",
    );
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.filter(([path]) => path.includes("?page=")).length,
    ).toBeGreaterThan(1);
  });

  it("does not send a write when confirmation is cancelled", async () => {
    mockResponses();
    vi.mocked(window.confirm).mockReturnValue(false);
    render(<CourseOperationsPage user={owner} />);
    await screen.findByText("CO-fixture");
    fill();
    fireEvent.click(screen.getByTestId("course-save-button"));
    expect(
      vi.mocked(apiRequest).mock.calls.some(([, init]) => init?.method),
    ).toBe(false);
  });

  it("locks confirmed records and attachment changes", async () => {
    mockResponses({ ...row, achievementStatus: "EVALUATION_CONFIRMED" });
    render(<CourseOperationsPage user={owner} />);
    await select();
    expect(screen.getByTestId("course-save-button")).toBeDisabled();
    expect(screen.getByTestId("course-attachment-ref")).toBeDisabled();
    expect(screen.getByTestId("course-performance-details")).toBeDisabled();
  });

  it("uses filtered pagination and supports 20/50/100", async () => {
    mockResponses(row, 120);
    render(<CourseOperationsPage user={owner} />);
    await screen.findByText("CO-fixture");
    fireEvent.change(screen.getByTestId("course-filter-management-no"), {
      target: { value: "CO-fixture" },
    });
    fireEvent.click(screen.getByTestId("course-search-button"));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/course-operations?page=0&pageSize=20&managementNo=CO-fixture",
      ),
    );
    fireEvent.change(screen.getByTestId("course-page-size"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/course-operations?page=0&pageSize=50&managementNo=CO-fixture",
      ),
    );
    fireEvent.click(screen.getByTestId("course-next-page"));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/course-operations?page=1&pageSize=50&managementNo=CO-fixture",
      ),
    );
  });

  it("shows permission errors and never grants R09 business write access", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "범위 밖입니다."),
    );
    const view = render(<CourseOperationsPage user={owner} />);
    await screen.findByText("범위 밖입니다.");
    view.unmount();
    render(<CourseOperationsPage user={{ ...owner, roles: ["R09"] }} />);
    expect(screen.getByText("권한이 없습니다")).toBeInTheDocument();
    expect(screen.queryByTestId("course-save-button")).not.toBeInTheDocument();
  });

  it("renders server field validation and preserves input on a conflict", async () => {
    mockResponses();
    render(<CourseOperationsPage user={owner} />);
    await select();
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(400, "입력 오류", {
        code: "VALIDATION_ERROR",
        message: "입력 오류",
        fields: [{ field: "performanceDetails", message: "실적 확인" }],
      }),
    );
    fireEvent.click(screen.getByTestId("course-save-button"));
    await screen.findByText("실적 확인");
    expect(screen.getByTestId("course-performance-details")).toHaveValue(
      "강좌 운영 내역",
    );
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(409, "CONFIRMED_DATA_LOCKED"),
    );
    fireEvent.click(screen.getByTestId("course-save-button"));
    await screen.findByText("CONFIRMED_DATA_LOCKED");
    expect(screen.getByTestId("course-performance-details")).toHaveValue(
      "강좌 운영 내역",
    );
  });

  it("authorized navigation renders the canonical route inside the existing shell", async () => {
    mockResponses();
    render(<AppRouter />);
    fireEvent.click(
      screen.getByRole("button", { name: "모바일 메뉴", exact: true }),
    );
    const entry = screen.getByRole("link", {
      name: "강좌 개설·운영 실적 관리",
    });
    expect(entry).toHaveAttribute(
      "href",
      "/faculty/education/course-operations",
    );
    fireEvent.click(entry);
    expect(
      await screen.findByTestId("course-operations-page"),
    ).toBeInTheDocument();
    await screen.findByText("CO-fixture");
    expect(
      screen.getByRole("heading", { name: "강좌 개설·운영 실적 관리" }),
    ).toBeInTheDocument();
  });
});

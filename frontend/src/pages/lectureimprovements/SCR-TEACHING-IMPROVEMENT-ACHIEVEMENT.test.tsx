import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { AppRouter } from "../../app/router";
import { LectureImprovementPage } from "./SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({ status: "authenticated", user: actor, logout: vi.fn() }),
}));

const faculty: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: ["R01"],
  menus: [
    {
      menuId: 21,
      menuName: "강의개선 메뉴",
      displayOrder: 1,
      children: [],
      url: "/faculty/education/lecture-improvements",
      screenId: "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT",
    },
  ],
};
let actor = faculty;
const row = {
  achievementId: 41,
  managementNo: "LI-001",
  teacherUserId: 101,
  teacherName: "교원",
  managementItemCode: "LECTURE_IMPROVEMENT",
  achievementDate: "2026-04-10",
  evaluationYear: "2026",
  performanceContent: "기존 내용",
  academicYear: "2025",
  semester: "2025-1",
  achievementStatus: "DRAFT",
  attachmentRef: null,
};
const list = {
  achievements: [row],
  page: 0,
  pageSize: 20,
  totalElements: 1,
  semesters: [{ code: "2025-1", name: "1학기", academicYear: "2025" }],
  managementItems: [{ code: "LECTURE_IMPROVEMENT", name: "강의개선" }],
};

function setup(detail = row) {
  vi.mocked(apiRequest).mockImplementation(async (path, init) => {
    if (init?.method)
      return {
        success: true,
        data: { achievement: detail },
        meta: {},
      } as never;
    return {
      success: true,
      data: path.includes("/41") ? detail : list,
      meta: {},
    } as never;
  });
}

async function select() {
  await screen.findByText("LI-001");
  fireEvent.click(screen.getByTestId("lecture-detail-41"));
  await waitFor(() =>
    expect(screen.getByTestId("lecture-content")).toHaveValue("기존 내용"),
  );
}

describe("강의개선 화면", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    actor = faculty;
    window.history.replaceState({}, "", "/");
    setup();
  });

  it("uses the existing server menu entry to reach the authorized screen", async () => {
    render(<AppRouter />);
    const links = screen.getAllByRole("link", {
      name: "강의개선 메뉴",
      hidden: true,
    });
    fireEvent.click(links[0]);
    expect(
      await screen.findByTestId("lecture-improvements-screen"),
    ).toBeInTheDocument();
    expect(window.location.pathname).toBe(
      "/faculty/education/lecture-improvements",
    );
    expect(await screen.findByText("LI-001")).toBeInTheDocument();
  });

  it("loads selected detail from the real detail URL instead of copying the list row", async () => {
    setup({ ...row, performanceContent: "상세 API 내용" });
    render(<LectureImprovementPage user={faculty} />);
    await screen.findByText("LI-001");
    fireEvent.click(screen.getByTestId("lecture-detail-41"));
    await waitFor(() =>
      expect(screen.getByTestId("lecture-content")).toHaveValue(
        "상세 API 내용",
      ),
    );
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/lecture-improvements/41",
    );
  });

  it("updates the selected path ID and excludes immutable evaluation year", async () => {
    render(<LectureImprovementPage user={faculty} />);
    await select();
    fireEvent.change(screen.getByTestId("lecture-content"), {
      target: { value: "수정 내용" },
    });
    expect(screen.getByTestId("lecture-evaluation-year")).toBeDisabled();
    fireEvent.click(screen.getByTestId("lecture-save"));
    await screen.findByText("저장되었습니다.");
    const write = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(write?.[0]).toBe("/api/business/lecture-improvements/41");
    const payload = JSON.parse(String(write?.[1]?.body));
    expect(payload.performanceContent).toBe("수정 내용");
    expect(payload).not.toHaveProperty("evaluationYear");
    expect(payload).not.toHaveProperty("achievementId");
  });

  it("creates a new record through POST with API driven semester options", async () => {
    render(<LectureImprovementPage user={faculty} />);
    await screen.findByText("LI-001");
    fireEvent.change(screen.getByTestId("lecture-management-item"), {
      target: { value: "LECTURE_IMPROVEMENT" },
    });
    fireEvent.change(screen.getByTestId("lecture-date"), {
      target: { value: "2026-04-10" },
    });
    fireEvent.change(screen.getByTestId("lecture-academic-year"), {
      target: { value: "2025" },
    });
    fireEvent.change(screen.getByTestId("lecture-semester"), {
      target: { value: "2025-1" },
    });
    fireEvent.change(screen.getByTestId("lecture-content"), {
      target: { value: "신규 내용" },
    });
    fireEvent.click(screen.getByTestId("lecture-save"));
    await screen.findByText("저장되었습니다.");
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(
          ([path, init]) =>
            path === "/api/business/lecture-improvements" &&
            init?.method === "POST",
        ),
    ).toBe(true);
  });

  it("blocks missing required values before sending a write", async () => {
    render(<LectureImprovementPage user={faculty} />);
    await screen.findByText("LI-001");
    fireEvent.click(screen.getByTestId("lecture-save"));
    expect(
      screen.getAllByText("필수 항목을 입력하세요.").length,
    ).toBeGreaterThan(0);
    expect(
      vi.mocked(apiRequest).mock.calls.some(([, init]) => init?.method),
    ).toBe(false);
  });

  it("does not save when confirmation is cancelled", async () => {
    vi.mocked(window.confirm).mockReturnValue(false);
    render(<LectureImprovementPage user={faculty} />);
    await select();
    fireEvent.click(screen.getByTestId("lecture-save"));
    expect(
      vi.mocked(apiRequest).mock.calls.some(([, init]) => init?.method),
    ).toBe(false);
  });

  it("locks confirmed records and attachments", async () => {
    setup({ ...row, achievementStatus: "EVALUATION_CONFIRMED" });
    render(<LectureImprovementPage user={faculty} />);
    await select();
    expect(screen.getByTestId("lecture-save")).toBeDisabled();
    expect(screen.getByTestId("lecture-attachment")).toBeDisabled();
    expect(screen.getByTestId("lecture-content")).toBeDisabled();
  });

  it("locks submitted records", async () => {
    setup({ ...row, achievementStatus: "SUBMITTED" });
    render(<LectureImprovementPage user={faculty} />);
    await select();
    expect(screen.getByTestId("lecture-save")).toBeDisabled();
  });

  it("locks other owners even for a multi role faculty reader", async () => {
    setup({ ...row, teacherUserId: 202 });
    render(
      <LectureImprovementPage user={{ ...faculty, roles: ["R01", "R02"] }} />,
    );
    await select();
    expect(screen.getByTestId("lecture-save")).toBeDisabled();
  });

  it("has a read only view for R02", async () => {
    render(<LectureImprovementPage user={{ ...faculty, roles: ["R02"] }} />);
    await screen.findByText("LI-001");
    expect(screen.queryByTestId("lecture-new")).not.toBeInTheDocument();
    expect(screen.queryByTestId("lecture-save")).not.toBeInTheDocument();
    expect(screen.getByTestId("lecture-content")).toBeDisabled();
  });

  it("denies R07 without calling the resource", () => {
    render(<LectureImprovementPage user={{ ...faculty, roles: ["R07"] }} />);
    expect(
      screen.getByText("강의개선 실적 접근 권한이 없습니다."),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("keeps administrator access", async () => {
    render(<LectureImprovementPage user={{ ...faculty, roles: ["R09"] }} />);
    await screen.findByText("LI-001");
    expect(screen.getByTestId("lecture-new")).toBeEnabled();
  });

  it("shows empty results without adding example rows", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: { ...list, achievements: [], totalElements: 0 },
      meta: {},
    });
    render(<LectureImprovementPage user={faculty} />);
    expect(await screen.findByText("데이터 없음")).toBeInTheDocument();
    expect(screen.queryByText("LI-001")).not.toBeInTheDocument();
  });

  it("shows loading while the request is pending", () => {
    vi.mocked(apiRequest).mockReturnValue(new Promise(() => {}));
    render(<LectureImprovementPage user={faculty} />);
    expect(screen.getByText("불러오는 중")).toBeInTheDocument();
    expect(screen.getByTestId("lecture-save")).toBeDisabled();
  });

  it("shows failed list requests", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(500, "조회 실패"),
    );
    render(<LectureImprovementPage user={faculty} />);
    expect(await screen.findByText("조회 실패")).toBeInTheDocument();
  });

  it("shows permission denial returned by server", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "권한 없음"),
    );
    render(<LectureImprovementPage user={faculty} />);
    expect(
      await screen.findByText("강의개선 실적 접근 권한이 없습니다."),
    ).toBeInTheDocument();
  });

  it("maps server field errors and does not announce success", async () => {
    render(<LectureImprovementPage user={faculty} />);
    await select();
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(400, "입력 오류", {
        code: "VALIDATION_ERROR",
        message: "입력 오류",
        fields: [{ field: "semester", message: "활성 학기만 허용" }],
      }),
    );
    fireEvent.click(screen.getByTestId("lecture-save"));
    expect(await screen.findByText("활성 학기만 허용")).toBeInTheDocument();
    expect(screen.queryByText("저장되었습니다.")).not.toBeInTheDocument();
  });

  it("shows period conflict without losing form data", async () => {
    render(<LectureImprovementPage user={faculty} />);
    await select();
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(409, "활성 입력기간이 아닙니다."),
    );
    fireEvent.click(screen.getByTestId("lecture-save"));
    expect(
      await screen.findByText("활성 입력기간이 아닙니다."),
    ).toBeInTheDocument();
    expect(screen.getByTestId("lecture-content")).toHaveValue("기존 내용");
  });
});

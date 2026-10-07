import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import {
  LectureImprovementsPage,
  type LectureImprovement,
} from "./SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";

const session = vi.hoisted(() => ({ user: null as CurrentUser | null }));
vi.mock("../../app/AuthProvider", () => ({ useAuth: () => session }));
vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const row: LectureImprovement = {
  achievementId: 83,
  userId: 101,
  managementNo: "LI-83",
  teacherName: "교원",
  managementItemCode: "LECTURE_IMPROVEMENT",
  achievementDate: "2026-04-10",
  performanceContent: "수업 개선 내용",
  academicYear: 2026,
  semester: 1,
  attachmentRef: "opaque-reference",
  achievementStatus: "DRAFT",
};
const ok = <T,>(data: T) => ({ success: true, data, meta: {} });
function list(
  achievements: LectureImprovement[] = [row],
  totalElements = achievements.length,
) {
  return ok({ achievements, totalElements, page: 0, pageSize: 20 });
}
function fillForm() {
  fireEvent.change(screen.getByLabelText("관리항목 코드 *"), {
    target: { value: "LECTURE_IMPROVEMENT" },
  });
  fireEvent.change(screen.getByLabelText("업적발생일 *"), {
    target: { value: "2026-04-10" },
  });
  fireEvent.change(screen.getByLabelText("실적내용 *"), {
    target: { value: "수업 개선 내용" },
  });
  fireEvent.change(screen.getByLabelText("학년도 *"), {
    target: { value: "2026" },
  });
  fireEvent.change(screen.getByLabelText("학기 *"), { target: { value: "1" } });
  fireEvent.change(screen.getByLabelText("첨부 참조"), {
    target: { value: "opaque-reference" },
  });
}

describe("SCR-LECTURE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    session.user = {
      userId: 101,
      loginId: "faculty",
      name: "교원",
      roles: ["R01"],
      menus: [],
    };
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders loading then empty state, and keeps the logical screen ID", async () => {
    let resolve!: (value: ReturnType<typeof list>) => void;
    vi.mocked(apiRequest).mockReturnValue(
      new Promise((done) => {
        resolve = done;
      }),
    );
    render(<LectureImprovementsPage />);
    expect(
      screen.getByText("강의개선 실적 목록을 불러오고 있습니다."),
    ).toBeInTheDocument();
    expect(screen.getByTestId("lecture-improvements-page")).toHaveAttribute(
      "data-screen-id",
      "SCR-LECTURE-IMPROVEMENTS",
    );
    resolve(list([]));
    expect(await screen.findByText("데이터 없음")).toBeInTheDocument();
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/lecture-improvements?page=0&pageSize=20",
    );
  });

  it("reads the selected ID from the detail API rather than treating the list row as a form", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list())
      .mockResolvedValueOnce(
        ok({ ...row, performanceContent: "상세 API 값", semester: 2 }),
      );
    render(<LectureImprovementsPage />);
    fireEvent.click(await screen.findByRole("button", { name: "LI-83 상세" }));
    await waitFor(() =>
      expect(screen.getByLabelText("실적내용 *")).toHaveValue("상세 API 값"),
    );
    expect(screen.getByLabelText("학기 *")).toHaveValue("2");
    expect(apiRequest).toHaveBeenLastCalledWith(
      "/api/business/lecture-improvements/83",
    );
    expect(
      screen.getByText("(서버 관리)", { exact: false }),
    ).toBeInTheDocument();
  });

  it("POSTs only the six approved fields and shows success", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list([]))
      .mockResolvedValueOnce(ok({ achievement: row }))
      .mockResolvedValueOnce(list());
    render(<LectureImprovementsPage />);
    await screen.findByText("데이터 없음");
    fillForm();
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(
      await screen.findByText("강의개선 실적이 저장되었습니다."),
    ).toBeInTheDocument();
    expect(window.confirm).toHaveBeenCalledWith(
      "강의개선 실적을 저장하시겠습니까?",
    );
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/lecture-improvements",
      {
        method: "POST",
        body: JSON.stringify({
          managementItemCode: "LECTURE_IMPROVEMENT",
          achievementDate: "2026-04-10",
          performanceContent: "수업 개선 내용",
          academicYear: 2026,
          semester: 1,
          attachmentRef: "opaque-reference",
        }),
      },
    );
  });

  it("uses PUT on the selected ID and retains the server status", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list())
      .mockResolvedValueOnce(ok(row))
      .mockResolvedValueOnce(
        ok({
          achievement: { ...row, semester: 2 },
          occurredDateWarning: true,
          warningMessage: "평가기간 밖 발생일입니다.",
        }),
      )
      .mockResolvedValueOnce(list());
    render(<LectureImprovementsPage />);
    fireEvent.click(await screen.findByRole("button", { name: "LI-83 상세" }));
    await waitFor(() =>
      expect(screen.getByLabelText("학년도 *")).toHaveValue(2026),
    );
    fireEvent.change(screen.getByLabelText("학기 *"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("평가기간 밖 발생일입니다.");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/lecture-improvements/83");
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({
      managementItemCode: row.managementItemCode,
      achievementDate: row.achievementDate,
      performanceContent: row.performanceContent,
      academicYear: 2026,
      semester: 2,
      attachmentRef: row.attachmentRef,
    });
  });

  it.each([
    ["R02", 101, "DRAFT"],
    ["R04", 101, "DRAFT"],
    ["R01", 102, "DRAFT"],
    ["R01", 101, "EVALUATION_CONFIRMED"],
    ["R01", 101, "SUBMITTED"],
    ["R01", 101, "CERTIFIED"],
    ["R01", 101, "UNKNOWN"],
  ])(
    "locks %s owner %s status %s including attachments",
    async (role, owner, status) => {
      session.user!.roles = [role];
      vi.mocked(apiRequest)
        .mockResolvedValueOnce(list())
        .mockResolvedValueOnce(
          ok({ ...row, userId: owner, achievementStatus: status }),
        );
      render(<LectureImprovementsPage />);
      fireEvent.click(
        await screen.findByRole("button", { name: "LI-83 상세" }),
      );
      await waitFor(() =>
        expect(screen.getByLabelText("학기 *")).toHaveValue("1"),
      );
      expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
      expect(screen.getByLabelText("첨부 참조")).toBeDisabled();
      expect(screen.getByLabelText("실적내용 *")).toBeDisabled();
      expect(apiRequest).toHaveBeenCalledTimes(2);
    },
  );

  it("permits own rejected records but prevents writes when detail retrieval fails", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list())
      .mockResolvedValueOnce(
        ok({ ...row, achievementStatus: "DEPARTMENT_REJECTED" }),
      )
      .mockRejectedValueOnce(new ApiClientError(404, "실적 없음"));
    render(<LectureImprovementsPage />);
    fireEvent.click(await screen.findByRole("button", { name: "LI-83 상세" }));
    await waitFor(() =>
      expect(screen.getByLabelText("학기 *")).toHaveValue("1"),
    );
    expect(screen.getByRole("button", { name: "저장" })).toBeEnabled();
    fireEvent.click(screen.getByRole("button", { name: "LI-83 상세" }));
    await screen.findByText("실적 없음");
    expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
  });

  it("validates required fields before confirmation and does not write on cancelled confirmation", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list([]));
    render(<LectureImprovementsPage />);
    await screen.findByText("데이터 없음");
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(screen.getByLabelText("학년도 *")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(window.confirm).not.toHaveBeenCalled();
    expect(apiRequest).toHaveBeenCalledTimes(1);
    fillForm();
    vi.mocked(window.confirm).mockReturnValue(false);
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(window.confirm).toHaveBeenCalledTimes(1);
    expect(apiRequest).toHaveBeenCalledTimes(1);
  });

  it("shows field validation and conflict errors without losing the entered values", async () => {
    vi.mocked(apiRequest)
      .mockResolvedValueOnce(list([]))
      .mockRejectedValueOnce(
        new ApiClientError(400, "검증 실패", {
          code: "VALIDATION_ERROR",
          message: "검증 실패",
          fields: [{ field: "academicYear", message: "학년도 범위 오류" }],
        }),
      )
      .mockRejectedValueOnce(
        new ApiClientError(409, "평가확정 실적은 수정할 수 없습니다."),
      );
    render(<LectureImprovementsPage />);
    await screen.findByText("데이터 없음");
    fillForm();
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("학년도 범위 오류");
    expect(screen.getByLabelText("학년도 *")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(screen.getByLabelText("실적내용 *")).toHaveValue(
      row.performanceContent,
    );
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await screen.findByText("평가확정 실적은 수정할 수 없습니다.");
    expect(
      screen.queryByText("강의개선 실적이 저장되었습니다."),
    ).not.toBeInTheDocument();
  });

  it.each(["R07", "R09"])(
    "rejects non-reader %s locally without an API call",
    async (role) => {
      session.user!.roles = [role];
      render(<LectureImprovementsPage />);
      expect(
        screen.getByText("강의개선 실적 권한이 없습니다"),
      ).toBeInTheDocument();
      expect(apiRequest).not.toHaveBeenCalled();
    },
  );

  it("handles server 403 and generic list errors", async () => {
    vi.mocked(apiRequest).mockRejectedValueOnce(new Error("연결 실패"));
    const view = render(<LectureImprovementsPage />);
    await screen.findByText("연결 실패");
    view.unmount();
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(403, "범위 밖"),
    );
    render(<LectureImprovementsPage />);
    expect(
      await screen.findByText("강의개선 실적 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "저장" }),
    ).not.toBeInTheDocument();
  });

  it("submits search, pages results, and resets page for sizes 50 and 100", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list([row], 105));
    render(<LectureImprovementsPage />);
    await screen.findByText("LI-83");
    fireEvent.change(screen.getByLabelText("관리항목 검색"), {
      target: { value: "LECTURE_IMPROVEMENT" },
    });
    fireEvent.change(screen.getByLabelText("학년도 검색"), {
      target: { value: "2026" },
    });
    fireEvent.change(screen.getByLabelText("학기 검색"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByRole("button", { name: "조회" }));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenLastCalledWith(
        "/api/business/lecture-improvements?page=0&pageSize=20&managementItemCode=LECTURE_IMPROVEMENT&academicYear=2026&semester=2",
      ),
    );
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "다음" })).toBeEnabled(),
    );
    fireEvent.click(screen.getByRole("button", { name: "다음" }));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenLastCalledWith(
        "/api/business/lecture-improvements?page=1&pageSize=20&managementItemCode=LECTURE_IMPROVEMENT&academicYear=2026&semester=2",
      ),
    );
    for (const size of [50, 100]) {
      fireEvent.change(screen.getByLabelText("페이지 크기"), {
        target: { value: String(size) },
      });
      await waitFor(() =>
        expect(apiRequest).toHaveBeenLastCalledWith(
          `/api/business/lecture-improvements?page=0&pageSize=${size}&managementItemCode=LECTURE_IMPROVEMENT&academicYear=2026&semester=2`,
        ),
      );
    }
    expect(screen.getByRole("button", { name: "이전" })).toBeDisabled();
  });
});

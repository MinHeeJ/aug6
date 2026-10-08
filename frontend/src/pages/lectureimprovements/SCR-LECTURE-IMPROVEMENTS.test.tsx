import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import { useAuth } from "../../app/AuthProvider";
import { LectureImprovementsPage } from "./SCR-LECTURE-IMPROVEMENTS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
vi.mock("../../app/AuthProvider", () => ({ useAuth: vi.fn() }));
const row = {
  achievementId: 82,
  teacherUserId: 101,
  evaluationYear: "2026",
  managementItemCode: "LECTURE_IMPROVEMENT",
  achievementDate: "2026-04-10",
  achievementContent: "강의 개선 내용",
  academicYear: 2026,
  semester: 1,
  achievementStatus: "DRAFT",
  attachmentIds: ["existing-attachment"],
};
function role(roles = ["R01"]) {
  vi.mocked(useAuth).mockReturnValue({
    user: { userId: 101, loginId: "faculty", name: "교원", roles, menus: [] },
    status: "authenticated",
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
  });
}
function list(rows = [row]) {
  return {
    success: true,
    data: { achievements: rows, totalElements: rows.length },
    meta: {},
  };
}

describe("SCR-LECTURE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    role();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });

  it("shows loading then an empty list", async () => {
    let finish!: (value: ReturnType<typeof list>) => void;
    vi.mocked(apiRequest).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve;
      }),
    );
    render(<LectureImprovementsPage />);
    expect(screen.getByText("강의개선 실적 조회 중")).toBeInTheDocument();
    finish(list([]));
    expect(
      await screen.findByText("조회된 강의개선 실적이 없습니다"),
    ).toBeInTheDocument();
  });

  it("loads selected detail and uses the selected path id for PUT without changing evaluation year", async () => {
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (init?.method === "PUT")
        return {
          success: true,
          data: {
            achievement: row,
            warningMessage: "발생일 경고와 함께 저장되었습니다.",
          },
          meta: {},
        };
      return path === "/api/business/lecture-improvements/82"
        ? { success: true, data: row, meta: {} }
        : list();
    });
    render(<LectureImprovementsPage />);
    fireEvent.click(await screen.findByTestId("lecture-improvement-detail-82"));
    await waitFor(() =>
      expect(
        screen.getByTestId("lecture-improvement-academicYear"),
      ).toHaveValue("2026"),
    );
    expect(
      screen.getByTestId("lecture-improvement-evaluationYear"),
    ).toBeDisabled();
    fireEvent.change(screen.getByTestId("lecture-improvement-semester"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByTestId("lecture-improvement-save"));
    expect(
      await screen.findByText("발생일 경고와 함께 저장되었습니다."),
    ).toBeInTheDocument();
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/lecture-improvements/82");
    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({
      semester: 2,
      evaluationYear: "2026",
      attachmentIds: ["existing-attachment"],
    });
  });

  it("creates using POST then refreshes", async () => {
    vi.mocked(apiRequest).mockImplementation(async (_path, init) =>
      init?.method === "POST"
        ? { success: true, data: { achievement: row }, meta: {} }
        : list([]),
    );
    render(<LectureImprovementsPage />);
    await screen.findByText("조회된 강의개선 실적이 없습니다");
    const values = {
      evaluationYear: "2026",
      managementItemCode: "LECTURE_IMPROVEMENT",
      achievementDate: "2026-04-10",
      academicYear: "2026",
      semester: "1",
      achievementContent: "강의 개선 내용",
    };
    Object.entries(values).forEach(([field, value]) => {
      fireEvent.change(screen.getByTestId(`lecture-improvement-${field}`), {
        target: { value },
      });
    });
    fireEvent.click(screen.getByTestId("lecture-improvement-save"));
    await screen.findByText("저장되었습니다.");
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/lecture-improvements",
      expect.objectContaining({ method: "POST" }),
    );
    expect(
      vi.mocked(apiRequest).mock.calls.filter(([, init]) => !init?.method),
    ).toHaveLength(2);
  });

  it("blocks missing required fields before confirming", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list([]));
    render(<LectureImprovementsPage />);
    await screen.findByText("조회된 강의개선 실적이 없습니다");
    fireEvent.click(screen.getByTestId("lecture-improvement-save"));
    expect(screen.getAllByText("필수 입력 항목입니다.")).toHaveLength(6);
    expect(window.confirm).not.toHaveBeenCalled();
  });

  it("cancellation does not mutate", async () => {
    vi.mocked(apiRequest).mockImplementation(async (path) =>
      path.endsWith("/82") ? { success: true, data: row, meta: {} } : list(),
    );
    vi.mocked(window.confirm).mockReturnValue(false);
    render(<LectureImprovementsPage />);
    fireEvent.click(await screen.findByTestId("lecture-improvement-detail-82"));
    await waitFor(() =>
      expect(
        screen.getByTestId("lecture-improvement-academicYear"),
      ).toHaveValue("2026"),
    );
    fireEvent.click(screen.getByTestId("lecture-improvement-save"));
    expect(
      vi.mocked(apiRequest).mock.calls.some(([, init]) => !!init?.method),
    ).toBe(false);
  });

  it.each(["EVALUATION_CONFIRMED", "SUBMITTED", "CERTIFIED"])(
    "locks all fields for %s",
    async (status) => {
      vi.mocked(apiRequest).mockImplementation(async (path) =>
        path.endsWith("/82")
          ? {
              success: true,
              data: { ...row, achievementStatus: status },
              meta: {},
            }
          : list(),
      );
      render(<LectureImprovementsPage />);
      fireEvent.click(
        await screen.findByTestId("lecture-improvement-detail-82"),
      );
      await screen.findByText(
        "확정·제출 상태 또는 타인 실적은 수정할 수 없습니다.",
      );
      expect(screen.getByTestId("lecture-improvement-save")).toBeDisabled();
      expect(
        screen.getByTestId("lecture-improvement-achievementContent"),
      ).toBeDisabled();
    },
  );

  it("read-only role has no save or new control", async () => {
    role(["R02"]);
    vi.mocked(apiRequest).mockResolvedValue(list());
    render(<LectureImprovementsPage />);
    await screen.findByTestId("lecture-improvement-detail-82");
    expect(
      screen.queryByTestId("lecture-improvement-save"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("lecture-improvement-new"),
    ).not.toBeInTheDocument();
  });

  it("R07 is denied without automatically calling single-record APIs", () => {
    role(["R07"]);
    render(<LectureImprovementsPage />);
    expect(
      screen.getByText("강의개선 실적 관리 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });

  it("R09 override exposes save", async () => {
    role(["R09"]);
    vi.mocked(apiRequest).mockResolvedValue(list([]));
    render(<LectureImprovementsPage />);
    await screen.findByText("조회된 강의개선 실적이 없습니다");
    expect(screen.getByTestId("lecture-improvement-save")).toBeEnabled();
  });

  it("failed list displays server error", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(500, "조회 실패"),
    );
    render(<LectureImprovementsPage />);
    expect(await screen.findByText("조회 실패")).toBeInTheDocument();
  });

  it("403 displays permission state", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "권한 없음"),
    );
    render(<LectureImprovementsPage />);
    expect(
      await screen.findByText("강의개선 실적 관리 권한이 없습니다"),
    ).toBeInTheDocument();
  });

  it("search and page size are passed through the real API path", async () => {
    vi.mocked(apiRequest).mockResolvedValue(list([]));
    render(<LectureImprovementsPage />);
    await screen.findByText("조회된 강의개선 실적이 없습니다");
    fireEvent.change(screen.getByTestId("lecture-improvement-filter"), {
      target: { value: "selected-code" },
    });
    fireEvent.click(screen.getByTestId("lecture-improvement-search"));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/lecture-improvements?page=0&pageSize=20&managementItemCode=selected-code",
      ),
    );
    fireEvent.change(screen.getByTestId("lecture-improvement-page-size"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/lecture-improvements?page=0&pageSize=50&managementItemCode=selected-code",
      ),
    );
  });
});

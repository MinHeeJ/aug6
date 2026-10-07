import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import { LectureImprovementPage } from "./SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";
import { AppRouter } from "../../app/router";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const user: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  employeeNo: "101",
  name: "교원",
  roles: ["R01"],
  menus: [
    {
      menuId: 992,
      menuName: "강의개선 실적 관리",
      displayOrder: 1,
      children: [],
      url: "/faculty/education/lecture-improvements",
      screenId: "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT",
    },
  ],
};
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({ status: "authenticated", user }),
}));
const row = {
  achievementId: 71,
  managementNo: "LI-71",
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2025",
  managementItemCode: "lecture-improvements",
  achievementDate: "2025-04-10",
  achievementContent: "개선 내용",
  academicYear: 2025,
  semester: 2,
  achievementStatus: "DRAFT",
  attachmentIds: [],
};
const list = {
  achievements: [row],
  totalElements: 1,
  managementItems: [
    {
      managementItemId: 1,
      managementItemCode: "lecture-improvements",
      managementItemName: "강의개선",
    },
  ],
};
const envelope = (data: unknown) => ({ success: true, data, meta: {} });

describe("강의개선 실적", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (init?.method)
        return envelope({
          achievement: row,
          occurredDateWarning: true,
          warningMessage: "발생일 기간 경고",
        });
      return envelope(path.endsWith("/71") ? row : list);
    });
  });

  it("authorized direct route renders the real screen in the existing shell", async () => {
    window.history.replaceState(
      {},
      "",
      "/faculty/education/lecture-improvements",
    );
    render(<AppRouter />);
    expect(await screen.findByText("LI-71")).toBeInTheDocument();
    expect(screen.getByTestId("lecture-improvements-page")).toHaveAttribute(
      "data-screen-id",
      "SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT",
    );
  });

  it("loads selected detail and PUTs its actual id, keeping client-owned fields only", async () => {
    render(<LectureImprovementPage user={user} />);
    await screen.findByText("LI-71");
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-71"));
    await waitFor(() =>
      expect(screen.getByTestId("lecture-improvements-year")).toHaveValue(2025),
    );
    expect(screen.getByTestId("lecture-improvements-semester")).toHaveValue(
      "2",
    );
    fireEvent.change(screen.getByTestId("lecture-improvements-content"), {
      target: { value: "수정 내용" },
    });
    fireEvent.click(screen.getByTestId("lecture-improvements-save"));
    await screen.findByText("발생일 기간 경고");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/lecture-improvements/71");
    const payload = JSON.parse(String(call?.[1]?.body));
    expect(payload).toMatchObject({
      academicYear: 2025,
      semester: 2,
      achievementContent: "수정 내용",
    });
    expect(payload).not.toHaveProperty("evaluationYear");
    expect(payload).not.toHaveProperty("teacherUserId");
    expect(payload).not.toHaveProperty("achievementId");
  });

  it("creates with POST and does not save when confirmation is declined", async () => {
    render(<LectureImprovementPage user={user} />);
    await screen.findByText("LI-71");
    fireEvent.change(screen.getByTestId("lecture-improvements-item"), {
      target: { value: "lecture-improvements" },
    });
    fireEvent.change(screen.getByTestId("lecture-improvements-date"), {
      target: { value: "2025-04-10" },
    });
    fireEvent.change(screen.getByTestId("lecture-improvements-year"), {
      target: { value: "2025" },
    });
    fireEvent.change(screen.getByTestId("lecture-improvements-semester"), {
      target: { value: "2" },
    });
    fireEvent.change(screen.getByTestId("lecture-improvements-content"), {
      target: { value: "개선 내용" },
    });
    vi.mocked(window.confirm).mockReturnValue(false);
    fireEvent.click(screen.getByTestId("lecture-improvements-save"));
    expect(
      vi.mocked(apiRequest).mock.calls.some(([, init]) => init?.method),
    ).toBe(false);
    vi.mocked(window.confirm).mockReturnValue(true);
    fireEvent.click(screen.getByTestId("lecture-improvements-save"));
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        "/api/business/lecture-improvements",
        expect.objectContaining({ method: "POST" }),
      ),
    );
  });

  it("confirmed records are readonly and R02 cannot create", async () => {
    vi.mocked(apiRequest).mockResolvedValueOnce(envelope(list));
    vi.mocked(apiRequest).mockResolvedValueOnce(
      envelope({ ...row, achievementStatus: "EVALUATION_CONFIRMED" }),
    );
    render(<LectureImprovementPage user={{ ...user, roles: ["R02"] }} />);
    await screen.findByText("LI-71");
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-71"));
    await screen.findByText(/평가연도: 2025/);
    expect(screen.getByTestId("lecture-improvements-save")).toBeDisabled();
    expect(screen.getByTestId("lecture-improvements-content")).toBeDisabled();
    expect(
      screen.queryByTestId("lecture-improvements-new"),
    ).not.toBeInTheDocument();
  });

  it("changes page size and shows API field errors", async () => {
    render(<LectureImprovementPage user={user} />);
    await screen.findByText("LI-71");
    fireEvent.change(screen.getByTestId("lecture-improvements-page-size"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(apiRequest).toHaveBeenCalledWith(
        expect.stringContaining("pageSize=50"),
      ),
    );
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-71"));
    await waitFor(() =>
      expect(screen.getByTestId("lecture-improvements-year")).toHaveValue(2025),
    );
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(400, "입력 오류", {
        code: "VALIDATION_ERROR",
        message: "입력 오류",
        fields: [{ field: "semester", message: "학기 오류" }],
      }),
    );
    fireEvent.click(screen.getByTestId("lecture-improvements-save"));
    expect(await screen.findByText("semester: 학기 오류")).toBeInTheDocument();
  });

  it("denies R07 without fetching business data", () => {
    render(<LectureImprovementPage user={{ ...user, roles: ["R07"] }} />);
    expect(
      screen.getByText("강의개선 실적 관리 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });
});

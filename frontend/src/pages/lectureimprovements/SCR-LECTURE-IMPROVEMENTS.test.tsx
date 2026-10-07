import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  apiRequest,
  ApiClientError,
  type CurrentUser,
} from "../../api/apiClient";
import { LectureImprovementPage } from "./SCR-LECTURE-IMPROVEMENTS";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
const user: CurrentUser = {
  userId: 101,
  loginId: "faculty",
  name: "교원",
  roles: ["R01"],
  menus: [],
};
const row = {
  achievementId: 501,
  managementNo: "LI-001",
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2026",
  managementItemCode: "LECTURE_IMPROVEMENT",
  achievementDate: "2025-12-31",
  achievementContent: "개선 내용",
  academicYear: 2025,
  semester: 2,
  achievementStatus: "DRAFT",
  attachmentIds: [],
};
const metadata = {
  managementItemCode: "LECTURE_IMPROVEMENT",
  managementItemName: "강의개선",
  evaluationYear: "2026",
  teacherEditableYn: "Y",
  requiredYn: "Y",
  dataType: "TEXT",
};
function setup(state = "DRAFT") {
  vi.mocked(apiRequest).mockImplementation(async (path, init) => {
    if (init?.method)
      return {
        success: true,
        data: {
          achievement: { ...row, achievementStatus: state },
          achievementId: 501,
          occurredDateWarning: true,
          warningMessage: "평가대상 기간 밖입니다.",
        },
        meta: {},
      };
    if (path.endsWith("/501"))
      return {
        success: true,
        data: { ...row, achievementStatus: state },
        meta: {},
      };
    return {
      success: true,
      data: {
        achievements: [{ ...row, achievementStatus: state }],
        page: 0,
        pageSize: 20,
        totalElements: 1,
        managementItems: [metadata],
      },
      meta: {},
    };
  });
}

describe("SCR-LECTURE-IMPROVEMENTS", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
  });
  it("loads API metadata, selects detail and PUTs only the selected identity", async () => {
    setup();
    render(<LectureImprovementPage user={user} />);
    await screen.findByText("LI-001");
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-501"));
    await waitFor(() =>
      expect(
        screen.getByTestId("lecture-improvements-academic-year"),
      ).toHaveValue(2025),
    );
    fireEvent.change(screen.getByTestId("lecture-improvements-semester"), {
      target: { value: "1" },
    });
    fireEvent.click(screen.getByTestId("lecture-improvements-save"));
    await screen.findByText("평가대상 기간 밖입니다.");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/lecture-improvements/501");
    const body = JSON.parse(String(call?.[1]?.body));
    expect(body).toMatchObject({
      academicYear: 2025,
      semester: 1,
      achievementContent: "개선 내용",
    });
    expect(body).not.toHaveProperty("achievementId");
    expect(body).not.toHaveProperty("evaluationYear");
    expect(body).not.toHaveProperty("teacherUserId");
  });
  it("creates using POST and blocks cancellation", async () => {
    setup();
    render(<LectureImprovementPage user={user} />);
    await screen.findByText("LI-001");
    fireEvent.change(
      screen.getByTestId("lecture-improvements-management-item"),
      { target: { value: "LECTURE_IMPROVEMENT" } },
    );
    fireEvent.change(screen.getByTestId("lecture-improvements-date"), {
      target: { value: "2025-12-31" },
    });
    fireEvent.change(screen.getByTestId("lecture-improvements-academic-year"), {
      target: { value: "2025" },
    });
    fireEvent.change(screen.getByTestId("lecture-improvements-semester"), {
      target: { value: "2" },
    });
    fireEvent.change(screen.getByTestId("lecture-improvements-content"), {
      target: { value: "강의 개선" },
    });
    vi.mocked(window.confirm).mockReturnValue(false);
    fireEvent.click(screen.getByTestId("lecture-improvements-save"));
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([, init]) => init?.method === "POST"),
    ).toBe(false);
    vi.mocked(window.confirm).mockReturnValue(true);
    fireEvent.click(screen.getByTestId("lecture-improvements-save"));
    await screen.findByText("평가대상 기간 밖입니다.");
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.find(([, init]) => init?.method === "POST")?.[0],
    ).toBe("/api/business/lecture-improvements");
  });
  it("locks confirmed details", async () => {
    setup("EVALUATION_CONFIRMED");
    render(<LectureImprovementPage user={user} />);
    await screen.findByText("LI-001");
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-501"));
    await screen.findByText("현재 상태에서는 수정할 수 없습니다.");
    expect(screen.getByTestId("lecture-improvements-save")).toBeDisabled();
    expect(screen.getByTestId("lecture-improvements-semester")).toBeDisabled();
  });
  it("R02 reads without a mutation control", async () => {
    setup();
    render(<LectureImprovementPage user={{ ...user, roles: ["R02"] }} />);
    await screen.findByText("LI-001");
    expect(
      screen.queryByTestId("lecture-improvements-save"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("lecture-improvements-new"),
    ).not.toBeInTheDocument();
  });
  it("R07 does not call individual APIs", async () => {
    render(<LectureImprovementPage user={{ ...user, roles: ["R07"] }} />);
    expect(
      screen.getByText("강의개선 실적 권한이 없습니다"),
    ).toBeInTheDocument();
    expect(apiRequest).not.toHaveBeenCalled();
  });
  it("empty response remains usable", async () => {
    vi.mocked(apiRequest).mockResolvedValue({
      success: true,
      data: {
        achievements: [],
        managementItems: [],
        totalElements: 0,
        page: 0,
        pageSize: 20,
      },
      meta: {},
    });
    render(<LectureImprovementPage user={user} />);
    await screen.findByText("조회 결과 없음");
    expect(screen.getByTestId("lecture-improvements-new")).toBeEnabled();
  });
  it("failed request is visible", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(500, "요청 실패"),
    );
    render(<LectureImprovementPage user={user} />);
    await screen.findByText("요청 실패");
  });
  it("displays object field errors from the approved error projection", async () => {
    setup();
    render(<LectureImprovementPage user={user} />);
    await screen.findByText("LI-001");
    fireEvent.click(screen.getByTestId("lecture-improvements-detail-501"));
    await waitFor(() =>
      expect(
        screen.getByTestId("lecture-improvements-academic-year"),
      ).toHaveValue(2025),
    );
    vi.mocked(apiRequest).mockRejectedValueOnce(
      new ApiClientError(400, "입력 오류", {
        code: "VALIDATION_ERROR",
        message: "입력 오류",
        fields: { semester: "학기 오류" } as unknown as {
          field: string;
          message: string;
        }[],
      }),
    );
    fireEvent.click(screen.getByTestId("lecture-improvements-save"));
    await screen.findByText("semester: 학기 오류");
  });
});

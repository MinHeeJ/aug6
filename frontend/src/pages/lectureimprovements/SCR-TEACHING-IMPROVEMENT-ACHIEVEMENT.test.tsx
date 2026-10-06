import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  LectureImprovementManagementPage,
  type LectureImprovement,
} from "./SCR-TEACHING-IMPROVEMENT-ACHIEVEMENT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, apiRequest: vi.fn() };
});
vi.mock("../../app/AuthProvider", () => ({
  useAuth: () => ({ user: { userId: 101, roles: ["R01"] } }),
}));

const row: LectureImprovement = {
  achievementId: 82,
  managementNo: "LI-test",
  teacherUserId: 101,
  teacherName: "교원",
  evaluationYear: "2025",
  managementItemCode: "FR-031",
  achievementDate: "2025-04-10",
  achievementStatus: "DRAFT",
  achievementContent: "기존 실적내용",
  academicYear: 2025,
  semester: 1,
  attachmentIds: ["retained-reference"],
};
const list = {
  achievements: [row],
  page: 0,
  pageSize: 20,
  totalElements: 1,
  academicYears: [{ value: "2025", label: "2025학년도" }],
  semesters: [
    { value: "1", label: "1학기" },
    { value: "2", label: "2학기" },
  ],
  managementItems: [{ value: "FR-031", label: "강의개선" }],
  canCreate: true,
  canUpdate: true,
};
const success = <T,>(data: T) => ({ success: true, data, meta: {} });

function fillForm() {
  fireEvent.change(
    screen.getByTestId("lecture-improvement-management-item-code"),
    { target: { value: "FR-031" } },
  );
  fireEvent.change(screen.getByTestId("lecture-improvement-achievement-date"), {
    target: { value: "2025-04-10" },
  });
  fireEvent.change(screen.getByTestId("lecture-improvement-academic-year"), {
    target: { value: "2025" },
  });
  fireEvent.change(screen.getByTestId("lecture-improvement-semester"), {
    target: { value: "1" },
  });
  fireEvent.change(
    screen.getByTestId("lecture-improvement-achievement-content"),
    { target: { value: "수업 개선" } },
  );
}

describe("강의개선 실적 화면", () => {
  beforeEach(() => {
    vi.mocked(apiRequest).mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (init?.method === "POST" || init?.method === "PUT") {
        return success({ achievement: row, occurredDateWarning: false });
      }
      if (path.endsWith("/82")) return success(row);
      return success(list);
    });
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("loads real API options and opens selected detail with its domain ID", async () => {
    render(<LectureImprovementManagementPage />);
    await screen.findByText("LI-test");
    fireEvent.click(screen.getByTestId("lecture-improvement-detail-82"));
    await waitFor(() =>
      expect(
        screen.getByTestId("lecture-improvement-achievement-content"),
      ).toHaveValue("기존 실적내용"),
    );
    expect(apiRequest).toHaveBeenCalledWith(
      "/api/business/lecture-improvements/82",
    );
    expect(screen.getByTestId("lecture-improvement-academic-year")).toHaveValue(
      "2025",
    );
    expect(screen.getByTestId("lecture-improvement-semester")).toHaveValue("1");
    expect(screen.getByText(/retained-reference/)).toBeInTheDocument();
  });

  it("creates with numeric year/semester and excludes server-owned fields", async () => {
    render(<LectureImprovementManagementPage />);
    await screen.findByText("LI-test");
    fillForm();
    fireEvent.click(screen.getByTestId("lecture-improvement-save-button"));
    await screen.findByText("저장되었습니다.");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "POST");
    expect(call?.[0]).toBe("/api/business/lecture-improvements");
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({
      managementItemCode: "FR-031",
      achievementDate: "2025-04-10",
      achievementContent: "수업 개선",
      academicYear: 2025,
      semester: 1,
    });
    expect(window.confirm).toHaveBeenCalled();
  });

  it("uses path PUT for selected row, warns on date and refreshes list", async () => {
    vi.mocked(apiRequest).mockImplementation(async (path, init) => {
      if (init?.method === "PUT")
        return success({
          achievement: { ...row, semester: 2 },
          occurredDateWarning: true,
          warningMessage: "발생일이 평가대상 기간 밖입니다.",
        });
      if (path.endsWith("/82")) return success(row);
      return success(list);
    });
    render(<LectureImprovementManagementPage />);
    await screen.findByText("LI-test");
    fireEvent.click(screen.getByTestId("lecture-improvement-detail-82"));
    await waitFor(() =>
      expect(
        screen.getByTestId("lecture-improvement-achievement-content"),
      ).toHaveValue(row.achievementContent),
    );
    fireEvent.change(screen.getByTestId("lecture-improvement-semester"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByTestId("lecture-improvement-save-button"));
    await screen.findByText("발생일이 평가대상 기간 밖입니다.");
    const call = vi
      .mocked(apiRequest)
      .mock.calls.find(([, init]) => init?.method === "PUT");
    expect(call?.[0]).toBe("/api/business/lecture-improvements/82");
    expect(JSON.parse(String(call?.[1]?.body)).semester).toBe(2);
    expect(JSON.parse(String(call?.[1]?.body))).not.toHaveProperty(
      "attachmentIds",
    );
    expect(apiRequest).toHaveBeenCalledTimes(4);
  });

  it.each(["failed", "missing", "wrong identity"])(
    "keeps %s detail readonly until explicitly starting a new entry",
    async (scenario) => {
      vi.mocked(apiRequest).mockImplementation(async (path) => {
        if (path.endsWith("/82")) {
          if (scenario === "failed")
            throw new ApiClientError(500, "상세 조회 실패");
          if (scenario === "missing") return success(null);
          return success({ ...row, achievementId: 99 });
        }
        return success(list);
      });
      render(<LectureImprovementManagementPage />);
      await screen.findByText("LI-test");
      fireEvent.click(screen.getByTestId("lecture-improvement-detail-82"));
      await screen.findByText(
        scenario === "failed"
          ? "상세 조회 실패"
          : "선택한 실적의 상세 응답을 확인할 수 없습니다.",
      );
      expect(
        screen.getByTestId("lecture-improvement-save-button"),
      ).toBeDisabled();
      expect(
        screen.getByTestId("lecture-improvement-achievement-content"),
      ).toBeDisabled();
      expect(
        vi
          .mocked(apiRequest)
          .mock.calls.some(([, init]) => init?.method === "POST"),
      ).toBe(false);
      fireEvent.click(screen.getByTestId("lecture-improvement-create-button"));
      expect(
        screen.getByTestId("lecture-improvement-save-button"),
      ).toBeEnabled();
    },
  );

  it("never saves when confirmation is cancelled", async () => {
    vi.mocked(window.confirm).mockReturnValue(false);
    render(<LectureImprovementManagementPage />);
    await screen.findByText("LI-test");
    fillForm();
    fireEvent.click(screen.getByTestId("lecture-improvement-save-button"));
    expect(
      vi
        .mocked(apiRequest)
        .mock.calls.some(([, init]) => init?.method === "POST"),
    ).toBe(false);
  });

  it("displays field errors and keeps the unsaved form", async () => {
    vi.mocked(apiRequest).mockImplementation(async (_path, init) => {
      if (init?.method === "POST")
        throw new ApiClientError(400, "입력값 오류", {
          code: "VALIDATION_ERROR",
          message: "입력값 오류",
          fields: [
            { field: "semester", message: "학기 코드가 만료되었습니다." },
          ],
        });
      return success(list);
    });
    render(<LectureImprovementManagementPage />);
    await screen.findByText("LI-test");
    fillForm();
    fireEvent.click(screen.getByTestId("lecture-improvement-save-button"));
    await screen.findByText("학기 코드가 만료되었습니다.");
    expect(
      screen.getByTestId("lecture-improvement-achievement-content"),
    ).toHaveValue("수업 개선");
  });

  it.each(["EVALUATION_CONFIRMED", "SUBMITTED"])(
    "locks %s detail without hiding its values",
    async (status) => {
      vi.mocked(apiRequest).mockImplementation(async (path) =>
        path.endsWith("/82")
          ? success({ ...row, achievementStatus: status })
          : success({
              ...list,
              achievements: [{ ...row, achievementStatus: status }],
            }),
      );
      render(<LectureImprovementManagementPage />);
      await screen.findByText("LI-test");
      fireEvent.click(screen.getByTestId("lecture-improvement-detail-82"));
      await screen.findByText("본인의 작성중 실적만 수정할 수 있습니다.");
      expect(
        screen.getByTestId("lecture-improvement-save-button"),
      ).toBeDisabled();
      expect(
        screen.getByTestId("lecture-improvement-achievement-content"),
      ).toHaveValue(row.achievementContent);
      expect(screen.getByTestId("lecture-improvement-semester")).toBeDisabled();
    },
  );

  it("keeps department-only views readonly and prevents another owner's editing", async () => {
    vi.mocked(apiRequest).mockImplementation(async (path) =>
      path.endsWith("/82")
        ? success({ ...row, teacherUserId: 102 })
        : success({
            ...list,
            canCreate: false,
            achievements: [{ ...row, teacherUserId: 102 }],
          }),
    );
    render(<LectureImprovementManagementPage />);
    await screen.findByText("LI-test");
    fireEvent.click(screen.getByTestId("lecture-improvement-detail-82"));
    await screen.findByText("본인의 작성중 실적만 수정할 수 있습니다.");
    expect(
      screen.getByTestId("lecture-improvement-save-button"),
    ).toBeDisabled();
    expect(
      screen.getByTestId("lecture-improvement-create-button"),
    ).toBeDisabled();
  });

  it("applies search and the chosen page size to the relative API", async () => {
    render(<LectureImprovementManagementPage />);
    await screen.findByText("LI-test");
    fireEvent.change(screen.getByTestId("lecture-improvement-search-input"), {
      target: { value: "교원" },
    });
    fireEvent.click(screen.getByTestId("lecture-improvement-search-button"));
    await waitFor(() =>
      expect(
        vi
          .mocked(apiRequest)
          .mock.calls.some(([path]) => path.includes("teacherName=")),
      ).toBe(true),
    );
    fireEvent.change(screen.getByTestId("lecture-improvement-page-size"), {
      target: { value: "50" },
    });
    await waitFor(() =>
      expect(
        vi
          .mocked(apiRequest)
          .mock.calls.some(([path]) => path.includes("pageSize=50")),
      ).toBe(true),
    );
  });

  it("shows permission-denied and empty states without fabricated rows", async () => {
    vi.mocked(apiRequest).mockRejectedValue(
      new ApiClientError(403, "권한 없음"),
    );
    const view = render(<LectureImprovementManagementPage />);
    await screen.findByText("인증 및 해당 실적 범위의 조회 권한이 필요합니다.");
    expect(
      screen.queryByTestId("lecture-improvement-detail-panel"),
    ).not.toBeInTheDocument();
    view.unmount();
    vi.mocked(apiRequest).mockResolvedValue(
      success({ ...list, achievements: [], totalElements: 0 }),
    );
    render(<LectureImprovementManagementPage />);
    await screen.findByText("조회 조건에 맞는 결과가 없습니다.");
  });

  it("blocks save when configured choices are missing", async () => {
    vi.mocked(apiRequest).mockResolvedValue(
      success({ ...list, academicYears: [], semesters: [] }),
    );
    render(<LectureImprovementManagementPage />);
    await screen.findByText(
      "관리항목·학년도·학기 코드가 없습니다. 관리 설정 확인 후 저장하세요.",
    );
    expect(
      screen.getByTestId("lecture-improvement-save-button"),
    ).toBeDisabled();
  });
});
